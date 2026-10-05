# Failures, conflicts, the coverage note and the practice

**Level:** Architect · **Module 72:** Scenario: multi-agent research system · **Page 2 of 2**
**Exams:** A1, A2, A5; S3

**After this page you can** decide what a coordinator does with a failed subagent, keep partial results without counting them as coverage, report conflicting sources with their dates, write a status and a coverage note that cannot overstate the work, and write the module's practice: the coordinator's last step as a function.

Checked on 2026-10-04 against Anthropic's engineering article on its multi-agent research system and the Architect exam guide (version 1.0, scenario 3). The practice is offline in Python, TypeScript, Java and Kotlin: the tests build the results of the subagents and read your report. The shapes of a result and of the report, the single retry and the order of the fields are this course's own design for the capstone, not an Anthropic interface, and the statement says so.

## Why it matters

Page 1 was about the coordinator's duties before and during a run. This page is about the end of it: what the coordinator knows when the subagents have finished, and what it may say. A research report is trusted because of its claims, its sources and its limits. A report that hides a failed search, picks one of two conflicting figures without saying so, or calls itself complete when a scope is missing has lost the third of these, and the reader cannot tell. The exam's questions about failures and about citations are questions about what the coordinator is allowed to claim.

## The idea

### A failure is a result with context

A subagent that fails returns a result, and the result has a shape the coordinator can act on: the type of the failure, the query that was tried, any partial results that arrived before it, and alternatives worth trying. The coordinator then has three choices, and it needs the context to make any of them. It can retry through an alternative. It can try a different approach to the same scope. Or it can continue with what is partial and say so.

The coordinator retries once through an alternative and then stops. The limit is the course's design, and the reason is that one failure of a different query is a reason to try again, while a second suggests that the source or the service has a problem a third query will not fix. A second failure stays a failure, and the report says so, with both queries named. A failure of one subagent does not end the run: the other scopes are still worth reporting.

Three tempting designs fail in different ways. A generic "search unavailable" after the retries leaves the coordinator nothing to decide with. An empty result marked as a success turns a gap into an apparent answer. And terminating the whole run on the first failure throws away every scope that worked.

### Partial is information and it is not coverage

When a search fails after returning something, the something is worth keeping. Partial findings stay in the claims, flagged, so a reader can use them and can see that they are incomplete. A partial result is information, and it is not coverage: the scope it belongs to is still a gap, because coverage is a property of the question and not of what happened to arrive. A scope that is marked covered without findings is a claim that nothing supports.

Two rules keep the flag honest. A claim is marked partial only when every finding behind it was partial: one finished source is enough to unmark it. And an error that a later result for the same scope made up for is dropped from the list of errors, since the report should list what is still wrong.

### Conflicts, and why the dates matter

Each finding carries its source and its date. When two findings give different values for the same claim, the report does not average them and does not pick the newer one in silence. A conflict is two different values for one claim, not two sources for the same value, so two sources that agree are one claim with two sources, and two that disagree are a conflict that lists both with their dates (module 69 splits this into a conflict on one date and a change across dates; this practice keeps the one name and lets the dates show which). The date matters because it is often the explanation: a figure measured in one year and a figure measured in the next can both be right, and the reader can only see that if the dates are shown. The conflicting claim is kept out of the list of agreed claims, so that no reader takes it for one.

### The status and the note

The status is computed from what the run covered: complete when every required scope has an `ok` result with at least one finding, partial otherwise. A conflict does not change the status, because it is information the reader needs and not a missing piece. The note says what the report could not cover and why, in the order of the scopes. Its three phrasings carry three different diagnoses. A scope whose search failed names the type and the query. A scope that no result names was never researched, which is the decomposition fault of page 1 and is fixed in the plan. A scope with a result but no findings has no findings. A reader of the note can tell a failure of a tool, a fault in the plan and a quiet source apart.

### The practice: the coordinator's last step

The practice is `exercises/72-scenario-multi-agent-research-system/unit-01/practice-1/statement.md`, in Python, TypeScript, Java and Kotlin. You write `synthesize(required, results)`: the covered scopes and the gaps, the status, the claims with their sources and their partial flag, the conflicts, the unresolved errors with their alternatives, the scopes that have only partial results, and the note.

The tests build the results of the subagents and grade seven cases: a full run with agreement, an empty run, a scope that was never planned against a scope whose search failed, a conflict between two sources, partial results with a retry that worked, a result with no findings, and the fixed order of claims, sources and scopes. The starter fails all seven, the reference passes them, and each of ten planted wrong solutions per language fails on an assertion of the case it breaks: an empty run called complete, a conflict settled by the first value, partial results counted as coverage, an empty result counted as coverage, partial findings dropped, a resolved error kept, a source listed twice, claims left unsorted, a note that leaves out the query of a failed search, and a claim flagged partial when any of its findings was.

## Traps

These are the wrong answers that the exam's options for this scenario offer, each with the reason it is rejected.

1. **"Retry inside the subagent, and return a generic 'search unavailable' once the retries are used up."** It is tempting because retries sound robust. The exam rejects it: the generic status hides what failed and what could be tried, so the coordinator cannot decide.
2. **"Catch the timeout and return an empty result marked as successful."** It is tempting because the run goes on without an error. The exam rejects it: it turns a failure into a false answer, and nothing downstream can recover from a failure it was never told about.
3. **"Send the timeout to a top-level handler that ends the whole run."** It is tempting because it is simple and loud. The exam rejects it: recovery was possible, and the scopes that worked are lost with it.

## Quiz

