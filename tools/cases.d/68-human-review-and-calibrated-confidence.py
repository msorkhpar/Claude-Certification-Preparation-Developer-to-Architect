# Case lists of module 68-human-review-and-calibrated-confidence: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/68-human-review-and-calibrated-confidence/unit-01/practice-1"] = {
    "name": "review_routing", "suite": "ReviewRoutingTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "accuracy is reported per document type and field next to the overall figure"),
        ("e1", "edge", "a weak segment is hidden by a high overall figure and found by the breakdown"),
        ("e2", "edge", "automation needs every segment to pass and enough samples in each"),
        ("e3", "edge", "the threshold is the lowest confidence whose accepted items meet the target precision"),
        ("e4", "edge", "no threshold exists when no confidence level meets the target"),
        ("e5", "edge", "the stratified sample takes the best ranked items of every stratum"),
        ("e6", "edge", "low confidence and conflicts go to review with the weakest first"),
        ("e7", "edge", "review capacity is respected and the rest wait in a backlog"),
        ("e8", "edge", "an irreversible action needs a person whatever the confidence"),
    ],
    "plants": {
        "wrong-overall-percent": (["m1", "e1"], "gives every segment the overall percentage"),
        "wrong-ignores-undersampled": (["e2"], "approves automation for a segment with too few samples"),
        "wrong-highest-confidence": (["e3"], "picks the highest qualifying confidence instead of the lowest"),
        "wrong-strict-target": (["e4"], "demands more than the target precision"),
        "wrong-first-n-sample": (["e5"], "samples by identifier instead of by rank"),
        "wrong-conflict-auto": (["e6"], "lets a confident extraction with a conflict through"),
        "wrong-id-order": (["e6"], "orders the review queue by identifier instead of weakest first"),
        "wrong-ignores-capacity": (["e7"], "sends everything to review regardless of capacity"),
        "wrong-irreversible-by-amount": (["e8"], "checks only the amount for an irreversible action"),
    },
}

_c = PRACTICES[f"{X}/68-human-review-and-calibrated-confidence/unit-01/practice-1"]["plants"]

_c["wrong-minn-boundary"] = (["e2"], "lets a segment with exactly the minimum number of records fail unnoticed")

_c["wrong-threshold-boundary"] = (["e2"], "fails a segment that is exactly at the threshold")

_c["wrong-route-boundary"] = (["e6"], "sends an extraction exactly at the threshold to review")
