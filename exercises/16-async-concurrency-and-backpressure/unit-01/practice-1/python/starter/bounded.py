"""Run async work over an iterable with a bound on how much runs at once. See ../../statement.md."""
from dataclasses import dataclass
from typing import Any


@dataclass
class Outcome:
    ok: bool
    value: Any = None
    error: Any = None


async def map_bounded(items, work, limit):
    # TODO: start work(item) for at most `limit` items at a time, pulling items lazily; one Outcome per item, in input order.
    return []
