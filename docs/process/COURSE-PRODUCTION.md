# How the course material is made

The course does not exist yet. These are the steps from an outline to a narrated, graded site, in
order, with what each step produces and how it is checked.

## The shape of a unit

A unit is one markdown file in `course/`, the page a reader opens, in this order:

1. A title, the level, the exam codes it serves, and what the reader can do afterwards, in one
   sentence.
2. **Why it matters:** the production situation or exam scenario that needs it, in two or three
   sentences.
3. **The idea:** the model, in plain words, with the smallest example that shows it.
4. **The example:** a real project under `examples/`, linked, with its tests; the output shown is
   the output the container produced; a recorded exchange names its model, SDK version and date.
5. **Traps:** two or three ways it goes wrong, each with a short demonstration (an exchange where
   the prompt-only fix fails and the code fix holds is the typical one).
6. **Practice:** one or two graded practices, or an exam-style quiz.

Code is shown exactly as typed (no font ligatures).

## Exchanges: recorded and scripted

- A **recorded** exchange is captured once against the real API by an approved capture run,
  stripped of identifying ids and the key, reviewed and committed beside its example. Examples
  replay it.
- A **scripted** exchange is written by hand for a practice: the model's turns are fixed so the
  test drives the reader's code through one exact case. A scripted turn must be a response the
  real API could return (same shapes, same `stop_reason` values); the harness validates it against
  the SDK's types.
- When the pinned model changes, the release pass re-captures recorded exchanges and diffs the
  pages that quote them.

## The steps

| Step | What happens | Produces | Checked by |
|---|---|---|---|
| **P0 Scope** | The owner cuts, adds and reorders the outline; levels, language and exam coverage chosen | A frozen `COURSE-OUTLINE.md` | The owner's yes |
| **P1 Sources** | Sources collected and licences checked; exam facts read on official pages | A source register outside the repository; `EXAM-MAP.md` marked confirmed | Every claim traces to an official page, a run or a capture |
| **P2 Objectives** | For each module, what the reader can do afterwards, as a testable sentence | A line per module in the outline | A module with no testable objective is rewritten or cut |
| **P3 Survey** | Prove what runs: a Python practice end to end offline, the SDKs in a profile, the harness, an MCP server over stdio, a hook script on sample input | A feasibility table | The register runs each claim |
| **P4 Harness** | Build `harness/`: replay, script, optional live, the capture tool and its id-stripping check | `harness/` with tests | A planted key or id in a capture fails the check |
| **P5 Prose** | Study every source for the module first (official docs and courses, the exam guides, the third-party guides and question sets in the register), then draft the units in the course's own words, one level at a time | `course/*.md` | Technical review against the pinned versions; plain-language read |
| **P6 Examples** | Write the examples as projects with tests; capture their exchanges | `examples/` | Every example's tests pass offline; a planted wrong expected output fails |
| **P7 Practices and quizzes** | Practices by coverage of every aspect (plants written once, see below); exam-style quizzes; a mock exam per level | `exercises/`, `tools/make_plants.py`, `tools/make_cases.py` | Reference passes, planted wrong solutions fail on assertions, starter fails; an independent quiz reader |
| **P8 Build** | `corpus.json`, `validate`, build the site, serve it, crawl it | A running site | Zero console errors; practices run end to end; the editor opens |
| **P9 Narration** | Once the prose is frozen, narrate the lesson prose (never practices or code) | Clips in release volumes | A listened sample from each level |
| **P10 Release** | Re-check exam facts and model ids; images, learner `main`, README | A repository and images the owner pushes | The cold-pull check on a clean machine |

## Planted wrong solutions are written once

A practice commits its `reference/`, `starter/`, `tests/` and `cases.json`, and nothing else per plant. Each
planted wrong solution is an ordered list of exact replacements against the reference, kept in a section of
the single plant tool `tools/make_plants.py` (practice, language, main file, then `plant name: [(old, new), ...]`).
`python3 tools/make_plants.py --modules REGEX` copies the reference into `<language>/<plant name>/` and applies
the replacements; it stops when a pattern is missing or occurs more than once, when a plant equals the reference,
or when a language's plants differ from the ones `cases.json` names. The `wrong-*/` folders are git-ignored:
every batch gate and `tools/l2_run_all.sh` / `tools/run_all_practices.sh` generate them first, so a fix to a
reference reaches every plant at the next run. `tools/make_cases.py --modules REGEX` writes `cases.json` for
modules 30 and later from the same kind of specification. Both tools take the module as an argument; a new batch
adds its sections, not a new script.

## Quality check against the exams

For every module, once drafted:

- **Against the exam map:** does it teach every topic its exam codes name, to the depth a
  scenario question needs?
- **Against the official material:** is anything the official courses and documentation teach for
  this topic missing? Findings are kept with the source register, outside the repository.
- **Against third-party references:** do the public study guides, practice-question sets and
  prep-course tables of contents name a topic this module or level lacks? They are a coverage
  check only; findings stay in the register outside the repository.
- **Against fresh-writing:** no sentence, example or question is a copy or close paraphrase of a
  source that does not permit it.

## Roles

- **The owner:** decides scope, approves capture runs and supplies the key for them from the
  environment, reviews, pushes.
- **The register:** keeps the board, verifies every batch by running it, merges.
- **Authoring offices:** one at a time for prose and practices; quiz readers separate. Research
  offices may run beside them; build offices never run beside another heavy job.

## What a batch is

A batch is a level, or half a level, carried from prose to examples to practices, and closed only
when its examples run offline, its practices are proved and its quizzes passed the reader.
