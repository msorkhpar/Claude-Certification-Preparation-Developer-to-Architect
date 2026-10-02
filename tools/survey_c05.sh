#!/bin/sh
# C-05: harness tests offline in the runner image, with the SDKs from the local wheelhouse install.
# usage: survey_c05.sh <image> [pytest args]
IMG=$1; shift; W=$(pwd)
docker --context desktop-linux run --rm --network none -e PYTHONDONTWRITEBYTECODE=1 \
  -e PYTHONPATH=/w/.survey-out/py/site -v "$W:/w:ro" -w /w --entrypoint sh "$IMG" \
  -c "python3 -m pytest harness/tests tests -q -p no:cacheprovider $*"
