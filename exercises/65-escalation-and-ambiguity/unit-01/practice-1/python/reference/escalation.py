"""When a support agent resolves, asks or hands off, and what a hand-off carries. See ../../statement.md."""

import logging

log = logging.getLogger(__name__)


def _decision(action, reason, acknowledge=False):
    return {"action": action, "reason": reason, "acknowledge": acknowledge}


def decide(case, max_attempts=2):
    log.debug("decide input: %r", case)
    if case.get("asked_for_person", False):
        return _decision("escalate", "customer asked for a person")
    if case.get("matches", 1) > 1:
        return _decision("clarify", "ambiguous customer match")
    if not case.get("policy_covers", True):
        return _decision("escalate", "policy does not cover the request")
    if case.get("attempts_without_progress", 0) >= max_attempts:
        return _decision("escalate", "no progress")
    return _decision("resolve", "within capability", case.get("sentiment", "calm") != "calm")


def clarifying_fields(matches):
    if len(matches) < 2:
        return []
    return [field for field in matches[0] if field != "id" and len({m.get(field) for m in matches}) > 1]


def handoff_text(case):
    if not case.get("customer_id") or not case.get("issue"):
        raise ValueError("a hand-off needs a customer id and an issue")
    lines = [
        f"Customer: {case['customer_id']}",
        f"Issue: {case['issue']}",
        f"Root cause: {case.get('root_cause') or 'unknown'}",
        f"Amount: {case.get('amount') or 'unknown'}",
        f"Actions taken: {'; '.join(case.get('actions', [])) or 'none'}",
        f"Recommended action: {case.get('recommended') or 'review the case'}",
    ]
    return "\n".join(lines)
