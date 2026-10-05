# Case lists of module 48-multi-step-workflows-with-guarantees: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/48-multi-step-workflows-with-guarantees/unit-01/practice-1"] = {
    "name": "desk", "suite": "RefundDeskTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "a verified customer can look up an order and be refunded within the limit"),
        ("e1", "edge", "a refund before identity is verified is blocked in code and never reaches the backend"),
        ("e2", "edge", "an order that belongs to someone else is neither shown nor refundable"),
        ("e3", "edge", "a refund is checked against the order its amount and what is left"),
        ("e4", "edge", "a refund over the limit is not executed and becomes a structured hand off"),
        ("e5", "edge", "a failed check does not unlock anything and three in a row lock the desk"),
        ("e6", "edge", "unknown tools and backend errors are reported and the hand off lists every block"),
    ],
    "plants": {
        "wrong-identity-unchecked": (["e1"], "lets any call but verification through without a verified customer"),
        "wrong-ownership-unchecked": (["e2"], "remembers and shows an order that belongs to another customer"),
        "wrong-over-limit-executes": (["e4"], "runs a refund above the limit instead of handing it to a person"),
        "wrong-failure-unlocks": (["e5"], "keeps the earlier verified customer after a failed check"),
        "wrong-no-lockout": (["e5"], "never locks the desk after repeated failed checks"),
        "wrong-exceeds-ignored": (["e3"], "refunds more than what is left on the order"),
        "wrong-zero-amount-ok": (["e3"], "accepts a refund of zero cents"),
        "wrong-handoff-no-blocks": (["e6"], "leaves the refusals out of the hand-off"),
    },
}
