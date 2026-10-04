"""A multi-agent run that survives injected failures: retries with one key, isolation, a breaker, a fallback and a checkpoint. See ../../statement.md."""


class Transient(Exception):
    """A failure worth retrying: a timeout, a rate limit, a lost response."""


class Fatal(Exception):
    """A failure that retrying cannot fix."""


def run_plan(plan, agents, store, attempts=3, breaker_threshold=3):
    # TODO: run the tasks in order and return {"done", "failed", "skipped", "degraded", "attempts", "resumed"}.
    return None
