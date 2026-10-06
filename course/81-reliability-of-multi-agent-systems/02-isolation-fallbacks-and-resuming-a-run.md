# Isolation, fallbacks and resuming a run

**Level:** Architect Professional · **Module 81:** Reliability of multi-agent systems · **Page 2 of 2**
**Exams:** P1, P5

**After this page you can** keep a failure inside its branch of a plan and skip only what depends on it, degrade a task to a weaker agent without mistaking that result for a final one, checkpoint finished work so that a second run does only what is left, and tell a crash that must not be swallowed from a failure that is handled.

Checked on 2026-10-04 against Anthropic's engineering article "How we built our multi-agent research system", Anthropic's "Building effective agents", and the Claude Certified Architect, Professional exam guide v1.0 (July 2026), domains 1 and 5. This page has no new example: the practice is the example, and it is graded in Python, TypeScript, Java and Kotlin with agents that fail on demand and no model call.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* the Professional guide expects a design in which one failure does not bring down the whole run, and in which work already done is not repeated. *What Anthropic's article says:* its research system can resume from where an agent was when an error occurred and does not restart from the beginning, because restarts are expensive and frustrating for users, and it tells the agent when a tool is failing so that the agent can adapt. *How to read both:* the exam keys the answer that confines the failure, keeps what is finished and reports what was lost, over the answer that retries the whole run or hides the gap. The article describes one production system, so read its practices as examples of the pattern and not as a requirement.

## Why it matters

A research run has twelve tasks. On the ninth, a source is unreachable. The simple design aborts, and eleven tasks of paid work are thrown away. A second design carries on and ships a report that quietly lacks a section, and nobody knows. A third design lets one retry loop grow until the bill is large. The architect's contribution is a fourth design, in which the failure stays in its branch, the independent work finishes, the report says what is missing and why, and a second run picks up the one task that remains.

## The idea

### A failure stays in its branch

A plan is a set of tasks, and some tasks need the results of others. When a task fails, three groups of tasks exist: the ones that needed it, the ones that did not, and the ones that already finished. Only the first group is affected. The runner marks each task that needs a failed task as **skipped**, with a reason that names the failed one (`dependency failed: b`), and it carries on with the independent tasks. The final report lists three things apart: what is done, what failed and why, and what was skipped because of it. A reader of that report knows exactly how far to trust it.

This is **failure isolation**, and it is the property that makes a team of agents worth more than a long chain. In a chain, one failure ends everything after it. In a plan with independent branches, it ends only its own branch.

### A fallback degrades, and says so

When a primary agent fails, a weaker one may be able to do the task: a smaller model, a cached answer, a simpler method. Using it keeps the run moving, with three conditions.