4. Under the module's bounded policy for failures, a search subagent times out and its error offers an alternative query. What should the coordinator do?
   - **a**: Retry the same query until it succeeds, however many attempts that takes in total
   - **b**: Retry with the suggested substitute once, then report the scope as not covered
   - **c**: Stop the whole run and return the error to the caller, who will decide
   - **d**: Mark the scope as covered and let the synthesis agent fill the gap

5. Two publications report 12 and 14 for the same statistic, a year apart. How should the report treat them?
   - **a**: Report the value that more of the other documents seem to support, whichever it is
   - **b**: Report the newer value alone, since a more recent figure replaces an older one
   - **c**: Report the average of the two figures, since each is probably close to the truth
   - **d**: Show both values with their sources and dates, and give neither as the answer

6. A search failed after it had already returned two usable results for its topic. How should the report record that topic?
   - **a**: As covered, because some usable results were returned for it
   - **b**: As uncovered, keeping what arrived but marking it incomplete
   - **c**: As covered once the synthesis agent has accepted the results
   - **d**: As missing, with the two results dropped so that they cannot mislead

<details>
<summary>Answer key</summary>

4. **b**. One retry through the alternative, then an honest gap. *a* is ruled out because the retry is bounded: "The coordinator retries once through an alternative and then stops." *c* is ruled out because one failed scope does not end the run: "A failure of one subagent does not end the run: the other scopes are still worth reporting." *d* is ruled out because a scope without findings is not covered: "A scope that is marked covered without findings is a claim that nothing supports."
5. **d**. A conflict is shown with both values, both sources and both dates. *b* is ruled out because the newer value is not picked in silence: "A conflict is two different values for one claim, not two sources for the same value". *c* is ruled out because the values are not blended: "the report does not average them and does not pick the newer one in silence". *a* is ruled out because the date is often the explanation: "a figure measured in one year and a figure measured in the next can both be right".
6. **b**. Partial findings are kept and flagged, and the topic is still a gap. *a* is ruled out because partial is not coverage: "A partial result is information, and it is not coverage". *c* is ruled out because acceptance is not the test: "coverage is a property of the question and not of what happened to arrive". *d* is ruled out because the findings are worth keeping: "Partial findings stay in the claims, flagged, so a reader can use them and can see that they are incomplete."

</details>

## Module quiz

This quiz covers both pages of the module.

1. Scenario S3, a multi-agent research system in which a coordinator delegates to specialised subagents. A run ends with three of four scopes covered, one unresolved error and one conflict between two sources. Which status does the report carry?
   - **a**: Complete, since a conflict is information and the scopes were mostly covered anyway
   - **b**: Partial, since one topic is missing, and the disagreement is listed beside it
   - **c**: Complete, since the unresolved error was only a timeout and nothing was lost
   - **d**: Failed, because an unresolved error invalidates the whole report

2. Scenario S3, a multi-agent research system in which a coordinator delegates to specialised subagents. The synthesis agent's verification tool is asked to confirm a market-share figure that it cannot check locally. What should it return?
   - **a**: A verdict that a lookup is needed, which the hub routes onward
   - **b**: A confirmation, since the tool must leave no claim unchecked
   - **c**: An error that ends the synthesis step for the whole run
   - **d**: A guess at the figure based on the other claims in the report

3. Scenario S3, a multi-agent research system in which a coordinator delegates to specialised subagents. A report's note lists `music (not researched)` beside `film (timeout on a query)`. What does the note tell the reader about music?
   - **a**: No subtask of the plan named it, so the fault is in the decomposition
   - **b**: Its search failed on a fault of the tool, with the query recorded
   - **c**: A source was searched and had nothing to say about it
   - **d**: A conflict between two sources left the topic incomplete

4. Scenario S3, a multi-agent research system in which a coordinator delegates to specialised subagents. A claim rests on two findings: one that arrived before a subagent failed and one from a subagent that finished. Both agree. How is the claim flagged?
   - **a**: As incomplete, because one of its findings arrived from a run that failed
   - **b**: As conflicting, since the two findings came from two different runs
   - **c**: Without the incomplete mark, because a completed run backs it as well
   - **d**: As missing, because partial evidence cannot support a claim

<details>
<summary>Answer key</summary>

1. **b**. A missing scope makes the run partial, and a conflict is reported next to it. *a* is ruled out because completeness is about coverage: "A run is complete when every scope is covered." *c* is ruled out because the reason for a gap does not matter: "It is partial otherwise, whatever the reason, and the report says which scopes are missing and why." *d* is ruled out because the report is still useful: "A report that did not cover film is still a useful report".
2. **a**. Anything the tool cannot check goes back to be delegated. *d* is ruled out because the tool does not guess: "In the example, the tool answers for a date it can check, and returns `needs_search` for anything else." *b* is ruled out because a deep check stays where it was: "leaves the deep verification where it was: with the coordinator". *c* is ruled out because the tool exists to remove the round trip for the common case: "A scoped tool of this kind removes the round trip for the common case".
3. **a**. A scope that no result names was never planned: "A scope that no result names was never researched, which is the decomposition fault of page 1 and is fixed in the plan." *b* is ruled out because that is the phrasing for a failed search: "A scope whose search failed names the type and the query." *c* is ruled out because that is the third phrasing: "A scope with a result but no findings has no findings." *d* is ruled out because a conflict is not a gap: "A conflict does not change the status, because it is information the reader needs and not a missing piece."
4. **c**. One finished source is enough to remove the flag. *a* is ruled out because the flag needs every finding to be partial: "A claim is marked partial only when every finding behind it was partial". *b* is ruled out because the findings agree: "A conflict is two different values for one claim, not two sources for the same value". *d* is ruled out because partial findings stay as claims: "Partial findings stay in the claims, flagged, so a reader can use them and can see that they are incomplete."

</details>
