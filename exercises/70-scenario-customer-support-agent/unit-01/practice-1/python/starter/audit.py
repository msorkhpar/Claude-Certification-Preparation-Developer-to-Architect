"""Audit a support agent's recorded sessions: the rates, the failure shapes and the first fix."""
import logging

log = logging.getLogger(__name__)

PROTECTED = ("lookup_order", "process_refund")
TARGET = 0.8


def skipped_prerequisite(steps):
    """TODO 1 of 5 (unlocks e2): did an order or refund call come before the first successful get_customer call?

    Receives a session's steps (dicts with tool and ok). Returns True or False; a failed get_customer does not identify anyone.
    Example: [lookup_order, get_customer] -> True, [get_customer, lookup_order] -> False
    """
    return False


def has_wrong_tool(steps):
    """TODO 2 of 5 (unlocks e6): does a step have a known right tool that differs from the tool used?

    Receives a session's steps (a step may lack `right_tool`). Returns True for at least one such step, False otherwise.
    Example: [{"tool": "get_customer", "ok": True, "right_tool": "lookup_order"}] -> True; a step with no right_tool never counts.
    """
    return False


def is_over_limit(session):
    """TODO 3 of 5 (unlocks e3): was a refund above the limit actually made?

    Receives one session. Returns True only when its outcome is `resolved` and `refund_cents` is above `limit_cents`.
    Example: resolved, refund 10001, limit 10000 -> True; escalated with the same refund -> False
    """
    return False


def first_contact_rate(resolved, n):
    """TODO 4 of 5 (unlocks e1 and e5): the first contact rate.

    Receives the resolved count and the session count. Returns resolved / n rounded to three decimals, 0.0 when n is 0.
    Example: first_contact_rate(2, 3) -> 0.667
    """
    return 0.0


def diagnose(skipped, over_limit, wrong, over, under):
    """TODO 5 of 5 (unlocks e4 and e1): the first fix, by the order in the statement.

    Receives the counts of skipped prerequisites, over-limit refunds, wrong-tool sessions, over- and under-escalations.
    Returns `enforce_in_code`, `rewrite_tool_descriptions`, `write_escalation_criteria` or `none`.
    Example: diagnose(0, 0, 2, 1, 0) -> "rewrite_tool_descriptions"
    """
    return ""


def audit(sessions):
    log.debug("audit input: %r", sessions)
    n = len(sessions)
    resolved = sum(1 for s in sessions if s["outcome"] == "resolved")
    over = sum(1 for s in sessions if s["outcome"] == "escalated" and not s["needs_human"])
    under = sum(1 for s in sessions if s["outcome"] == "resolved" and s["needs_human"])
    skipped = sum(1 for s in sessions if skipped_prerequisite(s["steps"]))
    wrong = sum(1 for s in sessions if has_wrong_tool(s["steps"]))
    over_limit = sum(1 for s in sessions if is_over_limit(s))
    fcr = first_contact_rate(resolved, n)
    return {"sessions": n, "resolved": resolved, "fcr": fcr, "meets_target": fcr >= TARGET, "over_escalated": over, "under_escalated": under,
            "skipped_prerequisite": skipped, "wrong_tool": wrong, "over_limit_refunds": over_limit,
            "diagnosis": diagnose(skipped, over_limit, wrong, over, under)}
