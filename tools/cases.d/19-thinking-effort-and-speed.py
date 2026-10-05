# Case lists of module 19-thinking-effort-and-speed: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/19-thinking-effort-and-speed/unit-01/practice-1"] = {
    "name": "params", "suite": "ParamsTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "each course model gets the request it accepts"),
        ("e1", "edge", "thinking modes a model does not have are refused"),
        ("e2", "edge", "the mode that skips thinking up front is sonnet only and needs high effort or below"),
        ("e3", "edge", "effort needs a supporting model and a real level"),
        ("e4", "edge", "newer models reject sampling parameters and haiku keeps them"),
        ("e5", "edge", "fast mode is opus only with its beta header and never in a batch"),
        ("e6", "edge", "a manual budget is at least 1024 and below max tokens"),
        ("e7", "edge", "effort lives in output config and never inside thinking"),
    ],
    "plants": {
        "wrong-allow-disabled": (["e1"], "lets thinking be disabled on models that reject it"),
        "wrong-between-any-effort": (["e2"], "accepts between_tools at xhigh and max effort"),
        "wrong-effort-on-haiku": (["e3"], "sends effort to Haiku 4.5, which does not support it"),
        "wrong-sampling-pass-through": (["e4"], "passes temperature, top_p and top_k through to every model"),
        "wrong-fast-in-batch": (["e5"], "allows fast mode inside a batch"),
        "wrong-budget-floor": (["e6"], "accepts a budget below the 1,024 minimum"),
        "wrong-effort-in-thinking": (["e7"], "puts the effort level inside the thinking object"),
    },
}
