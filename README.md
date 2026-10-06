# Claude Certification Preparation: Developer to Architect

A self-study course that takes an engineer from their first call to Claude up to the level the
Claude certification exams test, in one path of four levels. Each level builds on the one before,
so nothing you study for one exam is repeated for the next.

| Level | Modules | What you can do at the end |
|---|---|---|
| 1 Foundations | 1 to 11 | Explain how Claude works, prompt it well and use it safely |
| 2 Developer | 12 to 44 | Build with the API, tools, MCP, agents and Claude Code; secure and evaluate what you build |
| 3 Architect | 45 to 78 | Design agentic systems: orchestration, tool and MCP design, Claude Code configuration, structured output, context and reliability |
| 4 Architect Professional | 79 to 94 | Design, evaluate and govern production Claude solutions end to end |

Every page has a quiz in the exams' scenario style, every code module has a graded practice, and
each level ends with mock exams, a flashcard set and a review bank. Examples and practices come in
Python and TypeScript, and in Java and Kotlin wherever the topic does not need the Agent SDK.

## Not official

This course is not made, endorsed or reviewed by Anthropic. It does not reproduce any exam and it
does not promise a pass. Exam rules, fees and blueprints are Anthropic's and they change: read the
official exam pages before you book.

## What you need

- Docker Desktop (Windows, macOS or Linux).
- A browser.
- No API key and no network once the images are pulled.

## Starting the course

The course runs as a local site served from container images, started with one
`docker compose up`. The site images and the compose file are not published in this repository
yet; this section will give the exact commands when they are. Until then you can read every page
in [`course/`](course/) on GitHub.

## How a practice works

Each practice opens in the site with a starter file in your language.

- **Run** executes your try-it file and shows what your code printed and logged. Nothing is graded.
- **Submit** grades your solution against the practice's tests: the main task and its edge cases.

Every graded practice runs offline, against a stand-in for the Claude API that plays recorded or
scripted exchanges. The result is the same on every run and costs nothing.

## Your API key is optional

You never need a key to study, run the examples or pass a practice. If you want to see an example
against the real API, you can give your own key in the site; it is kept in your browser and passed
only to that live run. Live runs are never graded.

## Where help lives

- [`course/README.md`](course/README.md): the list of modules and pages.
- [`docs/COURSE-OUTLINE.md`](docs/COURSE-OUTLINE.md): what each module covers.
- [`docs/EXAM-MAP.md`](docs/EXAM-MAP.md): which module serves which exam topic.
- [`docs/VERSIONS.md`](docs/VERSIONS.md): the model ids and SDK versions the pages were checked on.
- The repository's issue tracker: report a wrong fact or a broken practice.
- Product questions: Anthropic's documentation and support pages.
