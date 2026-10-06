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
    for name in sorted(groups):
        c, t = groups[name]
        out.append({"segment": name, "correct": c, "total": t, "percent": _percent(c, t)})
    return out


def can_automate(records, threshold, min_n):
    failing, undersampled = [], []
    segments = accuracy_by(records)[1:]
    for s in segments:
        if s["total"] < min_n:
            undersampled.append(s["segment"])
        elif s["percent"] < threshold:
            failing.append(s["segment"])
    return {"automate": bool(segments) and not failing and not undersampled, "failing": failing, "undersampled": undersampled}


def calibrate_threshold(labeled, target):
    for t in sorted({c for c, _ in labeled}):
        kept = [ok for c, ok in labeled if c >= t]
        if 100 * sum(1 for ok in kept if ok) >= target * len(kept):
            return t
    return None


def stratified_sample(items, per_stratum):
    strata = []
    for item in items:
        if item["stratum"] not in strata:
            strata.append(item["stratum"])
    chosen = []
    for stratum in strata:
        members = sorted((i for i in items if i["stratum"] == stratum), key=lambda i: (i["rank"], i["id"]))
        chosen += [i["id"] for i in members[:per_stratum]]
    return chosen


def route(extractions, threshold, capacity):
    candidates = [e for e in extractions if e["conflict"] or e["confidence"] < threshold]
    candidates.sort(key=lambda e: (0 if e["conflict"] else e["confidence"], e["id"]))
    queue = [e["id"] for e in candidates]
    flagged = set(queue)
    return {"review": queue[:capacity], "backlog": queue[capacity:], "auto": [e["id"] for e in extractions if e["id"] not in flagged]}


def checkpoint(action, amount, limit=1000):
    return "human" if action in IRREVERSIBLE or amount > limit else "auto"
