#!/bin/sh
# Offline install of the pinned SDKs from the local caches that the fetch scripts filled (idempotent).
# usage: tools/l2_prepare_caches.sh <runner image id>     (cwd = repository root; runs with --network none)
IMG=$1; W=$(pwd)
mkdir -p "$W/.survey-out/py/site" "$W/.survey-out/npm/off/node_modules"
if [ ! -d "$W/.survey-out/py/wheelhouse" ] && [ ! -d "$W/.survey-out/npm/cache" ]; then
  echo "no SDK caches: the Python and npm SDK sites stay empty (practices and examples that need no SDK still run)"
  exit 0
fi
if [ ! -d "$W/.survey-out/py/site/mcp" ] || [ ! -d "$W/.survey-out/py/site/claude_agent_sdk" ]; then
  docker --context desktop-linux run --rm --network none --user 1000:1000 -e HOME=/work/home -v "$W/.survey-out/py:/work" -w /work --entrypoint sh "$IMG" -c \
    'mkdir -p home && python3 -m pip install --no-index --find-links wheelhouse --target site anthropic==1.11.0 mcp==2.2.0 claude-agent-sdk==0.2.163 pydantic==2.13.5 2>&1 | tail -1' || exit 1
fi
if [ ! -d "$W/.survey-out/npm/off/node_modules/@anthropic-ai/sdk" ]; then
  docker --context desktop-linux run --rm --network none --user 1000:1000 -e HOME=/work/home -v "$W/.survey-out/npm:/work" -w /work --entrypoint sh "$IMG" -c \
    'mkdir -p home && rm -rf off && mkdir off && cp proj/package.json proj/package-lock.json off/ && cd off && npm ci --offline --cache /work/cache --no-audit --no-fund 2>&1 | tail -2' || exit 1
fi
echo "caches ready"
