# Planted wrong solutions of module 91-stakeholders-and-the-project-lifecycle: exact replacements against the practice's reference (see tools/make_plants.py).
# Runs with PLANTS, X and both() in scope; names defined here are local to this file.

_D91 = "docs/design-record.md"

_P91 = {
    "wrong-section-renamed": {_D91: [("## Service levels\n", "## Targets\n")]},
    "wrong-todo-left": {_D91: [("## Hand-off and monitoring\n\n", "## Hand-off and monitoring\n\nTODO check this.\n\n")]},
    "wrong-no-role": {_D91: [("The billing operations manager is accountable for each credit, and the model prepares the file and does not decide.", "Someone is accountable for each credit, and the model prepares the file and does not decide.")]},
    "wrong-no-error-cost": {_D91: [("A wrong credit costs about 250 and a check by a person costs 5.", "A wrong credit is costly.")]},
    "wrong-cheapest-not-recommended": {_D91: [("| yes | alternative |", "| yes | recommended |"), ("| yes | recommended | The cheapest design that meets the service levels |", "| yes | alternative | The cheapest design that meets the service levels |")]},
    "wrong-two-recommended": {_D91: [("| yes | alternative |", "| yes | recommended |")]},
    "wrong-rejection-no-reason": {_D91: [("The most expensive option and the slowest to answer", "Too dear")]},
    "wrong-three-options": {_D91: [("| The model drafts and a person decides every item | 150,000 | yes | alternative | Safe, but it pays for checks on answers that are already right |\n", "")]},
    "wrong-breakeven-97": {_D91: [("at or above 98 percent", "at or above 97 percent")]},
    "wrong-latency-2500": {_D91: [("| 2000 ms |", "| 2500 ms |")]},
    "wrong-availability-99": {_D91: [("| 99.5 percent |", "| 99.0 percent |")]},
    "wrong-sla-no-owner": {_D91: [("| Billing quality lead |", "| TBD |")]},
    "wrong-status-reviewed": {_D91: [("| outdated figure | 12 | auto |", "| outdated figure | 12 | reviewed |")]},
    "wrong-status-97-auto": {_D91: [("| status | 100 | 98 percent |", "| status | 100 | 97 percent |")]},
    "wrong-segment-order": {_D91: [("| complaint | 100 | 91 percent | omitted exception | 60 | reviewed |\n| status | 100 | 98 percent | outdated figure | 12 | auto |", "| status | 100 | 98 percent | outdated figure | 12 | auto |\n| complaint | 100 | 91 percent | omitted exception | 60 | reviewed |")]},
    "wrong-shape-unnamed": {_D91: [("| wrong amount |", "| bad |")]},
    "wrong-pilot-three": {_D91: [("| Reviewers kept up | Compute reviewer hours from volume and routing | The review queue is older than 4 hours |\n", "")]},
    "wrong-trigger-no-number": {_D91: [("| Credit accuracy falls below 60 percent on the sample |", "| Credit accuracy falls on the sample |")]},
    "wrong-no-runbook": {_D91: [("owns the runbook that explains each alert", "owns the guide that explains each alert")]},
    "wrong-no-rollback": {_D91: [("Rollback sends every request to the previous model, which stays configured and tested until its own retirement date.", "If it goes wrong, we fix it.")]},
    "wrong-summary-81-words": {_D91: [("approve the pilot of this design", "approve the pilot of this whole design")]},
    "wrong-summary-no-cost": {_D91: [("It costs 80,000 a month against 315,000 for people alone, and it meets", "It costs less than people alone, and it meets")]},
    "wrong-summary-no-decision": {_D91: [("We ask for one decision today: approve", "We ask for approval today: approve")]},
    "wrong-home-path": {_D91: [("# Design record: dispute assistant\n", "# Design record: dispute assistant\n\nNotes live in /home/dev/notes.\n")]},
}

PLANTS[f"{X}/91-stakeholders-and-the-project-lifecycle/unit-01/practice-1"] = {l: (_D91, _P91) for l in ("python", "typescript", "java", "kotlin")}
