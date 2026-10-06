import os
import sys
from pathlib import Path

# The solution is the file beside this test.
sys.path.insert(0, str(Path(__file__).resolve().parent))
from agent import run_agent


def text(t):
    return {"type": "text", "text": t}


def call(id, name, **input):
    return {"type": "tool_use", "id": id, "name": name, "input": input}


def reply(stop_reason, *content):
    return {"stop_reason": stop_reason, "content": list(content)}


class Model:
    """A scripted model: replies in order, or the last one again when `repeat`; it keeps a snapshot of every request."""

    def __init__(self, *replies, repeat=False):
        self.replies, self.repeat, self.seen = list(replies), repeat, []

    def __call__(self, messages):
        self.seen.append(list(messages))
        if len(self.replies) > 1 or not self.repeat:
            if not self.replies:
                raise AssertionError("the loop called the model again after the script ended")
            return self.replies.pop(0)
        return self.replies[0]


class Tools(dict):
    """Handlers that record every call."""

    def __init__(self, **handlers):
        super().__init__({name: self._wrap(name, fn) for name, fn in handlers.items()})
        self.calls = []

    def _wrap(self, name, fn):
        def run(arguments):
            self.calls.append((name, arguments))
            return fn(arguments)
        return run


def lookup(arguments):
    return f"record {arguments['n']}"


def test_m1_a_run_alternates_model_and_tools_until_the_model_ends_its_turn():
    model = Model(reply("tool_use", text("Looking."), call("t1", "lookup", n=1)), reply("end_turn", text("Record 1 found.")))
    tools = Tools(lookup=lookup)
    result = run_agent(model, tools, "find record 1") or {}
    assert (result.get("status"), result.get("text"), result.get("turns")) == ("done", "Record 1 found.", 2)
    assert [m["role"] for m in result.get("messages", [])] == ["user", "assistant", "user", "assistant"]
    assert result["messages"][0] == {"role": "user", "content": "find record 1"}
    assert result["messages"][2]["content"] == [{"type": "tool_result", "tool_use_id": "t1", "content": "record 1"}]
    assert [len(seen) for seen in model.seen] == [1, 3] and tools.calls == [("lookup", {"n": 1})]


def test_e1_the_stop_reason_decides_and_the_words_of_the_text_do_not():
    model = Model(reply("tool_use", text("All done. Saving now."), call("t1", "lookup", n=7)), reply("end_turn", text("Saved.")))
    tools = Tools(lookup=lookup)
    result = run_agent(model, tools, "go") or {}
    assert (result.get("status"), result.get("turns"), tools.calls) == ("done", 2, [("lookup", {"n": 7})])
    announce = Model(reply("end_turn", text("Next I will call the lookup tool.")))
    again = Tools(lookup=lookup)
    ended = run_agent(announce, again, "go") or {}
    assert (ended.get("status"), ended.get("turns"), again.calls, len(announce.seen)) == ("done", 1, [], 1)


def test_e2_every_call_of_a_turn_is_answered_in_one_user_message_in_order():
    first = reply("tool_use", text("Three at once."), call("a", "lookup", n=1), call("b", "lookup", n=2), call("c", "lookup", n=3))
    result = run_agent(Model(first, reply("end_turn", text("ok"))), Tools(lookup=lookup), "go") or {}
    messages = result.get("messages", [])
    assert [m["role"] for m in messages] == ["user", "assistant", "user", "assistant"]
    assert messages[1]["content"] == first["content"]
    assert messages[2]["content"] == [{"type": "tool_result", "tool_use_id": i, "content": f"record {n}"} for i, n in (("a", 1), ("b", 2), ("c", 3))]


def test_e3_a_failing_or_unknown_tool_becomes_an_error_result_and_the_run_goes_on():
    def broken(arguments):
        raise RuntimeError("database offline")

    model = Model(reply("tool_use", call("a", "broken"), call("b", "missing"), call("c", "lookup", n=5)), reply("end_turn", text("Partly done.")))
    tools = Tools(broken=broken, lookup=lookup)
    result = run_agent(model, tools, "go") or {}
    assert result.get("status") == "done"
    assert result["messages"][2]["content"] == [
        {"type": "tool_result", "tool_use_id": "a", "content": "database offline", "is_error": True},
        {"type": "tool_result", "tool_use_id": "b", "content": "Unknown tool: missing", "is_error": True},
        {"type": "tool_result", "tool_use_id": "c", "content": "record 5"}]
    assert [name for name, _ in tools.calls] == ["broken", "lookup"]


def test_e4_the_turn_limit_is_a_backstop_that_ends_only_a_run_the_model_has_not_ended():
    endless = Model(reply("tool_use", call("t", "lookup", n=1)), repeat=True)
    tools = Tools(lookup=lookup)
    result = run_agent(endless, tools, "go", max_turns=3) or {}
    assert (result.get("status"), result.get("turns"), len(endless.seen), len(tools.calls)) == ("max_turns", 3, 3, 3)
    assert result["messages"][-1]["role"] == "user" and len(result["messages"]) == 7
    last = Model(reply("tool_use", call("a", "lookup", n=1)), reply("tool_use", call("b", "lookup", n=2)), reply("end_turn", text("Finished on the last turn.")))
    done = run_agent(last, Tools(lookup=lookup), "go", max_turns=3) or {}
    assert (done.get("status"), done.get("turns"), done.get("text")) == ("done", 3, "Finished on the last turn.")


def test_e5_a_cut_off_or_refused_reply_ends_the_run_with_its_own_status():
    expected = {"max_tokens": "truncated", "refusal": "refused", "stop_sequence": "done", "some_new_reason": "unexpected"}
    for stop_reason, status in expected.items():
        model = Model(reply(stop_reason, text("partial words")))
        result = run_agent(model, Tools(lookup=lookup), "go") or {}
        assert (result.get("status"), result.get("text"), result.get("turns"), len(model.seen)) == (status, "partial words", 1, 1), stop_reason


def test_e6_a_tool_use_reply_without_a_tool_call_is_malformed_and_sends_nothing_more():
    model = Model(reply("tool_use", text("I will call a tool.")))
    result = run_agent(model, Tools(lookup=lookup), "go") or {}
    assert (result.get("status"), result.get("turns"), len(model.seen), len(result.get("messages", []))) == ("malformed", 1, 1, 2)
