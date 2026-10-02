# Claude Certification Preparation: Developer to Architect

A self-study course that takes an engineer from their first call to Claude up to the level the
**Claude Certified Developer** and **Claude Certified Architect** exams test, in one incremental
path. The levels share their foundations: each level builds on the one before, so the
material you study for one exam is not repeated for the next.

It runs entirely in containers. You need Docker and a browser. No API key is needed to study,
run the examples or pass the practices: every graded practice runs offline against a recorded,
deterministic stand-in for the API. If you set your own key in the environment, the examples
can also be run live.

> **Status: in development.** The outline is a draft and no lesson is written yet.
> See [`docs/GOAL.md`](docs/GOAL.md) for what the course is for,
> [`docs/COURSE-OUTLINE.md`](docs/COURSE-OUTLINE.md) for what it will cover,
> [`docs/EXAM-MAP.md`](docs/EXAM-MAP.md) for how it maps to the exams, and
> [`docs/SETUP.md`](docs/SETUP.md) for how it is being built.

## What it is

- **Four levels in one path:** Foundations (how Claude works, prompting, safe use), Developer
  (the API, tools, MCP, agents, Claude Code, security, evaluation), Architect (agentic
  architecture, tool and MCP design, Claude Code configuration, structured output, context and
  reliability, in the exam's scenarios), and Architect Professional (enterprise scale).
- **Practices graded by a real runner:** you write the agent loop, the tool schema, the MCP
  server, the hook, the `CLAUDE.md` and the validation-and-retry loop, and tests grade them.
- **Exam-style quizzes:** scenario questions with one best answer and three plausible ones,
  explained, and a mock exam at the end of each level.

## What it is not

It is not affiliated with or endorsed by Anthropic, it does not reproduce any exam or
official course, and it does not promise a pass. Exam rules, eligibility and blueprints are
Anthropic's and change; check the official exam pages before you book.

## How you will run it

Like the other courses made with studyforge: clone this repository, set the Docker Hub account
the images are published under, and start it with one compose command. The exact steps will be
written here when the first build exists.
