"""Audit an extraction run: the accuracy on every document, the accuracy by kind of document, the failure shapes and the first fix.

Read statement.md for the fields of a run, of the policy and of the report, then replace the body of audit().
"""


def audit(runs, policy):
    return {"n": 0, "valid": 0, "needs_review": 0, "failed": 0, "accuracy_all": 0, "accuracy_validated": 0, "meets_target": False,
            "segments": [], "invented": 0, "wasted_retries": 0, "unchecked_totals": 0, "overstated": False, "first_fix": ""}
