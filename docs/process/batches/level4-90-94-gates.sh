#!/bin/sh
# Gate for Level 4 (Architect Professional), modules 90 to 94 (batch L4-C). Run from the repository root:
#   sh docs/process/batches/level4-90-94-gates.sh
# 1. write the case lists of the four practices (cases.json) and generate the planted wrong solutions (git-ignored, never committed) from the reference solutions
#    (exact replacements, each checked; the shared tools are tools/make_cases.py and tools/make_plants.py)
# 2. check every quiz of the course (pages agree with quiz.json; wording, length, stem-echo and quotation rules; no question restates
#    another of the course, across levels) and the checker's own planted-defect tests; quiz.json files are up to date
# 3. check the revision aids and the checker's planted-defect tests
# 4. the exam-map coverage check
# 5. in ONE heavy-slot job, offline in the runner image (network none): every variant of the four practices in Python, TypeScript, Java and Kotlin,
#    and the Python and TypeScript examples of modules 90 to 93 with their tests and printed output
# 6. in a second heavy-slot job, the Java and Kotlin editions of those four examples (Gradle, offline): tests, then printed output
# 7. in a third heavy-slot job, the harness tests
# 8. grade: reference passes, starter fails, every plant fails on an assertion and on its named case
# 9. every Java and Kotlin output equals the Python output (tools/compare_example_outputs.py), and the example blocks of the pages
#    match examples/ and the outputs the container produced (one tab per language)
# Exit status is 0 only when every step passed.
set -u
cd "$(dirname "$0")/../../.." || exit 2
IMG=${RUNNER_IMAGE:-93d052f3fc87}      # runner image id (digest sha256:93d052f3fc87...)
HEAVY=${HEAVY_SLOT:-../.heavy-slot/run-heavy.sh}
mkdir -p .survey-out
export STUDYFORGE_NAMESPACE=studyforge-local
export L2_MODULES='^9[0-4]-' L2_EXAMPLES='^9[0-3]-' L2_EXAMPLE_SPECS='examples/9[0-3]-*/example.json'
TASKS=""
for d in 90-control-chain 91-tradeoff-brief 92-policy-resolver 93-claims-assistant; do
  for l in java kotlin; do TASKS="$TASKS :$d:$l:test :$d:$l:runExample"; done
done
python3 tools/make_cases.py --modules '^9[0-3]-' || exit 1
python3 tools/make_plants.py --modules '^9[0-3]-' || exit 1
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
"$HEAVY" ccp-survey sh -c "tools/l2_prepare_jvm_examples.sh $IMG && tools/l2_run_jvm_examples.sh $IMG $TASKS" > .survey-out/gate-jvm.txt 2>&1
rc=$?; echo "jvm job rc=$rc"; [ $rc -eq 0 ] || { tail -20 .survey-out/gate-jvm.txt; exit 1; }
"$HEAVY" ccp-survey tools/l2_in_image.sh "$IMG" . "python3 -B -m pytest -q -p no:cacheprovider harness/tests" > .survey-out/gate-harness.txt 2>&1
rc=$?; echo "harness tests rc=$rc"; [ $rc -eq 0 ] || { tail -20 .survey-out/gate-harness.txt; exit 1; }
python3 tools/grade_practices.py || exit 1
for p in 90- 91- 92- 93-; do python3 tools/compare_example_outputs.py "$p" || exit 1; done
python3 tools/check_examples.py 'course/9[0-4]-*/*.md' || exit 1
echo "level 4 modules 90 to 94: gate passed"
