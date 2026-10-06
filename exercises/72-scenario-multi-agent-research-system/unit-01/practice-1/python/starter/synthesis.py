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
    """TODO 1 of 7 (unlocks e1, e5 and the coverage of every other case): split the required scopes into covered and gaps.

    Receives the required scopes and the results. Returns (covered, gaps), both in the order of `required`: a scope is covered
    when an `ok` result for it has at least one finding.
    Example: required ["a", "b"], one ok result for "a" with a finding -> (["a"], ["b"])
    """
    return [], []


def sources_of(group):
    """TODO 2 of 7 (unlocks e6): the sources of one claim.

    Receives the group of (finding, partial) pairs of one claim. Returns a list of {"source", "date"} in arrival order, no duplicates.
    Example: findings from s9, s1, s9 -> [{"source": "s9", ...}, {"source": "s1", ...}]
    """
    return []


def observed_values(group):
    """TODO 3 of 7 (unlocks e3): what the sources said about a conflicting claim.

    Receives the group of (finding, partial) pairs of one claim. Returns a list of {"value", "source", "date"} in arrival order, no duplicates.
    Example: s1 says "12", s2 says "14" twice -> [{"value": "12", "source": "s1", ...}, {"value": "14", "source": "s2", ...}]
    """
    return []


def all_partial(group):
    """TODO 4 of 7 (unlocks e4): is every finding of the claim from a partial list?

    Receives the group of (finding, partial) pairs of one claim. Returns True only when all of them are partial.
    Example: [(f, True), (f, False)] -> False
    """
    return False


def unresolved_errors(results, covered):
    """TODO 5 of 7 (unlocks e2 and e4): the errors that nothing made up for.

    Receives the results and the covered scopes. Returns {"scope", "type", "query", "alternatives"} for each `error` result whose scope is not covered.
    Example: an error for "b" while only "a" is covered -> [{"scope": "b", "type": "timeout", "query": "qb", "alternatives": [...]}]
    """
    return []


def partial_scopes(results, gaps):
    """TODO 6 of 7 (unlocks e4): the gaps for which a failed search still returned findings.

    Receives the results and the gaps. Returns the gaps that have an `error` result with a non-empty `partial` list.
    Example: gap "b" with an error that carries one partial finding -> ["b"]
    """
    return []


def coverage_note(gaps, errors, results):
    """TODO 7 of 7 (unlocks e1, e2 and e5): say what the report cannot cover.

    Receives the gaps, the unresolved errors and the results. Returns "all scopes covered" for no gaps; otherwise "not covered: " and
    each gap as "<scope> (<type> on '<query>')" for an unresolved error, "<scope> (not researched)" when no result names the scope,
    or "<scope> (no findings)", joined by ", ".
    Example: gaps ["b", "c"], an error for "b" (timeout on "qb"), no result for "c" -> "not covered: b (timeout on 'qb'), c (not researched)"
    """
    return ""


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
