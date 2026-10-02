# Course outline (draft 2)

**Status: a draft for the owner to cut, add to and reorder.** Nothing here is a promise until
milestone M3 freezes it. Each module will hold one to four pages; the page count is set in M3.
Draft 2 widens Level 2 to every topic the Developer exam is reported to cover, and adds the
official Academy's topics (skills, subagents, managed agents, retrieval, enterprise rollout).

**Exam codes used below** (defined in [`EXAM-MAP.md`](EXAM-MAP.md)):

- **DV1 to DV8**: the Developer exam's domains (DV1 applications and integration; DV2 model
  selection and optimisation; DV3 agents and workflows; DV4 prompt and context engineering; DV5
  tools and MCP; DV6 security and safety; DV7 Claude Code; DV8 evaluation, testing and
  debugging).
- **A1 to A5, with topic numbers**: the Architect Foundations exam's domains (A1 agentic
  architecture and orchestration; A2 tool design and MCP integration; A3 Claude Code configuration
  and workflows; A4 prompt engineering and structured output; A5 context management and
  reliability). Topic numbers such as A1.3 are the course's own numbering.
- **S1 to S6**: the Architect exam's six scenario settings (customer support agent; code
  generation with Claude Code; multi-agent research; developer productivity; Claude Code in CI;
  structured data extraction).
- **AS**: the Associate exam. **P**: Architect Professional, provisional.

**Practice kinds:** **code** (Python, graded by pytest against the scripted or replayed model),
**config** (Claude Code or MCP files, graded by tests that validate them and run their hooks),
**quiz** (exam-style scenario questions), **reading** (no practice).

**Versions:** pinned in M1 (model ids, Python, the Anthropic SDK, the MCP SDK and the Agent SDK).
A claim on a page names the versions it was checked on.

## Level 1: Foundations (shared by every exam)

| # | Module | Covers | Exams | Practice |
|---|---|---|---|---|
| 1 | How a language model works, for engineers | Next-token generation, tokens, the context window as working memory, sampling and temperature, why output varies, what the model knows and its cut-off, steerability, hallucination | all; DV2, DV4 | quiz |
| 2 | Claude's family and its surfaces | The three tiers and choosing by capability, speed and cost; the apps, Cowork, Claude Code, the API, the Agent SDK, managed agents, the cloud platforms; one model behind many surfaces; instructions that carry across them | all; DV1, DV2 | quiz |
| 3 | Capabilities and limits | Reasoning, coding, long documents, vision, maths and counting, recency; thinking at a glance; computer use, read only; when not to use a model | DV2, DV4 | quiz |
| 4 | Working with an AI, responsibly | Delegating well, describing a task, judging the output, owning the result; human and agent teams; disclosure | AS; all | quiz |
| 5 | Prompting fundamentals | Clear and direct instructions, context and purpose, roles, XML-tagged structure, zero-, one- and multi-shot, asking for reasoning, output format; the prompt as a specification | DV2, DV4; A4 | code: a prompt template builder graded on structure |
| 6 | Claude in the apps | Projects and standing instructions, artifacts, skills, connectors, research, plugins in the apps, working safely and sharing | AS | quiz |
| 7 | Safety, privacy and policy | The usage policy in practice, sensitive data, prompt injection explained for users, organisational controls at a glance | AS; DV6 | quiz |
| 8 | Exam readiness 1 | How the exams work (format, scenario questions, scoring, eligibility, renewal), how to read a scenario question, the Level 1 mock exam | all | quiz (mock) |

## Level 2: Developer (Claude Certified Developer)

