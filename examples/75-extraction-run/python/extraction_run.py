"""An extraction run in miniature: a scripted model reads six documents, every record is validated for what a schema cannot check, only the errors a second look can fix
are retried, what the document does not hold goes to a person, and the accuracy is reported on every document and not only on the validated ones.

The model is a table of made-up replies: this example is about what the pipeline does with a record, not about what a model writes. Amounts are in cents. The shapes
(a record, an error with a kind and a field, the status of a document) are this course's design, not an Anthropic interface.
"""
DOCS = {
    "d1": ("typed", "Vendor: Acme Ltd. Lines: 10.00 20.00. Total: 30.00"),
    "d2": ("typed", "Vendor: Borealis Co. Lines: 40.00 5.00. Total: 45.00"),
    "d3": ("scanned", "Lines: 8.00 2.00. Total: 10.00"),
    "d4": ("scanned", "Vendor: Corvid Inc. Lines: 12.00 8.00."),
    "d5": ("handwritten", "Vendor: Dunmore. Lines: 6.00 6.00. Total: 12.50"),
    "d6": ("handwritten", "Lines: 3.00. Total: 3.00"),
}
LABELS = {"d1": ("Acme Ltd", 3000), "d2": ("Borealis Co", 4500), "d3": (None, 1000), "d4": ("Corvid Inc", 2000), "d5": ("Dunmore", 1200), "d6": (None, 300)}


def rec(vendor, lines, total, conflict=False):
    return {"vendor": vendor, "lines": lines, "total": total, "conflict": conflict}


# what the scripted model returns on the first and on the second attempt (a missing second reply repeats the first)
REPLIES = {
    "d1": [rec("Acme Ltd", [1000, 2000], 3000)],
    "d2": [rec("Borealis Co", [4000, 500], 5400), rec("Borealis Co", [4000, 500], 4500)],
    "d3": [rec("Globex", [800, 200], 1000), rec(None, [800, 200], 1000)],
    "d4": [rec("Corvid Inc", [1200, 800], None)],
    "d5": [rec("Dunmore", [600, 600], 1250, conflict=True)],
    "d6": [rec("Hollis", [300], 300)],
}


def validate(record, text):
    """What a schema cannot check: a vendor the document never names, totals that disagree, a required total that is missing."""
    errors = []
    if record["vendor"] is not None and record["vendor"] not in text:
        errors.append(("ungrounded", "vendor"))
    if record["total"] is None:
        errors.append(("absent", "total"))
    elif record["total"] != sum(record["lines"]) and not record["conflict"]:
        errors.append(("semantic", "total"))
    return errors


def extract(doc_id, text):
    """One attempt, then one retry that carries the errors, and only when a second look can fix one. An absent value is never retried."""
    retried, replies = [], REPLIES[doc_id]
    for attempt in (1, 2):
        record = replies[min(attempt, len(replies)) - 1]
        errors = validate(record, text)
        fixable = [e for e in errors if e[0] != "absent"]
        if not fixable:
            return {"id": doc_id, "attempts": attempt, "record": record, "errors": errors, "retried": retried,
                    "status": "needs_review" if errors or record["conflict"] else "valid"}
        retried = sorted({kind for kind, _ in fixable})
    return {"id": doc_id, "attempts": 2, "record": record, "errors": errors, "retried": retried, "status": "failed"}


def percent(correct, total):
    return (200 * correct + total) // (2 * total) if total else 0


def main():
    results = []
    for doc_id, (kind, text) in DOCS.items():
        r = {**extract(doc_id, text), "kind": kind}
        label = LABELS[doc_id]
        r["correct"] = r["status"] == "valid" and (r["record"]["vendor"], r["record"]["total"]) == label
        results.append(r)
        note = f" (retried: {', '.join(r['retried'])})" if r["status"] == "valid" and r["retried"] else ""
        if r["status"] == "needs_review":
            note = " (conflict flagged)" if r["record"]["conflict"] else f" ({r['errors'][0][0]}: {r['errors'][0][1]}, not retried)"
        if r["status"] == "failed":
            note = f" ({', '.join(r['retried'])})"
        print(f"{doc_id} {kind}: {r['status']} after {r['attempts']} attempt{'s' if r['attempts'] > 1 else ''}{note}")
    valid = [r for r in results if r["status"] == "valid"]
    right_valid, right = sum(r["correct"] for r in valid), sum(r["correct"] for r in results)
    print(f"accuracy: validated only {right_valid} of {len(valid)} ({percent(right_valid, len(valid))}%), all documents {right} of {len(results)} ({percent(right, len(results))}%)")
    kinds = list(dict.fromkeys(r["kind"] for r in results))
    parts = [f"{k} {sum(r['correct'] for r in results if r['kind'] == k)}/{sum(1 for r in results if r['kind'] == k)}" for k in kinds]
    print("by kind: " + ", ".join(parts))
    ready = [k for k in kinds if sum(1 for r in results if r["kind"] == k) >= 2 and all(r["correct"] for r in results if r["kind"] == k)]
    print("automate: " + (", ".join(ready) or "none"))


if __name__ == "__main__":
    main()
