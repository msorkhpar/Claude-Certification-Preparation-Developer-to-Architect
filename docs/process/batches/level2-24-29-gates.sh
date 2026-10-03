#!/bin/sh
# Gate for Level 2, modules 24 to 29. Run from the repository root:
#   sh docs/process/batches/level2-24-29-gates.sh
# 1. regenerate the planted wrong solutions from the reference solutions (exact replacements, each checked)
# 2. check the quizzes (pages agree with tests/quiz.json; wording, length, stem-echo and quotation rules)
#    and the checker's own planted-defect tests; quiz.json files are up to date
# 3. the exam-map coverage check
# 4. in ONE heavy-slot job, offline in the runner image (network none): every variant of the four practices (modules 25, 26, 28 and 29; modules 24 and 27 have none) in Python,
#    TypeScript, Java and Kotlin, and the example programs with their tests and printed output
# 5. in a second heavy-slot job, the harness tests
# 6. grade: reference passes, starter fails, every plant fails on an assertion and on its named case
# 7. the example blocks of the pages against examples/ and the outputs the container produced
# Exit status is 0 only when every step passed.
set -u
cd "$(dirname "$0")/../../.." || exit 2
IMG=${RUNNER_IMAGE:-93d052f3fc87}      # runner image id (digest sha256:93d052f3fc87...)
HEAVY=${HEAVY_SLOT:-../.heavy-slot/run-heavy.sh}
mkdir -p .survey-out
export L2_MODULES='^2[4-9]-' L2_EXAMPLES='2[4-9]-' L2_EXAMPLE_SPECS='examples/2[4-9]-*/example.json'
python3 tools/make_plants_l2c.py || exit 1
python3 tools/test_check_quiz.py >/dev/null || { echo "check_quiz planted-defect tests failed"; exit 1; }
python3 tools/check_quiz.py || exit 1
before=$(cat exercises/*/tests/quiz.json | sha256sum)
python3 tools/build_quiz_json.py >/dev/null
after=$(cat exercises/*/tests/quiz.json | sha256sum)
[ "$before" = "$after" ] || { echo "quiz.json was out of date and has been rebuilt: review and commit it"; exit 1; }
python3 tools/check_coverage.py || exit 1
"$HEAVY" ccp-survey tools/l2_run_all.sh "$IMG" > .survey-out/gate-run.txt 2>&1
rc=$?; echo "heavy job rc=$rc"; [ $rc -eq 0 ] || { tail -20 .survey-out/gate-run.txt; exit 1; }
"$HEAVY" ccp-survey tools/l2_in_image.sh "$IMG" . "python3 -B -m pytest -q -p no:cacheprovider harness/tests" > .survey-out/gate-harness.txt 2>&1
rc=$?; echo "harness tests rc=$rc"; [ $rc -eq 0 ] || { tail -20 .survey-out/gate-harness.txt; exit 1; }
python3 tools/grade_practices.py || exit 1
python3 tools/check_examples.py 'course/2[4-9]-*/*.md' || exit 1
echo "level 2 modules 24 to 29: gate passed"
