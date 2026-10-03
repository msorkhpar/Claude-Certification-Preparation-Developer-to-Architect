# Matching a tier to a workload

**Level:** Developer · **Module 18:** Model choice, cost and migration · **Page 1 of 3**
**Exams:** DV2

**After this page you can** choose a model tier for a workload from quality, latency, cost and context needs, read the
price table without inferring a price from a model's name, and describe a router that sends each request to the cheapest
model that can do it.

Checked against the Claude API documentation (Models overview, Pricing) on 2026-10-02. The model ids and prices are those of
`docs/VERSIONS.md` on that date; they move, and a release pass re-reads them.

## Why it matters

Model choice is the largest cost lever an application has and the easiest to get wrong in both directions. A team that
sends everything to the largest model pays several times too much for classification and extraction. A team that sends
everything to the smallest model ships answers that fail on the hard cases. The exam asks you to pick a tier for a stated
workload and to say what evidence would change the pick.

## The idea

### Four tiers, four trade-offs

The documentation lists four current models side by side. This table keeps the columns a choice depends on (Models overview
and Pricing, read on 2026-10-02):

| Model (API id) | Latency | Input / output per million tokens | Context | Max output |
|---|---|---|---|---|
| Claude Fable 5.1 (`claude-fable-5-1`) | Slower | $10 / $50 | 1M | 128K |
| Claude Opus 5.5 (`claude-opus-5-5`) | Moderate | $4 / $20 | 1M | 128K |
| Claude Sonnet 5.5 (`claude-sonnet-5-5`) | Fast | $2 / $10 | 1M | 128K |
| Claude Haiku 4.5 (`claude-haiku-4-5-20251001`) | Fastest | $1 / $5 | 200K | 64K |

The latency column is relative: the page says actual latency "depends on prompt length, output length, and thinking effort".
Read the other columns as three separate questions. **Can it do the task?** is quality, and only your own evaluation answers
it. **Does the input fit?** is context: a 300,000-token input rules Haiku 4.5 out whatever its price. **Can it write the
answer?** is max output: a 100,000-token report rules Haiku 4.5 out too.

### Where the documentation starts you

Two official pages give advice that looks opposed. The Models overview says:

> If you're unsure which model to use, start with Claude Opus 5.5 for most workloads.

Source: Models overview.

The Pricing page, in its cost optimisation list, says:

> Choose Haiku for simple tasks, Sonnet for most production workloads, and Opus for the most complex reasoning

Source: Pricing.

They answer different questions. The first is where to begin when you know nothing: start high, so that a failure means the
task is hard and not that the model was too small. The second is where to end up once you have measured: the cheapest tier
that passes. The method joins them: **begin with a capable model, measure on your own cases, then move down a tier at a time
until quality drops, and keep the last tier that held.** The overview adds that Fable 5.1 is for "demanding reasoning and
long-horizon agentic work, or when your evals on Claude Opus 5.5 at higher effort still fall short", so a tier above Opus is
earned by failing an evaluation, not assumed.

### A price table is read, not guessed

Do not infer a price from a model's name or release order. On 2026-10-02 the table showed Claude Opus 5.5 at $4 and $20, below
the $5 and $25 of Opus 5, 4.8 and 4.7, and Claude Sonnet 5.5 at $2 and $10, below the $3 and $15 of Sonnet 4.6. A newer model
can be cheaper than the one it replaces, and the reverse can happen. Two further facts make the price per token a poor
proxy for the price per task:

- A tokenizer change moves the count. The Pricing page says Claude 4.7 and later models use a newer tokenizer and that
  "This tokenizer produces approximately 30% more tokens for the same text." The same document costs more tokens on a newer
  model, so compare cost per task on a sample, not per million tokens.
- Output costs five times input on every row, so a task that writes a lot is dominated by output. A classifier that returns a
  label is cheap on any tier; a drafting task is not.

### Routing: the cheapest model that can do each request

A router chooses the model per request. Three shapes cover most designs:

1. **Static routing by workload.** Each feature has its model: Haiku 4.5 for tagging, Sonnet 5.5 for drafting, Opus 5.5 for
   the agent that edits code. It is simple and testable, and it is the right first step.
2. **Rule-based routing by request shape.** The input size, the output cap or a user's plan picks the model. The rules in the
   practice of page 2 are of this kind: a model is skipped when its context or output limit cannot take the task.
3. **Cascade.** Try the cheap model, check the answer with code or with a second model, and escalate only the failures. It pays
   when most requests pass at the cheap tier; it costs a wasted call on each escalation.

One routing hazard is specific to thinking models. The Opus 5.5 migration guide notes that thinking blocks are tied to the
model that wrote them: "A router or fallback that moves a conversation from Claude Opus 5.5 to any other model runs those
turns without them." A router that switches model in the middle of a conversation should expect the new model to see the
earlier turns without the earlier reasoning, and should test for it.

## Traps

1. **Choosing by reputation.** A tier is right only if it passed your evaluation at the lowest cost. Test the cheap tier first
   on the hard cases, not on the easy ones.
2. **Comparing prices per token across tokenizers.** A model that is cheaper per token can cost more per task when it counts
   more tokens. Price a sample of real inputs on each candidate.
3. **Routing on the first message only.** A conversation that grows past a small model's context, or that needs a long answer
   later, outgrows the first choice. Re-check the limits on every request.

## Quiz

1. A team labels support tickets with one word each, and its tests show the cheapest tier is as accurate as the others. Which
   choice does the page back?
   - **a**: The newest release, since a later model has the lowest price
   - **b**: The most capable tier, because ticket text can always hide a hard case
   - **c**: The middle tier, since it balances the tests against the price list
   - **d**: The smallest model, since the extra capability above it buys nothing here

2. A job feeds 300,000 tokens to a model and expects a short answer. One model is cheapest by price per million. What rules it
   out first?
   - **a**: Its context window is smaller than what is sent
   - **b**: Its latency is the highest in the table
   - **c**: Its tokenizer is older than the others
   - **d**: Its output costs more than its input

3. A team moves from Sonnet 4.6 to Sonnet 5.5, which lists lower prices, and the bill for identical documents rises. Which
   explanation fits the page?
   - **a**: A lower list price always guarantees a lower cost per task
   - **b**: Output on the replacement costs about twice what input costs
   - **c**: The later tokenizer counts more tokens for the same text
   - **d**: Models released later always cost less than earlier ones

<details>
<summary>Answer key</summary>

1. **d**. The page's method is to move down "until quality drops, and keep the last tier that held", and the tests show nothing is lost at the cheapest tier. *b* is ruled out because a tier above Opus is "earned by failing an evaluation, not assumed". *c* is ruled out because the method keeps "the cheapest tier that passes", not a tier in the middle. *a* is ruled out because "A newer model can be cheaper than the one it replaces, and the reverse can happen".
2. **a**. The page says "a 300,000-token input rules Haiku 4.5 out whatever its price", because the input must fit the context window. *b* is ruled out because the latency column is relative and "depends on prompt length, output length, and thinking effort". *c* is ruled out because "A tokenizer change moves the count" and does not block an input. *d* is ruled out because the page states "Output costs five times input on every row", which is a cost fact and not a limit.
3. **c**. The page says the newer tokenizer "produces approximately 30% more tokens for the same text", so a lower list price can still cost more per task. *b* is ruled out because "Output costs five times input on every row". *a* is ruled out because the two facts "make the price per token a poor proxy for the price per task". *d* is ruled out because "A newer model can be cheaper than the one it replaces, and the reverse can happen".

</details>
