# The gateway, budgets and callers that cannot wait

**Level:** Architect Professional · **Module 84:** Cost and capacity engineering · **Page 2 of 2**
**Exams:** P2, P4

**After this page you can** say what one internal gateway for model traffic centralises and what it does not, route a request to a model by task and degrade it as a budget runs low, admit or block a request against a budget with exact edges, show each team what it spent with every token kind priced and the rounding done once, and give a caller with a hard latency limit accept-and-poll instead of a call that will time out.

Checked on 2026-10-04 against the Claude API documentation pages "Rate limits", "Pricing" and "Message Batches", and the Claude Certified Architect, Professional exam guide v1.0 (July 2026), domains 2 and 4. This page has no new example: the practice is the example, and it is graded in Python, TypeScript, Java and Kotlin with no model call. Its prices and numbers are test data, whole numbers in cents, tokens and percent, so that every language gives the same figures.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* the Professional guide expects an architect to control cost and capacity across many teams and to protect callers with hard constraints, so the exam asks which single control point does what, and what to do for a caller that cannot wait. *What the current documentation says (checked 2026-10-04):* limits and spend are set per organisation and per workspace, the Batch API bills at half price for work that can wait, and the usage fields of each response report input, cache-write, cache-read and output tokens. *How to read both:* the exam keys the answer that puts credentials, routing, budgets and the cost view in one gateway, and notes that a gateway centralises control and does not by itself cut the token bill or guarantee lower latency. The savings come from the levers (caching, model choice, batching), which the gateway makes enforceable.

## Why it matters

Four product teams each hold their own API key. One team's key leaks in a log, another team pins the most expensive model for a task that a cheap one handles, a third team's batch job spends the month's budget in a weekend, and finance asks who spent what and no one can say. None of this is a model problem. It is the absence of a single place where requests pass, so a single place is built: one internal gateway that holds the credentials, decides the model, checks the budget, records the spend and chooses how the answer is delivered.

## The idea

### What a gateway centralises, and what it does not

| A gateway does | A gateway does not |
|---|---|
| Hold the credentials, so teams never see the key | Reduce the tokens a prompt uses |
| Route each task to a model by one table | Make a model faster |
| Enforce budgets, per team and per project | Guarantee a lower bill by itself |
| Record usage once, so cost is shown back to each team | Replace caching, model choice or batching |
| Apply limits and retries in one place | Remove the need to evaluate a model for a task |

The distinction is the exam's. A gateway is a control point, and what it enforces is what saves money. A gateway that routes everything to the largest model is a well-organised way to overspend.

### Four decisions in code

The practice of this module is the four decisions of a gateway.

1. **Routing.** A request names a task and may pin a model. The gateway uses the pin only when the policy allows that model, because a pin is a request and not a right. Otherwise it uses the route for the task, and the default for a task the table does not know. When the team is near its budget the model is replaced by a cheaper one that the policy names, and when it has no cheaper entry the model is kept. A blocked team gets no model: the request is refused.
2. **Admission.** Given what a team has spent, its budget and the estimate for one more request, the gateway returns `allow`, `warn` or `block`. The edges are exact. A budget that is not positive blocks. A request that would bring the spend to more than the budget blocks, and one that brings it exactly to the budget does not. Reaching 80 percent warns, and exactly 80 percent warns. So a team is warned, then degraded, then stopped, in that order, and the first two steps leave its work running.
3. **Showback.** Each team sees what it spent, computed from usage rows of input, cache-read and output tokens at the price of the row's model; this example's rows leave out cache writes, which a full bill prices too (page 1). Every token kind has its own price (cache reads are cheap, output is dear), so a bill that ignores either is wrong. The total is divided by a million and rounded **once**, at the end: rounding each row first gains or loses cents, so two rows of 0.4 cent are 1 cent together and 0 apart. The figure shown to a team is a whole number of cents. A row whose model has no price is refused, because a gateway never guesses a price.
4. **Delivery.** Some callers cannot wait: a legacy system that times out after two seconds, a screen that cannot hold a connection open. The gateway compares the slow case (the 95th-percentile time plus a safety margin) with the caller's timeout. If it fits, the call is synchronous. If it does not, the gateway uses **accept-and-poll**: it takes the request, returns an identifier at once, does the work behind the scenes and lets the caller fetch the result by polling or receive it by callback. The constraint and the workload then coexist without cutting the work short.

### Why accept-and-poll beats the alternatives

Truncating the reasoning to fit two seconds sacrifices the task. Raising the legacy system's timeout is often not possible and moves the problem into a system nobody owns. A faster model may not exist for the quality needed. Decoupling submission from delivery is the design that respects both the limit and the work. It also pairs with the Batch API for work that can wait longer still, and with the retry and key discipline of module 81, since the identifier given to the caller is the key for the request.

### The practice: the decisions of an internal gateway

The practice is in [`exercises/84-cost-and-capacity-engineering`](../../exercises/84-cost-and-capacity-engineering/unit-01/practice-1/statement.md). You write `route`, `admit`, `showback` and `delivery`. It is graded in Python, TypeScript, Java and Kotlin, and the statement lists seven cases, each saying what you should see when it works.

