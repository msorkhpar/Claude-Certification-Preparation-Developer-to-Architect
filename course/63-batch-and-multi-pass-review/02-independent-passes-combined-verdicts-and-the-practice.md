# Independent passes, combined verdicts and the practice

**Level:** Architect · **Module 63:** Batch and multi-pass review · **Page 2 of 2**
**Exams:** A4.6; S2, S5

**After this page you can** say why a model is a weak reviewer of its own work and what an independent instance changes, split a large review into local passes and an integration pass, schedule those passes when they run as batches, combine the findings of several passes into one list that keeps the highest severity and the weakest confidence, treat a finding that only one pass reported as unverified however confident it sounds, and write the module's practice.

Checked on 2026-10-04 against the exam guide's task statement 4.6, the Claude Code documentation pages "Best practices for Claude Code" (the writer and reviewer pattern, the adversarial review step) and on subagents (fresh context windows), and the Claude API documentation page "Batch processing". Nothing here called a model: the practice is graded by Python, TypeScript, Java and Kotlin test suites, offline, on findings written by hand. Module 60 showed an independent reviewer in a CI job; this page is about the shape of the whole review and about what to believe in its output.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* for multi-pass review (4.6) it says that a model "retains reasoning context from generation", which makes it less likely to question its own decisions in the same session, that independent review instances without that context are "more effective at catching subtle issues than self-review instructions or extended thinking", that a large review is split into per-file local passes plus a cross-file integration pass to avoid attention dilution and contradictory findings, and that a verification pass in which the model reports confidence beside each finding enables calibrated review routing. *What the product documents now (read 2026-10-04):* "A fresh context improves code review since Claude won't be biased toward code it just wrote"; a reviewer in a fresh subagent context "sees only the diff and the criteria you give it, not the reasoning that produced the change"; and a verification subagent that checks findings has "a fresh model try to refute the result, so the agent doing the work isn't the one grading it". The documentation describes the mechanism and recommends the pattern; the claim that it beats extended thinking is the guide's, and the exam keys it. On the exam, a question about a reviewer that approves its own work has the answer an independent instance, never more thinking or a sterner instruction.

## Why it matters

A team's agent writes a change, then reviews it in the same conversation, and approves nearly every time. The team adds "question every decision" to the prompt and raises the thinking budget. The approvals barely move, because the reviewer is still the author: the reasons that made each decision look right are in its context, and it reads the code through them. The remedy is structural. A second instance, given the code and the criteria and nothing else, has no reasons to defend. Scenarios S2 (code generation) and S5 (CI) test whether you reach for the structure or for a stronger prompt.

## The idea

### Why self-review is weak, and what is not a fix

A model that wrote something has, in its context, the plan and the reasoning that produced it. Asked to review, it tends to confirm: the reasoning explains why each choice was reasonable. Three things look like fixes and are not:

- a stricter instruction in the same conversation, since the reasoning is still there;
- a larger thinking budget, which gives the model more room to reason from the same starting point;
- a second review in the same conversation, which adds a third reader who shares the first two readers' context.

What changes the outcome is a reviewer without the generator's context: a fresh subagent, a separate session that is given only a diff, or a CI job. That reviewer receives what the example's `review_request` shows: the code, and not the reasoning. A reviewer is independent if it could not have copied the author's rationale.

### Local passes and an integration pass

A review of many files in one request spreads attention thinly: the first files get depth, the last get little, and comments on different files can contradict each other. The structure that works is the one module 50 used for decomposition, now seen as review: a local pass per file for the problems that live inside the file, and one integration pass over the whole set for the problems that live between files (a changed signature and its callers, data that flows through three modules). `review_plan` produces exactly that list, and a single-file review needs no integration pass.

When the passes run as batches, there is an ordering to respect. The integration pass reads the results of the local passes, so it cannot be in the same batch as them: it is a second batch, submitted after the first ends. The worst case of the whole review is therefore two processing windows plus the handling after each, which is about 50 hours, not 24. A review that must come back sooner runs its passes on the synchronous API, in parallel for the local ones.

### Combining the passes

Several independent passes produce several lists. The combination is a function with rules:

- The same finding, identified by file, line and issue, reported by two passes is one finding. A pass that reports it twice counts once, since repetition inside a pass is not independent evidence.
- Take the highest severity any pass gave, so that a pass that saw the danger is not overruled by one that missed it, and the lowest confidence, so that the weakest evidence caps the claim.
- Count the passes. Agreement of independent passes is the evidence; the count of reports is not.

The practice's `merge_passes` implements these and gives each finding a route.

### Confidence that does not verify itself

A verification pass can ask the model for a confidence number beside each finding, which is useful for routing review attention (module 68 calibrates the number against labelled cases). But a number the same pass produced about its own finding carries the same blind spot as the finding: a confident-but-wrong claim is confident because the reasoning behind it looked sound to its author. So a finding reported by one pass is not accepted on confidence alone, however high. It goes to `verify`, which means an independent check by another instance that tries to refute it, and a finding is accepted when two independent passes agree and neither is unsure. Findings that survive are the ones worth a person's time; findings that do not are dropped without anyone reading them.

The same rule answers a question about processing time: independent checks cost more calls, and so the cheap first passes are batches and only the unconfirmed findings pay for a second look.

### The practice: a review pipeline

The practice is in [`exercises/63-batch-and-multi-pass-review`](../../exercises/63-batch-and-multi-pass-review/unit-01/practice-1/statement.md). You write the submission interval that keeps a promise, the choice of interface for a workload, the plan for resubmitting the entries that failed, the passes of a multi-file review and the combination of their findings. It is graded in Python, TypeScript, Java and Kotlin; the statement lists eight cases, each saying what you should see when it works.

