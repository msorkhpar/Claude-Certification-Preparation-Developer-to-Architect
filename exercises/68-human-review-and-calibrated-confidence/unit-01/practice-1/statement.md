# Practice: human review that does not fool itself

A team extracts fields from documents and reports "98% accurate" to justify removing the human reviewers. The figure is an average over thousands of easy invoices; handwritten documents are wrong
four times in ten. In this practice you write the pieces that make a review process honest: accuracy broken down by segment, the decision whether a segment may be automated, a confidence
threshold calibrated against labelled examples, a stratified sample for ongoing checks, routing of uncertain extractions to a limited number of reviewers, and a checkpoint for actions that
cannot be undone. The model is not called. It is in Python, TypeScript, Java and Kotlin; pick your language folder, open `starter/` and edit the file there.

Names are Python's (`accuracy_by`, `can_automate`, `calibrate_threshold`, `stratified_sample`, `route`, `checkpoint`); TypeScript has the camel-case names; Java has the same camel-case names as
static methods of `ReviewRouting` with the records the starter defines (`Rec`, `Seg`, `Automation`, `Labeled`, `Item`, `Extraction`, `Routing`); Kotlin has top-level functions and data classes.
Percentages are whole numbers, rounded half up (`(200 * correct + total) // (2 * total)`).

## What is already written, and what you write

The starter is a working review router with seven gaps cut out of it. Everything that is plumbing is written and correct: the rounding of a percentage, the overall figure, the order of the strata, the sorting of the stratum members, the set of irreversible actions and the shape of every answer. Each gap is marked `TODO k of N` with a comment that says what it receives and returns, with one example, and the cases it unlocks. A gap leaves a neutral value (nothing added, an empty list, `null`, the unchanged input), so the starter runs and fails the cases on an assertion. To debug a gap, log its input with the `log` line at the top of the file: a run shows the logged lines under the failing case. Write the gaps in this order (the TypeScript, Java and Kotlin names are the camel-case forms where a name is given):

1. The accuracy by segment (unlocks `m1`, `e1`): after the overall figure, one entry for each `document type/field` segment, sorted by name, with its correct count, its total and its rounded percentage, so that a weak segment shows next to a high overall figure.
2. The segments that stop automation (unlocks `e2`): a segment with fewer samples than `min_n` is undersampled, and one with enough samples whose percentage is below the threshold is failing; automation needs every segment to pass.
3. The calibrated threshold (unlocks `e3`, `e4`): the lowest confidence whose accepted items (confidence at or above it) have a precision of at least the target; none when no confidence level meets the target.
4. The best of every stratum (unlocks `e5`): the sample takes the first `per_stratum` items of each stratum by rank (then id), so every stratum is represented by its best ranked items.
5. What goes to review (unlocks `e6`): an extraction goes to review when it has a conflict or its confidence is below the threshold; conflicts come first, then the lowest confidence, then by id.
6. The review capacity (unlocks `e7`): no more than `capacity` items go to review, the first ones of the queue; the rest wait in the backlog, in order.
7. The checkpoint (unlocks `e8`): an irreversible action (`delete_records`, `send_payment`, `close_account`) needs a `human` whatever the confidence; so does an amount above the limit; any other action is `auto`.

`m1` needs gap 1. About twelve lines in all. The steps below describe the whole router, so you can see how your gaps are used.

## What to write

- `accuracy_by(records)` takes records `{doc_type, field, correct}` and returns a list of `{segment, correct, total, percent}`: first the segment `overall`, then one segment per `doc_type/field`
  in alphabetical order. No records give an `overall` segment with `0` for the percentage and no other segment.
- `can_automate(records, threshold, min_n)` returns `{automate, failing, undersampled}`. A segment with fewer than `min_n` records is `undersampled`; any other segment below `threshold` percent is
  `failing`. `automate` is true only when there is at least one segment and neither list has anything in it. The overall figure plays no part.
- `calibrate_threshold(labeled, target)` takes `(confidence, correct)` pairs and returns the lowest confidence value `t` such that, among the pairs with confidence of at least `t`, the share that
  is correct is at least `target` percent (compare without division). When no value qualifies, it returns `None` (`null`).
- `stratified_sample(items, per_stratum)` takes items `{id, stratum, rank}` and returns the ids of the `per_stratum` items with the lowest rank in every stratum (ties by id), strata in the order
  they first appear, items of a stratum together.
- `route(extractions, threshold, capacity)` takes `{id, confidence, conflict}` and returns `{review, backlog, auto}`. An extraction needs review when it has a conflict or its confidence is below
  the threshold. The queue puts conflicts first, then the lowest confidence first, ties by id. The first `capacity` of the queue are `review`, the rest `backlog`; everything else is `auto`, in input order.
- `checkpoint(action, amount, limit=1000)` returns `human` for the actions `delete_records`, `send_payment` and `close_account` whatever the amount, and for any action above the limit; otherwise `auto`.

## Why each part is there, and what you should see

1. **Break the figure down.** An average hides the segment that matters. *You should see* the overall figure next to one row per document type and field.
2. **Automate a segment only when it is proven.** A good average and ten samples prove nothing. *You should see* a segment with too few records held back, and a weak one named.
3. **Calibrate against labels.** A confidence score is only useful where it matches how often the answer is right. *You should see* the lowest cut-off that still meets the target, and no cut-off when none does.
4. **Sample every stratum.** A random pull of the easy cases says nothing about the hard ones. *You should see* the same number of items from every stratum.
5. **Spend scarce attention on the weakest.** Reviewers are few. *You should see* conflicts and low confidence first, and the overflow waiting in a backlog.
6. **Some actions need a person.** Confidence does not undo a deleted record. *You should see* `human` for those actions at any amount.

## The cases

| Id | What it checks |
|---|---|
| `m1` | Accuracy is reported per document type and field next to the overall figure |
| `e1` | A weak segment is hidden by a high overall figure and found by the breakdown |
| `e2` | Automation needs every segment to pass and enough samples in each |
| `e3` | The threshold is the lowest confidence whose accepted items meet the target precision |
| `e4` | No threshold exists when no confidence level meets the target |
| `e5` | The stratified sample takes the best ranked items of every stratum |
| `e6` | Low confidence and conflicts go to review with the weakest first |
| `e7` | Review capacity is respected and the rest wait in a backlog |
| `e8` | An irreversible action needs a person whatever the confidence |

Run the tests with the command in the language folder's `run.sh` (Python, TypeScript) or its build file (Java, Kotlin).
