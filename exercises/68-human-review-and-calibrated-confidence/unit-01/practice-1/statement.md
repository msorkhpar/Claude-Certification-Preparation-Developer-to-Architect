# Practice: human review that does not fool itself

A team extracts fields from documents and reports "98% accurate" to justify removing the human reviewers. The figure is an average over thousands of easy invoices; handwritten documents are wrong
four times in ten. In this practice you write the pieces that make a review process honest: accuracy broken down by segment, the decision whether a segment may be automated, a confidence
threshold calibrated against labelled examples, a stratified sample for ongoing checks, routing of uncertain extractions to a limited number of reviewers, and a checkpoint for actions that
cannot be undone. The model is not called. It is in Python, TypeScript, Java and Kotlin; pick your language folder, open `starter/` and edit the file there.

Names are Python's (`accuracy_by`, `can_automate`, `calibrate_threshold`, `stratified_sample`, `route`, `checkpoint`); TypeScript has the camel-case names; Java has the same camel-case names as
static methods of `ReviewRouting` with the records the starter defines (`Rec`, `Seg`, `Automation`, `Labeled`, `Item`, `Extraction`, `Routing`); Kotlin has top-level functions and data classes.
Percentages are whole numbers, rounded half up (`(200 * correct + total) // (2 * total)`).

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
