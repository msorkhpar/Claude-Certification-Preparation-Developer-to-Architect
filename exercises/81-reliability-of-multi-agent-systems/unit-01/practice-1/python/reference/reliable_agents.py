"""A multi-agent run that survives injected failures: retries with one key, isolation, a breaker, a fallback and a checkpoint. See ../../statement.md."""


class Transient(Exception):
    """A failure worth retrying: a timeout, a rate limit, a lost response."""


class Fatal(Exception):
    """A failure that retrying cannot fix."""


def run_plan(plan, agents, store, attempts=3, breaker_threshold=3):
    done, failed, skipped, degraded, resumed, calls = {}, {}, {}, [], [], {}
    consecutive = {}

    def attempt(agent_name, key, inputs, task_id):
        """(result, None) on success or (None, reason); every call to an agent is counted for the task."""
        for n in range(1, attempts + 1):
            if consecutive.get(agent_name, 0) >= breaker_threshold:
                return None, "circuit open"
            calls[task_id] += 1
            try:
                result = agents[agent_name](key, inputs)
            except Transient:
                consecutive[agent_name] = consecutive.get(agent_name, 0) + 1
                continue
            except Fatal as error:
                consecutive[agent_name] = consecutive.get(agent_name, 0) + 1
                return None, f"fatal: {error}"
            consecutive[agent_name] = 0
            return result, None
        return None, "retries exhausted"

    for task in plan:
        tid = task["id"]
        calls[tid] = 0
        if tid in store:
            done[tid] = store[tid]
            resumed.append(tid)
            continue
        needs = task.get("needs") or []
        missing = next((n for n in needs if n not in done), None)
        if missing is not None:
            skipped[tid] = f"dependency failed: {missing}"
            continue
        inputs = {n: done[n] for n in needs}
        result, reason = attempt(task["agent"], task["key"], inputs, tid)
        if result is None and task.get("fallback"):
            result, _ = attempt(task["fallback"], task["key"] + ":fallback", inputs, tid)
            if result is not None:
                done[tid] = result
                degraded.append(tid)
                continue
        if result is None:
            failed[tid] = reason
            continue
        done[tid] = result
        store[tid] = result
    return {"done": done, "failed": failed, "skipped": skipped, "degraded": degraded, "attempts": calls, "resumed": resumed}
