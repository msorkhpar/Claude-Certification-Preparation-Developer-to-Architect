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
        "wrong-bypass-mode": (["e1", "e2"], "sets defaultMode to bypassPermissions in the shared file, where it is ignored"),
        "wrong-write-deny": (["e1", "e2"], "writes the secret rules for the Write tool, which no check consults, instead of Read"),
        "wrong-push-ask-only": (["e1"], "asks before a push instead of denying it"),
        "wrong-bare-bash-allow": (["e1", "e2"], "allows every Bash command with a bare Bash rule"),
        "wrong-local-not-ignored": (["e3"], "leaves settings.local.json out of .gitignore"),
        "wrong-local-widens": (["e1", "e3"], "puts a permission rule in the personal file"),
        "wrong-bloated-memory": (["m1"], "lets CLAUDE.md grow past 200 lines"),
        "wrong-no-import": (["m1"], "names the architecture file without the @ that imports it"),
        "wrong-skill-auto": (["e4"], "lets Claude start the fix-issue command on its own"),
        "wrong-headless-bypass": (["e5"], "skips permissions in the headless run"),
        "wrong-headless-unbounded": (["e5"], "leaves the headless run without a turn limit"),
        "wrong-key-in-script": (["e6"], "writes an API key into the script"),
    },
}
