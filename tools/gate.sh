#!/bin/sh
# THE gate. One script for every module range; it replaces the per-batch scripts of docs/process/batches/.
#
# usage: tools/gate.sh --modules REGEX [--examples REGEX | --no-examples] [--no-harness-tests] [--report-only]
#   --modules REGEX    module folders of course/, exercises/ (practices) and examples/ the range covers, e.g. '^(41|62)-'
#   --examples REGEX   which example folders to run (default: the same regex); --no-examples runs none
#   --report-only      NO container job: re-judge the outputs already in .survey-out and run the fast checks (for the gate's own
#                      tests and for reading a finished run again); its report says mode=report-only and is never accepted as a hand-back
# Steps (every one is recorded in .survey-out/gate-report.json with its rc and seconds):
#   1 cases and plants for the range (tools/make_cases.py, tools/make_plants.py), the run-output harness when tools/make_harness.py exists
#   2 quiz checker and its planted-defect tests, quiz.json rebuild-equality, revision aids and tests, exam-map coverage, logger check
#     when tools/check_logger.py exists, personal-data scan of the range
#   3 ONE heavy job: cache preparation (SDK caches, Gradle, JVM example libraries) BEFORE any practice
#   4 heavy job: every practice variant in Python, TypeScript, Java and Kotlin (tools/l2_run_all.sh) and the Python/TypeScript examples
#   5 heavy job: the Java and Kotlin editions of the examples (Gradle, offline)      6 heavy job: the harness tests
#   7 the judgement (tools/gate_report.py): expected runs (reference, starter, each plant of make_plants --list) against executed
#     runs read from the runners' own output; all four languages required for every practice and example unless only Python and
#     TypeScript exist and the sources name the Agent SDK; a language with no runs fails; grade findings; examples per language
#   8 Java/Kotlin output equals Python output (tools/compare_example_outputs.py); page example blocks equal examples/ and the container output
# Exit status 0 only when every step passed AND the report says pass. Then: python3 tools/validate_handback.py .survey-out/gate-report.json
set -u
cd "$(dirname "$0")/.." || exit 2
IMG=${RUNNER_IMAGE:-93d052f3fc87}      # runner image id (digest sha256:93d052f3fc87...)
HEAVY=${HEAVY_SLOT:-../.heavy-slot/run-heavy.sh}
KIND=${GATE_HEAVY_KIND:-ccp-survey}
export STUDYFORGE_NAMESPACE=studyforge-local
MODULES=''; EXAMPLES=''; NOEX=''; HTESTS=1; MODE=full
while [ $# -gt 0 ]; do
  case "$1" in
    --modules) MODULES=$2; shift 2 ;;
    --examples) EXAMPLES=$2; shift 2 ;;
    --no-examples) NOEX=1; shift ;;
    --no-harness-tests) HTESTS=''; shift ;;
    --report-only) MODE=report-only; shift ;;
    -h|--help) sed -n '2,24p' "$0"; exit 0 ;;
    *) echo "gate.sh: unknown argument $1" >&2; exit 2 ;;
  esac
done
[ -n "$MODULES" ] || { echo "gate.sh: --modules REGEX is required (see --help)" >&2; exit 2; }
[ -n "$NOEX" ] && EXAMPLES='^$'
[ -n "$EXAMPLES" ] || EXAMPLES=$MODULES
mkdir -p .survey-out
STARTED=$(date +%s)
STEPS=.survey-out/gate-steps.tsv
R="python3 tools/gate_report.py"
FAILED=0
[ "$MODE" = full ] && $R clean --modules "$MODULES" --examples "$EXAMPLES" >/dev/null
: > "$STEPS"

