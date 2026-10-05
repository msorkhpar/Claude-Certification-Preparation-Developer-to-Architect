# Case lists of module 56-the-built-in-tools: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/56-the-built-in-tools/unit-01/practice-1"] = {
    "name": "explorer_setup", "suite": "ExplorerSetupTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "the permission rules let an explorer read search and take notes but not change the source"),
        ("e1", "edge", "a read rule protects secrets from reading searching and writing"),
        ("e2", "edge", "only rule forms that are consulted are used and no whole tool that changes things is allowed"),
        ("e3", "edge", "the explorer agent reads and searches and says when to use it"),
        ("e4", "edge", "the sdk options make the search tools available and remove the tools that change things"),
        ("e5", "edge", "the exploration plan starts with a search then reads and never reads everything first"),
        ("e6", "edge", "the edit fallback widens the anchor then replaces all then rewrites the file"),
        ("e7", "edge", "no file holds a personal path an address or a key"),
    ],
    "plants": {
        "wrong-no-src-deny": (["m1"], "drops the denial of edits under src"),
        "wrong-bash-bare": (["m1", "e2"], "allows the whole Bash tool instead of read-only git patterns"),
        "wrong-notes-everywhere": (["m1"], "allows edits to every path instead of notes only"),
        "wrong-write-rule": (["m1", "e2"], "writes the notes rule for the Write tool, whose path rules are never matched"),
        "wrong-no-env-deny": (["e1"], "drops the denial of reading the environment file"),
        "wrong-grep-rule": (["e1", "e2"], "writes the secrets rule for Grep instead of Read"),
        "wrong-agent-edit": (["e3"], "gives the explorer the Edit tool"),
        "wrong-agent-bash": (["e3"], "gives the explorer the Bash tool"),
        "wrong-agent-no-use-when": (["e3"], "leaves the when-to-use sentence out of the description"),
        "wrong-agent-unbounded": (["e3"], "leaves the turn limit out of the agent"),
        "wrong-options-no-search": (["e4"], "lists only Read in tools, so the search tools are missing"),
        "wrong-options-allow-bash": (["e4"], "pre-approves Bash in allowedTools"),
        "wrong-options-no-disallow": (["e4"], "removes only Bash and leaves Edit and Write in the context"),
        "wrong-plan-read-first": (["e5"], "reads the entry points before any search"),
        "wrong-plan-read-all": (["e5"], "starts by reading every file"),
        "wrong-no-export-step": (["e5"], "traces usage by function name without listing exported names"),
        "wrong-fallback-rewrite-first": (["e6"], "rewrites the whole file before widening the anchor"),
        "wrong-fallback-no-replace-all": (["e6"], "leaves replace_all out of the remedies"),
        "wrong-home-path": (["e7"], "writes a personal home path into the plan"),
    },
}
