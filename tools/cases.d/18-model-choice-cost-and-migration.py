# Case lists of module 18-model-choice-cost-and-migration: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/18-model-choice-cost-and-migration/unit-01/practice-1"] = {
    "name": "router", "suite": "RouterTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "cost of a plain request and the cheapest model that meets the tier"),
        ("e1", "edge", "cache reads and writes are priced by their own multipliers"),
        ("e2", "edge", "the batch discount halves every part of the cost"),
        ("e3", "edge", "the router picks by cost and tier not by the order of the catalog"),
        ("e4", "edge", "a model whose context or output limit is too small is skipped"),
        ("e5", "edge", "deprecated models are skipped and an empty choice raises"),
        ("e6", "edge", "a tie goes to the lower tier"),
    ],
    "plants": {
        "wrong-flat-cache-read": (["e1"], "prices every cache read at a tenth of the input price, whatever the model"),
        "wrong-batch-output-only": (["e2"], "applies the batch discount to the output tokens only"),
        "wrong-first-eligible": (["e3"], "returns the first eligible model of the catalog instead of the cheapest"),
        "wrong-ignore-context": (["e4"], "never checks the context window or the output limit"),
        "wrong-tie-high-tier": (["e6"], "breaks a cost tie towards the higher tier"),
    },
}
