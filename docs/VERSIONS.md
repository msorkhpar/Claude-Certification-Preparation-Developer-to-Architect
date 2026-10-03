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

## Prompts, structured output, tools, extensions, retrieval and context

Read on 2026-10-03 from the Anthropic documentation (Prompting best practices and the pages for Claude Opus 5.5, Claude Sonnet 5.5 and
Claude Fable 5.1; Structured outputs; Tool use overview, How tool use works, Define tools, Handle tool calls, Parallel tool use, Strict
tool use and Tool runner; Handling stop reasons; Embeddings; Search results; Citations; Files API; Context windows; Context editing;
Compaction, Compaction on demand and Compaction at a token threshold; Memory tool), the Claude Code documentation (Extend Claude Code)
and Anthropic's write-up on contextual retrieval. Re-check at release.

- Structured outputs: schema in `output_config.format` with `type: "json_schema"`; `strict: true` on a tool for strict tool use. Not
  supported in a schema: recursive schemas, numerical constraints (`minimum`, `maximum`, `multipleOf`), string constraints (`minLength`,
  `maxLength`), array `minItems` above 1, `additionalProperties` other than `false`; an unsupported feature is a 400 error. Limits per
  request: 20 strict tools, 24 optional parameters, 16 parameters with union types. Enum and const capitalisation is not guaranteed.
  Grammars are cached for 24 hours from last use. `refusal` and `max_tokens` replies may not match the schema.
- `tool_choice`: `auto`, `any`, `tool`, `none`; `any` and `tool` return a 400 error on Claude Opus 5.5, Claude Sonnet 5.5, Claude
  Fable 5.1 and Claude Mythos 5.1, and are an error under manual extended thinking. `disable_parallel_tool_use` is a field of the
  `tool_choice` object. The tool use system prompt is 286 tokens for `auto` and `none` on Opus 5.5 and Sonnet 5.5.
- Tool results: all `tool_result` blocks of one assistant turn go in the next user message, before any text; `is_error: true` reports a
  failure. Stop reasons: `end_turn`, `max_tokens`, `stop_sequence`, `tool_use`, `pause_turn`, `refusal`,
  `model_context_window_exceeded`. The tool runner is a beta feature in the SDKs and takes `max_iterations`.
- Prefilling a last assistant turn is a 400 error on Claude Opus 5.5, Claude Sonnet 5.5 and Claude Fable 5.1 (module 24).
- Embeddings: "Anthropic does not offer its own embedding model"; the documentation names Voyage AI. Contextual retrieval, as reported by
  Anthropic on its own data: top-20 failure rate 5.7% baseline, 3.7% with contextual embeddings (35% fewer), 2.9% with contextual BM25
  added (49%), 1.9% with reranking (67%); under about 200,000 tokens, skip retrieval and use a cached prompt.
- Citations: `citations.enabled` on every document or none; plain text `char_location` (0-indexed, exclusive end), PDF `page_location`
  (1-indexed), custom content `content_block_location`; `cited_text` is not billed as output; incompatible with `output_config.format`
  (400 error). Search result blocks need `source`, `title` and `content`, and carry the same all-or-nothing citation rule.
- Files API: generally available, no beta header; 500 MB per file and 1 TB per organization; files are visible to the whole workspace;
  file content in a request is billed as input tokens; only files created by skills or code execution can be downloaded.
- Context: 1M-token window on the current Opus, Sonnet, Fable and Mythos models, 200k on Claude Sonnet 4.5; context editing uses the
  beta header `context-management-2025-06-27` and `clear_tool_uses_20250919` (defaults: trigger 100,000 input tokens, keep 3 tool uses);
  on-demand compaction uses `compact-2026-09-04` and `"compaction": {"type": "summarize"}`; threshold compaction uses
  `compact-2026-01-12` and `compact_20260112` (trigger default 150,000, minimum 50,000); the memory tool is `memory_20250818`.
- Claude Code: CLAUDE.md loads every session (keep it under 200 lines), skills load descriptions at start and full content when used,
  MCP loads tool names with schemas on demand, hooks cost no context unless they return output, subagents run in isolated context;
  CLAUDE.md files add up, skills and subagents override by name, MCP servers override local over project over user, hooks merge.
- Not verified on an official page: the prompt improver tool in the Console (module 24 names it and marks it unverified), and the exact behaviour of the text
  editor's `undo_edit` command (not used).

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

## Vision, documents, computer use, MCP and the Agent SDK in practice (modules 30 to 35)

Read on **2026-10-03** from the Claude API documentation (Vision, Coordinates and bounding boxes, PDF support, Files API, Computer use
tool, Token counting), the Model Context Protocol specification and documentation of revision 2026-07-28 (architecture, server and client
concepts, versioning, server tools, stdio and Streamable HTTP transports, multi round-trip requests, elicitation, sampling, subscriptions,
progress, cancellation, authorization, security best practices, the Inspector, debugging, the SDK list and the deprecated-features
registry), the Agent SDK pages of the Claude Code documentation (overview, agent loop, permissions, hooks, custom tools, streaming
versus single input, the Python and TypeScript references) and Anthropic's engineering article "Building effective agents" (published
2024-12-19; it carries a note that much of its tooling landscape has changed). Re-check at release.

