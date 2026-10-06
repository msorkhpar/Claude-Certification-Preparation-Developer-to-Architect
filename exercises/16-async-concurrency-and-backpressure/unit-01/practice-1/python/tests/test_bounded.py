import asyncio
import os
import sys
from pathlib import Path

# SOLUTION_DIR selects starter, reference or a planted wrong solution.
sys.path.insert(0, os.environ.get("SOLUTION_DIR", str(Path(__file__).resolve().parent.parent / "starter")))
from bounded import map_bounded


def run(coro):
    """asyncio.run, but an exception from the code under test is reported as a failed assertion."""
    try:
        return asyncio.run(coro)
    except Exception as err:  # noqa: BLE001
        raise AssertionError(f"the run raised {type(err).__name__}: {err}") from None


class Probe:
    """An async work function that records how many calls are in flight at once."""

    def __init__(self, delay=0.02):
        self.delay, self.active, self.peak, self.started = delay, 0, 0, 0

    async def __call__(self, item):
        self.started += 1
        self.active += 1
        self.peak = max(self.peak, self.active)
        try:
            await asyncio.sleep(self.delay)
            return item * 2
        finally:
            self.active -= 1


def test_m1_results_come_back_for_every_item_and_never_more_than_limit_run_at_once():
    probe = Probe()
    outcomes = run(map_bounded(range(10), probe, 3))
    assert sorted(o.value for o in outcomes) == [n * 2 for n in range(10)]  # the order is the point of e1 alone
    assert all(o.ok for o in outcomes)
    assert probe.peak == 3
    assert probe.started == 10


def test_e1_results_keep_the_input_order_even_when_later_items_finish_first():
    async def slow_first(item):
        await asyncio.sleep(0.05 if item == 0 else 0.001)
        return item

    outcomes = run(map_bounded(range(5), slow_first, 5))
    assert [o.value for o in outcomes] == [0, 1, 2, 3, 4]


def test_e2_a_failing_item_is_reported_and_the_others_still_finish():
    async def sometimes(item):
        if item == 2:
            raise RuntimeError("boom")
        await asyncio.sleep(0.005)
        return item

    outcomes = run(map_bounded(range(5), sometimes, 2))
    failed = [o for o in outcomes if not o.ok]
    assert len(outcomes) == 5 and len(failed) == 1
    assert isinstance(failed[0].error, RuntimeError) and str(failed[0].error) == "boom"
    assert sorted(o.value for o in outcomes if o.ok) == [0, 1, 3, 4]


def test_e3_a_limit_above_the_item_count_and_an_empty_input_both_work():
    probe = Probe()
    outcomes = run(map_bounded([1, 2, 3], probe, 50))
    assert sorted(o.value for o in outcomes) == [2, 4, 6] and probe.peak == 3
    assert run(map_bounded([], Probe(), 4)) == []


def test_e4_a_limit_below_one_is_refused():
    for bad in (0, -1):
        try:
            asyncio.run(map_bounded([1], Probe(), bad))
        except ValueError:
            continue
        except Exception as err:  # noqa: BLE001
            raise AssertionError(f"limit {bad} raised {type(err).__name__}, want ValueError") from None
        raise AssertionError(f"limit {bad} was accepted")


def test_e5_items_are_pulled_lazily_so_a_slow_consumer_holds_the_producer_back():
    pulled = []

    def source():
        for n in range(20):
            pulled.append(n)
            yield n

    async def scenario():
        gate = asyncio.Event()

        async def blocked(item):
            await gate.wait()
            return item

        task = asyncio.ensure_future(map_bounded(source(), blocked, 2))
        await asyncio.sleep(0.05)
        held = len(pulled)  # every worker is blocked: only `limit` items may have been taken
        gate.set()
        outcomes = await task
        return held, outcomes

    held, outcomes = run(scenario())
    assert held == 2, f"{held} items were pulled while 2 workers were blocked"
    assert sorted(o.value for o in outcomes) == list(range(20))
