# Board

Milestones are high-level; a row is opened only when its milestone starts (mint only what blocks,
report milestones, not rows). Row status: `todo`, `doing`, `done`.

Milestone order: **M0, M1, then M2 and M3 together, then M4 to M6 level by level, then M7, M8,
M9.**

| ID | Milestone | Status | Exit test |
|---|---|---|---|
| M0 | Project start: goal, idea, setup, exam map, outline draft, board | **done** | The files exist and the owner has read them |
| M1 | Survey: exam facts confirmed, what runs in the container, the harness proved | **done** | A feasibility table, every cell run; the exam map marked confirmed where an official page says so; sources and licences recorded outside the repository |
| M2 | Framework readiness: Python wheels in a profile, mock-exam quiz units, Python and TypeScript tabs, optional live key for the editor; planned and tracked on the framework's own boards | todo | A fixture unit goes from markdown to a graded Python practice and a mock exam on the site |
| M3 | Outline frozen, layout settled | **done:** outline draft 5 frozen (94 modules, D13) | The owner's yes; `corpus.json` layout settled |
| M4 | Course prose, level by level | doing: Level 1 modules 1-11 written; Level 2 modules 12-35 written (57 pages) (prose complete for Level 1; module 11 pages and mock exam included), awaiting review | Each level reviewed by the register and a plain-language read |
| M5 | Examples with recorded exchanges, verified offline in the container | doing: Level 1 modules 1-6; Level 2 modules 12-35 (twenty-two examples in Python and TypeScript, harness extended with a scripted stand-in for the Claude Code binary) | Every example's tests pass offline; planted wrong output fails |
| M6 | Practices, quizzes and mock exams | doing: Level 1 modules 1-11 (quizzes for every page and module, the Level 1 mock exam of 30 questions, a flashcard set and a spaced-review bank), awaiting the independent quiz reader; Level 2 modules 12-35 (twenty-one practices proved offline, those of modules 12-34 in four languages and module 35 in Python and TypeScript; quizzes for every page and module; modules 18-23 passed one independent reader and one fix round; modules 24-29 await the register's check; modules 30-35 passed one independent reader and one fix round, see the log) | Reference passes, planted wrong solutions fail on assertions, starter fails; quizzes passed the reader |
| Q-L1 | A quiz polish pass over all of Level 1: one independent reader judges the whole level | done | Level 1 judged by readers; remaining WEAK items are listed in docs/process/QUIZ-POLISH.md |
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
| D11 | Third-party material | **decided:** third-party prep material (study guides, practice-question sets, course tables of contents) is read as a reference to check that the course covers every topic it lists. A source whose licence permits copying with credit (for example MIT, Apache 2.0, CC BY, CC BY-SA) may be copied or adapted, questions included, and is credited on the page where it is used, with its licence's terms kept (share-alike material stays under its licence). The course is non-commercial (board D12), so a non-commercial licence (CC BY-NC, CC BY-NC-SA) also permits copying with credit. A publicly available source with no licence or an unclear licence is studied in full, as a learner would: its explanations, depth, examples and order teach the authors what and how to cover, and the course then writes its own understanding in its own wording, examples and questions, without copying The register outside the repository records each source's licence |
| D12 | Purpose | **decided:** the course is non-commercial, made for study and exam preparation; it is never sold or put behind a paywall, so material under non-commercial licences may be used with credit and keeps its licence's terms |
| D13 | Comprehensiveness | **decided:** every module of draft 4 is kept and nothing is cut; what earlier drafts took out is restored. The course aims to be the most comprehensive course possible on its subject. Draft 5 adds how models are made, Claude for every role, Claude's apps in depth, computer use, cloud-platform set-up, two scenario capstones beyond the exam's six, four Level 4 modules (deployment and data handling, cost and capacity, multi-agent reliability, migration at scale), full exam logistics and revision aids per level. Modules no blueprint tests carry the code `X` in the exam map |
| D14 | Quiz bar | **decided:** a batch merges when its quizzes have no FAIL from an independent reader and every automatic check passes; WEAK items go to a polish list; from Level 2 on, each writing batch runs an independent reader and one fix round inside the batch before hand-back |

## M1 rows (opened now)

| Row | Task | Owner | Status |
|---|---|---|---|
| C-01 | Confirm the exam facts on official pages (names, format, eligibility, domains, weights, scenarios); mark each in `EXAM-MAP.md` confirmed or reported | office (reading) | **done:** All four exams have a public official guide (v1.0, July 2026); `EXAM-MAP.md` is marked confirmed (codes, items, fees, format, scoring, eligibility, renewal, retakes, domains, weights, six scenarios); the Architect Foundations task statements match the A-topics one to one, two small topics added to modules 64 and 67. |
| C-02 | Pin versions from the official documentation: current model ids and tiers, Python, the Anthropic SDK, the MCP SDK, the Agent SDK; record which features each version has | office (reading) | **done:** `VERSIONS.md` pins model ids, the client, MCP and Agent SDKs per language and the Agent SDK run-time needs (binary bundled); to be re-checked at release. |
| C-03 | A Python practice end to end in the existing runner, offline, with pytest and the standard library only (an agent loop against a scripted model); sizes and times | office (heavy, slot) | **done:** Python practice graded offline in the runner image (reference passes, starter fails, planted wrongs fail on assertions); about 0.6 s per variant; see `FEASIBILITY.md`. |
| C-04 | The SDK wheels offline: what the Anthropic SDK, MCP SDK, Agent SDK, Pydantic and httpx need as pinned wheels; whether the Agent SDK needs the Claude Code binary and Node; what a profile image would hold | office (heavy, slot) | **done:** Python wheels, npm packages and the JVM SDKs install and import offline; sizes per language and the bundled Agent SDK binary (about 235 MB per language) recorded; see `FEASIBILITY.md`. |
| C-05 | Harness design: replay through the SDK's transport hook, the scripted model, the capture tool and its id-stripping check; one proof example | office (heavy, slot) | **done:** `harness/` drives the official SDK through its `httpx2` transport hook; replay, scripted model, scrub check and an MCP stdio proof pass offline (22 tests). |
| C-06 | Coverage check: every exam-map topic has a module, every module names valid codes; a script the gate runs | office | **done:** `tools/check_coverage.py` passes on the current map and outline (51 topics, 94 modules in draft 5); planted defects fail. |
| C-07 | The other three languages: TypeScript on the base image's Node (type stripping, its test runner) or a pinned compiler; the Java API SDK and the JVM MCP SDK in a Gradle project for Java and Kotlin, offline; which SDK features each language lacks | office (heavy, slot) | **done:** TypeScript runs on the base image's Node with type stripping; Java runs on Maven; Kotlin needs Gradle, which the base image lacks (framework change listed in `FEASIBILITY.md`). |
| C-08 | Look again for a public blueprint, exam description or prep material for the Architect Professional exam; if found, redraft Level 4 from it | office (reading) | **done:** A public official Professional blueprint exists (CCAR-P, seven weighted domains); Level 4 is redrafted from it as modules 79 to 94 (draft 5 numbering) and no longer provisional. |
| C-09 | Topic-coverage audit against third-party references: list every topic the public study guides, practice-question sets and prep-course tables of contents cover, and name any the outline lacks; findings kept in the register outside the repository, outline gaps fixed in the outline | office (reading) | **done:** Public prep material for all four exams was read for topics; 19 gaps found, all closed by widening 28 Covers cells (no module added) and adding the Associate domain table to the exam map; outline is draft 4. |
| C-10 | Confirm the Associate domains (AS1 to AS7) against the official Associate exam guide, and correct the exam map | office (reading) | **done:** All seven Associate domains confirmed from official CCAO-F v1.0 guide (July 2026); domains and weights match the exam map; all Level 1 modules provide coverage. |

C-01, C-02, C-06, C-08 and C-09 are reading and may run beside each other and beside one heavy row. C-03 to
C-05 build or run containers and go through the heavy-job slot one at a time.

## Open course rows

| Row | Task | Status |
|---|---|---|
| J-1 | Java practices and examples build with Gradle (Kotlin DSL), like the Kotlin ones, so the course image grades them; convert modules 6 and 13 to 17 | **done:** the seven Java practices (modules 6 and 13 to 17, and the agent-loop exercise) build with `build.gradle.kts` and `settings.gradle.kts`, no `pom.xml` remains; the run scripts and graders read Gradle output; the examples of those modules have no Java side. Proved offline: every reference passes, every starter fails, every plant fails on its named case on an assertion. |

## Framework work

Not tracked here. The framework repositories plan it on their own boards, on release branches,
and it must not break any existing course. M2 closes when they report the support ready and this
repository's fixture unit runs on it. What the course needs is stated in `docs/IDEA.md`.

## Log

| Date | Entry |
|---|---|
| 2026-10-03 | Level 2 modules 24 to 29 authored on branch `feat/level2-24-29`: 14 pages (prompt engineering for applications, structured output, tool use, choosing an extension, retrieval, context engineering), five offline examples, four practices (modules 25, 26, 28 and 29) in Python, TypeScript, Java and Kotlin with planted wrong solutions proved offline, 42 page quiz questions and 24 module quiz questions. Gate: `sh docs/process/batches/level2-24-29-gates.sh`. |
| 2026-10-03 | J-1 done: the Java practices of modules 6 and 13 to 17 moved from Maven to Gradle (Kotlin DSL); gates `level1-1-6-gates.sh` and `level2-12-17-gates.sh` pass with Java graded through Gradle offline. |
| 2026-10-02 | Level 2 modules 18 to 23 authored on branch `feat/level2-18-23`: 13 pages (model choice and cost, thinking and effort, prompt caching, Message Batches, Claude on the cloud platforms, setting them up), six offline examples, six practices in Python, TypeScript, Java and Kotlin (Java and Kotlin on Gradle) with planted wrong solutions proved offline, 39 page quiz questions and 24 module quiz questions. Independent reader: 2 pages pass, 10 weak, 1 fail and 49 of 63 questions pass before the fix round; every finding was fixed. Gate: `sh docs/process/batches/level2-18-23-gates.sh`. Unverified: the resource ARN type for the `bedrock-mantle` action. |
| 2026-10-02 | Level 2 modules 12 to 17 authored on branch `feat/level2-12-17`: 16 pages, five offline examples, five practices in Python, TypeScript, Java and Kotlin with planted wrong solutions proved offline, quizzes for every page and module. Gate: `sh docs/process/batches/level2-12-17-gates.sh`. |
| 2026-10-02 | Level 1 modules 7 to 11 authored on branch `feat/level1-7-11`: 13 pages (apps, apps in depth, roles, safety and policy, exam readiness), 39 page quiz questions and 20 module quiz questions, a 30-question Level 1 mock exam, 131 flashcards and a 67-item spaced-review bank with their checker and planted-defect tests; the quiz checker, key balancer and quiz.json builder now cover modules 1 to 11 and a mock-exam section; gate `docs/process/batches/level1-7-11-gates.sh`. App and exam-logistics facts were read from official pages on this date and are marked with it on the pages. Awaiting the register's verification and Q-L1's independent quiz reader. |
| 2026-10-02 | Level 1 modules 1 to 6 authored on branch `feat/level1-1-6`: 13 pages, 54 quiz questions, two offline examples, and the module 6 prompt template builder in Python, TypeScript, Java and Kotlin with planted wrong solutions; gate `docs/process/batches/level1-1-6-gates.sh`. Awaiting the register's verification and an independent quiz reader. |
| 2026-10-02 | M3 done: outline draft 5 frozen with 94 modules (Level 1: 11, Level 2: 33, Level 3: 34, Level 4: 16). D13 keeps every module and restores what earlier drafts cut; exam code `X` added to the exam map and the coverage check. |
| 2026-10-02 | Survey C-09 done: topic-coverage audit closed; outline draft 4 widens 28 Covers cells, adds no module, and the exam map gains the Associate domains. |
| 2026-10-02 | Survey C-01, C-02 and C-08 done: exam map confirmed, versions pinned, Level 4 redrafted from the Professional blueprint (83 modules). |
| 2026-10-02 | M1 closed: all four exams confirmed from the official guides; practices graded offline in Python, TypeScript, Java and Kotlin; the SDKs install offline; the API stand-in and the coverage check proved. |
| 2026-10-02 | D10: the official exam guides are the topic maps, and their sample questions may be used with credit. Exam facts confirmed from the official guides; versions pinned; Level 4 drafted from the Professional blueprint. |
| 2026-10-02 | Decisions D2, D4, D6 and D7: four incremental levels, offline grading, Professional level after a fresh search, Associate covered by Level 1. |
| 2026-10-02 | Decisions D1, D3, D5, D8 and D9: name, four languages, the exam guide as topic map, the reader's key in the site and captures before release. |
| 2026-10-02 | Outline draft 2: 80 modules in four levels, widened to the Developer exam's reported topics and the official course topics. |
| 2026-10-02 | Project created: goal, idea, setup, exam map, outline draft, production steps and the board. Nothing has been built or run. |
