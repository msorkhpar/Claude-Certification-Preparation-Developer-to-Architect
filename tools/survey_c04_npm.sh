#!/bin/sh
# C-04 offline phase (npm): npm ci from the cache with no network, import, type-check. usage: <image>
IMG=$1; W=$(pwd)
docker --context desktop-linux run --rm --network none --user 1000:1000 -e HOME=/work/home -v "$W/.survey-out/npm:/work" -w /work --entrypoint sh "$IMG" -c '
set -e
rm -rf off && mkdir off && cp proj/package.json proj/package-lock.json off/ && cd off
s=$(date +%s); npm ci --offline --cache /work/cache --no-audit --no-fund 2>&1 | tail -3; echo ci_seconds=$(( $(date +%s) - s ))
du -sh node_modules; du -sh node_modules/@anthropic-ai/claude-agent-sdk-linux-x64 node_modules/@anthropic-ai/claude-agent-sdk node_modules/typescript
ls -l node_modules/@anthropic-ai/claude-agent-sdk-linux-x64 | head
cat > t.mjs <<JS
import Anthropic from "@anthropic-ai/sdk";
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { query } from "@anthropic-ai/claude-agent-sdk";
import { z } from "zod";
console.log(typeof Anthropic, typeof McpServer, typeof query, typeof z.object);
JS
s=$(date +%s%N); node t.mjs; echo import_ms=$(( ($(date +%s%N) - s) / 1000000 ))
cat > t.ts <<JS
import Anthropic from "@anthropic-ai/sdk";
const c = new Anthropic({ apiKey: "placeholder" });
const p: Parameters<typeof c.messages.create>[0] = { model: "claude-sonnet-5-5", max_tokens: 16, messages: [{ role: "user", content: "hi" }] };
console.log(p.model);
JS
node node_modules/typescript/bin/tsc --noEmit --strict --module nodenext --moduleResolution nodenext --target es2022 --skipLibCheck t.ts && echo tsc_ok
'
