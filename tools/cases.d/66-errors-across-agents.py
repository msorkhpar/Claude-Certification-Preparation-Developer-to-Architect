# Case lists of module 66-errors-across-agents: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/66-errors-across-agents/unit-01/practice-1"] = {
    "name": "error_flow", "suite": "ErrorFlowTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "a transient failure is retried locally and the success is reported with its attempts"),
        ("e1", "edge", "a valid empty result is a success with no findings and never an error"),
        ("e2", "edge", "a permission or invalid query error is not retried and carries what was attempted and its alternatives"),
        ("e3", "edge", "a failure that survives the retries carries the partial results of the last attempt"),
        ("e4", "edge", "the coordinator uses partial results tries an alternative or flags a gap and never stops the run"),
        ("e5", "edge", "the coverage note separates supported topics from gaps and names the cause"),
        ("e6", "edge", "a topic with no result is a gap that was not searched"),
    ],
    "plants": {
        "wrong-empty-is-error": (["e1"], "reports a search with no matches as a failure"),
        "wrong-retry-permission": (["e2"], "retries a permission error as if it were transient"),
        "wrong-drop-partial": (["e3"], "throws away the partial results of a failed search"),
        "wrong-generic-error": (["e2", "e3"], "reports only that the search failed, without its type and the query attempted"),
        "wrong-stop-on-failure": (["e4"], "abandons the whole run when one topic failed"),
        "wrong-gap-as-supported": (["e5"], "lists a failed topic among the well-supported ones"),
        "wrong-missing-topic-skipped": (["e6"], "leaves a topic that was never searched out of the note"),
    },
}
