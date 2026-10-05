#!/bin/sh
# Integrity of the warmed Gradle user home (.survey-out/gradle/home), host side, no network, no container.
#   manifest  after a warm-up: record sha256 of the downloaded libraries (caches/modules-2/files-2.1) and of the
#             finished transforms (caches/<gradle-version>/transforms, lock and properties files left out) in
#             .survey-out/gradle/cache-manifest/{modules-2,transforms}.sha256
#   verify    before a run: check every recorded file. Entries added by later runs are not recorded and not checked.
#     - no manifest yet      : adopt the current cache (write the manifest) and go on
#     - transforms differ    : a crash left a damaged transform. Delete only caches/<gradle-version>, which Gradle
#                              regenerates from the downloaded libraries, drop the two warm markers and write
#                              .survey-out/gradle/cache-manifest/needs-warm, so the caller re-runs its existing
#                              checksum-verified warm-up (exit 3)
#     - a library differs    : delete that damaged file and the resolution metadata, and ask for the same warm-up,
#                              which downloads it again and is checked against examples/gradle/verification-metadata.xml (exit 3)
#   exit 0 = cache fine, 3 = repaired, a warm-up is needed, 2 = usage.
# The cache is not given to Gradle as a read-only dependency cache (GRADLE_RO_DEP_CACHE): every runner script mounts the one
# read-write home at /g/home and a read-only copy would need a second mount and a copy per job in all of them.
# usage: tools/l2_gradle_cache.sh <manifest|verify>   (cwd = repository root)
W=$(pwd); G="$W/.survey-out/gradle"; H="$G/home/caches"; M="$G/cache-manifest"
VER=$(ls "$H" 2>/dev/null | grep -E '^[0-9]+\.[0-9]+' | head -1)
hash_libs() { [ -d "$H/modules-2/files-2.1" ] && (cd "$H/modules-2/files-2.1" && find . -type f | LC_ALL=C sort | xargs -r sha256sum); }
hash_transforms() { [ -n "$VER" ] && [ -d "$H/$VER/transforms" ] && (cd "$H/$VER/transforms" && find . -type f ! -name '*.lock' ! -name '*.properties' | LC_ALL=C sort | xargs -r sha256sum); }
# files of a manifest whose hash or presence no longer matches (the path of each)
bad() { [ -f "$2" ] && (cd "$1" && sha256sum -c --quiet "$2" 2>&1 | sed -n 's/^\(.*\): \(FAILED.*\)$/\1/p'); }
case "$1" in
  manifest)
    mkdir -p "$M" || exit 2
    hash_libs > "$M/modules-2.sha256.tmp"; hash_transforms > "$M/transforms.sha256.tmp"
    mv "$M/modules-2.sha256.tmp" "$M/modules-2.sha256"; mv "$M/transforms.sha256.tmp" "$M/transforms.sha256"
    rm -f "$M/needs-warm"
    echo "gradle cache manifest: $(wc -l < "$M/modules-2.sha256") libraries, $(wc -l < "$M/transforms.sha256") transform files" ;;
  verify)
    [ -d "$H" ] || exit 0                       # cold cache: the warm-up that follows fills it
    if [ ! -f "$M/modules-2.sha256" ]; then
      if [ -f "$M/needs-warm" ]; then echo "gradle cache: warm-up pending"; exit 3; fi
      echo "gradle cache: no manifest, adopting the current cache"; "$0" manifest; exit 0
    fi
    LIBS=$(bad "$H/modules-2/files-2.1" "$M/modules-2.sha256")
    TRANS=$([ -d "$H/$VER/transforms" ] || echo MISSING; bad "$H/$VER/transforms" "$M/transforms.sha256")
    [ -z "$LIBS" ] && [ -z "$TRANS" ] && { echo "gradle cache: verified"; exit 0; }
    if [ -n "$LIBS" ]; then
      echo "gradle cache: damaged libraries: $(echo "$LIBS" | wc -l); deleting them and the resolution metadata"
      echo "$LIBS" | while read -r f; do rm -f "$H/modules-2/files-2.1/$f"; done
      rm -rf "$H"/modules-2/metadata-*
    fi
    if [ -n "$TRANS" ]; then
      echo "gradle cache: damaged transforms, deleting the regenerable caches/$VER"
      rm -rf "$H/$VER"
    fi
    rm -f "$M/modules-2.sha256" "$M/transforms.sha256" "$G/jvm-examples.warm"
    touch "$M/needs-warm"
    exit 3 ;;
  *) echo "usage: $0 manifest|verify" >&2; exit 2 ;;
esac
