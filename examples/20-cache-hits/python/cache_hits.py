"""Prompt caching seen through the usage object, against a scripted server that applies the documented prefix rule.

The server (CacheSim) is an illustrative, hand-written stand-in, not a capture: it counts a token as four characters,
caches the prefix up to a block that carries cache_control when that prefix reaches the minimum size, keeps it for five
minutes from its last use, and reports `cache_creation_input_tokens`, `cache_read_input_tokens` and `input_tokens` (the
tokens after the last breakpoint) as the prompt caching page describes them (claude-sonnet-5-5, minimum 512 tokens).
"""
import logging
import json
import math

from harness import scripted_client
from harness.scripted import message, text

log = logging.getLogger(__name__)

MODEL = "claude-sonnet-5-5"
POLICY = "Refund policy clause: items may be returned within 14 days. " * 40  # about 600 tokens
MARK = {"type": "ephemeral"}


def tokens(piece):
    return math.ceil(len(piece) / 4)


def blocks_of(body):
    """The request flattened in prefix order: tools, then system, then messages, each with its cache_control."""
    out = [(json.dumps(tool, sort_keys=True), tool.get("cache_control")) for tool in body.get("tools", [])]
    system = body.get("system", [])
    out += [(b["text"], b.get("cache_control")) for b in ([{"text": system}] if isinstance(system, str) else system)]
    for m in body["messages"]:
        content = m["content"] if isinstance(m["content"], list) else [{"text": m["content"]}]
        out += [(m["role"] + ": " + b["text"], b.get("cache_control")) for b in content]
    return out


class CacheSim:
    def __init__(self, minimum=512, ttl=300):
        self.minimum, self.ttl, self.clock, self.entries = minimum, ttl, 0, {}

    def __call__(self, body):
        blocks = blocks_of(body)
        marks = [i for i, (_, mark) in enumerate(blocks) if mark]
        key = lambda i: "\x00".join(t for t, _ in blocks[: i + 1])  # noqa: E731
        size = lambda i: sum(tokens(t) for t, _ in blocks[: i + 1])  # noqa: E731
        read = written = 0
        hit = None
        for i in reversed(marks):
            if self.entries.get(key(i), -1) > self.clock:
                read, hit = size(i), i
                self.entries[key(i)] = self.clock + self.ttl  # a hit refreshes the entry
                break
        if marks and hit != marks[-1] and size(marks[-1]) >= self.minimum:
            written = size(marks[-1]) - read
            self.entries[key(marks[-1])] = self.clock + self.ttl
        fresh = size(len(blocks) - 1) - read - written
        usage = {"input_tokens": fresh, "output_tokens": 20, "cache_read_input_tokens": read, "cache_creation_input_tokens": written,
                 "cache_creation": {"ephemeral_5m_input_tokens": written, "ephemeral_1h_input_tokens": 0}}
        return message([text("ok")], usage=usage)


def request(system, question):
    return dict(model=MODEL, max_tokens=50, system=system, messages=[{"role": "user", "content": question}])


def stable(question):
    return request([{"type": "text", "text": POLICY, "cache_control": MARK}], question)


def stamp_first(clock_label, question):
    return request([{"type": "text", "text": f"Current time: {clock_label}"}, {"type": "text", "text": POLICY, "cache_control": MARK}], question)


def stamp_last(clock_label, question):
    return request([{"type": "text", "text": POLICY, "cache_control": MARK}, {"type": "text", "text": f"Current time: {clock_label}"}], question)


def run(sim, label, body, wait=0):
    sim.clock += wait
    client, _ = scripted_client(sim)
    usage = client.messages.create(**body).usage
    print(f"{label:34} write {usage.cache_creation_input_tokens:4}  read {usage.cache_read_input_tokens:4}  fresh {usage.input_tokens:4}")
    return usage


def main():
    sim = CacheSim()
    run(sim, "1 stable system, first call", stable("Can I return a lamp?"))
    run(sim, "2 same system, new question", stable("Can I return a chair?"), wait=60)
    run(sim, "3 six minutes of silence", stable("Can I return a desk?"), wait=360)
    sim = CacheSim()
    run(sim, "4 timestamp first, 10:01", stamp_first("10:01", "Can I return a lamp?"))
    run(sim, "5 timestamp first, 10:02", stamp_first("10:02", "Can I return a lamp?"), wait=60)
    run(sim, "6 timestamp last, 10:03", stamp_last("10:03", "Can I return a lamp?"), wait=60)
    run(sim, "7 timestamp last, 10:04", stamp_last("10:04", "Can I return a lamp?"), wait=60)


if __name__ == "__main__":
    main()
