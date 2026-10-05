#!/bin/sh
# Inner job of the Level 1 (modules 1 to 6) gate. Runs inside the heavy-job slot, one thing at a time:
# every variant of the module 6 practice in all four languages, and the example programs and their tests.
# usage: tools/run_all_practices.sh <runner image id>        (cwd = repository root)
IMG=$1; W=$(pwd); PD=exercises/06-prompting-fundamentals/unit-01/practice-1
mkdir -p "$W/.survey-out"
python3 tools/make_harness.py --modules '^06-' || exit 2
python3 tools/make_plants.py --modules '^06-' || exit 2   # the wrong-* folders are generated, never committed
VARIANTS="starter reference wrong-task-first wrong-empty-sections wrong-missing-variable-silent wrong-blank-task-accepted wrong-no-escape wrong-fill-documents"
if [ ! -d "$W/.survey-out/gradle/gradle-9.8.0" ]; then
  mkdir -p "$W/.survey-out/gradle"
  curl -sSL -o "$W/.survey-out/gradle/g.zip" https://services.gradle.org/distributions/gradle-9.8.0-bin.zip
  echo "bafd5ce9cfaea0fbccfdc8439a1ac42fbd4cd9c89dc9a988228d8a2639a58e6c  $W/.survey-out/gradle/g.zip" | sha256sum -c - || exit 2
  (cd "$W/.survey-out/gradle" && unzip -q g.zip)
fi
if [ ! -d "$W/.survey-out/gradle/home/caches" ]; then   # one online warm-up resolves the pinned plugin and JUnit
  tools/run_practice.sh "$IMG" kotlin "$PD" reference online
fi
for lang in python typescript java kotlin; do
  for v in $VARIANTS; do
    tools/run_practice.sh "$IMG" "$lang" "$PD" "$v"
    echo "$lang $v rc=$?"
  done
done
# the example programs: their tests, then their printed output
for ex in 01-language-model 02-toy-tokenizer; do
  for lang in python typescript; do
    mod=$(ls examples/$ex/$lang | grep -E '^(sampler|bpe)\.(py|ts)$' | head -1)
    if [ "$lang" = python ]; then T="python3 -B -m pytest -q -p no:cacheprovider"; R="python3 -B $mod"; else T="node --test"; R="node $mod"; fi
    docker --context desktop-linux run --rm --network none -v "$W/examples/$ex/$lang:/work:ro" -w /work --entrypoint sh "$IMG" -c "$T" > ".survey-out/ex-$ex-$lang-test.txt" 2>&1
    echo "example $ex $lang tests rc=$?"
    docker --context desktop-linux run --rm --network none -v "$W/examples/$ex/$lang:/work:ro" -w /work --entrypoint sh "$IMG" -c "$R" > ".survey-out/ex-$ex-$lang-out.txt" 2>&1
    echo "example $ex $lang run rc=$?"
  done
done
