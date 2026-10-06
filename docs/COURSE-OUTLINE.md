# Course outline

94 modules in four levels, each of one to five pages. The course follows the four exams' published
blueprints: Level 1 covers the Associate exam and the base all exams share, Level 2 every Developer
topic, Level 3 every Architect Foundations task statement, Level 4 the seven Professional domains.
It also covers the official Academy's topics (skills, subagents, managed agents, retrieval,
enterprise rollout) and, to be as complete as possible, topics no blueprint tests.

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
- **X**: beyond the exam blueprints. A module that carries X teaches something no published blueprint tests; it may carry X beside exam codes when part of it is tested.
- **AS**: the Associate exam. **P1 to P7**: the Architect Professional exam's seven domains (P1 solution design and architecture; P2 models, prompting and context engineering; P3 integration; P4 evaluation, testing and optimisation; P5 governance, safety and risk; P6 stakeholder communication and lifecycle; P7 developer productivity and operational enablement).

**Practice kinds:** **code** (Python and TypeScript everywhere, Java and Kotlin wherever the
module does not need the Agent SDK; graded by each language's test runner against the scripted
or replayed model),
**config** (Claude Code or MCP files, graded by tests that validate them and run their hooks),
**quiz** (exam-style scenario questions), **reading** (no practice).

**Languages:** modules whose practice needs the Agent SDK (35, and the Level 3
modules that use its subagent, hook and session features: 47, 49 and 51) are Python and TypeScript
only; every other code module is in all four. Each such module says what a Java or Kotlin team
uses instead.

**Versions:** pinned in [`VERSIONS.md`](VERSIONS.md) (model ids; the Anthropic, MCP and Agent SDKs
per language) and the language run-times.
A claim on a page names the versions it was checked on.

## Level 1: Foundations (shared by every exam)

| # | Module | Covers | Exams | Practice |
|---|---|---|---|---|
| 1 | How a language model works, for engineers | Next-token generation, tokens, the context window as working memory, sampling and temperature, why output varies, what the model knows and its cut-off, steerability, hallucination | all; DV2, DV4 | quiz |
| 2 | How models are made | Pre-training on large text collections, fine-tuning, reinforcement learning from human feedback and constitutional training at a concept level; tokenizers and why tokens are not words; the transformer architecture and attention as an idea; what training does and does not give a model; no training practice | X | quiz |
| 3 | Claude's family and its surfaces | The three tiers and choosing by capability, speed and cost; the apps, Cowork, Claude Code, the API, the Agent SDK, managed agents, the cloud platforms; one model behind many surfaces; instructions that carry across them | all; DV1, DV2 | quiz |
| 4 | Capabilities and limits | Reasoning, coding, long documents, vision, maths and counting, recency; thinking at a glance; computer use at a glance (module 31 teaches it); when not to use a model | DV2, DV4 | quiz |
| 5 | Working with an AI, responsibly | Delegating well, describing a task, judging the output (accuracy and completeness, bias, fact-checking, audience fit), owning the result; which steps need judgment and which can be automated; explaining Claude's value and limits to colleagues; human and agent teams; disclosure | AS; all | quiz |
| 6 | Prompting fundamentals | Clear and direct instructions, context and purpose, roles, XML-tagged structure, zero-, one- and multi-shot, asking for reasoning, output format; the prompt as a specification; stages for a complex request, a strategy per task type; diagnosing a weak output (missing context, ambiguity, wrong feature or model) and changing one thing at a time | DV2, DV4; A4 | code: a prompt template builder graded on structure |
| 7 | Claude in the apps | Projects and standing instructions, artifacts, skills, connectors, research, plugins in the apps, keeping instructions and knowledge sources current (stale sources, source quality, version control), browser and office integrations, working safely and sharing | AS | quiz |
| 8 | Claude's apps in depth | Cowork, Claude in Chrome, Claude for Microsoft 365 and Claude Tag; projects, artifacts, connectors and research worked through on real tasks; choosing the surface for a job; what each app can reach and what it must not | AS; X | quiz |
| 9 | Claude for every role | The fluency ideas applied for builders, students, educators, non-profits, small businesses and creative work: what each audience delegates, checks and owns, and how the shared ideas of module 5 change in each setting | AS; X | quiz |
| 10 | Safety, privacy and policy | The usage policy in practice, sensitive data and data classes (anonymise before upload), prompt injection explained for users, organisational controls at a glance, where to escalate a policy conflict | AS; DV6 | quiz |
| 11 | Exam readiness 1 | The full exam logistics: the four exams and how to choose, booking and fees, online proctoring rules and the test-centre option, accommodations, retakes, renewal and the badge; how the exams work (format, scenario questions, scoring, eligibility); how to read a scenario question; revision aids (a flashcard set and a spaced-review question bank for Level 1); two Level 1 mock exams of 60 questions, with multiple-response items | all | quiz (mock), flashcards and a review bank |

