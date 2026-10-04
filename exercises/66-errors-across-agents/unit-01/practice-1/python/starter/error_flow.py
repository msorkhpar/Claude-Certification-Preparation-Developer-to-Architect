"""How a subagent's failure reaches the coordinator and the report: local recovery, structured error context, valid empty results, and coverage notes. See ../../statement.md."""

ALTERNATIVES = {
    "timeout": ["retry later", "try a narrower query"],
    "unavailable": ["use a cached source", "try another provider"],
    "permission": ["request access", "use a public source"],
    "invalid_query": ["rewrite the query"],
}
TRANSIENT = ("timeout", "unavailable")


def search_with_recovery(query, call, max_attempts=2):
    # TODO: call(query, attempt) until it succeeds, fails for good or the attempts run out; return the outcome dict.
    return None


def coordinator_plan(results):
    # TODO: (topic, action) for every topic of results, in order; the run is never stopped.
    return None


def coverage_note(results, topics):
    # TODO: the coverage annotation for the report: which topics are well supported, partial, without findings or gaps.
    return None
