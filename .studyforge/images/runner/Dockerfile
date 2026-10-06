# Written by studyforge's execution skill (standalone). Regenerate it; never edit it.
# The course's runner: the toolchain's course layer, with the run service the
# site reaches on the compose file's internal network.
ARG RUNNER_PRIMED
FROM ${RUNNER_PRIMED}
COPY .studyforge/execution/runservice.pl /opt/studyforge/runservice.pl
# The course's npm packages, installed once, offline, from the npm cache:
# a program under /work finds them by Node's upward lookup.
COPY package.json package-lock.json /work/
RUN cd /work && npm ci --offline --ignore-scripts --no-audit --no-fund --no-update-notifier --logs-max=0
WORKDIR /work
CMD ["perl", "/opt/studyforge/runservice.pl"]
LABEL org.studyforge.course="claude-certification-preparation"
