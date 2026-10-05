# Planted wrong solutions of module 38-claude-code-for-developers: exact replacements against the practice's reference (see tools/make_plants.py).
# Runs with PLANTS, X and both() in scope; names defined here are local to this file.

PLANTS[f"{X}/38-claude-code-for-developers/unit-01/practice-1"] = both("CLAUDE.md", {
    "wrong-bloated-memory": {"CLAUDE.md": [("## Gotchas", "## Notes\n\n" + "- keep this in mind\n" * 205 + "\n## Gotchas")]},
    "wrong-no-import": {"CLAUDE.md": [("See @docs/architecture.md for", "See docs/architecture.md for")]},
    "wrong-push-ask-only": {".claude/settings.json": [('"Bash(git push *)",\n      "Bash(curl *)"', '"Bash(curl *)"'), ('"Bash(git commit *)"\n', '"Bash(git commit *)",\n      "Bash(git push *)"\n')]},
    "wrong-commit-allowed": {".claude/settings.json": [('"Bash(git log *)"\n    ],\n    "ask": [\n      "Bash(git commit *)"\n    ],', '"Bash(git log *)",\n      "Bash(git commit *)"\n    ],\n    "ask": [],')]},
    "wrong-env-readable": {".claude/settings.json": [('"Read(./.env)",\n      ', "")]},
    "wrong-write-rule": {".claude/settings.json": [('"Bash(curl *)"\n', '"Bash(curl *)",\n      "Write(./notes/**)"\n')]},
    "wrong-extra-permission-key": {".claude/settings.json": [('"defaultMode": "acceptEdits",', '"defaultMode": "acceptEdits",\n    "additionalDirectories": ["../shared"],')]},
    "wrong-local-not-ignored": {".gitignore": [(".claude/settings.local.json\n", "")]},
    "wrong-local-md-not-ignored": {".gitignore": [("CLAUDE.local.md\n", "")]},
    "wrong-local-env": {".claude/settings.local.json": [('"model": "sonnet"', '"model": "sonnet",\n  "env": {"LOG_LEVEL": "debug"}')]},
    "wrong-skill-auto": {".claude/skills/fix-issue/SKILL.md": [("disable-model-invocation: true\n", "")]},
    "wrong-skill-no-hint": {".claude/skills/fix-issue/SKILL.md": [("argument-hint: [issue-number]\n", "")]},
    "wrong-headless-bypass": {"scripts/ci-review.sh": [("--permission-mode dontAsk", "--dangerously-skip-permissions")]},
    "wrong-headless-unbounded": {"scripts/ci-review.sh": [("  --max-turns 5 \\\n", "")]},
    "wrong-headless-no-budget": {"scripts/ci-review.sh": [("  --max-budget-usd 1 \\\n", "")]},
    "wrong-headless-bash-tool": {"scripts/ci-review.sh": [('"Read,Bash(git diff *),Bash(git log *)"', '"Read,Bash"')]},
    "wrong-key-in-script": {"scripts/ci-review.sh": [("set -eu\n", "set -eu\nexport ANTHROPIC_API_KEY=sk-ant-api03-EXAMPLEKEY123\n")]},
    "wrong-path-in-memory": {"CLAUDE.md": [("## Gotchas\n", "## Gotchas\n\n- The checkout lives in /home/dev/invoice-api.\n")]},
})
