"""Audit a support agent's recorded sessions: the rates, the failure shapes and the first fix.

Read statement.md for the fields of a session and of the report, then replace the body of audit().
"""


def audit(sessions):
    return {"sessions": 0, "resolved": 0, "fcr": 0.0, "meets_target": False, "over_escalated": 0, "under_escalated": 0,
            "skipped_prerequisite": 0, "wrong_tool": 0, "over_limit_refunds": 0, "diagnosis": ""}
