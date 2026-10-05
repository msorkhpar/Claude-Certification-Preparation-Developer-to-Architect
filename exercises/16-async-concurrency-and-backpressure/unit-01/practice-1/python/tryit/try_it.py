"""Run executes this file. Change the calls to try your code; Submit runs the tests."""
import logging

# Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logging.basicConfig(level=logging.DEBUG, format="%(levelname)s %(message)s")

import asyncio

from bounded import map_bounded

active = {"now": 0, "peak": 0}


async def work(item):
    """A stand-in for one API call: it waits a little and doubles the item, noting how many run at once."""
    active["now"] += 1
    active["peak"] = max(active["peak"], active["now"])
    try:
        await asyncio.sleep(0.02)
        return item * 2
    finally:
        active["now"] -= 1


# Ten items, never more than three at a time.
outcomes = asyncio.run(map_bounded(range(10), work, 3))

print("outcomes:", len(outcomes))
print("values:", sorted(o.value for o in outcomes if o.ok))
print("all ok:", all(o.ok for o in outcomes))
print("peak in flight:", active["peak"])
