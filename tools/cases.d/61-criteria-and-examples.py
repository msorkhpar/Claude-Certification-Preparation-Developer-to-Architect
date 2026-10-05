# Case lists of module 61-criteria-and-examples: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/61-criteria-and-examples/unit-01/practice-1"] = {
    "name": "review_spec", "suite": "ReviewSpecTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "the prompt puts criteria first then examples then the diff last"),
        ("e1", "edge", "vague criteria are refused in both the report and the skip text"),
        ("e2", "edge", "a criterion needs report skip and a concrete severity example for high and low"),
        ("e3", "edge", "two to four examples with a report and a skip each carrying a reason"),
        ("e4", "edge", "a category with enough reviews and low precision is disabled"),
        ("e5", "edge", "the most dismissed patterns are listed by count then name and capped at three"),
        ("e6", "edge", "an attended run asks only what it cannot assume and states its assumptions"),
        ("e7", "edge", "an unattended run never asks it states assumptions or stops"),
    ],
    "plants": {
        "wrong-diff-first": (["m1"], "puts the diff before the criteria"),
        "wrong-vague-report-only": (["e1"], "checks the report text for vague phrases and not the skip text"),
        "wrong-vague-short-list": (["e1"], "leaves be conservative out of the vague phrases"),
        "wrong-skip-blank": (["e2"], "accepts a blank report or skip text"),
        "wrong-severity-high-only": (["e2"], "asks for a high severity example and not for a low one"),
        "wrong-examples-up-to-six": (["e3"], "allows up to six examples"),
        "wrong-all-report-examples": (["e3"], "accepts a set of examples that are all reports"),
        "wrong-reason-optional": (["e3"], "accepts an example whose reason is blank"),
        "wrong-unknown-category": (["e3"], "accepts a report example for a category that is not a criterion"),
        "wrong-disable-few": (["e4"], "disables a category on too few reviews"),
        "wrong-precision-inverted": (["e4"], "computes the share dismissed and calls it precision"),
        "wrong-boundary-disables": (["e4"], "disables a category whose precision is exactly the limit"),
        "wrong-top-by-name": (["e5"], "lists dismissed patterns by name and not by count"),
        "wrong-top-uncapped": (["e5"], "lists every dismissed pattern"),
        "wrong-counts-accepted": (["e5"], "counts the patterns of accepted findings as dismissed"),
        "wrong-ask-defaults": (["e6", "e7"], "asks for fields that have a default"),
        "wrong-no-assumptions": (["e6", "e7"], "assumes defaults without stating them"),
        "wrong-unattended-asks": (["e7"], "asks a question in a run that nobody attends"),
        "wrong-unattended-guess": (["e7"], "proceeds in an unattended run without a value that has no default"),
        "wrong-blank-is-present": (["e6"], "treats a field that holds only spaces as given"),
    },
}
