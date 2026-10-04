"""Audit a support agent's recorded sessions: the rates, the failure shapes and the first fix."""

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


def audit(sessions):
    n = len(sessions)
    resolved = sum(1 for s in sessions if s["outcome"] == "resolved")
    over = sum(1 for s in sessions if s["outcome"] == "escalated" and not s["needs_human"])
    under = sum(1 for s in sessions if s["outcome"] == "resolved" and s["needs_human"])
    skipped = sum(1 for s in sessions if skipped_prerequisite(s["steps"]))
    wrong = sum(1 for s in sessions if any(st.get("right_tool") and st["tool"] != st["right_tool"] for st in s["steps"]))
    over_limit = sum(1 for s in sessions if s["outcome"] == "resolved" and s["refund_cents"] > s["limit_cents"])
    fcr = round(resolved / n, 3) if n else 0.0
    if skipped or over_limit:
        diagnosis = "enforce_in_code"
    elif wrong and wrong >= over + under:
        diagnosis = "rewrite_tool_descriptions"
    elif over + under:
        diagnosis = "write_escalation_criteria"
    else:
        diagnosis = "none"
    return {"sessions": n, "resolved": resolved, "fcr": fcr, "meets_target": fcr >= TARGET, "over_escalated": over, "under_escalated": under,
            "skipped_prerequisite": skipped, "wrong_tool": wrong, "over_limit_refunds": over_limit, "diagnosis": diagnosis}
