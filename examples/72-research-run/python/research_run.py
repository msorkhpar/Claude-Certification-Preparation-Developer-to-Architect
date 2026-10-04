"""A multi-agent research run in miniature: a coordinator that checks its own decomposition, a search subagent whose failure comes back as structured context,
one retry through an alternative, a synthesis agent with a scoped verification tool, and a report that says what it could not cover.

The subagents are functions over made-up data: this example is about what the coordinator does with what comes back, not about what a model writes. The
shapes (a result with a status, an error with a type, the query, partial results and alternatives) are this course's design, not an Anthropic interface.
"""
REQUIRED = ["visual arts", "music", "writing", "film"]

# what the web search subagent finds for a query: (claim, value, source, date)
SOURCES = {
    "AI in digital art": [("studios using generative tools", "60%", "survey-a", "2025-02-01")],
    "AI in graphic design": [("designers using generative tools weekly", "48%", "survey-b", "2025-03-10")],
    "AI in photography": [("agencies labelling generated images", "yes", "policy-c", "2024-11-20")],
    "AI in music": [("labels licensing their catalogues for training", "3 of 5", "report-d", "2025-01-15")],
    "AI in writing": [("publishers with an AI policy", "70%", "survey-e", "2025-04-02")],
    "AI in film production": [("studios testing AI previsualisation", "4 of 6", "report-f", "2025-05-12")],
}
ALTERNATIVES = {"AI in film": ["AI in film production"]}
PUBLISHED = {"survey-a": "2025-02-01", "report-d": "2025-01-15"}  # what the synthesis agent can check here


def search(query, down=()):
    """The search subagent. A failure is returned with its type, the query, the partial results and what to try instead."""
    if query in down or query not in SOURCES:
        return {"status": "error", "error": {"type": "timeout", "query": query, "partial": [], "alternatives": ALTERNATIVES.get(query, []), "tried": [query]}}
    return {"status": "ok", "findings": SOURCES[query]}


def coverage(plan):
    covered = [s for s in REQUIRED if any(t["scope"] == s for t in plan)]
    return covered, [s for s in REQUIRED if s not in covered]


def replan(plan):
    """The coordinator compares its plan with the scopes the question needs and adds a subtask for each scope the plan leaves out."""
    return plan + [{"scope": s, "query": f"AI in {s}"} for s in coverage(plan)[1]]


def recover(result, down):
    """One retry through an alternative that the error offered. A second failure stays a failure and keeps both queries."""
    error = result.get("error")
    if result["status"] == "ok" or not error["alternatives"]:
        return result
    again = search(error["alternatives"][0], down)
    if again["status"] == "ok":
        return {**again, "recovered_from": error["query"]}
    return {"status": "error", "error": {**error, "tried": error["tried"] + [error["alternatives"][0]]}}


def research(plan, down=()):
    return [{**task, **recover(search(task["query"], down), down)} for task in plan]


def verify_fact(kind, source, value):
    """The synthesis agent's scoped tool: a date it can check here, and anything else goes back to the coordinator."""
    if kind == "date":
        return "confirmed" if PUBLISHED.get(source) == value else "mismatch"
    return "needs_search"


def report(results):
    covered = [s for s in REQUIRED if any(r["scope"] == s and r["status"] == "ok" for r in results)]
    notes = [f"{r['scope']} not covered: timeout on " + " and on ".join(f"'{q}'" for q in r["error"]["tried"]) for r in results if r["status"] == "error"]
    findings = [f for r in results if r["status"] == "ok" for f in r["findings"]]
    return {"status": "complete" if len(covered) == len(REQUIRED) else "partial", "covered": len(covered), "findings": len(findings),
            "errors": len(notes), "notes": notes or ["nothing left uncovered"]}


def main():
    narrow = [{"scope": "visual arts", "query": q} for q in ("AI in digital art", "AI in graphic design", "AI in photography")]
    covered, gaps = coverage(narrow)
    print(f"plan 1: {len(narrow)} subtasks, scopes covered {len(covered)} of {len(REQUIRED)}, gaps: {', '.join(gaps)}")
    plan = replan(narrow)
    covered, gaps = coverage(plan)
    print(f"plan 2: {len(plan)} subtasks, scopes covered {len(covered)} of {len(REQUIRED)}, gaps: {', '.join(gaps) or 'none'}")
    first = search("AI in film")
    e = first["error"]
    print(f"search '{e['query']}': {first['status']} {e['type']}, {len(e['partial'])} partial, alternative '{e['alternatives'][0]}'")
    results = research(plan)
    recovered = next(r for r in results if "recovered_from" in r)
    print(f"recovered: '{recovered['recovered_from']}' -> '{recovered['query']}' scope {recovered['scope']}, {len(recovered['findings'])} finding")
    checks = [("date", "survey-a", "2025-02-01"), ("date", "report-d", "2025-01-15"), ("statistic", "survey-a", "60%")]
    verdicts = [verify_fact(*c) for c in checks]
    print(f"verify_fact: {verdicts.count('confirmed')} confirmed here, {verdicts.count('needs_search')} sent back to the coordinator")
    for label, down in (("all sources up", ()), ("film search down for good", ("AI in film", "AI in film production"))):
        r = report(research(plan, down))
        print(f"report ({label}): status={r['status']}, covered={r['covered']}/{len(REQUIRED)}, findings={r['findings']}, errors={r['errors']}")
        print(f"  note: {'; '.join(r['notes'])}")


if __name__ == "__main__":
    main()
