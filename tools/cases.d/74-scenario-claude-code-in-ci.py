# Case lists of module 74-scenario-claude-code-in-ci: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/74-scenario-claude-code-in-ci/unit-01/practice-1"] = {
    "name": "ci_setup", "suite": "CiSetupTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "who waits decides between real time and batch"),
        ("e1", "edge", "a review has a pass for each file and then an integration pass"),
        ("e2", "edge", "a review runs in a fresh session and is given the earlier findings"),
        ("e3", "edge", "every claude command is headless json and bounded"),
        ("e4", "edge", "the review can only read"),
        ("e5", "edge", "the criteria name what to report what to skip and an example for each severity"),
        ("e6", "edge", "the test prompt passes the existing tests and says what a useful test is"),
        ("e7", "edge", "no file holds a personal path an address or a key"),
    ],
    "plants": {
        "wrong-review-batch": (["m1"], "runs the blocking merge check as a batch"),
        "wrong-debt-realtime": (["m1"], "runs the overnight report in real time"),
        "wrong-single-pass": (["e1"], "reviews all files in one pass with no integration pass"),
        "wrong-integration-first": (["e1"], "puts the integration pass before the per-file passes"),
        "wrong-shared-session": (["e2"], "runs the review in the session that wrote the code"),
        "wrong-no-prior-findings": (["e2"], "gives a re-run no earlier findings"),
        "wrong-no-print-flag": (["e3"], "leaves the -p flag out of the review command"),
        "wrong-no-schema": (["e3"], "leaves the schema out of the review command"),
        "wrong-unbounded": (["e3"], "leaves the turn limit out of the review command"),
        "wrong-review-bash": (["e4"], "gives the review the Bash tool"),
        "wrong-allowed-edit": (["e4"], "approves the Edit tool in the review command"),
        "wrong-severity-free-text": (["e3"], "makes severity free text in the schema"),
        "wrong-no-suggestion": (["e3"], "stops requiring a suggestion in a finding"),
        "wrong-no-skip-list": (["e5"], "leaves out the list of what to skip"),
        "wrong-no-example": (["e5"], "leaves the high severity without an example"),
        "wrong-vague-criteria": (["e5"], "adds a vague instruction to be conservative"),
        "wrong-prompt-no-existing": (["e6"], "does not pass the existing tests to the test prompt"),
        "wrong-prompt-no-useful": (["e6"], "does not say what a useful test is"),
        "wrong-home-path": (["e7"], "writes a personal home path into the criteria"),
    },
}
