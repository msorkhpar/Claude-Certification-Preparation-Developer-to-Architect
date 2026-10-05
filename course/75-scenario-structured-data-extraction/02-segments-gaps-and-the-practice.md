# Segments, gaps and the practice: what the audit decides

**Level:** Architect · **Module 75:** Scenario: structured data extraction · **Page 2 of 2**
**Exams:** A4, A5; S6

**After this page you can** decide whether a kind of document may go without a person, say when a pipeline's figure is overstated, order the fixes a run's failure shapes call for, and write the module's practice: the audit of an extraction run as a function.

Checked on 2026-10-04 against the Architect exam guide (version 1.0, scenario 6, task statements 4.4, 4.5, 5.5) and the Claude API documentation page "Batch processing". The practice is offline in Python, TypeScript, Java and Kotlin: the tests build the log of a run and read your report. The shapes of the log, of the policy and of the report, the percentages and the order of the fixes are this course's own design for the capstone, not an Anthropic interface, and the statement says so.

## Why it matters

Page 1 was about what a pipeline owns while it runs. This page is about the decision at the end of a run: what to believe, what to automate and what to repair first. A team that automates on an average, or that fixes whatever is most visible, spends effort in the wrong place and ships wrong records downstream. The exam's questions about accuracy, review and retries are questions about this decision.

## The idea

### What a kind of document needs before a person is removed

A kind of document may go without a person only when two things hold: the kind has at least a minimum number of documents behind it, and its accuracy is at least the target. Both comparisons are inclusive. A kind with exactly the minimum number is enough, and a kind exactly at the target is ready. The minimum matters because ten correct documents are a small sample, and the target is a floor and not a ceiling. The overall figure plays no part in this decision, because an average over easy documents can be high while one kind is far below the target.

### When the figure is overstated

The validated figure is above the all-document figure whenever anything failed. A small difference is the ordinary cost of doing business. The audit calls the figure overstated only when the difference is greater than a gap that the policy sets, and a difference that only reaches the gap is not yet overstated. The reason to set a gap and not to compare for equality is that two honest measurements never match to the last point, and the report should speak up when the distance is large enough to mislead a decision.

### The order of the fixes

A run often shows several failure shapes at once, and the audit names one fix first. The order follows the damage a shape does downstream. An invented value is a wrong record that looks right, so it travels on and nobody looks at it, and it comes first: make the field nullable and ask for a quote. A wasted retry costs money and time and corrupts nothing, so it comes second. An unchecked total is a missing check, third. An overstated figure is a measurement fault, fourth. A run that has none of these and still misses the target needs its weak kinds improved, and a run that meets the target needs nothing.

The audit counts an unchecked total only among documents accepted as valid. A document that went to a person was looked at, and a document that failed was never delivered, so only an accepted record can carry a total that nobody checked.

### Where the batch and the sample fit

A nightly batch suits documents that nobody is waiting for, and a failed entry is resubmitted by its `custom_id` so that the entries that succeeded are not paid for twice. The records that were accepted without a person still need a stratified random sample, drawn from every kind, because that is the only way to learn how many wrong records the accepted stream holds. An independent second instance, one that has not seen the first one's reasoning, is the check for a claim that the first instance is sure of.

### The practice: the audit of a run

The practice is `exercises/75-scenario-structured-data-extraction/unit-01/practice-1/statement.md`, in Python, TypeScript, Java and Kotlin. You write `audit(runs, policy)`: the counts, the accuracy on every document and on the validated ones, whether the run meets the target, one segment per kind with its right to be automated, the three failure shapes, whether the figure is overstated, and the first fix.

The tests build the log of a run and grade nine cases: a mixed run, an empty run, the target met at exactly the target, the minimum met at exactly the minimum, a kind exactly at the target, a gap exactly met and one point over, the shapes counted once per document, the order of the fixes, and the rounding of percentages. The starter fails all nine, the reference passes them, and each of nine planted wrong solutions per language fails on an assertion of the case it breaks: an accuracy divided by the valid documents, a target that must be exceeded, a kind that must be above the target, a minimum that must be exceeded, a gap that counts when met, an unchecked total counted in documents never accepted, the wrong order of two fixes, a percentage rounded down, and an empty run that meets the target.

## Traps

These are the wrong answers that the exam's options for this scenario offer, each with the reason it is rejected.

1. **"The overall accuracy is 96 percent, so automate every kind."** It is tempting because the figure clears the target. The exam rejects it: an average hides the kind that is weak, so each kind is checked against the target and against a minimum sample.
2. **"Fix the most frequent failure first."** It is tempting because frequency is easy to count. The exam rejects it: the first fix is the one that removes the most damage downstream, and an invented value that nobody checks outranks a retry that only wastes time.
3. **"Count every document with a bad total as an unchecked total."** It is tempting because a bad total is a bad total. The exam rejects it: a document sent to a person was reviewed and a failed one was never delivered, so only accepted records can carry an unchecked total.

## Quiz

1. A kind of document has exactly the minimum number of documents that the policy sets, and every one of them is correct. What does the audit decide about it?
   - **a**: Keep a person on it, since the minimum has to be passed and not only met
   - **b**: Automate it, as the evidence needed is reached and not exceeded
   - **c**: Wait for the run to meet the target before looking at this kind again
   - **d**: Automate it only after an independent instance has rated each of the records

2. The validated accuracy is 100 percent, the all-document accuracy is 95, and the policy sets a gap of five points. Does the audit call the figure overstated?
   - **a**: Yes, because any distance between the two figures is a warning in itself
   - **b**: Yes, since the validated figure is the one that matters to the customer
   - **c**: No, since the difference only reaches the limit and does not pass it
   - **d**: No, because the all-document figure alone is a fair measure of the run

