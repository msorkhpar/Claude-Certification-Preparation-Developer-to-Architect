#!/bin/sh
# C-04 JVM: Gradle resolves the Java, Kotlin SDKs. usage: <image> <online|offline> <task...>
IMG=$1; MODE=$2; shift 2; W=$(pwd)
NET=""; GFLAG=""; [ "$MODE" = offline ] && { NET="--network none"; GFLAG="--offline"; }
s=$(date +%s)
docker --context desktop-linux run --rm $NET --user 1000:1000 -e HOME=/work/home -e GRADLE_USER_HOME=/g/home \
  -v "$W/.survey-out/gradle:/g" -v "$W/tools/jvm-sdk:/work" -w /work --entrypoint sh "$IMG" \
  -c "mkdir -p /work/home; /g/gradle-9.8.0/bin/gradle $GFLAG --no-daemon --console=plain -q $*" 2>&1
echo "seconds=$(( $(date +%s) - s )) rc"
