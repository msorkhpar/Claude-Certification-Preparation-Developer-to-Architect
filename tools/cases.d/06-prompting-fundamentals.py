# Case lists of module 06-prompting-fundamentals: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/06-prompting-fundamentals/unit-01/practice-1"] = {
    "name": "prompt_builder", "suite": "PromptBuilderTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "full prompt has every section in order"),
        ("e1", "edge", "absent optional sections are omitted not empty"),
        ("e2", "edge", "variables fill once and a missing one is named"),
        ("e3", "edge", "blank task is refused"),
        ("e4", "edge", "document text cannot close its own tag"),
        ("e5", "edge", "placeholders inside documents stay literal"),
        ("e6", "edge", "documents and examples keep their order and index"),
    ],
    "plants": {
        "wrong-task-first": (["m1"], "puts the task section before the documents"),
        "wrong-empty-sections": (["e1"], "renders empty tags for absent optional sections"),
        "wrong-missing-variable-silent": (["e2"], "leaves an unknown placeholder in the output instead of failing"),
        "wrong-blank-task-accepted": (["e3"], "accepts a whitespace-only task"),
        "wrong-no-escape": (["e4"], "does not escape document text"),
        "wrong-fill-documents": (["e5"], "fills placeholders inside document text"),
        "wrong-documents-sorted": (["e6"], "renders documents sorted by name instead of in the order given"),
    },
}
