# Practice: subagents that recover locally, fail with context and leave the coordinator able to decide

A research system sends one subagent to each source. When a source times out, the subagent returns "search unavailable" and the coordinator learns nothing; when a source has nothing to say, the
subagent returns an error, so the coordinator retries a search that worked; when one source is closed to the system, the whole run stops; and the final report claims to cover every topic
although two of them were never read. In this practice you write the three decisions that fix this: what a subagent does about a failure (retry what is transient, report the rest with its context),
what the coordinator does with each result, and what the report says about coverage. The tool is not called: the tests give you scripted replies. It is in Python, TypeScript, Java and Kotlin;
pick your language folder, open `starter/` and edit the file there.

Names are Python's (`search_with_recovery`, `coordinator_plan`, `coverage_note`); TypeScript has the camel-case names; Java has the same camel-case names as static methods of `ErrorFlow` with
the records the starter defines (`Reply`, `Outcome`, `Step`); Kotlin has top-level functions and data classes with default values (its `search_with_recovery` takes the call function as the last argument).

## What is already written, and what you write

The starter is a working error flow with six gaps cut out of it. Everything that is plumbing is written and correct: the loop of attempts, the tables of alternatives and transient failures, the success outcome, the first rows of the coordinator's plan and the first groups of the coverage note. Each gap is marked `TODO k of N` with a comment that says what it receives and returns, with one example, and the cases it unlocks. A gap leaves a neutral value (nothing added, an empty list, `null`, the unchanged input), so the starter runs and fails the cases on an assertion. To debug a gap, log its input with the `log` line at the top of the file: a run shows the logged lines under the failing case. Write the gaps in this order (the TypeScript, Java and Kotlin names are the camel-case forms where a name is given):

1. The local retry (unlocks `m1`): a transient failure (`timeout`, `unavailable`) is retried locally while the attempts are below the limit, and the success that follows is reported with its number of attempts.
2. The valid empty result (unlocks `e1`): an `ok` reply with no items is the status `empty`, a success with no findings, never an error; with items it is `success`.
3. The failure context (unlocks `e2`, `e3`): a failure carries what was attempted, the number of attempts, the partial results of the last attempt and the alternatives for its type, so the coordinator can act on it; a permission or invalid query error is not retried.
4. The coordinator's actions (unlocks `e4`): a failed topic with partial results is used as it is (`use_partial`), one without them but with alternatives is retried another way (`try_alternative`), and one with neither is flagged as a gap (`flag_gap`); the run never stops.
5. The partial and gap groups (unlocks `e5`): a failed topic with partial results is listed under `Partial` with its failure type, and one without them under `Gaps` with the failure type and the query that was attempted, so the note names the cause.
6. The topic with no result (unlocks `e6`): a topic that has no result at all is a gap that was not searched: `topic (not searched)` under `Gaps`.

`m1` needs gap 1. About eight lines in all. The steps below describe the whole flow, so you can see how your gaps are used.

## What to write

A search tool is a function `call(query, attempt)` (the attempt number starts at 1) that returns either `{status: "ok", items: [...]}` or `{status: "error", type, partial: [...]}`, where `type` is
one of `timeout`, `unavailable`, `permission` and `invalid_query` (or anything else) and `partial` holds the items found before the error.

- `search_with_recovery(query, call, max_attempts=2)` returns an outcome:
  - an `ok` reply with items gives `{status: "success", items, attempts}`; an `ok` reply with no items is a valid empty result, `{status: "empty", items: [], attempts}`, and is never an error;
  - an error of type `timeout` or `unavailable` is transient: the subagent calls again, up to `max_attempts` calls in all;
  - any other error, or a transient one on the last attempt, ends in `{status: "failed", failure_type, attempted, attempts, partial_results, alternatives}`: `attempted` is the query,
    `partial_results` is the `partial` of the last reply, and `alternatives` comes from the `ALTERNATIVES` table in the starter (an empty list for a type the table does not know).
- `coordinator_plan(results)` takes a map from topic to outcome and returns `(topic, action)` pairs in the map's order: `use` for a success, `no_findings` for an empty result, and for a failure
  `use_partial` when it has partial results, otherwise `try_alternative` when it has alternatives, otherwise `flag_gap`. It never stops the run for a failure.
- `coverage_note(results, topics)` returns lines for the groups that are not empty, in this order: `Well-supported: ...` (successes), `Partial: ...` (failures with partial results, each as
  `<topic> (<failure_type>)`), `No findings: ...` (empty results) and `Gaps: ...` (other failures, each as `<topic> (<failure_type>: <attempted>)`, and a topic with no result at all as
  `<topic> (not searched)`). Topics are listed in the order of `topics`, and the lines are joined by a newline; with nothing to say the result is an empty string.

## Why each part is there, and what you should see

1. **Recover locally.** A timeout is the subagent's to retry, so that the coordinator hears only what the subagent could not fix. *You should see* a success after one retry, reported with two attempts.
2. **An empty result is an answer.** A search that finds nothing has succeeded. *You should see* `empty` and no retry.
3. **Errors carry context.** "Failed" is not enough for a coordinator to choose a recovery. *You should see* the failure type, the query, the attempts, the partial results and the alternatives.
4. **Do not retry what cannot change.** A closed door stays closed. *You should see* one attempt for a permission error.
5. **Never stop the run, never hide the gap.** The coordinator uses what it has and the report says what is missing and why. *You should see* a plan for every topic and a note with a gap named.

## The cases

| Id | What it checks |
|---|---|
| `m1` | A transient failure is retried locally and the success is reported with its attempts |
| `e1` | A valid empty result is a success with no findings and never an error |
| `e2` | A permission or invalid query error is not retried and carries what was attempted and its alternatives |
| `e3` | A failure that survives the retries carries the partial results of the last attempt |
| `e4` | The coordinator uses partial results, tries an alternative or flags a gap, and never stops the run |
| `e5` | The coverage note separates supported topics from gaps and names the cause |
| `e6` | A topic with no result is a gap that was not searched |

Run the tests with the command in the language folder's `run.sh` (Python, TypeScript) or its build file (Java, Kotlin).
