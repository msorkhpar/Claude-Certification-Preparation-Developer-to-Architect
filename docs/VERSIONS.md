# Versions

What the course pins, read from the official documentation and the package registries on
**2026-10-02**. **To be re-checked at release** (board milestone M9): model ids, SDK versions and
the feature gaps below all move. A page that makes a claim names the versions it was checked on.

## Models (Claude API)

| Model | API id | Tier | Context | Max output | Thinking | Default effort | Retirement not before |
|---|---|---|---|---|---|---|---|
| Claude Fable 5.1 | `claude-fable-5-1` | top tier, demanding reasoning and long-horizon agent work | 1M | 128K | adaptive, always on | high | 2027-09-01 |
| Claude Opus 5.5 | `claude-opus-5-5` | long-running agentic coding and knowledge work; the docs' starting point for most workloads | 1M | 128K | adaptive, always on | medium | 2027-09-22 |
| Claude Sonnet 5.5 | `claude-sonnet-5-5` | speed and intelligence together | 1M | 128K | adaptive | high | 2027-09-28 |
| Claude Haiku 4.5 | `claude-haiku-4-5-20251001` (alias `claude-haiku-4-5`) | fastest | 200K | 64K | extended (manual budget) | not supported | 2026-10-15 |

Notes: manual extended thinking (`thinking.type` of `enabled` with `budget_tokens`) is not accepted
on models after Opus 4.6 and Sonnet 4.6; the later models use adaptive thinking steered by effort.
Dateless ids from the 4.6 generation on are themselves pinned snapshots. Haiku 4.5 has the
nearest retirement date, so the course's examples default to Sonnet 5.5 for ordinary work and name
Haiku only where a cheap tier is the point. Older models still available are listed on the
official model pages; the course does not use them. Prices, cache multipliers and the batch
discount are in the section "Prices, caching, batches, thinking and cloud platforms" below.

## Sampling parameters (`temperature`, `top_p`, `top_k`)

