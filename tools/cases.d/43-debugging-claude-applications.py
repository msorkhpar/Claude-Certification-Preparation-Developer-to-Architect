# Case lists of module 43-debugging-claude-applications: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/43-debugging-claude-applications/unit-01/practice-1"] = {
    "name": "diagnose", "suite": "DiagnoseTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "each documented http error maps to a type an origin and a recovery"),
        ("e1", "edge", "a 429 is a rate limit or a spend cap and other statuses fall back by class"),
        ("e2", "edge", "a successful response can still fail by its stop reason"),
        ("e3", "edge", "an empty end turn is the integration when text followed the tool result and the model otherwise"),
        ("e4", "edge", "a parse failure is the integration when a json object is in the text and the model when not"),
        ("e5", "edge", "tool failures split into a model that called a missing tool and our tool that raised"),
        ("e6", "edge", "the first failure names the cause and a later good response marks it recovered"),
        ("e7", "edge", "a dropped connection has no status and belongs to the service side"),
    ],
    "plants": {
        "wrong-retry-400": (["m1"], "retries a malformed request with back-off instead of fixing it"),
        "wrong-413-service": (["m1"], "blames the service for a request that is too large"),
        "wrong-429-always-retry": (["e1"], "treats a spend-cap 429 as an ordinary rate limit"),
        "wrong-spend-400-ignored": (["e1"], "treats a 400 spend-limit message as a malformed request"),
        "wrong-maxtokens-model": (["e2"], "blames the model for a response cut off by the caller's own max_tokens"),
        "wrong-refusal-retry": (["e2"], "retries a refusal with back-off instead of using a fallback model"),
        "wrong-empty-always-model": (["e3"], "blames the model for every empty end turn"),
        "wrong-empty-text-anywhere": (["e3"], "blames the integration whenever text and a tool result are both present, in any order"),
        "wrong-parse-always-model": (["e4"], "blames the model for every parse failure"),
        "wrong-parse-braces-only": (["e4"], "calls any text with braces a JSON object without parsing it"),
        "wrong-unknown-tool-integration": (["e5"], "blames the integration for a tool name the model invented"),
        "wrong-tool-error-flagged": (["e5"], "reports a tool result flagged is_error as a failure of our code"),
        "wrong-last-failure": (["e6"], "reports the last failure in the trace instead of the first"),
        "wrong-recovered-ignored": (["e6"], "never reports a recovery"),
        "wrong-recovered-empty": (["e6"], "counts an empty end turn as a recovery"),
        "wrong-network-integration": (["e7"], "blames the integration for a dropped connection"),
    },
}
