# Case lists of module 84-cost-and-capacity-engineering: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/84-cost-and-capacity-engineering/unit-01/practice-1"] = {
    "name": "gateway_budget", "suite": "GatewayBudgetTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "a request follows the route table of the gateway"),
        ("e1", "edge", "a team near its budget is moved to a cheaper model and a team over it is refused"),
        ("e2", "edge", "a request is admitted warned or blocked against the budget"),
        ("e3", "edge", "showback adds each teams tokens at the price of the model and refuses an unknown model"),
        ("e4", "edge", "showback rounds each teams total to a cent once"),
        ("e5", "edge", "a caller with a hard latency limit gets accept and poll when the slow case does not fit"),
        ("e6", "edge", "a model pinned by a team is honoured only when the policy allows it"),
    ],
    "plants": {
        "wrong-route-ignores-table": (["m1", "e1", "e6"], "sends every task to the default model"),
        "wrong-warn-keeps-model": (["e1", "e6"], "keeps the expensive model for a team that is near its budget"),
        "wrong-block-still-routes": (["e1"], "routes a request of a team that is over its budget"),
        "wrong-warn-strict": (["e2"], "warns only above 80 percent and not at it"),
        "wrong-block-at-budget": (["e2"], "blocks a request that lands exactly on the budget"),
        "wrong-zero-budget-open": (["e2"], "lets a team with no budget through"),
        "wrong-showback-unknown-free": (["e3"], "prices an unknown model at nothing"),
        "wrong-showback-by-name": (["e3"], "lists teams by name and not by cost"),
        "wrong-showback-row-rounding": (["e4"], "rounds every row to a cent before adding them"),
        "wrong-showback-floors": (["e4"], "rounds a team's total down and not to the nearest cent"),
        "wrong-delivery-strict": (["e5"], "chooses accept and poll when the slow case exactly fits"),
        "wrong-delivery-no-margin": (["e5"], "leaves the safety margin out of the comparison"),
        "wrong-pin-always-honoured": (["e6"], "honours any model a team pins"),
    },
}
