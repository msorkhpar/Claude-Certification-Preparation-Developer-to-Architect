# The automation boundary, needed accuracy and the pilot

**Level:** Architect Professional · **Module 79:** From business problem to solution · **Page 2 of 2**
**Exams:** P1

**After this page you can** draw the line between what a solution may decide and what a person must, say who is accountable when an automated workflow is wrong, turn "as accurate as possible" into a needed accuracy that follows from the cost of an error and the cost of a review, price three designs on the same assumptions, and list which assumptions of a ten-user pilot fail at two thousand users.

Checked on 2026-10-04 against Anthropic's Usage Policy (the section on high-risk use cases), Anthropic's article "Building effective agents", and the Claude Certified Architect, Professional exam guide v1.0 (July 2026), domains 1 and 6. All money figures on this page are assumptions chosen to make the arithmetic visible. They are not measurements, and a real design replaces them with the customer's own numbers.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* domain 1 asks the candidate to translate business problems into solutions and to align them with value pillars, and domain 6 to manage expectations, so the exam asks architecture questions whose best answer is to establish a fact first: where accountability sits, what accuracy the use case needs and what an error costs. *What the product's policy says:* Anthropic's Usage Policy lists high-risk use cases (legal, healthcare, insurance, finance, employment and housing, academic testing, and media) and says that where outputs affect individuals "a qualified professional in that field must review the content or decision prior to dissemination or finalization", and that outputs presented directly to individuals or consumers must be disclosed as produced with AI help. The policy is a floor the deployer must meet. The exam keys the architect's reading of it: a design places a person at the decision the policy and the organisation's own accountability require, and automates the rest.

## Why it matters

The questions that decide whether a Claude project is allowed to exist are not about prompts. Who answers to the regulator when the system approves a payment it should not have? How accurate does it have to be before it earns its place? Will it still work with two thousand users? A sponsor who hears "99.9 percent or we cancel" and an architect who answers with a model benchmark are both having the wrong conversation. This page gives the architect the three facts to establish before promising anything: the boundary, the number and the scale.

## The idea

### The automation boundary and who is accountable

A system decides nothing in the legal or moral sense. The organisation that deploys an automated workflow owns what it does: "deploying an automated decision means owning its outcomes", and the model's provider supplies a capability, not accountability for how a customer uses it. That is why the **automation boundary** is an architectural decision made first. Ask of every decision in the process: if this is wrong, who answers for it, and could a person have caught it?

- **Automate** decisions that are reversible, cheap to get wrong and easy to audit afterwards, such as routing a ticket or drafting a reply.
- **Keep with a person** decisions that carry accountability, cannot be undone or fall under a rule that demands a human, such as approving a payment, denying a claim, or giving advice to an individual in a regulated field. The model prepares the file; a qualified person decides.
- **Enforce in code** any control that must always hold, such as a spending limit or a mandatory approval. Probabilistic behaviour cannot give a guarantee, however high the measured accuracy, so a regulatory control never rests on the model alone.

Where a person decides, the design must make their review real: they see the evidence, not only the model's verdict, and the interface does not turn review into a click-through. Disclosure belongs here too: people who receive output directly are told that AI helped produce it.

### Needed accuracy comes from two costs

"As accurate as possible" is not a requirement, because it cannot be designed against, tested or priced. A requirement is a number, and the number follows from two costs.

- The **error cost**: what one wrong decision costs, separately for each direction when they differ (a wrong approval pays money out, a wrong denial triggers an appeal).
- The **review cost**: what it costs to have a person check one item.

An item is worth reviewing when its expected error cost exceeds the review cost: `(1 − accuracy) × error cost > review cost`. With an error cost of 250 and a review cost of 5, the break-even accuracy is 98 percent, since `(1 − 0.98) × 250 = 5`. Where the model is more accurate than that, reviewing the item loses money; where it is less accurate, not reviewing it does. The needed accuracy is therefore a property of the slice of work, not of the model, and the way to reach it is to measure accuracy by slice and to route by confidence and consequence (module 90): high confidence and low consequence flow through, the rest go to a person.

Here is the arithmetic for one month at Northwind Insurance, 20,000 claim decisions, an error cost of 250 on average, with the assumptions written beside each design.

