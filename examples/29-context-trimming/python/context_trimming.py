"""Clearing old tool results, asking the API to clear them, and checking the citations in an answer.

The replies are illustrative, hand-written bodies in the shapes of the context editing and citations pages (claude-sonnet-5-5),
not captures; the numbers in the context editing response are the documentation's own example.
"""
import logging
import copy

from harness import scripted_client
from harness.scripted import message, text

log = logging.getLogger(__name__)

MODEL = "claude-sonnet-5-5"
POLICY = "The grass is green. The sky is blue. Water is essential for life."


def tokens(messages):
    """A rough size: 4 per message, 1 per 4 characters of text, 10 per tool call."""
    total = 0
    for m in messages:
        total += 4
        for b in m["content"] if isinstance(m["content"], list) else [{"type": "text", "text": m["content"]}]:
            total += {"text": lambda: (len(b.get("text", "")) + 3) // 4, "tool_result": lambda: (len(b["content"]) + 3) // 4, "tool_use": lambda: 10}[b["type"]]()
    return total


def conversation():
    messages = [{"role": "user", "content": "Find every mention of the grass in the logs."}]
    for i in range(1, 6):
        messages.append({"role": "assistant", "content": [{"type": "tool_use", "id": f"toolu_{i}", "name": "grep_logs", "input": {"pattern": f"grass-{i}"}}]})
        messages.append({"role": "user", "content": [{"type": "tool_result", "tool_use_id": f"toolu_{i}", "content": f"log line {i}: " + "x" * 400}]})
    messages.append({"role": "assistant", "content": [{"type": "text", "text": "Found them all."}]})
    return messages


def clear_tool_results(messages, keep=2, placeholder="[cleared]"):
    """A copy in which every tool result but the newest `keep` has its content replaced; the calls stay."""
    out = copy.deepcopy(messages)
    results = [b for m in out if isinstance(m["content"], list) for b in m["content"] if b["type"] == "tool_result"]
    for block in results[:max(len(results) - keep, 0)]:
        block["content"] = placeholder
    return out


def verify(blocks, documents):
    """A citation is a claim about where text came from; check it against the document."""
    bad = []
    for i, block in enumerate(blocks):
        for j, cite in enumerate(block.get("citations") or []):
            if documents[cite["document_index"]][cite["start_char_index"]:cite["end_char_index"]] != cite["cited_text"]:
                bad.append({"block": i, "citation": j, "problem": "text_mismatch"})
    return bad


def footnotes(blocks, titles):
    numbers, lines, out = {}, [], ""
    for block in blocks:
        out += block["text"]
        for cite in block.get("citations") or []:
            key = (cite["document_index"], cite["start_char_index"], cite["end_char_index"])
            if key not in numbers:
                numbers[key] = len(numbers) + 1
                lines.append(f'[{numbers[key]}] {titles[cite["document_index"]]}: "{cite["cited_text"]}"')
            out += f"[{numbers[key]}]"
    return out + ("\n\nSources:\n" + "\n".join(lines) if lines else "")


def cite(start, end):
    return {"type": "char_location", "cited_text": POLICY[start:end], "document_index": 0, "document_title": "Policy", "start_char_index": start, "end_char_index": end, "file_id": None}


EDITS = {"edits": [{"type": "clear_tool_uses_20250919", "trigger": {"type": "input_tokens", "value": 30000}, "keep": {"type": "tool_uses", "value": 3},
                    "clear_at_least": {"type": "input_tokens", "value": 5000}, "exclude_tools": ["web_search"]}]}


def editing_reply():
    body = message([text("Found them all.")], model=MODEL)
    body["context_management"] = {"applied_edits": [{"type": "clear_tool_uses_20250919", "cleared_tool_uses": 8, "cleared_input_tokens": 50000}]}
    return body


def cited_reply():
    blocks = [{"type": "text", "text": "The grass is green. ", "citations": [cite(0, 19)]}, {"type": "text", "text": "Water matters. ", "citations": [cite(37, 65)]},
              {"type": "text", "text": "Green again.", "citations": [cite(0, 19)]}]
    return message(blocks, model=MODEL)


def main():
    before = conversation()
    after = clear_tool_results(before, 2)
    cleared = sum(1 for m in after if isinstance(m["content"], list) for b in m["content"] if b["type"] == "tool_result" and b["content"] == "[cleared]")
    print(f"conversation: {len(before)} messages, 5 tool results, about {tokens(before)} tokens")
    print(f"after clearing all but the newest 2 results: about {tokens(after)} tokens, {cleared} results replaced, calls kept: {[m for m in after if m['role'] == 'assistant'] == [m for m in before if m['role'] == 'assistant']}")
    client, transport = scripted_client(editing_reply(), cited_reply())
    reply = client.beta.messages.create(model=MODEL, max_tokens=300, messages=before, betas=["context-management-2025-06-27"], context_management=EDITS)
    print("beta header sent:", transport.headers[0]["anthropic-beta"])
    edit = transport.requests[0]["context_management"]["edits"][0]
    print("edit sent:", edit["type"], "trigger", edit["trigger"]["value"], "keep", edit["keep"]["value"], "exclude", edit["exclude_tools"])
    applied = reply.context_management.applied_edits[0]
    print("applied edit reported:", applied.type, f"cleared {applied.cleared_tool_uses} tool uses, {applied.cleared_input_tokens} input tokens")
    document = {"type": "document", "source": {"type": "text", "media_type": "text/plain", "data": POLICY}, "title": "Policy", "citations": {"enabled": True}}
    answer = client.messages.create(model=MODEL, max_tokens=300, messages=[{"role": "user", "content": [document, {"type": "text", "text": "What does the policy say about grass and water?"}]}])
    blocks = [b.model_dump() for b in answer.content]
    print("citations enabled in the request:", transport.requests[1]["messages"][0]["content"][0]["citations"])
    print("citation problems:", verify(blocks, [POLICY]))
    tampered = copy.deepcopy(blocks)
    tampered[1]["citations"][0]["cited_text"] = "Water is optional."
    print("after tampering with one cited_text:", verify(tampered, [POLICY]))
    print(footnotes(blocks, ["Policy"]))


if __name__ == "__main__":
    main()