- **It is tried once, under its own key.** The fallback is a different attempt with a different effect, so it carries its own idempotency key (the primary's key with a suffix), and the breaker rules apply to it as to any agent.
- **It is marked as degraded.** The result is used, and the task is listed as degraded, so that the report and the caller know the answer is weaker than planned.
- **It is not checkpointed.** A degraded result is not written to the store of finished work. If it were, a second run would find the task done and never try the primary again, and the weaker answer would become permanent by accident.

### Checkpoint and resume

A **checkpoint** is a store of finished results, written as each task completes. A run that starts with a store takes every task found there as done (it is **resumed**) and calls no agent for it, so a second run does only the work that is left: the task that failed, the tasks that were skipped because of it, and nothing else. Three points matter.

1. **Write on success, immediately.** A result is stored the moment its task finishes, not at the end of the run. A crash in the tenth task then loses nothing from the first nine.
2. **Store only final results.** Failures, skips and degraded answers are not stored, because a later run must be free to try them again.
3. **Pair it with the key.** A task whose effect happened but whose result was not stored is retried with the same key on the next run, and the tool returns the recorded result and does not act again. The checkpoint and the idempotency key cover the two halves of the same gap.

### A crash is not a failure

The runner handles failures it expects: a transient one (retry), a fatal one (record it), an open breaker (fail fast). Anything else is a **crash**, a defect or an unexpected state, and it must not be caught. If a runner swallows an unknown exception and carries on, it hides the bug and reports a result it cannot vouch for. Because the checkpoint was written as work was done, the crash still loses nothing finished: the process stops, the cause is fixed, and the run resumes.

### The practice: a run graded on injected failures

The practice is in [`exercises/81-reliability-of-multi-agent-systems`](../../exercises/81-reliability-of-multi-agent-systems/unit-01/practice-1/statement.md). You write `run_plan`: it runs a plan in order, passes each task its inputs, retries a transient failure with the same key, stops at a limit, never retries a fatal one, keeps a breaker per agent, falls back once under its own key, checkpoints finished work and resumes from it. It is graded in Python, TypeScript, Java and Kotlin on agents that fail on demand, and the statement lists eight cases, each saying what you should see when it works.

## Traps

These are the wrong answers the exam's options for this domain offer, each with the reason it is rejected.

1. **"When a task fails, abort the run and start again once the cause is fixed."** It is tempting because it is simple and leaves no half-finished state. The exam rejects it: it discards the work that finished, which is the expensive part, and a checkpoint lets a second run do only what is left.
2. **"Save the fallback's answer to the store so that the work is not repeated."** It is tempting because it looks like caching. The exam rejects it: a stored degraded answer makes the task look done, so no later run tries the primary again and the weaker answer becomes final without anyone deciding it.
3. **"Catch every exception in the runner so that one bad task never stops the run."** It is tempting because it looks like resilience. The exam rejects it: a crash is a defect, swallowing it hides the defect and reports unverified results, and the checkpoint already keeps the finished work safe.

## Quiz

1. Scenario: Elm Biotech's plan has five tasks: a, b, c, d and e. Task a completes first, then b breaks for good; d depends on b, and c and e depend on neither. What should the runner report?
   - **a**: a, c and e done, b failed, d skipped because of b, with each reason stated
   - **b**: a, c and e done, with b and d both listed as failed, each with its reason given
   - **c**: a, c, d and e done, with b alone listed as failed and the reason for it stated
   - **d**: a done and b failed, with c, d and e held back until someone has repaired b

2. Scenario: Rowan Insights' search agent fails, and a cheaper backup agent answers the task. The runner keeps the backup's answer. What should it do with that result?
   - **a**: Write it to the store of finished work, so that a later run does not repeat the same effort
   - **b**: Use it marked as degraded and leave it unsaved, so that a later run can try the primary
   - **c**: Discard it, because only the primary agent's answers may ever appear in a final report
   - **d**: Use it and say nothing, because the reader of the report wants only the content of the answer

<details>
<summary>Answer key</summary>

1. **a**. Only the task that needed b is affected, and the report lists what is done, what failed and what was skipped. *b* is ruled out because d did not fail on its own, and the report keeps apart "what failed and why, and what was skipped because of it". *c* is ruled out because d needs b and cannot be done, so the runner marks it skipped "with a reason that names the failed one". *d* is ruled out because c and e need neither, and the runner "carries on with the independent tasks".
2. **b**. A degraded result is used and flagged, and it is not stored, so a later run can try the primary again. *a* is ruled out because a stored answer makes the task look done: "the weaker answer would become permanent by accident". *c* is ruled out because the fallback "keeps the run moving", and discarding it loses the point of having one. *d* is ruled out because the task is listed as degraded "so that the report and the caller know the answer is weaker than planned".

</details>

## Module quiz

This quiz covers both pages of the module.

1. Scenario: Dovetail Labs sets a breaker threshold of three consecutive failures for each agent. Agent X fails, succeeds, fails, succeeds, and fails again. Agent Y fails three times running. Which breakers are open afterwards?
   - **a**: Only Y's, since X's tally returns to zero whenever a call works
   - **b**: Both, since each agent has failed at least three times in all across the run
   - **c**: Only X's, since an agent that keeps alternating is the less stable of the two
   - **d**: Neither, because a breaker opens only after a cooldown has already passed

2. Scenario: Larkspur Media's runner wraps every task in a catch-all that records any exception as a failure and carries on. Task seven raises an error that none of the runner's handlers was written for, and it does so in every run. What is wrong with this design?
   - **a**: It records the error without a retry, so a passing glitch is turned into a lasting failure
   - **b**: It keeps the results saved before the error, so the next run builds on work it cannot trust
   - **c**: It treats a crash as an expected outcome, so the bug behind it never comes to light
   - **d**: It lets the run go on, so the tasks that need task seven then run without their input

3. Scenario: Hazel Analytics' plan has tasks a, b, c and d, where d needs b. In the first run b used up its retries, and a and c finished and were saved. What does a second run call?
   - **a**: All four, since a run restarts the whole plan
   - **b**: Only b, since a skipped task is not retried later
   - **c**: b, then d, since a and c come from the checkpoint
   - **d**: Just d, since b is known to have failed in the earlier run

<details>
<summary>Answer key</summary>

1. **a**. The count is of consecutive failures, and a success clears it. *b* is ruled out because "The count is of consecutive failures for one agent, not of all failures in the run". *c* is ruled out because "A success resets the count, so scattered failures do not open it". *d* is ruled out because the breaker opens "After a set number of failures in a row", and the cooldown comes after it opens.
2. **c**. An error that no handler expects is a crash, and recording it as a routine failure hides the defect behind it. *a* is ruled out because the error returns in every run, and of a failure that never passes the page says "It does not help with one that does not". *b* is ruled out because the results saved before the error are finished work, and "the crash still loses nothing finished". *d* is ruled out because the runner marks "each task that needs a failed task" as skipped, so no task runs without its input.
3. **c**. The second run does the failed task and the one that was skipped because of it, and takes the rest from the checkpoint. *a* is ruled out because "a second run does only the work that is left". *b* is ruled out because "Failures, skips and degraded answers are not stored, because a later run must be free to try them again". *d* is ruled out because d needs b, and the remaining work is "the task that failed, the tasks that were skipped because of it, and nothing else".

</details>
