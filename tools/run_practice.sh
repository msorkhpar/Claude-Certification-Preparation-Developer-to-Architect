#!/bin/sh
# Run one variant of a practice offline in the runner image.
# usage: tools/run_practice.sh <image> <python|typescript|java|kotlin> <practice-dir> <variant> [online]
# Output goes to .survey-out/<practice>-<lang>-<variant>.txt ; exit status is the test run's.
# Java and Kotlin need .survey-out/gradle/gradle-9.8.0 (checksum-pinned distribution) and a warmed Gradle home.
IMG=$1; LANGX=$2; PD=$3; V=$4; MODE=${5:-offline}
W=$(pwd); NAME=$(echo "$PD" | tr '/' '_')
OUT="$W/.survey-out/$NAME-$LANGX-$V.txt"
NET="--network none"; GFLAG="--offline"; [ "$MODE" = online ] && { NET=""; GFLAG=""; }
mkdir -p "$W/.survey-out/gradle/home"
case "$LANGX" in
  python|typescript)
    docker --context desktop-linux run --rm --network none -v "$W/$PD/$LANGX:/work:ro" -w /work --entrypoint sh "$IMG" ./run.sh "$V" > "$OUT" 2>&1 ;;
  java)
    mkdir -p "$W/$PD/.build-java"
    docker --context desktop-linux run --rm $NET --user 1000:1000 -e HOME=/work/home -e GRADLE_USER_HOME=/g/home \
      -v "$W:/w:ro" -v "$W/.survey-out/gradle:/g" -v "$W/$PD/java:/work" -v "$W/$PD/.build-java:/work/../.build-java" -w /work --entrypoint sh "$IMG" \
      -c "mkdir -p /work/home; /g/gradle-9.8.0/bin/gradle $GFLAG --no-daemon --console=plain -Psolution=$V cleanTest test" > "$OUT" 2>&1 ;;
  kotlin)
    mkdir -p "$W/$PD/.build-kotlin"
    docker --context desktop-linux run --rm $NET --user 1000:1000 -e HOME=/work/home -e GRADLE_USER_HOME=/g/home \
      -v "$W:/w:ro" -v "$W/.survey-out/gradle:/g" -v "$W/$PD/kotlin:/work" -v "$W/$PD/.build-kotlin:/work/../.build-kotlin" -w /work --entrypoint sh "$IMG" \
      -c "mkdir -p /work/home; /g/gradle-9.8.0/bin/gradle $GFLAG --no-daemon --console=plain -Psolution=$V cleanTest test" > "$OUT" 2>&1 ;;
esac
exit $?