| # | Module | Covers | Exams | Practice |
|---|---|---|---|---|
| 9 | From business need to a testable spec | Functional requirements for a Claude feature; latency, throughput, region and cost budgets; matching an architecture to the requirement; the life cycle: develop, implement, operate, maintain | DV1 | quiz |
| 10 | One REST API under every SDK | HTTP and JSON bodies, headers and versions, what the SDK does for you, server-sent events versus websockets, connection basics | DV1, DV2 | code: a raw HTTP call and the SDK call compared, against replay |
| 11 | The Messages API | Turns, roles and content blocks, the system prompt, `max_tokens`, stop sequences, `stop_reason`, usage fields, the conversation state the client keeps | DV1; A1.1 | code: a conversation client against replay |
| 12 | Errors, retries and timeouts | Error classes by type and origin, which to retry, back-off, timeouts, idempotency, logging without secrets | DV1, DV8 | code: a retry policy graded on scripted failures |
| 13 | Async, concurrency and backpressure | `async` and `await` for API calls, bounded concurrency, rate limits as a client concern, backpressure when streaming | DV1 | code |
| 14 | Streaming | The event types, assembling a message, streaming tool use, when streaming matters | DV1 | code |
| 15 | Model choice, cost and migration | Matching a tier to a workload; quality, latency and cost; reading usage and modelling cost; parameters a newer model rejects; pinning model ids; migrating safely | DV2; P | code: a router and a cost model |
| 16 | Thinking, effort and speed | Extended and adaptive thinking, effort levels, fast mode, thinking with tools, cost | DV2 | code |
| 17 | Prompt caching | The prefix rule, breakpoints, time to live, measuring hits, when caching pays | DV2 | code: order a request for cache hits |
| 18 | Message Batches | Real time versus batch, `custom_id`, results, cost, limits | DV1, DV2; A4.5 | code |
| 19 | Claude on the cloud platforms | Calling Claude through Amazon Bedrock and Google Vertex AI: what differs from the direct API (model ids, auth, features), choosing a platform | DV1; P | code against replay |
| 20 | Prompt engineering for applications | Templates and variables, system versus user placement, constraints, few-shot design, chain of thought, prefilling, long-document placement, prompt chaining, versioning prompts | DV4; A4.1, A4.2 | code |
| 21 | Structured output and defensive parsing | JSON Schema, structured outputs, tool-forced output, validate and re-prompt, parsing defensively, distrusting confident but wrong output | DV1, DV4; A4.3, A4.4 | code: an extractor with validation |
| 22 | Tool use | Tool definitions and schemas, descriptions as the routing contract, the tool loop, `tool_choice`, parallel calls, results and errors, client versus server tools (web search, code execution, text editor), approval gates | DV5; A1.1, A2.1 | code: a tool loop against a scripted model |
| 23 | Choosing an extension | A built-in tool, a custom tool, a skill, an MCP server or a plugin: what each is for and what it costs in context | DV5, DV7 | quiz |
| 24 | Retrieval | Chunking, embeddings, lexical search, combining indexes, reranking, contextual retrieval | DV4 | code: a retrieval pipeline graded on recall |
| 25 | Context engineering | Long documents, citations, the Files API, managing the window in code, pruning tool output, compaction, content and data boundaries, session hygiene | DV4; A5.1 | code |
| 26 | Vision and documents | Images and PDFs as input, what they cost, what Claude reads well and badly | DV1 | code |
| 27 | MCP fundamentals | Hosts, clients and servers; tools, resources and prompts; authoring a server with the Python SDK; stdio; the inspector; a client | DV5; A2.4 | code: an MCP server with tests |
| 28 | MCP advanced | Message types, sampling, roots, log and progress notifications, Streamable HTTP and its state, remote servers and authorisation | DV5 | code |
| 29 | Workflows and agents | Agent or workflow: the criteria; cost, latency and reliability; chaining, routing, parallelisation, orchestrator and workers, evaluator and optimiser | DV3; A1.6 | code: two workflow patterns |
| 30 | The Claude Agent SDK | `query()` and the loop under it, streaming versus single-message input, built-in tools, permissions, custom tools, a custom loop and harness (turns, state, control) | DV3; A1 | code against a scripted model |
| 31 | Managed and self-hosted agents | Anthropic-hosted managed agents versus running your own, configuring a managed agent, what each costs and controls | DV3; P | quiz |
| 32 | Agent frameworks compared | What graph-based, model-driven and typed frameworks are, and when one beats the SDK | DV3 | quiz |
| 33 | Claude Code for developers | Installing, explore, plan, code and commit, the memory file and its precedence, settings and their layers (user, project, local), permission modes, sessions, built-in and custom slash commands, headless and auto modes, initialising a repository | DV7; A3 | config: a project set-up graded by tests |
| 34 | Extending Claude Code | Skills, subagents and their memory, hooks, plugins and their dependencies, sharing them across a team | DV1, DV7; A3.2 | config |
| 35 | Claude in the software life cycle | Git workflows, configuration committed with the code, versioning prompts and settings, reviewing and refactoring integration code, CI and code review with Claude | DV1 | config |
| 36 | Security and safety | Direct and indirect prompt injection, jailbreaks, delimiting untrusted input, data leakage and PII, layered input and output guardrails, least privilege, hooks that block destructive actions, secrets in development and production, identity and access monitoring | DV6; A2.3 | code: an injection-resistant tool gate |
| 37 | Evaluation | Success criteria, test sets, code-graded and model-graded evals, refining prompts against an eval set, regression runs | DV8; A4.4 | code: an eval harness |
| 38 | Debugging Claude applications | Classifying a failure by type and origin, choosing a recovery, reading a trace to tell the integration from the model | DV8 | code |
| 39 | Exam readiness 2 | Developer exam strategy and two Developer mock exams | DV1 to DV8 | quiz (mock) |

