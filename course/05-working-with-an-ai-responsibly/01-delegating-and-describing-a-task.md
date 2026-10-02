# Delegating and describing a task

**Level:** Foundations · **Module 5:** Working with an AI, responsibly · **Page 1 of 2**
**Exams:** AS1, AS4 (and all exams: the working habits behind every later module)

**After this page you can** decide which steps of a job to hand to Claude and which to keep, and describe a
task so that the result can be judged.

Checked against the Associate exam guide (domains 1, 2, 4, 6 and 7, version 1.0, July 2026) and the Claude
API documentation on 2026-10-02. The four habits used here (delegate, describe, discern, take
responsibility) follow the shape of Anthropic's published AI fluency material for its audience; the wording,
examples and questions are the course's own.

## Why it matters

Most poor results come before the first prompt is typed: the wrong job was handed over, or the right job
was handed over without saying what good looks like. The Associate exam spends a domain on exactly this
(workflow integration and solution design, 16 percent) and the Developer exam expects the same judgment when
a team decides what an agent may do alone. This page is about the two habits that come first.

## The idea

### The four habits

Working well with an AI comes down to four things you keep doing:

1. **Delegate:** choose what to hand over and what to keep.
2. **Describe:** say what you want, so the job can be done and judged.
3. **Discern:** judge what comes back (page 2).
4. **Take responsibility:** own the result, and be honest about the AI's part in it (page 2).

They run in a loop, not a line: what you discern shapes the next description.

### Delegate: which steps go to Claude

Break the job into steps, then sort the steps. A step suits Claude when it is language-shaped (drafting,
summarising, comparing, extracting, rewording, brainstorming), tolerant of an occasional miss that a review
will catch, and does not need data or authority Claude lacks. A step stays with a person, or with code,
when it needs **judgment that carries accountability** (a hiring decision, a medical or legal call), when
it needs **exactness** (money, counts, identifiers), when it touches **data that policy keeps away** from
the tool, or when a **mistake would be hard to reverse**.

The skill the Associate guide names is separating *judgment steps* from *automatable steps*. A worked
example: monthly sales commentary.

| Step | Who | Reason |
|---|---|---|
| Pull figures from the finance system | Code or an analyst | Exact numbers, authoritative source |
| Draft the variance narrative from the figures | Claude | Language-shaped, reviewable |
| Decide which variances matter to the board | A person | Judgment with accountability |
| Check every figure in the draft against the source | A person or a script | Claude can state a figure that was never supplied |
| Format into the board template | Claude, or a template | Mechanical, cheap to check |
| Approve and send | A person | Ownership |

Note what Claude does here: it saves the time spent on prose, and nothing in the process depends on
trusting it where it is weak.

Also decide **how much to hand over at once**. A small, clear step with a visible result is easier to judge
than the whole job in one go. Start narrow, learn how Claude does on your material, then widen.

### Describe: the three things to say

A good description covers the product, the process and the performance of the job:

- **Product: what you want.** The deliverable, its form (an email, a table, a list of options), its length
  and its audience. "A two-paragraph update for the board, plain language, no jargon" is a product.
- **Process: how to go about it.** The steps or approach you want followed, what to consult, what to
  ignore, and what to ask you about if unclear. "Use only the attached figures. If something is missing,
  say so rather than estimating." is process.
- **Performance: how Claude should behave.** Tone, level of detail, whether to challenge you, whether to ask
  questions first. "Be direct, flag anything you are unsure of" is performance.

Add what a colleague new to the job would need: the **goal** behind the request (why, not only what), the
**context** (who reads it, what happened before), any **constraints** (length, words to avoid, rules) and,
when a style matters, **an example** of the target. Module 6 turns this into specific techniques; for now
the point is that the description is where most of the quality is decided.

A request that omits these gets a generic answer, which is the right response to a generic request.

<!-- illustrative -->
Two descriptions of the same job. The replies are summaries of what each tends to produce, hand-written for
this page and labelled illustrative, not recorded model output.

```text
Weak:    "Write something about our Q3 results."
         -> a generic, upbeat paragraph with no figures, because none were given

Better:  "Draft a two-paragraph Q3 update for the board (non-technical).
          Use only the figures below. Lead with revenue against plan, then the two largest
          variances and one risk. If a figure you need is missing, say so instead of estimating.
          Figures: revenue 4.2M (plan 4.5M); ..."
         -> a short update that uses the supplied figures, and flags what it lacks
```
<!-- /illustrative -->

### Know what you are describing for

Whether you are asking for **analysis**, **research**, **drafting** or **brainstorming** changes how you
describe the job. Analysis wants the material and the question; research wants sources named and
unsupported claims flagged; drafting wants audience, tone and an example; brainstorming wants breadth first
and judgment later. Module 6 returns to this as a strategy per task type.

### Human and agent teams

The same decisions apply when the AI is not one chat but an agent that does several steps itself. The
questions get sharper: what may it do without asking, what must a person approve, who is notified of what it
did, and where does it stop? Treat an agent like a new colleague with limited access: a defined scope, a
review before consequences, and a record of what happened. The Level 2 and 3 modules build the controls in
code; the habit starts here.

## Traps

1. **Delegating a whole job because the first step went well.** A good draft does not show the figures were
   right. Hand over in steps you can judge.
2. **Describing the topic, not the job.** "Write about X" has no audience, form or standard; the result has
   none either.
3. **Skipping the goal.** Without the reason for the request, Claude cannot choose between reasonable
   readings, and may pick one you did not mean.

## Quiz

1. A team lead wants an AI assistant to prepare a weekly status email from a spreadsheet of task
   progress notes. Which split of work is best?
   - **a**: The tool drafts from the rows; a person checks facts and sends
   - **b**: The tool collects, writes and sends without any review
   - **c**: A person writes it all; the tool only fixes spelling
   - **d**: The tool decides which risks to escalate to executives

2. A request reads: "Summarise this report." The summary is accurate but useless to the sales team who
   asked. Which missing piece of the description most likely caused it?
   - **a**: A temperature setting
   - **b**: A longer system prompt
   - **c**: The intended audience and purpose
   - **d**: A faster model tier

<details>
<summary>Answer key</summary>

1. **a**. Drafting is language-shaped and reviewable, while facts and sending remain with the owner (the worked example's table). *b* is ruled out because nothing in the process checks the output before it reaches readers. *c* is ruled out because it keeps all the hand-over value on the table. *d* is ruled out because deciding what to escalate is a judgment step that stays with a person.
2. **c**. The description covers the audience and goal as well as the deliverable, and a generic request gets a generic reply. *a* is ruled out because randomness does not supply a purpose. *b* is ruled out because length of instruction is not the missing element. *d* is ruled out because a faster tier does not know the readers either.

</details>
