# Case lists of module 34-workflows-and-agents: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/34-workflows-and-agents/unit-01/practice-1"] = {
    "name": "workflows", "suite": "WorkflowsTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "an orchestrator plans runs a worker per subtask and combines"),
        ("e1", "edge", "the plan is read from prose cleaned capped and replaced when unusable"),
        ("e2", "edge", "one failing worker does not stop the others or the answer"),
        ("e3", "edge", "a draft is revised with the feedback until the judge accepts it"),
        ("e4", "edge", "when the rounds run out the best draft wins and an unreadable judge scores zero"),
        ("e5", "edge", "a label is read from the reply and anything else takes the default route"),
        ("e6", "edge", "the majority answer wins and a tie goes to the one seen first"),
        ("e7", "edge", "a writer that fails ends the loop with the best draft so far"),
    ],
    "plants": {
        "wrong-no-cap": (["e1"], "runs every subtask the planner lists instead of the first max_subtasks"),
        "wrong-stop-at-first-failure": (["e2"], "stops running workers after the first one fails"),
        "wrong-no-feedback": (["e3"], "revises without sending the judge's feedback to the writer"),
        "wrong-last-draft": (["e4"], "returns the last draft instead of the best one when the rounds run out"),
        "wrong-trust-unreadable-judge": (["e4"], "scores a judge reply it cannot read as a pass"),
        "wrong-punctuation-kept": (["e5"], "does not strip punctuation around the label, so a reply of Billing. misses its route"),
        "wrong-tie-goes-last": (["e6"], "breaks a voting tie in favour of the answer seen last"),
        "wrong-error-rounds": (["e7"], "counts the round in which the writer failed as completed"),
    },
}
