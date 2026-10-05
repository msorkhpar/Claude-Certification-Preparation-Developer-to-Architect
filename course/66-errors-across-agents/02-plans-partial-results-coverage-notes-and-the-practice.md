# Plans, partial results, coverage notes and the practice

**Level:** Architect · **Module 66:** Errors across agents · **Page 2 of 2**
**Exams:** A5.3; S3

**After this page you can** choose the coordinator's action for each kind of subagent result without stopping the run, use and mark partial results, handle a subagent that stopped at its turn limit, write a coverage note for a synthesis that separates well-supported topics from gaps and names the cause of each gap, and write the module's practice.

Checked on 2026-10-04 against the exam guide's task statement 5.3 and the Claude Agent SDK documentation page on subagents (output marked as partial at the turn limit; resuming a subagent). Nothing here called a model or a search tool: the practice is graded by Python, TypeScript, Java and Kotlin test suites, offline, on outcomes written by hand. Module 46 covered the coordinator's bounded rounds and the honest `partial` status of an answer; this page is the per-result decision that feeds them, and the note that goes into the report.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* the synthesis output carries "coverage annotations indicating which findings are well-supported versus which topic areas have gaps due to unavailable sources", and the coordinator, given structured error context, chooses a recovery (retry, an alternative, proceed with what exists) without terminating the workflow. *What the product does now (documentation read 2026-10-04):* a subagent that reaches `maxTurns` has its output returned marked as partial so that "Claude knows the run is unfinished", and a custom subagent can be resumed with its full history to continue where it stopped (the built-in `Explore` and `Plan` agents are one-shot and cannot). The product marks one kind of incompleteness. Missing sources, refused access and empty searches are for your instructions and code to mark. On the exam, a question about a report that reads as complete although sources failed has the answer: a coverage annotation.

## Why it matters

The coordinator now receives five structured outcomes. What it does with each decides whether the report is honest. A success is used. An empty result is written down as "no findings" and not hidden. A failure with partial results is used, marked partial. A failure without partial results but with an alternative is tried once more by another route. A failure with nothing to try becomes a named gap. In every case the run goes on: one refused source does not cancel the other four. Then the report says, in a short note, which topics rest on evidence and which do not, and why. A reader who sees "Gaps: filings (permission denied)" knows what to ask for next. A reader who sees a confident survey with a hole in it learns about the hole later and from someone else.

## The idea

### One decision per result, and the run goes on

The practice's `coordinator_plan` maps each topic's outcome to an action:

| Outcome | Action | Why |
|---|---|---|
| Success | `use` | The evidence is in hand |
| Empty | `no_findings` | A valid answer, recorded as such |
| Failed, with partial results | `use_partial` | Work already done is kept, and marked partial |
| Failed, no partial results, alternatives exist | `try_alternative` | A cached source, another provider, a narrower query |
| Failed, nothing to try | `flag_gap` | Say what is missing, and do not guess |

The two outcomes that are easiest to swap are the empty result and the failure. Treat the empty result as a failure, and the system retries a search that already worked. A search that runs and finds nothing has succeeded. A failure reported as empty is the worse mistake, since nobody looks again.

The plan is a list with an entry for every topic. There is no entry that ends the run. An alternative is tried once, within the bounded rounds of module 46, and a second failure becomes a gap.

### Partial results are marked, not hidden and not discarded

Three results of ten expected is not nothing and not everything. Discarding them wastes work the subagent did; presenting them as the full answer is the silent suppression of page 1 in another form. They are used and labelled: the synthesis says that the topic rests on three of ten sources and why.

The product's own partial marker is narrower. When a subagent stops at its `maxTurns` limit, the parent receives its output flagged as partial (Claude Code v2.1.246 or later). The coordinator then has a real choice: accept the partial output and note it, or resume that subagent, which keeps its full history, to let it finish. Resuming is cheaper than starting a fresh subagent on the same task when the work so far is good. The built-in `Explore` and `Plan` agents are one-shot and cannot be resumed, so a task that may need continuation is given to a custom subagent.

### The coverage note

The report ends with, or opens with, a short note built from the outcomes. The practice's `coverage_note` has four lines at most, in a fixed order:

- `Well-supported`: the topics with evidence;
- `Partial`: topics that rest on part of their sources, with the cause;
- `No findings`: topics that were searched and have nothing;
- `Gaps`: topics that could not be covered, each with the cause and the query attempted, and a topic that was never searched, marked as such.

A topic that was never searched is a gap. A report that quietly omits a topic is saying something false by leaving it out, and the note is the place where the system's own plan is compared with what happened. A generic line such as "some sources may be unavailable" is not a coverage note: it names no topic and no cause, so no reader can act on it.

### The practice: errors that travel well

The practice is in [`exercises/66-errors-across-agents`](../../exercises/66-errors-across-agents/unit-01/practice-1/statement.md). You write the subagent's recovery (local retries, the outcome with its context), the coordinator's plan for every result and the coverage note. It is graded in Python, TypeScript, Java and Kotlin; the statement lists seven cases, each saying what you should see when it works.

## Traps

