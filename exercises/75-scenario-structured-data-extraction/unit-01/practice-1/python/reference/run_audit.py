"""Audit an extraction run: the accuracy on every document, the accuracy by kind of document, the failure shapes and the first fix."""
import logging

log = logging.getLogger(__name__)


def percent(correct, total):
    """A whole percentage, rounded half up; 0 when there is nothing to divide."""
    return (200 * correct + total) // (2 * total) if total else 0


def segments_of(runs, policy):
    """One entry per kind, sorted by kind: {kind, n, correct, percent, automate}."""
    result = []
    for kind in sorted({r["kind"] for r in runs}):
        group = [r for r in runs if r["kind"] == kind]
        ok = sum(1 for r in group if r["correct"])
        result.append({"kind": kind, "n": len(group), "correct": ok, "percent": percent(ok, len(group)),
                       "automate": len(group) >= policy["min_n"] and ok * 100 >= policy["target"] * len(group)})
    return result


def failure_shapes(runs):
    """(invented, wasted_retries, unchecked_totals) counted in documents."""
    invented = sum(1 for r in runs if r["invented"])
    wasted = sum(1 for r in runs if r["retried_absent"])
    unchecked = sum(1 for r in runs if r["status"] == "valid" and not r["sum_ok"])
    return invented, wasted, unchecked


def meets(right, n, target):
    """True when the run has a document and right * 100 >= target * n."""
    return n > 0 and right * 100 >= target * n


def is_overstated(accuracy_validated, accuracy_all, gap):
    """True when the validated accuracy exceeds the all-document accuracy by more than the gap."""
    return accuracy_validated - accuracy_all > gap


def choose_fix(n, invented, wasted, unchecked, overstated, meets_target):
    """The first fix that applies, in the order of the statement."""
    if n == 0:
        return "none"
    if invented:
        return "make_fields_nullable"
    if wasted:
        return "stop_retrying_absent"
    if unchecked:
        return "add_semantic_checks"
    if overstated:
        return "measure_all_documents"
    if not meets_target:
        return "improve_weak_segments"
    return "none"


def audit(runs, policy):
    log.debug("audit input: %r", runs)
    n = len(runs)
    counts = {s: sum(1 for r in runs if r["status"] == s) for s in ("valid", "needs_review", "failed")}
    right = sum(1 for r in runs if r["correct"])
    right_valid = sum(1 for r in runs if r["status"] == "valid" and r["correct"])
    accuracy_all = percent(right, n)
    accuracy_validated = percent(right_valid, counts["valid"])
    invented, wasted, unchecked = failure_shapes(runs)
    overstated = is_overstated(accuracy_validated, accuracy_all, policy["gap"])
    meets_target = meets(right, n, policy["target"])
    return {"n": n, "valid": counts["valid"], "needs_review": counts["needs_review"], "failed": counts["failed"],
            "accuracy_all": accuracy_all, "accuracy_validated": accuracy_validated, "meets_target": meets_target,
            "segments": segments_of(runs, policy), "invented": invented, "wasted_retries": wasted, "unchecked_totals": unchecked,
            "overstated": overstated, "first_fix": choose_fix(n, invented, wasted, unchecked, overstated, meets_target)}
