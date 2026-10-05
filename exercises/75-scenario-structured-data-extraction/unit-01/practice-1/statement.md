# Practice: audit an extraction run

A team extracts the vendor, the currency and the totals from invoices of three kinds: typed, scanned and handwritten. The pipeline forces an extraction tool, validates every record, retries once with
feedback and sends what it cannot settle to a person. Its dashboard says 99 percent accurate, and the reviewers say the handwritten forms are a disaster. In this practice you write the audit that
settles the argument: the accuracy on every document next to the accuracy on the validated ones, the accuracy of each kind of document and whether that kind may be automated, the failure shapes the
run shows and the first fix to make. The model is not called: the tests give you the log of a run. The shapes of the log, of the policy and of the report are this course's own design for the
capstone, not an Anthropic interface. It is in Python, TypeScript, Java and Kotlin; pick your language folder, open `starter/` and edit the file there.

Names are Python's (`audit`); TypeScript has the same name and the same snake-case fields. Java has the static method `RunAudit.audit` and the records `Run`, `Policy`, `Segment` and `Report` with
camel-case fields (`retriedAbsent`, `sumOk`, `minN`, `needsReview`, `accuracyAll`); Kotlin has the top-level function `audit` and the data classes of the same names.

## What to write

A run is a list of documents `{id, kind, status, correct, invented, retried_absent, sum_ok}`: `status` is `valid`, `needs_review` or `failed`; `correct` says that the record the pipeline delivered
matches the label (a document that was not delivered is not correct); `invented` says that a value was not in the document; `retried_absent` says that a retry was sent for a value the document does not
contain; `sum_ok` says that the record's totals agree with its line items. The policy is `{target, min_n, gap}`, whole numbers: the target accuracy in percent, the fewest documents of a kind that
may justify automating it, and the gap in percentage points.

`audit(runs, policy)` returns a report with:

- `n`, `valid`, `needs_review`, `failed`: the counts.
- `accuracy_all`: the percentage of all documents that are correct. `accuracy_validated`: the percentage of the `valid` documents that are correct (0 when none is valid). Percentages are whole numbers
  rounded half up: `(200 * correct + total) // (2 * total)`, and 0 when the total is 0.
- `meets_target`: true when the run has at least one document and `correct * 100 >= target * n`.
- `segments`: one entry per kind, sorted by kind, `{kind, n, correct, percent, automate}`. `automate` is true when the kind has at least `min_n` documents and `correct * 100 >= target * n` for that kind.
- `invented`, `wasted_retries`: the number of documents flagged `invented` and `retried_absent`. `unchecked_totals`: the number of `valid` documents whose `sum_ok` is false (a document that was never
  accepted is not counted).
- `overstated`: true when `accuracy_validated - accuracy_all` is greater than `gap`.
- `first_fix`: `none` for an empty run; otherwise the first that applies of `make_fields_nullable` (any invented value), `stop_retrying_absent` (any wasted retry), `add_semantic_checks` (any unchecked
  total), `measure_all_documents` (the figure is overstated), `improve_weak_segments` (the run does not meet the target) and `none`.

## Why each part is there, and what you should see

1. **Measure every document.** A document that failed validation is still a document the customer sent. *You should see* 100 percent on the validated ones next to 57 percent on all of them, and the
   gap named as the figure being overstated.
2. **A target is met at the target.** The goal is "at least", so a run exactly on it meets it. *You should see* 9 correct documents of 10 meet a target of 90, and 8 of 10 do not.
3. **Automate a kind only with evidence.** Too few documents prove nothing, and a kind below the target is not ready. *You should see* a kind with exactly `min_n` documents automated, one with one
   fewer held back, and 17 of 19 refused while 9 of 10 pass.
4. **Name the shape, then order the fixes.** An invented value is a schema fault, a wasted retry a loop fault, an unchecked total a missing check. *You should see* the cheapest fix that removes the
   most damage first.

## The cases

| Id | What it checks |
|---|---|
| `m1` | A mixed run gets every count, both accuracies, the segments and the first fix |
| `e1` | An empty run has zero figures, no segments and never meets the target |
| `e2` | The run meets the target at exactly the target and not below it |
| `e3` | A kind needs at least the minimum number of documents to be automated |
| `e4` | A kind is automated at exactly the target accuracy and not below it |
| `e5` | The figure is overstated only when the gap is exceeded, not when it is met |
| `e6` | Each shape counts documents once, and an unchecked total counts only valid documents |
| `e7` | The first fix follows the order of what costs most |
| `e8` | Percentages are whole numbers rounded half up |

Run the tests with the command in the language folder's `run.sh` (Python, TypeScript) or its build file (Java, Kotlin).
