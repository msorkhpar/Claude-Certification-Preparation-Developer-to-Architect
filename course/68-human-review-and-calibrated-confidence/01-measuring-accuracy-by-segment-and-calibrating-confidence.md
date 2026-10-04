# Measuring accuracy by segment, sampling, and making a confidence score honest

**Level:** Architect · **Module 68:** Human review and calibrated confidence · **Page 1 of 2**
**Exams:** A5.5; S6

**After this page you can** explain why one accuracy figure can hide a weak document type or field, check each segment before reducing human review, decide when a segment has too few labelled examples to judge, sample the auto-accepted items by stratum to measure the error rate and find new kinds of error, and calibrate a confidence threshold against a labelled validation set.

Checked on 2026-10-04 against the exam guide's task statement 5.5 and scenario S6 (structured data extraction). Nothing here called a model: the example generates 200 extractions by a fixed rule and does arithmetic over them (`examples/68-calibration-table`), and the practice is graded by test suites. This module builds on module 63, which showed that a model's confidence about its own work carries its blind spots, and on module 65, which said that calibrated confidence is a measured thing. This page is the measurement.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* "aggregate accuracy metrics (e.g., 97% overall) may mask poor performance on specific document types or fields"; accuracy must be validated "by document type and field segment" before high-confidence extractions are automated; "stratified random sampling" of high-confidence extractions measures the error rate and detects novel error patterns; and field-level confidence scores are "calibrated using labeled validation sets" before they route review attention. *What the product offers now:* none of the documentation pages read for modules 61 to 67 describes a built-in facility that measures or calibrates a model's confidence. A confidence value is a field of your own extraction schema that the model fills in, and the segments, the sample and the calibration table are computed over your own labelled data. The method here is therefore the exam's and standard evaluation practice, and the example is arithmetic you can run. On the exam, a question about relaxing human review has the answer: measure per segment first, sample the accepted items by stratum, and calibrate the threshold against labels.

## Why it matters

A team extracts totals and dates from scanned documents and reports 98% accuracy over a validation set. The set is mostly typed invoices. Handwritten delivery notes are one document in ten in production, and the pipeline gets four of them in ten wrong. The overall figure is true and misleading at once: it averages a segment that is nearly perfect with one that is not. The team removes the human reviewers, and the errors that follow are concentrated where the average was not looking. The scenario (S6) tests whether the design measures where it matters before it takes the safety net away.

## The idea

### One figure hides the weak segment

An overall accuracy is an average weighted by how many records each segment contributes. A large, easy segment dominates it. The remedy is to compute the figure for every segment that could behave differently, at the least each document type and each field, and to read the lowest one next to the overall number. The practice's `accuracy_by` returns the overall row followed by a row for every `type/field` pair for exactly that reason: the second table is the one that finds the problem.

A bigger validation set of the same mix gives the same hidden figure, only with more decimals. And a handful of random documents are mostly typed invoices, because random samples reflect the mix. What changes the picture is looking at the segments one at a time.

### A segment needs enough evidence

A segment that scores 100% on four examples has shown very little. Before automating a segment, require a minimum number of labelled examples as well as a minimum accuracy. The practice's `can_automate` separates the two failures: a segment below the threshold is `failing`, and a segment with fewer than `min_n` labelled records is `undersampled`. Both stop automation, and they call for different actions: fix the pipeline for the first, label more examples for the second. The overall figure plays no part in the decision. A segment is automated when it, itself, has been shown to work.

### Sample the accepted items by stratum

Suppose a segment is automated, and its high-confidence extractions go through without a person. Two things can still go wrong: the true error rate among accepted items is unknown, and new kinds of error appear when document formats change. Both are measured the same way: keep checking a sample of the accepted items.

The sample must be random within each stratum (a stratum is a document type, a field, or any slice you want to guarantee coverage of), and it must take items from every stratum. A single random pull from the whole stream is mostly easy cases and says almost nothing about the small, hard strata. Drawing the same number per stratum means a rare segment is checked as often as a common one, so a decay in it shows up. The practice's `stratified_sample` takes a fixed number per stratum. It picks the lowest-ranked items in each, which makes the result testable. In production the pick within a stratum is random, and the point of the stratification is the same.

