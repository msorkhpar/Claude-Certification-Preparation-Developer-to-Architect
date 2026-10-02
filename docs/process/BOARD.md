# Board

Milestones are high-level; a row is opened only when its milestone starts (mint only what blocks,
report milestones, not rows). Row status: `todo`, `doing`, `done`.

Milestone order: **M0, M1, then M2 and M3 together, then M4 to M6 level by level, then M7, M8,
M9.**

| ID | Milestone | Status | Exit test |
|---|---|---|---|
| M0 | Project start: goal, idea, setup, exam map, outline draft, board | **done** | The files exist and the owner has read them |
| M1 | Survey: exam facts confirmed, what runs in the container, the harness proved | todo | A feasibility table, every cell run; the exam map marked confirmed where an official page says so; sources and licences recorded outside the repository |
| M2 | Framework readiness: Python wheels in a profile, mock-exam quiz units, Python and TypeScript tabs, optional live key for the editor; planned and tracked on the framework's own boards | todo | A fixture unit goes from markdown to a graded Python practice and a mock exam on the site |
| M3 | Outline frozen, layout settled | todo | The owner's yes; `corpus.json` layout settled |
| M4 | Course prose, level by level | todo | Each level reviewed by the register and a plain-language read |
| M5 | Examples with recorded exchanges, verified offline in the container | todo | Every example's tests pass offline; planted wrong output fails |
| M6 | Practices, quizzes and mock exams | todo | Reference passes, planted wrong solutions fail on assertions, starter fails; quizzes passed the reader |
| M7 | Build with studyforge: ingest, validate, site, crawl | todo | Zero console errors; practices run end to end; editor opens |
| M8 | Narration of the lesson prose | todo | A listened sample from every level |
| M9 | Release-ready: exam facts and model ids re-checked, images, learner `main`, README | todo | A cold pull on a clean machine runs the course |

## Decisions

| # | Decision | Status |
|---|---|---|
| D1 | Name | **proposed:** "Claude Certification Preparation: Developer to Architect", repository `claude-certification-preparation` |
| D2 | Levels | **proposed:** four incremental levels in one site: Foundations, Developer, Architect, Architect Professional; a topic is taught once, at the first level that needs it, and deepened later by link, never repeated |
| D3 | Language | **proposed:** Python for every practice and example; a TypeScript tab where the TypeScript form differs in a way an exam can ask about |
| D4 | The API in practices | **proposed:** graded runs are offline against recorded exchanges (captured once, replayed) and scripted exchanges; live runs optional with the reader's own key, never graded |
| D5 | Exam guide copies | **open:** a circulating copy of an exam guide is marked confidential; proposed: not used at all; the exam map uses only the domain names, weights and scenarios reported publicly, and confirms them on official pages |
| D6 | Architect Professional | **proposed:** Level 4 is provisional and rebuilt when a public blueprint exists |
| D7 | Associate exam | **proposed:** covered by Level 1 only; no separate Associate track |
| D8 | Capture key | **open:** recorded exchanges need one approved capture run per batch with a key from the environment; who supplies it, and a spend cap per run |

## M1 rows (opened now)

| Row | Task | Owner | Status |
|---|---|---|---|
| C-01 | Confirm the exam facts on official pages (names, format, eligibility, domains, weights, scenarios); mark each in `EXAM-MAP.md` confirmed or reported | office (reading) | todo |
| C-02 | Pin versions from the official documentation: current model ids and tiers, Python, the Anthropic SDK, the MCP SDK, the Agent SDK; record which features each version has | office (reading) | todo |
| C-03 | A Python practice end to end in the existing runner, offline, with pytest and the standard library only (an agent loop against a scripted model); sizes and times | office (heavy, slot) | todo |
| C-04 | The SDK wheels offline: what the Anthropic SDK, MCP SDK, Agent SDK, Pydantic and httpx need as pinned wheels; whether the Agent SDK needs the Claude Code binary and Node; what a profile image would hold | office (heavy, slot) | todo |
| C-05 | Harness design: replay through the SDK's transport hook, the scripted model, the capture tool and its id-stripping check; one proof example | office (heavy, slot) | todo |
| C-06 | Coverage check: every exam-map topic has a module, every module names valid codes; a script the gate runs | office | todo |

C-01, C-02 and C-06 are reading and may run beside each other and beside one heavy row. C-03 to
C-05 build or run containers and go through the heavy-job slot one at a time.

## Framework work

Not tracked here. The framework repositories plan it on their own boards, on release branches,
and it must not break any existing course. M2 closes when they report the support ready and this
repository's fixture unit runs on it. What the course needs is stated in `docs/IDEA.md`.

## Log

| Date | Entry |
|---|---|
| 2026-10-02 | Outline draft 2: 80 modules in four levels, widened to the Developer exam's reported topics and the official course topics. |
| 2026-10-02 | Project created: goal, idea, setup, exam map, outline draft, production steps and the board. Nothing has been built or run. |
