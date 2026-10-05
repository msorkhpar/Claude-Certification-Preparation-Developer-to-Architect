# Case lists of module 57-memory-files-and-rules: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/57-memory-files-and-rules/unit-01/practice-1"] = {
    "name": "memory_setup", "suite": "MemorySetupTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "the conventions of each area load for exactly the files that area governs"),
        ("e1", "edge", "the root file is short and holds only what every task needs"),
        ("e2", "edge", "the testing rule follows the file type and not the folder"),
        ("e3", "edge", "the import names a file that exists and loads at launch"),
        ("e4", "edge", "personal lines sit in personal files and the local file is ignored"),
        ("e5", "edge", "a rule that must always hold is a permission rule and not a sentence"),
        ("e6", "edge", "every rule scopes itself with paths that are valid and match something"),
        ("e7", "edge", "no file holds a personal path an address or a key"),
    ],
    "plants": {
        "wrong-api-unscoped": (["e6", "m1"], "leaves the API rule without paths, so it loads in every session"),
        "wrong-api-bare-folder": (["e6", "m1"], "scopes the API rule with a bare folder name, which is not a glob and matches no file"),
        "wrong-testing-folder": (["e2", "m1"], "scopes the testing rule to one folder, so test files elsewhere miss it"),
        "wrong-testing-ts-only": (["e2", "m1"], "scopes the testing rule to the ts files and leaves the tsx files out"),
        "wrong-terraform-everything": (["m1"], "scopes the Terraform rule to every file"),
        "wrong-root-keeps-testing": (["e1", "m1"], "leaves one testing convention in the root file"),
        "wrong-root-long": (["e1"], "pads the root file past fifty lines"),
        "wrong-import-typo": (["e3"], "misspells the path of the import, so it imports nothing"),
        "wrong-import-in-code-span": (["e3"], "writes the import inside a code span, where it is not expanded"),
        "wrong-personal-in-root": (["e4"], "puts a personal preference into the shared root file"),
        "wrong-local-not-ignored": (["e4"], "leaves the local instructions file out of the ignore file"),
        "wrong-no-migration-deny": (["e5"], "leaves the migrations protected by a sentence only"),
        "wrong-write-rule": (["e5"], "writes the migrations rule for the Write tool, whose path rules are never matched"),
        "wrong-home-path": (["e7"], "writes a personal home path into the root file"),
        "wrong-testing-too-wide": (["e2"], "adds a glob for the SQL files to the testing rule, which are not tests"),
        "wrong-terraform-dead-glob": (["e6"], "adds a glob to the Terraform rule that matches no file of the project"),
    },
}
