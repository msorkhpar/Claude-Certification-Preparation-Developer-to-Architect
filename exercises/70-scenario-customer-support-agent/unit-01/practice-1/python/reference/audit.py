"""Audit a support agent's recorded sessions: the rates, the failure shapes and the first fix."""
import logging

log = logging.getLogger(__name__)

PROTECTED = ("lookup_order", "process_refund")
TARGET = 0.8


def skipped_prerequisite(steps):
    """True when an order or refund call came before the first successful get_customer call."""
    verified = False
    for step in steps:
        if step["tool"] == "get_customer" and step["ok"]:
            verified = True
        elif step["tool"] in PROTECTED and not verified:
            return True
    return False


def has_wrong_tool(steps):
    """True when at least one step has a known right tool that differs from the tool used."""
    return any(st.get("right_tool") and st["tool"] != st["right_tool"] for st in steps)


def is_over_limit(session):
    """True when the session was resolved with a refund above its limit."""
    return session["outcome"] == "resolved" and session["refund_cents"] > session["limit_cents"]


def first_contact_rate(resolved, n):
    """The resolved share, rounded to three decimals; 0.0 for no sessions."""
    return round(resolved / n, 3) if n else 0.0


def diagnose(skipped, over_limit, wrong, over, under):
    """The first fix, by the order of the statement."""
    if skipped or over_limit:
        return "enforce_in_code"
    if wrong and wrong >= over + under:
        return "rewrite_tool_descriptions"
    if over + under:
        return "write_escalation_criteria"
    return "none"


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
