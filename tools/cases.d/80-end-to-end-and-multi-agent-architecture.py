# Case lists of module 80-end-to-end-and-multi-agent-architecture: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/80-end-to-end-and-multi-agent-architecture/unit-01/practice-1"] = {
    "name": "architecture_review", "suite": "ArchitectureReviewTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "a sound design passes review with no findings"),
        ("e1", "edge", "a design without a feedback loop or a stage is rejected"),
        ("e2", "edge", "autonomy is flagged only when the path is known"),
        ("e3", "edge", "several agents need independent parts and no shared context"),
        ("e4", "edge", "an unapproved write is a finding only when an audit is needed"),
        ("e5", "edge", "output that nobody validates is flagged"),
        ("e6", "edge", "findings are ordered by severity then rule and the verdict follows the worst"),
        ("e7", "edge", "the cheapest design that is not rejected wins and ties go by name"),
    ],
    "plants": {
        "wrong-no-feedback-check": (["e1", "e6", "e7"], "never reports a design without a feedback loop"),
        "wrong-input-stage-skipped": (["e1"], "does not require an input stage"),
        "wrong-autonomy-always": (["e2", "e3"], "flags every agent design, whether or not the path is known"),
        "wrong-autonomy-agent-only": (["e2"], "flags a single agent on a known path and not a multi-agent design"),
        "wrong-team-shared-only": (["e3"], "flags a team only for a shared context and not for parts that depend on each other"),
        "wrong-team-single-agent": (["m1", "e1", "e2", "e3", "e4", "e5", "e6", "e7"], "applies the team rule to a design with one agent"),
        "wrong-write-without-audit": (["e4"], "flags an unapproved write even when no audit is needed"),
        "wrong-validate-inverted": (["m1", "e1", "e2", "e3", "e4", "e5", "e6"], "flags output that has a validation step and not output that lacks one"),
        "wrong-lone-validate-ignored": (["e5"], "does not count a validation step that is the last step of the output stage"),
        "wrong-order-by-rule": (["e6"], "orders the findings by rule name and ignores severity"),
        "wrong-medium-approves": (["e6"], "approves a design that has only a medium finding"),
        "wrong-any-finding-rejects": (["e6", "e7"], "rejects a design for a medium finding"),
        "wrong-rejected-allowed": (["e7"], "lets a rejected design win on price"),
        "wrong-tie-by-position": (["e7"], "breaks a price tie by list position and not by name"),
    },
}
