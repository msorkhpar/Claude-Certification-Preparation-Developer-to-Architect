# Rollout brief: scaffolds, a visible logger and single-edge plants, per practice

Applies to every practice a rollout batch touches, in all four languages (Python and TypeScript only where the Agent SDK is needed).
The pilot practices 41, 52 and 62 are the model: read their starters, `statement.md` ("What is already written, and what you write")
and their `make_plants.py` sections first. Read `tools/harness/README.md` for how a logged line reaches the report.

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
5. **Tools.** Sections in `tools/make_cases.py` and `tools/make_plants.py` are edited **only inside your own module range, in module order**; never add a
   generator. `cases.json` is written by `make_cases.py`, `wrong-*/` by `make_plants.py`, and the harness files by `make_harness.py`; none of the generated files is committed.
6. **Checks before the proof.** `python3 tools/check_logger.py --modules REGEX` (0 files without the declaration) and `python3 tools/practice_effort.py --modules REGEX`.

## Proof commands (heavy slot, desktop-linux, at most 2 test workers)

```
export STUDYFORGE_NAMESPACE=studyforge-local L2_MODULES='^(NN|MM)-' L2_EXAMPLES='^(NN|MM)-'
IMG=93d052f3fc87
python3 tools/make_cases.py --modules "$L2_MODULES" && python3 tools/make_plants.py --modules "$L2_MODULES" && python3 tools/make_harness.py --modules "$L2_MODULES"
../.heavy-slot/run-heavy.sh ccp-survey tools/l2_run_all.sh $IMG > .survey-out/gate-run.txt 2>&1   # runs make_plants and make_harness itself; prepares the caches
python3 tools/grade_practices.py                      # must print 0 findings
```

A fresh worktree has no caches: `tools/l2_prepare_caches.sh`, `l2_prepare_gradle.sh` and `l2_prepare_jvm_examples.sh` fill them (or copy `.survey-out/{py,npm,gradle}` from a
worktree that has them). A practice with no logger line still runs exactly as before, so a module may be proved before its logger is added.

## Hand-back

Branch and commits; per module: gaps and lines to write per language (`practice_effort`), plants per edge case with their `caught_by`, `check_logger` output, the
`grade_practices` summary line, and one report XML excerpt showing a logged `[log...]` line under its testcase; anything unverified; the exact proof command run.
