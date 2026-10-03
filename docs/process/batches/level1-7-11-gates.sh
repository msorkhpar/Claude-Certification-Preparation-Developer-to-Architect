#!/bin/sh
# Gate for Level 1, modules 7 to 11 (apps, roles, policy, exam readiness, mock exam, revision aids).
# Run from the repository root:   sh docs/process/batches/level1-7-11-gates.sh
# 1. the quiz checker's own planted-defect tests, then the quiz checker on modules 1 to 11
#    (pages agree with tests/quiz.json; wording, length, stem-echo and quotation rules; the mock exam against all of Level 1)
# 2. quiz.json files are up to date: rebuilding them from the pages changes nothing
# 3. the flashcard set and the review bank: their planted-defect tests, then the checks
# 4. the example blocks of the pages against examples/ when the container outputs (.survey-out) are present in this
#    worktree; modules 7 to 11 have no code examples, so a worktree without them skips this step and says so
# 5. the exam-map coverage check
# Nothing here starts a container, so no heavy-slot job is needed. Exit status is 0 only when every step passed.
set -u
cd "$(dirname "$0")/../../.." || exit 2
python3 tools/test_check_quiz.py >/dev/null || { echo "check_quiz planted-defect tests failed"; exit 1; }
python3 tools/check_quiz.py || exit 1
before=$(cat exercises/*/tests/quiz.json | sha256sum)
python3 tools/build_quiz_json.py >/dev/null
after=$(cat exercises/*/tests/quiz.json | sha256sum)
[ "$before" = "$after" ] || { echo "quiz.json was out of date and has been rebuilt: review and commit it"; exit 1; }
python3 tools/test_check_revision.py >/dev/null || { echo "check_revision planted-defect tests failed"; exit 1; }
python3 tools/check_revision.py || exit 1
if [ -d .survey-out ]; then python3 tools/check_examples.py || exit 1; else echo "examples: skipped (no .survey-out here; modules 7 to 11 carry no example blocks)"; fi
grep -l '<!-- example:' course/0[7-9]-*/*.md course/1[01]-*/*.md 2>/dev/null && { echo "an example block appeared in modules 7 to 11: run the example check with container outputs"; exit 1; }
python3 tools/check_coverage.py || exit 1
echo "level 1 modules 7 to 11: gate passed"
