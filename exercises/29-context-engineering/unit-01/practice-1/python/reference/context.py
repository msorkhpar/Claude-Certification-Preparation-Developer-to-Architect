"""Keeping a conversation inside its budget, and checking the citations in an answer. See ../../statement.md."""
import copy
import logging

log = logging.getLogger(__name__)

SUMMARY_OPEN, SUMMARY_CLOSE = "<summary>\n", "\n</summary>"


def _tokens(text):
    return (len(text) + 3) // 4


def count_tokens(messages):
    """Given: a rough size of a conversation. 4 per message, plus 1 per 4 characters of text and of tool result text, plus 10 per tool call."""
    total = 0
    for message in messages:
        total += 4
        content = message["content"]
        if isinstance(content, str):
            total += _tokens(content)
            continue
        for block in content:
            if block["type"] == "text":
                total += _tokens(block["text"])
            elif block["type"] == "tool_result":
                total += _tokens(block.get("content") or "")
            elif block["type"] == "tool_use":
                total += 10
    return total


def _blocks(message):
    content = message["content"]
    return [{"type": "text", "text": content}] if isinstance(content, str) else copy.deepcopy(content)


def _starts_turn(message):
    """A user message that is not made only of tool results begins a turn."""
    if message["role"] != "user":
        return False
    content = message["content"]
    return isinstance(content, str) or any(block["type"] != "tool_result" for block in content)


def split_turns(messages):
    """The messages as a list of turns: each turn is a user message that is not a tool result, and everything up to the next one."""
    log.debug("split_turns input: %r", messages)
    turns = []
    for message in messages:
        if _starts_turn(message) or not turns:
            turns.append([])
        turns[-1].append(message)
    return turns


def _clear_oldest(results, keep, placeholder):
    """Replace the content of every result but the newest `keep` with the placeholder."""
    for block in results[:max(len(results) - keep, 0)]:
        block["content"] = placeholder


def clear_tool_results(messages, keep=2, exclude=(), placeholder="[cleared]"):
    """Copy of the conversation in which every tool result but the newest `keep` has its content replaced by the placeholder."""
    out = copy.deepcopy(messages)
    names = {}
    for message in out:
        if isinstance(message["content"], list):
            for block in message["content"]:
                if block["type"] == "tool_use":
                    names[block["id"]] = block["name"]
    results = [block for message in out if isinstance(message["content"], list)
               for block in message["content"] if block["type"] == "tool_result" and names.get(block["tool_use_id"]) not in exclude]
    _clear_oldest(results, keep, placeholder)
    return out


def _trim(pinned, rest, budget):
    """The turns of `rest` that remain: the oldest are dropped while the conversation is over budget, the newest always stays."""
    while len(rest) > 1 and count_tokens([m for t in pinned + rest for m in t]) > budget:
        rest = rest[1:]
    return rest


def window(messages, budget, pin=False):
    """Drop the oldest whole turns until the conversation fits; the newest turn always stays. With pin, the first turn stays too."""
    turns = split_turns(messages)
    pinned, rest = (turns[:1], turns[1:]) if pin else ([], turns)
    rest = _trim(pinned, rest, budget)
    return [m for t in pinned + rest for m in t]


def _leave_alone(messages, turns, budget, keep_turns):
    """Whether compaction has nothing to do: the conversation fits, or there are no more turns than `keep_turns`."""
    return count_tokens(messages) <= budget or len(turns) <= keep_turns


def _with_summary(kept, summary):
    """The first kept message with the summary block placed before its own blocks."""
    return {"role": kept[0]["role"], "content": [{"type": "text", "text": f"{SUMMARY_OPEN}{summary}{SUMMARY_CLOSE}"}] + _blocks(kept[0])}


def compact(messages, budget, summarise, keep_turns=1):
    """When the conversation is over budget, replace everything before the newest `keep_turns` turns by one summary."""
    turns = split_turns(messages)
    if _leave_alone(messages, turns, budget, keep_turns):
        return list(messages)
    older = [m for t in turns[:-keep_turns] for m in t]
    kept = [m for t in turns[-keep_turns:] for m in t]
    summary = summarise(older)
    return [_with_summary(kept, summary)] + kept[1:]


def _span_problem(text, cite):
    """The problem of a citation whose document exists, or None when it can be trusted."""
    start, end = cite["start_char_index"], cite["end_char_index"]
    if start < 0 or end <= start or end > len(text):
        return "bad_range"
    if text[start:end] != cite["cited_text"]:
        return "text_mismatch"
    return None


def verify_citations(blocks, documents):
    """One {"block", "citation", "problem"} per citation that cannot be trusted, in order."""
    problems = []
    for i, block in enumerate(blocks):
        for j, cite in enumerate(block.get("citations") or []):
            if cite.get("type") != "char_location":
                problem = "unsupported_type"
            elif not 0 <= cite["document_index"] < len(documents):
                problem = "unknown_document"
            else:
                problem = _span_problem(documents[cite["document_index"]]["text"], cite)
            if problem:
                problems.append({"block": i, "citation": j, "problem": problem})
    return problems


def _number_for(numbers, key):
    """Give a new key the next number; True when the key was new."""
    if key not in numbers:
        numbers[key] = len(numbers) + 1
        return True
    return False


def footnotes(blocks, documents):
    """The answer text with a [n] after each cited block and a Sources list; one number per distinct cited span, in order."""
    numbers, sources, out = {}, [], []
    for block in blocks:
        out.append(block["text"])
        for cite in block.get("citations") or []:
            key = (cite["document_index"], cite["start_char_index"], cite["end_char_index"])
            if _number_for(numbers, key):
                sources.append(f'[{numbers[key]}] {documents[cite["document_index"]]["title"]}: "{cite["cited_text"]}"')
            out.append(f"[{numbers[key]}]")
    text = "".join(out)
    return text + ("\n\nSources:\n" + "\n".join(sources) if sources else "")
