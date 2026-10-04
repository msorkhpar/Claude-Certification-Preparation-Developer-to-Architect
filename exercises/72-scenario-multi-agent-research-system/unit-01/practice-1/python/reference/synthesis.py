"""The coordinator's last step in a research run: merge what the subagents returned into claims with their sources, conflicts, errors and a coverage note."""


def _same(items, item):
    return items if item in items else items + [item]


def synthesize(required, results):
    ok_scopes = {r["scope"] for r in results if r["status"] == "ok" and r["findings"]}
    covered = [s for s in required if s in ok_scopes]
    gaps = [s for s in required if s not in ok_scopes]
    seen = []
    for r in results:
        if r["status"] == "ok":
            seen += [(f, False) for f in r["findings"]]
        else:
            seen += [(f, True) for f in r["error"]["partial"]]
    by_claim = {}
    for f, partial in seen:
        by_claim.setdefault(f["claim"], []).append((f, partial))
    claims, conflicts = [], []
    for claim in sorted(by_claim):
        group = by_claim[claim]
        values = []
        for f, _ in group:
            values = _same(values, f["value"])
        if len(values) > 1:
            observed = []
            for f, _ in group:
                observed = _same(observed, {"value": f["value"], "source": f["source"], "date": f["date"]})
            conflicts.append({"claim": claim, "values": observed})
        else:
            sources = []
            for f, _ in group:
                sources = _same(sources, {"source": f["source"], "date": f["date"]})
            claims.append({"claim": claim, "value": values[0], "sources": sources, "partial": all(p for _, p in group)})
    errors = [{"scope": r["scope"], "type": r["error"]["type"], "query": r["error"]["query"], "alternatives": r["error"]["alternatives"]}
              for r in results if r["status"] == "error" and r["scope"] not in covered]
    partial = [s for s in gaps if any(r["status"] == "error" and r["scope"] == s and r["error"]["partial"] for r in results)]
    parts = []
    for g in gaps:
        failed = next((e for e in errors if e["scope"] == g), None)
        if failed:
            parts.append(f"{g} ({failed['type']} on '{failed['query']}')")
        elif not any(r["scope"] == g for r in results):
            parts.append(f"{g} (not researched)")
        else:
            parts.append(f"{g} (no findings)")
    return {"status": "partial" if gaps else "complete", "covered": covered, "gaps": gaps, "partial": partial, "claims": claims, "conflicts": conflicts,
            "errors": errors, "note": "not covered: " + ", ".join(parts) if parts else "all scopes covered"}
