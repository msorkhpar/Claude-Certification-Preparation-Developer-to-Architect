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
    # TODO 1 of 7 (unlocks m1, e2, e3): group the messages into turns.
    # Receives the list of messages (`_starts_turn(message)` says whether one begins a turn; the first message always does).
    # Returns a list of turns, each a list of messages, in order.
    # Example: [user "q", assistant tool_use, user tool_result, assistant "a", user "q2"] -> [[q, tool_use, tool_result, a], [q2]]
    return [list(messages)]


def _clear_oldest(results, keep, placeholder):
    """TODO 2 of 7 (unlocks e1): replace the content of every result but the newest `keep` with the placeholder.

    Receives the list of tool_result blocks in conversation order, `keep` and the placeholder; changes the blocks in place, returns nothing.
    Example: three results with keep=2 -> the first one's content becomes "[cleared]".
    """


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
    """TODO 3 of 7 (unlocks e2): the turns of `rest` that remain.

    Receives the pinned turns, the other turns oldest first, and the budget. Drops the oldest turn of `rest` while
    `count_tokens` of pinned + rest is over the budget, but never the last one. Returns the remaining turns.
    Example: three turns that are over budget by one turn's size -> the last two.
    """
    return rest


def window(messages, budget, pin=False):
    """Drop the oldest whole turns until the conversation fits; the newest turn always stays. With pin, the first turn stays too."""
    turns = split_turns(messages)
    pinned, rest = (turns[:1], turns[1:]) if pin else ([], turns)
    rest = _trim(pinned, rest, budget)
    return [m for t in pinned + rest for m in t]


def _leave_alone(messages, turns, budget, keep_turns):
    """TODO 4 of 7 (unlocks e3): whether compaction has nothing to do.

    Receives the messages, their turns, the budget and keep_turns. True when the conversation fits the budget or there are no more
    turns than `keep_turns`. The turn-count half is written; add the budget half.
    Example: a conversation of 40 tokens with budget 50 -> True.
    """
    return len(turns) <= keep_turns


def _with_summary(kept, summary):
    """TODO 5 of 7 (unlocks m1, e4): the first kept message with the summary block placed before its own blocks.

    Receives the kept messages and the summary text. Returns one message with the role of `kept[0]` and as content the text block
    `SUMMARY_OPEN + summary + SUMMARY_CLOSE` followed by `_blocks(kept[0])`.
    Example: kept[0] = user "q2", summary "S" -> {"role": "user", "content": [<summary block>, {"type": "text", "text": "q2"}]}
    """
    return kept[0]


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
    """TODO 6 of 7 (unlocks e5): the problem of a citation whose document exists, or None when it can be trusted.

    Receives the document text and the citation. Returns "bad_range" (start below 0, end not after start, or end past the text),
    else "text_mismatch" (`text[start:end]` is not the cited text, end excluded), else None.
    Example: text "abcdef", span 1 to 3, cited_text "bc" -> None; cited_text "cd" -> "text_mismatch"
    """
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
    """TODO 7 of 7 (unlocks e6): give a new key the next number; True when the key was new.

    Receives the dict of numbers so far and a key. A key not in it gets `len(numbers) + 1` and the answer is True; a known key keeps
    its number and the answer is False. Example: {} and "a" -> True, numbers == {"a": 1}; then "a" again -> False
    """
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
