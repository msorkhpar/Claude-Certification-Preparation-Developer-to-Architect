"""Keeping a conversation inside its budget, and checking the citations in an answer. See ../../statement.md."""
import copy

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


def split_turns(messages):
    # TODO: the messages as a list of turns: a turn is a user message that is not only tool results, and everything up to the next one.
    return None


def clear_tool_results(messages, keep=2, exclude=(), placeholder="[cleared]"):
    # TODO: a copy in which every tool result but the newest `keep` has its content replaced by the placeholder.
    return None


def window(messages, budget, pin=False):
    # TODO: drop the oldest whole turns until the conversation fits; the newest turn always stays; with pin the first stays too.
    return None


def compact(messages, budget, summarise, keep_turns=1):
    # TODO: over budget, replace everything before the newest `keep_turns` turns by one summary block.
    return None


def verify_citations(blocks, documents):
    # TODO: one {"block", "citation", "problem"} per citation that cannot be trusted, in order.
    return None


def footnotes(blocks, documents):
    # TODO: the answer text with a [n] after each cited block and a Sources list; one number per distinct cited span, in order.
    return None
