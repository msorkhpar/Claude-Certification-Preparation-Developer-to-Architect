# The idea

## One sentence

A studyforge course that prepares engineers for the Claude Developer and Architect
certifications in one incremental path, where every concept an exam asks about is something the
reader has built and seen graded, offline, in a container.

## Why this is different from the earlier courses

The earlier courses taught a language, and their practices compile and run code with no outside
service. **This course's subject is a remote model.** A practice cannot call the API: graded runs
are offline, a call costs money and the answer changes between runs. So the course separates
two things the exam questions are about:

1. **The system around the model**: the agent loop, the tool schemas and their error replies,
   the MCP server, the validation-and-retry loop, the hook that blocks a refund over the limit,
   the `CLAUDE.md` and the skill. These are ordinary code and configuration, and **they are what
   the practices grade**.
2. **The model's behaviour**: what a prompt, an example or a tool description changes. This is
   shown with **recorded exchanges**: real API exchanges captured once against a pinned model,
   stored beside the example, and replayed by a stand-in client. The page says which model and
   date it was captured on, and a reader with their own key can run it live.

## The stand-in for the API (the course's own test harness)

A small library in this repository, used by examples and practices alike:

- **Replay:** plays back a recorded exchange through the official SDK's own transport hook, so
  the code under test is the code a reader would ship, not a toy.
- **Script:** a scripted model for practices: "first ask for `get_customer`, then answer"; "return
  malformed JSON once, then valid JSON"; "call a tool that fails with a retryable error". The
  tests drive the reader's loop through exactly the case the lesson is about.
- **Live (optional, never graded):** the same example against the real API, when the reader sets
  their own key in the environment. Off by default.

The harness is course material, not a framework feature: it lives in `harness/` here, with its own
tests.

## Four levels, one path

| Level | Who the reader is at the end | Exam it prepares |
|---|---|---|
| **1 Foundations** | Knows how Claude works, what it is good and bad at, how to prompt it and use it safely | The shared base of all exams; most of the Associate exam |
| **2 Developer** | Builds applications with the API, tools, MCP, agents and Claude Code, secures and evaluates them | Claude Certified Developer, Foundations |
| **3 Architect** | Designs agentic systems: orchestration, tool and MCP design, Claude Code configuration, structured output, context and reliability, in the exam's six scenarios | Claude Certified Architect, Foundations |
| **4 Architect Professional** | Designs, evaluates and governs production Claude solutions end to end: architecture patterns, integration and retrieval, evaluation, observability, risk and compliance, stakeholder communication | Claude Certified Architect, Professional |

Each level builds on the one before and **does not repeat it**: a topic is taught once, at the
level that first needs it, and a later level deepens it in its own module that links back. A
reader may start at a higher level; a placement quiz at the top of each level says whether they
should. Each level ends with a mock exam in the exam's own question style, with a flashcard set and a spaced-review question bank for revision. The course aims to be as comprehensive as possible: modules that no blueprint tests are kept and marked `X` in the exam map.

## Exam-style questions

The exams ask scenario questions with one best answer and three plausible distractors. The
course's quizzes use the same form, written fresh: a short production situation, four options
parallel in form, and a folded explanation of why the key is best and each other option is not.
The recurring lessons (a guarantee belongs in code, not in a prompt; the simplest fix at the root
cause beats extra machinery) are taught in the lessons first, so a quiz never tests something the
page did not teach.

## Four languages, one tab block

Every example and practice exists in **Python and TypeScript**, the two languages that have every
SDK the course uses (the API SDK, the MCP SDK and the Agent SDK). Wherever a topic does not need
the Agent SDK, it also exists in **Java and Kotlin**, through the Java API SDK and the MCP SDK for
the JVM. An example is one block with a tab per language, the same tab feature the Kotlin course
uses; the reader picks a language on the first visit, a remembered switch on every page changes it,
and a language a topic or practice does not exist in is greyed out with a line naming the languages
that carry it. Claude Code and MCP configuration files (settings, hooks, skills, the memory file,
the project MCP file) are language-neutral and shown once; a hook script is shown in each language
that has the topic.

## What runs, and where

| Thing | In a container |
|---|---|
| The SDK request and response cycle, tool use, structured output, batches, caching headers | Runs, against recorded exchanges |
| Agent loops, subagent coordinators, hooks, validation and retry | Runs, against a scripted model |
| MCP servers and clients (tools, resources, prompts) | Runs, both ends in the container, over stdio |
| Claude Code configuration: settings, permissions, hooks, skills, commands, rules, `.mcp.json` | Files validated by tests; hook scripts run on sample event input |
| Claude Code itself, the Agent SDK against the real model, the apps | Live only, with the reader's own key; reading and recorded output otherwise |
| Evaluation suites (code-graded, model-graded with recorded grades) | Runs |

## What the course needs from the framework

Planned and tracked on the framework repositories' own boards, backward compatible with every
existing course. This repository only states the need:

1. **SDKs in a profile, for four languages:** the toolchain's profile mechanism gains pinned
   Python-wheel and npm-package entry kinds (its Maven and Gradle kinds already exist), so a
   profile image carries the Anthropic, MCP and Agent SDKs for Python and TypeScript and the
   Anthropic and MCP SDKs for Java and Kotlin, offline, without moving any shared base tag.
   TypeScript runs on the base image's Node if its built-in type stripping suffices (survey C-07).
2. **A quiz-heavy unit:** a mock-exam page of many scenario questions that covers a whole level,
   graded in the page, within the quiz gates.
3. **Optional live runs:** the reader types their own key in the site; it is kept in the browser,
   passed to a live run as an environment variable, never stored on disk and never given to the
   graded runner (board D9).
4. **Four-language tabs and reading languages:** the example tab block and the reading-mode choice
   generalised from the Kotlin course to any list of languages, with greyed-out languages.

## How the repository is laid out (draft)

```
course/            the lessons, one markdown file per unit, and corpus.json
examples/          one project per example, with tests and its recorded exchanges
exercises/         the authored practices: statement, starter, tests, reference solution
harness/           the stand-in for the API: replay, script, optional live
docs/              what the course is and how it is built
docs/process/      the board and the working notes (moves to a branch before any publish)
```

## Principles

- Build it, then ask about it: every quiz question is about something the reader built.
- A guarantee belongs in code; a prompt is a request. The course teaches both and says which is
  which.
- Show it running; a recorded exchange names its model and date.
- One small real example beats a long explanation.
- The course never copies a source, an exam or an official course: it teaches, cites and links.
