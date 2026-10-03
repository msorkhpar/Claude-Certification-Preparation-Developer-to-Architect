import anthropic
import httpx2

from harness import ScriptedTransport
from conversation import QUESTIONS, REPLIES, run


def drive():
    transport = ScriptedTransport(*REPLIES)
    client = anthropic.Anthropic(api_key="placeholder", max_retries=0, http_client=httpx2.Client(transport=transport))
    results = list(run(client, QUESTIONS))
    return transport, results


def test_every_request_carries_the_whole_history():
    transport, _ = drive()
    assert [len(r["messages"]) for r in transport.requests] == [1, 3, 5]
    assert [m["role"] for m in transport.requests[2]["messages"]] == ["user", "assistant", "user", "assistant", "user"]


def test_assistant_turns_are_sent_back_as_received():
    transport, _ = drive()
    assert transport.requests[1]["messages"][1] == {"role": "assistant", "content": [{"type": "text", "text": "Paris."}]}


def test_usage_adds_up_and_stop_reasons_are_read():
    _, results = drive()
    assert results[-1][1] == {"input": 89, "output": 12}
    assert [r.stop_reason for r, _ in results] == ["end_turn", "max_tokens", "stop_sequence"]


def test_system_is_top_level_never_a_message_role():
    transport, _ = drive()
    assert all(r["system"] and all(m["role"] != "system" for m in r["messages"]) for r in transport.requests)
