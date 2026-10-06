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
```python
"""Reading a review process honestly: one accuracy figure hides the weak segment, and a confidence score is only worth what a calibration table says it is.

The exam guide (task 5.5) says that stratified random sampling of high-confidence extractions measures the error rate and catches new patterns, that aggregate accuracy can mask poor performance on specific document types or fields,
and that field-level confidence scores should be calibrated using labelled validation sets. Below, 200 extractions are generated by a fixed rule (nothing here calls a model): the invoices are mostly right, the handwritten
documents are wrong more often, and the confidence says one thing while the labels say another. The numbers are invented for the illustration; the arithmetic is the lesson.
"""
import logging

log = logging.getLogger(__name__)


def make_records(n=200):
    """(document type, stated confidence, was it correct) for n extractions, from a fixed rule."""
    records = []
    for i in range(n):
        doc_type = "handwritten" if i % 5 == 0 else "invoice"
        confidence = 50 + (i * 37) % 51
        penalty = 30 if doc_type == "handwritten" else 0
        records.append((doc_type, confidence, (i * 53) % 100 < confidence - 12 - penalty))
    return records


def ratio(part, whole):
    """A whole-number percentage, rounded half up; 0 for an empty whole."""
    return (200 * part + whole) // (2 * whole) if whole else 0


def overall_accuracy(records):
    return ratio(sum(1 for _, _, ok in records if ok), len(records))


def accuracy_by_type(records):
    groups = {}
    for doc_type, _, ok in records:
        c, n = groups.get(doc_type, (0, 0))
        groups[doc_type] = (c + (1 if ok else 0), n + 1)
    return [(t, c, n, ratio(c, n)) for t, (c, n) in sorted(groups.items())]


def calibration_table(records):
    """One row per non-empty confidence bucket: (label, count, confidence it claimed, how often it was right)."""
    buckets = {}
    for _, confidence, ok in records:
        low = min(confidence // 10 * 10, 90)
        count, conf_sum, right = buckets.get(low, (0, 0, 0))
        buckets[low] = (count + 1, conf_sum + confidence, right + (1 if ok else 0))
    rows = []
    for low in sorted(buckets):
        count, conf_sum, right = buckets[low]
        label = "90-100" if low == 90 else f"{low}-{low + 9}"
        rows.append((label, count, ratio(conf_sum, 100 * count), ratio(right, count)))
    return rows


def precision_above(records, threshold):
    """(how many extractions would be accepted without review, how often those are right)."""
    kept = [ok for _, confidence, ok in records if confidence >= threshold]
    return len(kept), ratio(sum(1 for ok in kept if ok), len(kept))


def main():
    records = make_records()
    print(f"overall accuracy: {overall_accuracy(records)}%")
    for doc_type, correct, total, percent in accuracy_by_type(records):
        print(f"{doc_type}: {correct} of {total} correct, {percent}%")
    for label, count, said, right in calibration_table(records):
        print(f"bucket {label}: {count} extractions, said {said}%, right {right}%")
    for threshold in (60, 70, 80, 90):
        kept, precision = precision_above(records, threshold)
        print(f"accept at {threshold} or above: {kept} extractions, {precision}% right")


if __name__ == "__main__":
    main()
```
```text
overall accuracy: 57%
handwritten: 13 of 40 correct, 33%
invoice: 100 of 160 correct, 63%
bucket 50-59: 39 extractions, said 54%, right 41%
bucket 60-69: 39 extractions, said 65%, right 54%
bucket 70-79: 39 extractions, said 74%, right 46%
bucket 80-89: 40 extractions, said 85%, right 60%
bucket 90-100: 43 extractions, said 95%, right 79%
accept at 60 or above: 161 extractions, 60% right
accept at 70 or above: 122 extractions, 62% right
accept at 80 or above: 83 extractions, 70% right
accept at 90 or above: 43 extractions, 79% right
```
```typescript
import { logger } from "./logger.ts";
const log = logger("calibration_table");
/**
 * Reading a review process honestly: one accuracy figure hides the weak segment, and a confidence score is only worth what a calibration table says it is.
 *
 * The exam guide (task 5.5) says that stratified random sampling of high-confidence extractions measures the error rate and catches new patterns, that aggregate accuracy can mask poor performance on specific document types or fields,
 * and that field-level confidence scores should be calibrated using labelled validation sets. Below, 200 extractions are generated by a fixed rule (nothing here calls a model): the invoices are mostly right, the handwritten
 * documents are wrong more often, and the confidence says one thing while the labels say another. The numbers are invented for the illustration; the arithmetic is the lesson.
 */
export type Rec = [string, number, boolean];

/** (document type, stated confidence, was it correct) for n extractions, from a fixed rule. */
export function makeRecords(n = 200): Rec[] {
  const records: Rec[] = [];
  for (let i = 0; i < n; i++) {
    const docType = i % 5 === 0 ? "handwritten" : "invoice";
    const confidence = 50 + ((i * 37) % 51);
    const penalty = docType === "handwritten" ? 30 : 0;
    records.push([docType, confidence, (i * 53) % 100 < confidence - 12 - penalty]);
  }
  return records;
}

/** A whole-number percentage, rounded half up; 0 for an empty whole. */
export function ratio(part: number, whole: number): number {
  return whole ? Math.floor((200 * part + whole) / (2 * whole)) : 0;
}

export function overallAccuracy(records: Rec[]): number {
  return ratio(records.filter(([, , ok]) => ok).length, records.length);
}

export function accuracyByType(records: Rec[]): Array<[string, number, number, number]> {
  const groups = new Map<string, [number, number]>();
  for (const [docType, , ok] of records) {
    const [c, n] = groups.get(docType) ?? [0, 0];
    groups.set(docType, [c + (ok ? 1 : 0), n + 1]);
  }
  return [...groups.keys()].sort().map((t) => [t, groups.get(t)![0], groups.get(t)![1], ratio(groups.get(t)![0], groups.get(t)![1])]);
}

/** One row per non-empty confidence bucket: (label, count, confidence it claimed, how often it was right). */
export function calibrationTable(records: Rec[]): Array<[string, number, number, number]> {
  const buckets = new Map<number, [number, number, number]>();
  for (const [, confidence, ok] of records) {
    const low = Math.min(Math.floor(confidence / 10) * 10, 90);
    const [count, confSum, right] = buckets.get(low) ?? [0, 0, 0];
    buckets.set(low, [count + 1, confSum + confidence, right + (ok ? 1 : 0)]);
  }
  return [...buckets.keys()].sort((a, b) => a - b).map((low) => {
    const [count, confSum, right] = buckets.get(low)!;
    return [low === 90 ? "90-100" : `${low}-${low + 9}`, count, ratio(confSum, 100 * count), ratio(right, count)];
  });
}

/** (how many extractions would be accepted without review, how often those are right). */
export function precisionAbove(records: Rec[], threshold: number): [number, number] {
  const kept = records.filter(([, confidence]) => confidence >= threshold);
  return [kept.length, ratio(kept.filter(([, , ok]) => ok).length, kept.length)];
}

export function main(): void {
  const records = makeRecords();
  console.log(`overall accuracy: ${overallAccuracy(records)}%`);
  for (const [docType, correct, total, percent] of accuracyByType(records)) console.log(`${docType}: ${correct} of ${total} correct, ${percent}%`);
  for (const [label, count, said, right] of calibrationTable(records)) console.log(`bucket ${label}: ${count} extractions, said ${said}%, right ${right}%`);
  for (const threshold of [60, 70, 80, 90]) {
    const [kept, precision] = precisionAbove(records, threshold);
    console.log(`accept at ${threshold} or above: ${kept} extractions, ${precision}% right`);
  }
}

if (import.meta.main) main();
```
```text
overall accuracy: 57%
handwritten: 13 of 40 correct, 33%
invoice: 100 of 160 correct, 63%
bucket 50-59: 39 extractions, said 54%, right 41%
bucket 60-69: 39 extractions, said 65%, right 54%
bucket 70-79: 39 extractions, said 74%, right 46%
bucket 80-89: 40 extractions, said 85%, right 60%
bucket 90-100: 43 extractions, said 95%, right 79%
accept at 60 or above: 161 extractions, 60% right
accept at 70 or above: 122 extractions, 62% right
accept at 80 or above: 83 extractions, 70% right
accept at 90 or above: 43 extractions, 79% right
```
```java
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Reading a review process honestly: one accuracy figure hides the weak segment, and a confidence score is only worth what a calibration table says it is.
 *
 * <p>The exam guide (task 5.5) says that stratified random sampling of high-confidence extractions measures the error rate and catches new patterns, that aggregate accuracy can mask poor performance on specific document types or fields,
 * and that field-level confidence scores should be calibrated using labelled validation sets. Below, 200 extractions are generated by a fixed rule (nothing here calls a model): the invoices are mostly right, the handwritten
 * documents are wrong more often, and the confidence says one thing while the labels say another. The numbers are invented for the illustration; the arithmetic is the lesson.
 */
public final class CalibrationTable {
    private static final System.Logger LOG = System.getLogger(CalibrationTable.class.getName());
    record Rec(String docType, int confidence, boolean correct) {}

    record TypeRow(String docType, int correct, int total, int percent) {}

    record Bucket(String label, int count, int said, int right) {}

    record Kept(int count, int precision) {}

    /** (document type, stated confidence, was it correct) for n extractions, from a fixed rule. */
    static List<Rec> makeRecords(int n) {
        List<Rec> records = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            String docType = i % 5 == 0 ? "handwritten" : "invoice";
            int confidence = 50 + (i * 37) % 51;
            int penalty = docType.equals("handwritten") ? 30 : 0;
            records.add(new Rec(docType, confidence, (i * 53) % 100 < confidence - 12 - penalty));
        }
        return records;
    }

    /** A whole-number percentage, rounded half up; 0 for an empty whole. */
    static int ratio(int part, int whole) {
        return whole > 0 ? (200 * part + whole) / (2 * whole) : 0;
    }

    static int overallAccuracy(List<Rec> records) {
        return ratio((int) records.stream().filter(Rec::correct).count(), records.size());
    }

    static List<TypeRow> accuracyByType(List<Rec> records) {
        Map<String, int[]> groups = new TreeMap<>();
        for (Rec r : records) {
            int[] g = groups.computeIfAbsent(r.docType(), k -> new int[2]);
            if (r.correct()) g[0]++;
            g[1]++;
        }
        List<TypeRow> rows = new ArrayList<>();
        for (Map.Entry<String, int[]> e : groups.entrySet()) rows.add(new TypeRow(e.getKey(), e.getValue()[0], e.getValue()[1], ratio(e.getValue()[0], e.getValue()[1])));
        return rows;
    }

    /** One row per non-empty confidence bucket: label, count, confidence it claimed, how often it was right. */
    static List<Bucket> calibrationTable(List<Rec> records) {
        Map<Integer, int[]> buckets = new TreeMap<>();
        for (Rec r : records) {
            int[] b = buckets.computeIfAbsent(Math.min(r.confidence() / 10 * 10, 90), k -> new int[3]);
            b[0]++;
            b[1] += r.confidence();
            if (r.correct()) b[2]++;
        }
        List<Bucket> rows = new ArrayList<>();
        for (Map.Entry<Integer, int[]> e : buckets.entrySet()) {
            int low = e.getKey();
            int[] b = e.getValue();
            rows.add(new Bucket(low == 90 ? "90-100" : low + "-" + (low + 9), b[0], ratio(b[1], 100 * b[0]), ratio(b[2], b[0])));
        }
        return rows;
    }

    /** How many extractions would be accepted without review, and how often those are right. */
    static Kept precisionAbove(List<Rec> records, int threshold) {
        int kept = 0;
        int right = 0;
        for (Rec r : records) {
            if (r.confidence() >= threshold) {
                kept++;
                if (r.correct()) right++;
            }
        }
        return new Kept(kept, ratio(right, kept));
    }

    public static void main(String[] args) {
        List<Rec> records = makeRecords(200);
        System.out.println("overall accuracy: " + overallAccuracy(records) + "%");
        for (TypeRow t : accuracyByType(records)) System.out.println(t.docType() + ": " + t.correct() + " of " + t.total() + " correct, " + t.percent() + "%");
        for (Bucket b : calibrationTable(records)) System.out.println("bucket " + b.label() + ": " + b.count() + " extractions, said " + b.said() + "%, right " + b.right() + "%");
        for (int threshold : new int[] {60, 70, 80, 90}) {
            Kept k = precisionAbove(records, threshold);
            System.out.println("accept at " + threshold + " or above: " + k.count() + " extractions, " + k.precision() + "% right");
        }
    }
}
```
```text
overall accuracy: 57%
handwritten: 13 of 40 correct, 33%
invoice: 100 of 160 correct, 63%
bucket 50-59: 39 extractions, said 54%, right 41%
bucket 60-69: 39 extractions, said 65%, right 54%
bucket 70-79: 39 extractions, said 74%, right 46%
bucket 80-89: 40 extractions, said 85%, right 60%
bucket 90-100: 43 extractions, said 95%, right 79%
accept at 60 or above: 161 extractions, 60% right
accept at 70 or above: 122 extractions, 62% right
accept at 80 or above: 83 extractions, 70% right
accept at 90 or above: 43 extractions, 79% right
```
```kotlin
private val log = System.getLogger("calibration_table")

/**
 * Reading a review process honestly: one accuracy figure hides the weak segment, and a confidence score is only worth what a calibration table says it is.
 *
 * The exam guide (task 5.5) says that stratified random sampling of high-confidence extractions measures the error rate and catches new patterns, that aggregate accuracy can mask poor performance on specific document types or fields,
 * and that field-level confidence scores should be calibrated using labelled validation sets. Below, 200 extractions are generated by a fixed rule (nothing here calls a model): the invoices are mostly right, the handwritten
 * documents are wrong more often, and the confidence says one thing while the labels say another. The numbers are invented for the illustration; the arithmetic is the lesson.
 */
data class Rec(val docType: String, val confidence: Int, val correct: Boolean)

data class TypeRow(val docType: String, val correct: Int, val total: Int, val percent: Int)

data class Bucket(val label: String, val count: Int, val said: Int, val right: Int)

data class Kept(val count: Int, val precision: Int)

/** (document type, stated confidence, was it correct) for n extractions, from a fixed rule. */
fun makeRecords(n: Int = 200): List<Rec> = List(n) { i ->
    val docType = if (i % 5 == 0) "handwritten" else "invoice"
    val confidence = 50 + (i * 37) % 51
    val penalty = if (docType == "handwritten") 30 else 0
    Rec(docType, confidence, (i * 53) % 100 < confidence - 12 - penalty)
}

/** A whole-number percentage, rounded half up; 0 for an empty whole. */
fun ratio(part: Int, whole: Int): Int = if (whole > 0) (200 * part + whole) / (2 * whole) else 0

fun overallAccuracy(records: List<Rec>): Int = ratio(records.count { it.correct }, records.size)

fun accuracyByType(records: List<Rec>): List<TypeRow> =
    records.groupBy { it.docType }.toSortedMap().map { (t, rs) -> TypeRow(t, rs.count { it.correct }, rs.size, ratio(rs.count { it.correct }, rs.size)) }

/** One row per non-empty confidence bucket: label, count, confidence it claimed, how often it was right. */
fun calibrationTable(records: List<Rec>): List<Bucket> =
    records.groupBy { minOf(it.confidence / 10 * 10, 90) }.toSortedMap().map { (low, rs) ->
        Bucket(if (low == 90) "90-100" else "$low-${low + 9}", rs.size, ratio(rs.sumOf { it.confidence }, 100 * rs.size), ratio(rs.count { it.correct }, rs.size))
    }

/** How many extractions would be accepted without review, and how often those are right. */
fun precisionAbove(records: List<Rec>, threshold: Int): Kept {
    val kept = records.filter { it.confidence >= threshold }
    return Kept(kept.size, ratio(kept.count { it.correct }, kept.size))
}

fun main() {
    val records = makeRecords()
    println("overall accuracy: ${overallAccuracy(records)}%")
    for (t in accuracyByType(records)) println("${t.docType}: ${t.correct} of ${t.total} correct, ${t.percent}%")
    for (b in calibrationTable(records)) println("bucket ${b.label}: ${b.count} extractions, said ${b.said}%, right ${b.right}%")
    for (threshold in listOf(60, 70, 80, 90)) {
        val k = precisionAbove(records, threshold)
        println("accept at $threshold or above: ${k.count} extractions, ${k.precision}% right")
    }
}
```
```text
overall accuracy: 57%
handwritten: 13 of 40 correct, 33%
invoice: 100 of 160 correct, 63%
bucket 50-59: 39 extractions, said 54%, right 41%
bucket 60-69: 39 extractions, said 65%, right 54%
bucket 70-79: 39 extractions, said 74%, right 46%
bucket 80-89: 40 extractions, said 85%, right 60%
bucket 90-100: 43 extractions, said 95%, right 79%
accept at 60 or above: 161 extractions, 60% right
accept at 70 or above: 122 extractions, 62% right
accept at 80 or above: 83 extractions, 70% right
accept at 90 or above: 43 extractions, 79% right
```
<!-- /example -->

The records are generated by a fixed rule, and nothing calls a model. The overall accuracy is 57%, a figure that describes neither type: the handwritten type is at 33% and the invoices at 63%. The calibration table shows that the score overstates: the top bucket said 95% and was right 79% of the time, and every other bucket says more than it delivers. The last block shows the cost of automating at each threshold: accepting only items scored 90 or above leaves 43 of the 200 extractions and is right 79% of the time, and none of the four thresholds reaches 90%, so for a target of 90% the honest answer here is that there is no cut-off yet. The numbers are invented for the illustration, and they describe this generated set only; the point is the method, and the figures that matter are the ones computed over your own labelled data.

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
