#!/bin/sh
# Gate for the Java and Kotlin editions of the examples of modules 1 to 43. Run from the repository root:
#   sh docs/process/batches/jvm-examples-l1-l2-gates.sh
# 1. in ONE heavy-slot job, offline in the runner image (network none): every example in Python, TypeScript, Java and Kotlin, with its tests
#    and printed output (the JVM editions are one Gradle build in examples/; the libraries come from the Gradle cache that
#    tools/l2_prepare_jvm_examples.sh fills with one online warm-up); module 35 stays Python and TypeScript
# 2. in a second heavy-slot job, the harness tests of the Python harness (the JVM harness is tested by the Gradle job above)
# 3. every example test passed in every language; every Java and Kotlin output equals the Python output or is explained
#    (tools/compare_example_outputs.py)
# 4. the example blocks of the pages against examples/ and the outputs the container produced (one tab per language)
# Exit status is 0 only when every step passed.
set -u
cd "$(dirname "$0")/../../.." || exit 2
IMG=${RUNNER_IMAGE:-93d052f3fc87}      # runner image id (digest sha256:93d052f3fc87...)
HEAVY=${HEAVY_SLOT:-../.heavy-slot/run-heavy.sh}
mkdir -p .survey-out
# no practices in this gate (the module pattern matches none); the examples of modules 1 to 43; the JVM editions on
export L2_MODULES='^$' L2_JVM_EXAMPLES=1
export L2_EXAMPLE_SPECS='examples/0*/example.json examples/1*/example.json examples/2*/example.json examples/3*/example.json examples/4[0-3]-*/example.json'
"$HEAVY" ccp-survey tools/l2_run_all.sh "$IMG" > .survey-out/gate-run.txt 2>&1
rc=$?; echo "heavy job rc=$rc"; [ $rc -eq 0 ] || { tail -20 .survey-out/gate-run.txt; exit 1; }
bad=$(grep -E '^example .* (tests|run) rc=[1-9]|^jvm examples .* rc=[1-9]' .survey-out/gate-run.txt)
[ -z "$bad" ] || { echo "$bad"; exit 1; }
"$HEAVY" ccp-survey tools/l2_in_image.sh "$IMG" . "python3 -B -m pytest -q -p no:cacheprovider harness/tests" > .survey-out/gate-harness.txt 2>&1
rc=$?; echo "harness tests rc=$rc"; [ $rc -eq 0 ] || { tail -20 .survey-out/gate-harness.txt; exit 1; }
python3 tools/compare_example_outputs.py || exit 1
python3 tools/check_examples.py 'course/0[1-9]-*/*.md' 'course/[1-3][0-9]-*/*.md' 'course/4[0-3]-*/*.md' || exit 1
echo "JVM editions of the examples, modules 1 to 43: gate passed"
