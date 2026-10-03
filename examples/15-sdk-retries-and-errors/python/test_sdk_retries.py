import anthropic
import pytest

from harness import ScriptedTransport
from harness.scripted import message, text
from sdk_retries import BAD_REQUEST, OVERLOADED, PARAMS, SPEND_CAP, client_for


def test_two_retries_recover_from_two_overloads():
    t = ScriptedTransport(OVERLOADED, OVERLOADED, message([text("Hello.")]))
    assert client_for(t, 2).messages.create(**PARAMS).content[0].text == "Hello."
    assert [h["x-stainless-retry-count"] for h in t.headers] == ["0", "1", "2"]


def test_no_retries_surfaces_the_status_and_request_id():
    t = ScriptedTransport(OVERLOADED, message([text("x")]))
    with pytest.raises(anthropic.APIStatusError) as err:
        client_for(t, 0).messages.create(**PARAMS)
    assert err.value.status_code == 529 and err.value.request_id == "req_illustrative_0529" and len(t.requests) == 1


def test_a_400_is_not_retried():
    t = ScriptedTransport(BAD_REQUEST, message([text("x")]))
    with pytest.raises(anthropic.BadRequestError):
        client_for(t, 2).messages.create(**PARAMS)
    assert len(t.requests) == 1


def test_the_sdk_retries_a_spend_cap_429_although_it_cannot_succeed():
    t = ScriptedTransport(SPEND_CAP, SPEND_CAP, SPEND_CAP)
    with pytest.raises(anthropic.RateLimitError):
        client_for(t, 2).messages.create(**PARAMS)
    assert len(t.requests) == 3
