import pytest

from cost import MODEL, PARAMS, PRICES, USAGE, cost
from harness import scripted_client
from harness.scripted import message, text


def usage_of(**changes):
    client, _ = scripted_client(message([text("x")], usage={**USAGE, **changes}))
    return client.messages.create(**PARAMS).usage


def test_cost_adds_input_cache_write_and_output():
    # 120*2 + 4000*2*1.25 + 340*10 micro-dollars
    assert cost(MODEL, usage_of()) == pytest.approx((240 + 10000 + 3400) / 1_000_000)


def test_a_cache_read_is_a_fraction_of_the_input_price_that_depends_on_the_model():
    usage = usage_of(cache_read_input_tokens=1_000_000, cache_creation_input_tokens=0, cache_creation={"ephemeral_5m_input_tokens": 0, "ephemeral_1h_input_tokens": 0}, output_tokens=0, input_tokens=0)
    assert cost("claude-sonnet-5-5", usage) == pytest.approx(0.2)
    assert cost("claude-opus-5-5", usage) == pytest.approx(0.2)
    assert cost("claude-fable-5-1", usage) == pytest.approx(0.25)
    assert cost("claude-haiku-4-5-20251001", usage) == pytest.approx(0.1)


def test_batch_halves_the_whole_cost():
    usage = usage_of()
    assert cost(MODEL, usage, batch=True) == pytest.approx(cost(MODEL, usage) / 2)


def test_count_tokens_is_a_separate_free_call_to_its_own_path():
    client, transport = scripted_client({"input_tokens": 99})
    assert client.messages.count_tokens(model=MODEL, messages=PARAMS["messages"]).input_tokens == 99
    assert transport.urls[0].endswith("/v1/messages/count_tokens")
    assert "max_tokens" not in transport.requests[0]
