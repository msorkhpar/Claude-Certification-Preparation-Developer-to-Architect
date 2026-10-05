# Planted wrong solutions of module 71-scenario-code-generation-with-claude-code: exact replacements against the practice's reference (see tools/make_plants.py).
# Runs with PLANTS, X and both() in scope; names defined here are local to this file.

_P71 = {
    "wrong-docs-unscoped": {".claude/rules/docs.md": [('---\npaths:\n  - "docs/**/*.md"\n---\n\n', "")]},
    "wrong-handlers-wide": {".claude/rules/handlers.md": [('  - "server/handlers/**/*.ts"', '  - "**/*.ts"')]},
    "wrong-tests-folder": {".claude/rules/tests.md": [('  - "**/*.spec.ts"\n  - "**/*.spec.tsx"\n', '  - "src/ui/**/*.spec.tsx"\n')]},
    "wrong-tests-ts-only": {".claude/rules/tests.md": [('  - "**/*.spec.tsx"\n', "")]},
    "wrong-root-keeps-hooks": {"CLAUDE.md": [("- Run `npm test` before finishing a task.\n", "- Run `npm test` before finishing a task.\n- Write function components that use hooks.\n")]},
    "wrong-root-long": {"CLAUDE.md": [("# Dispatch app\n", "# Dispatch app\n" + "- Background note: kept for history, it changes no behaviour.\n" * 30)]},
    "wrong-review-bare-bash": {".claude/commands/review.md": [("allowed-tools: Read Grep Glob Bash(git diff *)", "allowed-tools: Read Grep Glob Bash")]},
    "wrong-review-no-description": {".claude/commands/review.md": [("description: Review the current changes against the team checklist\n", "")]},
    "wrong-no-env-deny": {".claude/settings.json": [('"deny": ["Read(./.env)"]', '"deny": []')]},
    "wrong-bash-allow": {".claude/settings.json": [('"allow": ["Bash(npm test)"]', '"allow": ["Bash"]')]},
    "wrong-monolith-direct": {"docs/working-modes.md": [("across dozens of files | plan |", "across dozens of files | direct |")]},
    "wrong-typo-plan": {"docs/working-modes.md": [("| Fix a typo in an error message | direct |", "| Fix a typo in an error message | plan |")]},
    "wrong-home-path": {"CLAUDE.md": [("# Dispatch app\n", "# Dispatch app\n\n- My notes are in /home/dev/notes.\n")]},
}

PLANTS[f"{X}/71-scenario-code-generation-with-claude-code/unit-01/practice-1"] = {l: ("CLAUDE.md", _P71) for l in ("python", "typescript", "java", "kotlin")}
