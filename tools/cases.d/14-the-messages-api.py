# Case lists of module 14-the-messages-api: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/14-the-messages-api/unit-01/practice-1"] = {
    "name": "conversation", "suite": "ConversationTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "every request carries the whole history in order"),
        ("e1", "edge", "usage adds up over the turns"),
        ("e2", "edge", "a failed call leaves no dangling user turn"),
        ("e3", "edge", "stop reason is reported and max tokens marks the reply truncated"),
        ("e4", "edge", "system is a top level field and stop sequences are passed on"),
        ("e5", "edge", "each request is a snapshot and history is a copy"),
        ("e6", "edge", "a blank turn is refused before anything is sent"),
    ],
    "plants": {
        "wrong-send-last-only": (["m1"], "sends only the newest user turn, as if the API remembered the rest"),
        "wrong-no-rollback": (["e2"], "keeps the user turn when the call fails, so the next request has two user turns in a row"),
        "wrong-shared-list": (["e5"], "hands its own history list to send, so later turns rewrite earlier requests"),
    },
}
