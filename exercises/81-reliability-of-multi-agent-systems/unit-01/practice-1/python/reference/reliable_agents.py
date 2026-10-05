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
    """True when the agent has failed `threshold` calls in a row."""
    return consecutive.get(name, 0) >= threshold


def record_outcome(consecutive, name, ok):
    """A success resets the agent's count to zero; a failure adds one."""
    consecutive[name] = 0 if ok else consecutive.get(name, 0) + 1


def attempt(agents, name, key, inputs, calls, task_id, consecutive, attempts, threshold):
    """(result, None) on success or (None, reason); every call to an agent is counted for the task."""
    for n in range(1, attempts + 1):
        if breaker_open(consecutive, name, threshold):
            return None, "circuit open"
        calls[task_id] += 1
        status, value = call_once(agents, name, key, inputs)
        record_outcome(consecutive, name, status == "ok")
        if status == "ok":
            return value, None
        if status == "fatal":
            return None, f"fatal: {value}"
    return None, "retries exhausted"


def missing_dependency(needs, done):
    """The first id in `needs` that is not done, or None."""
    return next((n for n in needs if n not in done), None)


def fallback_key(key):
    """The key the fallback agent receives."""
    return key + ":fallback"


def should_resume(tid, store):
    """True when the store already holds this task's result."""
    return tid in store


def checkpoint(store, tid, result, degraded):
    """Write a final result to the store as soon as the task finishes; a degraded result is not final."""
    if not degraded:
        store[tid] = result


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
