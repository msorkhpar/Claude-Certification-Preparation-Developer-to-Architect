# The model family and choosing a tier

**Level:** Foundations · **Module 3:** Claude's family and its surfaces · **Page 1 of 2**
**Exams:** all (AS3, DV1, DV2)

**After this page you can** name the current Claude models and their tiers, choose a tier by capability,
speed and cost for a workload, and explain why effort and mixing models are cheaper levers than always
using the largest model.

Checked against the models overview and "Choosing the right model" pages on 2026-10-02. Model ids,
windows, prices and dates below are read from those pages; they change, so the release pass of the course
re-checks them.

## Why it matters

"Which model?" is the most common first design decision and the easiest to get expensively wrong. The
Associate exam asks it as a business trade-off (a high volume of short drafts), the Developer exam as a
latency-and-cost trade-off with usage figures. The reasoning is the same: match the model to the
requirement in both directions, spend capability where it earns something, and measure instead of guessing.

## The idea

### The lineup on 2026-10-02

| | Claude Fable 5.1 | Claude Opus 5.5 | Claude Sonnet 5.5 | Claude Haiku 4.5 |
|---|---|---|---|---|
| API id | `claude-fable-5-1` | `claude-opus-5-5` | `claude-sonnet-5-5` | `claude-haiku-4-5-20251001` (alias `claude-haiku-4-5`) |
| What the page says it is for | demanding reasoning and long-horizon agentic work | long-running agentic coding and knowledge work | the best combination of speed and intelligence | the fastest model with near-frontier intelligence |
| Latency, relative | slower | moderate | fast | fastest |
| Price per million tokens, input / output | $10 / $50 | $4 / $20 | $2 / $10 | $1 / $5 |
| Context window | 1M | 1M | 1M | 200K |
| Maximum output | 128K | 128K | 128K | 64K |
| Thinking | adaptive, always on | adaptive, always on | adaptive | extended (manual budget) |
| Retirement, not sooner than | 2027-09-01 | 2027-09-22 | 2027-09-28 | 2026-10-15 |

Source: Models overview, Claude API documentation, read 2026-10-02.

Notes that the exams lean on:

- **Tier names.** The Associate guide speaks of Haiku, Sonnet and Opus as the three model types; the
  current lineup adds a top tier above Opus. The reasoning transfers: a fast low-cost tier, a balanced tier,
  and higher-capability tiers.
- **Every id is a snapshot.** The page says every Claude model id is a pinned snapshot, "including the
  dateless IDs used from the 4.6 generation on". For Haiku 4.5 the dated id is the snapshot and the shorter
  name is an alias. Pin ids in production and migrate on purpose (module 18).
- **Retirement dates are real.** Haiku 4.5 has the nearest one on the table. A design that hard-codes a
  model that retires is a future incident.
- **Batch and caching change the price.** The same page says Batch API requests are 50 percent cheaper and
  that prompt cache reads cost a fraction of the base input price. Modules 20 and 21 teach both.

### How the documentation says to choose

The "Choosing the right model" page starts with four criteria: **capabilities** (what the task needs),
**speed** (how quickly the application must answer), **cost** (development and production), and **effort**.
Effort deserves attention: several models take an effort parameter that "trades intelligence for latency
and cost within a single model", and the page says "tuning effort is often a better lever than switching
models". Fast mode, a research preview on some Opus models, offers "up to 2.5x higher output speed at
premium pricing".

Two starting strategies, both ending in measurement:

- **Efficiency first.** Begin with the fast, low-cost tier, test thoroughly, and move up only for a gap you
  can name. Suited to prototyping, tight latency, cost sensitivity and "high-volume, straightforward
  tasks".
- **Capability first.** Begin with the strongest starting point for complex tasks, get the quality, then
  work down to cheaper options through lower effort or smaller models. Suited to complex reasoning, accuracy
  that outweighs cost, and high-autonomy agent work. The overview's default advice is to start with Claude
  Opus 5.5 for most workloads and use Claude Fable 5.1 when evals at higher effort still fall short.

Source: Choosing the right model, Claude API documentation.

Either way the page's decision rule is the same: build benchmark tests for your use case, run your own
prompts and data across the candidates, and compare **accuracy, quality and edge-case handling**, then weigh
cost. "Having a good evaluation set is the most important step."

### Combine models

The page also describes multi-model designs that put most tokens on the cheaper model: an **executor** that
escalates hard decisions to an advisor, and an **orchestrator** that hands bulk work to lower-cost workers.
For exam scenarios this is the answer to "mostly easy, occasionally hard".

### Worked decision

An operations team drafts about 2,000 short customer replies a day. Most are routine; perhaps one in fifty
is a delicate escalation. Reasoning: routine drafts are straightforward, high-volume work where speed and
cost dominate, so start with the fast, low-cost tier and an evaluation set of real past replies; route the
rare escalations to a higher tier (or raise effort for them only); re-measure monthly. The wrong answers
are the two extremes: the top tier for everything wastes the budget, and the cheapest model for everything
under-serves exactly the cases that need depth.

## Traps

1. **Always choosing the largest model.** It raises cost and latency on work that does not need it, and
   nothing on the pages says the biggest model is the safest default for every task.
2. **Switching models when effort would do.** The documentation names effort as often the better lever.
3. **Using an unpinned or retiring id.** The table shows dates; an id with a retirement date needs a
   migration plan before the date, not after.

## Quiz

1. A team tags 50,000 short support tickets overnight, each with one of eight categories. Speed per ticket is
   irrelevant, spend matters, and the categories are easy to tell apart. What is the best first step?
   - **a**: Begin with the top tier, then step down if the budget complains
   - **b**: Begin with Claude Opus 5.5, because the overview names it the usual starting point
   - **c**: Begin with the lowest-priced tier and score it on genuine samples
   - **d**: Send the first thousand to each tier and keep whichever answers quickest

2. A coding agent running on the mid tier fails a handful of hard tasks each day. The team plans to move the
   entire system to the top tier. Which alternative should they try first?
   - **a**: Move all traffic to Claude Fable 5.1, then work the effort down later
   - **b**: Run the difficult cases at higher effort and measure the change
   - **c**: Drop to the fastest tier and retry each failure several times
   - **d**: Pin the id to a dated snapshot so results stop varying

<details>
<summary>Answer key</summary>

1. **c**. Simple, high-volume, cost-sensitive work fits the efficiency-first path, validated on real data (the two strategies). *a* is ruled out because the capability-first path is "Suited to complex reasoning, accuracy that outweighs cost, and high-autonomy agent work", and this task is the opposite. *b* is ruled out because the efficiency-first path says to "Begin with the fast, low-cost tier, test thoroughly, and move up only for a gap you can name", and the overview's default is only a starting point for most workloads. *d* is ruled out because the decision rule is to "compare accuracy, quality and edge-case handling", and speed per ticket is irrelevant here.
2. **b**. The page says tuning effort is often a better lever than switching models, and a narrower change targets the hard tasks only. *a* is ruled out because the largest model "raises cost and latency on work that does not need it", and effort would do. *c* is ruled out because the efficiency-first path says to "move up only for a gap you can name", and retrying hard tasks on a weaker tier moves the wrong way. *d* is ruled out because "an id with a retirement date needs a migration plan before the date, not after" is about the life of an id and says nothing about capability.

</details>
