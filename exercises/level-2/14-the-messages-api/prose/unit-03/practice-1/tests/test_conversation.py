import os
import sys
from pathlib import Path

# The solution is the file beside this test.
sys.path.insert(0, str(Path(__file__).resolve().parent))
from conversation import Conversation


def answer(text, stop="end_turn", usage=(10, 5), blocks=None):
    content = blocks if blocks is not None else [{"type": "text", "text": text}]
    return {"id": "msg_x", "type": "message", "role": "assistant", "model": "claude-sonnet-5-5", "content": content,
            "stop_reason": stop, "stop_sequence": None, "usage": {"input_tokens": usage[0], "output_tokens": usage[1]}}


class Script:
    """A scripted `send`: replies in order (a reply may be an exception to raise); keeps the bodies it was given."""

    def __init__(self, *replies):
        self.replies, self.bodies = list(replies), []

    def __call__(self, body):
        self.bodies.append(body)  # no copy: a client that shares its list with us shows it here
        item = self.replies.pop(0)
        if isinstance(item, Exception):
            raise item
        return item


def error_of(fn):
    try:
        fn()
    except Exception as err:  # noqa: BLE001
        return err
    return None


def test_m1_every_request_carries_the_whole_history_in_order():
    send = Script(answer("Paris."), answer("Since 987."), answer("The Seine."))
    chat = Conversation(send, "claude-sonnet-5-5", 64)
    assert chat.say("Capital of France?").text == "Paris."
    chat.say("Since when?")
    chat.say("Its river?")
    assert [len(b["messages"]) for b in send.bodies] == [1, 3, 5]
    assert [m["role"] for m in send.bodies[2]["messages"]] == ["user", "assistant", "user", "assistant", "user"]
    assert send.bodies[1]["messages"][1] == {"role": "assistant", "content": [{"type": "text", "text": "Paris."}]}
    assert send.bodies[0]["model"] == "claude-sonnet-5-5" and send.bodies[0]["max_tokens"] == 64


def test_e1_usage_adds_up_over_the_turns():
    chat = Conversation(Script(answer("a", usage=(12, 4)), answer("b", usage=(30, 9))), "m", 64)
    chat.say("one")
    chat.say("two")
    assert chat.totals() == {"input_tokens": 42, "output_tokens": 13}


def test_e2_a_failed_call_leaves_no_dangling_user_turn():
    send = Script(answer("ok"), RuntimeError("overloaded"), answer("fine"))
    chat = Conversation(send, "m", 64)
    chat.say("first")
    err = error_of(lambda: chat.say("second"))
    assert isinstance(err, RuntimeError)
    assert [m["role"] for m in chat.history()] == ["user", "assistant"]
    chat.say("second again")
    assert [m["role"] for m in send.bodies[2]["messages"]] == ["user", "assistant", "user"]


def test_e3_stop_reason_is_reported_and_max_tokens_marks_the_reply_truncated():
    chat = Conversation(Script(answer("Complete."), answer("Cut o", stop="max_tokens"),
                               answer("done", stop="stop_sequence")), "m", 64)
    first, second, third = chat.say("a"), chat.say("b"), chat.say("c")
    assert (first.stop_reason, first.truncated) == ("end_turn", False)
    assert (second.stop_reason, second.truncated, second.text) == ("max_tokens", True, "Cut o")
    assert (third.stop_reason, third.truncated) == ("stop_sequence", False)


def test_e4_system_is_a_top_level_field_and_stop_sequences_are_passed_on():
    send = Script(answer("x"), answer("y"))
    with_system = Conversation(send, "m", 8, system="Be brief.", stop_sequences=["END"])
    with_system.say("hi")
    assert len(send.bodies) == 1
    body = send.bodies[0]
    assert body.get("system") == "Be brief." and body.get("stop_sequences") == ["END"]
    assert all(m["role"] != "system" for m in body["messages"])
    Conversation(send, "m", 8).say("hi")
    assert len(send.bodies) == 2 and "system" not in send.bodies[1] and "stop_sequences" not in send.bodies[1]


def test_e5_each_request_is_a_snapshot_and_history_is_a_copy():
    send = Script(answer("a"), answer("b"))
    chat = Conversation(send, "m", 8)
    chat.say("one")
    chat.say("two")
    assert len(send.bodies) == 2 and len(send.bodies[0]["messages"]) == 1  # a later turn must not change an earlier request
    chat.history().append({"role": "user", "content": "injected"})
    chat.history()[0]["content"] = "changed"
    assert len(chat.history()) == 4 and chat.history()[0]["content"] == "one"


def test_e6_a_blank_turn_is_refused_before_anything_is_sent():
    send = Script(answer("never used"))
    chat = Conversation(send, "m", 8)
    for blank in ("", "   ", "\n"):
        assert isinstance(error_of(lambda: chat.say(blank)), ValueError), repr(blank)
    assert send.bodies == [] and chat.history() == []
