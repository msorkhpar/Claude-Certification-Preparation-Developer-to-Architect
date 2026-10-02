# Feasibility survey (rows C-03 to C-07)

What was run in the container on 2026-10-02, and what it showed. Every graded run used
`--network none`; the only networked steps are the labelled warm-ups that fill a local cache.
Nothing called the Claude API and no key was used.

**Base image used** (the existing runner image, referred to by digest only):
`<namespace>/...-runner@sha256:93d052f3fc8765d09866927f3a95a29778f3d52fdfd4e9f65a6167034ae9578a`,
1.04 GB: Python 3.14.7, pytest 9.1.1, pip 26.2.1, Node 24.21.0, npm 11.19.0, JDK 25.0.4 (Temurin),
Maven 3.9.16 with a 74 MB prime repository (JUnit 5.10.2, surefire 3.2.5, compiler 3.11.0). It has
no Gradle and no Kotlin compiler. A bare `python:3.14.7` image
(`python@sha256:cad9a2c871761c413caa6fdd6441c783451e740a48aaeba60ae62a8b53525ef6`, 191 MB) has no
pytest, so it cannot grade offline without a wheel; the runner image is the right base.

Re-run any row with `/path/to/.heavy-slot/run-heavy.sh ccp-survey <command>`; the script names are
in the last column. `<img>` is the runner image id.

## Table

| Row | Claim | How run | Result | Time | Size | Script |
|---|---|---|---|---|---|---|
| C-06 | Every exam-map topic has a module; every module names a defined code | `tools/check_coverage.py` and `tests/test_check_coverage.py` | 51 topic codes, 94 modules, 0 problems; 8 tests pass, 5 planted defects (unknown code, unknown domain, uncovered topic, uncovered scenario, undefined X) each fail the check | under 1 s | 0 | `python3 tools/check_coverage.py` |
| C-03 | Python practice graded offline with pytest and the standard library | runner image, `--network none`, worktree mounted read-only | reference 6 passed; starter 6 failed (each on an assertion); planted wrong solutions fail on `AssertionError` (stop on text: 2 failed; one user turn per result: 2 failed) | 0.5 to 0.6 s per variant, cold and warm alike (image local; no pull measured) | image 1.04 GB, practice under 10 KB | `tools/survey_c03.sh <img>` |
| C-07 | TypeScript on the base image's Node alone | Node 24.21 `node --test` with built-in type stripping | works with no compiler and no install: reference 6 pass, starter 6 fail, both planted wrongs fail on `AssertionError`; no type checking happens (stripping only), and `enum` or parameter properties are not allowed | 0.3 s per variant | 0 extra | `tools/survey_c07_ts.sh <img>` |
| C-07 | Java with Maven and JUnit 5, offline | `mvn -o`, repository already in the image, release 21 | reference 6 pass; starter 5 fail 1 error; planted wrongs fail on `AssertionFailedError` (2 and 1+1 error) | 2.5 to 2.8 s per variant | 0 extra | `tools/survey_c07_jvm.sh <img>` |
| C-07 | Kotlin with Gradle and JUnit 5, offline after a warm-up | Gradle 9.8.0 (checksum-verified zip), Kotlin plugin 2.4.20, JDK 25; no toolchain pin | reference 6 pass; starter 6 fail; planted wrongs fail on `AssertionFailedError`; the runner image has no Gradle or Kotlin, so a profile must supply them | warm-up 21 s (first resolve), then 12 s per offline run (`--no-daemon`) | Gradle distribution 151 MB zip / 165 MB unpacked, Gradle user home after the warm-up 288 MB | `tools/survey_c07_kt.sh <img> online reference` then `offline <variants>` |
| C-04 | Python SDK wheels install and import offline | `pip download` (warm-up), then `pip install --no-index --find-links` with `--network none` | anthropic 1.11.0, mcp 2.2.0, claude-agent-sdk 0.2.163, pydantic 2.13.5 install (33 wheels, httpx2 2.13.1) and import | install 10 s, import 2.1 s | wheelhouse 110 MB, installed 323 MB | `tools/survey_c04_fetch_py.sh`, `tools/survey_c04_py.sh` |
| C-04 | The Python Agent SDK bundles the Claude Code binary | listed the wheel and the install | yes: `claude_agent_sdk/_bundled/claude`, 241.7 MB (wheel 103 MB); `claude --version` prints 2.1.286 offline, with no model call. Without the Agent SDK the Python set is 93 MB | | 231 MB of the 323 MB | same |
| C-04 | npm SDK packages install and import offline | `npm install` with a local cache (warm-up), then `npm ci --offline` with `--network none` | @anthropic-ai/sdk 0.131.0, @modelcontextprotocol/sdk 1.31.0, @anthropic-ai/claude-agent-sdk 0.3.287, zod 4.6.5 import; typescript 5.9.3 type-checks a file against the SDK types offline | `npm ci` 5 s, import 0.3 s | cache 162 MB, `node_modules` 310 MB | `tools/survey_c04_fetch_npm.sh`, `tools/survey_c04_npm.sh` |
| C-04 | The Agent SDK's native binary comes as a per-platform optional package in npm | `node_modules/@anthropic-ai/claude-agent-sdk-linux-x64` | present: `claude`, 244 MB; the SDK package itself is 5.3 MB; without the binary, `node_modules` is about 76 MB | | 234 MB of 310 MB | same |
| C-04 | JVM SDKs resolve and run offline | Gradle: `anthropic-java` 2.68.0, `io.modelcontextprotocol.sdk:mcp` 2.0.1, `io.modelcontextprotocol:kotlin-sdk` 0.15.0 (warm-up online, then `--offline`, `--network none`) | a Java class builds a client and request params, loads `McpServer` and `McpClient`; a Kotlin `main` loads the Kotlin SDK's `Server` and `Implementation`; 66 runtime jars | warm-up 29 s, offline run 7 s | runtime jars 59 MB (Gradle user home 288 MB in total) | `tools/survey_c04_jvm.sh <img> online sizes run`, then `offline run` |
| C-04 | Java SDK carries a tool runner and MCP helpers | listed the `anthropic-java-core` jar | classes `com.anthropic.helpers.BetaToolRunner`, `BetaRunnableTool` and `McpBetaTool` exist (beta namespace); not exercised against the API, so the VERSIONS "not documented" cells for Java and Kotlin become "present, beta, not run" | | | listing only |
| C-05 | Official SDK driven by a scripted model through its HTTP hook | `anthropic.Anthropic(http_client=httpx2.Client(transport=...))` with a `BaseTransport` subclass | the hook is the `http_client` argument; it must be an `httpx2` client (an `httpx` object is rejected by the SDK). Real `client.messages.create` code, with tool use then end of turn, runs unchanged; error statuses (529) raise the SDK's own `APIStatusError`; a scripted turn is validated against the SDK's `Message` type | 22 tests in 2.4 s | | `tools/survey_c05.sh <img>` |
| C-05 | Replay of a recorded exchange file | `harness/replay.py`, `harness/examples/illustrative_exchange.json` (hand-written, labelled illustrative) | the two-turn exchange replays through the real SDK; a request with the wrong model fails with a replay mismatch | in the 2.4 s above | | same |
| C-05 | Capture scrub check | `harness/scrub.py` and `harness/capture.py` | detects API keys, request ids, organisation and account ids, email addresses and auth headers; 7 planted leaks are each found; a recorder that wraps a transport drops headers and ids and its output passes the check | in the 2.4 s above | | same |
| C-05 | MCP server and client over stdio in the offline container | Python `mcp` 2.2.0: `MCPServer` (one tool, one templated resource) and `ClientSession` over `stdio_client` | initialise, list tools (schema derived from type hints), call a tool, read a resource: all pass | in the 2.4 s above | | same |

