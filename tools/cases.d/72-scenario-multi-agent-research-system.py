# Case lists of module 72-scenario-multi-agent-research-system: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/72-scenario-multi-agent-research-system/unit-01/practice-1"] = {
    "name": "synthesis", "suite": "SynthesisTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "a full run with agreement returns claims with every source and a complete status"),
        ("e1", "edge", "no results leave every scope a gap and say not researched"),
        ("e2", "edge", "a scope the plan never covered and a scope whose search failed read differently in the note"),
        ("e3", "edge", "two values for one claim are a conflict that names both sources and no claim"),
        ("e4", "edge", "partial results are kept and flagged but do not cover the scope and a retry that worked clears the error"),
        ("e5", "edge", "a result with no findings does not cover its scope"),
        ("e6", "edge", "claims sources and scopes come out in a fixed order without duplicates"),
    ],
    "plants": {
        "wrong-complete-by-errors": (["e1", "e5"], "calls the run complete when there are no errors, although a scope was never researched"),
        "wrong-conflict-first-wins": (["e3"], "keeps the first value of a claim and drops the other source"),
        "wrong-partial-covers": (["e4"], "counts a scope with only partial results as covered"),
        "wrong-empty-covers": (["e5"], "counts a scope whose result holds no findings as covered"),
        "wrong-partial-dropped": (["e4"], "drops the partial findings of a failed search"),
        "wrong-resolved-error-kept": (["e4"], "keeps the error of a scope that a later result covered"),
        "wrong-sources-duplicated": (["e6"], "lists the same source twice for one claim"),
        "wrong-claims-unsorted": (["e6"], "returns the claims in the order they arrived"),
        "wrong-note-silent": (["e1", "e2", "e5"], "writes an all-clear note whatever is missing"),
        "wrong-partial-flag-any": (["e4"], "flags a claim as partial when any of its findings was partial"),
    },
}
