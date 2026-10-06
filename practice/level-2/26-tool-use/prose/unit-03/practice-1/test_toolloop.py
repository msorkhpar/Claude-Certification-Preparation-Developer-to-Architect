import copy
import json
import os
import sys
from pathlib import Path

# The solution is the file beside this test.
sys.path.insert(0, str(Path(__file__).resolve().parent))
from toolloop import RequestError, ToolError, run_agent


def text(t):
    return {"type": "text", "text": t}


def tool_use(id, name, **input):
    return {"type": "tool_use", "id": id, "name": name, "input": input}


def reply(content, stop_reason="end_turn"):
    return {"id": "msg_illustrative", "type": "message", "role": "assistant", "model": "claude-sonnet-5-5",
            "content": content, "stop_reason": stop_reason, "usage": {"input_tokens": 1, "output_tokens": 1}}


class Scripted:
    """A hand-written, illustrative model: returns the next reply and records a copy of each request."""

    def __init__(self, *replies):
        self.replies, self.seen = list(replies), []

    def __call__(self, request):
        self.seen.append(copy.deepcopy(request))
        return self.replies.pop(0)


CALLS = []


def get_weather(args):
    CALLS.append(("get_weather", args))
    return f"{args['city']}: 18 C"


def get_time(args):
    CALLS.append(("get_time", args))
    return f"{args['city']}: 14:05"


def broken(args):
    raise ToolError("the weather service is down")


def structured(args):
    return {"city": args["city"], "temp_c": 18, "tags": ["mild"]}


def tools():
    schema = {"type": "object", "properties": {"city": {"type": "string"}}, "required": ["city"]}
    return [
        {"name": "get_weather", "description": "Current weather for a city.", "input_schema": schema, "handler": get_weather},
        {"name": "get_time", "description": "Local time for a city.", "input_schema": schema, "handler": get_time},
        {"name": "broken", "description": "Always fails.", "input_schema": schema, "handler": broken},
        {"name": "structured", "description": "Returns an object.", "input_schema": schema, "handler": structured},
    ]


def json_or_text(text):
    """The parsed JSON, or a marker string when the text is not JSON."""
    try:
        return json.loads(text)
    except ValueError:
        return "not JSON: " + text


def failure_of(fn):
    """The RequestError field fn raises, 'crash' for another exception, None when it returns."""
    try:
        fn()
    except RequestError as err:
        return err.field
    except Exception:  # noqa: BLE001
        return "crash"
    return None


def test_m1_a_tool_call_is_run_and_its_result_sent_back_until_the_model_ends_its_turn():
    CALLS.clear()
    first = [text("Let me check."), tool_use("tu_1", "get_weather", city="Oslo")]
    model = Scripted(reply(first, "tool_use"), reply([text("It is 18 C in Oslo.")]))
    result = run_agent(model, tools(), "Weather in Oslo?") or {}
    assert (result.get("status"), result.get("text"), result.get("turns")) == ("done", "It is 18 C in Oslo.", 2)
    assert CALLS == [("get_weather", {"city": "Oslo"})]
    second = model.seen[1]["messages"] if len(model.seen) > 1 else []
    assert [m["role"] for m in second] == ["user", "assistant", "user"]
    assert second[1]["content"] == first
    assert second[2]["content"] == [{"type": "tool_result", "tool_use_id": "tu_1", "content": "Oslo: 18 C"}]
    sent = model.seen[0]
    assert sent["model"] == "claude-sonnet-5-5" and [t["name"] for t in sent["tools"]] == ["get_weather", "get_time", "broken", "structured"]
    assert all("handler" not in t for t in sent["tools"]) and "tool_choice" not in sent


def test_e1_parallel_calls_get_one_user_message_with_every_result_in_order():
    CALLS.clear()
    content = [text("Checking both."), {"type": "server_tool_use", "id": "srvtoolu_1", "name": "web_search", "input": {"query": "x"}},
               tool_use("tu_a", "get_time", city="Rome"), tool_use("tu_b", "get_weather", city="Rome")]
    model = Scripted(reply(content, "tool_use"), reply([text("done")]))
    run_agent(model, tools(), "Rome?")
    second = model.seen[1]["messages"] if len(model.seen) > 1 else []
    assert [m["role"] for m in second] == ["user", "assistant", "user"]
    assert second[1]["content"] == content
    assert [(r["type"], r["tool_use_id"], r["content"]) for r in second[2]["content"]] == [
        ("tool_result", "tu_a", "Rome: 14:05"), ("tool_result", "tu_b", "Rome: 18 C")]
    assert [name for name, _ in CALLS] == ["get_time", "get_weather"]


