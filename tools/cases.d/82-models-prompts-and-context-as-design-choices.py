# Case lists of module 82-models-prompts-and-context-as-design-choices: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/82-models-prompts-and-context-as-design-choices/unit-01/practice-1"] = {
    "name": "prompt_plan", "suite": "PromptPlanTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "static modules come first and the breakpoint follows the last one"),
        ("e1", "edge", "a variable in a static module is refused"),
        ("e2", "edge", "dynamic variables are filled and a missing one is refused"),
        ("e3", "edge", "the lowest priority dynamic module is dropped first and a tie drops the later one"),
        ("e4", "edge", "static modules are never dropped and a budget they exceed is refused"),
        ("e5", "edge", "a prefix under the minimum gets no breakpoint"),
        ("e6", "edge", "the cheapest model that meets the tier and the latency wins and ties go by name"),
        ("e7", "edge", "only an identical static prefix can be reused"),
    ],
    "plants": {
        "wrong-static-reversed": (["e3", "m1"], "puts the static modules in reverse order"),
        "wrong-breakpoint-after-all": (["e7", "m1"], "marks the breakpoint after the last block, dynamic ones included"),
        "wrong-variable-in-static-allowed": (["e1"], "accepts a variable inside a static module"),
        "wrong-missing-variable-blank": (["e2"], "replaces a missing variable with nothing"),
        "wrong-drop-highest-priority": (["e3"], "drops the module with the highest priority first"),
        "wrong-drop-tie-earlier": (["e3"], "drops the earlier of two modules with the same priority"),
        "wrong-over-budget-ignored": (["e4"], "returns a prompt over the budget when only static modules are left"),
        "wrong-minimum-exclusive": (["e5", "e7", "m1"], "needs more than the minimum for a breakpoint"),
        "wrong-minimum-one-low": (["e5"], "gives a breakpoint to a prefix one token under the minimum"),
        "wrong-model-tier-ignored": (["e6"], "ignores the capability tier the workload needs"),
        "wrong-model-latency-ignored": (["e6"], "ignores the latency limit"),
        "wrong-model-tie-by-position": (["e6"], "breaks a price tie by list position and not by name"),
        "wrong-reuse-first-block-only": (["e7"], "compares only the first block of the prefix"),
        "wrong-reuse-needs-whole-prompt": (["e7"], "reuses nothing unless the whole prompt, dynamic blocks included, is identical"),
    },
}
