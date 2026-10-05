# Case lists of module 26-tool-use: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/26-tool-use/unit-01/practice-1"] = {
    "name": "toolloop", "suite": "ToolLoopTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "a tool call is run and its result sent back until the model ends its turn"),
        ("e1", "edge", "parallel calls get one user message with every result in order"),
        ("e2", "edge", "a failing unknown or malformed call becomes an error result and the loop goes on"),
        ("e3", "edge", "the number of turns is bounded"),
        ("e4", "edge", "refusal and truncation end the loop and a paused turn continues"),
        ("e5", "edge", "tool choice is validated for the model and a forced choice applies to the first request only"),
        ("e6", "edge", "results that are not text are sent as json text"),
    ],
    "plants": {
        "wrong-result-per-message": (["e1"], "sends each tool result in a user message of its own"),
        "wrong-swallow-errors": (["e2"], "sends a failing tool's message without the error flag"),
        "wrong-extra-turn": (["e3"], "makes one model call more than the turn limit"),
        "wrong-ignore-stop-reason": (["e4"], "reports a refusal as a cut-off answer"),
        "wrong-forced-every-turn": (["e5"], "repeats a forced tool choice on every request, so the model can never finish"),
        "wrong-forced-on-new-models": (["e5"], "lets a forced tool choice through for models that reject it"),
        "wrong-repr-result": (["e6"], "sends the language's text form of an object instead of JSON"),
    },
}
