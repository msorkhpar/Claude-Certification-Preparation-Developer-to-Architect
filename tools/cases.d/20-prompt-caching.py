# Case lists of module 20-prompt-caching: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/20-prompt-caching/unit-01/practice-1"] = {
    "name": "cacheplan", "suite": "CachePlanTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "stable content comes first and the volatile date goes last"),
        ("e1", "edge", "sections follow the prefix order and keep their own order"),
        ("e2", "edge", "a breakpoint needs the stable prefix to reach the minimum"),
        ("e3", "edge", "at most four breakpoints are sent"),
        ("e4", "edge", "a one hour breakpoint may not follow a five minute one"),
        ("e5", "edge", "volatile blocks never carry a breakpoint and tools cannot be volatile"),
        ("e6", "edge", "every block comes out once and the input is not changed"),
    ],
    "plants": {
        "wrong-no-reorder": (["m1", "e1"], "keeps the blocks in the order they were given"),
        "wrong-volatile-first": (["m1"], "puts the volatile blocks ahead of the stable ones"),
        "wrong-per-block-minimum": (["e2"], "tests each block's own size against the minimum instead of the prefix"),
        "wrong-five-breakpoints": (["e3"], "allows five breakpoints"),
        "wrong-ttl-unchecked": (["e4"], "never checks the order of one hour and five minute breakpoints"),
        "wrong-volatile-cached": (["e5"], "puts a breakpoint on a volatile block"),
    },
}
