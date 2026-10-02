#!/bin/sh
# C-03 proof: each variant graded offline in the runner image. usage: survey_c03.sh <image>
IMG=$1; W=$(pwd)
for v in reference starter wrong-ignores-stop-reason wrong-one-turn-per-result; do
  s=$(date +%s.%N)
  docker --context desktop-linux run --rm --network none -e PYTHONDONTWRITEBYTECODE=1 \
    -v "$W/exercises/agent-loop/python:/work:ro" -w /work --entrypoint sh "$IMG" ./run.sh "$v" > ".survey-out/c03-$v.txt" 2>&1
  rc=$?; e=$(date +%s.%N)
  echo "$v rc=$rc seconds=$(echo "$e - $s" | bc) $(tail -1 .survey-out/c03-$v.txt)"
done
