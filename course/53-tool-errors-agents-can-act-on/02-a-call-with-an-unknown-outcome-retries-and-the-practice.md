# A call with an unknown outcome, safe retries and the practice

**Level:** Architect · **Module 53:** Tool errors that agents can act on · **Page 2 of 2**
**Exams:** A2.2; S1, S3

**After this page you can** say what to report when a write timed out and nobody knows whether it happened, decide when a retry is safe (a read, a keyed call, a call that checked first), bound a retry loop and honour a wait the service asks for, recover locally inside a tool or subagent and pass up only what cannot be recovered, and write the module's practice: a structured error, a retry loop that knows the difference, and the next action for each kind of result.

Checked on 2026-10-03 against the Claude Code documentation pages "Work with sessions" (what a resumed session does with a call that was still running) and the Agent SDK page "Custom tools" (tool annotations), the Claude API page "Handle tool calls", the MCP specification Tools page (version 2026-07-28), and the exam guide for the Architect Foundations exam (version 1.0, July 2026): Python `claude-agent-sdk` 0.2.163 and TypeScript `@anthropic-ai/claude-agent-sdk` 0.3.287. The practice is offline in Python, TypeScript, Java and Kotlin: the tools are functions that the tests script. None of the pages read describes an idempotency key for tools; the key, the retry limits and the result shapes of this page are the course's own design, built on the documented `is_error` flag and on module 15's account of retries and idempotency keys, and the statement says so. This page deepens module 15 (retries and keys at the HTTP level) and module 26 (errors are results), and it does not repeat them.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* the agent should recover locally where it can (a subagent handles a transient failure itself), propagate to the coordinator only the errors that cannot be resolved locally, together with partial results and what was attempted, and tell an access failure, which needs a retry decision, from a valid empty result; and, in its sample questions, a subagent whose search timed out should hand the coordinator the failure type, the attempted query, partial results and alternatives, not a generic status and not an empty success. The guide has no item about a call whose outcome is unknown. *What the current product does (checked 2026-10-03):* when a session is resumed after a crash, the product itself applies the rule this page teaches: a tool that was still running "doesn't finish or run again when you resume", and "Claude sees the call marked as cut off before its result was recorded and is told to check whether it took effect before running it again". Tool annotations such as `idempotentHint` ("Repeated calls with the same arguments have no additional effect") are "Informational only", and "Annotations are metadata, not enforcement". On the exam, choose structured context to the coordinator over a generic or empty reply; in your own code, add the case the guide leaves out: a write that may have happened is reported as unknown and checked before it is repeated.

## Why it matters

A refund is sent to the billing service and the connection times out. The request may never have arrived. It may have arrived and been processed, with only the answer lost. A tool that treats the timeout like a refusal and tries again will, in the second case, refund the customer twice. A tool that gives up and says "failed" will, in the same case, make the agent tell the customer that nothing happened while the money has moved. Both are confident answers to a question that has no answer yet, and the exam's scenarios (a support agent with refund tools, a research system with a search subagent) are built around the difference between a failure that is known and one that is not.

## The idea

### Three outcomes, not two

A call to a tool that changes something has three possible outcomes, and only two of them are the ones people plan for.

| Outcome | What the tool knows | What it reports |
|---|---|---|
| It worked | The service answered with success | A result |
| It failed before it had any effect | The service refused, or was unreachable before the request was accepted | A `transient`, `validation`, `permission` or `business` error |
| It may or may not have worked | The request left, and no answer came back (a timeout, a dropped connection) | `outcome_unknown` |

The third row is the one that a loop must not fold into the second. A timeout after sending is not a failure of the effect; it is the absence of news. The honest reply says what is known: "No answer from the refund service. The call may have taken effect: check the current state before trying again." That sentence is what the practice returns, and it tells the agent its next move: look first.

### When a retry is safe

A retry is safe when repeating the call cannot do harm. Four cases qualify, and every other case is checked first.

- **A read.** Repeating a read changes nothing, so a timed out lookup can simply be sent again. The practice marks such a call `read_only`.
- **A call that is idempotent by nature**, such as setting a field to a value: doing it twice leaves the same state.
- **A call that carries an idempotency key** that the service honours. The tool sends the same key on every attempt, and the service performs the action once however often it receives the key (module 15 gives the HTTP-level account). The practice adds the key to a copy of the arguments on every attempt and never writes it into the caller's own map.
- **A call after a check.** The agent reads the current state, finds that the refund is not there, and then repeats it.

Everything else is unsafe: a refund sent twice refunds twice, an email that left the outbox cannot be called back, and a ticket created twice is two tickets. For those, the timeout becomes `outcome_unknown`, the tool does not call again, and the next action is `verify_first`.

