"""A review specification that cuts false positives: the prompt, the trust in each category and the next step when a request is incomplete. See ../../statement.md."""

import logging

log = logging.getLogger(__name__)

VAGUE = ("be conservative", "high-confidence", "high confidence", "only important", "only significant", "if you are sure", "when you are sure", "use your judgment")


def _present(value):
    return isinstance(value, str) and value.strip() != ""


def _blank(value):
    return value is None or (isinstance(value, str) and value.strip() == "")


def _vague(text):
    # TODO 2 of 7 (finish this to pass e1): the vague check. Receives a criterion's report or skip text. Return the
    #   first phrase of VAGUE that the lower-cased text contains, or nothing when it contains none. Example: "Be
    #   conservative here" -> "be conservative".
    return None


def build_review_prompt(spec, diff):
    log.debug("build_review_prompt input: %r", spec)
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
        # TODO 3 of 7 (finish this to pass e2): the severity check. For each of the levels high and low, refuse the
        #   criterion when its severity has no concrete example text for that level. Example: severity {high: "..."} with
        #   no low -> refused.
        severity = {"high": severity.get("high", ""), "low": severity.get("low", "")}
        ids.append(c["id"])
        blocks.append(f'<criterion id="{c["id"]}">\nReport: {c["report"]}\nSkip: {c["skip"]}\nSeverity high: {severity["high"]}\nSeverity low: {severity["low"]}\n</criterion>')
    # TODO 4 of 7 (finish this to pass e3): the examples check. Refuse the specification unless it has two to four
    #   examples and their verdicts are exactly report and skip (at least one of each, nothing else). Example: three
    #   report examples and no skip -> refused.
    shown = []
    for e in examples:
        if not _present(e.get("reason")):
            raise ValueError("every example needs a reason")
        if e["verdict"] == "report" and e.get("category") not in ids:
            raise ValueError("a report example names one of the criteria")
        tag = f'verdict="report" category="{e["category"]}"' if e["verdict"] == "report" else 'verdict="skip"'
        shown.append(f'<example {tag}>\n<code>{e["code"]}</code>\n<reason>{e["reason"]}</reason>\n</example>')
    # TODO 1 of 7 (finish this to pass m1): the end of the prompt. After the criteria and examples blocks, add the diff
    #   in a <diff> block and return the whole prompt joined with newlines, so that the diff comes last. Example:
    #   criteria, examples, then <diff>, the diff, </diff>.
    return diff


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
        # TODO 6 of 7 (finish this to pass e5): the top dismissed patterns. Receives the count of dismissals per
        #   detected pattern. Return up to three [pattern, count] pairs, ordered by count (highest first) and then by
        #   pattern name. Example: {a: 2, b: 3, c: 2, d: 1} -> [b 3], [a 2], [c 2].
        top = list(counts.items())
        precision = round(accepted / len(items), 2)
        # TODO 5 of 7 (finish this to pass e4): the disable flag of a category. Receives the number reviewed, the
        #   precision, min_reviewed and min_precision. A category is disabled when it has at least min_reviewed reviews
        #   and a precision below min_precision. Example: 5 reviews, precision 0.4, defaults -> disabled; 4 reviews ->
        #   not.
        categories[name] = {"reviewed": len(items), "precision": precision, "disable": False, "top_dismissed": [[pattern, count] for pattern, count in top]}
    return {"categories": categories, "disable": sorted(n for n, c in categories.items() if c["disable"])}


def next_step(request, required, defaults, attended):
    missing = [f for f in required if _blank(request.get(f))]
    assumptions = {f: defaults[f] for f in missing if f in defaults}
    unresolved = [f for f in missing if f not in defaults]
    # TODO 7 of 7 (finish this to pass e6, e7): the next step. `unresolved` holds the missing fields that have no
    #   default. When some are unresolved and the run is attended, return action ask with those fields in `ask`; when
    #   unattended, return action stop with an empty `ask`. Both carry the assumptions. Example: attended, unresolved
    #   [repo] -> ask [repo].
    return {"action": "proceed", "ask": [], "assumptions": assumptions}
