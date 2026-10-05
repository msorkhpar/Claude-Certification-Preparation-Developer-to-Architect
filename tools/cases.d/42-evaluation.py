# Case lists of module 42-evaluation: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/42-evaluation/unit-01/practice-1"] = {
    "name": "harness", "suite": "HarnessTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "a run grades every case with its own check and reports the pass rate"),
        ("e1", "edge", "an exact check ignores case and spacing but nothing else"),
        ("e2", "edge", "a json field check needs a json object with the field and the same typed value"),
        ("e3", "edge", "a judge check sends the rubric prompt and accepts only a bare score at the threshold"),
        ("e4", "edge", "a model that fails on one case does not stop the run"),
        ("e5", "edge", "tags report their own rates and success criteria judge each dimension"),
        ("e6", "edge", "a regression run names what broke what was fixed and what went missing"),
        ("e7", "edge", "repeated runs expose flaky cases and a case passes only if every run does"),
    ],
    "plants": {
        "wrong-regex-full-match": (["m1"], "requires the whole output to match the regular expression instead of searching it"),
        "wrong-exact-case-sensitive": (["e1"], "compares exact answers without lower-casing them"),
        "wrong-exact-contains": (["e1"], "accepts an output that merely contains the expected answer"),
        "wrong-json-loose-type": (["e2"], "compares the field and the expected value as text, so the string 3 equals the number 3"),
        "wrong-json-fence-ok": (["e2"], "strips a code fence before parsing, so fenced output passes"),
        "wrong-judge-first-digit": (["e3"], "takes the first digit it finds in the judge's reply instead of requiring a bare score"),
        "wrong-judge-strict-threshold": (["e3"], "requires a score above the threshold instead of at it"),
        "wrong-model-error-passes": (["e4"], "counts a run in which the model raised an error as a pass"),
        "wrong-tag-total": (["e5"], "counts a case in its tag total only when it passed"),
        "wrong-meets-at-or-below": (["e5"], "fails the overall criterion when the pass rate equals the minimum"),
        "wrong-meets-missing-tag-ok": (["e5"], "lets a criterion pass when no case carries its tag"),
        "wrong-compare-rate-only": (["e6"], "calls a run fine whenever the pass rate did not fall, even with a regression"),
        "wrong-compare-removed-ignored": (["e6"], "ignores cases that disappeared from the set"),
        "wrong-flaky-any-run": (["e7"], "passes a case when any of its repeated runs passes"),
    },
}
