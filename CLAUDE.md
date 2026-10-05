# CLAUDE.md: how to work in this repository

This repository is a course: **Claude Certification Preparation: Developer to Architect**, one
incremental path in four levels (Foundations, Developer, Architect, Architect Professional),
taught in a container, where every graded practice runs offline against a stand-in for the API.
Read `docs/GOAL.md`, `docs/IDEA.md`, `docs/SETUP.md` and `docs/EXAM-MAP.md` first, then
`docs/process/BOARD.md` for what is open.

## The scope guard

- **Building with Claude**: the API, tools, MCP, agents, the Agent SDK, Claude Code and their
  design, security and evaluation, to the depth the Developer and Architect exams ask. The apps
  appear at Level 1 only.
- **Not a copy of an exam or an official course.** Exam blueprints are a map of topics, written
  in the course's own words. Quiz questions are written fresh, except the official exam guides'
  sample questions, which may be used with credit (board D10).
- **Not official.** No page claims endorsement or a guaranteed pass, and no page uses Anthropic's
  logos or exam branding as its own.
- **Everything graded runs offline** in the container, against recorded or scripted exchanges.
  Live API runs are optional, use the reader's own key from the environment, and are never graded.
- A claim about Claude names the model id and SDK version it was checked on. Do not state a
  behaviour that has not been run, or replayed from a real capture, or read on an official page.

## Rules that never bend

1. **No personal data in any file.** No name, email, account, organisation id, machine name or
   home path. Use placeholders (`<owner>`, `contact@example.com`, `/path/to/project`). If a task
   cannot proceed without a real value, stop and ask. A real value, once approved, comes from the
   environment at runtime, never a literal in source.
2. **No API key in any file, log or capture.** A key comes from `ANTHROPIC_API_KEY` at run time.
   A recorded exchange is stripped of keys, account, organisation and request ids before it is
   written, and a test checks every capture for them.
3. **No conversation in files.** Never quote the owner or record who said what. Write decisions
   as decisions, with their reason, in current-product wording.
4. **Nothing is pushed.** No `git push`, no `gh`, no `docker login` or `docker push`. The owner
   reviews, then pushes. Hand them the exact commands.
5. **Containers run on Docker Desktop**, context `desktop-linux`, passed on every command. Never
   `docker context use`. No host `/tmp` mounts. It must work on Windows.
6. **Pinned pulls are allowed** (by digest or checksum). Nothing else goes off the host, except a
   capture run the owner has approved, which calls the API with the owner's key from the
   environment.
7. **Memory: at most two heavy jobs at a time; file work runs in parallel.** A heavy job is any Docker
   build, run or exec, any test suite and any API capture run. It goes through the
   two heavy-job slots (`run-heavy.sh <kind> <command>` in the `.heavy-slot` folder beside the project). Two jobs may run side by side when free host and Docker VM memory cover both their ledger needs plus a margin; an image build (a ledger need of 2.5 GB or more) takes both slots and runs alone.
   Test runs use at most 4 workers (never `-n auto`). Remove scratch images and containers as soon
   as a proof is done. The host has 24 GB and Docker's VM 12 GB.
8. **Fixes land in the framework, not the course.** A defect that the course exposes in
   studyforge, the toolchain or narrate-service becomes a change there, planned on the
   framework's own board, backward compatible with every existing course. The course is then
   regenerated. Never hand-edit generated output. The API stand-in in `harness/` is course
   material and is fixed here.
9. **Verify by the effect, not the proxy.** Run the thing. A report that says "done" is a claim
   to check: read the commit, plant a bug of your own, run the gate.
10. **The licence decides how a source is used.** Any public source may be read to understand a
    topic. A source whose licence permits copying may be adapted and is credited on the page where
    it is used. The official exam guides are the topic maps, and their sample questions may be
    used in quizzes and mock exams, credited on the page (board D10). Any other source with a
    restricted, unclear or no licence, and every official course, is studied in full to learn
    what and how to teach, then never copied: the course writes its own understanding in its own
    wording, examples and questions.
11. **Sources are not named in this repository.** The register of sources, their licences and
    any audit are kept outside the repository and never committed. Nothing in a commit, a branch
    or a merge names a source, except the credit line on a page for a source whose licence permits
    copying. Anthropic's own product, documentation and exam names are the subject, not a source,
    and may be named.

## How work is delegated

- The coordinating session (the register) plans, delegates, verifies and merges. It does not
  write lessons.
- Heavy work goes to background offices on their own branch and worktree, in a directory beside
  the project under the same parent folder (not `/tmp`).
- **Authoring batches run in parallel**, each on its own branch and worktree, over disjoint module ranges; heavy runs still queue through the heavy-job slots. A batch never edits the board (the register does), inserts its sections into the shared tools in module order, and lists new JVM libraries for the register instead of regenerating the checksum file. A batch whose questions must draw on a whole level (the exam-readiness modules with mock exams) runs after the rest of that level has merged. Quiz readers stay separate.
- **Models:** the register runs on Opus 5.5 and picks the office's model by the task's weight:
  Sonnet for ordinary work (authoring, examples, framework changes, surveys), Haiku for very light
  tasks (a scan, a rename, a board edit). A task that turns out harder than its model is
  re-assigned, not forced.
- Every hand-back is verified by the register before merging: read the commit, one plant, the
  gate, then merge.
- Mint only what blocks. Report milestones, not rows.

## What a unit, an example and a practice must satisfy

- **Unit:** one markdown file in `course/`; the level; the exam codes it serves (from
  `docs/EXAM-MAP.md`); the idea; a real example; two or three traps; a practice or a quiz. See
  `docs/process/COURSE-PRODUCTION.md`.
- **Example:** a project under `examples/` with tests that run offline against the harness; the
  output shown on the page is the output the container produced; a recorded exchange names its
  model id, SDK version and capture date.
- **Practice:** graded on the main ask and on edge cases by a real runner; the starter fails, the
  reference passes, and every planted wrong solution fails **on an assertion** (read the failure
  output, not only the exit code). Dependencies are vendored; no network.
- **Quiz:** exam style: a short scenario, one best answer and three plausible options parallel in
  form, so the key is not the odd one out. The key never repeats a word of the stem; every wrong
  option is ruled out by a passage or an output on the page; the quiz is answerable from the page
  it closes, or says it covers the whole module or level (a mock exam); the folded explanation
  says why the key is best and why each other option is not, and matches `quiz.json`. An
  independent reader judges every quiz before it merges.

## Branches and files

- `main` is the course and, once built, only what a learner needs.
- Process material (`docs/process/`, `CLAUDE.md`) moves to a `process` branch before any
  publish. Nothing is purged.
- Keep documents about the **current** state. No fix history, no round or task ids in anything a
  learner reads.

## When in doubt

Ask the owner about scope, depth, exam coverage, keys and sources. Decide implementation details
yourself and say what you decided.
