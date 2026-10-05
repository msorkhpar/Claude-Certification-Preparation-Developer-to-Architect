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
        "wrong-assistant-text-only": (["m1"], "stores the assistant turn as plain text instead of the content blocks as received"),
        "wrong-usage-input-overwritten": (["e1"], "replaces the input token total with the last turn instead of adding to it"),
        "wrong-no-rollback": (["e2"], "keeps the user turn when the call fails, so the next request has two user turns in a row"),
        "wrong-truncated-on-any-stop": (["e3"], "marks every reply truncated unless it ended with end_turn"),
        "wrong-stop-sequences-dropped": (["e4"], "does not pass the stop sequences on"),
        "wrong-history-not-copied": (["e5"], "hands out its own history list instead of a copy"),
        "wrong-blank-turn-accepted": (["e6"], "sends a turn that is empty or only whitespace"),
    },
}
