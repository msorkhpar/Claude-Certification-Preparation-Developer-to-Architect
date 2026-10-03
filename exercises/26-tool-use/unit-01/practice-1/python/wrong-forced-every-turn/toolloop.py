"""A tool loop against a scripted model. See ../../statement.md."""
import json

FORCED_UNSUPPORTED = {"claude-opus-5-5", "claude-sonnet-5-5", "claude-fable-5-1", "claude-mythos-5-1"}
CHOICE_TYPES = {"auto", "any", "tool", "none"}


class ToolError(Exception):
    """A tool refuses or fails; its message goes back to the model as an error result."""


class RequestError(Exception):
    """The request would be rejected with a 400. `field` names the offending part."""

    def __init__(self, field, reason):
        super().__init__(f"{field}: {reason}")
        self.field, self.reason = field, reason


def _check_choice(tools, model, choice):
    kind = choice.get("type")
    if kind not in CHOICE_TYPES:
        raise RequestError("tool_choice.type", "must be auto, any, tool or none")
    if "disable_parallel_tool_use" in choice and not isinstance(choice["disable_parallel_tool_use"], bool):
        raise RequestError("tool_choice.disable_parallel_tool_use", "must be a boolean")
    if kind in ("any", "tool") and model in FORCED_UNSUPPORTED:
        raise RequestError("tool_choice", f"{model} does not support forced tool use")
    if kind == "tool" and choice.get("name") not in [t["name"] for t in tools]:
        raise RequestError("tool_choice.name", "names no tool in the request")


def _run_one(tools, block):
    tool = next((t for t in tools if t["name"] == block["name"]), None)
    result = {"type": "tool_result", "tool_use_id": block["id"]}
    if tool is None:
        return {**result, "content": f"Unknown tool: {block['name']}", "is_error": True}
    missing = [k for k in tool["input_schema"].get("required", []) if k not in block["input"]]
    if missing:
        return {**result, "content": f"Missing required input: {', '.join(missing)}", "is_error": True}
    try:
        out = tool["handler"](block["input"])
    except Exception as err:  # a tool that fails must not end the loop
        return {**result, "content": str(err), "is_error": True}
    return {**result, "content": out if isinstance(out, str) else json.dumps(out)}


def _text(content):
    return "".join(b["text"] for b in content if b["type"] == "text")


def run_agent(ask, tools, user_text, model="claude-sonnet-5-5", max_turns=8, tool_choice=None):
    """Call the model, run every tool it asks for, send the results back, until it ends its turn or a limit is hit."""
    if tool_choice is not None:
        _check_choice(tools, model, tool_choice)
    definitions = [{k: v for k, v in t.items() if k != "handler"} for t in tools]
    messages = [{"role": "user", "content": user_text}]
    text, calls = "", 0
    for turn in range(1, max_turns + 1):
        request = {"model": model, "max_tokens": 1024, "messages": list(messages), "tools": definitions}
        if tool_choice is not None:
            forced = tool_choice["type"] in ("any", "tool")
            request["tool_choice"] = tool_choice
        reply = ask(request)
        calls = turn
        messages.append({"role": "assistant", "content": reply["content"]})
        text = _text(reply["content"])
        stop = reply["stop_reason"]
        if stop == "tool_use":
            results = [_run_one(tools, b) for b in reply["content"] if b["type"] == "tool_use"]
            messages.append({"role": "user", "content": results})
        elif stop == "pause_turn":
            continue
        elif stop in ("end_turn", "stop_sequence"):
            return {"status": "done", "text": text, "turns": calls, "messages": messages}
        elif stop == "refusal":
            return {"status": "refused", "text": text, "turns": calls, "messages": messages}
        else:  # max_tokens, model_context_window_exceeded: the answer is cut off
            return {"status": "truncated", "text": text, "turns": calls, "messages": messages}
    return {"status": "max_turns", "text": text, "turns": calls, "messages": messages}
