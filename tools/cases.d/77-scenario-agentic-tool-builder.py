# Case lists of module 77-scenario-agentic-tool-builder: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/77-scenario-agentic-tool-builder/unit-01/practice-1"] = {
    "name": "tool_review", "suite": "ToolReviewTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "a well formed read only tool is approved and leaves an audit line"),
        ("e1", "edge", "a description needs at least the minimum number of words"),
        ("e2", "edge", "the timeout and the memory may equal their limits and not exceed them"),
        ("e3", "edge", "a name is lower case snake case of at most sixty four characters"),
        ("e4", "edge", "forbidden calls in the code refuse the tool and are all listed in order"),
        ("e5", "edge", "a permission the code uses without declaring it or a denied one refuses the tool"),
        ("e6", "edge", "a declared write is approved only with a gate and a read alone is approved outright"),
        ("e7", "edge", "a refusal beats a revision and a revision beats a gate"),
        ("e8", "edge", "the permissions the code uses are reported in alphabetical order"),
    ],
    "plants": {
        "wrong-description-inclusive": (["e1"], "calls a description too short when it has exactly the minimum number of words"),
        "wrong-timeout-inclusive": (["e2"], "flags a timeout that equals the limit"),
        "wrong-memory-inclusive": (["e2"], "flags a memory request that equals the limit"),
        "wrong-name-too-long": (["e3"], "rejects a name of exactly sixty four characters"),
        "wrong-forbidden-first-only": (["e4"], "lists only the first forbidden call"),
        "wrong-undeclared-ignored": (["e5"], "does not refuse a permission that the code uses and the proposal does not declare"),
        "wrong-denied-ignored": (["e5"], "does not refuse a denied permission that the proposal declares"),
        "wrong-gate-skipped": (["e6"], "approves a declared write outright with no gate"),
        "wrong-revise-before-refuse": (["e7"], "sends a refused tool back for revision because it also has a finding"),
        "wrong-used-unsorted": (["e6", "e8"], "reports the permissions in the order of the scan table and not alphabetically"),
    },
}