## Traps

These are the wrong answers the exam's options for this domain offer, each with the reason it is rejected.

1. **"Put a gateway in front of the API; the token bill will fall."** It is tempting because the gateway is the visible new component. The exam rejects it: a gateway centralises authentication, routing, budgets and logging, and the savings come from caching, the choice of model and batching that it enforces.
2. **"Raise the legacy system's timeout, or cut the model's reasoning to fit two seconds."** It is tempting because it removes the conflict at once. The exam rejects it: the timeout is often not yours to change, and truncating the work sacrifices the task, so accept the request, return an identifier and deliver the result by polling or a callback.
3. **"Give each team its own key so that teams are independent."** It is tempting because it avoids a shared dependency. The exam rejects it: separate keys leave no single place to enforce budgets, route models or show spend, and a leaked key reaches everything its team can call.

## Quiz

1. Scenario: Oriel Telecom's legacy billing system can call only synchronous endpoints and times out after two seconds, but the task needs long reasoning that takes about twenty. What design fits best?
   - **a**: Cut the reasoning short, so that the answer arrives before the legacy timeout expires
   - **b**: Ask the platform team to raise the legacy timeout to thirty seconds for this one job
   - **c**: Accept the job, hand back a ticket number at once, and deliver the outcome afterwards
   - **d**: Move the task to the smallest available model, which answers quickly enough to fit

2. Scenario: Pylon Software's four teams each hold a separate key, and finance cannot say which team spent what. The platform team proposes an internal gateway. What will the gateway achieve by itself?
   - **a**: A smaller token bill for each team, since every request now passes one central point
   - **b**: One place for credentials, routes, budgets and usage records, since all traffic passes it
   - **c**: Faster answers on every call, since the gateway adds capacity of its own to the model
   - **d**: Better answers on every call, since the gateway rewrites the prompts it forwards

<details>
<summary>Answer key</summary>

1. **c**. Accept-and-poll decouples submission from delivery, so the limit and the work coexist. *a* is ruled out because "truncating the work sacrifices the task". *b* is ruled out because the timeout "is often not yours to change", and raising it moves the problem into a system nobody owns. *d* is ruled out because "A faster model may not exist for the quality needed".
2. **b**. A gateway is a control point for credentials, routing, budgets and usage records. *a* is ruled out because the savings "come from caching, the choice of model and batching" and not from the gateway. *c* is ruled out because a gateway does not "Make a model faster" and adds no capacity of its own. *d* is ruled out because it does not "Remove the need to evaluate a model for a task", and it does not rewrite prompts.

</details>

## Module quiz

This quiz covers both pages of the module.

1. Scenario: Zephyr Games' team pins its requests to the most expensive model, which the gateway policy does not list as allowed. What does the gateway do?
   - **a**: Honours the pin, since the team chose that model deliberately
   - **b**: Blocks the team for breaking the policy until it has been reviewed
   - **c**: Uses the pin once and warns the platform team afterwards
   - **d**: Routes by the table entry for the task, as that choice is a wish

2. Scenario: Tallow Insurance's gateway sees a team at exactly 80 percent of its budget, and the next request is estimated to stay under 100 percent. What does admission return?
   - **a**: Allow, since the team has not yet passed its budget and nothing is wrong at this point
   - **b**: Warn, since the threshold is reached even though the spend remains inside the limit
   - **c**: Block, because a team near its budget should not be given any more requests at all
   - **d**: Warn only if the same team has also overspent in a previous month of the same year

3. Scenario: Umber Retail's showback holds two usage lines for one team, each worth 0.4 of a cent. What amount in whole cents should be shown?
   - **a**: 0, because each line rounds down to nothing when it is taken on its own account
   - **b**: 2, because every line is rounded up so that none of the usage goes unbilled
   - **c**: 1, because rounding happens once after they are summed
   - **d**: 0.8, since fractions of a cent are kept for each team on the final screen

<details>
<summary>Answer key</summary>

1. **d**. A pin is honoured only when the policy allows it, and otherwise the table decides. *a* is ruled out because "The gateway uses the pin only when the policy allows that model". *b* is ruled out because the gateway does not punish: "Otherwise it uses the route for the task, and the default for a task the table does not know". *c* is ruled out because there is no allowance for one exception, and the page says "because a pin is a request and not a right".
2. **b**. Admission warns at 80 percent, and exactly 80 percent warns. *a* is ruled out because "Reaching 80 percent warns, and exactly 80 percent warns". *c* is ruled out because a request that stays within budget does not block: "one that brings it exactly to the budget does not". *d* is ruled out because admission takes "Given what a team has spent, its budget and the estimate for one more request", and not earlier months.
3. **c**. The total is divided by a million and rounded once, so two rows of 0.4 cent are 1 cent. *a* is ruled out because "rounding each row first gains or loses cents". *b* is ruled out because rounding up each row gives 2 and is the loss of precision that "two rows of 0.4 cent are 1 cent together and 0 apart" shows. *d* is ruled out because "The figure shown to a team is a whole number of cents".

</details>

Adapted from CLAUDE-CERTIFICATIONS by Amey Thakur (MIT License).
