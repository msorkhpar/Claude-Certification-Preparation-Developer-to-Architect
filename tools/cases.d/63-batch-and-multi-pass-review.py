# Case lists of module 63-batch-and-multi-pass-review: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/63-batch-and-multi-pass-review/unit-01/practice-1"] = {
    "name": "batch_review", "suite": "BatchReviewTest", "langs": ["python", "typescript", "java", "kotlin"], "pyfile": "test_batch_review.py", "tsfile": "batchReview.test.ts",
    "cases": [
        ("m1", "main", "the interval between submissions leaves room for the window and the handling"),
        ("e1", "edge", "an sla without room for a batch is refused"),
        ("e2", "edge", "a blocking check or a tool loop needs the synchronous api"),
        ("e3", "edge", "only the items that did not succeed are resubmitted by custom id"),
        ("e4", "edge", "an item over the limit is chunked and a rejected request is fixed first"),
        ("e5", "edge", "a multi file review gets a local pass per file and one integration pass"),
        ("e6", "edge", "the same finding from two passes is one finding with the highest severity and the lowest confidence"),
        ("e7", "edge", "a finding is accepted only when two independent passes agree with confidence"),
    ],
    "plants": {
        "wrong-ignores-handling": (["m1", "e1"], "leaves the handling time out of the interval"),
        "wrong-oversized-resubmitted": (["e4"], "resubmits an oversized item unchanged instead of chunking it"),
        "wrong-resubmit-all": (["e3"], "resubmits the items that succeeded too"),
        "wrong-no-integration-pass": (["e5"], "plans local passes only and no integration pass"),
        "wrong-lone-confident-accepted": (["e7"], "accepts a finding that one pass reported with high confidence"),
    },
}

_c = PRACTICES[f"{X}/63-batch-and-multi-pass-review/unit-01/practice-1"]["plants"]

_c["wrong-chunk-at-limit"] = (["e4"], "chunks an entry that is exactly at the limit")

_c["wrong-interval-boundary"] = (["m1"], "refuses a submission interval of one hour")
