# Review, iteration, failures and the practice

**Level:** Architect · **Module 46:** Coordinator and subagents · **Page 2 of 2**
**Exams:** A1.2; S3

**After this page you can** describe the review-and-refine cycle of a coordinator and bound it, keep a failed subagent from looking like a finding, hand large outputs between agents by reference, say how the Agent SDK's budget behaves when subagents spend, and write the module's practice: a coordinator with isolated subagents.

Checked on 2026-10-03 against Anthropic's engineering article on its multi-agent research system and the Claude Code documentation pages "Subagents in the SDK" and "How the agent loop works". The practice is offline in Python, TypeScript, Java and Kotlin: the coordinator's four model roles are functions that the tests script, so no model and no network are used. The Agent SDK itself is Python and TypeScript only; the coordinator of the practice is the one you write in ordinary code when you build the team on the Messages API, as the example of page 1 does.

## Why it matters

The first round of a research team rarely answers the question. Something is thin, something was missed, one subagent failed. What the coordinator does next decides whether the user gets a better answer or a larger bill. The exam asks about this stage as a design: who finds the gap, who closes it, when does it stop, and what is the user told about what is still missing.

## The idea

### Review, then refine, and stop

The research article describes the cycle as the lead's job: "The LeadResearcher synthesizes these results and decides whether more research is needed—if so, it can create additional subagents or refine its strategy." Three rules make that cycle safe in code.

- **Only the review finds a gap, and only a subagent closes it.** A gap is a statement of what is missing ("2023 baseline for chip supply"). It becomes a new brief, and the new finding is added to the old ones. The synthesis step is never asked to fill a thin part from its own knowledge: a gap is found by the review and closed by a subagent, never invented by the synthesis.
- **Send out the gaps and nothing else.** A first-round subagent that did its job has a finding. Sending every brief out again in the next round doubles the cost and adds repeats, which is the mistake that the practice's case `e5` plants.
- **Bound the rounds, and report what is left.** A reviewer can always find one more thing. The practice caps the rounds, calls the reviewer once more after the last round, and returns the gaps that remain. The status is `complete` only when no gap remains and nothing failed; otherwise it is `partial`, and the caller can show the user the list of what is missing. Each round is paid for: "multi-agent systems require tasks where the value of the task is high enough to pay for the increased performance."

The article's search advice is the same shape: "Start wide, then narrow down. Search strategy should mirror expert human research: explore the landscape before drilling into specifics." A first round of broad briefs and a second round of narrow gap briefs follow it.

### Failures are not findings

A subagent can fail by throwing, by returning nothing, or by being cut short. The documentation is explicit about the last one: "An API error that ends the subagent early, such as a rate limit, is never delivered as its result." The coordinator therefore sees a failure as a failure, with the exception's message or an empty report, and not as text that can go into the synthesis. Three rules follow.

1. A failure is recorded against its scope (`failed`), and the other subagents still run.
2. A failure is never turned into a finding. Putting "API error: rate limited" into the pile that the synthesizer reads produces an answer that quotes the error as a fact.
3. A run with no findings at all is `failed`: nothing is reviewed or synthesized, because a confident answer with nothing under it is the worst outcome.

When some subagents failed, the answer can still be useful, and the honest status is `partial`, because a partial answer looks like a full one unless the status says otherwise. The user-facing text should say which part is missing. Whether to retry a failed scope is the reviewer's decision: it can list the scope as a gap, and the cap on rounds keeps the retry bounded.

### Hand large outputs over by reference

A coordinator that receives every subagent's full output through its own context pays for each token twice, once when the subagent writes it and once when the coordinator reads it, and loses detail at every summary. The research article recommends the other route: "Rather than requiring subagents to communicate everything through the lead agent, implement artifact systems where specialized agents can create outputs that persist independently." Its reason: "This prevents information loss during multi-stage processing and reduces token overhead from copying large outputs through conversation history." A subagent writes its output to a store and returns a short reference and a summary; the coordinator, or a later subagent, reads the part it needs. The same article has the lead save its plan outside its context too: it begins "by thinking through the approach and saving its plan to Memory to persist the context, since if the context window exceeds 200,000 tokens it will be truncated." The plan, the references and the status are state that survives a long run; the full texts need not be in the coordinator's window.

### What the budget does when subagents spend

Every subagent makes its own API requests. In the Agent SDK "the budget cap covers subagents: their spend counts toward the total." Once the cap is reached, "spawning another subagent fails with `Budget limit reached`" and Claude Code stops any background subagents still running, and the query ends with the `error_max_budget_usd` subtype (module 35). So a coordinator written on the SDK sees a refused spawn as a normal outcome of a run that spent its budget, and should treat it as a failure of that scope, not as a bug. A team without any ceiling is the usual way a research system gets expensive: set the depth, the concurrency and the budget before a model-driven lead is trusted with the decision.

### The practice

The practice is `exercises/46-coordinator-and-subagents/unit-01/practice-1/statement.md`, in Python, TypeScript, Java and Kotlin. You write `coordinate(planner, subagent, reviewer, synthesizer, question, max_agents, max_rounds)`: a planner that may say "no team" and answer directly; a plan cleaned of empty briefs and duplicate scopes and capped; one call per subagent with exactly its brief; failures recorded and not stopping the others; a run with no findings that neither reviews nor synthesizes; a review that sends only the gaps out as follow-up briefs; and rounds that are capped with the remaining gaps reported.

