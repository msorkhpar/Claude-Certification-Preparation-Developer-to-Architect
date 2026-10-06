# Hand-back: cloud round cloud-r6-split

## Task 1: module 35 try-it (commit 95bb3dc)
- Both try-it files copy `fake_claude.py` into the scratch project and `chmod 0755` the copy (works on a read-only harness).
- Reproduced first with the stand-in's exec bit cleared: `raised: CLIConnectionError ... [Errno 13] Permission denied`.
- With the bit still cleared, after the fix: `make_tryit.py run <practice> python reference` exit 0, `status: done`,
  `tools used: ['Read', 'Bash']`, `DEBUG decide input: 'Read' {'file_path': 'a.txt'}`.
- `... python starter` exit 0, prints `raised: ProcessError ...`. No crash. The SDK logs a traceback for the starter's missing
  `can_use_tool` (TODO 5), which is the gap the learner fills.
- TypeScript: the TS SDK isn't installed here, so it was run with a stub `agent.ts` against a non-executable stand-in.
  The copy passed `X_OK` and launched.

## Task 2: the split
- `process` = 95bb3dc (everything). `cloud/learner-main` = 95bb3dc + aaab9cd (one commit).
- Removed 282 paths: `CLAUDE.md` 1, `.claude/` 6, `docs/process/` 64, `docs/SETUP.md` 1, `tests/` 1, top-level `tools/` 49,
  `tools/plants.d/` 76, `tools/cases.d/` 75, `tools/hooks/` 3, `tools/jvm-sdk/` 5, `tools/npm-sdk/` 1. No learner `CLAUDE.md`.
- Links changed (learner branch):
  - `course/README.md`: `tools/check_examples.py --fill` → "the build"; `tools/check_quiz.py` (2×) → "the quiz check";
    `tools/check_revision.py` / `tools/test_check_revision.py` → "the course's revision check ... its own tests".
  - `docs/EXAM-MAP.md`: `tools/check_coverage.py` → "the course's coverage check".
  - `docs/IDEA.md`: dropped the `docs/process/` layout row; `docs/` row now reads "what the course is, what it covers and what it was checked on".
  - `.gitignore`: comment `tools/make_plants.py` → "the authoring tools".
- Checks on the learner tree (tools borrowed from `process`, untracked, then removed): check_quiz 94 modules ok;
  check_coverage 0 problems; check_revision ok; check_logger 788 files, 0 missing; check_personal_data `--modules '.*'`
  3668 files, 0 findings; make_tryit check 62 practices, 0 problems. All exit 0.

## For the maintainer
- The learner branch has no authoring checks, so `course/README.md` describes checks a learner cannot run. The split's open items
  (`check_*.py` on main, `make_harness.py`) are still open.
- `.gitignore` keeps its `tools/jvm-sdk/` entries. They're harmless and protect a checkout that brings `process` tools back.
