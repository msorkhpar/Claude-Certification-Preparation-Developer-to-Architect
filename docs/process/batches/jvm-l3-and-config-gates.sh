#!/bin/sh
# Gate for the Java and Kotlin editions of the Level 3 examples (modules 45 to 62) and of the configuration practices that had no JVM
# edition (modules 38, 39, 40, 55, 56, 57, 58 and 60). Run from the repository root:
#   sh docs/process/batches/jvm-l3-and-config-gates.sh
# 1. write the case lists (cases.json) and generate the planted wrong solutions (git-ignored, never committed) from the reference solutions
#    (exact replacements, each checked) for the touched practices
# 2. check every quiz and the revision aids (pages agree with quiz.json, checker tests), and the exam-map coverage
# 3. in ONE heavy-slot job, offline in the runner image (network none): every variant of every touched practice in every language it has
#    (Python, TypeScript, Java and Kotlin), then every example in Python and TypeScript with its tests and output, and the Java and Kotlin editions
#    of every example in one Gradle build (modules 47, 49 and 51 need the Agent SDK and stay Python and TypeScript)
# 4. in a second heavy-slot job, the harness tests of the Python harness (the JVM harness is tested by the Gradle job above)
# 5. grade: the reference passes, the starter fails, every plant fails on an assertion and on its named case
# 6. every example test passed in every language; every Java and Kotlin output equals the Python output or is explained
# 7. the example blocks of every page against examples/ and the outputs the container produced (one tab per language)
# Exit status is 0 only when every step passed.
set -u
cd "$(dirname "$0")/../../.." || exit 2
IMG=${RUNNER_IMAGE:-93d052f3fc87}      # runner image id (digest sha256:93d052f3fc87...)
HEAVY=${HEAVY_SLOT:-../.heavy-slot/run-heavy.sh}
mkdir -p .survey-out
export L2_MODULES=${L3_PRACTICES:-'^(38|39|40|55|56|57|58|60)-'} L2_JVM_EXAMPLES=1
export L2_EXAMPLE_SPECS=${L3_EXAMPLES:-'examples/*/example.json'}
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
bad=$(grep -E '^example .* (tests|run) rc=[1-9]|^jvm examples .* rc=[1-9]' .survey-out/gate-run.txt)
[ -z "$bad" ] || { echo "$bad"; exit 1; }
"$HEAVY" ccp-survey tools/l2_in_image.sh "$IMG" . "python3 -B -m pytest -q -p no:cacheprovider harness/tests" > .survey-out/gate-harness.txt 2>&1
rc=$?; echo "harness tests rc=$rc"; [ $rc -eq 0 ] || { tail -20 .survey-out/gate-harness.txt; exit 1; }
python3 tools/grade_practices.py || exit 1
python3 tools/compare_example_outputs.py || exit 1
python3 tools/check_examples.py || exit 1
echo "JVM editions of the Level 3 examples and of the configuration practices: gate passed"
