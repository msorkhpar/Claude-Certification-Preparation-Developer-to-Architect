# Practice: subagents that recover locally, fail with context and leave the coordinator able to decide

A research system sends one subagent to each source. When a source times out, the subagent returns "search unavailable" and the coordinator learns nothing; when a source has nothing to say, the
subagent returns an error, so the coordinator retries a search that worked; when one source is closed to the system, the whole run stops; and the final report claims to cover every topic
although two of them were never read. In this practice you write the three decisions that fix this: what a subagent does about a failure (retry what is transient, report the rest with its context),
what the coordinator does with each result, and what the report says about coverage. The tool is not called: the tests give you scripted replies. It is in Python, TypeScript, Java and Kotlin;
pick your language folder, open `starter/` and edit the file there.

Names are Python's (`search_with_recovery`, `coordinator_plan`, `coverage_note`); TypeScript has the camel-case names; Java has the same camel-case names as static methods of `ErrorFlow` with
the records the starter defines (`Reply`, `Outcome`, `Step`); Kotlin has top-level functions and data classes with default values (its `search_with_recovery` takes the call function as the last argument).

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
