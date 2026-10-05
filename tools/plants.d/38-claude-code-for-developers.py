# Planted wrong solutions of module 38-claude-code-for-developers: exact replacements against the practice's reference (see tools/make_plants.py).
# Runs with PLANTS, X and both() in scope; names defined here are local to this file.

PLANTS[f"{X}/38-claude-code-for-developers/unit-01/practice-1"] = both("CLAUDE.md", {
    "wrong-bypass-mode": {".claude/settings.json": [('"defaultMode": "acceptEdits"', '"defaultMode": "bypassPermissions"')]},
    "wrong-write-deny": {".claude/settings.json": [('"Read(./.env)"', '"Write(./.env)"'), ('"Read(./secrets/**)"', '"Write(./secrets/**)"')]},
    "wrong-push-ask-only": {".claude/settings.json": [('"Bash(git push *)",\n      "Bash(curl *)"', '"Bash(curl *)"'), ('"Bash(git commit *)"\n', '"Bash(git commit *)",\n      "Bash(git push *)"\n')]},
    "wrong-bare-bash-allow": {".claude/settings.json": [('"Bash(make test)",', '"Bash",\n      "Bash(make test)",')]},
    "wrong-local-not-ignored": {".gitignore": [(".claude/settings.local.json\n", "")]},
    "wrong-local-widens": {".claude/settings.local.json": [('"model": "sonnet"', '"model": "sonnet",\n  "permissions": {"allow": ["Bash(make deploy)"]}')]},
    "wrong-bloated-memory": {"CLAUDE.md": [("## Gotchas", "## Notes\n\n" + "- keep this in mind\n" * 205 + "\n## Gotchas")]},
    "wrong-no-import": {"CLAUDE.md": [("See @docs/architecture.md for", "See docs/architecture.md for")]},
    "wrong-skill-auto": {".claude/skills/fix-issue/SKILL.md": [("disable-model-invocation: true\n", "")]},
    "wrong-headless-bypass": {"scripts/ci-review.sh": [("--permission-mode dontAsk", "--dangerously-skip-permissions")]},
    "wrong-headless-unbounded": {"scripts/ci-review.sh": [("  --max-turns 5 \\\n", "")]},
    "wrong-key-in-script": {"scripts/ci-review.sh": [("set -eu\n", "set -eu\nexport ANTHROPIC_API_KEY=sk-ant-api03-EXAMPLEKEY123\n")]},
})
