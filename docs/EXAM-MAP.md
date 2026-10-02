# Exam map

How the course maps to Anthropic's Claude certifications. Exam facts change: the official exam
pages are the authority, and this map is re-checked before every publish.

**Confidence:** every fact below is marked **confirmed** (read on an official page) or
**reported** (consistent across several public descriptions, not yet read on an official page).
At draft 1 nothing is confirmed: no official exam page has been read yet (board row C-01).

## The exams

| Exam | Audience | Course levels | Status |
|---|---|---|---|
| Claude Certified Associate, Foundations | Business users and consultants | Level 1 | reported |
| Claude Certified Developer, Foundations | Engineers building with the API and MCP | Levels 1 and 2 | reported |
| Claude Certified Architect, Foundations | Solution architects | Levels 1 to 3 | reported |
| Claude Certified Architect, Professional | Senior and enterprise architects | Levels 1 to 4 | reported; no public blueprint |

**Reported common terms:** proctored online or at a test centre, closed book with no AI assistant,
about 120 minutes, multiple choice with one correct answer and three distractors, scenario-based,
a scaled score from 100 to 1000 with 720 to pass, valid for a year with a renewal. **Reported
eligibility:** the exams are offered through Anthropic's partner programme, not open to everyone.
Module 7 teaches the format; the reader checks eligibility on the official page.

## Developer, Foundations (reported domains)

| Domain | Reported weight | Modules |
|---|---|---|
| DV1 Applications and integration | about 33% | 2, 9, 10, 11, 12, 13, 14, 18, 19, 21, 26, 34, 35 |
| DV2 Model selection and optimisation | about 16% | 1, 2, 3, 5, 10, 15, 16, 17, 18 |
| DV3 Agents and workflows | about 14% | 29, 30, 31, 32 |
| DV4 Prompt and context engineering | about 11% | 1, 3, 5, 20, 21, 24, 25 |
| DV5 Tools and MCP | about 10% | 22, 23, 27, 28 |
| DV6 Security and safety | about 8% | 7, 36 |
| DV7 Claude Code | about 3% | 23, 33, 34 |
| DV8 Evaluation, testing and debugging | about 2% | 12, 37, 38 |

The weights agree across several public descriptions but have not been read on an official
page. The Developer exam is reported to reach into Claude Code configuration (memory files,
settings, plugins) under DV1 as well as DV7. The Level 3 modules deepen DV3, DV5 and DV7 and are worth reading for the Developer
exam too.

## Architect, Foundations (reported domains)

| Domain | Reported weight | Topics (course numbering) | Modules |
|---|---|---|---|
| A1 Agentic architecture and orchestration | 27% | A1.1 the agentic loop; A1.2 coordinator and subagents; A1.3 invoking subagents and passing context; A1.4 multi-step workflows with enforced prerequisites and hand-offs; A1.5 hooks; A1.6 task decomposition; A1.7 session state | 40 to 46 |
| A2 Tool design and MCP integration | 18% | A2.1 tool interfaces; A2.2 structured error replies; A2.3 distributing tools and `tool_choice`; A2.4 MCP servers in Claude Code; A2.5 built-in tools | 47 to 51 |
| A3 Claude Code configuration and workflows | 20% | A3.1 memory file hierarchy; A3.2 commands and skills; A3.3 path-scoped rules; A3.4 plan mode; A3.5 iterative refinement; A3.6 CI use | 52 to 55 |
| A4 Prompt engineering and structured output | 20% | A4.1 explicit criteria; A4.2 few-shot; A4.3 structured output through tools; A4.4 validation and retry; A4.5 batch processing; A4.6 multi-pass review | 56 to 58 |
| A5 Context management and reliability | 15% | A5.1 long-conversation context; A5.2 escalation and ambiguity; A5.3 error propagation; A5.4 large-codebase exploration; A5.5 human review and confidence; A5.6 provenance and uncertainty | 59 to 64 |

**Scenarios (reported):** each sitting draws four of six settings, and the questions sit inside
them: S1 customer support resolution agent, S2 code generation with Claude Code, S3 multi-agent
research system, S4 developer productivity, S5 Claude Code in CI, S6 structured data extraction.
Modules 65 to 70 are one capstone per setting.

**Reported out of scope** for this exam: fine-tuning, authentication and billing, hosting MCP
servers, computer use, vision, streaming, rate limits and pricing, caching internals,
tokenisation, cloud-provider specifics, and model training methods. The course still teaches
streaming, vision, caching and model choice at Level 2, because the Developer exam asks about
them.

## Architect, Professional

No public blueprint. Level 4 is provisional and is rebuilt from the blueprint when one is
published.

## How a page uses this map

Every page states the exam codes it serves in its header. The coverage check (board row C-05)
fails if a topic above has no page, or a page names a code that is not here.
