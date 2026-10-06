"""A tool loop against a scripted model. See ../../statement.md."""
import json
import logging

log = logging.getLogger(__name__)

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
    """TODO 1 of 7 (finish this to pass e5): refuse a tool_choice the API would reject.

    Receives the tools, the model id and the tool_choice map. Raises RequestError, in this order: field "tool_choice.type" unless the
    type is in CHOICE_TYPES; "tool_choice.disable_parallel_tool_use" when that key is present and not a bool; "tool_choice" for type
    "any" or "tool" on a model in FORCED_UNSUPPORTED; "tool_choice.name" for type "tool" when no tool has that name. Returns None otherwise.
    Example: _check_choice([], "claude-opus-5-5", {"type": "any"}) raises RequestError with field "tool_choice"
    """
    return None


def _missing_inputs(tool, tool_input):
    """TODO 2 of 7 (finish this to pass e2): the required keys a call leaves out.

    Receives a tool and the input map of the call. Returns the keys in the tool's input_schema "required" list that the input lacks,
    in the order of that list; an empty list when none is missing.
    Example: _missing_inputs({"input_schema": {"required": ["city"]}}, {}) -> ["city"]
    """
    return []


def _result_content(out):
    """TODO 3 of 7 (finish this to pass e6): the content of a tool_result.

    Receives what a handler returned. Returns a string as it is and any other value as JSON text (use json.dumps).
    Example: _result_content({"city": "Oslo"}) -> '{"city": "Oslo"}'
    """
    return out


def _run_one(tools, block):
    tool = next((t for t in tools if t["name"] == block["name"]), None)
    result = {"type": "tool_result", "tool_use_id": block["id"]}
    if tool is None:
        return {**result, "content": f"Unknown tool: {block['name']}", "is_error": True}
    missing = _missing_inputs(tool, block["input"])
    if missing:
        return {**result, "content": f"Missing required input: {', '.join(missing)}", "is_error": True}
    try:
        out = tool["handler"](block["input"])
    except Exception as err:  # a tool that fails must not end the loop
        return {**result, "content": str(err), "is_error": True}
    return {**result, "content": _result_content(out)}


def _text(content):
    return "".join(b["text"] for b in content if b["type"] == "text")


def _tool_results(tools, content):
    """TODO 4 of 7 (finish this to pass m1 and e1): run every tool call of a reply.

    Receives the tools and the content blocks of the reply. Returns one tool_result (from _run_one) per block of type "tool_use", in
    the order of the blocks; blocks of any other type, such as "server_tool_use", get no result.
    Example: [{"type": "text", ...}, a tool_use block] -> a list with one tool_result
    """
    return []


def _final_status(stop):
    """TODO 5 of 7 (finish this to pass e4): the status of a reply that ends the loop.

    Receives the stop_reason. Returns "done" for "end_turn" and "stop_sequence", "refused" for "refusal" and "truncated" for any other.
    Example: _final_status("max_tokens") -> "truncated"
    """
    return "done"


def _turn_numbers(max_turns):
    """TODO 6 of 7 (finish this to pass e3): the turns the loop may use.

    Receives max_turns. Returns the turn numbers 1 to max_turns, inclusive, so at most max_turns calls are made.
    Example: _turn_numbers(3) -> 1, 2, 3
    """
    return range(1, 2)


def _sent_choice(tool_choice, turn):
    """TODO 7 of 7 (finish this to pass e5): the tool_choice to send on a turn.

    Receives the tool_choice map and the turn number (1 for the first request). A forced choice (type "any" or "tool") is sent as given
    on turn 1 and as {"type": "auto"} from turn 2 on; "auto" and "none" are sent unchanged every turn.
    Example: _sent_choice({"type": "any"}, 2) -> {"type": "auto"}
    """
    return tool_choice


def run_agent(ask, tools, user_text, model="claude-sonnet-5-5", max_turns=8, tool_choice=None):
    """Call the model, run every tool it asks for, send the results back, until it ends its turn or a limit is hit."""
    log.debug("run_agent input: %r", user_text)
    if tool_choice is not None:
        _check_choice(tools, model, tool_choice)
    definitions = [{k: v for k, v in t.items() if k != "handler"} for t in tools]
    messages = [{"role": "user", "content": user_text}]
    text, calls = "", 0
    for turn in _turn_numbers(max_turns):
        request = {"model": model, "max_tokens": 1024, "messages": list(messages), "tools": definitions}
        if tool_choice is not None:
            request["tool_choice"] = _sent_choice(tool_choice, turn)
        reply = ask(request)
        calls = turn
        messages.append({"role": "assistant", "content": reply["content"]})
        text = _text(reply["content"])
        stop = reply["stop_reason"]
        if stop == "tool_use":
            messages.append({"role": "user", "content": _tool_results(tools, reply["content"])})
        elif stop == "pause_turn":
            continue
        else:
            return {"status": _final_status(stop), "text": text, "turns": calls, "messages": messages}
    return {"status": "max_turns", "text": text, "turns": calls, "messages": messages}
