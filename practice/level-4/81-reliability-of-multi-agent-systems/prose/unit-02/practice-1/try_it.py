"""Run executes this file. Change the calls to try your code; Submit runs the tests."""
import logging

# Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logging.basicConfig(level=logging.DEBUG, format="%(levelname)s %(message)s")

from reliable_agents import Transient, run_plan

def task(tid, agent="w", needs=(), fallback=None):
    return {"id": tid, "agent": agent, "key": f"key-{tid}", "needs": list(needs), "fallback": fallback}

calls = {"n": 0}

def flaky(key, inputs):
    """A stand-in for a worker agent, like the tests use: its first call is lost, then it answers."""
    calls["n"] += 1
    if calls["n"] == 1:
        raise Transient("lost")
    return key + "|" + ",".join(inputs[k] for k in sorted(inputs))

# Task b needs the result of a; the lost call is retried, the result is checkpointed in the store.
store = {}
result = run_plan([task("a"), task("b", needs=["a"])], {"w": flaky}, store)
print("done:", result["done"])
print("attempts:", result["attempts"])
print("failed:", result["failed"], "| skipped:", result["skipped"])
print("store:", store)
