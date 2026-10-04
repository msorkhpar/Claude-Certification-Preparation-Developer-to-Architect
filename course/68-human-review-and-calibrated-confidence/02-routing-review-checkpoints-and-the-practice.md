# Routing review, checkpoints for irreversible actions, and the practice

**Level:** Architect · **Module 68:** Human review and calibrated confidence · **Page 2 of 2**
**Exams:** A5.5; S6, S1

**After this page you can** decide which extractions a limited team of reviewers should see and in what order, treat contradictory or ambiguous sources as a reason for review whatever the confidence, keep a backlog instead of silently dropping what does not fit, put an authorisation checkpoint in code in front of actions that cannot be undone, and write the module's practice.

Checked on 2026-10-04 against the exam guide's task statement 5.5 and scenarios S6 and S1, and against the course's own earlier pages on guarantees in code (modules 48 and 49) and on permissions (module 56). Nothing here called a model: the practice is graded by Python, TypeScript, Java and Kotlin test suites, offline. The first page measured accuracy and calibrated a threshold; this page spends the reviewers that the measurement leaves and guards the actions that no measurement makes safe.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* extractions with "low model confidence or ambiguous/contradictory source documents" go to human review, "prioritizing limited reviewer capacity". The guide's wording says nothing about irreversible actions in particular; the outline of this course includes the authorisation checkpoint, and the course's own pages (module 48) place a guarantee in code, because "a control the model could talk its way around is not a control". *What the product documents now:* a `PreToolUse` hook runs before every other step of the permission decision, and its denial holds even in `bypassPermissions` (read for modules 48 and 56). That is the mechanism for a checkpoint in the Agent SDK and Claude Code. The review queue, its order and its capacity are yours: no product feature manages reviewers. On the exam, a question about limited reviewers has the answer: route by low confidence and by conflict, weakest first, and keep what does not fit in a backlog.

## Why it matters

After calibration, a team automates what is safe and flags the rest: 300 extractions a day. Two reviewers can read 120. If the queue is first come, first served, the reviewers spend their day on whatever arrived early, and the contradictory invoice with a 99 confidence (the model was sure of the wrong total) is read last or never. A different team lets a support agent close accounts on its own when its confidence is high, and one confident mistake closes the wrong account for good. The first failure wastes scarce attention, the second spends something that cannot be recovered. The scenarios (S6 and S1) test whether the design points attention at the right items and keeps people in front of the actions that matter.

## The idea

### What goes to a person

Two kinds of extraction need review, and they are independent:

- **Low confidence.** The score is below the calibrated threshold from page 1.
- **Ambiguous or contradictory sources.** Two documents give different values for the same field, or the source cannot be read one way. The model may report high confidence, because it picked one reading and never noticed the other. A conflict is a reason for review whatever the score.

Everything else is accepted automatically, and is still sampled by stratum (page 1) so that the accepted stream is measured as well.

### Order the queue for scarce attention

When reviewers can read fewer items than are flagged, the order decides what is looked at. The practice's rule puts conflicts first (their score says nothing), then the lowest confidence first, and breaks ties by identifier so the result is the same every time. The first `capacity` items are the day's review. The rest are a backlog: a queue that is kept and shown, not dropped. A backlog that grows is information: it says the threshold or the reviewer capacity is wrong, and the answer is to change one of them on purpose.

Raising the threshold to shrink the queue throws away the calibration that made the threshold mean something. Lowering the capacity of the review to match a low count of staff does the same from the other side. Keep the threshold where the labelled data put it, queue what falls below it, and report the backlog.

### A checkpoint for what cannot be undone

Confidence is a statement about being right, and it does nothing about a deleted record, a sent payment or a closed account. For such actions the design puts a checkpoint in code that does not read the confidence at all: an action on the irreversible list always needs a person, and so does any action above a monetary or size limit. The practice's `checkpoint` returns `human` for the three irreversible actions at any amount, and for anything above its limit.

The checkpoint belongs in code, not in a prompt. Module 48 gave the reason: an instruction the model can be talked out of is a request, and a hook or a tool wrapper that refuses until a person approves is a control. In the Agent SDK and in Claude Code that is a `PreToolUse` hook, whose denial holds even when the session skips permission prompts. The checkpoint is also a place to learn: each approval or rejection is a labelled example for the next calibration.

### The practice: human review that does not fool itself

The practice is in [`exercises/68-human-review-and-calibrated-confidence`](../../exercises/68-human-review-and-calibrated-confidence/unit-01/practice-1/statement.md). You write the accuracy breakdown, the decision to automate, the calibrated threshold, the stratified sample, the routing of the review queue and the checkpoint. It is graded in Python, TypeScript, Java and Kotlin; the statement lists nine cases, each saying what you should see when it works. The outline marks this module as code, and the practice is a small review pipeline without the model.

## Traps

