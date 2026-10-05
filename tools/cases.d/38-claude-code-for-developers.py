# Case lists of module 38-claude-code-for-developers: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/38-claude-code-for-developers/unit-01/practice-1"] = {
    "name": "project_setup", "suite": "ProjectSetupTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "the memory file is short concrete and pulls in the architecture notes"),
        ("e1", "edge", "the permission rules allow the daily commands ask before commits and deny secrets and pushes"),
        ("e2", "edge", "the shared file sets a mode it may set and uses only rules that are consulted"),
        ("e3", "edge", "personal settings stay local and the local file wins"),
        ("e4", "edge", "the custom command is a skill that only a person can start"),
        ("e5", "edge", "the headless script is bounded and does not skip permissions"),
        ("e6", "edge", "no file holds a personal path an address or a key"),
    ],
    "plants": {
        "wrong-bloated-memory": (["m1"], "lets CLAUDE.md grow past 200 lines"),
        "wrong-no-import": (["m1"], "names the architecture file without the @ that imports it"),
        "wrong-push-ask-only": (["e1"], "asks before a push instead of denying it"),
        "wrong-commit-allowed": (["e1"], "allows git commit without asking"),
        "wrong-env-readable": (["e1"], "leaves the .env file out of the deny rules"),
        "wrong-write-rule": (["e2"], "adds a path rule for the Write tool, which no check consults"),
        "wrong-extra-permission-key": (["e2"], "adds a permissions key that the shared file may not hold"),
        "wrong-local-not-ignored": (["e3"], "leaves settings.local.json out of .gitignore"),
        "wrong-local-md-not-ignored": (["e3"], "leaves CLAUDE.local.md out of .gitignore"),
        "wrong-local-env": (["e3"], "puts an env block in the personal file"),
        "wrong-skill-auto": (["e4"], "lets Claude start the fix-issue command on its own"),
        "wrong-skill-no-hint": (["e4"], "leaves the argument hint out of the command"),
        "wrong-headless-bypass": (["e5"], "skips permissions in the headless run"),
        "wrong-headless-unbounded": (["e5"], "leaves the headless run without a turn limit"),
        "wrong-headless-no-budget": (["e5"], "leaves the headless run without a dollar cap"),
        "wrong-headless-bash-tool": (["e5"], "pre-approves the whole Bash tool in the headless run"),
        "wrong-key-in-script": (["e6"], "writes an API key into the script"),
        "wrong-path-in-memory": (["e6"], "writes a personal home path into CLAUDE.md"),
    },
}