The tests script the four roles and grade seven cases: the main hub-and-spoke run, the direct answer, isolation, plan cleaning, failure handling, refinement and the round cap. The starter fails all seven, the reference passes them, and each of eight planted wrong solutions per language fails on an assertion of the case it breaks: a team built for a question the planner answered, a brief that carries earlier findings, two subagents on one scope, an empty brief sent, an error reported as a finding, an answer synthesized from nothing, a refinement that sends everyone out again, and one round too many.

## Traps

1. **Sending everyone out again in each round.** The first-round findings are already in hand. Send out the gaps, as new briefs, and keep the rest.
2. **Letting a failure into the synthesis.** An error message is not evidence. Record it against its scope and say what is missing.
3. **A review with no end.** A reviewer finds one more gap every time. Cap the rounds, return the gaps that are left and let the status say `partial`.
4. **Passing big outputs through the lead.** The lead pays for them twice and loses detail in each summary. Store them, and pass a reference.

## Quiz

3. A subagent's call is cut short by a rate limit, and the coordinator's synthesis quotes the line "API error: rate limited" as if it were a finding. What should the coordinator do instead?
   - **a**: Keep the error line among the findings so that the user sees what happened
   - **b**: Record the scope as failed and tell the user what is missing
   - **c**: Instruct the synthesizer to skip any sentence that looks like an error
   - **d**: Send the whole team out again until every subagent succeeds

4. After its first wave of subagents the lead finds that two of five sections are weak. It sends every assignment out again, and the bill doubles with little new content. Which change fits best?
   - **a**: Stop after the first wave and report the whole answer as complete and final
   - **b**: Start more subagents in the first wave so that fewer gaps appear afterwards
   - **c**: Let the synthesis step fill the weak sections from what the model already knows
   - **d**: Dispatch follow-ups for the flagged gaps alone, inside a fixed number of rounds

<details>
<summary>Answer key</summary>

3. **b**. A failure belongs to its scope, and the user is owed a list of what is missing. *a* is ruled out because the documentation says of such an error that it "is never delivered as its result", so it is not evidence. *c* is ruled out because the problem is then hidden: "A partial answer looks like a full one". *d* is ruled out because it is the other coordinator mistake: "Sending every brief out again in the next round doubles the cost and adds repeats".
4. **d**. The review names the gaps, only they are sent out again, and the rounds are capped. *b* is ruled out because every wave is paid for: "multi-agent systems require tasks where the value of the task is high enough to pay for the increased performance." *c* is ruled out because "a gap is found by the review and closed by a subagent, never invented by the synthesis." *a* is ruled out because the answer is complete only when "no gap remains and nothing failed".

</details>

## Module quiz

This quiz covers both pages of the module.

1. Two of the three subagents of a research team come back with almost the same findings on the same period. Which cause fits best?
   - **a**: The coordinator ran too few rounds of review after the first answers
   - **b**: The context of one subagent leaked into another during the run
   - **c**: The briefs did not divide the scope or say what each part must leave out
   - **d**: The subagents ran on a smaller model than the coordinator uses

2. A coordinator starts the same five subagents for every query, including "what is the boiling point of water". Which redesign fits best?
   - **a**: Keep the five subagents but lower the token limit of each one
   - **b**: Scale the team to the request, down to a solo worker for a plain fact
   - **c**: Let the five subagents share one context so that they finish sooner
   - **d**: Start the team only when the user asks for a long report

3. Specialist agents hand forty-page drafts up through the coordinator, and cost rises while details get lost in the summaries. Which architecture change fits best?
   - **a**: Cap the turns of each specialist so that its drafts stay short and focused
   - **b**: Ask the coordinator to condense every draft before the next specialist starts work
   - **c**: Hand each specialist the coordinator's whole conversation so that drafts can be rebuilt
   - **d**: Keep each output in an outside store and forward only a short pointer

<details>
<summary>Answer key</summary>

1. **c**. Overlap comes from the brief, because the subagents cannot see each other. *b* is ruled out because nothing crosses between subagents: "A subagent does not know the user's original question, the plan, the other subagents". *a* is ruled out because review cannot undo duplicate work already paid for, and the page puts the cause in the briefs: "Without detailed task descriptions, agents duplicate work, leave gaps, or fail to find necessary information." *d* is ruled out because the model size is not the cause in the article's example: "one subagent explored the 2021 automotive chip crisis while 2 others duplicated work investigating current 2025 supply chains."
2. **b**. The effort should follow the question: "Simple fact-finding requires just 1 agent with 3-10 tool calls". *a* is ruled out because the cost of the team comes from running it: "agents typically use about 4× more tokens than chat interactions, and multi-agent systems use about 15× more tokens than chats". *c* is ruled out because a shared context is what a team does not have: "The only content you pass from parent to subagent is the Agent tool's prompt string". *d* is ruled out because a long report does not make a team worth its cost: "the value of the task is high enough to pay for the increased performance".
3. **d**. A reference keeps the output out of the lead's window and out of its summaries. *b* is ruled out because condensing loses detail: "This prevents information loss during multi-stage processing". *c* is ruled out because copying through conversation history is the cost: "reduces token overhead from copying large outputs through conversation history". *a* is ruled out because a cap on turns does not change where the outputs travel, and the lead still reads each one: "Rather than requiring subagents to communicate everything through the lead agent".

</details>
