# Case lists of module 69-provenance-and-uncertainty: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/69-provenance-and-uncertainty/unit-01/practice-1"] = {
    "name": "ledger", "suite": "LedgerTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "findings are merged per claim with every source kept once"),
        ("e1", "edge", "a finding without a source or a date is refused"),
        ("e2", "edge", "two values from the same date are a conflict that keeps both"),
        ("e3", "edge", "two values from different dates are a change not a conflict"),
        ("e4", "edge", "the coverage note separates what is well supported from what is not"),
        ("e5", "edge", "a planned claim with no finding is a gap with its reason"),
        ("e6", "edge", "financial data is rendered as a table"),
        ("e7", "edge", "news is rendered as prose that says when sources disagree"),
        ("e8", "edge", "technical findings are a list and an unknown content type is refused"),
    ],
    "plants": {
        "wrong-first-wins": (["e2", "e3"], "keeps only the first value of a claim and drops the others"),
        "wrong-ignores-dates": (["e3", "e4"], "calls every difference a conflict whatever the dates"),
        "wrong-sources-collapsed": (["m1", "e4"], "keeps only the first source of a value"),
        "wrong-date-optional": (["e1"], "does not require a date"),
        "wrong-accepts-incomplete": (["e1"], "merges a finding with a missing field"),
        "wrong-gaps-hidden": (["e5"], "leaves the planned claims with no finding out of the note"),
        "wrong-single-source-supported": (["e4"], "calls a claim with one source well supported"),
        "wrong-unknown-kind-accepted": (["e8"], "renders an unknown content type"),
        "wrong-no-table": (["e6"], "renders financial data as prose"),
        "wrong-conflict-unmarked": (["e7"], "leaves the disagreement out of the prose"),
    },
}
