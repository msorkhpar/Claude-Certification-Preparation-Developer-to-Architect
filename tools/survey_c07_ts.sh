#!/bin/sh
# C-07 TypeScript: node:test with built-in type stripping, offline. usage: survey_c07_ts.sh <image>
IMG=$1; W=$(pwd)
for v in reference starter wrong-ignores-stop-reason wrong-one-turn-per-result; do
  s=$(date +%s.%N)
  docker --context desktop-linux run --rm --network none \
    -v "$W/exercises/agent-loop/typescript:/work:ro" -w /work --entrypoint sh "$IMG" ./run.sh "$v" > ".survey-out/c07ts-$v.txt" 2>&1
  rc=$?; e=$(date +%s.%N)
  echo "$v rc=$rc seconds=$(echo "$e - $s" | bc) $(grep -E '^# (pass|fail)' .survey-out/c07ts-$v.txt | tr '\n' ' ')"
done
