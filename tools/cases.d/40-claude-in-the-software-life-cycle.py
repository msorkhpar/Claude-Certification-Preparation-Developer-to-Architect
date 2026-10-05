# Case lists of module 40-claude-in-the-software-life-cycle: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/40-claude-in-the-software-life-cycle/unit-01/practice-1"] = {
    "name": "pipeline_setup", "suite": "PipelineSetupTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "the mention workflow answers only claude comments and holds no key"),
        ("e1", "edge", "the review workflow reads the code and posts the review"),
        ("e2", "edge", "each prompt file carries a version that its changelog explains"),
        ("e3", "edge", "the review guidance and the git workflow are files the reviewer and claude read"),
        ("e4", "edge", "every run is bounded by turns time and concurrency"),
        ("e5", "edge", "no file holds a key a personal path or an address"),
    ],
    "plants": {
        "wrong-mention-unguarded": (["m1"], "starts a runner on every comment instead of only on @claude mentions"),
        "wrong-mention-beta": (["m1"], "uses the beta action instead of v1"),
        "wrong-review-writes": (["e1"], "gives the review job write access to the contents"),
        "wrong-review-no-comment": (["e1"], "runs the review without --comment, so nothing is posted"),
        "wrong-review-no-checkout": (["e1"], "runs the review skill without checking the repository out"),
        "wrong-prompt-version-mismatch": (["e2"], "leaves the changelog on an older version than the prompt"),
        "wrong-prompt-no-version": (["e2"], "drops the version from the prompt file"),
        "wrong-review-no-skip": (["e3"], "has no Skip section in the review guidance"),
        "wrong-no-timeout": (["e4"], "leaves the mention job without a timeout"),
        "wrong-no-turn-cap": (["e4"], "leaves the mention run without a turn limit"),
        "wrong-literal-key": (["e5"], "writes the API key into the workflow"),
    },
}
