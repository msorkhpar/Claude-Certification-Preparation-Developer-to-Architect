# Case lists of module 87-observability-at-scale: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/87-observability-at-scale/unit-01/practice-1"] = {
    "name": "triage", "suite": "TriageTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "every trace gets one reason and a healthy trace is kept by its id"),
        ("e1", "edge", "an error outranks a slow root which outranks retries which outrank a flag"),
        ("e2", "edge", "the root cause is the deepest failing span and its path starts at the root"),
        ("e3", "edge", "a stale or empty retrieval is blamed only when no span failed"),
        ("e4", "edge", "drift reports a move in either direction over the tolerance and never divides by zero"),
        ("e5", "edge", "an alert needs consecutive windows over the threshold and a dip starts the count again"),
        ("e6", "edge", "a log record drops the content fields unless they are allowed by name"),
        ("e7", "edge", "a requests trail joins the events of every component in time order"),
    ],
    "plants": {
        "wrong-error-after-slow": (["e1"], "labels a trace that failed and was slow as slow"),
        "wrong-no-feedback": (["m1"], "drops a trace the user flagged as a bad answer"),
        "wrong-retries-ignored": (["e1"], "does not keep a trace in which one tool was called three times"),
        "wrong-root-reporter": (["e2"], "blames the first failing span, which only reported its child's error"),
        "wrong-stale-first": (["e3"], "blames a stale retrieval even when another span failed"),
        "wrong-drift-up-only": (["e4"], "misses a metric that fell"),
        "wrong-alert-no-reset": (["e5"], "keeps counting windows across a window that was under the threshold"),
        "wrong-redact-keeps-content": (["e6"], "keeps prompts and tool inputs in the log record"),
        "wrong-redact-ignores-allowed": (["e6"], "drops a content field even when it is allowed by name"),
        "wrong-trail-unsorted": (["e7"], "lists a request's events in the order they were received"),
    },
}