## Level 3: Architect (Claude Certified Architect, Foundations)

Ordered by the exam's five domains, then the six scenarios as capstones. Where a Level 2 module
introduced a topic, the Level 3 module deepens it and links back; it does not repeat it.

| # | Module | Covers | Exams | Practice |
|---|---|---|---|---|
| 40 | The agentic loop, in depth | Control flow on `stop_reason`, appending tool results, termination; anti-patterns: parsing text to stop, an iteration cap as the main stop | A1.1 | code |
| 41 | Coordinator and subagents | Hub and spoke, isolated subagent context, choosing subagents dynamically, partitioning scope, iterative refinement | A1.2; S3 | code |
| 42 | Invoking subagents | The task tool and its permission, agent definitions, passing context explicitly, parallel subagents, separating content from metadata | A1.3 | code |
| 43 | Multi-step workflows with guarantees | A prerequisite enforced in code versus asked for in a prompt; structured hand-offs to a human | A1.4; S1 | code: a refund flow that cannot skip identity |
| 44 | Hooks | Tool-call interception, post-tool normalisation, deterministic versus probabilistic compliance, hooks in the Agent SDK and in Claude Code | A1.5, A3 | code and config |
| 45 | Task decomposition | Fixed chains versus adaptive decomposition, per-item and cross-item passes | A1.6 | code |
| 46 | Session state | Resuming, forking, starting fresh with a summary, what each costs | A1.7 | code |
| 47 | Designing tool interfaces | Names, descriptions and boundaries; splitting versus consolidating; what a model needs to choose correctly | A2.1 | code: a tool set graded on schema rules |
| 48 | Tool errors that agents can act on | Structured error replies: category, retryable or not, the error flag; what the loop does with each | A2.2 | code |
| 49 | Distributing tools across agents | Which agent gets which tool, `tool_choice` for control, least privilege | A2.3 | code |
| 50 | MCP in Claude Code | Project and user scope, the project MCP file, environment expansion, resources versus tools | A2.4 | config |
| 51 | The built-in tools | Read, Write, Edit, Bash, Grep, Glob: what each is for and how permissions bound them | A2.5; S4 | config |
| 52 | Memory files and rules | The memory hierarchy (user, project, directory), imports, path-scoped rules | A3.1, A3.3 | config |
| 53 | Commands and skills | Custom commands, skills and their frontmatter (forked context, allowed tools, argument hints), plugins at a glance | A3.2 | config |
| 54 | Plan mode and iterative refinement | Plan versus direct execution, examples and tests as the target, the interview pattern | A3.4, A3.5; S2 | quiz |
| 55 | Claude Code in CI | Headless runs, JSON output and an output schema, automated review that keeps false positives low, test generation | A3.6; S5 | config and code |
| 56 | Criteria and examples | Explicit criteria that cut false positives, two to four targeted examples, when examples beat instructions | A4.1, A4.2 | code |
| 57 | Structured output at the architect level | Tool-forced output, `tool_choice` modes, nullable and "unclear" values, validation, retry with feedback | A4.3, A4.4; S6 | code: an extraction pipeline |
| 58 | Batch and multi-pass review | Batches in a pipeline, several independent passes, combining verdicts | A4.5, A4.6 | code |
| 59 | Keeping what matters in long conversations | What to pin, summarise or drop, case facts kept outside the transcript | A5.1 | code |
| 60 | Escalation and ambiguity | When the agent must ask, hand off or stop; designing the hand-off | A5.2; S1 | code |
| 61 | Errors across agents | How a subagent's failure travels to the coordinator and the user, partial results | A5.3 | code |
| 62 | Exploring a large codebase | Scratchpad files, delegating exploration to subagents, keeping the main context lean | A5.4; S4 | config |
| 63 | Human review and calibrated confidence | Sampling for review, stratified sampling, making a confidence score honest | A5.5 | code |
| 64 | Provenance and uncertainty | Linking claims to sources, recording conflicts and gaps | A5.6; S3 | code |
| 65 | Scenario: customer support agent | A capstone that brings A1, A2 and A5 together | S1 | code (capstone) and quiz |
| 66 | Scenario: code generation with Claude Code | Capstone | S2 | config and quiz |
| 67 | Scenario: multi-agent research system | Capstone | S3 | code (capstone) and quiz |
| 68 | Scenario: developer productivity | Capstone | S4 | config and quiz |
| 69 | Scenario: Claude Code in CI | Capstone | S5 | config and quiz |
| 70 | Scenario: structured data extraction | Capstone | S6 | code (capstone) and quiz |
| 71 | Exam readiness 3 | Architect exam strategy and two Architect mock exams | A1 to A5 | quiz (mock) |

