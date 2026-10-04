# The research system: who owns coverage

**Level:** Architect · **Module 72:** Scenario: multi-agent research system · **Page 1 of 2**
**Exams:** A1, A2, A5; S3

**After this page you can** describe the exam's research system and what each of its four subagents may do, route a symptom in a finished report to the failure behind it and to the first fix, say what the coordinator owns that no subagent can, and read a run in which the plan is too narrow, a search fails and a scoped tool checks a date.

Checked on 2026-10-04 against Anthropic's engineering article on its multi-agent research system and the Architect exam guide (version 1.0, scenario 3 and its sample questions). The example runs offline in Python, TypeScript, Java and Kotlin: the subagents are functions over made-up data, so the example is about what the coordinator does with what comes back. This page is a capstone: it uses modules 46, 47, 52, 53 and 54, and names modules 66 (Errors across agents) and 69 (Provenance and uncertainty), which treat the failure path and the citations in full.

## Why it matters

Scenario S3 is the architecture the exam keeps returning to: one coordinator and several specialised subagents. The failures it asks about share a property that makes them hard to see. Every subagent can succeed, and the report can still be wrong, because the error is in what was assigned, in what came back and was passed on, or in what the final status claims. The exam rewards the habit of reading the coordinator's side: the plan it made, the errors it received and the status it wrote.

## The idea

### The scenario in plain words

A research system is built on the Claude Agent SDK. A coordinator receives a topic and delegates to four specialised subagents: one searches the web, one analyses documents, one synthesises the findings and one writes the report. The output is a comprehensive report with citations. The domains the exam draws on are the agent architecture, the design of tools and their integration, and the management of context and reliability.

### Read a symptom as a failure shape

| What the finished report or the logs show | The failure shape | The first fix | Taught in |
|---|---|---|---|
| Every subagent succeeded, yet the report covers one part of the topic, and the coordinator's log shows narrow subtasks | A decomposition that is too narrow | Fix the plan: compare it with the scopes the question needs before any subagent runs | Modules 46 and 50 |
| A subagent returns work on the wrong period or the wrong scope | A brief that did not carry its context | Put the objective, the output format, the tools and sources, and the boundaries into the brief | Module 47 |
| The search times out and the coordinator is told "unavailable", or gets an empty result marked as a success | A failure with no context, or a failure turned into a success | A structured error: type, the query, partial results, alternatives | Module 53, and module 66 (Errors across agents) |
| Synthesis keeps asking the coordinator to check a date, and each round trip adds latency | A tool the agent needs sits behind a round trip | A narrow tool for the frequent simple check, and the deep ones still go through the coordinator | Module 54 |
| Two sources give two figures and the report shows one | A conflict resolved silently | Show both values with their sources and dates | Module 62, and module 69 (Provenance and uncertainty) |
| The coordinator's context fills with the raw output of its subagents | Large results passed through the hub | Subagents store their work and pass back references | Module 46 |
| The report says it is complete and a scope is missing | A status that is not derived from coverage | Compute the status from the scopes covered | This module |

The article on Anthropic's own research system gives the brief its four parts: "Each subagent needs an objective, an output format, guidance on the tools and sources to use, and clear task boundaries." It describes the way large results travel: "Subagents call tools to store their work in external systems, then pass lightweight references back to the coordinator." And it says what to do about a failing tool: "Letting the agent know when a tool is failing and letting it adapt works surprisingly well." Each line is a design rule of the table above.

### What the coordinator owns

A subagent owns the scope it was given. The coordinator owns everything that is about the whole question, and four of those duties are exactly where the exam's failures sit.

1. **The decomposition.** A plan is a claim that its subtasks, together, cover the question. The coordinator can check the claim: list the scopes the question needs and compare them with the scopes of the subtasks. The example's first plan covers one of four scopes, so the check names the other three before a single search runs. A subagent cannot do this check: it sees only its own brief. A downstream agent that is asked to notice gaps is a late and unreliable substitute for a check that costs one comparison.
2. **The recovery.** When a subagent fails, the coordinator decides: try an alternative, try another approach, or go on with what is partial. It can decide only if the error says what failed and what could be tried.
3. **The status.** A run is complete when every scope is covered. It is partial otherwise, whatever the reason, and the report says which scopes are missing and why.
4. **The conflicts.** When two subagents return different values for the same claim, the coordinator does not pick. It carries both, with their sources and dates, to the report.

### What each subagent may do

The least-privilege rule of module 54 applies to each of the four. The search agent has the web tools and nothing that writes. The analysis agent reads the documents it is given. The synthesis agent combines findings, and for the frequent simple check (a date, a name) it has one narrow verification tool and no search tools. The report agent writes the report and calls nothing else. A scoped tool of this kind removes the round trip for the common case and leaves the deep verification where it was: with the coordinator, who delegates it to the search agent. In the example, the tool answers for a date it can check, and returns `needs_search` for anything else.