| Design | Review cost | Errors | Error cost | Total |
|---|---|---|---|---|
| **People decide everything** (12 each, 1.5 percent wrong) | 240,000 | 300 | 75,000 | 315,000 |
| **Model decides everything** (6 percent wrong) | 0 | 1,200 | 300,000 | 300,000 |
| **Person decides every item with the model's draft** (5 each, 1 percent wrong) | 100,000 | 200 | 50,000 | 150,000 |
| **Route by confidence**: 70 percent of items are confident and 99 percent right, the other 30 percent go to a person (5 each, 1 percent wrong) | 30,000 | 140 + 60 = 200 | 50,000 | 80,000 |

Three things follow. Full automation at 6 percent wrong barely beats the human baseline once errors are priced, so the headline "accuracy is high" is not a business case. Reviewing everything wastes review on the slice where the model is already better than the break-even. And the lowest total comes from the design that spends human attention only where it changes the outcome. The accuracy to ask for is "99 percent on the confident slice, with the rest reviewed", not one number for the whole system. If the sponsor insists on a single figure, give the number that matches the decision they face and say what it does not cover (module 91).

### From a pilot to the full population

A pilot with ten users proves that the idea can work. It does not prove that it will work for two thousand, because the pilot's conditions were friendly in ways nobody wrote down. The architect's contribution is to name those assumptions before the roll-out, not after the first incident.

| Assumption that held in the pilot | What may differ at scale | How to test it |
|---|---|---|
| **The inputs were typical.** Ten volunteers sent clean cases | A long tail of odd, long, multilingual or hostile inputs | Sample real production inputs and evaluate by slice (module 88) |
| **People covered the edge cases.** The pilot team fixed problems by hand | Support load grows with users | Count escalations per hundred tasks and staff for the rate |
| **Capacity was never close.** A handful of requests a minute | Rate limits, concurrency and cost per task move together | Model peak load, tokens per task and cache hit rate (module 84) |
| **Reviewers could keep up.** Two people checked everything | Review queues grow, so quality of review falls | Compute reviewer hours per day from volume and routing rates |
| **Access was simple.** One team's data | Many sources, owners and permissions | Test permissions with the least privileged real role (module 86) |
| **The data did not drift.** A short window | Inputs change with products, seasons and rules | Monitor distributions and re-run the evaluation set (modules 87 and 89) |

None of the rows is about the model getting worse. All of them are about the surroundings that stopped being friendly. A roll-out plan that lists these assumptions, the evidence for each and the trigger that stops the roll-out is a plan; a roll-out plan that lists licence counts is not.

## Traps

These are the wrong answers the exam's options for this domain offer, each with the reason it is rejected.

1. **"Promise the 99.9 percent and expect later models to close the gap."** It is tempting because it ends an uncomfortable meeting. The exam rejects it: an unmeasured promise on a number nobody can support destroys the trust the role exists to build. Establish what accuracy the use case needs and what an error costs, then show the design that meets the outcome.
2. **"Automate the whole approval process because the model scored well in testing."** It is tempting because the test result is a number. The exam rejects it: a test score says nothing about who is accountable, and a decision that carries accountability stays with a person while the model prepares the file.
3. **"Scale the pilot as it is and buy more licences."** It is tempting because the pilot succeeded. The exam rejects it: what scale changes is the assumptions, so the first thing to raise is which of them held at ten users and may not hold at two thousand.

## Quiz

1. Scenario: Ridgeway Credit wants an autonomous agent that approves loan disbursements end to end, and the sponsor is eager to start. What should the architect establish first?
   - **a**: Which model tier will run the approval steps, so that the cost of the project can be estimated early in planning
   - **b**: How many tokens one disbursement review is expected to consume across all the steps
   - **c**: Whether the build can be completed inside the quarter that the sponsor has proposed
   - **d**: Who at the lender answers when a payout goes wrong, and so where a person must make the call

2. Scenario: Alder Health's ten-user pilot of a note-summarising assistant went well, and the business now wants it for two thousand clinicians. What should the architect raise first?
   - **a**: The cost of the additional licences for the two thousand new users
   - **b**: Whether the trial's inputs, review load and data access will look the same across all wards
   - **c**: Whether a larger and more capable model should replace the smaller one that was used in the pilot
   - **d**: The training timetable for the clinicians who will start using it

