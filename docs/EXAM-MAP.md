# Exam map

How the course maps to Anthropic's Claude certifications. Exam facts change: the official exam
pages are the authority, and this map is re-checked before every publish.

**Confidence:** every fact below is marked **confirmed** (read on an official exam page or exam
guide, version 1.0, effective July 2026) or **reported** (not read on an official page). The
sources are kept outside the repository. Every exam in the table has a public official guide.

## The exams

| Exam | Code | Audience | Items | Fee | Course levels | Status |
|---|---|---|---|---|---|---|
| Claude Certified Associate, Foundations | CCAO-F | Business users and consultants who use Claude as a productivity tool | 60 | $99 | Level 1 | **confirmed** |
| Claude Certified Developer, Foundations | CCDV-F | Engineers building with the API, Claude Code and MCP | 53 | $125 | Levels 1 and 2 | **confirmed** |
| Claude Certified Architect, Foundations | CCAR-F | Solution architects building production applications with Claude | 60 | $125 | Levels 1 to 3 | **confirmed** |
| Claude Certified Architect, Professional | CCAR-P | Mid to senior architects who design, build and govern production Claude solutions | 63 | $175 | Levels 1 to 4 | **confirmed** |

**Common terms (confirmed):** 120 minutes of exam time; multiple-choice and multiple-response items, each item stating how many
answers to select; proctored online or at a Pearson test centre; a scaled score from 100 to 1,000
with 720 to pass, reported as pass or fail with a percent-correct figure per domain; valid for 12
months from the date awarded; renewal on time by a free non-proctored assessment, a lapsed
credential needs the full exam again; retakes after 14, 30 and 90 days following the first,
second and third failed attempt, at most four attempts in a rolling 12 months, each attempt at the
full fee. Cancellation or rescheduling is allowed up to 24 hours before the appointment, and a change inside 24
hours forfeits the fee (confirmed in the guides; Pearson's page for Anthropic exams gives 48 hours, so use
the longer notice). The guides tell candidates to review the Certification Terms and the Exam Policy before
registering; the Policy and Terms (updated 2026-06-25) say a certification is valid for its stated term, must be
renewed before it ends, and cannot be renewed once expired (confirmed). Partner tiers get fee discounts at
checkout (confirmed in the guides). **Reported, not found on any official guide, Terms or Policy read on
2026-10-02:** that candidates must work at an organisation in Anthropic's Claude Partner Network, must register
with a partner e-mail address, must be at least 18, and that the sitting takes about 135 minutes in all with
check-in and survey. The course pages say only what the guides state and tell the reader to check the
registration page. The Associate, Developer and Professional guides state that no course is mandatory and the
experience they describe is recommended (about six months with Claude for the Developer and
Professional exams, three or more years in architecture for the Professional); the Architect
Foundations guide describes a typical candidate with six months or more of hands-on experience. **Exam-day rules (confirmed):** valid
government photo ID matching the registration, a clear workspace with no notes, phones, study
materials or recording devices, no communication with anyone, and no copying of exam content;
exam content is confidential. Fees, eligibility and policy change, so module 11 teaches the format and tells the reader to check the official pages.

## Associate, Foundations (confirmed domains)

The Associate guide's seven domains, with the course's own codes. All domains and weights are
confirmed from the official Claude Certified Associate – Foundations Exam Guide (v1.0, effective
July 2026).

