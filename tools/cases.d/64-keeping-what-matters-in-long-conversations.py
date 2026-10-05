# Case lists of module 64-keeping-what-matters-in-long-conversations: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/64-keeping-what-matters-in-long-conversations/unit-01/practice-1"] = {
    "name": "context_builder", "suite": "ContextBuilderTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "trimming keeps only the named fields with their exact values in the named order"),
        ("e1", "edge", "a field that the record does not have is skipped"),
        ("e2", "edge", "a newer fact replaces the old one and the old value is kept as history"),
        ("e3", "edge", "an older fact that arrives late does not replace the current one"),
        ("e4", "edge", "the case facts of another customer never enter the context"),
        ("e5", "edge", "the context puts case facts first then the summary then the recent messages"),
        ("e6", "edge", "a summary that loses an exact value is reported"),
        ("e7", "edge", "the window drops the oldest messages and keeps a tool call with its result"),
    ],
    "plants": {
        "wrong-no-trim": (["m1", "e1"], "returns the whole tool record instead of the named fields"),
        "wrong-older-overwrites": (["e3"], "lets a fact that arrives late replace a newer one"),
        "wrong-all-customers": (["e4"], "puts the case facts of every customer into the context"),
        "wrong-facts-last": (["e5"], "places the case facts after the summary and the recent messages"),
        "wrong-pair-split": (["e7"], "keeps a tool result without the call that produced it"),
        "wrong-loose-summary-check": (["e6"], "counts a value as kept when only its first two characters appear"),
    },
}

PRACTICES[f"{X}/64-keeping-what-matters-in-long-conversations/unit-01/practice-1"]["plants"]["wrong-equal-date-ignored"] = (["e2"], "ignores a fact dated the same day as the stored one")
