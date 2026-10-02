"""The reader's real SDK code runs unchanged; only the transport is replaced."""
from pathlib import Path

import anthropic
import pytest

from harness import load_exchange
from harness.replay import replay_client
from harness.scripted import message, scripted_client, text, tool_use

EXAMPLE = Path(__file__).resolve().parents[1] / "examples" / "illustrative_exchange.json"
TOOLS = [{"name": "get_weather", "description": "Weather for a city.",
          "input_schema": {"type": "object", "properties": {"city": {"type": "string"}}, "required": ["city"]}}]


def agent_loop(client, user_text):
    """Ordinary SDK code, as a reader would write it: no stand-in anywhere in it."""
    messages = [{"role": "user", "content": user_text}]
    while True:
        resp = client.messages.create(model="claude-sonnet-5-5", max_tokens=256, tools=TOOLS, messages=messages)
        messages.append({"role": "assistant", "content": resp.content})
        if resp.stop_reason != "tool_use":
            return "".join(b.text for b in resp.content if b.type == "text")
        results = [{"type": "tool_result", "tool_use_id": b.id, "content": "4 degrees, clear"}
                   for b in resp.content if b.type == "tool_use"]
        messages.append({"role": "user", "content": results})


def test_scripted_two_turn_loop_through_the_real_sdk():
    client, t = scripted_client(
        message([text("Looking."), tool_use("toolu_1", "get_weather", city="Oslo")], "tool_use"),
        message([text("Clear.")]))
    assert agent_loop(client, "Weather in Oslo?") == "Clear."
    assert len(t.requests) == 2
    assert t.requests[1]["messages"][2]["content"][0]["tool_use_id"] == "toolu_1"
    assert t.requests[0]["tools"][0]["name"] == "get_weather"


def test_scripted_error_status_raises_the_sdks_own_exception():
    client, _ = scripted_client((529, {"type": "error", "error": {"type": "overloaded_error", "message": "Overloaded"}}))
    with pytest.raises(anthropic.APIStatusError) as e:
        client.messages.create(model="claude-sonnet-5-5", max_tokens=8, messages=[{"role": "user", "content": "hi"}])
    assert e.value.status_code == 529


def test_scripted_turn_that_is_not_a_real_response_is_rejected():
    with pytest.raises(Exception):
        message([{"type": "tool_use", "id": "x"}], "tool_use")  # lacks name and input


def test_replay_of_the_illustrative_exchange():
    ex = load_exchange(EXAMPLE)
    assert ex["illustrative"] is True and ex["captured"] is None
    client, t = replay_client(ex)
    assert agent_loop(client, "Weather in Oslo?") == "It is 4 degrees and clear in Oslo."
    assert not t.pending


def test_replay_mismatch_fails_loudly():
    client, _ = replay_client(load_exchange(EXAMPLE))
    with pytest.raises(anthropic.BadRequestError) as e:
        client.messages.create(model="claude-haiku-4-5", max_tokens=8, messages=[{"role": "user", "content": "hi"}])
    assert "replay mismatch" in str(e.value)
