# Contracts between parts, and the review

**Level:** Architect Professional · **Module 80:** End-to-end and multi-agent architecture · **Page 2 of 2**
**Exams:** P1

**After this page you can** explain why two correct components can fail together and write the contract between them, decide what context a subagent receives and what it hands back, run an architecture review against a rubric whose findings and verdict follow fixed rules, and pick the cheapest design among those that pass.

Checked on 2026-10-04 against Anthropic's engineering articles "Building effective agents" and "How we built our multi-agent research system", and the Claude Certified Architect, Professional exam guide v1.0 (July 2026), domain 1. This page has no new example: the practice is the example, and it is graded in Python, TypeScript, Java and Kotlin without calling a model.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* the Professional guide expects the architect to design the whole solution, including the interfaces between its parts, and to review a proposed design for gaps. *What Anthropic's articles say:* in a team of agents the lead agent writes each subagent a task description, and a vague one leads subagents to duplicate work or leave gaps; each subagent returns its findings to the lead, which synthesises them. *How to read both:* the exam keys the answer that names the contract (what goes in, what comes out, what happens at the edges) over the answer that adds a component or a larger model. The articles are accounts of one research system, so take their lessons as design pressure and not as a specification.

## Why it matters

Two services were each tested and each passed. Together they fail one request in fifty. One of them returns an empty list when it finds nothing, and the other treats an empty list as "no answer yet" and waits. Neither is wrong by its own rules. The failure lives in the space between them, where no test was written and no one owned the question of what an empty result means. Multi-agent systems have more of these spaces than any other design, because each agent's output is another agent's input and the format is prose.

## The idea

### The failure at the boundary

When components that each work correctly fail together, the cause is almost always an under-specified contract, especially for edge and error cases. More components, a larger model or a retry do not touch it: a retry repeats the same disagreement, and a bigger model changes the wording of the output and not what the receiver assumes. The fix is to write down, for every boundary, four things.

| Part of the contract | The question it answers | Example for a subagent that searches |
|---|---|---|
| **Input** | What exactly does the receiver get? | A question, a source scope, a limit on the number of searches |
| **Output** | What shape does the result have? | A list of findings, each with a source and a one-line claim |
| **Empty and error cases** | What does "nothing found" or "could not look" return? | An explicit `found: none` and a separate `failed` with a reason |
| **Ownership** | Who checks the result before it is used? | The lead validates the shape, a reviewer validates the claims |

A structured output at each boundary is the tool: the receiving agent gets fields it can test, not prose it has to interpret (module 62). The empty case matters most, because it is the one nobody demonstrates.

### What a subagent gets, and what it returns

A subagent starts with only what it is given. It does not see the lead's conversation. That is a cost, since the lead must put everything the task needs into the task description, and it is a benefit, since the subagent's context stays small and its reading does not pile into the lead's. Three rules follow.

1. **Write the task like a brief.** The objective, the output format, the tools and sources to use, and the boundaries of the task, so that two subagents do not research the same thing or leave a gap between them.
2. **Return a digest, not a transcript.** The subagent's reading is thrown away when it finishes. What goes back is the finding, with its source, in the contract's shape.
3. **Keep unrelated work apart.** If the parts need the same context, or each step depends on the result of another, they should not be separate agents (page 1).

### An architecture review as a rubric

A review is repeatable when it is code. The practice has a rubric whose rules are the ones of this module.

| Finding | Severity | Raised when |
|---|---|---|
| `missing-stage:input`, `missing-stage:processing`, `missing-stage:output` | high | A stage is empty or absent |
| `no-feedback` | high | There is no feedback stage |
| `team-without-independence` | high | More than one agent, and the context is shared or the parts are not independent |
| `unapproved-write` | high | The design writes without approval and an audit is needed |
| `autonomy-without-need` | medium | An agent or a team is used on a path that is already known |
| `unvalidated-output` | medium | There is an output stage that has no validation step |

The verdict follows the worst finding: any `high` gives `reject`, otherwise any `medium` gives `revise`, otherwise `approve`. Price enters last, and only among the designs that were not rejected: the cheapest of those wins, a tie goes to the name that sorts first, and when every design is rejected there is no winner. Putting price last is the point of the exercise, because a cheaper design with a missing feedback loop is not cheaper, only unfinished.

### The practice: an architecture review

The practice is in [`exercises/80-end-to-end-and-multi-agent-architecture`](../../exercises/80-end-to-end-and-multi-agent-architecture/unit-01/practice-1/statement.md). You write `review`, `verdict` and `cheapest_adequate` for design descriptions the tests supply. It is graded in Python, TypeScript, Java and Kotlin, and the statement lists eight cases, each saying what you should see when it works.

## Traps

These are the wrong answers the exam's options for this domain offer, each with the reason it is rejected.