- Vision: JPEG, PNG, GIF (first frame only) and WebP; image blocks from base64, a URL or a `file_id` (Bedrock and Google Cloud take base64
  only). Limits: 600 images per request (100 on 200k-window models), 8000 by 8000 pixels per image, 10 MB per image on the API and 5 MB on
  Bedrock and Google Cloud, 32 MB per request. More than 20 images in one request puts every image under a stricter per-side limit of
  2000 px. Resizing: standard tier (all other models) 1568 px on the long edge and 1568 visual tokens, high-resolution tier (Claude Opus
  4.7 and later, including the models of the computer use toolset) 2576 px and 4784 tokens; tokens are `ceil(width / 28) * ceil(height / 28)`;
  the model sees the resized picture padded on the bottom and right to a multiple of 28, and its coordinates are in the resized, not the
  padded, picture. Worked: 1920x1080 resizes to 1456x819, an A4 scan of 1075x1520 costs 2145 tokens and is resized to 924x1307 on the
  standard tier. `"transformations": {"oversized_image": "error"}` makes the API reject an image that would be resized.
- PDFs: converted page by page to an image plus extracted text; 32 MB and 600 pages (100 when the window is under 1M tokens); text
  typically 1,500 to 3,000 tokens a page; no PDF surcharge. Files API: 500 MB a file, 1 TB an organisation, no beta header, visible to the
  whole workspace (one workspace per tenant), expiry 3,600 to 7,776,000 seconds set at upload, `.txt`, `.csv` and `.md` upload as
  `text/plain`, `.xlsx` and `.docx` are not accepted in document blocks.
- Computer use: one entry `{"type": "computer_toolset_20260801"}` gives 17 member tools, no beta header; results echo
  `"toolset_name": "computer"`; a batch runs in order and stops at the first failure, later blocks answered with the fixed halt text
  `Not executed: an earlier computer action in this turn failed.`; the entry rejects `name`, `display_width_px`, `display_height_px`,
  `display_number` and `enable_zoom`; screenshots must already fit the image limits (the API does not downscale); the toolset definition
  costs about 4,500 input tokens (about 410 for `zoom`); a screenshot costs roughly 1,000 to 1,800 tokens. On the Claude API and Google
  Cloud, Claude 5.5 and later models accept only this toolset; the earlier `computer_20251124` tool needs a beta header and is for older
  models and other platforms. For Claude Fable 5.1, Opus 5.5 and Sonnet 5.5 the documentation advises against pruning screenshots on the
  client (it invalidates later thinking blocks) and prefers resizing to 2000 px or less and server-side tool result clearing.
- MCP revision 2026-07-28: no handshake, version and capabilities on every request, `server/discover` mandatory, Streamable HTTP without
  protocol sessions or a GET stream, server-to-client requests replaced by multi round-trip requests (an input-required result with
  `inputRequests` and an opaque `requestState` that the server must treat as attacker-controlled), sampling, roots and logging over the
  protocol deprecated (earliest removal: the first revision released on or after 2027-07-28), Dynamic Client Registration deprecated in
  favour of Client ID Metadata Documents, resource-not-found as -32602, errors -32020 to -32022 (-32021 missing client capability, -32022
  unsupported protocol version), `subscriptions/listen` for change notifications, `Origin` validation required on Streamable HTTP, token
  passthrough forbidden, `resource` parameter and PKCE `S256` required of clients. Authorization is optional, applies to HTTP transports, and
  stdio servers take credentials from the environment.
- MCP SDK support, found by running each pinned SDK in the course container: Python `mcp` 2.2.0 serves the handshake versions up to
  2025-11-25 and the modern 2026-07-28 (stateless, `server/discover`, multi round-trip requests through `Resolve`, `Elicit` and `Sample`);
  TypeScript 1.31.0, Java 2.0.1 and Kotlin 0.15.0 contain no support for 2026-07-28 and their latest revision is 2025-11-25. SDK tiers on
  the documentation's list: TypeScript and Python Tier 1, Java Tier 2, Kotlin Tier 3. A Python tool that raises an exception other than
  `ToolError` is reported to the client without the exception's message.
- Agent SDK: a single-shot `query()` that ends on an error result (`error_max_turns`, `error_max_budget_usd`, `error_during_execution`)
  yields that result and then raises, because the binary exits with a nonzero code; `allowed_tools` pre-approves and does not restrict,
  and an `allowed_tools` entry shadows `can_use_tool`; the order is hooks, deny rules, ask rules, mode, allow rules, callback; `allowed_tools`
  does not constrain `bypassPermissions`; a hook deny applies in every mode; omitting the permission mode in the TypeScript SDK can start the
  session in auto mode (read in the 0.3.287 reference), so the course sets it explicitly; a custom tool's uncaught exception reaches Claude
  as an error result carrying the raw message; tool search is on by default and defers in-process MCP tools.
- The stand-in `harness/fake_claude.py` is the course's own scripted replacement for the Claude Code binary, reached through `cli_path`
  (Python) or `pathToClaudeCodeExecutable` (TypeScript). It applies a simplified copy of the documented rules, exits with code 1 after an
  error result and runs in-process MCP tools through `mcp_message` control requests. Its message wording (for example the prefix
  `Permission denied:`) and its message counts are its own, not the real binary's.
- Not verified by a run: the headless CLI route for Java and Kotlin (`-p` and `--output-format json`), the Inspector, the OAuth flow of the
  authorization specification, and the real binary's wording and scheduling; the practices and examples prove the course's code, not those.

## Not yet pinned

Python, Node, JDK and Kotlin versions for the container, Gradle, and the test runners are chosen
in the heavy rows C-04 and C-07 and recorded here when they are. Every figure above is a registry
or documentation reading, not yet a run in the container.
