"""A review specification that cuts false positives: the prompt, the trust in each category and the next step when a request is incomplete. See ../../statement.md."""

VAGUE = ("be conservative", "high-confidence", "high confidence", "only important", "only significant", "if you are sure", "when you are sure", "use your judgment")


def _present(value):
    return isinstance(value, str) and value.strip() != ""


def _blank(value):
    return value is None or (isinstance(value, str) and value.strip() == "")


def _vague(text):
    lowered = text.lower()
    return next((phrase for phrase in VAGUE if phrase in lowered), None)


def build_review_prompt(spec, diff):
    criteria = spec.get("criteria") or []
    examples = spec.get("examples") or []
    if not criteria:
        raise ValueError("at least one criterion is required")
    ids = []
    blocks = []
    for c in criteria:
        for key in ("report", "skip"):
            if not _present(c.get(key)):
                raise ValueError(f"criterion {c.get('id')}: {key} is required")
            phrase = _vague(c[key])
            if phrase:
                raise ValueError(f"criterion {c.get('id')}: {key} is vague ({phrase!r}): name the pattern instead")
        severity = c.get("severity") or {}
        for level in ("high", "low"):
            if not _present(severity.get(level)):
                raise ValueError(f"criterion {c.get('id')}: severity {level} needs a concrete example")
        ids.append(c["id"])
        blocks.append(f'<criterion id="{c["id"]}">\nReport: {c["report"]}\nSkip: {c["skip"]}\nSeverity high: {severity["high"]}\nSeverity low: {severity["low"]}\n</criterion>')
    if not 2 <= len(examples) <= 4:
        raise ValueError("use two to four examples")
    if {e.get("verdict") for e in examples} != {"report", "skip"}:
        raise ValueError("the examples need at least one report and one skip, and no other verdict")
    shown = []
    for e in examples:
        if not _present(e.get("reason")):
            raise ValueError("every example needs a reason")
        if e["verdict"] == "report" and e.get("category") not in ids:
            raise ValueError("a report example names one of the criteria")
        tag = f'verdict="report" category="{e["category"]}"' if e["verdict"] == "report" else 'verdict="skip"'
        shown.append(f'<example {tag}>\n<code>{e["code"]}</code>\n<reason>{e["reason"]}</reason>\n</example>')
    return "\n".join(["<criteria>", *blocks, "</criteria>", "<examples>", *shown, "</examples>", "<diff>", diff, "</diff>"])


def category_report(findings, min_reviewed=5, min_precision=0.5):
    by_category = {}
    for f in findings:
        by_category.setdefault(f["category"], []).append(f)
    categories = {}
    for name, items in by_category.items():
        accepted = sum(1 for f in items if f["verdict"] == "accepted")
        counts = {}
        for f in items:
            if f["verdict"] == "dismissed":
                counts[f["detected_pattern"]] = counts.get(f["detected_pattern"], 0) + 1
        top = sorted(counts.items(), key=lambda kv: (-kv[1], kv[0]))[:3]
        precision = round(accepted / len(items), 2)
        categories[name] = {"reviewed": len(items), "precision": precision, "disable": len(items) >= min_reviewed and precision < min_precision,
                            "top_dismissed": [[pattern, count] for pattern, count in top]}
    return {"categories": categories, "disable": sorted(n for n, c in categories.items() if c["disable"])}


def next_step(request, required, defaults, attended):
    missing = [f for f in required if _blank(request.get(f))]
    assumptions = {f: defaults[f] for f in missing if f in defaults}
    unresolved = [f for f in missing if f not in defaults]
    if unresolved and attended:
        return {"action": "ask", "ask": unresolved, "assumptions": assumptions}
    if unresolved:
        return {"action": "stop", "ask": [], "assumptions": assumptions}
    return {"action": "proceed", "ask": [], "assumptions": assumptions}
