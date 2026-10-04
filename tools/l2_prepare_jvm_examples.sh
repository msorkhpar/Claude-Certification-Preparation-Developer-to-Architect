#!/bin/sh
# The libraries of the JVM example editions (the Anthropic Java SDK, the MCP SDKs, the YAML reader, Ktor, JUnit) in the Gradle cache
# (idempotent; one online warm-up, every download checked against examples/gradle/verification-metadata.xml, repeated only when
# a build file of examples/ or that checksum file changes).
# usage: tools/l2_prepare_jvm_examples.sh <runner image id>   (cwd = repository root)
IMG=$1; W=$(pwd)
MARK="$W/.survey-out/gradle/jvm-examples.warm"
SUM=$(cat "$W/examples/build.gradle.kts" "$W/examples/settings.gradle.kts" "$W"/examples/*/*/build.gradle.kts "$W/examples/gradle/verification-metadata.xml" | sha256sum | cut -d' ' -f1)
[ -f "$MARK" ] && [ "$(cat "$MARK")" = "$SUM" ] && exit 0
tools/l2_run_jvm_examples.sh "$IMG" online warm || exit 2
echo "$SUM" > "$MARK"
