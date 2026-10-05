# Case lists of module 71-scenario-code-generation-with-claude-code: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/71-scenario-code-generation-with-claude-code/unit-01/practice-1"] = {
    "name": "team_setup", "suite": "TeamSetupTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "each convention loads for exactly the files of its area"),
        ("e1", "edge", "the root file is short and holds only what every task needs"),
        ("e2", "edge", "the review command is shared read only and says what it does"),
        ("e3", "edge", "the settings protect the environment file and approve no whole tool"),
        ("e4", "edge", "the modes table sends open design work to plan mode and clear small work to direct"),
        ("e5", "edge", "every rule scopes itself with a glob that matches a file"),
        ("e6", "edge", "no file holds a personal path an address or a key"),
    ],
    "plants": {
        "wrong-docs-unscoped": (["e5"], "leaves the documentation rule without paths, so it loads in every session"),
        "wrong-handlers-wide": (["m1"], "scopes the handlers rule to every ts file, so it also loads for the database files"),
        "wrong-tests-folder": (["m1"], "scopes the testing rule to one folder, so test files elsewhere miss it"),
        "wrong-tests-ts-only": (["m1"], "scopes the testing rule to the ts files and leaves the tsx files out"),
        "wrong-root-keeps-hooks": (["e1"], "leaves one component convention in the root file"),
        "wrong-root-long": (["e1"], "pads the root file past twenty-five lines"),
        "wrong-review-bare-bash": (["e2"], "pre approves the whole Bash tool in the review command"),
        "wrong-review-no-description": (["e2"], "leaves the description out of the review command"),
        "wrong-no-env-deny": (["e3"], "leaves the environment file readable"),
        "wrong-bash-allow": (["e3"], "approves the whole Bash tool in the settings"),
        "wrong-monolith-direct": (["e4"], "sends the restructuring of the monolith to direct execution"),
        "wrong-typo-plan": (["e4"], "sends a one-line typo fix through plan mode"),
        "wrong-home-path": (["e6"], "writes a personal home path into the root file"),
    },
}
