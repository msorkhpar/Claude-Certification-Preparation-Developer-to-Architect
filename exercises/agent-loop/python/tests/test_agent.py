import pytest
from scripted import ScriptedModel, reply, text, tool_use

from agent import run_agent

TOOLS = {"add": lambda a, b: str(a + b)}


def test_two_turns_returns_final_text():
    m = ScriptedModel(
        reply([text("Adding."), tool_use("tu_1", "add", a=2, b=3)], "tool_use"),
        reply([text("The sum is 5.")], "end_turn"),
    )
    assert run_agent(m, TOOLS, "2+3?") == "The sum is 5."
    assert len(m.seen) == 2


def test_tool_result_is_appended_as_one_user_turn():
    m = ScriptedModel(
        reply([tool_use("tu_1", "add", a=2, b=3), tool_use("tu_2", "add", a=1, b=1)], "tool_use"),
        reply([text("done")], "end_turn"),
    )
    run_agent(m, TOOLS, "go")
    second = m.seen[1]
    assert [t["role"] for t in second] == ["user", "assistant", "user"]
    results = second[2]["content"]
    assert [(r["type"], r["tool_use_id"], r["content"]) for r in results] == [
        ("tool_result", "tu_1", "5"), ("tool_result", "tu_2", "2")]


def test_failing_and_unknown_tools_become_error_results():
    def boom(**_):
        raise ValueError("bad input")

    m = ScriptedModel(
        reply([tool_use("tu_1", "boom"), tool_use("tu_2", "nope")], "tool_use"),
        reply([text("recovered")], "end_turn"),
    )
    assert run_agent(m, {"boom": boom}, "go") == "recovered"
    res = m.seen[1][2]["content"]
    assert res[0]["is_error"] is True and "bad input" in res[0]["content"]
    assert res[1]["is_error"] is True and "nope" in res[1]["content"]


def test_no_tool_use_returns_immediately():
    m = ScriptedModel(reply([text("hi")], "end_turn"))
    assert run_agent(m, TOOLS, "hello") == "hi"
    assert len(m.seen) == 1


def test_turn_cap_raises():
    loop = [reply([tool_use(f"tu_{i}", "add", a=1, b=1)], "tool_use") for i in range(10)]
    with pytest.raises(RuntimeError):
        run_agent(ScriptedModel(*loop), TOOLS, "go", max_turns=3)


def test_text_in_tool_use_turn_does_not_end_the_loop():
    # stop_reason decides, not the presence of text
    m = ScriptedModel(
        reply([text("I will call a tool."), tool_use("tu_1", "add", a=1, b=2)], "tool_use"),
        reply([text("3")], "end_turn"),
    )
    assert run_agent(m, TOOLS, "go") == "3"
