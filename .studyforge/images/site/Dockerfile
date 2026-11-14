# Written by studyforge's execution skill (standalone). Regenerate it; never edit it.
# The course's study site: every file of the course on the shared server.
# Two targets: without-narration, whose pages name no clip, and with-narration,
# which adds every clip and refuses to build unless each matches the checksum
# the course committed.
ARG STUDYFORGE_NAMESPACE

FROM ${STUDYFORGE_NAMESPACE:?STUDYFORGE_NAMESPACE-is-not-set}/studyforge-serve:0.1.0-4345c945f8981d8c6a006a92ee069e8d666e936c8a6fc3110e7588be3d231432@sha256:4132e0e3a8b9e7cd00ea3e202a3af2bf85c4549045ef2adfae9a1c7e1519edb0 AS without-narration
USER root
RUN --mount=type=bind,source=.,target=/context \
    set -eu; mkdir -p /corpus; \
    tar -C /context --exclude=./.git --exclude='./.studyforge/*/audio' --exclude='*/study/audio' -cf - . \
      | tar -C /corpus -xf -; \
    mkdir -p /corpus/.studyforge/progress; \
    find /corpus -type f -name '*.html' -exec sed -i -E 's/ data-audio="[^"]*"//g' {} +; \
    chown -R 1000:1000 /corpus
USER 1000:1000
WORKDIR /corpus
LABEL org.studyforge.course="claude-certification-preparation" org.studyforge.narration="without"

FROM ${STUDYFORGE_NAMESPACE:?STUDYFORGE_NAMESPACE-is-not-set}/studyforge-serve:0.1.0-4345c945f8981d8c6a006a92ee069e8d666e936c8a6fc3110e7588be3d231432@sha256:4132e0e3a8b9e7cd00ea3e202a3af2bf85c4549045ef2adfae9a1c7e1519edb0 AS with-narration
USER root
RUN --mount=type=bind,source=.,target=/context \
    set -eu; mkdir -p /corpus; \
    tar -C /context --exclude=./.git --exclude='./.studyforge/*/audio' --exclude='*/study/audio' -cf - . \
      | tar -C /corpus -xf -; \
    mkdir -p /corpus/.studyforge/progress; \
    cd /context; \
    find . \( -path './.studyforge/*' -o -path '*/study/*' \) -type d -name audio -prune | while IFS= read -r dir; do \
      mkdir -p "/corpus/$dir"; cp -R "$dir/." "/corpus/$dir/"; \
    done; \
    chown -R 1000:1000 /corpus; \
    cd /corpus; sha256sum -c --quiet .studyforge/narration-release/clips.sha256 \
      || { echo 'narration clips missing or wrong: restore them first' >&2; \
           exit 1; }
USER 1000:1000
WORKDIR /corpus
LABEL org.studyforge.course="claude-certification-preparation" org.studyforge.narration="with"
