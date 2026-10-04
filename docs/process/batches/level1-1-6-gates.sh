#!/bin/sh
# Gate for Level 1, modules 1 to 6. Run from the repository root:
#   sh docs/process/batches/level1-1-6-gates.sh
# 1. generate the planted wrong solutions (git-ignored, never committed) from the reference solutions by exact replacements, each checked
# 2. check the quizzes (pages agree with tests/quiz.json, key wording and length rules; the checker's own planted-defect tests)
# 3. in ONE heavy-slot job, offline in the runner image (network none): every variant of the module 6
#    practice in Python, TypeScript, Java and Kotlin, and the example programs with their tests
# 4. grade: reference passes, starter fails, every plant fails on an assertion and on its named case
# Exit status is 0 only when every step passed.
set -u
cd "$(dirname "$0")/../../.." || exit 2
IMG=${RUNNER_IMAGE:-93d052f3fc87}      # runner image id (digest sha256:93d052f3fc87...)
HEAVY=${HEAVY_SLOT:-../.heavy-slot/run-heavy.sh}
python3 tools/make_plants.py --modules '^06-' || exit 1
python3 tools/test_check_quiz.py >/dev/null || { echo "check_quiz planted-defect tests failed"; exit 1; }
python3 tools/check_quiz.py || exit 1
python3 tools/build_quiz_json.py >/dev/null && git diff --quiet -- exercises/*/tests/quiz.json || { echo "quiz.json is out of date: run tools/build_quiz_json.py"; exit 1; }
"$HEAVY" ccp-survey tools/run_all_practices.sh "$IMG" > .survey-out/gate-run.txt 2>&1
rc=$?; echo "heavy job rc=$rc"; [ $rc -eq 0 ] || { tail -20 .survey-out/gate-run.txt; exit 1; }
python3 tools/grade_practice.py || exit 1
python3 tools/check_examples.py 'course/0[1-6]-*/*.md'
