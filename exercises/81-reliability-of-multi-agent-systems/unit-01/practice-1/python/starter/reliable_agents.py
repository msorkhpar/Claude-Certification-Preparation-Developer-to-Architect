"""A multi-agent run that survives injected failures: retries with one key, isolation, a breaker, a fallback and a checkpoint. See ../../statement.md."""
import logging

log = logging.getLogger(__name__)


class Transient(Exception):
    """A failure worth retrying: a timeout, a rate limit, a lost response."""


class Fatal(Exception):
    """A failure that retrying cannot fix."""


def call_once(agents, name, key, inputs):
    """One call to an agent: ("ok", result), ("transient", None) or ("fatal", message). Any other exception is a crash and is not caught."""
    try:
        return "ok", agents[name](key, inputs)
    except Transient:
        return "transient", None
    except Fatal as error:
        return "fatal", str(error)


def breaker_open(consecutive, name, threshold):
    """TODO 1 of 7 (unlocks e4): is the breaker of this agent open?

    Receives the map of consecutive failures per agent, the agent name and the threshold. Returns True when the agent's count has reached the threshold
    (a name that is not in the map has a count of 0). Example: breaker_open({"w": 3}, "w", 3) -> True, breaker_open({}, "w", 3) -> False
    """
    return False


def record_outcome(consecutive, name, ok):
    """TODO 2 of 7 (unlocks e4): update the consecutive-failure count of an agent after a call.

    Receives the map of consecutive failures, the agent name and whether the call succeeded. Sets the count to 0 on a success and adds one on a failure.
    Example: after a failure then a success then a failure, the count is 1
    """
    return None


def attempt(agents, name, key, inputs, calls, task_id, consecutive, attempts, threshold):
    """TODO 3 of 7 (unlocks m1, e1, e2 and e4): call an agent for a task, retrying a transient failure.

    Receives the agents, the agent name, the task's key and inputs, the per-task call counts, the task id, the breaker counts, the attempt limit and the breaker
    threshold. Up to `attempts` times: if the breaker is open return (None, "circuit open") without calling; otherwise add one to `calls[task_id]`, make the
    call with `call_once` (always the same key), record the outcome and return (result, None) on "ok"; return (None, "fatal: <message>") on "fatal" without
    retrying; retry on "transient". After the last attempt return (None, "retries exhausted").
    Example: an agent that fails once with Transient and then answers "ok" -> ("ok", None) with two calls counted
    """
    return None, "retries exhausted"


def missing_dependency(needs, done):
    """TODO 4 of 7 (unlocks e3): the first dependency that did not finish.

    Receives the ids a task needs and the dict of finished results. Returns the first id in `needs` (in order) that is not in `done`, or None when all are.
    Example: missing_dependency(["a", "b"], {"a": "x"}) -> "b"
    """
    return None


def fallback_key(key):
    """TODO 5 of 7 (unlocks e5): the idempotency key for the fallback agent.

    Receives the task's key. Returns it followed by `:fallback`, so that a repeat of the fallback is recognised without colliding with the primary.
    Example: fallback_key("key-s") -> "key-s:fallback"
    """
    return key


def should_resume(tid, store):
    """TODO 6 of 7 (unlocks e6): is this task already checkpointed?

    Receives a task id and the store of checkpointed results. Returns True when the store holds the task, so that no agent is called for it.
    Example: should_resume("a", {"a": "ok"}) -> True
    """
    return False


def checkpoint(store, tid, result, degraded):
    """TODO 7 of 7 (unlocks m1, e5, e6 and e7): write a finished task's result to the store.

    Receives the store, the task id, the result and whether the result is degraded (it came from the fallback). Writes the result to the store unless it is
    degraded, so that a later run tries the primary again. It is called as soon as each task finishes, so a crash later in the run loses nothing finished.
    Example: checkpoint(store, "a", "ok", False) -> store == {"a": "ok"}; with degraded True the store is unchanged
    """
    return None


def run_plan(plan, agents, store, attempts=3, breaker_threshold=3):
    log.debug("run_plan input: %r", plan)
    done, failed, skipped, degraded, resumed, calls = {}, {}, {}, [], [], {}
    consecutive = {}
    for task in plan:
        tid = task["id"]
        calls[tid] = 0
        if should_resume(tid, store):
            done[tid] = store[tid]
            resumed.append(tid)
            continue
        needs = task.get("needs") or []
        missing = missing_dependency(needs, done)
        if missing is not None:
            skipped[tid] = f"dependency failed: {missing}"
            continue
        inputs = {n: done.get(n) for n in needs}
        result, reason = attempt(agents, task["agent"], task["key"], inputs, calls, tid, consecutive, attempts, breaker_threshold)
        if result is None and task.get("fallback"):
            result, _ = attempt(agents, task["fallback"], fallback_key(task["key"]), inputs, calls, tid, consecutive, attempts, breaker_threshold)
            if result is not None:
                done[tid] = result
                degraded.append(tid)
                checkpoint(store, tid, result, True)
                continue
        if result is None:
            failed[tid] = reason
            continue
        done[tid] = result
        checkpoint(store, tid, result, False)
    return {"done": done, "failed": failed, "skipped": skipped, "degraded": degraded, "attempts": calls, "resumed": resumed}