## Traps

1. **"Tell the reviewer to be critical of its own code."** It is tempting because it costs one sentence. The exam rejects it: the reasoning that justified the code is still in the context, so the instruction changes the tone of the review and not its starting point. An independent instance is the structure.
2. **"Give the review a bigger thinking budget."** It is tempting because deeper reasoning sounds like better review. The exam rejects it: the guide places independent instances above extended thinking, since more thought from the same context confirms the same conclusions.
3. **"Review all fourteen files in one request; the model can see everything."** It is tempting because it is the shortest design. The exam rejects it: attention thins and findings contradict one another. Review each file locally and the connections in an integration pass.
4. **"Accept any finding the model is 95 percent sure of."** It is tempting because the number looks like evidence. The exam rejects it: one pass's confidence in its own finding is not independent, so the finding is verified by a second instance before it is accepted.

## Quiz

1. Scenario S2, code generation with Claude Code. A team reviews each change in a separate run, yet nearly every verdict is still approval, because the run is also handed the author's rationale for each change. Which step removes the remaining cause?
   - **a**: Keep the notes and add a second review turn in the same conversation
   - **b**: Drop the notes and give the new instance the diff and the criteria
   - **c**: Keep the notes and raise the thinking budget until verdicts stop approving
   - **d**: Keep the notes and ask the instance to write a stricter checklist first

2. Scenario S6, structured data extraction. A verification pass gives each extracted claim a confidence score, and claims scored 95 or more are accepted without further checks. An audit finds wrong claims among the accepted ones. Which change fits best?
   - **a**: Accept at 99 instead, because a higher bar lets fewer wrong ones through
   - **b**: Have the same pass explain its score and accept when the explanation reads sound
   - **c**: Send the lowest-scoring tenth to a person and accept the rest as they are
   - **d**: Have a separate instance try to refute each one before it is trusted

<details>
<summary>Answer key</summary>

1. **b**. The page says "the reasoning that justified the code is still in the context", and the notes carry that reasoning into the new instance, so withholding them removes the cause. *a* is ruled out because the option is "a second review in the same conversation, which adds a third reader who shares the first two readers' context". *c* is ruled out because it is "a larger thinking budget, which gives the model more room to reason from the same starting point". *d* is ruled out because a checklist is "a stricter instruction in the same conversation, since the reasoning is still there".
2. **d**. A score the same pass produced about its own claim is not independent evidence, and another instance that tries to refute the claim is. *a* is ruled out because "a number the same pass produced about its own finding carries the same blind spot as the finding". *b* is ruled out because "confident-but-wrong claim is confident because the reasoning behind it looked sound to its author", so a sound-sounding explanation proves nothing. *c* is ruled out because "a finding reported by one pass is not accepted on confidence alone, however high", and this option accepts the rest on exactly that.

</details>

## Module quiz

This quiz covers both pages of the module.

1. Scenario S5, Claude Code in CI. A weekly audit submits per-file entries as one batch. When it ends, an integration pass over those results is submitted as a second batch. Each batch finishes within 24 hours, and handling takes 1 hour after each. What is the longest the audit can take, in hours?
   - **a**: 26
   - **b**: 48
   - **c**: 50
   - **d**: 49

2. Scenario S6, structured data extraction. A team will extract fields from 400,000 archived documents through a batch. A trial on 100 easy documents shows no failures. What should happen before the full submission?
   - **a**: Submit everything and repair the prompt from whatever fails
   - **b**: Run a varied sample synchronously, long and incomplete ones included, and refine
   - **c**: Repeat the trial on another hundred easy ones to be sure
   - **d**: Run the sample through the batch queue, so that the trial costs half as much per item

3. Scenario S2, code generation with Claude Code. Two independent review passes report the same injection risk. One rates it 1 on a severity scale of 1 to 3 with confidence 95, and the other rates it 3 with confidence 70. Which entry does the combined list carry?
   - **a**: 2 / 83
   - **b**: 1 / 95
   - **c**: 3 / 95
   - **d**: 3 / 70

<details>
<summary>Answer key</summary>

1. **c**. The integration pass reads the first batch's results, so the two windows run one after the other, with the handling after each: 24 + 1 + 24 + 1. *a* is ruled out because "it cannot be in the same batch as them", so the audit is not one window. *b* is ruled out because the worst case is "two processing windows plus the handling after each", and 48 has no handling. *d* is ruled out for the same sentence, since the handling is "plus the handling after each" batch and not once.
2. **b**. A prompt is proven on a sample that includes the awkward documents, run synchronously so the answer comes at once, before the discount is spent on volume. *a* is ruled out because "a day passes before the failures show, and the whole run is repeated". *c* is ruled out because "a sample of easy documents proves nothing". *d* is ruled out because the advice is to "run the prompt synchronously on a small, varied sample", not to wait a window for a trial.
3. **d**. The entry keeps the highest severity and the lowest confidence. *b* is ruled out because the rule is "so that a pass that saw the danger is not overruled by one that missed it". *c* is ruled out because the rule is "the lowest confidence, so that the weakest evidence caps the claim". *a* is ruled out because the rule is "Take the highest severity any pass gave", and nothing is averaged.

</details>

Adapted from the Claude Certified Architect - Foundations exam preparation guide by Daron Yondem (CC BY 4.0, https://creativecommons.org/licenses/by/4.0/), changed for this course.
