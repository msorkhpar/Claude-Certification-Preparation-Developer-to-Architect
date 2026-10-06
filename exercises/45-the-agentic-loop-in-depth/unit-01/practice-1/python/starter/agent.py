"""The agent loop, driven by the stop reason. See ../../statement.md."""
import logging

log = logging.getLogger(__name__)


def _text(content):
    """TODO 1 of 6 (unlocks m1 and e5): the text of a reply.

    Receives a reply's content list of blocks. Returns the `text` of every block whose type is "text", joined with nothing between.
    Example: _text([{"type": "text", "text": "Hi "}, {"type": "tool_use", "id": "t"}, {"type": "text", "text": "there"}]) -> "Hi there"
    """
    return ""


def _calls(content):
    """TODO 2 of 6 (unlocks e2 and e6): the tool calls of a reply.

    Receives a reply's content list. Returns its `tool_use` blocks, in order; an empty list when there are none.
    Example: _calls([{"type": "text", "text": "x"}, {"type": "tool_use", "id": "a"}]) -> [{"type": "tool_use", "id": "a"}]
    """
    return []


def _tool_result(block, content, is_error=False):
    """TODO 3 of 6 (unlocks m1, e2 and e3): one tool_result block.

    Receives the tool_use block, the result text and a flag. Returns {"type": "tool_result", "tool_use_id": <the block's id>,
    "content": <the text>}, with "is_error": True added only when the flag is set (a good result has no is_error key).
    Example: _tool_result({"id": "t1"}, "boom", True) -> {"type": "tool_result", "tool_use_id": "t1", "content": "boom", "is_error": True}
    """
    return {}


def _status_for(reason, calls):
    """TODO 4 of 6 (unlocks m1, e5 and e6): the status a stop reason ends the run with (the loop only gets here when it cannot go on).

    Receives the stop reason and the tool calls of the reply. Returns "malformed" for tool_use with no calls, "done" for end_turn and
    stop_sequence, "truncated" for max_tokens, "refused" for refusal and "unexpected" for anything else.
    Example: _status_for("max_tokens", []) -> "truncated", _status_for("tool_use", []) -> "malformed", _status_for("brand_new", []) -> "unexpected"
    """
    return ""


def _at_limit(turns, max_turns):
    """TODO 5 of 6 (unlocks e4): has the turn limit been reached before the next model call?

    Receives the model calls made so far and the limit. Returns True when no call is left. Example: _at_limit(3, 3) -> True, _at_limit(2, 3) -> False
    """
    return False


def _snapshot(messages):
    """TODO 6 of 6 (unlocks m1): the copy of the messages that the model is handed.

    Receives the list of messages so far. Returns a new list with the same items, so later changes do not rewrite what the model saw.
    Example: _snapshot([a, b]) -> a new list [a, b]
    """
    return messages


def _run_tool(block, tools):
    handler = tools.get(block.get("name"))
    if handler is None:
        return _tool_result(block, f"Unknown tool: {block.get('name')}", True)
    try:
        return _tool_result(block, handler(block.get("input", {})))
    except Exception as error:  # a failing tool is a result for the model, not a crash of the loop
        return _tool_result(block, str(error), True)


def run_agent(model, tools, task, max_turns=8):
    log.debug("run_agent input: %r", task)
    messages = [{"role": "user", "content": task}]
    turns, last_text = 0, ""
    while True:
        if _at_limit(turns, max_turns):  # the count is a backstop: it only ends a run the model has not ended itself
            return {"status": "max_turns", "text": last_text, "turns": turns, "messages": messages}
        turns += 1
        reply = model(_snapshot(messages))
        content = reply["content"]
        last_text = _text(content)
        messages.append({"role": "assistant", "content": content})
        reason = reply["stop_reason"]
        calls = _calls(content)
        if reason == "tool_use" and calls:
            messages.append({"role": "user", "content": [_run_tool(block, tools) for block in calls]})
        else:
            return {"status": _status_for(reason, calls), "text": last_text, "turns": turns, "messages": messages}
