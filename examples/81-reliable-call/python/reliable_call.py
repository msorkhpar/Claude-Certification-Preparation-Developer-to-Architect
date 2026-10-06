"""A retried refund that must not pay twice, and a circuit breaker around a failing agent.

Anthropic's multi-agent research write-up (read on 2026-10-04) says it combines "the adaptability of AI agents built on Claude with
deterministic safeguards like retry logic and regular checkpoints". This file shows two such safeguards on scripted failures. A retry is
safe only when the tool it repeats is idempotent: the refund tool records its idempotency key together with its effect, so a second
attempt with the same key returns the first result. A breaker stops calling an agent that keeps failing and lets one probe through after
a cooldown. Nothing is called over a network: the failures are scripted and the clock is a number.
"""
import logging

log = logging.getLogger(__name__)



class Transient(Exception):
    """A failure worth retrying: a timeout, a rate limit, a lost response."""


class Ledger:
    """The refunds actually paid, and the idempotency keys already used."""

    def __init__(self):
        self.paid = []
        self.keys = {}


def refund(ledger, key, order, amount, lose_response):
    """Pays once per key. The response may be lost after the money has moved, which is the dangerous case."""
    log.debug("refund input: %r", key)
    if key is not None and key in ledger.keys:
        return ledger.keys[key]
    ledger.paid.append((order, amount))
    receipt = f"refund-{len(ledger.paid)}"
    if key is not None:
        ledger.keys[key] = receipt
    if lose_response:
        raise Transient("response lost")
    return receipt


def retry(attempt, tries):
    """Calls attempt(n) up to `tries` times while it raises Transient; returns (result, calls made)."""
    for n in range(1, tries + 1):
        try:
            return attempt(n), n
        except Transient:
            if n == tries:
                raise


class Breaker:
    """Closed until `threshold` failures in a row, then open for `cooldown` seconds, then one probe (half open)."""

    def __init__(self, threshold, cooldown):
        self.threshold, self.cooldown = threshold, cooldown
        self.failures, self.opened_at = 0, None

    def state(self, now):
        if self.opened_at is None:
            return "closed"
        return "half-open" if now - self.opened_at >= self.cooldown else "open"

    def allow(self, now):
        return self.state(now) != "open"

    def record(self, ok, now):
        if ok:
            self.failures, self.opened_at = 0, None
        else:
            self.failures += 1
            if self.failures >= self.threshold or self.state(now) == "half-open":
                self.opened_at = now


def main():
    naive = Ledger()
    retry(lambda n: refund(naive, None, "order-7", 40, lose_response=(n == 1)), 2)
    print(f"retry without a key: {len(naive.paid)} refunds paid for one order")
    keyed = Ledger()
    receipt, calls = retry(lambda n: refund(keyed, "order-7:refund", "order-7", 40, lose_response=(n == 1)), 2)
    print(f"retry with a key: {len(keyed.paid)} refund paid for one order, {calls} calls, receipt {receipt}")
    breaker = Breaker(threshold=3, cooldown=30)
    reached = 0
    for now, healthy in [(0, False), (1, False), (2, False), (3, True), (40, True), (41, True)]:
        if not breaker.allow(now):
            print(f"t={now}: breaker {breaker.state(now)}, call refused without reaching the agent")
            continue
        reached += 1
        breaker.record(healthy, now)
        print(f"t={now}: call {'succeeded' if healthy else 'failed'}, breaker {breaker.state(now)}")
    print(f"the agent was reached {reached} times in 6 attempts")


if __name__ == "__main__":
    main()