| Domain | Weight | Topics (own words) | Modules |
|---|---|---|---|
| AS1 Prompting and task execution | 14% | clear objectives; stages for a complex request; iterating; a strategy per task type | 6 |
| AS2 Output evaluation and validation | 21% | accuracy and completeness; hallucination; bias; fact-checking; audience fit | 1, 4, 5 |
| AS3 Product and model selection | 12% | chat, Projects, research and artifacts; the three tiers; cost, speed and quality; context limits | 3, 7, 8, 11 |
| AS4 Workflow integration and solution design | 16% | analysing a use case; judgment steps versus automatable steps; explaining value and limits | 5, 9 |
| AS5 Configuration and knowledge management | 12% | Projects, instructions, uploads, connectors; keeping sources current | 7, 8 |
| AS6 Governance, risk and responsible use | 15% | usage policy; sensitive and regulated data; privacy; escalation of policy conflicts | 5, 10 |
| AS7 Troubleshooting and optimisation | 10% | diagnosing a failure type; controlled fixes; cheaper and faster at equal quality | 6 |

## Developer, Foundations (confirmed domains)

| Domain | Weight | Modules |
|---|---|---|
| DV1 Applications and integration | 33.1% | 3, 12, 13, 14, 15, 16, 17, 21, 22, 23, 25, 30, 39, 40 |
| DV2 Model selection and optimisation | 16.8% | 1, 3, 4, 6, 13, 18, 19, 20, 21 |
| DV3 Agents and workflows | 14.7% | 34, 35, 36, 37 |
| DV4 Prompt and context engineering | 11.0% | 1, 4, 6, 24, 25, 28, 29 |
| DV5 Tools and MCP | 10.6% | 26, 27, 32, 33, 31 |
| DV6 Security and safety | 8.1% | 10, 41, 31 |
| DV7 Claude Code | 3.1% | 27, 38, 39 |
| DV8 Evaluation, testing and debugging | 2.6% | 15, 42, 43 |

The official guide numbers its domains in a different order (1 Agents and Workflows, 2 Applications
and Integration, 3 Claude Code, 4 Eval, Testing and Debugging, 5 Model Selection and Optimization,
6 Prompt and Context Engineering, 7 Security and Safety, 8 Tools and MCPs); DV1 to DV8 are the
course's own codes. Domain 2 is a third of the exam and covers requirements, the systems life
cycle, API mechanics (messages, tools, streaming, vision, thinking, caching, third-party platforms,
batch), software-engineering foundations, application design across interfaces, and configuration
management. Claude Code is a small domain of its own (3.1%) and also appears under DV1
(CLAUDE.md, settings, plugins, model pinning). The Level 3 modules deepen DV3, DV5 and DV7 and are
worth reading for the Developer exam too.

## Architect, Foundations (confirmed domains)

| Domain | Weight | Topics (course numbering) | Modules |
|---|---|---|---|
| A1 Agentic architecture and orchestration | 27% | A1.1 the agentic loop; A1.2 coordinator and subagents; A1.3 invoking subagents and passing context; A1.4 multi-step workflows with enforced prerequisites and hand-offs; A1.5 hooks; A1.6 task decomposition; A1.7 session state | 45 to 51 |
| A2 Tool design and MCP integration | 18% | A2.1 tool interfaces; A2.2 structured error replies; A2.3 distributing tools and `tool_choice`; A2.4 MCP servers in Claude Code; A2.5 built-in tools | 52 to 56 |
| A3 Claude Code configuration and workflows | 20% | A3.1 memory file hierarchy; A3.2 commands and skills; A3.3 path-scoped rules; A3.4 plan mode; A3.5 iterative refinement; A3.6 CI use | 57 to 60 |
| A4 Prompt engineering and structured output | 20% | A4.1 explicit criteria; A4.2 few-shot; A4.3 structured output through tools; A4.4 validation and retry; A4.5 batch processing; A4.6 multi-pass review | 61 to 63 |
| A5 Context management and reliability | 15% | A5.1 long-conversation context; A5.2 escalation and ambiguity; A5.3 error propagation; A5.4 large-codebase exploration; A5.5 human review and confidence; A5.6 provenance and uncertainty | 64 to 69 |

The guide lists 30 task statements (1.1 to 1.7, 2.1 to 2.5, 3.1 to 3.6, 4.1 to 4.6, 5.1 to 5.6);
the course's A-numbers follow the guide's task numbering one to one, and every task statement has a
module.

