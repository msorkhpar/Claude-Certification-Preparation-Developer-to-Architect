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
    "wrong-timeout-too-long": [("    timeout-minutes: 20\n", "    timeout-minutes: 60\n")],
    "wrong-turns-too-many": [("claude_args: --max-turns 8\n", "claude_args: --max-turns 25\n")],
    "wrong-review-no-cancel": {".github/workflows/review.yml": [("cancel-in-progress: true", "cancel-in-progress: false")]},
    "wrong-review-types": {".github/workflows/review.yml": [("[opened, synchronize, ready_for_review, reopened]", "[opened, synchronize]")]},
    "wrong-review-no-plugin": {".github/workflows/review.yml": [('          plugins: "code-review@claude-code-plugins"\n', "")]},
    "wrong-main-allowed": {"CLAUDE.md": [("never commit to `main`", "prefer not to touch `main`")]},
    "wrong-key-in-notes": {"CLAUDE.md": [("- Tests: `make test`.", "- Key for local runs: sk-ant-api03-EXAMPLEKEY12345. Tests: `make test`.")]},
    "wrong-home-path": {"prompts/CHANGELOG.md": [("- triage: add the question label.", "- triage: add the question label (notes in /home/dev/notes).")]},
    "wrong-email-address": {"REVIEW.md": [("- Formatting and import order", "- Ask dev@corp.example.org about formatting and import order")]},
})
