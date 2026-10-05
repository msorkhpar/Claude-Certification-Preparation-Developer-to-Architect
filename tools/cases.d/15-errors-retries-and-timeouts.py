# Case lists of module 15-errors-retries-and-timeouts: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/15-errors-retries-and-timeouts/unit-01/practice-1"] = {
    "name": "retry", "suite": "RetryTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "overloaded twice then success returns the good reply after two waits"),
        ("e1", "edge", "client errors are not retried"),
        ("e2", "edge", "retry after is a floor for the wait"),
        ("e3", "edge", "delay doubles up to the cap and the jitter is applied last"),
        ("e4", "edge", "a spend cap 429 is not retried"),
        ("e5", "edge", "connection errors are retried like server errors"),
        ("e6", "edge", "giving up reports the last reply and does not wait after the last attempt"),
    ],
    "plants": {
        "wrong-retry-everything": (["e1"], "retries every failing status, including 400, 401 and 404"),
        "wrong-ignore-retry-after": (["e2"], "waits by its own schedule and never reads retry-after"),
        "wrong-no-cap": (["e3"], "lets the delay double without limit"),
        "wrong-spend-cap-retried": (["e4"], "retries a 429 that says the spend limit is reached"),
        "wrong-connection-not-retried": (["e5"], "gives up at the first lost connection"),
        "wrong-sleep-after-last": (["e6"], "waits once more after the final failed attempt"),
    },
}
