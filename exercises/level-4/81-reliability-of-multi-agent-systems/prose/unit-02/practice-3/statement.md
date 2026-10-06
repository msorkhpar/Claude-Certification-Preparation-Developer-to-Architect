# Practice: a multi-agent run graded on injected failures

A research team of agents runs a plan of tasks, and in production some of them time out, some fail for good and some take the process down. In this practice you write
the runner that decides what happens next: which task is retried and with which key, which tasks are skipped because something they need failed, when an agent is
cut off, when a weaker agent takes over, and what a second run does with the work that was already done. The model is not called: the tests give you agents that
fail on demand. It is in Python, TypeScript, Java and Kotlin;

Names are Python's (`run_plan`, `Transient`, `Fatal`); TypeScript has `runPlan`; Java has the static method `runPlan` of `ReliableAgents` with the nested classes `Transient`,
`Fatal` and the interface `Agent`; Kotlin has top-level `runPlan`, the classes `Transient` and `Fatal` and the type `Agent`. The starters declare them.

## The plan, the agents and the store

- `plan` is a list of tasks in running order, each `{id, agent, key, needs, fallback}`: `agent` names an entry of `agents`, `key` is the task's idempotency key, `needs` lists the
  ids of earlier tasks whose results it takes (maybe empty or missing), `fallback` names a weaker agent (maybe none).
- An agent is a function `(key, inputs) -> string`, where `inputs` maps each needed id to its result. It may raise `Transient` (worth retrying), `Fatal` (not) or anything else.
- `store` is the checkpoint: a map from task id to result, owned by the caller and updated by the runner.

## What is already written, and what you write

The starter is a working runner with seven gaps cut out of it. The loop over the tasks, the single call to an agent (`call_once`, which turns a transient or fatal failure into a status and lets any other exception through), the inputs a task receives and the report are written and correct. Each gap is a small function with its signature, a comment that says what it receives and returns with one example, and the cases it unlocks. A gap returns a neutral value, so the starter runs and fails the cases on an assertion. To debug a gap, log its input with the `log` line at the top of the file; a run shows the lines under the failing case. Write them in this order (the TypeScript, Java and Kotlin names are the camel-case forms):

1. `breaker_open` unlocks `e4`: whether an agent has failed its threshold of calls in a row.
2. `record_outcome` unlocks `e4`: a success resets the count, a failure adds one.
3. `attempt` unlocks `m1`, `e1`, `e2` and `e4`: the call with the same key, the retry limit, the fatal failure and the open breaker.
4. `missing_dependency` unlocks `e3`: the first dependency that did not finish.
5. `fallback_key` unlocks `e5`: the fallback's own key.
6. `should_resume` unlocks `e6`: a task the store already holds is not run again.
7. `checkpoint` unlocks `m1`, `e5`, `e6` and `e7`: a finished result is written at once, a degraded one is not.

About fifteen lines in all.

## What to write

`run_plan(plan, agents, store, attempts, breaker_threshold)` runs the tasks in order and returns a map with `done` (id to result), `failed` (id to reason), `skipped` (id to reason),
`degraded` (ids), `attempts` (id to the number of calls made to agents for the task, 0 when none) and `resumed` (ids taken from the store).

- A task already in `store` is `resumed`: its stored result is `done` and no agent is called.
- A task whose `needs` holds an id that is not `done` is `skipped` with the reason `dependency failed: <that id>` (the first one, in `needs` order).
- Otherwise the agent is called with the task's `key` and its inputs. A `Transient` failure is retried with **the same key**, up to `attempts` calls in all; when they are used up the
  task `failed` with `retries exhausted`. A `Fatal` failure is not retried: the task `failed` with `fatal: <message>`.
- A breaker per agent name counts **consecutive** failed calls (a success resets it to zero). Before each call, if the count has reached `breaker_threshold`, the call is not made and
  the task fails with `circuit open`. The breaker stays open for the rest of the run.
- When the primary fails and the task has a `fallback`, the fallback agent is tried once, with the key `<key>:fallback` (and the same breaker rules). If it succeeds, its result is the
  task's `done` result and the id goes in `degraded`; a degraded result is **not** written to the store, so that a later run tries the primary again. If it fails too, the task `failed`
  with the primary's reason.
- A successful primary result is written to `store` as soon as the task finishes. An exception that is not `Transient` or `Fatal` is a crash: it is not caught, and everything written
  to the store before it stays there.

## Why each part is there, and what you should see

1. **Order and inputs.** A task sees only the results it asked for. *You should see* three tasks in order, the third with two inputs.
2. **One key for every retry.** A tool that pays or sends is safe to retry only if the repeat is recognised. *You should see* the same key on all three calls.
3. **Retry limits and fatal errors.** *You should see* three calls for a transient failure that never clears and one for a fatal one.
4. **Isolation.** A failed branch does not stop an independent one, and its dependents are skipped with a reason. *You should see* `c` done and `b` and `d` skipped.
5. **A breaker.** An agent that keeps failing stops being called, and one success clears the count. *You should see* zero calls for the third task of the failing agent.
6. **Degradation.** A weaker agent keeps the run moving, under its own key, without being mistaken for a final result. *You should see* `degraded` and a store without it.
7. **Resumption and crashes.** A second run does only what is left, and a crash loses nothing that was finished. *You should see* the first task called once over two runs.

## The cases

| Id | What it checks |
|---|---|
| `m1` | Tasks run in order and receive the results of the tasks they need |
| `e1` | Every retry of a task carries the same idempotency key |
| `e2` | Retries stop at the limit and a fatal failure is not retried |
| `e3` | A failure stays inside its branch and dependents are skipped |
| `e4` | A breaker stops calls to an agent that keeps failing, and a success resets it |
| `e5` | A fallback degrades one task with its own key and is not checkpointed |
| `e6` | A second run resumes from the checkpoint and retries only what failed |
| `e7` | An unexpected crash is not swallowed and keeps the work already checkpointed |
