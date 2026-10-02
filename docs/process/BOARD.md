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
| D1 | Name | **decided:** "Claude Certification Preparation: Developer to Architect", repository `claude-certification-preparation` |
| D2 | Levels | **decided:** four incremental levels in one site: Foundations, Developer, Architect, Architect Professional; a topic is taught once, at the first level that needs it, and deepened later by link, never repeated |
| D3 | Languages | **decided:** four languages. Python and TypeScript for every example and practice; Java and Kotlin too wherever the topic does not need the Agent SDK (which exists only for Python and TypeScript). One example block with a tab per language; the reader picks a language on the first visit and a remembered switch changes it; a language a topic or practice does not exist in is greyed out and says which languages carry it. Claude Code and MCP configuration files are language-neutral and shown once |
| D4 | The API in practices | **decided:** graded runs are offline against recorded exchanges (captured once, replayed) and scripted exchanges; live runs optional with the reader's own key, never graded |
| D5 | Exam guide | **decided:** the Architect Foundations exam guide is public and is used as the topic map: its domains, topics and scenarios shape Level 3. Its wording and sample questions are not copied; the course writes its own |
| D6 | Architect Professional | **decided:** a survey looks again for a public blueprint or public prep material for the Professional exam (row C-08); if none exists, Level 4 is authored after Level 3 as drafted, marked provisional, and rebuilt when a blueprint is published. **Outcome:** the official blueprint is public; Level 4 is drafted from it and is no longer provisional |
| D7 | Associate exam | **decided:** covered by Level 1 only; no separate Associate track |
| D8 | API key | **decided:** the reader enters their own key in the site's UI, which passes it to the code at run time and never stores it in a file; grading stays offline and deterministic against the scripted model for everyone; with a key, the reader can also run examples and their own solution live, never graded. Examples ship with exchanges captured by the course's authors, labelled with model and date |
| D9 | Key storage and captures | **decided:** the reader's key is typed in the site and kept in the browser tab's session storage by default (gone when the tab closes); "remember on this device" is an opt-in to the browser's local storage. The site sends it with a live-run request; the local service passes it as an environment variable to that run's container only, never writes it to disk and never gives it to the graded runner. A reader may instead set `ANTHROPIC_API_KEY` in their own ignored `course.env`. Authoring uses hand-scripted exchanges labelled illustrative; real exchanges are captured in one pass before release, with the owner's key set in the environment for that run only |
| D10 | Official exam guides | **decided:** all four official exam guides (Associate, Developer, Architect Foundations, Architect Professional) are used as the topic maps of their levels, and their sample questions may be used in the course's quizzes and mock exams, credited on the page where used. Questions from any other source are never copied |

## M1 rows (opened now)

| Row | Task | Owner | Status |
|---|---|---|---|
| C-01 | Confirm the exam facts on official pages (names, format, eligibility, domains, weights, scenarios); mark each in `EXAM-MAP.md` confirmed or reported | office (reading) | **done:** All four exams have a public official guide (v1.0, July 2026); `EXAM-MAP.md` is marked confirmed (codes, items, fees, format, scoring, eligibility, renewal, retakes, domains, weights, six scenarios); the Architect Foundations task statements match the A-topics one to one, two small topics added to modules 59 and 62. |
| C-02 | Pin versions from the official documentation: current model ids and tiers, Python, the Anthropic SDK, the MCP SDK, the Agent SDK; record which features each version has | office (reading) | **done:** `VERSIONS.md` pins model ids, the client, MCP and Agent SDKs per language and the Agent SDK run-time needs (binary bundled); to be re-checked at release. |
| C-03 | A Python practice end to end in the existing runner, offline, with pytest and the standard library only (an agent loop against a scripted model); sizes and times | office (heavy, slot) | **done:** Python practice graded offline in the runner image (reference passes, starter fails, planted wrongs fail on assertions); about 0.6 s per variant; see `FEASIBILITY.md`. |
| C-04 | The SDK wheels offline: what the Anthropic SDK, MCP SDK, Agent SDK, Pydantic and httpx need as pinned wheels; whether the Agent SDK needs the Claude Code binary and Node; what a profile image would hold | office (heavy, slot) | **done:** Python wheels, npm packages and the JVM SDKs install and import offline; sizes per language and the bundled Agent SDK binary (about 235 MB per language) recorded; see `FEASIBILITY.md`. |
| C-05 | Harness design: replay through the SDK's transport hook, the scripted model, the capture tool and its id-stripping check; one proof example | office (heavy, slot) | **done:** `harness/` drives the official SDK through its `httpx2` transport hook; replay, scripted model, scrub check and an MCP stdio proof pass offline (22 tests). |
| C-06 | Coverage check: every exam-map topic has a module, every module names valid codes; a script the gate runs | office | **done:** `tools/check_coverage.py` passes on the current map and outline (51 topics, 83 modules); planted defects fail. |
| C-07 | The other three languages: TypeScript on the base image's Node (type stripping, its test runner) or a pinned compiler; the Java API SDK and the JVM MCP SDK in a Gradle project for Java and Kotlin, offline; which SDK features each language lacks | office (heavy, slot) | **done:** TypeScript runs on the base image's Node with type stripping; Java runs on Maven; Kotlin needs Gradle, which the base image lacks (framework change listed in `FEASIBILITY.md`). |
| C-08 | Look again for a public blueprint, exam description or prep material for the Architect Professional exam; if found, redraft Level 4 from it | office (reading) | **done:** A public official Professional blueprint exists (CCAR-P, seven weighted domains); Level 4 is redrafted from it as modules 72 to 83 and no longer provisional. |

C-01, C-02, C-06 and C-08 are reading and may run beside each other and beside one heavy row. C-03 to
C-05 build or run containers and go through the heavy-job slot one at a time.

## Framework work

Not tracked here. The framework repositories plan it on their own boards, on release branches,
and it must not break any existing course. M2 closes when they report the support ready and this
repository's fixture unit runs on it. What the course needs is stated in `docs/IDEA.md`.

## Log

| Date | Entry |
|---|---|
| 2026-10-02 | Survey C-01, C-02 and C-08 done: exam map confirmed, versions pinned, Level 4 redrafted from the Professional blueprint (83 modules). |
| 2026-10-02 | D10: the official exam guides are the topic maps, and their sample questions may be used with credit. Exam facts confirmed from the official guides; versions pinned; Level 4 drafted from the Professional blueprint. |
| 2026-10-02 | Decisions D2, D4, D6 and D7: four incremental levels, offline grading, Professional level after a fresh search, Associate covered by Level 1. |
| 2026-10-02 | Decisions D1, D3, D5, D8 and D9: name, four languages, the exam guide as topic map, the reader's key in the site and captures before release. |
| 2026-10-02 | Outline draft 2: 80 modules in four levels, widened to the Developer exam's reported topics and the official course topics. |
| 2026-10-02 | Project created: goal, idea, setup, exam map, outline draft, production steps and the board. Nothing has been built or run. |
