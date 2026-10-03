#!/bin/sh
# Run the variants of one practice in one language offline in the runner image, in ONE container.
# Output of each variant goes to .survey-out/<practice with / as _>-<lang>-<variant>.txt, ending with a line rc=<n>.
# usage: tools/l2_run_practice.sh <image> <python|typescript|java|kotlin> <practice-dir> <variant>...
IMG=$1; LANGX=$2; PD=$3; shift 3
W=$(pwd); NAME=$(echo "$PD" | tr '/' '_'); VARS="$*"
mkdir -p "$W/.survey-out/gradle/home"
case "$LANGX" in
  python|typescript)
    docker --context desktop-linux run --rm --network none -v "$W/$PD/$LANGX:/work:ro" -v "$W/.survey-out:/o" -w /work --entrypoint sh "$IMG" -c \
      "for v in $VARS; do ./run.sh \$v > /o/$NAME-$LANGX-\$v.txt 2>&1; echo rc=\$? >> /o/$NAME-$LANGX-\$v.txt; done" ;;
  java)
    if [ -f "$W/$PD/java/pom.xml" ]; then   # practices written for Maven (modules 13 to 17)
      mkdir -p "$W/$PD/.build-java"
      docker --context desktop-linux run --rm --network none -v "$W/$PD/java:/work" -v "$W/$PD/.build-java:/work/../.build-java" -v "$W/.survey-out:/o" -w /work --entrypoint sh "$IMG" -c \
        "for v in $VARS; do mvn -o -q -B -Dsolution.dir=\$v test > /o/$NAME-java-\$v.txt 2>&1; echo rc=\$? >> /o/$NAME-java-\$v.txt; done"
    else                                      # Gradle (Kotlin DSL), like the Kotlin practices
      mkdir -p "$W/$PD/.build-java"
      docker --context desktop-linux run --rm --network none --user 1000:1000 -e HOME=/work/home -e GRADLE_USER_HOME=/g/home \
        -v "$W/.survey-out/gradle:/g" -v "$W/$PD/java:/work" -v "$W/$PD/.build-java:/work/../.build-java" -v "$W/.survey-out:/o" -w /work --entrypoint sh "$IMG" -c \
        "mkdir -p /work/home; for v in $VARS; do /g/gradle-9.8.0/bin/gradle --offline --console=plain -Psolution=\$v cleanTest test > /o/$NAME-java-\$v.txt 2>&1; echo rc=\$? >> /o/$NAME-java-\$v.txt; done; /g/gradle-9.8.0/bin/gradle --stop >/dev/null 2>&1"
    fi ;;
  kotlin)
    mkdir -p "$W/$PD/.build-kotlin"
    docker --context desktop-linux run --rm --network none --user 1000:1000 -e HOME=/work/home -e GRADLE_USER_HOME=/g/home \
      -v "$W/.survey-out/gradle:/g" -v "$W/$PD/kotlin:/work" -v "$W/$PD/.build-kotlin:/work/../.build-kotlin" -v "$W/.survey-out:/o" -w /work --entrypoint sh "$IMG" -c \
      "mkdir -p /work/home; for v in $VARS; do /g/gradle-9.8.0/bin/gradle --offline --console=plain -Psolution=\$v cleanTest test > /o/$NAME-kotlin-\$v.txt 2>&1; echo rc=\$? >> /o/$NAME-kotlin-\$v.txt; done; /g/gradle-9.8.0/bin/gradle --stop >/dev/null 2>&1" ;;
esac
