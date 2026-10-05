# Case lists of module 90-governance-safety-and-risk: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/90-governance-safety-and-risk/unit-01/practice-1"] = {
    "name": "governance", "suite": "GovernanceTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "no control that guards an input or an action fails open"),
        ("e1", "edge", "every high consequence action has a human step that exists and holds"),
        ("e2", "edge", "the automatic threshold is at least 95 and a high consequence action is never automatic"),
        ("e3", "edge", "retention keeps at least 90 days within the ceiling and stores no content"),
        ("e4", "edge", "the risk register names a control and an owner for each of four failure modes"),
        ("e5", "edge", "users are told that ai helped and no file holds personal data"),
        ("e6", "edge", "erasure removes the vault mapping within 30 days"),
    ],
    "plants": {
        "wrong-screen-open": (["m1"], "lets a request through unscreened when the input screen is down"),
        "wrong-approval-open": (["m1", "e1"], "lets a refund go ahead when its approval step fails"),
        "wrong-no-refund-review": (["e1"], "leaves the refund action with no human review step"),
        "wrong-review-unknown-control": (["e1"], "names a review control that is not defined"),
        "wrong-review-low-tier": (["e1"], "makes the review control an ordinary tier control and not a high tier one"),
        "wrong-threshold-94": (["e2"], "sends an answer out unreviewed at a confidence of 94"),
        "wrong-high-auto": (["e2"], "lets a high consequence action run without a person"),
        "wrong-unsupported-sent": (["e2"], "sends an answer that the source does not support"),
        "wrong-floor-89": (["e3"], "sets the audit floor to 89 days"),
        "wrong-retain-over-ceiling": (["e3"], "keeps audit entries one day beyond the ceiling"),
        "wrong-content-stored": (["e3"], "stores the text of every prompt in the audit log"),
        "wrong-no-hold-override": (["e3"], "lets the ceiling purge an entry under a legal hold"),
        "wrong-erasure-31": (["e6"], "allows 31 days to complete an erasure"),
        "wrong-erasure-keeps-map": (["e6"], "keeps the map from tokens to people after an erasure"),
        "wrong-register-three-rows": (["e4"], "leaves the unfair outcome out of the register"),
        "wrong-register-ghost-control": (["e4"], "names a control that is not defined in controls.json"),
        "wrong-register-no-owner": (["e4"], "gives a risk no named owner"),
        "wrong-register-no-mode": (["e4"], "leaves the privacy leak out of the register"),
        "wrong-no-disclosure": (["e5"], "does not say that users are told that AI helped"),
        "wrong-home-path": (["e5"], "writes a personal home path into the register"),
    },
}