3. An audit finds invented values, wasted retries and unchecked totals in one run. Which fix does it name first, and why?
   - **a**: Add semantic checks, since they catch the largest number of defects in a run
   - **b**: Stop retrying absent values, since that wastes the most money of the three
   - **c**: Let fields be null, since a wrong record that looks right travels on unseen
   - **d**: Measure every document, since the figures cannot be trusted until then

<details>
<summary>Answer key</summary>

1. **b**. Both comparisons are inclusive. *a* is ruled out because reaching the minimum is enough: "A kind with exactly the minimum number is enough, and a kind exactly at the target is ready." *c* is ruled out because the overall figure takes no part: "The overall figure plays no part in this decision". *d* is ruled out because the decision rests on the two conditions and not on a further review: "A kind of document may go without a person only when two things hold".
2. **c**. A difference that reaches the gap is not overstated. *a* is ruled out because small differences are normal: "A small difference is the ordinary cost of doing business." *b* is ruled out because the test is a size and not a viewpoint: "The audit calls the figure overstated only when the difference is greater than a gap that the policy sets". *d* is ruled out because the two figures are reported together: "The validated figure is above the all-document figure whenever anything failed."
3. **c**. The order follows the damage downstream. *a* is ruled out because a missing check comes third: "An unchecked total is a missing check, third." *b* is ruled out because a wasted retry does little harm: "A wasted retry costs money and time and corrupts nothing, so it comes second." *d* is ruled out because the measurement fault comes fourth: "An overstated figure is a measurement fault, fourth."

</details>

## Module quiz

This quiz covers both pages of the module.

1. Scenario S6, structured data extraction, in which a pipeline validates every record and sends the doubtful ones to a person. Typed forms are right 38 times in 40 and handwritten forms 12 times in 20. The policy asks for 90 percent and at least 30 documents. What does the audit conclude?
   - **a**: Both kinds are ready, since the overall figure of the whole set is above the target
   - **b**: Neither kind is ready, since the weaker kind drags the figure of the stronger one down
   - **c**: The smaller kind is ready and the larger is not, since fewer forms mean fewer errors
   - **d**: The larger kind qualifies, while the smaller lacks both sample size and accuracy

2. Scenario S6, structured data extraction, in which a pipeline validates every record and sends the doubtful ones to a person. A colleague asks why the audit counts an unchecked total only in documents accepted as valid. What is the answer?
   - **a**: A reviewed case was seen by someone, and a failed one never went out
   - **b**: Failed documents hold no total at all, so there would be nothing in them to count
   - **c**: Documents sent to a person are counted elsewhere, in the figure for wasted retries
   - **d**: The count would otherwise be larger than the number of documents that the run held

3. Scenario S6, structured data extraction, in which a pipeline validates every record and sends the doubtful ones to a person. The example's by-kind line reads typed 2/2, scanned 1/2 and handwritten 0/2, under a policy of at least two documents and a target of 100 percent. Why is only the typed kind listed for automation?
   - **a**: It is the only kind that has a total printed in every one of its documents
   - **b**: It meets the minimum and the goal, while the others fall short on accuracy
   - **c**: It was the first kind to appear in the run, and the list stops after the first
   - **d**: It is the kind with the most documents behind it in the whole run

4. Scenario S6, structured data extraction, in which a pipeline validates every record and sends the doubtful ones to a person. A run shows a few invented vendor names and a long list of wasted retries. The team wants to begin with the retries, because they are far more frequent. What does the order of fixes say?
   - **a**: Begin with the retries, since frequency is the best guide to what a defect costs overall
   - **b**: Fields come first, as a wrong entry that looks fine goes unseen downstream
   - **c**: Fix both at once, as the two shapes share one cause that sits in the schema
   - **d**: Begin with the measurement, because both of the counts could be wrong in the first place

<details>
<summary>Answer key</summary>

1. **d**. Both conditions are checked per kind. *a* is ruled out because the average is not the test: "The overall figure plays no part in this decision". *b* is ruled out because a kind is judged on its own documents: "A kind of document may go without a person only when two things hold". *c* is ruled out because a small sample is no evidence: "The minimum matters because ten correct documents are a small sample, and the target is a floor and not a ceiling."
2. **a**. Only an accepted record can carry an unchecked total. *b* is ruled out because the reason is who looked and not whether a total exists: "A document that went to a person was looked at, and a document that failed was never delivered". *c* is ruled out because the wasted-retry count is a different shape: "A wasted retry costs money and time and corrupts nothing, so it comes second." *d* is ruled out because the counts are per shape and not capped by the run: "The audit counts an unchecked total only among documents accepted as valid."
3. **b**. The kind needs documents and accuracy. *a* is ruled out because totals are not the test: "A kind of document may go without a person only when two things hold". *c* is ruled out because the order of kinds plays no part and the list is built per kind: "Only typed documents have earned automation." *d* is ruled out because every kind has the same count and the test is documents and accuracy: "The overall figure plays no part in this decision".
4. **b**. Damage downstream sets the order. *a* is ruled out because frequency does not set it: "The order follows the damage a shape does downstream." *c* is ruled out because the audit names one fix first: "A run often shows several failure shapes at once, and the audit names one fix first." *d* is ruled out because the measurement fault comes fourth: "An overstated figure is a measurement fault, fourth."

</details>

Adapted from the sample scenario of the Claude Certified Architect, Foundations exam guide, version 1.0 (Anthropic), with credit; the scenario is Anthropic's. The questions here are written for this course.