Read on 2026-10-02 from the Anthropic documentation: the Messages API reference (page title "Messages"),
"Migrating to Claude Opus 5.5" and "What's new in Claude Fable 5.1". **Claude Fable 5.1, Opus 5.5 and
Sonnet 5.5 reject any non-default value** of all three with a 400 error; omit them. For `temperature` the
reference says models released after Claude Opus 4.6 do not support setting it, that 1.0 is accepted for
backwards compatibility and that every other value is rejected. The Opus 5.5 migration guide states the same
for `temperature`, `top_p` and `top_k` ("Omit ... or leave them at their defaults: any other value is
rejected") and the Fable 5.1 page lists non-default values of all three as returning a 400 error. The Sonnet 5.5
"What's new" page does not repeat the rule, but "Migrating to Claude Sonnet 5.5" states it directly (Claude Sonnet 4.6
and earlier and Claude Haiku 4.5 accept the three; on Sonnet 5.5 a non-default value returns a 400 error). Claude Haiku 4.5 predates that cut and is not covered by these sentences; the course
does not use a sampling setting on it. Re-check at release.

## Prices, caching, batches, thinking and cloud platforms

Read on 2026-10-02 from the Anthropic documentation (Prompt caching, Batch processing, Thinking, Steering
thinking, Effort, Fast mode, Claude in Amazon Bedrock, Claude on Google Cloud), the Amazon Bedrock User Guide page on
identity-based policy examples and the Google Cloud documentation on Agent Platform access control with IAM and on
Claude models. Re-check at release.

| Model | Input $/MTok | Output $/MTok | 5-minute write | 1-hour write | Cache read | Batch input / output | Minimum cacheable prompt |
|---|---|---|---|---|---|---|---|
| Claude Fable 5.1 | 10 | 50 | 12.50 | 20 | 0.25 (0.025 times) | 5 / 25 | 512 tokens |
| Claude Opus 5.5 | 4 | 20 | 5 | 8 | 0.20 (0.05 times) | 2 / 10 | 512 tokens |
| Claude Sonnet 5.5 | 2 | 10 | 2.50 | 4 | 0.20 (0.1 times) | 1 / 5 | 512 tokens |
| Claude Haiku 4.5 | 1 | 5 | 1.25 | 2 | 0.10 (0.1 times) | 0.50 / 2.50 | 4,096 tokens |

- Cache multipliers: a 5-minute write is 1.25 times the base input price, a 1-hour write 2 times, a read 0.1 times, except Opus 5.5
  (0.05) and Fable 5.1 (0.025). They stack with the batch discount. The default lifetime is 5 minutes and is refreshed free by each
  use; the lookback window is 20 blocks; at most 4 breakpoints; a 1-hour entry must come before 5-minute entries.
- Batches: 50 percent of standard prices; at most 100,000 requests or 256 MB; expire after 24 hours; results kept 29 days from creation;
  `stream`, `speed` and `max_tokens` of 0 are refused; `custom_id` is 1 to 64 characters of letters, digits, hyphen and underscore.
- Thinking: Fable 5.1 and Opus 5.5 are adaptive and always on; Sonnet 5.5 is adaptive and accepts `between_tools` at high effort or below;
  Haiku 4.5 takes a manual budget of at least 1,024 and below `max_tokens`. Effort goes in `output_config.effort` and is not supported on
  Haiku 4.5. A thinking or effort change invalidates cached messages.
- Fast mode: `speed: "fast"` with the beta header `fast-mode-2026-02-01`, up to 2.5 times the output speed, at twice the price on Opus 5.5
  ($8 input and $40 output); Opus 5.5, Opus 5 and Opus 4.8 only; first-party API only; not in a batch.
- Bedrock: endpoint `https://bedrock-mantle.<region>.api.aws/anthropic/v1/messages`, model ids with the `anthropic.` prefix, header
  `anthropic-version: 2023-06-01`; no Message Batches, Files API, server-side tools or structured outputs; regional endpoints cost 10 percent
  more than global; default quota 2 million input tokens per minute, up to 5 million input and 500,000 output without extra approval.
- Google Cloud: `rawPredict` URL with the model in it, `anthropic_version` of `vertex-2023-10-16` in the body; global, multi-region (`us`, `eu`)
  and regional endpoints, the last two at a 10 percent premium; specific regions serve Claude Sonnet 4.6 and earlier only; web search and
  structured outputs are available, Message Batches and the Files API are not.
- Not verified on an official page: the resource ARN type to use with the `bedrock-mantle:CreateInference` action (the course checks the
  documented `foundation-model` shape only), and a Vertex `streamRawPredict` URL (not used).

## SDKs by language

| Component | Package | Pinned | Needs |
|---|---|---|---|
| Python client SDK | `anthropic` | 1.11.0 | Python 3.10 or later |
| TypeScript client SDK | `@anthropic-ai/sdk` | 0.131.0 | Node 20 LTS or later, TypeScript 5 or later |
| Java client SDK | `com.anthropic:anthropic-java` | 2.68.0 | Java 8 or later (also used from Kotlin; no separate Kotlin artifact) |
| MCP, Python | `mcp` | 2.2.0 | Python 3.10 or later |
| MCP, TypeScript | `@modelcontextprotocol/sdk` | 1.31.0 | Node 18 or later; peer `zod` |
| MCP, Java | `io.modelcontextprotocol.sdk:mcp` | 2.0.1 | JVM; Tier 2 SDK |
| MCP, Kotlin | `io.modelcontextprotocol:kotlin-sdk` (and `-server`, `-client`) | 0.15.0 | Kotlin; Tier 3 SDK |
| Agent SDK, Python | `claude-agent-sdk` | 0.2.163 | Python 3.10 or later |
| Agent SDK, TypeScript | `@anthropic-ai/claude-agent-sdk` | 0.3.287 | Node 18 or later; peer `zod` 4 |
| Claude Code (CLI) | `@anthropic-ai/claude-code` | 2.1.287 | Node 22 or later when installed through npm |
| MCP protocol revision | specification | 2026-07-28 | |

Supporting libraries seen as dependencies: `pydantic` 2.13.5 (the Python client SDK allows 1.10 and
later); the Python client SDK now depends on `httpx2`, not `httpx`, so the stand-in harness must
intercept at the SDK's transport layer and not assume the old name.

## The Agent SDK at run time

The Agent SDK is a library that drives the Claude Code binary. Both language packages **bundle a
native Claude Code binary**: the Python platform wheels (macOS, Linux; x86-64 and ARM64) carry it
and the TypeScript package gets it through per-platform optional dependencies, so a normal install
needs neither a separate Claude Code install nor Node for the Python SDK. Exceptions: a Python
install that falls back to the source distribution (for example Windows on ARM64) has no binary
and needs Claude Code on `PATH`; a TypeScript install that skips optional dependencies has none
either and must set `pathToClaudeCodeExecutable`. The SDK reads `ANTHROPIC_API_KEY` from the
process environment and does not load `.env` files. It also runs on Bedrock, Google Cloud, Foundry
and Claude Platform on AWS by environment switch. Consequence for the course: graded practices
cannot call the real binary offline, so Agent SDK practices run against the course's scripted
stand-in, and the live path is optional (board rows C-04 and C-05).

## Features each language lacks

| Feature | Python | TypeScript | Java | Kotlin |
|---|---|---|---|---|
| Messages API, streaming, batches, files, tool use, structured outputs | yes | yes | yes | through the Java SDK |
| Agent SDK (`query`, hooks, subagents, sessions, permissions) | yes | yes | **no** | **no** |
| Tool runner helper that drives the tool loop | documented | documented (`toolRunner`, Zod or JSON Schema tools) | not documented: the loop is written by hand, tools derive from annotated classes | same as Java |
| MCP helpers that convert MCP tools and resources into API types | via the `mcp` extra | yes (`mcpTools`, `mcpMessages`) | not documented | not documented |
| Official MCP SDK tier | Tier 1 | Tier 1 | Tier 2 | Tier 3 |
| Cloud-platform clients | extras for Bedrock, Vertex, Foundry and AWS | separate packages for each | separate artifacts for each | through the Java artifacts |
| Beta API features | `beta` namespace | `beta` namespace | `beta()` namespace | through the Java SDK |

A Java or Kotlin team that needs Agent SDK behaviour can run the Claude Code CLI as a subprocess
with `-p` and `--output-format json`, which the Agent SDK documentation names as the route for
other languages; the course teaches that route in the modules marked Python and TypeScript only.
The "tool runner" and "MCP helpers" rows for Java and Kotlin say "not documented" because the
official pages checked do not describe them, not because they were proved absent: C-07 settles
them by running the build.

## Not yet pinned

Python, Node, JDK and Kotlin versions for the container, Gradle, and the test runners are chosen
in the heavy rows C-04 and C-07 and recorded here when they are. Every figure above is a registry
or documentation reading, not yet a run in the container.
