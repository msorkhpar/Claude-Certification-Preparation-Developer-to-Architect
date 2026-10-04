# Practice: a review pipeline that picks the right interface, resubmits only what failed and combines independent passes

A team reviews pull requests and audits services with Claude. Its first design puts every job on the discounted batch queue, submits one batch a night while promising a 30-hour turnaround,
resubmits the whole batch when a few entries fail, and reviews a large change in a single pass whose findings nobody can confirm. In this practice you write the pipeline's decisions: the
submission interval that keeps a promise, the choice between the synchronous API and a batch, the plan for resubmitting failed entries by `custom_id`, the passes of a multi-file review, and the
combination of the passes' findings into one list with a route for each finding. The model is not called: the tests give you results, sizes and findings. It is in Python, TypeScript, Java and
Kotlin; pick your language folder, open `starter/` and edit the file there.

Names are Python's (`submission_interval`, `choose_api`, `resubmission_plan`, `review_plan`, `merge_passes`); TypeScript has the camel-case names (`submissionInterval`, `chooseApi`, ...); Java has
the same camel-case names as static methods of `BatchReview`, with the records the starter defines (`Result`, `Step`, `Pass`, `Finding`, `Merged`); Kotlin has top-level functions and the same data
classes. Python and TypeScript use tuples (arrays) and plain objects as the starters show.

## What to write

- `submission_interval(sla_hours, window_hours=24, handling_hours=2)` returns the longest gap in hours between submissions that still keeps every item inside the SLA: the SLA minus the
  processing window minus the handling time. An SLA that leaves no positive gap is refused (an error).
- `choose_api(blocking, needs_tool_loop=False)` returns `"synchronous"` for a blocking workload or one that needs a tool loop (a batch cannot run a tool and continue), and `"batch"` otherwise.
- `resubmission_plan(results, sizes, limit)` takes `(custom_id, kind)` results, where `kind` is `succeeded`, `expired`, `canceled`, `server_error` or `invalid_request`, a map of each
  `custom_id` to the size of its document in tokens, and a size limit. It returns `(custom_id, action)` pairs, in the order of the results, for the entries that did not succeed: `chunk` when the
  entry is over the limit, otherwise `fix` for an `invalid_request` and `resubmit` for the other kinds. Succeeded entries do not appear.
- `review_plan(files)` returns the passes of a review as `{name, files}`: one local pass per file named `local:<file>`, in order, and, when there is more than one file, a last pass named
  `integration` that holds all the files.
- `merge_passes(passes)` takes the findings of each independent pass (each finding has `file`, `line`, `severity` of `low`, `medium` or `high`, `issue` and `confidence`, a whole number from 0
  to 100) and returns one entry per distinct `(file, line, issue)` in the order first seen: the highest severity reported, the lowest confidence reported, `passes` as the number of distinct passes
  that reported it, and `route`: `accept` when at least two passes reported it and the confidence is 80 or more, otherwise `verify` (an independent verification, not a human review).

## Why each part is there, and what you should see

1. **The interval.** A promise is the interval plus the window plus the handling. *You should see* 4 hours for a 30-hour promise, and an error for a promise of 26.
2. **The interface.** A check that blocks a merge, or work that needs tools mid-request, cannot wait for a window. *You should see* `synchronous` for both and `batch` for the rest.
3. **Resubmit only what failed.** Succeeded entries are paid for already. *You should see* only the failed ids, with an oversized entry chunked and a rejected request fixed before it is sent again.
4. **Local passes plus an integration pass.** A single pass over many files dilutes attention and contradicts itself. *You should see* one local pass per file and one integration pass.
5. **Combining independent passes.** The same finding from two passes is one finding, and agreement of independent passes, not one pass's own confidence, earns acceptance. *You should see* a single
   high-confidence finding routed to `verify`, and a finding repeated inside one pass counted once.

## The cases

| Id | What it checks |
|---|---|
| `m1` | The interval leaves room for the window and the handling |
| `e1` | An SLA with no room for a batch is refused |
| `e2` | A blocking check or a tool loop needs the synchronous API |
| `e3` | Only the entries that did not succeed are resubmitted, by `custom_id` |
| `e4` | An entry over the limit is chunked and a rejected request is fixed first |
| `e5` | A multi-file review gets a local pass per file and one integration pass |
| `e6` | The same finding from two passes is one finding with the highest severity and the lowest confidence |
| `e7` | A finding is accepted only when two independent passes agree with confidence |

Run the tests with the command in the language folder's `run.sh` (Python, TypeScript) or its build file (Java, Kotlin).
