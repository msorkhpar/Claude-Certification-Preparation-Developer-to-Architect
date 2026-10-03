"""Run async work over an iterable with a bound on how much runs at once. See ../../statement.md."""
import asyncio
from dataclasses import dataclass
from typing import Any


@dataclass
class Outcome:
    ok: bool
    value: Any = None
    error: Any = None


async def map_bounded(items, work, limit):
    if not isinstance(limit, int) or limit < 1:
        raise ValueError("limit must be at least 1")
    source = iter(items)
    results = []  # one slot per item, in input order, filled as items finish

    async def worker():
        while True:
            # The next item is taken only when this worker is free: a slow consumer holds the producer back.
            try:
                item = next(source)
            except StopIteration:
                return
            index = len(results)
            results.append(None)
            try:
                outcome = Outcome(True, await work(item))
            except Exception as err:  # noqa: BLE001 - one failure must not stop the others
                outcome = Outcome(False, error=err)
            results[index] = outcome

    await asyncio.gather(*(worker() for _ in range(limit)))
    return results
