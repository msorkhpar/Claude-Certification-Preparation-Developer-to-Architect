# Case lists of module 16-async-concurrency-and-backpressure: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/16-async-concurrency-and-backpressure/unit-01/practice-1"] = {
    "name": "bounded", "suite": "BoundedTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "results come back for every item and never more than limit run at once"),
        ("e1", "edge", "results keep the input order even when later items finish first"),
        ("e2", "edge", "a failing item is reported and the others still finish"),
        ("e3", "edge", "a limit above the item count and an empty input both work"),
        ("e4", "edge", "a limit below one is refused"),
        ("e5", "edge", "items are pulled lazily so a slow consumer holds the producer back"),
    ],
    "plants": {
        "wrong-unbounded": (["m1", "e5"], "starts every item at once, ignoring the limit"),
        "wrong-fail-fast": (["e2"], "lets the first failure abort the whole run"),
        "wrong-completion-order": (["e1"], "returns outcomes in the order the items finished"),
        "wrong-eager-input": (["e5"], "reads the whole input into memory before starting any work"),
        "wrong-large-limit-sequential": (["e3"], "runs one item at a time when the limit is above ten"),
        "wrong-zero-limit-accepted": (["e4"], "accepts a limit of zero instead of refusing it"),
    },
}
