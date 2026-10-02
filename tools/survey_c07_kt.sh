#!/bin/sh
# C-07 Kotlin via Gradle + JUnit 5. usage: survey_c07_kt.sh <image> <online|offline> [variant...]
# online: warm-up with network; offline: --network none, --offline. Gradle dist and user home live in .survey-out/gradle.
IMG=$1; MODE=$2; shift 2; W=$(pwd)
NET=""; GFLAG=""; [ "$MODE" = offline ] && { NET="--network none"; GFLAG="--offline"; }
mkdir -p "$W/.survey-out/gradle/home" "$W/exercises/agent-loop/.build-kotlin"
for v in "$@"; do
  s=$(date +%s.%N)
  docker --context desktop-linux run --rm $NET --user 1000:1000 -e HOME=/work/home -e GRADLE_USER_HOME=/g/home \
    -v "$W/.survey-out/gradle:/g" -v "$W/exercises/agent-loop/kotlin:/work" -v "$W/exercises/agent-loop/.build-kotlin:/.build-kotlin" \
    -w /work --entrypoint sh "$IMG" -c "mkdir -p /work/home; /g/gradle-9.8.0/bin/gradle $GFLAG --no-daemon --console=plain -Psolution=$v cleanTest test" > ".survey-out/c07kt-$MODE-$v.txt" 2>&1
  rc=$?; e=$(date +%s.%N)
  echo "$MODE $v rc=$rc seconds=$(echo "$e - $s" | bc) passed=$(grep -c PASSED .survey-out/c07kt-$MODE-$v.txt) failed=$(grep -c FAILED .survey-out/c07kt-$MODE-$v.txt)"
done