## Level 4: Architect Professional (provisional)

No public blueprint exists for this exam at the time of writing. This level is drafted from the
Foundations blueprint's next steps and from enterprise practice, and it is rebuilt when a
blueprint is published (board D6).

| # | Module | Covers | Exams | Practice |
|---|---|---|---|---|
| 72 | Deployment architecture | The API directly or through a cloud platform, data handling and retention options, regions, choosing for a regulated customer | P | quiz |
| 73 | Rolling Claude out to an organisation | Organisations and groups, which surfaces each group gets, connectors, spend caps, measuring adoption | P | quiz |
| 74 | Governance at scale | Organisation-wide Claude Code policy (managed settings, permissions), approved MCP servers, governing skills and plugins, audit | P | config |
| 75 | Cost and capacity engineering | Rate limits and tiers, caching and batching strategy, routing at scale, budgets and alerts | P | code |
| 76 | Observability and evaluation in production | Tracing agent runs, online evaluation, drift after a model upgrade, incident review | P | code |
| 77 | Reliability of multi-agent systems | Failure isolation, idempotent tools, retries across agents, degradation paths | P | code |
| 78 | Migration and model upgrades at scale | Moving prompts and agents to a new model, regression suites, staged roll-out | P | code |
| 79 | Professional capstone | An enterprise scenario designed end to end, with a written design review | P | code and quiz |
| 80 | Exam readiness 4 | The Professional mock exam (provisional) | P | quiz (mock) |

## Excluded on purpose

- Training, fine-tuning, tokenizer internals and model architecture.
- Cloud-provider console set-up, billing and account administration (module 19 calls Claude on
  the platforms; it does not configure them).
- Computer use beyond one reading page in module 3.
- Audience-specific fluency courses (education, non-profits, creative work); Level 1 teaches the
  shared ideas once.

## Size

80 modules: Level 1 has 8, Level 2 has 31, Level 3 has 32, Level 4 has 9. Level 2 is the widest
because the Developer exam is reported to span the whole platform; Level 3 is the deepest because
the Architect Foundations exam has the most detailed public blueprint and the scenario capstones.
