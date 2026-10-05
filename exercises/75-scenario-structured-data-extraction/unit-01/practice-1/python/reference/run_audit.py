"""Audit an extraction run: the accuracy on every document, the accuracy by kind of document, the failure shapes and the first fix."""


def percent(correct, total):
    """A whole percentage, rounded half up; 0 when there is nothing to divide."""
    return (200 * correct + total) // (2 * total) if total else 0


def audit(runs, policy):
    n = len(runs)
    target, min_n, gap = policy["target"], policy["min_n"], policy["gap"]
    counts = {s: sum(1 for r in runs if r["status"] == s) for s in ("valid", "needs_review", "failed")}
    right = sum(1 for r in runs if r["correct"])
    right_valid = sum(1 for r in runs if r["status"] == "valid" and r["correct"])
    accuracy_all = percent(right, n)
    accuracy_validated = percent(right_valid, counts["valid"])
    segments = []
    for kind in sorted({r["kind"] for r in runs}):
        group = [r for r in runs if r["kind"] == kind]
        ok = sum(1 for r in group if r["correct"])
        segments.append({"kind": kind, "n": len(group), "correct": ok, "percent": percent(ok, len(group)),
                         "automate": len(group) >= min_n and ok * 100 >= target * len(group)})
    invented = sum(1 for r in runs if r["invented"])
    wasted = sum(1 for r in runs if r["retried_absent"])
    unchecked = sum(1 for r in runs if r["status"] == "valid" and not r["sum_ok"])
    overstated = accuracy_validated - accuracy_all > gap
    meets_target = n > 0 and right * 100 >= target * n
    if n == 0:
        first_fix = "none"
    elif invented:
        first_fix = "make_fields_nullable"
    elif wasted:
        first_fix = "stop_retrying_absent"
    elif unchecked:
        first_fix = "add_semantic_checks"
    elif overstated:
        first_fix = "measure_all_documents"
    elif not meets_target:
        first_fix = "improve_weak_segments"
    else:
        first_fix = "none"
    return {"n": n, "valid": counts["valid"], "needs_review": counts["needs_review"], "failed": counts["failed"],
            "accuracy_all": accuracy_all, "accuracy_validated": accuracy_validated, "meets_target": meets_target,
            "segments": segments, "invented": invented, "wasted_retries": wasted, "unchecked_totals": unchecked,
            "overstated": overstated, "first_fix": first_fix}
