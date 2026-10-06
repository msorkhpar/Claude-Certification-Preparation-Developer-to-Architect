"""Human review without fooling yourself: accuracy by segment, the decision to automate, a calibrated confidence threshold, a stratified sample, review routing within capacity, and checkpoints for irreversible actions. See ../../statement.md."""

import logging

log = logging.getLogger(__name__)

IRREVERSIBLE = ("delete_records", "send_payment", "close_account")


def _percent(correct, total):
    return (200 * correct + total) // (2 * total)


def accuracy_by(records):
    log.debug("accuracy_by input: %r", records)
    groups = {}
    for r in records:
        c, t = groups.get(f"{r['doc_type']}/{r['field']}", (0, 0))
        groups[f"{r['doc_type']}/{r['field']}"] = (c + (1 if r["correct"] else 0), t + 1)
    correct = sum(c for c, _ in groups.values())
    total = sum(t for _, t in groups.values())
    out = [{"segment": "overall", "correct": correct, "total": total, "percent": _percent(correct, total) if total else 0}]
    # TODO 1 of 7 (finish this to pass m1, e1): the segment rows. After the overall row, add one row for each segment
    #   ("doc_type/field"), sorted by name, with its correct count, its total and the rounded percent. Example:
    #   invoice/total 8 of 10 -> {segment: invoice/total, correct 8, total 10, percent 80}.
    for name in sorted(groups):
        c, t = groups[name]
        out.append({"segment": name, "correct": c, "total": t, "percent": out[0]["percent"]})
    return out


def can_automate(records, threshold, min_n):
    failing, undersampled = [], []
    segments = accuracy_by(records)[1:]
    for s in segments:
        # TODO 2 of 7 (finish this to pass e2): the sort of the segments. For each segment (not the overall row): when
        #   its total is below min_n it is undersampled; otherwise when its percent is below the threshold it is failing.
        #   Example: min_n 5, segment with 4 records -> undersampled; 5 records at 70 percent, threshold 90 -> failing.
        pass
    return {"automate": bool(segments) and not failing and not undersampled, "failing": failing, "undersampled": undersampled}


def calibrate_threshold(labeled, target):
    # TODO 3 of 7 (finish this to pass e3, e4): the threshold. Receives the labelled items (confidence, correct) and the
    #   target precision in percent. Try each distinct confidence from the lowest; return the first for which the items at
    #   or above it are right at least target percent of the time; return none when no level does. Example: target 90 and
    #   no level reaches it -> none.
    return 0


def stratified_sample(items, per_stratum):
    strata = []
    for item in items:
        if item["stratum"] not in strata:
            strata.append(item["stratum"])
    chosen = []
    for stratum in strata:
        members = sorted((i for i in items if i["stratum"] == stratum), key=lambda i: (i["rank"], i["id"]))
        # TODO 4 of 7 (finish this to pass e5): the sample of one stratum. `members` are the stratum's items sorted by
        #   rank then id. Take only the first per_stratum of them and add their ids. Example: 5 items, per_stratum 2 ->
        #   the 2 best ranked.
        chosen += [i["id"] for i in members]
    return chosen


def route(extractions, threshold, capacity):
    # TODO 5 of 7 (finish this to pass e6): the review queue. Keep the extractions that have a conflict or a confidence
    #   below the threshold, ordered with conflicts first, then by confidence (lowest first), then by id. Example:
    #   threshold 80, a conflict, a 60 and a 90 -> the conflict, then the 60.
    candidates = list(extractions)
    queue = [e["id"] for e in candidates]
    flagged = set(queue)
    # TODO 6 of 7 (finish this to pass e7): the capacity cut. Split the queue: the first `capacity` ids go to review,
    #   the others to the backlog, both in order. Example: queue [a, b, c], capacity 2 -> review [a, b], backlog [c].
    return {"review": queue, "backlog": [], "auto": [e["id"] for e in extractions if e["id"] not in flagged]}


def checkpoint(action, amount, limit=1000):
    # TODO 7 of 7 (finish this to pass e8): the checkpoint. Receives the action, the amount and the limit. Return human
    #   when the action is in IRREVERSIBLE or the amount is above the limit, otherwise auto. Example: close_account for 10
    #   -> human.
    return "auto"