<details>
<summary>Answer key</summary>

1. **d**. Where accountability sits fixes what may be automated at all, and everything else follows from that boundary. *a* is ruled out because the model comes after the boundary, which "is an architectural decision made first". *b* is ruled out because a token count prices a design whose shape is not yet settled, and the fact to establish first is "where accountability sits, what accuracy the use case needs". *c* is ruled out because a schedule is not among the facts the page says to establish before promising anything: "the boundary, the number and the scale".
2. **b**. Scale changes the surroundings, so the first contribution is naming the assumptions that held only in friendly conditions. *a* is ruled out because licences are a consequence and not a risk, and a roll-out plan "that lists licence counts is not" a plan. *c* is ruled out because the page says "None of the rows is about the model getting worse". *d* is ruled out because a timetable is not what makes a roll-out plan, which is one that "lists these assumptions, the evidence for each and the trigger that stops the roll-out".

</details>

## Module quiz

This quiz covers both pages of the module.

1. Scenario: Quillfeather Retail's sponsor tells the architect that a returns-triage system must be "as accurate as possible" and asks for a design. Which response fits best?
   - **a**: Ask what a wrong decision and a human check each cost, and set the target from both
   - **b**: Start from the most capable model and optimise the prompts until accuracy stops improving
   - **c**: Choose the most elaborate pattern the budget allows, since that gives the highest ceiling
   - **d**: Hold the question open until the pilot has produced some real accuracy figures to use

2. Scenario: Pine Ridge Insurance prices three designs for 20,000 monthly decisions. A person checking one item costs 5, a wrong decision costs 250, and the model is right on 99 percent of its confident items. Which handling of the confident items fits best?
   - **a**: Send each of them to a person, because a human check lowers the error count and so repays its price
   - **b**: Pass them through unreviewed, because reviewing them would burn more than the errors it prevents
   - **c**: Sample a tenth of them for review, since a sample is cheaper than a full check
   - **d**: Route them by the same rule as the unconfident items, since one policy for the whole flow is simpler to run

3. Scenario: Corvid Care pilots a discharge-letter drafting assistant on two wards. Ward staff still check and sign every letter, and the sponsor's case rests on what they do with the time the drafts give back. The sponsor must say which value pillar the project serves and how it will be shown. Which answer fits best?
   - **a**: Productivity, measured by the number of patients sent home per person in a shift
   - **b**: Cost, measured by the token price of each discharge letter that the assistant drafts
   - **c**: Transformation, measured by the share of discharge letters the assistant drafts each week
   - **d**: Performance, measured by the 95th percentile time the assistant takes to return a draft

<details>
<summary>Answer key</summary>

1. **a**. An unbounded requirement cannot be designed against, so it is made concrete first: "needed accuracy comes from two costs". *b* is ruled out because capability without a target spends money without a bar: "cannot be designed against, tested or priced". *c* is ruled out because the ladder buys complexity only for a stated requirement, and the page says "find the simplest solution possible". *d* is ruled out because the design is aimed at a number, and "the first piece of work" is finding it before the pilot is built.
2. **b**. The expected error cost of 2.50 per item is below the review cost of 5, so a review loses money. *a* is ruled out because "reviewing the item loses money" where the model beats the break-even, and the page says "Reviewing everything wastes review". *c* is ruled out because a sample is a guess at the same trade-off, and the page asks for design that spends attention "only where it changes the outcome". *d* is ruled out because the needed accuracy is "a property of the slice of work, not of the model", and the page asks for a target per slice, "not one number for the whole system".
3. **a**. Time handed back to staff who then send more patients home is people getting more done, so the pillar is productivity and the measure follows the people, not the tool. *b* is ruled out because the page says the cost pillar counts "tokens, review time and rework, not model price alone". *c* is ruled out because transformation claims "Something that could not be done before", and ward staff wrote discharge letters before the pilot. *d* is ruled out because a draft's turnaround time is a system target of the performance pillar, with "95th percentile latency, availability, accuracy by case type", and says nothing of what the staff complete.

</details>

Adapted from CLAUDE-CERTIFICATIONS by Amey Thakur (MIT License).
