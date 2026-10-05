# Planted wrong solutions of module 56-the-built-in-tools: exact replacements against the practice's reference (see tools/make_plants.py).
# Runs with PLANTS, X and both() in scope; names defined here are local to this file.

_P56 = {
    "wrong-no-src-deny": {".claude/settings.json": [('"deny": ["Read(./.env)", "Read(secrets/**)", "Edit(src/**)"]', '"deny": ["Read(./.env)", "Read(secrets/**)"]')]},
    "wrong-bash-bare": {".claude/settings.json": [('"Bash(git log *)", "Bash(git diff *)", "Bash(git status)"', '"Bash"')]},
    "wrong-notes-everywhere": {".claude/settings.json": [('"Edit(notes/**)"', '"Edit(**)"')]},
    "wrong-write-rule": {".claude/settings.json": [('"Edit(notes/**)"', '"Write(notes/**)"')]},
    "wrong-no-env-deny": {".claude/settings.json": [('"Read(./.env)", ', "")]},
    "wrong-grep-rule": {".claude/settings.json": [('"Read(secrets/**)"', '"Grep(secrets/**)"')]},
    "wrong-agent-edit": {".claude/agents/explorer.md": [("tools: Read, Grep, Glob", "tools: Read, Grep, Glob, Edit")]},
    "wrong-agent-bash": {".claude/agents/explorer.md": [("tools: Read, Grep, Glob", "tools: Read, Grep, Glob, Bash")]},
    "wrong-agent-no-use-when": {".claude/agents/explorer.md": [("Use when a question is about", "For questions about")]},
    "wrong-agent-unbounded": {".claude/agents/explorer.md": [("maxTurns: 12\n", "")]},
    "wrong-options-no-search": {"agent-options.json": [('"tools": ["Read", "Grep", "Glob"],', '"tools": ["Read"],')]},
    "wrong-options-allow-bash": {"agent-options.json": [('"allowedTools": ["Read", "Grep", "Glob"]', '"allowedTools": ["Read", "Grep", "Glob", "Bash"]')]},
    "wrong-options-no-disallow": {"agent-options.json": [('"disallowedTools": ["Bash", "Edit", "Write"]', '"disallowedTools": ["Bash"]')]},
    "wrong-plan-read-first": {"docs/exploration-plan.md": [("1. Find the entry points with Grep: search for", "1. Read the entry points in full: look at")]},
    "wrong-plan-read-all": {"docs/exploration-plan.md": [("Do not read every file first. Build", "Read every file first. Build")]},
    "wrong-no-export-step": {"docs/exploration-plan.md": [("list the names each wrapper exports, then search the repository for each exported name with Grep.", "search the repository for the function name with Grep.")]},
    "wrong-fallback-rewrite-first": {"docs/exploration-plan.md": [("1. If Edit says the text appears more than once, repeat it with more surrounding lines until it is unique.", "1. If Edit says the text appears more than once, Read the whole file and Write it back with the change."), ("3. If no unique anchor exists, Read the whole file and Write it back with the change.", "3. If that does not help, repeat it with more surrounding lines until it is unique.")]},
    "wrong-fallback-no-replace-all": {"docs/exploration-plan.md": [("2. If every occurrence should change, use replace_all.", "2. If every occurrence should change, run the edit once for each of them.")]},
    "wrong-home-path": {"docs/exploration-plan.md": [("Do not read every file first.", "Do not read every file first (the clone is in /home/dev/inventory).")]},
}

PLANTS[f"{X}/56-the-built-in-tools/unit-01/practice-1"] = both(".claude/settings.json", _P56)
