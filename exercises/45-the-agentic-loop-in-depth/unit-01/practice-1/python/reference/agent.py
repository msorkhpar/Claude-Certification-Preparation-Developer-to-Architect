"""The agent loop, driven by the stop reason. See ../../statement.md."""
import logging

log = logging.getLogger(__name__)


def _text(content):
    return "".join(block.get("text", "") for block in content if block.get("type") == "text")


def _calls(content):
    return [block for block in content if block.get("type") == "tool_use"]


def _tool_result(block, content, is_error=False):
    result = {"type": "tool_result", "tool_use_id": block["id"], "content": content}
    if is_error:
        result["is_error"] = True
    return result


def _status_for(reason, calls):
    if reason == "tool_use" and not calls:
        return "malformed"
    if reason in ("end_turn", "stop_sequence"):
        return "done"
    if reason == "max_tokens":
        return "truncated"
    if reason == "refusal":
        return "refused"
    return "unexpected"


def _at_limit(turns, max_turns):
    return turns >= max_turns


def _snapshot(messages):
    return list(messages)


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
