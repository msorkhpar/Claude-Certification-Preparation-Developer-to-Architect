#!/bin/sh
# C-04 online phase (npm): resolve and fill an npm cache + lockfile. usage: <image>
IMG=$1; W=$(pwd)
mkdir -p "$W/.survey-out/npm/proj"; cp "$W/tools/npm-sdk/package.json" "$W/.survey-out/npm/proj/"
docker --context desktop-linux run --rm --user 1000:1000 -e HOME=/work/home -v "$W/.survey-out/npm:/work" -w /work/proj --entrypoint sh "$IMG" -c '
set -e
mkdir -p /work/home
npm install --cache /work/cache --no-audit --no-fund 2>&1 | tail -5
npm ls --all --depth=0 2>&1 | head -20
du -sh /work/cache node_modules; ls node_modules/@anthropic-ai; ls node_modules/@anthropic-ai/claude-agent-sdk* -d'
