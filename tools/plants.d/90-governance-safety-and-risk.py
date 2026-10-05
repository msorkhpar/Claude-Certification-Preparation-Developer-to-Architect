# Planted wrong solutions of module 90-governance-safety-and-risk: exact replacements against the practice's reference (see tools/make_plants.py).
# Runs with PLANTS, X and both() in scope; names defined here are local to this file.

_P90 = {
    "wrong-screen-open": {"governance/controls.json": [('"id": "input-screen", "layer": "input", "tier": "all", "on_failure": "hold"', '"id": "input-screen", "layer": "input", "tier": "all", "on_failure": "proceed-flagged"')]},
    "wrong-approval-open": {"governance/controls.json": [('"id": "refund-approval", "layer": "action", "tier": "high", "on_failure": "hold"', '"id": "refund-approval", "layer": "action", "tier": "high", "on_failure": "proceed-flagged"')]},
    "wrong-no-refund-review": {"governance/controls.json": [('"human_review": {"issue_refund": "refund-approval", "close_account": "refund-approval"}', '"human_review": {"close_account": "refund-approval"}')]},
    "wrong-review-unknown-control": {"governance/controls.json": [('"issue_refund": "refund-approval"', '"issue_refund": "manager-approval"')]},
    "wrong-review-low-tier": {"governance/controls.json": [('"id": "refund-approval", "layer": "action", "tier": "high"', '"id": "refund-approval", "layer": "action", "tier": "all"')]},
    "wrong-threshold-94": {"governance/routing.json": [('"auto_confidence_min": 95', '"auto_confidence_min": 94')]},
    "wrong-high-auto": {"governance/routing.json": [('"high_consequence_auto": false', '"high_consequence_auto": true')]},
    "wrong-unsupported-sent": {"governance/routing.json": [('"unsupported_answer": "hold"', '"unsupported_answer": "send"')]},
    "wrong-floor-89": {"governance/retention.json": [('"floor_days": 90', '"floor_days": 89')]},
    "wrong-retain-over-ceiling": {"governance/retention.json": [('"retain_days": 365', '"retain_days": 366')]},
    "wrong-content-stored": {"governance/retention.json": [('"store_content": false', '"store_content": true')]},
    "wrong-no-hold-override": {"governance/retention.json": [('"legal_hold_overrides_ceiling": true', '"legal_hold_overrides_ceiling": false')]},
    "wrong-erasure-31": {"governance/retention.json": [('"max_days_to_complete": 30', '"max_days_to_complete": 31')]},
    "wrong-erasure-keeps-map": {"governance/retention.json": [('"remove_vault_mapping": true', '"remove_vault_mapping": false')]},
    "wrong-register-three-rows": {"docs/risk-register.md": [("| Different outcomes for different groups | unfair outcome | parity-report | Model risk lead | Medium: monitored and not prevented |\n", "")]},
    "wrong-register-ghost-control": {"docs/risk-register.md": [("| hallucination | grounding-check |", "| hallucination | pii-filter |")]},
    "wrong-register-no-owner": {"docs/risk-register.md": [("| Claims quality lead |", "| TBD |")]},
    "wrong-register-no-mode": {"docs/risk-register.md": [("| privacy leak |", "| data leak |")]},
    "wrong-no-disclosure": {"docs/risk-register.md": [("Every person who receives output is told that AI helped produce it. Disclosure", "Disclosure")]},
    "wrong-home-path": {"docs/risk-register.md": [("# Risk register\n", "# Risk register\n\nNotes live in /home/dev/notes.\n")]},
}

PLANTS[f"{X}/90-governance-safety-and-risk/unit-01/practice-1"] = {l: ("governance/controls.json", _P90) for l in ("python", "typescript", "java", "kotlin")}
