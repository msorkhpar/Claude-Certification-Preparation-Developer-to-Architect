#!/bin/sh
# Build, test and run the Java and Kotlin editions of every example, in ONE container (one Gradle build, examples/).
# The examples and the JVM harness are copied to .survey-out/snap first (the build runs on that copy, so editing the repository
# during a run changes nothing); build output, Gradle state and printed output go under .survey-out/.
# Per edition: .survey-out/ex-<example dir>-<java|kotlin>-test.txt (a test summary) and ...-out.txt (what the program printed);
# the whole log is .survey-out/jvm-examples.txt and ends with a line rc=<n>.
# usage: tools/l2_run_jvm_examples.sh <image> [online] [gradle args...]   (default tasks: test runExample, offline)
IMG=$1; shift; MODE=offline; [ "${1:-}" = online ] && { MODE=online; shift; }
W=$(pwd); ARGS="${*:-test runExample}"
NET="--network none"; GFLAG="--offline"; [ "$MODE" = online ] && { NET=""; GFLAG=""; }
mkdir -p "$W/.survey-out/gradle/home" "$W/.survey-out/jvmbuild"
rm -rf "$W/.survey-out/snap" && mkdir -p "$W/.survey-out/snap/harness" || exit 2
cp -a "$W/examples" "$W/.survey-out/snap/examples" && cp -a "$W/harness/jvm" "$W/.survey-out/snap/harness/jvm" || exit 2
rm -rf "$W"/.survey-out/snap/examples/.gradle "$W"/.survey-out/snap/examples/.kotlin "$W"/.survey-out/snap/harness/jvm/.gradle
docker --context desktop-linux run --rm $NET --user 1000:1000 -e HOME=/b/h -e GRADLE_USER_HOME=/g/home \
  -v "$W/.survey-out/snap:/w" -v "$W/.survey-out/gradle:/g" -v "$W/.survey-out/jvmbuild:/b" -v "$W/.survey-out:/o" -w /w/examples --entrypoint sh "$IMG" -c \
  "mkdir -p /b/h; /g/gradle-9.8.0/bin/gradle $GFLAG --console=plain --continue --project-cache-dir /b/cache -Pkotlin.project.persistent.dir=/b/kotlin -PbuildRoot=/b/out -PexOut=/o $ARGS > /o/jvm-examples.txt 2>&1; echo rc=\$? >> /o/jvm-examples.txt; /g/gradle-9.8.0/bin/gradle --stop > /dev/null 2>&1"
tail -1 "$W/.survey-out/jvm-examples.txt" | grep -q '^rc=0$'
