import pytest

from reliable_call import Breaker, Ledger, Transient, refund, retry


def test_a_retry_without_a_key_pays_twice():
    ledger = Ledger()
    retry(lambda n: refund(ledger, None, "o1", 10, lose_response=(n == 1)), 2)
    assert len(ledger.paid) == 2


def test_a_retry_with_the_same_key_pays_once_and_returns_the_first_receipt():
    ledger = Ledger()
    receipt, calls = retry(lambda n: refund(ledger, "k", "o1", 10, lose_response=(n == 1)), 2)
    assert (receipt, calls, len(ledger.paid)) == ("refund-1", 2, 1)


def test_a_new_key_is_a_new_refund():
    ledger = Ledger()
    refund(ledger, "a", "o1", 10, False)
    refund(ledger, "b", "o1", 10, False)
    assert len(ledger.paid) == 2


def test_retry_gives_up_after_the_last_try():
    with pytest.raises(Transient):
        retry(lambda n: (_ for _ in ()).throw(Transient("down")), 3)


def test_the_breaker_opens_after_the_threshold_and_probes_after_the_cooldown():
    b = Breaker(3, 30)
    for t in (0, 1, 2):
        b.record(False, t)
    assert b.state(3) == "open" and not b.allow(3)
    assert b.state(32) == "half-open" and b.allow(32)
    b.record(False, 32)
    assert b.state(33) == "open"
    b.record(True, 70)
    assert b.state(71) == "closed"
