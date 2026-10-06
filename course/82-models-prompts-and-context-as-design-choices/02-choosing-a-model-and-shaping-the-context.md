# Choosing a model and shaping the context

**Level:** Architect Professional · **Module 82:** Models, prompts and context as design choices · **Page 2 of 2**
**Exams:** P2

**After this page you can** pick a model for a workload from its tier, latency limit and price instead of by habit, state the rule that gives the cheapest model that meets the requirement, decide when two prompts can share a cached prefix, apply the prompting guide's habits (clear instructions, a reason with each rule, a few diverse examples, structure in tags) as design choices, and re-check a prompt rule against your own evaluation before moving it to another model.

Checked on 2026-10-04 against the Claude API documentation pages "Prompting best practices" and "Prompt caching", the models overview, and the Claude Certified Architect, Professional exam guide v1.0 (July 2026), domain 2. This page has no new example: the practice is the example, and it is graded in Python, TypeScript, Java and Kotlin with no model call. The model names in the practice are placeholders in test data, not a statement about any model's real tier, latency or price.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* domain 2 expects the architect to match a model to a workload by capability, latency and cost, and to treat prompt and context design as decisions that affect all three. *What the current documentation says (checked 2026-10-04):* the prompting guide lists model-specific pages for each current model and says that "where a technique names a specific model, treat it as measured on that model and re-check it against your own evals before applying it to another". The models overview lists each model's capabilities, so use it, not memory, for any figure. *How to read both:* the exam keys the answer that chooses the cheapest model that meets the stated requirement and measures a change before adopting it, and rejects the answer that picks the largest model for safety or copies a prompt between models unchanged.

## Why it matters

A team has one assistant and three workloads: a classifier that tags every incoming ticket within a second, a drafting task that writes replies to customers, and a weekly analysis of thousands of tickets. They use the largest model for all three because the demo was good. The classifier is slow and expensive for a job a smaller model does well, the drafting is fine, and the analysis could run overnight at a lower price. The choice was made once and for everything, and no one can say what it cost them. A model is a design choice per workload, with the numbers written down.

## The idea

### A model is chosen for a workload

Describe each workload with the facts that bound the choice and then pick from the list of candidate models. Three facts decide.

| Fact | Where it comes from | What it rules out |
|---|---|---|
| **Tier**: the capability the task needs | An evaluation of the task on candidate models (module 88) | A model below the tier fails the task |
| **Latency limit** | The caller: a person at a screen or a nightly job | A model slower than the limit |
| **Price** | The models overview and your own token counts (module 84) | Nothing alone; it ranks the models that remain |

The rule is short: among the models whose tier is at least the workload's and whose latency is within the limit, take the cheapest, and break a tie by name so that the choice is repeatable. When no model fits, the answer is none, and that is a finding: the requirement and the model list disagree, and someone has to relax the latency, accept a lower tier or change the design. Price comes last because it is the only fact that cannot make a model wrong, it can only rank models that are already right.

Two cautions. The tier a task needs is found by evaluating the task, not by reading a model's name, and it can change when a model is replaced. And the cheapest model per token is not always the cheapest per task: a weaker model that needs three attempts or a longer prompt can cost more than a stronger one that answers once (module 84).

### When two requests can share a prefix

Page 1 showed that a prefix is reused only when it is identical. The check is all or nothing: both prompts must have a breakpoint, the breakpoints must be equal, and every block up to it must match in name and text. If so, the reusable size is the tokens of those blocks. If any block differs, or either prompt has no breakpoint, nothing is reusable and the answer is zero, not "most of it". This is what the architect uses to decide whether two workloads should share one system prompt: if they can, one cache entry serves both, and if each adds its own text to the front, they cannot.

### Prompt design as architecture

The prompting guide is long, and four of its habits carry most of the weight for an architect.

- **Be clear and direct.** The guide's test: "Show your prompt to a colleague with minimal context on the task and ask them to follow it. If they'd be confused, Claude will be too."
- **Give the reason.** An instruction with its purpose generalises better than a bare rule: the guide's example is explaining that a reply will be read aloud, so that the model avoids ellipses by understanding why.
- **Use a few diverse examples.** The guide advises 3 to 5, relevant, diverse and wrapped in tags so that they are not confused with instructions.
- **Structure with tags.** Tags separate instructions, context, examples and variable input, which reduces misreading in a prompt that mixes them.

Each habit has a place in the module scheme of page 1: instructions and examples are static modules, so they come first and are cached; the variable input is a dynamic module, in its own tag, last. Putting structure in the assembler and not in each caller's string is how a team keeps ten prompts consistent.

### Re-check, do not assume

The guide is written per model, and it says to measure before moving a technique. An instruction that fixed a behaviour on one model can be unnecessary or harmful on another, and a model update can change what a prompt does. The design answer is an evaluation set that runs on every candidate model and every prompt change, with a recorded result, so that "it should work" is replaced by a number (modules 88 and 89).

### The practice: a prompt plan under a token budget

The practice is in [`exercises/82-models-prompts-and-context-as-design-choices`](../../exercises/82-models-prompts-and-context-as-design-choices/unit-01/practice-1/statement.md). You write `assemble` (static first, variables checked, a budget that drops the lowest priority first and refuses when only static modules remain, and a breakpoint when the prefix reaches the minimum), `choose_model` (the cheapest that meets tier and latency) and `reusable_prefix`. It is graded in Python, TypeScript, Java and Kotlin, and the statement lists eight cases, each saying what you should see when it works.

## Traps

