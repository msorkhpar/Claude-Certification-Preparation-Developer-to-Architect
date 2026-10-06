"""A claims assistant on one page of code: identifiers swapped for tokens before anything is sent, retrieval that filters by the reader's rights first and refuses stale evidence, a source check, a route to a person, a trace that holds no content, and a release gate that protects the costly segment.

The documents, requests, answers and cases are invented, and the model is a scripted answer, so nothing here calls a model. The thresholds (a confidence of 95) are design values.
"""
import logging

log = logging.getLogger(__name__)
import re
from collections import namedtuple

Chunk = namedtuple("Chunk", "id doc version text")
Request = namedtuple("Request", "id text allowed consequence quote confidence")
Case = namedtuple("Case", "id segment old_ok new_ok")

CURRENT = {"policy": 3, "contracts": 1}
INDEX = [Chunk("policy-1", "policy", 3, "Claims must be reported within 30 days of the loss."),
         Chunk("policy-2", "policy", 3, "Water damage is covered up to 5,000 per claim."),
         Chunk("contract-9", "contracts", 1, "Partner commission is 12 percent of premiums.")]
STALE_INDEX = [INDEX[0], Chunk("policy-2-old", "policy", 2, "Water damage is covered up to 3,000 per claim."), INDEX[2]]


def tokenise(text):
    """Identifiers become tokens before the text leaves the caller; the map from token to value stays here."""
    vault = {}
    def swap(match):
        value = match.group(0)
        for token, known in vault.items():
            if known == value:
                return token
        token = f"<EMAIL_{len(vault) + 1}>"
        vault[token] = value
        return token
    return re.sub(r"[\w.+-]+@[\w-]+\.[\w.]+", swap, text), vault


def words(text):
    return {w for w in re.findall(r"[a-z]+", text.lower()) if len(w) > 3}


def retrieve(question, allowed, index):
    """The reader's rights are applied before ranking; the best match wins, a tie goes to the smaller id, and no overlap is no evidence."""
    scored = sorted(((-len(words(question) & words(c.text)), c.id, c) for c in index if c.doc in allowed), key=lambda t: t[:2])
    return scored[0][2] if scored and scored[0][0] < 0 else None


def handle(request, index):
    """One request through the chain; the outcome says why a request was held."""
    log.debug("handle input: %r", request)
    sent, _ = tokenise(request.text)
    chunk = retrieve(sent, request.allowed, index)
    if chunk is None:
        outcome = "hold: no evidence"
    elif chunk.version != CURRENT[chunk.doc]:
        outcome = f"hold: stale evidence ({chunk.id} v{chunk.version}, current v{CURRENT[chunk.doc]})"
    elif request.quote not in chunk.text:
        outcome = "hold: unsupported"
    elif request.consequence == "high":
        outcome = "human"
    else:
        outcome = "auto" if request.confidence >= 95 else "review"
    trace = {"request": request.id, "chunk": f"{chunk.id}@v{chunk.version}" if chunk else "none", "outcome": outcome, "chars": len(sent)}
    return sent, trace


def keep(trace):
    """Tail-based: a trace that was held or reached a person is kept, the rest are sampled elsewhere."""
    return trace["outcome"].startswith("hold") or trace["outcome"] == "human"


def release(cases, protected):
    """A change ships only when no protected segment loses an answer and the losses do not outnumber the gains."""
    lost = [c for c in cases if c.old_ok and not c.new_ok]
    gained = [c for c in cases if c.new_ok and not c.old_ok]
    hit = sorted({c.segment for c in lost if c.segment in protected})
    if hit:
        return "no-go: protected segment lost answers: " + ", ".join(hit)
    if len(lost) > len(gained):
        return f"no-go: net loss: lost {len(lost)}, gained {len(gained)}"
    return f"go: lost {len(lost)}, gained {len(gained)}"


def main():
    water = "Water damage is covered up to 5,000 per claim."
    late = "Claims must be reported within 30 days of the loss."
    requests = [
        (Request("r1", "How much does the policy cover for water damage?", {"policy"}, "low", water, 97), INDEX),
        (Request("r2", "How much does the policy cover for water damage?", {"policy"}, "low", water, 97), STALE_INDEX),
        (Request("r3", "Can I get a refund of 400 for water damage?", {"policy"}, "high", water, 99), INDEX),
        (Request("r4", "What is the partner commission?", {"policy"}, "low", water, 99), INDEX),
        (Request("r5", "I reported my claim from jo@example.com, how many days do I have?", {"policy"}, "low", late, 96), INDEX),
    ]
    traces = []
    for request, index in requests:
        sent, trace = handle(request, index)
        traces.append(trace)
        print(f"{request.id}: sent={sent!r}; evidence={trace['chunk']}; outcome={trace['outcome']}")
    print("traces kept:", ", ".join(t["request"] for t in traces if keep(t)))
    cases = [Case(f"s{i}", "status", i > 2, True) for i in range(1, 7)] + [Case(f"f{i}", "refund", True, i != 4) for i in range(1, 5)] + [Case("c1", "complaint", False, True), Case("c2", "complaint", True, True)]
    print("release with refunds protected:", release(cases, {"refund"}))
    fixed = [Case(c.id, c.segment, c.old_ok, True) if c.id == "f4" else c for c in cases]
    print("release after the refund fix:", release(fixed, {"refund"}))


if __name__ == "__main__":
    main()
