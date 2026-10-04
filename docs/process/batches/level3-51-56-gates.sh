#!/bin/sh
# Gate for Level 3, modules 51 to 56 (Architect Foundations, the second batch). Run from the repository root:
#   sh docs/process/batches/level3-45-50-gates.sh
# 1. write the case lists of the six practices (cases.json) and generate the planted wrong solutions (git-ignored, never committed) from the reference solutions
#    (exact replacements, each checked; the shared tools are tools/make_cases.py and tools/make_plants.py)
# 2. check every quiz of Levels 1 to 3 (pages agree with quiz.json; wording, length, stem-echo and quotation rules; no question restates
#    another of the course, across levels) and the checker's own planted-defect tests; quiz.json files are up to date
# 3. check the revision aids of Levels 1 and 2 and the checker's planted-defect tests
# 4. the exam-map coverage check
# 5. in ONE heavy-slot job, offline in the runner image (network none): every variant of the six practices in the languages each has
#    (Python and TypeScript for 51, 55 and 56; Python, TypeScript, Java and Kotlin for 52, 53 and 54), and the example programs of
#    modules 51 to 56 with their tests and printed output
# 6. in a second heavy-slot job, the harness tests
# 7. grade: reference passes, starter fails, every plant fails on an assertion and on its named case
# 8. the example blocks of the pages against examples/ and the outputs the container produced
# Exit status is 0 only when every step passed.
set -u
cd "$(dirname "$0")/../../.." || exit 2
IMG=${RUNNER_IMAGE:-93d052f3fc87}      # runner image id (digest sha256:93d052f3fc87...)
HEAVY=${HEAVY_SLOT:-../.heavy-slot/run-heavy.sh}
mkdir -p .survey-out
export STUDYFORGE_NAMESPACE=studyforge-local
export L2_MODULES='^5[1-6]-' L2_EXAMPLES='^5[1-6]-' L2_EXAMPLE_SPECS='examples/5[1-6]-*/example.json'
python3 tools/make_cases.py --modules "$L2_MODULES" || exit 1
python3 tools/make_plants.py --modules "$L2_MODULES" || exit 1
python3 tools/test_check_quiz.py >/dev/null || { echo "check_quiz planted-defect tests failed"; exit 1; }
python3 tools/check_quiz.py || exit 1
before=$(cat exercises/*/tests/quiz.json | sha256sum)
python3 tools/build_quiz_json.py >/dev/null
after=$(cat exercises/*/tests/quiz.json | sha256sum)
[ "$before" = "$after" ] || { echo "quiz.json was out of date and has been rebuilt: review and commit it"; exit 1; }
python3 tools/test_check_revision.py >/dev/null || { echo "check_revision planted-defect tests failed"; exit 1; }
python3 tools/check_revision.py || exit 1
python3 tools/check_coverage.py || exit 1
"$HEAVY" ccp-survey tools/l2_run_all.sh "$IMG" > .survey-out/gate-run.txt 2>&1
rc=$?; echo "heavy job rc=$rc"; [ $rc -eq 0 ] || { tail -20 .survey-out/gate-run.txt; exit 1; }
"$HEAVY" ccp-survey tools/l2_in_image.sh "$IMG" . "python3 -B -m pytest -q -p no:cacheprovider harness/tests" > .survey-out/gate-harness.txt 2>&1
rc=$?; echo "harness tests rc=$rc"; [ $rc -eq 0 ] || { tail -20 .survey-out/gate-harness.txt; exit 1; }
python3 tools/grade_practices.py || exit 1
python3 tools/check_examples.py 'course/5[1-6]-*/*.md' || exit 1
echo "level 3 modules 51 to 56: gate passed"
