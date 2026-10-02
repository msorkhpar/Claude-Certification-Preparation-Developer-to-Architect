#!/bin/sh
# C-04 offline phase (python): install from the wheelhouse with no network, import, measure. usage: <image>
IMG=$1; W=$(pwd)
docker --context desktop-linux run --rm --network none --user 1000:1000 -e HOME=/work/home -v "$W/.survey-out/py:/work" -w /work --entrypoint sh "$IMG" -c '
set -e
rm -rf site; s=$(date +%s)
python3 -m pip install --no-index --find-links wheelhouse --target site anthropic==1.11.0 mcp==2.2.0 claude-agent-sdk==0.2.163 pydantic==2.13.5 2>&1 | tail -2
echo install_seconds=$(( $(date +%s) - s ))
du -sh site; du -sh site/claude_agent_sdk/_bundled
s=$(date +%s%N)
PYTHONPATH=site python3 -c "
import anthropic, mcp, claude_agent_sdk, pydantic, httpx2, importlib.metadata as m
for p in (\"anthropic\",\"mcp\",\"claude-agent-sdk\",\"pydantic\",\"httpx2\"): print(p, m.version(p))
print(\"anthropic uses transport from\", anthropic.DefaultHttpxClient.__module__ if hasattr(anthropic,\"DefaultHttpxClient\") else None)
"
echo import_ms=$(( ($(date +%s%N) - s) / 1000000 ))
ls -l site/claude_agent_sdk/_bundled/
site/claude_agent_sdk/_bundled/claude --version 2>&1 | head -2 || true
'
