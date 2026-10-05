# Case lists of module 29-context-engineering: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/29-context-engineering/unit-01/practice-1"] = {
    "name": "context", "suite": "ContextTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "an over budget conversation becomes a summary and the newest turn"),
        ("e1", "edge", "old tool results are cleared but their calls and flags stay"),
        ("e2", "edge", "the window drops whole turns and never splits a tool call from its result"),
        ("e3", "edge", "a conversation within budget or with nothing older is left alone"),
        ("e4", "edge", "a second compaction folds the earlier summary into the new one"),
        ("e5", "edge", "a citation that does not match its document is reported"),
        ("e6", "edge", "footnotes number each distinct source once in order of appearance"),
    ],
    "plants": {
        "wrong-clear-all": (["e1"], "clears every tool result, the newest ones too"),
        "wrong-clear-excluded": (["e1"], "clears the results of tools that were to be kept"),
        "wrong-window-splits-pair": (["e2"], "drops single messages, so a tool result can lose its call"),
        "wrong-summarise-under-budget": (["e3"], "summarises a conversation that already fits"),
        "wrong-skips-old-summary": (["e4"], "leaves the earlier summary out of what the summariser reads"),
        "wrong-cited-end-inclusive": (["e5"], "reads the end of a cited span as inclusive"),
        "wrong-footnote-duplicates": (["e6"], "gives a source that is cited twice a second number"),
    },
}
