# Case lists of module 70-scenario-customer-support-agent: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/70-scenario-customer-support-agent/unit-01/practice-1"] = {
    "name": "audit", "suite": "AuditTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "a mixed set of sessions gets every rate and the first fix"),
        ("e1", "edge", "no sessions give zero rates and no diagnosis"),
        ("e2", "edge", "a protected call before a successful identity check is a skipped prerequisite"),
        ("e3", "edge", "a refund over the limit counts only when it was made"),
        ("e4", "edge", "money first then tool descriptions then escalation criteria"),
        ("e5", "edge", "the target boundary and rounding of the first contact rate"),
        ("e6", "edge", "a wrong tool counts sessions and ignores steps with no known right tool"),
    ],
    "plants": {
        "wrong-failed-identity-opens": (["e2"], "treats a failed identity check as an identification"),
        "wrong-over-limit-any-outcome": (["e3"], "counts a refund over the limit in a session where it was refused and escalated"),
        "wrong-diagnosis-criteria-first": (["e4"], "puts the escalation criteria before the tool descriptions in the order of fixes"),
        "wrong-target-strict": (["e5"], "requires the rate to be above the target and not at it"),
        "wrong-wrong-tool-steps": (["e6"], "counts wrong steps and not the sessions that have one"),
        "wrong-empty-rate-one": (["e1"], "reports a rate of 1 when there are no sessions"),
        "wrong-over-counts-needs-human": (["m1"], "counts every escalation as an over-escalation, even one that needed a person"),
    },
}
