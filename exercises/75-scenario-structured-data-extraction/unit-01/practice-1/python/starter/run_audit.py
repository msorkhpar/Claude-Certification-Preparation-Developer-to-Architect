"""Audit an extraction run: the accuracy on every document, the accuracy by kind of document, the failure shapes and the first fix."""
import logging

log = logging.getLogger(__name__)


def percent(correct, total):
    """TODO 1 of 6 (unlocks m1, e1 and e8): a whole percentage, rounded half up.

    Receives the correct count and the total. Returns `(200 * correct + total) // (2 * total)`, and 0 when the total is 0.
    Example: percent(2, 3) -> 67, percent(1, 3) -> 33, percent(0, 0) -> 0
    """
    return 0


def segments_of(runs, policy):
    """TODO 2 of 6 (unlocks m1, e3 and e4): one entry per kind of document.

    Receives the runs and the policy. Returns a list sorted by kind of {kind, n, correct, percent, automate}; `automate` needs at
    least `min_n` documents of the kind and `correct * 100 >= target * n` for it.
    Example: 9 of 10 typed documents with min_n 5 and target 90 -> [{"kind": "typed", "n": 10, "correct": 9, "percent": 90, "automate": True}]
    """
    return [{"kind": "", "n": 0, "correct": 0, "percent": 0, "automate": False}]


def failure_shapes(runs):
    """TODO 3 of 6 (unlocks m1 and e6): count the failure shapes in documents.

    Receives the runs. Returns (invented, wasted_retries, unchecked_totals): documents flagged `invented`, documents flagged
    `retried_absent`, and `valid` documents whose `sum_ok` is false (a document never accepted is not counted).
    Example: one valid document with sum_ok false and one failed document with sum_ok false -> (0, 0, 1)
    """
    return 0, 0, 0


def meets(right, n, target):
    """TODO 4 of 6 (unlocks m1, e1 and e2): does the run meet the target?

    Receives the correct count, the document count and the target in percent. Returns True when there is at least one document
    and `right * 100 >= target * n`. Example: meets(9, 10, 90) -> True, meets(8, 10, 90) -> False, meets(0, 0, 90) -> False
    """
    return False


def is_overstated(accuracy_validated, accuracy_all, gap):
    """TODO 5 of 6 (unlocks m1 and e5): is the figure overstated?

    Receives both accuracies and the gap in points. Returns True only when the validated accuracy exceeds the all-document
    accuracy by more than the gap. Example: overstated(100, 95, 5) -> False, overstated(100, 94, 5) -> True
    """
    return False


def choose_fix(n, invented, wasted, unchecked, overstated, meets_target):
    """TODO 6 of 6 (unlocks m1, e1 and e7): the first fix that applies.

    Receives the document count, the three shape counts, `overstated` and `meets_target`. Returns `none` for an empty run, else the
    first that applies of make_fields_nullable (invented), stop_retrying_absent (wasted), add_semantic_checks (unchecked),
    measure_all_documents (overstated), improve_weak_segments (target not met), and `none`.
    Example: choose_fix(3, 0, 1, 1, False, True) -> "stop_retrying_absent"
    """
    return ""


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
