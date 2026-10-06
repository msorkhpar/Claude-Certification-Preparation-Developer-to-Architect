# Setup: how this project is built

## The pieces

| Piece | What it is | Where |
|---|---|---|
| This repository | The course: lessons, examples, practices, the API stand-in, and later the built site | `claude-certification-preparation` |
| studyforge | The framework that turns the lessons into a site and a graded practice runner | its own repository |
| studyforge-code-toolchain | The runner and editor base images; already carries Python with pytest and Node | its own repository |
| studyforge-narrate-service | The narration synthesis service | its own repository |

The three framework repositories have their own boards and release branches. What they must
change to support this course is planned and tracked there, never in this repository. This
project consumes the result: the images by digest, and the framework by version. The framework
must keep working for every existing course.

## How the work is run

- **The register** (the coordinating session) plans, delegates, verifies and merges. It does not
  write lessons itself.
- **Offices** (background agents) do the heavy work, each on its own branch in its own worktree
  beside the project: a survey, a lesson batch, an example batch, a practice batch.
- **At most two heavy jobs at a time** through the heavy-job slots; file work runs in parallel. Test
  runs use at most 4 workers.
- **The register verifies every hand-back:** read the commit, plant a bug of its own, run the
  gate, then merge.
- **Nothing is pushed by the register.** The owner reviews, then pushes.

## Containers

- Docker Desktop, context `desktop-linux`, passed explicitly on every command. Never
  `docker context use`.
- No host `/tmp` mounts. The course must work on Windows.
- Pulls of pinned images and packages (by digest or checksum) are allowed. Nothing is pushed or
  logged into by the register.

## API keys and recorded exchanges

- **No key is needed** to build, test or study the course. Graded runs are offline.
- Recorded exchanges are captured by the register, once per example, with a key the owner
  approves for that purpose, supplied **from the environment at run time** (`ANTHROPIC_API_KEY`),
  never written to a file. A capture is reviewed before commit: it holds the request and the
  response only, with no key, account id, organisation id or request id that identifies an
  account (the capture tool strips them, and a test checks it).
- A reader's live run uses the reader's own key from their environment.

## Branches

- `main`: the course and, once built, only what a learner needs.
- `process`: the board, the working notes and anything about how the course was made. Until the
  first publish they live in `docs/process/` on `main`; before the first publish that directory
  moves to a `process` branch, and nothing is deleted.

## Sources and material

The register keeps the list of sources, their licences and how each may be used **outside this
repository**; it is never committed. A source whose licence permits copying is credited on the
page where it is used. Exam guides and official courses are read to map topics; their wording,
questions and examples are never copied.

## Personal data

No file in this repository contains the owner's name, email, accounts, machine name or home
path. See `CLAUDE.md`.

## Definition of done for a milestone

A milestone closes when its rows are closed, the register has verified them by the effect (run
the thing, not just read the report), and the board says so.
