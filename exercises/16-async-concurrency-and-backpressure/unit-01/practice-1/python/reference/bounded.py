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
    """Refuse a limit that is not a whole number of at least 1."""
    if not isinstance(limit, int) or limit < 1:
        raise ValueError("limit must be at least 1")


def _next_slot(source, results):
    """Take the next item from the iterator `source` and reserve its slot at the end of `results`."""
    try:
        item = next(source)
    except StopIteration:
        return None
    results.append(None)
    return len(results) - 1, item


def _succeeded(value):
    """The Outcome of an item whose work returned `value`."""
    return Outcome(True, value)


def _failed(error):
    """The Outcome of an item whose work raised `error`."""
    return Outcome(False, error=error)


def _place(results, index, outcome):
    """Store `outcome` in the slot of its item, so the list stays in input order."""
    results[index] = outcome


def _pool(limit, worker):
    """The `limit` worker coroutines that run side by side."""
    return [worker() for _ in range(limit)]


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
