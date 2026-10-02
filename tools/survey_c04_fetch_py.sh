#!/bin/sh
# C-04 online phase (python): download pinned wheels into .survey-out/py/wheelhouse. usage: <image>
IMG=$1; W=$(pwd)
docker --context desktop-linux run --rm --user 1000:1000 -e HOME=/work/home -v "$W/.survey-out/py:/work" -w /work --entrypoint sh "$IMG" -c '
set -e
mkdir -p home wheelhouse
python3 -m pip download --dest wheelhouse "anthropic==1.11.0" "mcp==2.2.0" "claude-agent-sdk==0.2.163" "pydantic==2.13.5" 2>&1 | tail -15
ls wheelhouse | wc -l; du -sh wheelhouse'
