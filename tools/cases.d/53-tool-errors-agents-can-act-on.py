# Case lists of module 53-tool-errors-agents-can-act-on: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/53-tool-errors-agents-can-act-on/unit-01/practice-1"] = {
    "name": "errors", "suite": "ErrorsTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "a failed call becomes a structured error with a category a retry flag and an error flag"),
        ("e1", "edge", "a generic message or an unknown category is refused"),
        ("e2", "edge", "only transient failures are retried with growing waits and the other kinds return at once"),
        ("e3", "edge", "the retries are bounded and a wait the service asks for is honoured"),
        ("e4", "edge", "a valid empty result is a success and not an error"),
        ("e5", "edge", "a timeout on a write is an unknown outcome and is retried only when repeating it is safe"),
        ("e6", "edge", "the next action follows the category"),
        ("e7", "edge", "an unexpected exception becomes an internal error and the run goes on"),
    ],
    "plants": {
        "wrong-flag-missing": (["m1"], "reports a failed call as a tool result without the error flag"),
        "wrong-business-retryable": (["m1"], "marks a business rule violation as worth retrying"),
        "wrong-generic-message": (["e1"], "accepts a message such as Operation failed"),
        "wrong-unknown-kind-accepted": (["e1"], "accepts a category that is not one of the six"),
        "wrong-retries-validation": (["e2"], "retries a call that failed validation"),
        "wrong-flat-wait": (["e2"], "waits the same time before every retry"),
        "wrong-extra-retry": (["e3"], "makes one attempt more than the retry limit allows"),
        "wrong-ignores-retry-after": (["e3"], "sleeps its own backoff when the service named a wait"),
        "wrong-empty-is-error": (["e4"], "reports a valid empty result as a failure"),
        "wrong-timeout-retried": (["e5"], "retries a timed out write that has no key"),
        "wrong-key-dropped": (["e5"], "leaves the idempotency key out of the arguments it retries with"),
        "wrong-mutates-args": (["e5"], "writes the idempotency key into the caller's own arguments"),
        "wrong-permission-retry-later": (["e6"], "tells the loop to retry later after a permission error"),
        "wrong-internal-retryable": (["e7"], "marks an unexpected failure as retryable"),
    },
}