## Level 2: Developer (Claude Certified Developer)

| # | Module | Covers | Exams | Practice |
|---|---|---|---|---|
| 12 | From business need to a testable spec | Functional requirements for a Claude feature; latency, throughput, region and cost budgets; matching an architecture to the requirement; the life cycle: develop, implement, operate, maintain | DV1 | quiz |
| 13 | One REST API under every SDK | HTTP and JSON bodies, headers and versions, what the SDK does for you, server-sent events versus websockets, connection basics | DV1, DV2 | code: a raw HTTP call and the SDK call compared, against replay |
| 14 | The Messages API | Turns, roles and content blocks, the system prompt, `max_tokens`, stop sequences, `stop_reason`, usage fields, the conversation state the client keeps | DV1; A1.1 | code: a conversation client against replay |
| 15 | Errors, retries and timeouts | Error classes by type and origin, which to retry, back-off, timeouts, idempotency, logging without secrets | DV1, DV8 | code: a retry policy graded on scripted failures |
| 16 | Async, concurrency and backpressure | `async` and `await` for API calls, bounded concurrency, rate limits as a client concern, backpressure when streaming | DV1 | code |
| 17 | Streaming | The event types, assembling a message, streaming tool use, when streaming matters | DV1 | code |
| 18 | Model choice, cost and migration | Matching a tier to a workload; quality, latency and cost; reading usage, counting tokens before sending and modelling cost; parameters a newer model rejects; pinning model ids; migrating safely | DV2; P2, P4 | code: a router and a cost model |
| 19 | Thinking, effort and speed | Extended and adaptive thinking, effort levels, fast mode, thinking with tools, cost | DV2 | code |
| 20 | Prompt caching | The prefix rule, breakpoints, time to live, measuring hits, when caching pays | DV2 | code: order a request for cache hits |
| 21 | Message Batches | Real time versus batch, `custom_id`, results, cost, limits, trying the prompt on a sample first, failed items, planning around the processing window | DV1, DV2; A4.5 | code |
| 22 | Claude on the cloud platforms | Calling Claude through Amazon Bedrock and Google Vertex AI: what differs from the direct API (model ids, auth, features), choosing a platform | DV1; P3 | code against replay |
| 23 | Setting up Claude on the cloud platforms | Bedrock and Vertex AI access: identity and permissions, quotas and limits, regions, model access requests, per-platform model ids and feature gaps; reading and checking the configuration (module 22 calls Claude on the platforms, this module prepares them) | DV1; P3; X | config: platform settings graded by tests that validate them |
| 24 | Prompt engineering for applications | Templates and variables, system versus user placement, constraints, few-shot design, chain of thought, prefilling, long-document placement, prompt chaining, versioning prompts, principles versus conditionals, prompt dilution, clarifying questions and stated assumptions | DV4; A4.1, A4.2 | code |
| 25 | Structured output and defensive parsing | JSON Schema, structured outputs, tool-forced output, validate and re-prompt, parsing defensively, distrusting confident but wrong output | DV1, DV4; A4.3, A4.4 | code: an extractor with validation |
| 26 | Tool use | Tool definitions and schemas, descriptions as the routing contract, the tool loop, `tool_choice`, parallel calls, results and errors, client versus server tools (web search, code execution, text editor), approval gates | DV5; A1.1, A2.1 | code: a tool loop against a scripted model |
| 27 | Choosing an extension | A built-in tool, a custom tool, a skill, an MCP server or a plugin: what each is for and what it costs in context | DV5, DV7 | quiz |
| 28 | Retrieval | Chunking, embeddings, lexical search, combining indexes, reranking, contextual retrieval | DV4 | code: a retrieval pipeline graded on recall |
| 29 | Context engineering | Long documents, citations, the Files API, managing the window in code (sliding window, progressive summaries, API-native context editing and the memory tool), pruning tool output, compaction, where content sits in the window (attention), content and data boundaries, session hygiene | DV4; A5.1 | code |
| 30 | Vision and documents | Images and PDFs as input, what they cost, what Claude reads well and badly | DV1 | code |
| 31 | Computer use | The computer-use tool and the agent loop around screenshots and actions; coordinates, scaling and the action set; where it fails; safety and sandboxing (an isolated machine, no secrets, human confirmation, injection through the screen) | DV5, DV6; X | code against recorded exchanges |
| 32 | MCP fundamentals | Hosts, clients and servers; tools, resources and prompts; authoring a server with the Python SDK; stdio; the inspector; a client | DV5; A2.4 | code: an MCP server with tests |
| 33 | MCP advanced | Message types, sampling, roots, log and progress notifications, Streamable HTTP and its state, remote servers and authorisation | DV5 | code |
| 34 | Workflows and agents | Agent or workflow: the criteria; cost, latency and reliability; chaining, routing, parallelisation, orchestrator and workers, evaluator and optimiser | DV3; A1.6 | code: two workflow patterns |
| 35 | The Claude Agent SDK | `query()` and the loop under it, streaming versus single-message input, built-in tools, permissions, custom tools, a custom loop and harness (turns, state, control) | DV3; A1 | code against a scripted model |
| 36 | Managed and self-hosted agents | Anthropic-hosted managed agents versus running your own, configuring a managed agent, what each costs and controls | DV3; P1 | quiz |
| 37 | Agent frameworks compared | What graph-based, model-driven and typed frameworks are, and when one beats the SDK | DV3 | quiz |
| 38 | Claude Code for developers | Installing, explore, plan, code and commit, the memory file and its precedence, settings and their layers (user, project, local), permission modes, sessions, built-in and custom slash commands, headless and auto modes, initialising a repository | DV7; A3 | config: a project set-up graded by tests |
| 39 | Extending Claude Code | Skills, subagents and their memory, hooks, plugins and their dependencies, sharing them across a team, skills compared with other features, troubleshooting a skill | DV1, DV7; A3.2 | config |
| 40 | Claude in the software life cycle | Git workflows, configuration committed with the code, versioning prompts and settings, reviewing and refactoring integration code, CI and code review with Claude, workflows triggered by repository events (for example GitHub Actions and webhooks) | DV1 | config |
| 41 | Security and safety | Direct and indirect prompt injection, jailbreaks, delimiting untrusted input, data leakage and PII, layered input and output guardrails, least privilege, hooks that block destructive actions, secrets in development and production, identity and access monitoring | DV6; A2.3 | code: an injection-resistant tool gate |
| 42 | Evaluation | Success criteria, test sets, code-graded and model-graded evals, refining prompts against an eval set, regression runs | DV8; A4.4 | code: an eval harness |
| 43 | Debugging Claude applications | Classifying a failure by type and origin, choosing a recovery, reading a trace to tell the integration from the model | DV8 | code |
| 44 | Exam readiness 2 | Developer exam strategy; revision aids (a flashcard set and a spaced-review question bank for Level 2); two Developer mock exams with multiple-response items | DV1 to DV8 | quiz (mock), flashcards and a review bank |