Checking the accepted sample does two jobs. The share found wrong is the measured error rate of automation, and a wrong item that fits no known pattern is a novel error, which is the signal to change the pipeline.

### Calibrating a confidence threshold

The model can be asked to output a confidence for each field. The number is only a score: whether 90 means "right nine times in ten" is an empirical question. Calibration answers it with a labelled validation set, a set where the right answer is known:

1. Group the labelled items by the confidence they were given.
2. For a candidate threshold, take every item at or above it, and measure how many are right. This is the precision of automation at that threshold.
3. Pick the lowest threshold whose precision reaches the target. Everything below it goes to a person.

The lowest qualifying threshold automates the most work while meeting the target. If no threshold reaches the target, there is no honest cut-off: every item needs review until the pipeline improves. The practice's `calibrate_threshold` is these three steps, and returns nothing when none qualifies. A calibration table (confidence bucket, what it claimed, how often it was right) shows where the score is overconfident, and the gap between the two columns is the point of the exercise.

### The example

<!-- example: m68-calibration-table tabs: python,typescript,java,kotlin -->
<!-- /example -->

The records are generated by a fixed rule, and nothing calls a model. The overall accuracy sits between the two document types, a good-looking figure that describes neither: the handwritten type is well below it and the invoices above it. The calibration table lists the buckets, and the last block shows the cost of automating at each threshold: a higher threshold accepts fewer items and is right more often. The numbers are invented for the illustration, and they describe this generated set only; the point is the method, and the figures that matter are the ones computed over your own labelled data.

## Traps

1. **"The accuracy is 97%, so reduce the reviewers."** It is tempting because the number is high and true. The exam rejects it: an aggregate hides the weak document type or field, so measure each segment before relaxing review.
2. **"Take a random sample of all the accepted items."** It is tempting because random sampling sounds rigorous. The exam rejects it: one pull from the stream is mostly easy cases. Stratify, so that every document type and field is sampled.
3. **"A score of 90 means it is right 90% of the time."** It is tempting because the number looks like a probability. The exam rejects it: the model wrote the number, and only a labelled validation set says how often that score is right.
4. **"A segment with all its examples correct is safe to automate."** It is tempting because 100% is the best possible score. The exam rejects it: a handful of examples is too little evidence, so such a segment needs more labels first.

## Quiz

1. Scenario S6, structured data extraction. A pipeline gets 97% of extracted values right across a validation set that is mostly typed invoices, and the team plans to drop human checks on all of them. What should happen first?
   - **a**: Break the results down by document kind and by field name before relaxing oversight
   - **b**: Confirm that the model reports high confidence on most of the validation set
   - **c**: Double the validation set and confirm that the overall figure holds up
   - **d**: Ask the reviewers to confirm the model's answers on a handful of randomly chosen documents

2. Scenario S6, structured data extraction. Everything scored 90 or above is accepted untouched. The team wants the true wrong-rate of that stream and an early warning when a supplier changes its layout. Which sampling fits?
   - **a**: Pull random documents from the whole stream in a single draw, with no regard to kind
   - **b**: Pull the documents that scored just under the cut-off every week
   - **c**: Pull random documents of each kind that passed with no human look
   - **d**: Pull one fixed set of old documents on a regular schedule

<details>
<summary>Answer key</summary>

1. **a**. The overall figure is an average that the large easy segment dominates, so each document type and field is measured on its own before any review is removed. *b* is ruled out because "the model wrote the number, and only a labelled validation set says how often that score is right". *c* is ruled out because "A bigger validation set of the same mix gives the same hidden figure". *d* is ruled out because "a handful of random documents are mostly typed invoices, because random samples reflect the mix".
2. **c**. A random draw inside every stratum of the unreviewed stream measures the wrong-rate of automation and exposes a new pattern, as the page puts it: "Drawing the same number per stratum means a rare segment is checked as often as a common one". *b* is ruled out because "Everything below it goes to a person", so those documents are reviewed already. *a* is ruled out because "A single random pull from the whole stream is mostly easy cases". *d* is ruled out because "new kinds of error appear when document formats change", and a fixed set only repeats what is known.

</details>

Adapted from the Claude Certified Architect - Foundations exam preparation guide by Daron Yondem (CC BY 4.0, https://creativecommons.org/licenses/by/4.0/), changed for this course.
