"""The coordinator's last step in a research run: merge what the subagents returned into claims with their sources, conflicts, errors and a coverage note."""
import logging

log = logging.getLogger(__name__)


def _same(items, item):
    return items if item in items else items + [item]


def _findings(results):
    """Every finding as (finding, came_from_a_partial_list), in arrival order."""
    seen = []
    for r in results:
        if r["status"] == "ok":
            seen += [(f, False) for f in r["findings"]]
        else:
            seen += [(f, True) for f in r["error"]["partial"]]
    return seen


def covered_scopes(required, results):
    ok_scopes = {r["scope"] for r in results if r["status"] == "ok" and r["findings"]}
    return [s for s in required if s in ok_scopes], [s for s in required if s not in ok_scopes]


def sources_of(group):
    sources = []
    for f, _ in group:
        sources = _same(sources, {"source": f["source"], "date": f["date"]})
    return sources


def observed_values(group):
    observed = []
    for f, _ in group:
        observed = _same(observed, {"value": f["value"], "source": f["source"], "date": f["date"]})
    return observed


def all_partial(group):
    return all(p for _, p in group)


def unresolved_errors(results, covered):
    return [{"scope": r["scope"], "type": r["error"]["type"], "query": r["error"]["query"], "alternatives": r["error"]["alternatives"]}
            for r in results if r["status"] == "error" and r["scope"] not in covered]


def partial_scopes(results, gaps):
    return [s for s in gaps if any(r["status"] == "error" and r["scope"] == s and r["error"]["partial"] for r in results)]


def coverage_note(gaps, errors, results):
    parts = []
    for g in gaps:
        failed = next((e for e in errors if e["scope"] == g), None)
        if failed:
            parts.append(f"{g} ({failed['type']} on '{failed['query']}')")
        elif not any(r["scope"] == g for r in results):
            parts.append(f"{g} (not researched)")
        else:
            parts.append(f"{g} (no findings)")
    return "not covered: " + ", ".join(parts) if parts else "all scopes covered"


def synthesize(required, results):
    log.debug("synthesize input: %r", results)
    covered, gaps = covered_scopes(required, results)
    by_claim = {}
    for f, partial in _findings(results):
        by_claim.setdefault(f["claim"], []).append((f, partial))
    claims, conflicts = [], []
    for claim in sorted(by_claim):
        group = by_claim[claim]
        values = []
        for f, _ in group:
            values = _same(values, f["value"])
        if len(values) > 1:
            conflicts.append({"claim": claim, "values": observed_values(group)})
        else:
            claims.append({"claim": claim, "value": values[0], "sources": sources_of(group), "partial": all_partial(group)})
    errors = unresolved_errors(results, covered)
    return {"status": "partial" if gaps else "complete", "covered": covered, "gaps": gaps, "partial": partial_scopes(results, gaps), "claims": claims,
            "conflicts": conflicts, "errors": errors, "note": coverage_note(gaps, errors, results)}