These are the wrong answers the exam's options for this domain offer, each with the reason it is rejected.

1. **"Use the largest model for every workload, because quality is what the business cares about."** It is tempting because the biggest model looks safe. The exam rejects it: the requirement is a tier, a latency limit and a price, and the cheapest model that meets the first two is the right one for each workload.
2. **"Copy the prompt that worked on the current model; models in one family behave alike."** It is tempting because prompts are expensive to write. The exam rejects it: a technique is measured on a model, so it is re-checked against your own evaluation before it is applied to another.
3. **"Share one cached prompt between two workloads by adding each one's own text at the front."** It is tempting because the shared text looks like a saving. The exam rejects it: a prefix is reused only when every block up to the breakpoint is identical, so text added at the front gives zero reuse.

## Quiz

1. Scenario: Osprey Support needs ticket tagging answered within 800 ms. Candidates A and B are capable enough and fast enough, and B has the lower price of those two. Candidate C costs less still, but it takes 1,400 ms. Which candidate should the plan pick?
   - **a**: C, since the lowest price decides the matter once capability has been established
   - **b**: B, since it is the cheaper of the two models that meet the response-time limit
   - **c**: A, since the dearer of the two capable models is the safer pick for live tagging
   - **d**: A or B alike, since price cannot make a model wrong once both are within the limit

2. Scenario: Kestrel Labs needs a model for a task at tier 3 with a latency limit of 500 ms. The candidates are a tier 3 model at 2,500 ms and a tier 2 model at 300 ms. What should the plan record?
   - **a**: The tier 2 candidate, since it is the only one that stays within the limit
   - **b**: The tier 3 candidate, since capability matters more than speed in a first release to customers
   - **c**: Neither option, since each one fails one of the two stated requirements
   - **d**: The cheaper of the two, since price settles it when neither fits fully

<details>
<summary>Answer key</summary>

1. **b**. Latency rules C out, and price ranks the models that remain. *a* is ruled out because C fails the limit, and price "can only rank models that are already right". *c* is ruled out because the rule is to choose "among the models whose tier is at least the workload's and whose latency is within the limit" and take the cheapest, with no bonus for the dearer one. *d* is ruled out because price still ranks the two that remain, and the rule is to "take the cheapest, and break a tie by name so that the choice is repeatable".
2. **c**. No candidate meets both the tier and the latency, so the finding is a mismatch that someone must resolve by relaxing a requirement. *a* is ruled out because "A model below the tier fails the task". *b* is ruled out because the limit is part of the requirement too, and the candidate would fall under "A model slower than the limit". *d* is ruled out because price "can only rank models that are already right", and here neither candidate is right.

</details>

## Module quiz

This quiz covers both pages of the module.

1. Scenario: Plover Insurance's assembler receives a dynamic module with the placeholder {claim_id}, and the caller supplies no value for it. What should the assembler do?
   - **a**: Send the request, leaving the placeholder in its text
   - **b**: Stop with an error, sending no request to the model
   - **c**: Send it, replacing the placeholder with an empty string
   - **d**: Fill it, using the caller's value from its previous request

2. Scenario: Teal Logistics runs a ticket tagger that must answer within 300 ms at tier 1, and its prompt carries a long block of unchanging text. Which two decisions follow?
   - **a**: Take the largest model for safety, and put the constant passage first
   - **b**: Take the cheapest model of any tier, and place the constant passage in the final message
   - **c**: Take the model that was used last time, and cache the whole request as one piece
   - **d**: Take the cheapest model that is capable and fast, and put the constant passage first

3. Scenario: Avocet Travel builds a request from four pieces: the house rules, the fare policy, the traveller's current question and the traveller's earlier chat. Which arrangement fits best?
   - **a**: The conduct guide, pricing terms, latest query and prior messages all as static modules
   - **b**: The conduct guide and latest query as static modules, the pricing terms and prior messages as dynamic ones
   - **c**: The conduct guide and pricing terms as static modules, the latest query and prior messages as dynamic ones
   - **d**: The conduct guide, pricing terms and prior messages as static modules, the latest query as a dynamic one

<details>
<summary>Answer key</summary>

1. **b**. A missing value is a defect in the caller, so the assembler stops with an error and sends nothing. *a* is ruled out because "A request that goes out with `{customer}` in its text" is sent with a gap that nobody checks. *c* is ruled out because a blank is the same silent gap, and such a request "is a defect that no model will report". *d* is ruled out because an old value is only a guess, and "A missing variable is an error": "the assembler refuses it".
2. **d**. The model is chosen by tier and latency with price ranking the rest, and the stable text leads so that it can be cached. *a* is ruled out because the largest model is not needed when "the cheapest model that meets the first two is the right one for each workload". *b* is ruled out because a model chosen by price alone may be "A model slower than the limit", and the constant text belongs at the front, where "the stable content goes first and the variables go after the breakpoint". *c* is ruled out because "Two requests share a cache entry only when everything up to the breakpoint is identical", so the whole request, which includes the changing part, cannot be cached as one piece.
3. **c**. The rules and the policy are the same for every traveller, so they go first as static modules, and the question and the chat follow as dynamic ones, so that the stable prefix can be cached. *a* is ruled out because "Two requests share a cache entry only when everything up to the breakpoint is identical", and the question and the chat differ on every request, so no entry would ever match. *b* is ruled out because the question changes on every request, and "a changing value in the prefix breaks the cache on every request". *d* is ruled out because the earlier chat is the history, which grows with each turn, and a dynamic module is one that "changes with the customer, the question or the history".

</details>
