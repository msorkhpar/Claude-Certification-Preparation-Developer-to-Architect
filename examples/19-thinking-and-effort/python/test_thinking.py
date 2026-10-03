from harness import scripted_client
from harness.scripted import message, text
from thinking import THINKING_BLOCK, USAGE, request


def test_the_request_carries_adaptive_thinking_and_effort_in_output_config():
    client, transport = scripted_client(message([text("x")]))
    request(client, "medium")
    sent = transport.requests[0]
    assert sent["thinking"] == {"type": "adaptive"}
    assert sent["output_config"] == {"effort": "medium"}
    assert "budget_tokens" not in str(sent) and "temperature" not in sent


def test_a_thinking_block_can_be_empty_and_billed_tokens_exceed_visible_ones():
    client, _ = scripted_client(message([THINKING_BLOCK, text("A")], usage=USAGE))
    reply = request(client, "high")
    assert reply.content[0].type == "thinking" and reply.content[0].thinking == ""
    assert reply.usage.model_extra["output_tokens_details"]["thinking_tokens"] == 1650
    assert reply.usage.output_tokens == 1900


def test_a_turn_may_have_no_thinking_block_at_all():
    client, _ = scripted_client(message([text("B.")]))
    assert [b.type for b in request(client, "low").content] == ["text"]
