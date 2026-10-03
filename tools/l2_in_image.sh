#!/bin/sh
# Run a shell command offline in the runner image with the repository mounted read-only at /w, the Python SDK
# site on PYTHONPATH and the npm packages at /node_modules.  Meant to be called through the heavy slot.
# usage: tools/l2_in_image.sh <runner image id> <workdir under /w> <shell command>
IMG=$1; WD=$2; shift 2; W=$(pwd)
docker --context desktop-linux run --rm --network none -e PYTHONDONTWRITEBYTECODE=1 \
  -e PYTHONPATH=/w:/w/.survey-out/py/site -v "$W:/w:ro" -v "$W/.survey-out/npm/off/node_modules:/node_modules:ro" \
  -w "/w/$WD" --entrypoint sh "$IMG" -c "$*"
