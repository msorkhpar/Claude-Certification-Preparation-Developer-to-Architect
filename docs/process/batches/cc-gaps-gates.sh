#!/bin/sh
# Gate for the Claude Code coverage batch: module 27 (the scenario bank and practice extended with loops, routines, tasks, monitors, goals, headless runs and
# personal settings), module 39 (page 5 and a personal-setup practice) and module 60 (page 3 and a rhythm practice). Run from the repository root:
#   sh docs/process/batches/cc-gaps-gates.sh
# 1. write the case lists (cases.json) of the touched practices and generate their planted wrong solutions (git-ignored, never committed) from the
#    reference solutions (exact replacements, each checked; the shared tools are tools/make_cases.py and tools/make_plants.py)
# 2. check every quiz of the course (pages agree with quiz.json; wording, length, stem-echo and quotation rules; no question restates another) and the
#    checker's own planted-defect tests; quiz.json files are up to date; the revision aids and the exam-map coverage
# 3. in ONE heavy-slot job, offline in the runner image (network none): every variant of the practices of modules 27, 39 and 60 in Python, TypeScript,
#    Java and Kotlin, then the examples of modules 39 and 60 (regression: this batch does not change them) in every language with tests and printed output
# 4. in a second heavy-slot job, the harness tests
# 5. grade: the reference passes, the starter fails, every plant fails on an assertion and on its named case
# 6. every Java and Kotlin example output equals the Python output or is explained; the example blocks of the pages of modules 27, 39 and 60 against
#    examples/ and the outputs the container produced
# Exit status is 0 only when every step passed.
set -u
cd "$(dirname "$0")/../../.." || exit 2
IMG=${RUNNER_IMAGE:-93d052f3fc87}      # runner image id (digest sha256:93d052f3fc87...)
HEAVY=${HEAVY_SLOT:-../.heavy-slot/run-heavy.sh}
mkdir -p .survey-out
export STUDYFORGE_NAMESPACE=studyforge-local
export L2_MODULES='^(27|39|60)-' L2_JVM_EXAMPLES=1 L2_EXAMPLES='^(39|60)-'
export L2_EXAMPLE_SPECS='examples/39-*/example.json examples/60-*/example.json'
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
# the offline caches the Java and Kotlin editions need (the practices run before the example warm-up in l2_run_all.sh, so warm them first; idempotent)
"$HEAVY" ccp-survey sh -c "tools/l2_prepare_caches.sh $IMG && tools/l2_prepare_gradle.sh $IMG && tools/l2_prepare_jvm_examples.sh $IMG" > .survey-out/gate-prepare.txt 2>&1
rc=$?; echo "cache preparation rc=$rc"; [ $rc -eq 0 ] || { tail -20 .survey-out/gate-prepare.txt; exit 1; }
"$HEAVY" ccp-survey tools/l2_run_all.sh "$IMG" > .survey-out/gate-run.txt 2>&1
rc=$?; echo "heavy job rc=$rc"; [ $rc -eq 0 ] || { tail -20 .survey-out/gate-run.txt; exit 1; }
bad=$(grep -E '^example .* (tests|run) rc=[1-9]|^jvm examples .* rc=[1-9]' .survey-out/gate-run.txt)
[ -z "$bad" ] || { echo "$bad"; exit 1; }
"$HEAVY" ccp-survey tools/l2_in_image.sh "$IMG" . "python3 -B -m pytest -q -p no:cacheprovider harness/tests" > .survey-out/gate-harness.txt 2>&1
rc=$?; echo "harness tests rc=$rc"; [ $rc -eq 0 ] || { tail -20 .survey-out/gate-harness.txt; exit 1; }
python3 tools/grade_practices.py || exit 1
python3 tools/compare_example_outputs.py 39- || exit 1
python3 tools/compare_example_outputs.py 60- || exit 1
python3 tools/check_examples.py 'course/27-*/*.md' 'course/39-*/*.md' 'course/60-*/*.md' || exit 1
echo "claude code coverage (modules 27, 39 and 60): gate passed"