# step <name> <command...>: run, log to .survey-out/gate-<name>.txt, record rc and seconds
step() {
  name=$1; shift; t0=$(date +%s)
  "$@" > ".survey-out/gate-$name.txt" 2>&1; rc=$?
  printf '%s\t%s\t%s\n' "$name" "$rc" "$(( $(date +%s) - t0 ))" >> "$STEPS"
  if [ $rc -eq 0 ]; then echo "[gate] $name: ok"; else echo "[gate] $name: rc=$rc"; tail -15 ".survey-out/gate-$name.txt"; FAILED=1; fi
  return $rc
}
# a tool that exits "no practice matches" has nothing to do for this range: that is not a failure
tolerant() { out=$("$@" 2>&1); rc=$?; echo "$out"; [ $rc -eq 0 ] || { echo "$out" | grep -q "no practice matches" && return 0; return $rc; }; }
quizjson_equal() {
  before=$(cat exercises/*/tests/quiz.json | sha256sum); python3 tools/build_quiz_json.py >/dev/null
  after=$(cat exercises/*/tests/quiz.json | sha256sum)
  [ "$before" = "$after" ] || { echo "quiz.json was out of date and has been rebuilt: review and commit it"; return 1; }
}
HARNESS_SPECS=''; JVM_TASKS=''; NPRACTICES=0; EXAMPLE_NAMES=''
eval "$($R info --modules "$MODULES" --examples "$EXAMPLES" | awk '{k=$1; $1=""; sub(/^ /,""); gsub(/\047/,""); printf "INFO_%s=\047%s\047\n", k, $0}')"
NPRACTICES=$INFO_PRACTICES; EXAMPLE_NAMES=$INFO_EXAMPLES; HARNESS_SPECS=$INFO_SPECS; JVM_TASKS=$INFO_JVMTASKS
PAGES=$(python3 - "$MODULES" <<'PY'
import re, sys
from pathlib import Path
rx = re.compile(sys.argv[1])
print(" ".join(f"course/{p.name}/*.md" for p in sorted(Path("course").iterdir()) if p.is_dir() and rx.match(p.name)))
PY
)

# ---- 1-2 static steps (all of them run; a failure skips the container jobs) ----
if [ "$MODE" = full ]; then
  [ "$NPRACTICES" -gt 0 ] && { step cases tolerant python3 tools/make_cases.py --modules "$MODULES"; step plants tolerant python3 tools/make_plants.py --modules "$MODULES"; }
  [ "$NPRACTICES" -gt 0 ] && [ -f tools/make_harness.py ] && step harness-drop-in python3 tools/make_harness.py --modules "$MODULES"
  step quiz-tests sh -c 'python3 tools/test_check_quiz.py >/dev/null'
  step quiz python3 tools/check_quiz.py
  step quiz-json quizjson_equal
  step revision-tests sh -c 'python3 tools/test_check_revision.py >/dev/null'
  step revision python3 tools/check_revision.py
  step coverage python3 tools/check_coverage.py
  [ -f tools/check_logger.py ] && step logger python3 tools/check_logger.py --modules "$MODULES"
  step personal-data python3 tools/check_personal_data.py --modules "$MODULES" --examples "$EXAMPLES"
else
  step personal-data python3 tools/check_personal_data.py --modules "$MODULES" --examples "$EXAMPLES"
fi

# ---- 3-6 container jobs, through the heavy slot (never a bare docker or gradle run) ----
if [ "$MODE" = full ] && [ $FAILED -eq 0 ] && { [ "$NPRACTICES" -gt 0 ] || [ -n "$EXAMPLE_NAMES" ]; }; then
  [ "$NPRACTICES" -gt 0 ] && L2M=$MODULES || L2M='^$'
  export L2_MODULES=$L2M L2_EXAMPLE_SPECS="${HARNESS_SPECS:- }" L2_EXAMPLES="^($(echo $EXAMPLE_NAMES | tr ' ' '|'))\$"
  step prepare "$HEAVY" "$KIND" sh -c "tools/l2_prepare_caches.sh $IMG && tools/l2_prepare_gradle.sh $IMG && tools/l2_prepare_jvm_examples.sh $IMG"
  if [ $FAILED -eq 0 ]; then
    step run-all "$HEAVY" "$KIND" tools/l2_run_all.sh "$IMG"
    [ -n "$JVM_TASKS" ] && step jvm-examples "$HEAVY" "$KIND" sh -c "tools/l2_run_jvm_examples.sh $IMG $JVM_TASKS"
    [ -n "$HTESTS" ] && step harness-tests "$HEAVY" "$KIND" tools/l2_in_image.sh "$IMG" . "python3 -B -m pytest -q -p no:cacheprovider harness/tests"
  fi
fi

# ---- 7-8 judgement ----
if [ "$MODE" = full ]; then
  [ -n "$EXAMPLE_NAMES" ] && step compare-outputs python3 tools/compare_example_outputs.py $EXAMPLE_NAMES
  [ -n "$PAGES" ] && step check-examples python3 tools/check_examples.py $PAGES
fi
$R report --modules "$MODULES" --examples "$EXAMPLES" --mode "$MODE" --steps "$STEPS" --started "$STARTED"; rc=$?
[ $rc -eq 0 ] && [ $FAILED -eq 0 ] && { echo "gate passed: modules $MODULES (mode $MODE)"; exit 0; }
echo "GATE FAILED: modules $MODULES (see .survey-out/gate-report.json)"; exit 1
