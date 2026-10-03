import anthropic
import pytest

from streaming import FAILING, EVENTS, PARAMS, client_for


def test_text_stream_yields_the_text_deltas_only():
    client, _ = client_for(EVENTS)
    with client.messages.stream(**PARAMS) as stream:
        assert list(stream.text_stream) == ["Let me ", "check."]


def test_final_message_assembles_text_tool_input_and_usage():
    client, _ = client_for(EVENTS)
    with client.messages.stream(**PARAMS) as stream:
        final = stream.get_final_message()
    assert final.stop_reason == "tool_use"
    assert final.content[0].text == "Let me check."
    assert final.content[1].input == {"city": "Paris"}
    assert (final.usage.input_tokens, final.usage.output_tokens) == (52, 38)


def test_an_error_event_after_a_200_raises():
    client, _ = client_for(FAILING)
    with pytest.raises(anthropic.APIStatusError):
        with client.messages.stream(**PARAMS) as stream:
            for _ in stream.text_stream:
                pass
