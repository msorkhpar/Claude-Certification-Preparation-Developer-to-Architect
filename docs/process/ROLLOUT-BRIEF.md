# Rollout brief: scaffolds, a visible logger and single-edge plants, per practice

Applies to every practice a rollout batch touches, in all four languages (Python and TypeScript only where the Agent SDK is needed).
The pilot practices 41, 52 and 62 are the model: read their starters, `statement.md` ("What is already written, and what you write")
and their files in `tools/plants.d/` and `tools/cases.d/` first. Read `tools/harness/README.md` for how a logged line reaches the report.

## Per-practice checklist

1. **Scaffold.** The starter is the working reference with 6 to 10 small gaps cut out. Each gap is one function with its signature,
   a comment that says what it receives and returns, **one example**, and the **cases it unlocks**. A gap returns a neutral typed value
   (empty list, `None`/`null`, `false`, the unchanged input) so the starter runs and fails on an assertion, never on a compile or import
   error. Plumbing is written and correct. `python3 tools/practice_effort.py --modules REGEX` must show **at most 25 lines to write in every
   language** (hard limit 30).
2. **Logger.** One visible line at the top of every starter, reference and lesson-example main file, the same in all three variants:
   Python `log = logging.getLogger(__name__)` (with `import logging`); Java `private static final System.Logger LOG = System.getLogger(<Class>.class.getName());`
   as the first line in the class; Kotlin `private val log = System.getLogger("<name>")` at file top; TypeScript `const log = logger("<name>")` with
   `import { logger } from "../logger.ts";` in practices and `import { logger } from "./logger.ts";` in lesson examples (the helper is generated, see below).
   Add **one `log.debug` of the input** as the first statement of one function the learner calls, identical in starter and reference (Java/Kotlin
   `LOG.log(System.Logger.Level.DEBUG, "<fn> input: {0}", x)`; TypeScript `log.debug("<fn> input", x)`). It never prints at default levels in any language (the TypeScript helper is silent outside a test), so a lesson example's printed output is unchanged; check that every
   lesson example's output is unchanged.
3. **Statement.** In `statement.md` add one sentence that points to the logger: debug a gap by logging its input with the `log` line at the top of the file;
   a run shows the lines under the failing case. List the gaps in "What is already written" with the cases each unlocks.
4. **Plants: single-edge.** For **every edge case** (`e*`) at least one plant that passes every main case (`m*`) and every other edge case and fails **only that
   edge**; `caught_by` in `cases.json` names exactly that case. Each plant is a minimal ordered replacement against the reference (each `old` occurs exactly once);
   never copy a reference file.
5. **Tools.** Plants and case lists live one file per module: `tools/plants.d/<module folder>.py` and `tools/cases.d/<module folder>.py`. Edit
   **only the files of your own modules**; never edit `make_plants.py`, `make_cases.py` or `modular_data.py`, and never add a generator. `cases.json` is written by
   `make_cases.py`, `wrong-*/` by `make_plants.py`, and the harness files by `make_harness.py`; none of the generated files is committed.
6. **Checks before the proof.** `python3 tools/check_logger.py --modules REGEX` (0 files without the declaration) and `python3 tools/practice_effort.py --modules REGEX`.

## Proof (one gate, heavy slot inside it, desktop-linux, at most 2 test workers)

```
export STUDYFORGE_NAMESPACE=studyforge-local
tools/gate.sh --modules '^(NN|MM)-'          # static checks, then the container jobs through ../.heavy-slot/run-heavy.sh; writes .survey-out/gate-report.json
python3 tools/validate_handback.py            # must print ACCEPTED with executed == expected in all four languages
```

Commit first, then run the gate on the committed tree; a report older than the range's last commit is refused. Do not run the old batch scripts.
A fresh worktree has no caches: `tools/l2_prepare_caches.sh`, `l2_prepare_gradle.sh` and `l2_prepare_jvm_examples.sh` fill them (or copy `.survey-out/{py,npm,gradle}` from a
worktree that has them). A practice with no logger line still runs exactly as before, so a module may be proved before its logger is added.

## Hand-back

Branch and commits; per module: gaps and lines to write per language (`practice_effort`), plants per edge case with their `caught_by`, `check_logger` output, the
`grade_practices` summary line, and one report XML excerpt showing a logged `[log...]` line under its testcase; anything unverified; the exact proof command run.
