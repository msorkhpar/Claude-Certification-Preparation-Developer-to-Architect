# Guard hooks

`guard.py` is a Claude Code PreToolUse hook (matcher `Bash|Write|Edit`, set in
`.claude/settings.json`). It checks each tool call against the repository rules: docker
without `--context desktop-linux`, `docker context use|login|push`, `git push`, `gh`,
`-n auto`, heavy commands (`pytest`, `gradle`, `gradlew`, `mvn`, `docker run|exec|build|compose`)
outside `run-heavy.sh <kind>` while `HEAVY_SLOT_HELD` is unset, `kill`/`pkill`/`killall`, and
personal data in written text (an email other than example.com, example.invalid or
noreply@anthropic.com; a `/home/<user>/` or `/Users/<user>/` path; the machine name, read from
the environment at run time).

## Modes (`STUDYFORGE_GUARD_MODE`)

- `log` (default): record what would be denied in `<repo parent>/.register-logs/guard-log.tsv`
  (UTC time, agent type, tool, rule id, 120-character excerpt with personal data replaced by
  `<redacted>`) and allow. An internal error logs rule `guard-error` and allows.
- `deny`: block with exit 2 and a one-line fix on stderr. An internal error blocks (fail closed).

Known limit: the hook sees only the command string, so `sh -c` wrappers escape it; the slot
script's own memory check stays the second layer.

## Test

`python3 tools/hooks/test_guard.py` (unittest, milliseconds, no docker).

## Rollback

Delete the `hooks` block from `.claude/settings.json`. Nothing else depends on it.
