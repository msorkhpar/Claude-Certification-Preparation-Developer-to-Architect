# Planted wrong solutions of module 73-scenario-developer-productivity: exact replacements against the practice's reference (see tools/make_plants.py).
# Runs with PLANTS, X and both() in scope; names defined here are local to this file.

_P73 = {
    "wrong-settings-ghost": {".claude/settings.json": [('"mcp__tickets__get_ticket"]', '"mcp__ticket__get_ticket"]')]},
    "wrong-literal-token": {".mcp.json": [("Bearer ${TICKETS_TOKEN}", "Bearer <token>")]},
    "wrong-token-default": {".mcp.json": [("Bearer ${TICKETS_TOKEN}", "Bearer ${TICKETS_TOKEN:-changeme}")]},
    "wrong-explorer-bash": {".claude/agents/explorer.md": [("tools: Read, Grep, Glob, mcp__docs__search", "tools: Read, Grep, Glob, Bash, mcp__docs__search")]},
    "wrong-explorer-inherits": {".claude/agents/explorer.md": [("tools: Read, Grep, Glob, mcp__docs__search\n", "")]},
    "wrong-explorer-description": {".claude/agents/explorer.md": [("description: Use when a question is about", "description: Explains how a question is about")]},
    "wrong-scaffolder-bash": {".claude/agents/scaffolder.md": [("tools: Read, Glob, Edit, Write", "tools: Read, Glob, Edit, Write, Bash")]},
    "wrong-write-wide": {".claude/settings.json": [('"Edit(src/generated/**)", ', '"Edit(src/generated/**)", "Write(src/**)", ')]},
    "wrong-tickets-wildcard": {".claude/settings.json": [('"mcp__tickets__get_ticket"]', '"mcp__tickets__*"]')]},
    "wrong-no-delete-deny": {".claude/settings.json": [(', "mcp__tickets__delete_ticket"', "")]},
    "wrong-no-env-deny": {".claude/settings.json": [('"Read(./.env)", ', "")]},
    "wrong-bash-allow": {".claude/settings.json": [('"mcp__tickets__get_ticket"]', '"mcp__tickets__get_ticket", "Bash"]')]},
    "wrong-note-missing-variable": {"docs/team-setup.md": [("- `TICKETS_URL`: the ticket server's address (optional, `.mcp.json` sets a default)\n", "")]},
    "wrong-rewritten-resumes": {"docs/team-setup.md": [("rewritten since the last session | fresh |", "rewritten since the last session | resume |")]},
    "wrong-home-path": {"docs/team-setup.md": [("# Team setup\n", "# Team setup\n\nNotes live in /home/dev/notes.\n")]},
}

PLANTS[f"{X}/73-scenario-developer-productivity/unit-01/practice-1"] = {l: (".mcp.json", _P73) for l in ("python", "typescript", "java", "kotlin")}
