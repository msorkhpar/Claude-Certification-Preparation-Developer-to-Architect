"""Run async work over an iterable with a bound on how much runs at once. See ../../statement.md."""
import asyncio
import logging
from dataclasses import dataclass
from typing import Any

log = logging.getLogger(__name__)


@dataclass
class Outcome:
    ok: bool
    value: Any = None
    error: Any = None


def _check_limit(limit):
    """TODO 1 of 6 (finish this to pass e4): refuse a limit that is not a whole number of at least 1.

    Receives the limit. Raises ValueError("limit must be at least 1") for 0, a negative number or a value that is not an int; returns nothing otherwise.
    Example: _check_limit(0) raises ValueError, _check_limit(3) returns None
    """


def _next_slot(source, results):
    """TODO 2 of 6 (finish this to pass e5 and m1): take the next item and reserve its slot.

    Receives the iterator `source` and the list `results`. Takes ONE item from the iterator, appends None to `results` as the slot of that item and
    returns (index of the slot, item); returns None when the iterator is exhausted. Items are taken only here, one at a time.
    Example: with results == [None] and source yielding "b": returns (1, "b") and results becomes [None, None]
    """
    return None


def _succeeded(value):
    """TODO 3 of 6 (finish this to pass m1, e1 and e3): the Outcome of an item whose work returned `value`.

    Example: _succeeded(4) -> Outcome(True, 4)
    """
    return Outcome(False)


def _failed(error):
    """TODO 4 of 6 (finish this to pass e2): the Outcome of an item whose work raised `error`.

    Example: _failed(RuntimeError("boom")) -> Outcome(False, error=RuntimeError("boom"))
    """
    return Outcome(True)


def _place(results, index, outcome):
    """TODO 5 of 6 (finish this to pass e1): store `outcome` in slot `index` of `results`, so the list stays in input order.

    Example: results == [None, None], _place(results, 1, o) makes results [None, o]
    """


def _pool(limit, worker):
    """TODO 6 of 6 (finish this to pass m1 and e5): the `limit` worker coroutines that run side by side.

    Receives the limit and the worker coroutine function. Returns a list of `limit` fresh worker() coroutines.
    Example: _pool(3, worker) -> [worker(), worker(), worker()]
    """
    return []


async def map_bounded(items, work, limit):
    log.debug("map_bounded input: items=%r limit=%r", items, limit)
    _check_limit(limit)
    source = iter(items)
    results = []  # one slot per item, in input order, filled as items finish

    async def worker():
        while True:
            # The next item is taken only when this worker is free: a slow consumer holds the producer back.
            slot = _next_slot(source, results)
            if slot is None:
                return
            index, item = slot
            try:
                outcome = _succeeded(await work(item))
            except Exception as err:  # noqa: BLE001 - one failure must not stop the others
                outcome = _failed(err)
            _place(results, index, outcome)

    await asyncio.gather(*_pool(limit, worker))
    return results
