# Case lists of module 35-the-claude-agent-sdk: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/35-the-claude-agent-sdk/unit-01/practice-1"] = {
    "name": "agent", "suite": "", "langs": ["python", "typescript"],
    "cases": [
        ("m1", "main", "every tool call goes through the permission callback and the run is summarised"),
        ("e1", "edge", "the options reach the cli as flags and the run starts in the project"),
        ("e2", "edge", "file tools stay inside the project and away from secrets"),
        ("e3", "edge", "bash is limited to a few commands and dangerous ones stop the run"),
        ("e4", "edge", "a hook blocks a push before the permission callback is asked"),
        ("e5", "edge", "the messages of a run fold into a summary"),
        ("e6", "edge", "an error result still gives its summary and a crash is not hidden"),
        ("e7", "edge", "denied calls are counted and the run still ends with a result"),
    ],
    "plants": {
        "wrong-auto-approve": (["e1"], "lists the read tools in allowed_tools, so they run without the permission callback ever being asked"),
        "wrong-edit-in-readonly": (["e2"], "lets Write and Edit through in read-only mode"),
        "wrong-env-variants": (["e2"], "protects .env but not .env.local or other .env files"),
        "wrong-chaining-allowed": (["e3"], "allows a command that chains another with a semicolon or a pipe when it starts with a safe word"),
        "wrong-danger-no-interrupt": (["e3"], "denies sudo and rm -rf without interrupting the run"),
        "wrong-push-substring": (["e4"], "blocks any command that merely starts a word with git push, such as git pushd"),
        "wrong-no-turn-limit": (["e6"], "leaves max turns unset, so the run is not capped"),
        "wrong-raises-after-error-result": (["e6"], "lets the error that follows an error result escape, so a run that hit its limit has no summary"),
        "wrong-hides-crash": (["e6"], "swallows every error, so a process that died before any result looks like a run that ended"),
        "wrong-status-unmapped": (["e5"], "reports the raw result subtype instead of the course status for the turn limit"),
    },
}
