# Planted wrong solutions of module 74-scenario-claude-code-in-ci: exact replacements against the practice's reference (see tools/make_plants.py).
# Runs with PLANTS, X and both() in scope; names defined here are local to this file.

_P74 = {
    "wrong-review-batch": {"ci/pipeline.json": [('"name": "pre-merge-review", "kind": "review", "audience": "waiting", "api": "realtime"', '"name": "pre-merge-review", "kind": "review", "audience": "waiting", "api": "batch"')]},
    "wrong-debt-realtime": {"ci/pipeline.json": [('"name": "debt-report", "kind": "report", "audience": "scheduled", "api": "batch"', '"name": "debt-report", "kind": "report", "audience": "scheduled", "api": "realtime"')]},
    "wrong-single-pass": {"ci/pipeline.json": [('["per-file", "integration"]', '["per-file"]')]},
    "wrong-integration-first": {"ci/pipeline.json": [('["per-file", "integration"]', '["integration", "per-file"]')]},
    "wrong-shared-session": {"ci/pipeline.json": [('"session": "fresh", "context": ["prior_findings"]', '"session": "shared", "context": ["prior_findings"]')]},
    "wrong-no-prior-findings": {"ci/pipeline.json": [('"context": ["prior_findings"]', '"context": []')]},
    "wrong-no-print-flag": {"ci/pipeline.json": [(r'"command": "claude -p \"Review', r'"command": "claude \"Review')]},
    "wrong-no-schema": {"ci/pipeline.json": [(" --json-schema ci/review.schema.json", "")]},
    "wrong-unbounded": {"ci/pipeline.json": [(" --max-turns 8", "")]},
    "wrong-review-bash": {"ci/pipeline.json": [('"tools": ["Read", "Grep", "Glob"]', '"tools": ["Read", "Grep", "Glob", "Bash"]')]},
    "wrong-allowed-edit": {"ci/pipeline.json": [(r'--allowedTools \"Read,Grep,Glob\"', r'--allowedTools \"Read,Grep,Glob,Edit\"')]},
    "wrong-severity-free-text": {"ci/review.schema.json": [('"severity": {"type": "string", "enum": ["high", "medium", "low"]}', '"severity": {"type": "string"}')]},
    "wrong-no-suggestion": {"ci/review.schema.json": [('"required": ["file", "line", "severity", "category", "issue", "suggestion"]', '"required": ["file", "line", "severity", "category", "issue"]')]},
    "wrong-no-skip-list": {"ci/review-criteria.md": [("## Skip\n", "## Ignore\n")]},
    "wrong-no-example": {"ci/review-criteria.md": [(" Example: a query built by joining the user's text into the SQL string.", "")]},
    "wrong-vague-criteria": {"ci/review-criteria.md": [("Review only the lines", "Be conservative. Review only the lines")]},
    "wrong-prompt-no-existing": {"ci/testgen-prompt.md": [("{{existing_tests}}", "(none)")]},
    "wrong-prompt-no-useful": {"ci/testgen-prompt.md": [("## A useful test\n", "## Tests\n")]},
    "wrong-home-path": {"ci/review-criteria.md": [("# Review criteria\n", "# Review criteria\n\nNotes live in /home/dev/notes.\n")]},
}

PLANTS[f"{X}/74-scenario-claude-code-in-ci/unit-01/practice-1"] = {l: ("ci/pipeline.json", _P74) for l in ("python", "typescript", "java", "kotlin")}
