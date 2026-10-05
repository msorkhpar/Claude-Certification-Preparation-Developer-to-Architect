# Planted wrong solutions of module 40-claude-in-the-software-life-cycle: exact replacements against the practice's reference (see tools/make_plants.py).
# Runs with PLANTS, X and both() in scope; names defined here are local to this file.

PLANTS[f"{X}/40-claude-in-the-software-life-cycle/unit-01/practice-1"] = both(".github/workflows/claude.yml", {
    "wrong-mention-unguarded": [("    if: contains(github.event.comment.body, '@claude')\n", "")],
    "wrong-mention-beta": [("claude-code-action@v1", "claude-code-action@beta")],
    "wrong-review-writes": {".github/workflows/review.yml": [("      contents: read\n", "      contents: write\n")]},
    "wrong-review-no-comment": {".github/workflows/review.yml": [("/code-review:code-review --comment ", "/code-review:code-review ")]},
    "wrong-review-no-checkout": {".github/workflows/review.yml": [("      - uses: actions/checkout@v6\n        with:\n          fetch-depth: 1\n", "")]},
    "wrong-prompt-version-mismatch": {"prompts/CHANGELOG.md": [("## 1.2.0\n\n- triage: ask for one sentence of reasoning.\n\n", "")]},
    "wrong-prompt-no-version": {"prompts/triage.md": [("version: 1.2.0\n", "")]},
    "wrong-review-no-skip": {"REVIEW.md": [("## Skip", "## Ignore")]},
    "wrong-no-timeout": [("    timeout-minutes: 20\n", "")],
    "wrong-no-turn-cap": [("          claude_args: --max-turns 8\n", "")],
    "wrong-literal-key": [("${{ secrets.ANTHROPIC_API_KEY }}", "sk-ant-api03-EXAMPLEKEY12345")],
})
