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

Describe each workload with the facts that bound the choice and then pick from the table of candidate models. The practice uses three.

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

1. Scenario: Osprey Support needs ticket tagging answered within 800 ms. Candidates A and B are capable enough and fast enough, and B costs less per output token. Candidate C costs less still, but it takes 1,400 ms. Which candidate should the plan pick?
   - **a**: C, since the lowest price decides the matter once capability has been established
   - **b**: B, the lowest-priced of the ones that satisfy every requirement
   - **c**: A, since the dearer option is the safer one for a first release to customers
   - **d**: Whichever the team used last, because consistency across workloads outweighs the saving

2. Scenario: Kestrel Labs needs a model for a task at tier 3 with a latency limit of 500 ms. The candidates are a tier 3 model at 2,500 ms and a tier 2 model at 300 ms. What should the plan record?
   - **a**: The tier 2 candidate, since it is the only one that stays within the limit
   - **b**: The tier 3 candidate, since capability matters more than speed in a first release to customers
   - **c**: That nothing qualifies, a mismatch that someone must resolve by relaxing a requirement
   - **d**: The cheaper of the two candidates, so that the budget of the project is protected

<details>
<summary>Answer key</summary>

1. **b**. Latency rules C out, and price ranks the models that remain. *a* is ruled out because C fails the limit, and price "can only rank models that are already right". *c* is ruled out because the rule is to choose "among the models whose tier is at least the workload's and whose latency is within the limit" and take the cheapest, with no bonus for the dearer one. *d* is ruled out because the choice is "a design choice per workload", not one habit applied to all.
2. **c**. No candidate meets both the tier and the latency, so the finding is the mismatch. *a* is ruled out because "A model below the tier fails the task". *b* is ruled out because the limit is part of the requirement too, and the candidate would fall under "A model slower than the limit". *d* is ruled out because "Price comes last because it is the only fact that cannot make a model wrong".

</details>

## Module quiz

This quiz covers both pages of the module.

1. Scenario: Plover Insurance's assembler receives a dynamic module with the placeholder {claim_id}, and the caller supplies no value for it. What should the assembler do?
   - **a**: Send the request with the placeholder left in the text as it stands
   - **b**: Refuse to build the request and name what is absent
   - **c**: Send it with the placeholder replaced by an empty string instead
   - **d**: Fill the gap with a guess drawn from the previous call of the same caller

2. Scenario: Teal Logistics runs a ticket tagger that must answer within 300 ms at tier 1, and its prompt carries a long block of unchanging text. Which two decisions follow?
   - **a**: Take the largest model for safety, and place the long text last in the request
   - **b**: Take the cheapest model of any tier, and place the long text in the final message
   - **c**: Take the model that was used last time, and cache the whole request as one piece
   - **d**: Take the cheapest that is capable and quick enough, and put the bulky constant passage first

3. Scenario: Avocet Travel builds a request from four pieces: the house rules, the fare policy, the traveller's current question and the traveller's earlier chat. Which arrangement fits best?
   - **a**: All four as static modules, so that the whole request can be cached together
   - **b**: Rules and question as static modules, with policy and chat as dynamic ones
   - **c**: Stable parts as static modules, per-visit parts as dynamic ones, the older talk ranked lower
   - **d**: All four as dynamic modules, so that no edit to any piece can block the others

<details>
<summary>Answer key</summary>

1. **b**. A missing value is a defect in the caller, so the assembler refuses and names it. *a* is ruled out because "A request that goes out with `{customer}` in its text" is sent with a gap that nobody checks. *c* is ruled out because a blank is the same silent defect, and "A missing variable is an error". *d* is ruled out because a guess invents data nobody supplied, and a request with a gap "is a defect that no model will report".
2. **d**. The model is chosen by tier and latency with price ranking the rest, and the stable text leads so that it can be cached. *a* is ruled out because the page says "static modules first, in the order given, then the dynamic ones". *b* is ruled out because "A model below the tier fails the task", whatever its price. *c* is ruled out because "Two requests share a cache entry only when everything up to the breakpoint is identical", so the whole request, which includes the changing part, cannot be cached as one piece.
3. **c**. The pieces that never change go first as static modules, and the per-request pieces follow, with the less useful one ranked lower so that it is shed first. *a* is ruled out because "A static module whose text holds a `{variable}` is refused", and the question changes on every request. *b* is ruled out because it puts the changing question among the static modules and the stable policy among the dynamic ones, against "static modules first, in the order given, then the dynamic ones". *d* is ruled out because the cache works on "the request from its start up to a marked breakpoint", so with no static modules there is nothing to cache.

</details>
