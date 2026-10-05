# Case lists of module 21-message-batches: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/21-message-batches/unit-01/practice-1"] = {
    "name": "batches", "suite": "BatchesTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "results are matched to requests by custom id not by position"),
        ("e1", "edge", "a custom id is 1 to 64 safe characters and unique"),
        ("e2", "edge", "parameters a batch cannot take are refused"),
        ("e3", "edge", "a big job is cut in order by request count and by size"),
        ("e4", "edge", "invalid requests are fixed and the rest are retried"),
        ("e5", "edge", "a request with no result is missing and a stranger is reported"),
        ("e6", "edge", "only requests that succeeded count toward usage"),
    ],
    "plants": {
        "wrong-by-position": (["m1"], "pairs results with requests by their position in the file"),
        "wrong-duplicate-ids": (["e1"], "accepts a custom id that is used twice"),
        "wrong-allow-stream": (["e2"], "lets stream and speed into a batch"),
        "wrong-count-only": (["e3"], "cuts by request count and ignores the size limit"),
        "wrong-retry-invalid": (["e4"], "retries an invalid request unchanged instead of fixing it"),
        "wrong-missing-ignored": (["e5"], "drops a request that has no result instead of retrying it"),
    },
}
