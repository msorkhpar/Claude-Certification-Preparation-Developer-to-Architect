# Process split

Which tracked paths move to the `process` branch before release, and which stay on `main` for
learners. Nothing has moved yet; nothing is deleted when it does. Rule (from `CLAUDE.md`): `main`
holds only what a learner needs; process material is everything about how the course is made,
checked and delegated.

## Moves to `process`

| Path | Why |
|---|---|
| `CLAUDE.md` | How to work in the repository |
| `.claude/agents/`, `.claude/skills/`, `.claude/settings.json` | The offices, the gate-and-merge skill and the guard-hook settings of the authoring sessions |
| `docs/process/` (all: board, briefs, production notes, feasibility, quiz polish, batch gate scripts, rounds and hand-backs, this file) | Planning and working notes |
| `docs/SETUP.md` | How the project is built and run by its authors (register, offices, heavy-job slots, captures, branches) |
| `tests/test_check_coverage.py` | Test of an authoring check |
| `tools/hooks/` | Guard hook for the authoring sessions |
| `tools/survey_*.sh` (10 files) | Feasibility surveys |
| `tools/gate.sh`, `tools/gate_report.py`, `tools/test_gate.py`, `tools/test_gate.sh`, `tools/validate_handback.py` | The gate and the hand-back validator |
| `tools/round_check.py`, `tools/test_round_check.py`, `tools/usage_ledger.py`, `tools/test_usage_ledger.py`, `tools/practice_effort.py` | Round, usage and effort bookkeeping |
| `tools/run_practice.sh`, `tools/run_all_practices.sh`, `tools/grade_practice.py`, `tools/grade_practices.py`, `tools/l2_*.sh`, `tools/l2_practices.py` | Proof runs of practices and JVM examples in the heavy-job slots |
| `tools/check_coverage.py`, `tools/check_examples.py`, `tools/check_logger.py`, `tools/check_personal_data.py`, `tools/check_quiz.py`, `tools/check_revision.py`, `tools/compare_example_outputs.py`, `tools/test_check_quiz.py`, `tools/test_check_revision.py` | Authoring checks |
| `tools/make_plants.py`, `tools/plants.d/` | Planted wrong solutions: proofs that the tests catch mistakes, not learner material |
| `tools/make_cases.py`, `tools/cases.d/`, `tools/modular_data.py`, `tools/test_modular_data.py`, `tools/build_quiz_json.py`, `tools/balance_keys.py`, `tools/make_tryit.py` | Generators of committed files (`cases.json`, `quiz.json`, try-it files) and quiz-key balancing |
| `tools/npm-sdk/`, `tools/jvm-sdk/` | SDK pin manifests used by the surveys and cache preparation |

## Stays on `main`

| Path | Why |
|---|---|
| `README.md`, `.gitignore` | The learner's entry point |
| `course/` | The lessons, quizzes and mock exams |
| `examples/` | The examples each page shows and runs |
| `exercises/` | The practices: statements, starters, try-it files, tests, reference solutions, `cases.json`, `quiz.json` and revision aids |
| `harness/` | The stand-in for the API that examples and practices import (replay, scripted model, Claude Code stand-in, JVM and TypeScript editions, its tests, and the capture and scrub tools a reader with a key can use) |
| `tools/harness/`, `tools/make_harness.py` | The run-output harness a build drops into each practice so Run and Submit report prints, logs and per-case failures |
| `docs/GOAL.md`, `docs/IDEA.md`, `docs/COURSE-OUTLINE.md`, `docs/EXAM-MAP.md`, `docs/VERSIONS.md` | What the course is, what it covers, how it maps to the exams and what it was checked on |

## Open for the owner

- `tools/make_harness.py` and `tools/harness/` stay only if the release build still assembles
  practices from `main`; if the published images carry the assembled practices, they move too.
- The checks in `tools/check_*.py` could stay on `main` if learners are meant to contribute fixes.
