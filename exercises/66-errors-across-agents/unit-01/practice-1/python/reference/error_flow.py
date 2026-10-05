"""How a subagent's failure reaches the coordinator and the report: local recovery, structured error context, valid empty results, and coverage notes. See ../../statement.md."""

import logging

log = logging.getLogger(__name__)

ALTERNATIVES = {
    "timeout": ["retry later", "try a narrower query"],
    "unavailable": ["use a cached source", "try another provider"],
    "permission": ["request access", "use a public source"],
    "invalid_query": ["rewrite the query"],
}
TRANSIENT = ("timeout", "unavailable")


def search_with_recovery(query, call, max_attempts=2):
    log.debug("search_with_recovery input: %r", query)
    attempts = 0
    while True:
        attempts += 1
        reply = call(query, attempts)
        if reply["status"] == "ok":
            items = reply["items"]
            return {"status": "success" if items else "empty", "items": items, "attempts": attempts}
        kind = reply["type"]
        if kind in TRANSIENT and attempts < max_attempts:
            continue
        return {"status": "failed", "failure_type": kind, "attempted": query, "attempts": attempts, "partial_results": reply.get("partial", []), "alternatives": ALTERNATIVES.get(kind, [])}


def coordinator_plan(results):
    plan = []
    for topic, outcome in results.items():
        if outcome["status"] == "success":
            action = "use"
        elif outcome["status"] == "empty":
            action = "no_findings"
        elif outcome["partial_results"]:
            action = "use_partial"
        elif outcome["alternatives"]:
            action = "try_alternative"
        else:
            action = "flag_gap"
        plan.append((topic, action))
    return plan


def coverage_note(results, topics):
    groups = {"Well-supported": [], "Partial": [], "No findings": [], "Gaps": []}
    for topic in topics:
        outcome = results.get(topic)
        if outcome is None:
            groups["Gaps"].append(f"{topic} (not searched)")
        elif outcome["status"] == "success":
            groups["Well-supported"].append(topic)
        elif outcome["status"] == "empty":
            groups["No findings"].append(topic)
        elif outcome["partial_results"]:
            groups["Partial"].append(f"{topic} ({outcome['failure_type']})")
        else:
            groups["Gaps"].append(f"{topic} ({outcome['failure_type']}: {outcome['attempted']})")
    return "\n".join(f"{name}: {', '.join(items)}" for name, items in groups.items() if items)