### The example

The example runs the coordinator's side of one research run. A first plan of three subtasks, all in visual arts, is checked against the four scopes the question needs; the check finds three gaps, and the coordinator adds a subtask for each. The search for film then fails as a timeout, and the failure comes back as a result with its type, its query, the partial results (none) and an alternative query. The coordinator tries the alternative once and it works. The synthesis agent's verification tool confirms two dates and sends one statistic back. Finally the report is built twice, once with every source up and once with the film search down for good. In that second run the status is `partial`, three of four scopes are covered, and the note names the two queries that failed.

<!-- example: m72-research-run tabs: python -->
<!-- /example -->

The point of the last two lines is the second one. A report that did not cover film is still a useful report, and the status and the note are what keep it honest: the reader learns that a scope is missing, and which queries were tried. All four languages print the same lines.

## Traps

These are the wrong answers that the exam's options for this scenario offer, each with the reason it is rejected.

1. **"The synthesis agent needs instructions to spot missing coverage."** It is tempting because the synthesis agent sees all the findings. The exam rejects it as the root cause when the coordinator's plan was narrow: the subagents did what they were assigned, and the fault is the assignment. Fix the plan.
2. **"Make the search agent's queries broader, or loosen the analysis agent's filter."** It is tempting because the agents are visible in the logs. The exam rejects both for the same reason: each agent worked correctly within its scope, so widening it does not reach the scope that was never assigned.
3. **"Give the synthesis agent every search tool so that it never has to ask."** It is tempting because it removes the round trip. The exam rejects it: it gives one agent the tools of another, and the answer to a frequent small need is one narrow tool.

## Quiz

1. A report on AI in the creative industries covers only visual arts, with nothing on music, writing or film. Every subagent finished its task, and the log shows three subtasks: digital art, graphic design and photography. Which change fixes the cause?
   - **a**: Loosen the relevance filter of the analysis agent so that fewer documents are dropped
   - **b**: Instruct the synthesis agent to look for missing coverage in everything it receives
   - **c**: Widen the queries of the search agent so that it returns more kinds of source
   - **d**: Check the coordinator's plan against the question's scopes, ahead of any delegation

2. A run ends with a report marked complete. The film search timed out, nobody retried it, and the report holds nothing on film. Where is the defect?
   - **a**: The status is computed from errors seen and not from scopes covered
   - **b**: The report agent should have written a longer summary of the other scopes
   - **c**: The search agent should have returned an empty list instead of an error
   - **d**: The timeout was set too short, so the search never had time to finish

3. Each of four subagents returns about forty pages of raw text, and the coordinator's context is nearly full before synthesis starts. What does the research article suggest?
   - **a**: Move the synthesis into the coordinator so that nothing has to be sent to it
   - **b**: Ask the coordinator to summarise each result in its own words as soon as it arrives
   - **c**: Have each one store its work outside the conversation and send back a reference
   - **d**: Cap the length of every search result so that no single one can be long

<details>
<summary>Answer key</summary>

1. **d**. The subagents did what they were assigned, so the plan is what to check, and it can be checked against the question. *b* is ruled out because it is a late substitute: "A downstream agent that is asked to notice gaps is a late and unreliable substitute for a check that costs one comparison." *c* is ruled out because the search agent worked within its scope: "each agent worked correctly within its scope, so widening it does not reach the scope that was never assigned". *a* is ruled out for the same reason: "the subagents did what they were assigned, and the fault is the assignment".
2. **a**. A status must come from coverage. *b* is ruled out because more prose does not repair the claim: "A run is complete when every scope is covered." *c* is ruled out because an empty success hides the gap: "A failure with no context, or a failure turned into a success". *d* is ruled out because the defect is in what the report claims and not in how long the search waited: "It is partial otherwise, whatever the reason, and the report says which scopes are missing and why."
3. **c**. Work stored outside the conversation travels as a reference. *b* is ruled out because the coordinator would still receive all the text first: "Large results passed through the hub". *a* is ruled out because it puts the synthesis in the context that is already full: "The coordinator owns everything that is about the whole question". *d* is ruled out because the article's remedy is the reference and not a cap: "Subagents call tools to store their work in external systems, then pass lightweight references back to the coordinator."

</details>

Adapted from the sample questions of the Claude Certified Architect, Foundations exam guide, version 1.0 (Anthropic), with credit; the questions are Anthropic's. The first question follows the guide's sample question on a report that covers only part of a topic, rewritten here.
