#!/bin/sh
# C-07 Java via Maven + JUnit 5, offline (-o), repository baked in the runner image. usage: survey_c07_jvm.sh <image>
IMG=$1; W=$(pwd)
mkdir -p "$W/exercises/agent-loop/.build-java"
for v in reference starter wrong-ignores-stop-reason wrong-one-turn-per-result; do
  s=$(date +%s.%N)
  docker --context desktop-linux run --rm --network none \
    -v "$W/exercises/agent-loop/java:/work" -v "$W/exercises/agent-loop/.build-java:/work/../.build-java" -w /work --entrypoint sh "$IMG" \
    -c "mvn -o -q -B -Dsolution.dir=$v test" > ".survey-out/c07java-$v.txt" 2>&1
  rc=$?; e=$(date +%s.%N)
  echo "$v rc=$rc seconds=$(echo "$e - $s" | bc) $(grep -E 'Tests run:.*Fail' .survey-out/c07java-$v.txt | tail -1)"
done