**Scenarios (confirmed):** each sitting draws four of six settings, and the questions sit inside
them: S1 customer support resolution agent, S2 code generation with Claude Code, S3 multi-agent
research system, S4 developer productivity, S5 Claude Code in CI, S6 structured data extraction.
Modules 70 to 75 are one capstone per setting; modules 76 and 77 are two further capstones beyond the six.

**Out of scope (confirmed)** for this exam: fine-tuning and training, authentication, billing and
account management, programming-language specifics, deploying or hosting MCP servers, model
internals, safety-training methods, embeddings and vector-database internals, computer use, vision,
streaming, rate limits and pricing, OAuth and key rotation, cloud-provider configuration, benchmark
comparison, caching internals beyond knowing it exists, and tokenisation. The course still teaches
streaming, vision, caching and model choice at Level 2, because the Developer exam asks about them.

## Architect, Professional (confirmed domains)

The official guide for this exam is public (CCAR-P, 63 items). Its seven domains, with the
course's own codes and the modules that serve them:

| Domain | Weight | What it measures (own words) | Modules |
|---|---|---|---|
| P1 Solution design and architecture | 17% | business problem to Claude solution; end-to-end design with feedback loops; workflow, agentic or augmented pattern; multi-agent orchestration; decomposition; business-value pillars and SLAs | 79, 80, 81, 83 |
| P2 Models, prompting and context engineering | 13% | model trade-offs; system prompts, templates and guardrails; zero-shot, few-shot, chain of thought; context and token budgets; reuse through caching, modular prompts and skills | 82, 84 |
| P3 Integration | 19% | capability bloat in tool and agent set-up; authentication and authorisation gaps; accuracy against latency; observability at scale; RAG pipeline design; retrieval matched to data shape; choosing MCP, API or CLI, or agent-to-agent; progressive discovery against one large context | 85, 86, 87 |
| P4 Evaluation, testing and optimisation | 16% | metrics for accuracy, latency, cost, safety and security; evaluation datasets and test frameworks; A/B tests; diagnosing prompt failure, hallucination and model mismatch; token, latency and cost trade-offs; monitoring | 87, 88, 84, 89 |
| P5 Governance, safety and risk | 14% | guardrails and safety controls; failure modes of language-model systems; human-in-the-loop checks; regulatory compliance (for example GDPR, HIPAA, FedRAMP); fairness and transparency | 90, 81, 83 |
| P6 Stakeholder communication and lifecycle | 14% | structured discovery; explaining decisions and trade-offs; feedback loops and expectations including SLAs; architecture documents and implementation guidance; discovery, design, hand-off, monitoring and iteration | 91, 89 |
| P7 Developer productivity and operational enablement | 7% | configuring Claude tools for teams; AI-assisted developer workflows; debugging and operational support | 92 |

The Professional exam is design and judgment at the level of a solution architect, not a second
round of Claude Code and Agent SDK detail. The guide does not use the six Foundations scenarios; its
own sample items are architecture decisions (least privilege for an agent's tool set,
placing static content ahead of dynamic content for caching, tracing stale retrieval after a
re-index). The guide recommends building at least one end-to-end solution with retrieval,
evaluation and observability.

## Beyond the exam blueprints (X)

| Code | What it marks | Modules |
|---|---|---|
| X Beyond the exam blueprints | Material no published blueprint tests, taught to make the course complete: how models are made, Claude's apps in depth, Claude for every role, setting up the cloud platforms, computer use and the two extra scenario capstones. It is never a topic to be covered for an exam, so the coverage check requires no module per X; it only requires that a module carrying X names a defined code. | 2, 8, 9, 23, 31, 76, 77 |

## How a page uses this map

Every page states the exam codes it serves in its header. The coverage check (board row C-06)
fails if a topic above has no page, or a page names a code that is not here.