## Level 3: Architect (Claude Certified Architect, Foundations)

Ordered by the exam's five domains, then the six scenarios as capstones. Where a Level 2 module
introduced a topic, the Level 3 module deepens it and links back; it does not repeat it.

| # | Module | Covers | Exams | Practice |
|---|---|---|---|---|
| 45 | The agentic loop, in depth | Control flow on `stop_reason`, appending tool results, termination; anti-patterns: parsing text to stop, an iteration cap as the main stop; repeated-identical-call detection, escalation when progress stalls | A1.1 | code |
| 46 | Coordinator and subagents | Hub and spoke, isolated subagent context, choosing subagents dynamically, partitioning scope, iterative refinement; when the coordinator should not delegate at all; subagents return structured extracts with citations and confidence | A1.2; S3 | code |
| 47 | Invoking subagents | The task tool and its permission, agent definitions, passing context explicitly, parallel subagents, separating content from metadata; structured extracts with citations and confidence | A1.3 | code |
| 48 | Multi-step workflows with guarantees | A prerequisite enforced in code versus asked for in a prompt; structured hand-offs to a human; policy caps and authorisation for irreversible tools enforced in code; authorisation checkpoints or reversible mechanisms for irreversible effects | A1.4; S1 | code: a refund flow that cannot skip identity |
| 49 | Hooks | Tool-call interception, post-tool normalisation, deterministic versus probabilistic compliance, hooks in the Agent SDK and in Claude Code; policy enforcement via hooks | A1.5, A3 | code and config |
| 50 | Task decomposition | Fixed chains versus adaptive decomposition, per-item and cross-item passes | A1.6 | code |
| 51 | Session state | Resuming, forking, starting fresh with a summary, what each costs | A1.7 | code |
| 52 | Designing tool interfaces | Names, descriptions and boundaries; splitting versus consolidating; what a model needs to choose correctly; paging large results, tool annotations and how far to trust them, large tool sets and tool search | A2.1 | code: a tool set graded on schema rules |
| 53 | Tool errors that agents can act on | Structured error replies: category, retryable or not, the error flag; what the loop does with each; protocol errors versus tool-execution errors, a failed call whose side effect is uncertain and when a retry is safe | A2.2 | code |
| 54 | Distributing tools across agents | Which agent gets which tool, `tool_choice` for control, least privilege; policy caps and authorisation for irreversible tools | A2.3 | code |
| 55 | MCP in Claude Code | Project and user scope, the project MCP file, environment expansion, resources versus tools | A2.4 | config |
| 56 | The built-in tools | Read, Write, Edit, Bash, Grep, Glob: what each is for and how permissions bound them; falling back to Read and Write when Edit cannot apply | A2.5; S4 | config |
| 57 | Memory files and rules | The memory hierarchy (user, project, directory), imports, path-scoped rules; the `/memory` command; oversized instruction files dilute priority guidance | A3.1, A3.3 | config |
| 58 | Commands and skills | Custom commands, skills and their frontmatter (forked context, allowed tools, argument hints), plugins at a glance | A3.2 | config |
| 59 | Plan mode and iterative refinement | Plan versus direct execution, examples and tests as the target, the interview pattern | A3.4, A3.5; S2 | quiz |
| 60 | Claude Code in CI | Headless runs, JSON output and an output schema, automated review that keeps false positives low, test generation, scheduled runs, checking an unsupervised run; headless runs that return output matching a schema (the `--json-schema` flag) | A3.6; S5 | config and code |
| 61 | Criteria and examples | Explicit criteria that cut false positives, two to four targeted examples, when examples beat instructions; principles versus long lists of conditions, prompt dilution, asking a clarifying question or stating an assumption | A4.1, A4.2 | code |
| 62 | Structured output at the architect level | Tool-forced output, `tool_choice` modes, nullable and "unclear" values, validation, retry with feedback; syntax versus semantic errors, reducing fabrication, grounding values in the source, long and scattered documents; accuracy measured only on validated records is biased—evaluate across all documents; provenance as a required schema field | A4.3, A4.4; S6 | code: an extraction pipeline |
| 63 | Batch and multi-pass review | Batches in a pipeline, several independent passes, combining verdicts; a self-review pass and its limits; failed items and the processing window; independent verification of confident-but-wrong output beyond self-check | A4.5, A4.6 | code |
| 64 | Keeping what matters in long conversations | What to pin, summarise or drop, case facts kept outside the transcript, trimming tool output, placing key facts where the model attends to them (the lost-in-the-middle effect); sliding windows and progressive summaries, API-native context editing, a returning user's stale data and updates that arrive mid-conversation; cross-customer contamination prevention via conversation-scope isolation | A5.1 | code |
| 65 | Escalation and ambiguity | When the agent must ask, hand off or stop; designing the hand-off; an ambiguous record match is a question, not a guess; escalation triggers: policy gap, explicit customer request, stalled progress; scope isolation to prevent cross-customer contamination | A5.2; S1 | code |
| 66 | Errors across agents | How a subagent's failure travels to the coordinator and the user, partial results | A5.3 | code |
| 67 | Exploring a large codebase | Scratchpad files, delegating exploration to subagents (the Explore subagent), `/compact`, keeping the main context lean, state manifests that let a coordinator recover after a crash | A5.4; S4 | config |
| 68 | Human review and calibrated confidence | Sampling for review, stratified sampling, making a confidence score honest; authorisation checkpoints for irreversible effects; accuracy evaluated across all document records | A5.5 | code |
| 69 | Provenance and uncertainty | Linking claims to sources, recording conflicts and gaps; dates for correct interpretation, rendering each content type fittingly, coverage notes on what a synthesis could not cover; provenance as a required schema field | A5.6; S3 | code |
| 70 | Scenario: customer support agent | A capstone that brings A1, A2 and A5 together | S1 | code (capstone) and quiz |
| 71 | Scenario: code generation with Claude Code | Capstone | S2 | config and quiz |
| 72 | Scenario: multi-agent research system | Capstone | S3 | code (capstone) and quiz |
| 73 | Scenario: developer productivity | Capstone | S4 | config and quiz |
| 74 | Scenario: Claude Code in CI | Capstone | S5 | config and quiz |
| 75 | Scenario: structured data extraction | Capstone | S6 | code (capstone) and quiz |
| 76 | Scenario: conversational AI assistant | Beyond the exam's six settings. A capstone for a multi-turn assistant: persona and system prompt, memory across sessions, context limits, safe handling of sensitive talk, escalation to a person, evaluation of conversation quality | X | code (capstone) and quiz |
| 77 | Scenario: agentic tool builder | Beyond the exam's six settings. A capstone in which Claude builds and runs its own tools: designing a tool the agent can call, generating and sandboxing code, validating results, permissions and approval gates, auditing what the agent built | X | code (capstone) and quiz |
| 78 | Exam readiness 3 | Architect exam strategy; revision aids (a flashcard set and a spaced-review question bank for Level 3); two Architect mock exams and a scenario question pool, with multiple-response items | A1 to A5 | quiz (mock), flashcards and a review bank |