Annotations do not decide this. The Agent SDK table lists `readOnlyHint` and `idempotentHint`, and says that the second one is "Informational only" and that "A tool marked `readOnlyHint: true` can still write to disk if that's what the handler does." A hint is the tool author's claim; your retry policy has to rest on what the tool actually does, which module 52 returns to when it asks how far to trust annotations.

### Check, then decide

What the agent does with `outcome_unknown` is ordinary: it calls a read tool that can tell, such as a lookup of the refunds on the order. If the refund is there, the work is done and the agent reports success. If it is not, the agent can repeat the call (with a key, if there is one) or escalate. The product's own behaviour after a crash is the same shape: the call is marked as cut off, and Claude "is told to check whether it took effect before running it again". The rule travels: wherever a write may have happened, the next step is a read.

### Bounded retries, waits and the service's own advice

Retries for a `transient` failure follow three rules that the tests check one by one.

1. **A bound.** After a fixed number of retries the tool gives up with a transient error that says how many attempts it made ("Gave up after 3 attempts"). A loop with no bound turns a service outage into a run that never ends.
2. **A growing wait.** The wait doubles from a base: 100, 200, 400 milliseconds. A constant wait keeps the pressure on a service that is already struggling (module 15).
3. **The service's word first.** When the failure carries a wait (a rate limit with a `retry-after`), that value replaces the computed one for that attempt.

The practice takes the wait as a function that you call (`sleep(ms)`), so the tests see the numbers and nothing sleeps for real. The exhausted-retries error keeps the arguments of the call, so that whoever receives it knows what was attempted.

### Recover locally, propagate what is left

The guide's second skill is about where recovery happens. A tool, or a subagent, that meets a transient failure retries it itself; the coordinator does not need to hear about a busy service that recovered. What it does need to hear about is what could not be recovered, in a form it can use: the category, what was attempted, and whatever partial results exist. The result of the practice's `run_tool` does that on failure: category, retry flag, message, number of attempts and the arguments. Module 46 showed the coordinator's side (a failing subagent must not stop the others), and the later module on error propagation deepens the whole path; this module's contribution is that the reply the tool builds is already what the coordinator needs. The two opposite mistakes in the guide's sample are the generic status that hides the context and the empty reply marked as a success that hides the failure.

A validation failure follows the same principle on a smaller scale. When a call is invalid, the documentation notes that Claude "will retry 2-3 times with corrections before apologizing to the user", and it advises that "your best bet during development is to try the request again with more-detailed `description` values". A message that names the field and gives an example value ("amount must be a positive whole number, for example 40") turns those retries into one correction.

### The practice

The practice is `exercises/53-tool-errors-agents-can-act-on/unit-01/practice-1/statement.md`, in Python, TypeScript, Java and Kotlin, and it is written as three steps, each with the reason the exam cares and what you should see when it works. You write `make_error` and `to_tool_result` (the structured result and the block for the API), `run_tool` (bounded retries for transient failures only, the unknown outcome, the key) and `next_action` (the table of page 1 and the cases above).

Eight cases grade it: the main path, the refusal of generic messages and unknown categories, the retry rules, the bound and the service's own wait, the empty result, the timed out write, the next action, and the bug in the tool. The starter fails all eight, the reference passes them, and each of fourteen planted wrong solutions per language fails on an assertion of the case it breaks: a result without the error flag, a business failure marked as retryable, a generic message accepted, an unknown category accepted, a validation failure retried, a constant wait, one retry too many, a service's wait ignored, an empty result treated as a failure, a timed out write retried without a key, a key left out of the retries, a key written into the caller's map, a permission error answered with another try, and a bug in the tool marked as retryable.

## Traps

These are the wrong answers that the exam's options for this task statement offer, each with the reason it is rejected.

1. **"A timeout is just a failure, so retry the refund."** It is tempting because a timeout looks like every other transient error. The exam rejects it: the request may have been processed, and a refund sent twice refunds twice. Report an unknown outcome and check the state, or retry with a key.
2. **"Give up quietly and tell the customer it failed."** It is tempting because it avoids a duplicate. The exam rejects it: the money may have moved, and the agent will say the opposite of what happened. Say that the outcome is not known.
3. **"The tool's annotation says it is idempotent, so retry freely."** It is tempting because the hint is part of the tool's definition. The exam rejects it: annotations are hints written by the tool's author, not a guarantee. Base the retry on what the tool does, or on a key.
4. **"Let the tool retry until it succeeds."** It is tempting because transient failures pass. The exam rejects it: with no bound an outage becomes a run that never ends. Retry a bounded number of times and return an error that says how many.
5. **"Send every failure straight to the coordinator."** It is tempting because the coordinator sees everything. The exam rejects it: a busy service that recovered on the second try is not news. Recover locally and pass up what cannot be resolved, with what was attempted and any partial results.