1. **"Three of ten sources answered; discard them, a partial answer is unreliable."** It is tempting because partial feels unfinished. The exam rejects it: partial results are used and marked, and the work already done is kept.
2. **"Say in the report that some sources may be unavailable."** It is tempting because the sentence is always true. The exam rejects it: a coverage note names the topics that are supported and the topics that are gaps, with the cause of each.
3. **"A topic with no result is left out of the report; nobody asked for it."** It is tempting because the report reads cleaner. The exam rejects it: a topic that was planned and never searched is a gap, and omitting it presents the survey as complete.
4. **"The source refused access; retry it until it opens."** It is tempting because persistence sounds diligent. The exam rejects it: a refusal is not transient, so the subagent reports it at once with the alternatives, and the coordinator records a gap if none works.

## Quiz

1. Scenario S3, multi-agent research system. A subagent returns an empty list when its lookup service times out, and the coordinator's report says that the topic has nothing in the sources. What fixes the cause?
   - **a**: Treat every blank answer as a failure and send it back for another attempt
   - **b**: Mark the timeout as a typed failure, distinct from a genuine absence of matches
   - **c**: Remove the topics with no results from the report so that no false claim remains in it
   - **d**: Have the coordinator rerun any topic whose report says that nothing was found

2. Scenario S3, multi-agent research system. A synthesis step merges findings on six topics. Two sources were unreachable, and one search found nothing. The report reads as a complete survey. What should the step add?
   - **a**: A confidence score for the whole report, shown as a single number between zero and one
   - **b**: A line saying that some sources may be unavailable at the moment of writing
   - **c**: A note that sorts subjects into evidenced and unevidenced, with the cause of each gap
   - **d**: A longer introduction describing in detail how the survey was carried out and by whom

<details>
<summary>Answer key</summary>

1. **b**. A failure and a valid empty result are different values with different handling. *a* is ruled out because "Treat the empty result as a failure, and the system retries a search that already worked". *c* is ruled out because "A report that quietly omits a topic is saying something false by leaving it out". *d* is ruled out because "A search that runs and finds nothing has succeeded", so rerunning it repeats a finished job and leaves the timeout unreported.
2. **c**. The note compares what was planned with what happened, topic by topic. *a* is ruled out because the note is "the place where the system's own plan is compared with what happened", and a single score compares nothing. *b* is ruled out because a line like that "names no topic and no cause, so no reader can act on it". *d* is ruled out because "A reader who sees a confident survey with a hole in it learns about the hole later and from someone else".

</details>

## Module quiz

This quiz covers both pages of the module.

1. Scenario S3, multi-agent research system. One of four research subagents is refused access to a source, and the coordinator cancels the run and tells the user that it failed. Which handling fits?
   - **a**: Retry the refused one again and again until the door finally opens, then continue
   - **b**: Ask the user to restart the whole run after the source has been fixed
   - **c**: Fill the closed gap with a plausible summary drawn from the model's own general knowledge
   - **d**: Carry on with the other three and log the closed topic as a gap, with a way to reopen it

2. A subagent stops at its maxTurns limit after doing good work. What does the parent receive, and what can it do?
   - **a**: An error that discards the output, so that the task has to be started again
   - **b**: Nothing at all until the user raises the limit and runs it again
   - **c**: The output so far, flagged as partial, and the option to resume it later
   - **d**: The output with no flag at all, which looks like a finished answer

3. Scenario S3, multi-agent research system. During a run on one subject, a subagent's source stops answering after supplying three of the ten items that were expected. What should the coordinator do with the three?
   - **a**: Discard the three, since a partial answer is judged too unreliable to use at all
   - **b**: Present the three to readers as the subject's complete answer
   - **c**: Wait and rerun the lookup until all ten items have arrived
   - **d**: Use them, mark the topic as partly evidenced and say what is lacking

<details>
<summary>Answer key</summary>

1. **d**. Terminating on one failure wastes the other work, and a refusal is recorded as a gap with the way to get access. *a* is ruled out because "a permission error or an invalid query fails again the same way, and the subagent reports it at once". *b* is ruled out because "terminating on a single failure throws away the work of the other subagents". *c* is ruled out because the plan for a failure with nothing to try is "Say what is missing, and do not guess".
2. **c**. The output comes back marked as partial, and a custom subagent keeps its history so that it can be resumed. *a* is ruled out because "the parent receives its output flagged as partial", and nothing is discarded. *b* is ruled out because "Resuming is cheaper than starting a fresh subagent on the same task when the work so far is good", and no one has to rerun it. *d* is ruled out because the marker exists so that "Claude knows the run is unfinished".
3. **d**. The three results are kept and labelled, with the gap stated. *a* is ruled out because "Discarding them wastes work the subagent did". *b* is ruled out because "presenting them as the full answer is the silent suppression of page 1 in another form". *c* is ruled out because "An alternative is tried once, within the bounded rounds of module 46, and a second failure becomes a gap".

</details>

Adapted from the Claude Certified Architect - Foundations exam preparation guide by Daron Yondem (CC BY 4.0, https://creativecommons.org/licenses/by/4.0/), changed for this course.
