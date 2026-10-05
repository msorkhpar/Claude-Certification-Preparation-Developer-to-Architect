# Case lists of module 75-scenario-structured-data-extraction: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/75-scenario-structured-data-extraction/unit-01/practice-1"] = {
    "name": "run_audit", "suite": "RunAuditTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "a mixed run gets every count both accuracies the segments and the first fix"),
        ("e1", "edge", "an empty run has zero figures no segments and never meets the target"),
        ("e2", "edge", "the run meets the target at exactly the target and not below it"),
        ("e3", "edge", "a kind needs at least the minimum number of documents to be automated"),
        ("e4", "edge", "a kind is automated at exactly the target accuracy and not below it"),
        ("e5", "edge", "the figure is overstated only when the validated accuracy exceeds the all document accuracy by more than the gap"),
        ("e6", "edge", "each shape counts documents once and an unchecked total counts only documents accepted as valid"),
        ("e7", "edge", "the first fix follows the order of what costs most"),
        ("e8", "edge", "percentages are whole numbers rounded half up"),
    ],
    "plants": {
        "wrong-validated-denominator": (["m1", "e5", "e7"], "divides the correct documents by the valid ones and calls that the accuracy of the run"),
        "wrong-target-strict": (["e2"], "requires the accuracy to be above the target and not at it"),
        "wrong-segment-strict": (["e4"], "automates a kind only when its accuracy is above the target and not at it"),
        "wrong-min-n-ignored": (["e3"], "automates a kind at the target accuracy however few documents it has"),
        "wrong-gap-inclusive": (["e5"], "calls the figure overstated when the gap equals the limit"),
        "wrong-unchecked-any-status": (["e6"], "counts an unchecked total in documents that were never accepted as valid"),
        "wrong-fix-unchecked-before-wasted": (["e7"], "puts the unchecked totals before the wasted retries in the order of fixes"),
        "wrong-rounding-floor": (["e8"], "rounds a percentage down and not half up"),
        "wrong-empty-meets-target": (["e1"], "says an empty run meets the target"),
    },
}
