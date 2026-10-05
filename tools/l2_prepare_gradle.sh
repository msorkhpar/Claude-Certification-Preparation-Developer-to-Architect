#!/bin/sh
# Gradle 9.8.0 (checksum-pinned) and its warmed plugin and JUnit cache, for the Kotlin practices (idempotent; one online warm-up).
# usage: tools/l2_prepare_gradle.sh <runner image id>   (cwd = repository root)
IMG=$1; W=$(pwd)
if [ ! -d "$W/.survey-out/gradle/gradle-9.8.0" ]; then
  mkdir -p "$W/.survey-out/gradle"
  curl -sSL -o "$W/.survey-out/gradle/g.zip" https://services.gradle.org/distributions/gradle-9.8.0-bin.zip
  echo "bafd5ce9cfaea0fbccfdc8439a1ac42fbd4cd9c89dc9a988228d8a2639a58e6c  $W/.survey-out/gradle/g.zip" | sha256sum -c - || exit 2
  (cd "$W/.survey-out/gradle" && unzip -q g.zip)
fi
FIRST=$(python3 tools/l2_practices.py list | cut -d' ' -f1 | while read -r p; do [ -d "$p/kotlin" ] && echo "$p" && break; done)   # the first practice that has a Kotlin edition
tools/l2_gradle_cache.sh verify; CACHE_RC=$?   # checksum manifest of the downloaded libraries and the finished transforms; 3 = repaired, warm again
if [ ! -d "$W/.survey-out/gradle/home/caches" ] || [ "$CACHE_RC" = 3 ]; then   # one online warm-up resolves the pinned plugin and JUnit
  tools/run_practice.sh "$IMG" kotlin "$FIRST" reference online
  tools/l2_gradle_cache.sh manifest
fi