## Findings the register needs

- **The starter of C-03 must fail on an assertion.** Its first draft raised `NotImplementedError`,
  which is a `RuntimeError`, so the turn-cap test passed by accident; the starter now returns
  `None`.
- **Python:** the hook is `httpx2`. A harness that imports `httpx` fails in the SDK's own argument
  check. The MCP stdio client does not pass `PYTHONPATH` to its server subprocess (it keeps a
  safe environment list), so the survey sets `env` on `StdioServerParameters`; a profile that
  installs into site-packages needs nothing.
- **MCP 2.x names** are snake_case (`input_schema`, `is_error`), different from the 1.x camel case.
- **TypeScript** needs no compiler to run. A type check needs the pinned `typescript` package from
  the npm profile entry; a practice can be graded without it.
- **Kotlin needs Gradle.** The Kotlin MCP SDK is a Kotlin Multiplatform module that Maven cannot
  resolve from its Gradle module metadata; the survey used Gradle for Kotlin and for the JVM SDK
  proof. Java alone runs on Maven from the existing prime repository.
- **Offline needs a populated local cache** in every language. A reader's first run is offline
  only if the image carries the wheels, the npm cache and the Maven or Gradle repository.

## What a profile image would hold

| Layer | Contents | Size |
|---|---|---|
| Python | wheels for anthropic, mcp, pydantic and their dependencies | about 93 MB installed, 110 MB as wheels with the Agent SDK |
| Python Agent SDK | `claude-agent-sdk` with its bundled binary (graded practices never run it) | adds about 231 MB |
| TypeScript | the three packages, zod and typescript, as an npm cache or `node_modules` | about 76 MB; the Agent SDK's platform binary adds 234 MB |
| JVM | the three SDKs' jars in a Maven repository, plus Gradle 9.8.0 and a Gradle cache for Kotlin | jars 59 MB; Gradle 165 MB; Gradle cache 288 MB |

The two bundled Claude Code binaries make up about 465 MB of the total. Whether a profile may
omit them was not tested: the Python wheel is one artifact, so the binary comes with it, and a
TypeScript install that skips optional dependencies loses it (VERSIONS.md). The survey did not
run either binary against a model.

## Framework changes this implies

- A profile entry kind for **pinned Python wheels** (a wheelhouse with hashes, installed with
  `--no-index`) and one for **pinned npm packages** (a lockfile and a cache, installed with
  `npm ci --offline`); the Maven kind already covers the Java SDK jars.
- A Gradle distribution and Kotlin plugin cache kind, or a Maven path for Kotlin that avoids
  Gradle module metadata; today the base image has neither Gradle nor a Kotlin compiler.
- Per-variant grading as a runner command: the starter, the reference and each wrong solution
  differ only by one directory, selected by an environment variable (Python, TypeScript) or a
  build property (Maven `solution.dir`, Gradle `solution`).

## Preparing the local caches

All caches live in the gitignored `.survey-out/`. Order: `tools/survey_c04_fetch_py.sh <img>`,
`tools/survey_c04_fetch_npm.sh <img>`, and Gradle 9.8.0 fetched once from the official distribution
URL and checked against its published SHA-256
(`bafd5ce9cfaea0fbccfdc8439a1ac42fbd4cd9c89dc9a988228d8a2639a58e6c`) into `.survey-out/gradle/`,
then the `online` warm-up of `tools/survey_c07_kt.sh` and `tools/survey_c04_jvm.sh`. Every other
script runs with `--network none`. The SDK probe project for the JVM is `tools/jvm-sdk/`, the npm
pins are `tools/npm-sdk/package.json`.
