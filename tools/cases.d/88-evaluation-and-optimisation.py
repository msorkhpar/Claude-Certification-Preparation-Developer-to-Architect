# Case lists of module 88-evaluation-and-optimisation: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/88-evaluation-and-optimisation/unit-01/practice-1"] = {
    "name": "evalkit", "suite": "EvalKitTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "a segment table reports accuracy and error cost with the costliest segment first"),
        ("e1", "edge", "a segment with no cost entry costs one per error and no results give an empty table"),
        ("e2", "edge", "a percentile uses the nearest rank and does not need sorted input"),
        ("e3", "edge", "a test with fewer cases than the minimum in either arm decides nothing"),
        ("e4", "edge", "a difference is called only when it clears the 95 percent bar and the better side is named"),
        ("e5", "edge", "a shadow run is held for a regression in a protected segment or for more losses than gains"),
        ("e6", "edge", "diagnosis checks the evidence then the grounding then the format then the stronger model"),
        ("e7", "edge", "model choice takes the cheapest option that meets the accuracy floor and the latency limit"),
    ],
    "plants": {
        "wrong-pct-truncated": (["m1"], "rounds an accuracy down instead of to the nearest whole percent"),
        "wrong-sorted-by-name": (["m1"], "orders the report by segment name and not by error cost"),
        "wrong-default-cost-zero": (["e1"], "makes the errors of a segment with no cost entry free"),
        "wrong-percentile-floor": (["e2"], "takes the rank rounded down, so the 95th percentile of four values is the third"),
        "wrong-percentile-unsorted": (["e2"], "reads the percentile from the values in the order they arrived"),
        "wrong-ab-min-ignored": (["e3"], "gives a verdict on a handful of cases"),
        "wrong-ab-always-new": (["e4"], "names the new version as better whenever it clears the bar, even when it is worse"),
        "wrong-ab-90-percent": (["e4"], "uses the 90 percent bar and calls a difference that could be chance"),
        "wrong-gate-net-only": (["e5"], "ships a version that lost a right answer in a protected segment because it gained as many elsewhere"),
        "wrong-gate-protected-only": (["e5"], "ships a version that lost more answers than it gained in segments that are not protected"),
        "wrong-diagnose-grounding-late": (["e6"], "checks the format before it asks whether the answer is supported by the evidence"),
        "wrong-choose-ignores-latency": (["e7"], "picks a model that is too slow because it meets the accuracy floor"),
        "wrong-choose-ties-by-order": (["e7"], "breaks a tie in cost by the order the options were listed"),
    },
}
