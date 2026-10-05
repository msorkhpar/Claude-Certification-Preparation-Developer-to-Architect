# Case lists of module 67-exploring-a-large-codebase: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/67-exploring-a-large-codebase/unit-01/practice-1"] = {
    "name": "recovery", "suite": "RecoveryTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "a finding is recorded once per area and fact in first seen order"),
        ("e1", "edge", "the scratchpad groups findings under their area"),
        ("e2", "edge", "the manifest lists every agent with its state file and status and refuses bad input"),
        ("e3", "edge", "a finished agent with its state file is reused and not run again"),
        ("e4", "edge", "a running or failed agent with a state file is resumed from it"),
        ("e5", "edge", "an agent whose state file is missing is restarted from scratch"),
        ("e6", "edge", "the resume prompt carries the task and the state lines and nothing else"),
        ("e7", "edge", "the compact command names what to keep"),
    ],
    "plants": {
        "wrong-duplicate-findings": (["m1"], "records the same fact twice for one area"),
        "wrong-ungrouped-scratchpad": (["e1"], "repeats an area heading for every finding"),
        "wrong-manifest-unsorted": (["e2"], "lists the agents in the order they were given"),
        "wrong-manifest-unvalidated": (["e2"], "accepts a status the manifest does not know"),
        "wrong-rerun-done": (["e3", "e5"], "resumes an agent that already finished"),
        "wrong-restart-running": (["e4"], "restarts an agent that has a state file to resume from"),
        "wrong-ignore-missing-file": (["e5"], "trusts the manifest without checking that the state file exists"),
        "wrong-no-continue-line": (["e6"], "leaves the instruction to continue out of the prompt"),
        "wrong-compact-without-focus": (["e7"], "compacts without telling the command what to keep"),
        "wrong-reuse-needs-company": (["e3"], "reuses a finished agent only when the manifest lists more than one agent"),
    },
}
