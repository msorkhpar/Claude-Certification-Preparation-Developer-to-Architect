import asyncio

import anthropic

from bounded import run_all


def test_unbounded_runs_everything_at_once():
    results, peak = asyncio.run(run_all(None))
    assert peak == 12


def test_semaphore_caps_in_flight_requests():
    results, peak = asyncio.run(run_all(4))
    assert peak == 4


def test_one_failure_does_not_cancel_the_others_and_order_is_kept():
    results, _ = asyncio.run(run_all(4))
    assert isinstance(results[6], anthropic.RateLimitError)
    assert [r for i, r in enumerate(results) if i != 6] == [f"label for ticket {n}" for n in range(1, 13) if n != 7]
