# Case lists of module 33-mcp-advanced: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/33-mcp-advanced/unit-01/practice-1"] = {
    "name": "mrtr", "suite": "MrtrTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "a production deploy takes three round trips and keeps no state"),
        ("e1", "edge", "a staging deploy and a status call finish at once and the tool list is cacheable"),
        ("e2", "edge", "the server only asks what the client declared it can answer"),
        ("e3", "edge", "a no or a missing answer is handled without an error"),
        ("e4", "edge", "a state the server did not sign is refused"),
        ("e5", "edge", "a state works only for the same user the same call and before it expires"),
        ("e6", "edge", "a request the server cannot serve is a protocol error with a code"),
        ("e7", "edge", "the state carries the whole context and an answer alone never skips a step"),
    ],
    "plants": {
        "wrong-no-expiry": (["e5"], "accepts a requestState after its expiry time"),
        "wrong-any-principal": (["e5"], "accepts a requestState that was issued to another user"),
        "wrong-no-digest": (["e5"], "accepts a requestState on a call whose arguments differ from the ones it was issued for"),
        "wrong-bad-state-restarts": (["e4"], "silently starts the flow over when the requestState is not one it signed"),
        "wrong-elicit-without-capability": (["e2"], "sends an elicitation request to a client that did not declare the capability"),
        "wrong-decline-continues": (["e3"], "carries on with the deployment after the user declined"),
        "wrong-error-on-missing-input": (["e3"], "answers a retry that lacks the requested input with a protocol error instead of asking again"),
    },
}