## Quiz

1. After a timeout, an agent's wrapper sends the same call again at once. Which call may it repeat without any check first?
   - **a**: Issuing a refund that carries no idempotency key at all
   - **b**: Emailing the customer a confirmation of the refund
   - **c**: Creating a ticket without any key attached to it
   - **d**: Looking up the order record by its order number

2. A refund call times out, and the tool reports that its outcome is unknown. What should the agent do next?
   - **a**: Read the records kept for the order, then decide whether any work remains
   - **b**: Send the payment again under a fresh key straight away so that it surely goes through
   - **c**: Tell the customer that the attempt failed and invite them to begin a new request
   - **d**: Mark the case as resolved, because the service usually processes whatever it receives

<details>
<summary>Answer key</summary>

1. **d**. A read changes nothing when it is repeated. *a* is ruled out because "a refund sent twice refunds twice". *b* is ruled out because "an email that left the outbox cannot be called back". *c* is ruled out because "a ticket created twice is two tickets".
2. **a**. A write that may have happened is followed by a read. *b* is ruled out because a new key defeats the protection that a key gives: "The tool sends the same key on every attempt". *c* is ruled out because the agent would state something unknown as a fact: "the money may have moved, and the agent will say the opposite of what happened". *d* is ruled out because a guess is not news: "it is the absence of news".

</details>

## Module quiz

This quiz covers both pages of the module.

1. Scenario S1, a customer support resolution agent. The agent handles returns, billing disputes and account problems with tools that verify identity, look up orders and issue refunds, and it escalates to a person when it cannot resolve a case. The billing system refuses a refund because the amount is above the customer's refundable balance. What should the refund tool return?
   - **a**: A failure marked as transient, with a wait of a few seconds before the next try
   - **b**: A flagged failure not worth repeating, with a sentence to pass on to the shopper
   - **c**: A success whose text mentions the balance, so that the model can decide freely
   - **d**: A failure marked as a validation problem, asking for a smaller number to be tried

2. Scenario S3, a multi-agent research system. A coordinator delegates to a web search subagent, a document analysis subagent and a synthesis subagent, and it produces a cited report. The web search subagent retries a timeout twice and the service stays down. What should it hand the coordinator?
   - **a**: A short note that the lookup service was unavailable, sent once every retry is spent
   - **b**: An empty set of results marked as a success, so that the run is never interrupted
   - **c**: The kind of failure, the query that ran, any partial findings and other routes to try
   - **d**: The timeout itself, passed up unhandled so that the whole research workflow ends

3. Scenario S1, a customer support resolution agent. The agent handles returns, billing disputes and account problems with tools that verify identity, look up orders and issue refunds, and it escalates to a person when it cannot resolve a case. A wrapper retries any failed tool call twice, and customers are being refunded twice after timeouts. Which change to the wrapper is best?
   - **a**: Retry nothing at all, so that every failure goes to a person for review
   - **b**: Retry five times instead of twice, so that fewer timeouts reach the customer
   - **c**: Retry after a longer fixed pause, so that a late answer arrives before the next call
   - **d**: Repeat only what is known to be harmless, and report a lost write as unsettled

<details>
<summary>Answer key</summary>

1. **b**. A business rule is understood and refused, and the shopper can be told why. *a* is ruled out because a rule answers the same way every time: "a validation, permission or business failure gives the same answer every time". *c* is ruled out because the refusal must come back flagged: "a business-rule violation comes back with retriable: false". *d* is ruled out because the amount was valid and a rule refused it: "The request is understood and refused by a rule".
2. **c**. The coordinator needs the context to choose a recovery. *a* is ruled out because a generic status hides what the coordinator needs: "The two opposite mistakes in the guide's sample are the generic status that hides the context". *b* is ruled out because "an empty list marked as a success tells the agent that the search found nothing". *d* is ruled out because the other subagents can go on: "a failing subagent must not stop the others".
3. **d**. Only a call known to be harmless is safe to repeat. *a* is ruled out because many failures do pass on their own: "most failures in a network are transient". *b* is ruled out because more attempts multiply the duplicates: "a refund sent twice refunds twice". *c* is ruled out because a pause does not tell whether the first call worked: "A timeout after sending is not a failure of the effect; it is the absence of news".

</details>

Adapted from the sample questions of the Claude Certified Architect, Foundations exam guide, version 1.0 (Anthropic), with credit; the questions are Anthropic's. The second module question follows the guide's sample question on a search subagent that times out, rewritten here.