1. **"Replace the weaker component with a larger model."** It is tempting because the failure looks like quality. The exam rejects it: both parts pass their own checks, so the defect is the contract between them, and a bigger model only changes how the same ambiguity is worded.
2. **"Retry the failed hand-off; the failure is transient."** It is tempting because it is intermittent. The exam rejects it: a failure that follows an input pattern is deterministic, and a retry repeats it. Find the case the contract left out.
3. **"Give every subagent the whole conversation so nothing is lost."** It is tempting because context feels safe. The exam rejects it: the subagent's small context is the reason it exists, so give it a brief with the objective, format, tools and boundaries.

## Quiz

1. Scenario: At Alder Freight an intake service and a pricing service each pass every one of their own tests. In production about one request in fifty stalls between them, and always for shipments with no matching tariff. What should the architect fix?
   - **a**: The pricing service's model, swapped for a larger one that copes with odd shipments
   - **b**: The empty result, by writing down what both parts do when a lookup finds nothing
   - **c**: The transient network fault, by adding an automatic retry on each stall
   - **d**: The missing visibility, by placing a logging component between the two

2. Scenario: Cobalt Freight's design has an input stage, a processing stage, a feedback loop and an output stage that emails each quote to the customer the moment the model has written it. What does the rubric report?
   - **a**: No finding, since all four stages are present in the design and the loop is in place
   - **b**: A high finding, so the verdict is reject whatever the price of the whole design
   - **c**: A medium finding, so the verdict is revise until a vetting step is added
   - **d**: A medium finding, offset by the feedback loop, so the verdict is approve

<details>
<summary>Answer key</summary>

1. **b**. Both parts pass their own checks, so the gap is what neither contract says about the empty case. *a* is ruled out because the ambiguity stays whatever the model, and "a bigger model only changes how the same ambiguity is worded". *c* is ruled out because a failure that follows an input pattern "is deterministic, and a retry repeats it". *d* is ruled out because a logger is one more component, and "More components, a larger model or a retry do not touch it".
2. **c**. The output stage is present, since it delivers every quote, but nothing checks a quote before it goes out, which is a medium finding, and a medium finding gives `revise`. *a* is ruled out because the rubric raises a finding when "There is an output stage that has no validation step". *b* is ruled out because the high finding for output is raised when "A stage is empty or absent", and this output stage is present and delivers every quote. *d* is ruled out because no stage offsets a finding: "otherwise any `medium` gives `revise`, otherwise `approve`".

</details>

## Module quiz

This quiz covers both pages of the module.

1. Scenario: Fenwick Bank's design review turns up two problems, one rated medium and one rated high. What outcome does the rubric give?
   - **a**: Revise, since a design is rejected only when two of its findings are high
   - **b**: Reject, since the lesser issue does nothing to soften the graver one
   - **c**: Approve, as long as it is the cheapest of the designs under review
   - **d**: Revise, because the two ratings are averaged into one level of severity

2. Scenario: Willow Bank's complaint triage design collects a rating from customers for every reply and stores the ratings in a table that nobody reads. How should the review treat the feedback stage?
   - **a**: As present, since a rating is collected for every reply that customers receive
   - **b**: As absent, since nothing in the system ever changes because of those scores
   - **c**: As absent, which the rubric rates as medium, so the verdict is revise
   - **d**: As present, since the ratings stay available for anyone who later wants them

3. Scenario: Quince Retail runs a returns agent that writes refunds to the ledger without approval, and the auditors require a record of each approval. What does the rubric report?
   - **a**: A medium finding, because the refunds can be checked after the fact
   - **b**: No finding, since the agent's answers have already been validated before the write
   - **c**: An autonomy finding, since the agent acts with no person in the loop
   - **d**: A high finding, because each payment goes out with no one signing it off first

<details>
<summary>Answer key</summary>

1. **b**. One high finding settles the verdict, and the medium finding beside it does not soften it, so the outcome is `reject`. *a* is ruled out because no count of findings is needed: "any `high` gives `reject`". *c* is ruled out because price never rescues a design with a high finding: "Price enters last, and only among the designs that were not rejected". *d* is ruled out because there is no averaging: "The verdict follows the worst finding".
2. **b**. The stage is defined by "the path by which they change the system", and here there is none. *a* is ruled out because collecting a rating changes nothing, and the stage counts as missing when "Nobody learns that it was wrong". *c* is ruled out because the rubric lists `no-feedback` as high, and "any `high` gives `reject`". *d* is ruled out because ratings that sit unread are not a loop, and a pipeline that "never learns from the result, goes wrong silently and stays wrong".
3. **d**. A write without approval where an audit is needed is a `high` finding, so the verdict is reject, and "Price enters last, and only among the designs that were not rejected". *a* is ruled out because a later check is not an approval, and "a design with a missing stage or an unapproved write is rejected first", which only a `high` finding does. *b* is ruled out because the rule fires when "The design writes without approval and an audit is needed", whatever was validated. *c* is ruled out because the rubric raises the autonomy finding for a lack of need, not for a missing person in the loop: it applies only when "An agent or a team is used on a path that is already known", and a missing approval is the write rule's finding.

</details>

Adapted from CLAUDE-CERTIFICATIONS by Amey Thakur (MIT License).
