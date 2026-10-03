#!/bin/sh
# Inner job of the Level 2 gates. Runs inside the heavy-job slot, one thing at a time, offline:
# every variant of every Level 2 practice in four languages, then the Level 2 examples (tests and output).
# usage: tools/l2_run_all.sh <runner image id>        (cwd = repository root)
IMG=$1; W=$(pwd)
mkdir -p "$W/.survey-out"
tools/l2_prepare_caches.sh "$IMG" || exit 2
tools/l2_prepare_gradle.sh "$IMG" || exit 2
python3 tools/l2_practices.py list | while read -r PD VARIANTS; do
  for lang in python typescript java kotlin; do
    tools/l2_run_practice.sh "$IMG" "$lang" "$PD" $VARIANTS
    echo "$PD $lang done rc=$?"
  done
done
# the Level 2 examples: their tests, then their printed output
for spec in examples/*/example.json; do
  d=$(basename "$(dirname "$spec")")
  for lang in python typescript; do
    file=$(python3 -c "import json,sys; print(json.load(open('$spec'))['files']['$lang'])")
    if [ "$lang" = python ]; then T="python3 -B -m pytest -q -p no:cacheprovider"; R="python3 -B $file"; else T="node --test"; R="node $file"; fi
    tools/l2_in_image.sh "$IMG" "examples/$d/$lang" "$T" > ".survey-out/ex-$d-$lang-test.txt" 2>&1
    echo "example $d $lang tests rc=$?"
    tools/l2_in_image.sh "$IMG" "examples/$d/$lang" "$R" > ".survey-out/ex-$d-$lang-out.txt" 2>&1
    echo "example $d $lang run rc=$?"
  done
done
