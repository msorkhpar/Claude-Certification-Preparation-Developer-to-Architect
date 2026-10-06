# Practice: a roll-out gate

A team moves its product to a newer model the week before the old one retires. It runs the regression suite once, reads the overall score, switches all traffic at midnight and finds out at noon that refunds regressed, that the bill is a third higher and that three request settings now return an error. In this practice you write the pieces that replace that: the calendar of retirements with its urgency, the migration of a request to the settings a new model accepts, the gate that a regression suite must pass, and the step of a staged roll-out with its way back. The model is not called: the tests give you cases, costs, timings, requests and counts. It is in Python, TypeScript, Java and Kotlin; pick your language folder, open `starter/` and edit the file there.

Names are Python's (`retirement_status`, `migrate_request`, `gate`, `rollout_step`); TypeScript has the camel-case names (`retirementStatus`, `migrateRequest`, `rolloutStep`); Java has the same camel-case names as static methods of `Rollout`; Kotlin has top-level functions. A case is `Case(id, segment, must_pass, old_ok, new_ok, old_cost, new_cost, new_ms)` and a request is `Request(model, temperature, top_p, top_k, thinking, tool_choice, strict, prefill)`; the starter shows both in each language, with `days_until` and `percentile` (nearest rank) already written. A model is a `(name, retirement date, tentative)` triple, where the date is ISO text (`2026-11-30`) and tentative says that the date may still move later.

## What is already written, and what you write

The starter is a working kit with nine small gaps cut out of it. The plumbing is written: the data types, `days_until`, `percentile`, the sorting of the calendar, the sampling and tool-choice parts of the migration and the assembly of the gate from its checks. Each gap is a small function with its signature, a comment that says what it receives and returns with one example, and the cases it unlocks; a gap returns a neutral value, so the starter runs and fails the cases on an assertion. To debug a gap, log its input with the `log` line at the top of the file: a run shows the lines under the failing case. Write them in this order (the TypeScript, Java and Kotlin names are the camel-case forms):

1. `_status_line` unlocks `e7`: one calendar line, with its level and the tentative mark.
2. `_migrate_thinking` unlocks `e8`: the thinking setting and the change that names it.
3. `_must_pass` unlocks `e1`: the failed must-pass ids, sorted.
4. `_protected` unlocks `e2`: the protected segments that lost answers.
5. `_net_loss` unlocks `e3`: more losses than gains, with both counts.
6. `_cost` unlocks `e4`: the cost rise in whole percent against the limit.
7. `_latency` unlocks `e5`: the 95th percentile against the limit.
8. `_decision` unlocks `m1`: go with no reasons, no-go otherwise.
9. `rollout_step` unlocks `e6`: hold, roll back, complete or advance.

About twenty lines in all. The sections below describe the whole kit.

## What to write

- `retirement_status(models, today)` returns one line per model, `<name>: <days> days, <level>`, with ` (tentative)` added when the date is tentative. Days come from `days_until(today, date)` and are negative once the date has passed. The level is `retired` below zero, `urgent` up to 14 days, `migrate now` up to 60 days and `watch` beyond. The lines run from the fewest days left to the most, ties by name.
- `migrate_request(request)` returns the request for `claude-sonnet-5-5` and the list of changes, in this order: `model set to claude-sonnet-5-5` when the model differs; `removed temperature`, `removed top_p` and `removed top_k` for each of them that is present; a thinking setting of `budget` becomes `adaptive` (`thinking budget replaced by adaptive thinking; sweep the effort`) and `disabled` becomes `between_tools` (`thinking disabled replaced by between_tools`), anything else is kept; a forced tool choice (`any` or `tool`) becomes `auto` with `strict` set (`forced tool choice replaced by auto with strict tools`); an assistant prefill is dropped (`assistant prefill removed; state the format in the instructions`). The new request has no sampling settings and no prefill. A request that needs nothing comes back equal, with no changes.
- `gate(cases, protected, max_cost_up, max_p95)` returns a `decision` (`go` or `no-go`) and a list of `reasons`, one for each check that fails, in this order. `must-pass failed: <ids>` lists, sorted, the cases marked must pass that the new model fails. `protected segment lost answers: <segments>` lists, sorted and distinct, the protected segments with a case the old model got right and the new one did not. `net loss: lost N, gained M` appears when the cases lost outnumber the cases gained. `cost up X% over the Y% limit` appears when the total new cost exceeds the total old cost by more than `max_cost_up` percent (X is the rise in whole percent, rounded down; no rise is 0). `p95 latency X ms over the Y ms limit` appears when the nearest-rank 95th percentile of the new timings is over `max_p95`. The decision is `go` only when there is no reason.
- `rollout_step(stage, requests, errors, min_requests, max_errors_per_1000)` takes the stages 1, 5, 25 and 100 percent of traffic and returns `hold at <stage>` when the stage has fewer than `min_requests` requests, `rollback to 0` when errors per thousand requests (whole number, rounded down) are over the limit, `complete` at stage 100 when healthy, and `advance to <next stage>` otherwise.

## Why each part is there, and what you should see

1. **A go with no reasons.** *You should see* a change inside every limit get an empty list of reasons.
2. **A case that must pass.** *You should see* a change refused for one broken must-pass case, whatever the average says.
3. **A protected segment.** *You should see* a change refused when a protected segment lost answers although the gains elsewhere match the losses.
4. **A net loss.** *You should see* both counts named when the losses win.
5. **A budget for cost.** *You should see* a rise exactly at the limit pass and a rise one point over it refused.
6. **The tail, not the worst case.** *You should see* one slow case in forty not block the change and three slow cases block it.
7. **Small steps and a way back.** *You should see* a stage wait for enough requests, then advance, or go back to zero when errors pass the limit.
8. **A calendar that ranks.** *You should see* the nearest retirement first, and exactly fourteen days left called urgent.
9. **Settings a new model refuses.** *You should see* the sampling settings, the forced tool choice, the prefill and the disabled thinking replaced or removed, each one named.

## The cases

| Id | What it checks |
|---|---|
| `m1` | A change that regresses nothing and stays inside its limits gets a go with no reasons |
| `e1` | A must-pass case that fails blocks the change and the ids are listed in order |
| `e2` | A protected segment that lost answers blocks the change even when gains elsewhere match the losses |
| `e3` | More losses than gains blocks the change and both counts are named |
| `e4` | A cost rise over the limit blocks the change and a rise exactly at the limit does not |
| `e5` | The tail is the nearest-rank 95th percentile and a single slow case does not block |
| `e6` | A roll-out advances when healthy, holds with too few requests and rolls back to zero when errors pass the limit |
| `e7` | The retirement calendar counts days, ranks the nearest first and names the level |
| `e8` | Migration removes the settings the new model refuses and names each change |

Run the tests with the command in the language folder's `run.sh` (Python, TypeScript) or its build file (Java, Kotlin).