def test_e2_a_failing_unknown_or_malformed_call_becomes_an_error_result_and_the_loop_goes_on():
    CALLS.clear()
    calls = [tool_use("t1", "broken", city="Oslo"), tool_use("t2", "teleport", city="Oslo"), tool_use("t3", "get_weather"), tool_use("t4", "get_time", city="Oslo")]
    model = Scripted(reply(calls, "tool_use"), reply([text("recovered")]))
    result = run_agent(model, tools(), "go") or {}
    assert result.get("status") == "done" and result.get("text") == "recovered"
    results = model.seen[1]["messages"][2]["content"] if len(model.seen) > 1 else []
    assert [(r["tool_use_id"], r.get("is_error", False)) for r in results] == [("t1", True), ("t2", True), ("t3", True), ("t4", False)]
    assert "weather service is down" in results[0]["content"]
    assert "teleport" in results[1]["content"] and "city" in results[2]["content"]
    assert CALLS == [("get_time", {"city": "Oslo"})]


def test_e3_the_number_of_turns_is_bounded():
    loop = [reply([tool_use(f"tu_{i}", "get_time", city="Oslo")], "tool_use") for i in range(10)]
    model = Scripted(*loop)
    result = run_agent(model, tools(), "go", max_turns=3) or {}
    assert (result.get("status"), result.get("turns")) == ("max_turns", 3)
    assert len(model.seen) == 3


def test_e4_refusal_and_truncation_end_the_loop_and_a_paused_turn_continues():
    refused = run_agent(Scripted(reply([text("I can't help.")], "refusal")), tools(), "go") or {}
    assert (refused.get("status"), refused.get("turns")) == ("refused", 1)
    cut = run_agent(Scripted(reply([text("The answer is")], "max_tokens")), tools(), "go") or {}
    assert (cut.get("status"), cut.get("text")) == ("truncated", "The answer is")
    paused = [{"type": "server_tool_use", "id": "srvtoolu_1", "name": "web_search", "input": {"query": "x"}}]
    model = Scripted(reply(paused, "pause_turn"), reply([text("found it")]))
    result = run_agent(model, tools(), "search") or {}
    assert (result.get("status"), result.get("text"), result.get("turns")) == ("done", "found it", 2)
    again = model.seen[1]["messages"] if len(model.seen) > 1 else []
    assert [m["role"] for m in again] == ["user", "assistant"] and again[1]["content"] == paused


def test_e5_tool_choice_is_validated_for_the_model_and_a_forced_choice_applies_to_the_first_request_only():
    for choice in ({"type": "any"}, {"type": "tool", "name": "get_time"}):
        for model_id in ("claude-sonnet-5-5", "claude-opus-5-5", "claude-fable-5-1"):
            assert failure_of(lambda: run_agent(Scripted(reply([text("x")])), tools(), "go", model=model_id, tool_choice=choice)) == "tool_choice", (choice, model_id)
    assert failure_of(lambda: run_agent(Scripted(reply([text("x")])), tools(), "go", tool_choice={"type": "maybe"})) == "tool_choice.type"
    assert failure_of(lambda: run_agent(Scripted(reply([text("x")])), tools(), "go", model="claude-opus-5", tool_choice={"type": "tool", "name": "nope"})) == "tool_choice.name"
    assert failure_of(lambda: run_agent(Scripted(reply([text("x")])), tools(), "go", tool_choice={"type": "auto", "disable_parallel_tool_use": "yes"})) == "tool_choice.disable_parallel_tool_use"
    model = Scripted(reply([tool_use("tu_1", "get_time", city="Oslo")], "tool_use"), reply([text("ok")]))
    result = run_agent(model, tools(), "go", model="claude-opus-5", tool_choice={"type": "tool", "name": "get_time"}) or {}
    assert result.get("status") == "done"
    assert [r.get("tool_choice") for r in model.seen] == [{"type": "tool", "name": "get_time"}, {"type": "auto"}]
    auto = Scripted(reply([tool_use("tu_1", "get_time", city="Oslo")], "tool_use"), reply([text("ok")]))
    run_agent(auto, tools(), "go", tool_choice={"type": "auto", "disable_parallel_tool_use": True})
    assert [r.get("tool_choice") for r in auto.seen] == [{"type": "auto", "disable_parallel_tool_use": True}] * 2
    none = Scripted(reply([text("ok")]))
    run_agent(none, tools(), "go", tool_choice={"type": "none"})
    assert none.seen[0].get("tool_choice") == {"type": "none"}


def test_e6_results_that_are_not_text_are_sent_as_json_text():
    model = Scripted(reply([tool_use("tu_1", "structured", city="Oslo")], "tool_use"), reply([text("ok")]))
    run_agent(model, tools(), "go")
    results = model.seen[1]["messages"][2]["content"] if len(model.seen) > 1 else []
    content = results[0]["content"] if results else None
    assert isinstance(content, str)
    assert json_or_text(content) == {"city": "Oslo", "temp_c": 18, "tags": ["mild"]}