1. **"Review the flagged items in the order they arrived."** It is tempting because it is fair and needs no logic. The exam rejects it: reviewer attention is limited, so spend it on conflicts and the lowest confidence first.
2. **"A high confidence means the sources agree."** It is tempting because the number is the model's summary. The exam rejects it: the model may have chosen one reading of a contradiction without noticing, so a conflict goes to review whatever the score.
3. **"Raise the threshold until the queue fits the reviewers."** It is tempting because it makes the backlog vanish. The exam rejects it: the threshold comes from labelled data. Keep it, queue the overflow and report the backlog.
4. **"The model is sure, so let it close the account."** It is tempting because the confidence is 99. The exam rejects it: an irreversible action needs a checkpoint in code, whatever the score.

## Quiz

1. Scenario S6, structured data extraction. Flagged extractions pile up faster than two reviewers can read them. Which handling fits?
   - **a**: Read them as they come in, because a first-in order treats every document and every reviewer fairly
   - **b**: Take conflicts and the least certain items first, and keep the overflow as a backlog
   - **c**: Drop whatever has not been read by the end of the week, so the pile never grows
   - **d**: Lift the cut-off until the pile matches what two people can read in a day

2. Scenario S1, customer support resolution agent. An agent that can close accounts reports 99 out of 100 for its confidence in closing one. Where should the guard on that step live?
   - **a**: In a note appended to the closing message after the action has been taken
   - **b**: In the system prompt, as an instruction to ask the customer first whenever the number is under 100
   - **c**: In a confidence floor above which the agent may act on its own
   - **d**: In a code-level check that always requires a person here, whatever the number

<details>
<summary>Answer key</summary>

1. **b**. Conflicts and the lowest confidence are read first and the overflow waits, visibly, in a backlog. *a* is ruled out because "reviewer attention is limited, so spend it on conflicts and the lowest confidence first". *c* is ruled out because a backlog is "a queue that is kept and shown, not dropped". *d* is ruled out because "the threshold comes from labelled data. Keep it, queue the overflow and report the backlog".
2. **d**. The checkpoint reads no confidence, and the irreversible step always needs a person, as the page says: "an action on the irreversible list always needs a person". *b* is ruled out because "an instruction the model can be talked out of is a request". *c* is ruled out because the checkpoint "does not read the confidence at all", and a floor reads it. *a* is ruled out because the account is already closed by then, and "it does nothing about a deleted record, a sent payment or a closed account".

</details>

## Module quiz

This quiz covers both pages of the module.

1. Scenario S6, structured data extraction. On a validation set with its right answers known, extractions scored 80 are right 62% of the time, and the goal for automatic acceptance is 90%. What follows?
   - **a**: Lower the cut-off until the share of accepted items reaches the whole validation set
   - **b**: Keep 80 as the cut-off, because the model itself reported that score with a great deal of certainty
   - **c**: Place the cut-off at the lowest level whose admitted items reach that bar in these records
   - **d**: Replace the scores by an average over the document types, then reapply them

2. Scenario S6, structured data extraction. One kind of form has only a dozen checked samples, all of them extracted correctly, while every other kind has hundreds. May that kind be automated?
   - **a**: Yes, because a perfect score is the best a kind can have
   - **b**: No, until further ground-truth records give it enough evidence
   - **c**: Yes, once the overall figure is above the goal for the whole set
   - **d**: No, because a kind with no mistakes cannot be measured at all

3. Scenario S6, structured data extraction. Two source documents give different totals for one invoice, and the model reports 96 for the value it chose. What happens to the extraction?
   - **a**: It is accepted, because the score is above the calibrated cut-off
   - **b**: It is dropped from the batch, because the sources cannot both be right
   - **c**: It is accepted after the model is asked to restate its score
   - **d**: It goes to a person, because the disagreement is the reason, whatever the score

<details>
<summary>Answer key</summary>

1. **c**. The page says to "Pick the lowest threshold whose precision reaches the target", so the cut-off moves up to the lowest score that does. *b* is ruled out because "the model wrote the number, and only a labelled validation set says how often that score is right". *a* is ruled out because a lower cut-off lets in the less reliable items, while the page measures the precision of "every item at or above it". *d* is ruled out because an average over types is the "aggregate" that "hides the weak segment".
2. **b**. The page requires "a minimum number of labelled examples as well as a minimum accuracy". *a* is ruled out because "A segment that scores 100% on four examples has shown very little". *c* is ruled out because "The overall figure plays no part in the decision". *d* is ruled out because "a segment with fewer than `min_n` labelled records is `undersampled`", which is measurable and calls for more records.
3. **d**. A conflict is a reason for review whatever the score. *a* is ruled out because the model "picked one reading and never noticed the other". *c* is ruled out because "the model wrote the number, and only a labelled validation set says how often that score is right", so a restated score settles nothing. *b* is ruled out because a backlog is "kept and shown, not dropped", and a dropped item loses the invoice.

</details>

Adapted from the Claude Certified Architect - Foundations exam preparation guide by Daron Yondem (CC BY 4.0, https://creativecommons.org/licenses/by/4.0/), changed for this course.