## Level 4: Architect Professional (Claude Certified Architect, Professional)

Built from the exam's seven published domains. The exam tests design judgment and
communication at the level of a solution architect: choosing patterns, protocols and models,
evaluating and governing a system, and explaining the trade-offs to the people who fund it. Where
a Level 2 or 3 module introduced a topic, the Level 4 module deepens it from the architect's seat
and links back.

| # | Module | Covers | Exams | Practice |
|---|---|---|---|---|
| 79 | From business problem to solution | Translating a business need into a Claude solution; choosing workflow, agentic or augmented-model design; tying the design to value (efficiency, transformation, productivity, cost, performance targets); automation boundary and deploying organisation accountability; pilot-to-scale assumptions and defining needed accuracy and error cost | P1 | quiz |
| 80 | End-to-end and multi-agent architecture | Input, processing, output and feedback loops; decomposing a complex problem; when several agents earn their cost; orchestration choices and their failure modes; reference architectures and their anti-patterns; pilot-to-scale assumptions and defining needed accuracy and error cost | P1 | code: an architecture review against a rubric |
| 81 | Reliability of multi-agent systems | Failure isolation between agents, idempotent tools, retries across agents without duplicated side effects, timeouts and circuit breakers, degradation paths when an agent or tool is down, recovering after a crash; deepens modules 66 and 46 | P1, P5 | code: a multi-agent run graded on injected failures |
| 82 | Models, prompts and context as design choices | Model trade-offs for a workload; system prompts, templates and guardrails; zero-shot, few-shot and chain of thought as design options; token and context budgets; reuse through caching, modular prompts and skills | P2 | code |
| 83 | Deployment architecture and data handling | Direct API versus the cloud platforms as a design choice; data retention and zero-data-retention options; regions and data residency; isolation of customer data; regulated customers and what to ask a vendor; links to modules 22 and 23; de-identify or tokenise protected data before model calls; audit logging versus data-protection retention requirements | P1, P5 | quiz and config |
| 84 | Cost and capacity engineering | Rate limits and usage tiers; capacity planning and headroom; caching and batching strategy as a budget lever; routing across models at scale; budgets, alerts and showback; deepens modules 18, 20 and 21; internal gateway for centralised authentication, rate limiting, model routing; accept-and-poll when a caller has a hard latency limit | P2, P4 | code: a capacity and cost model |
| 85 | Retrieval pipelines at design level | Chunking and indexing strategies; matching retrieval to data shape and query pattern; freshness after re-indexing; evaluating retrieval separately from generation; for knowledge that changes, retrieval versus fine-tuning versus putting it in the prompt | P3 | code: a pipeline graded on recall and staleness |
| 86 | Integration choices, access and capability bloat | MCP versus direct API or CLI versus agent-to-agent; progressive discovery versus one large context; spotting capability bloat; authentication and authorisation gaps; the accuracy and latency trade; tool search and progressive availability at design level; internal gateway for centralised authentication and rate limiting | P3 | quiz and config |
| 87 | Observability at scale | What to log and trace across agents and tools, sampling, dashboards and alerts, tracing a bad answer back to its cause; drift detection; internal gateway centralises authentication, rate limiting, model routing | P3, P4 | code |
| 88 | Evaluation and optimisation | Metrics for accuracy, latency, cost, safety and security; building evaluation sets and mixed test methods; A/B tests; diagnosing prompt failure, hallucination and model mismatch; cost and latency trade-offs; shadow testing before a change goes live; expectation management: segmented accuracy by case type with failure shapes and costs | P4 | code |
| 89 | Migration and model upgrades at scale | Regression suites that gate an upgrade; staged roll-out and rollback; model deprecations and their calendar; prompt and parameter changes a new model needs; deepens modules 18 and 42 | P4, P6 | code: a roll-out gate graded on a regression suite |
| 90 | Governance, safety and risk | Guardrails and safety controls; failure modes and limits of language-model systems; human-in-the-loop design; compliance regimes at design level (GDPR, HIPAA, FedRAMP); fairness and transparency; degrading safely when a control fails; independent verification of confident-but-wrong output; de-identify protected data before calls; automation boundary and organisation accountability; audit versus data-protection retention | P5 | quiz and config |
| 91 | Stakeholders and the project lifecycle | Structured discovery; explaining decisions and trade-offs to technical and executive audiences; expectations and SLAs; architecture documents and hand-off; discovery, design, hand-off, monitoring and iteration; automation boundary and organisation accountability; expectation management with segmented accuracy; pilot-to-scale validation | P6 | written design record graded on a rubric, and quiz |
| 92 | Enabling teams and operations | Claude Code and tool environments for a team; AI-assisted developer workflows; supporting debugging and operational incidents; organisations and groups, spend caps, governing connectors and customisations, seeing adoption; server-managed settings that users cannot override, and a managed plugin marketplace; managed-settings precedence; enablement measured by outcome adoption, not activity | P7 | config |
| 93 | Professional capstone | An enterprise scenario designed end to end, with retrieval, evaluation, observability and a written design review | P1 to P7 | code and quiz |
| 94 | Exam readiness 4 | Professional exam strategy; revision aids (a flashcard set and a spaced-review question bank for Level 4); two Professional mock exams of 63 questions, with multiple-response items | P1 to P7 | quiz (mock), flashcards and a review bank |

## Beyond the exams

Nothing is excluded. These modules go past the official blueprints and carry the code
`X` (some beside exam codes, where part of the module is tested): how models are made (2), Claude's
apps in depth (8), Claude for every role (9), setting up Claude on the cloud platforms (23),
computer use (31), and the two extra scenario capstones, the conversational AI assistant (76) and
the agentic tool builder (77). Four Level 4 modules cover topics the blueprint covers only
lightly and carry exam codes: reliability of multi-agent systems (81), deployment architecture and
data handling (83), cost and capacity engineering (84), and migration and model upgrades at
scale (89). Modules marked X are never needed to pass; they are there to make the course complete.

## Size

94 modules: Level 1 has 11, Level 2 has 33, Level 3 has 34, Level 4 has 16. Level 2 is the widest
because the Developer exam spans the whole platform; Level 3 is the deepest because the Architect
Foundations exam has the most detailed blueprint and the scenario capstones (the six official
ones, then two beyond them); Level 4 adds deployment, cost, reliability and migration modules for
topics its blueprint touches only lightly.
