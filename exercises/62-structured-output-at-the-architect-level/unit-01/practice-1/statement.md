# Practice: an extraction pipeline that admits absence, checks what a schema cannot, retries with feedback and is measured on every document

A team extracts the vendor, the currency and the totals from invoices. The schema makes every field required, so when an invoice names no vendor the model invents
one; a retry loop repeats the same request ten times for a value the document never contained; the accuracy report counts only the documents that passed
validation and says 99 percent. In this practice you write the pipeline's decisions: the validation of a record (what a schema cannot check), the extraction loop with
retries that carry feedback, the merge of the chunks of a long document, the accuracy measured on every document, and the tool choice of the request. The model is not
called: the tests give you documents, scripted model replies and labels. It is in Python, TypeScript, Java and Kotlin; pick your language folder, open `starter/` and
edit the file there.

Names are Python's (`validate`, `extract_document`, `merge_chunks`, `accuracy`, `request_choice`); TypeScript has the camel-case names (`extractDocument`, `mergeChunks`,
`requestChoice`); Java has the same camel-case names as static methods of `Extraction`; Kotlin has top-level functions. Results are maps and lists, as the starters show.
The model reply is a function `call_model(document, feedback)`.

## What is already written, and what you write

The starter is a working pipeline with ten gaps cut out of it. Everything that is plumbing is written and correct: the type and enum checks of
`validate`, the loop of `extract_document` with its retry limit and feedback, the chunk merge, the counting of `accuracy` and the model list. Each
gap is a small function with its signature, a comment that says what it receives and returns with one example, and the cases it unlocks. A gap
returns a neutral value, so the starter runs and fails the cases on an assertion. Write them in this order (the TypeScript, Java and Kotlin
names are the camel-case forms):

1. `_quote_found` unlocks `e1`: a quote supports a value only when it is a non-empty string found in the document.
2. `_currency_ok` unlocks `e6`: the currency values the schema allows.
3. `_detail_missing` unlocks `e6`: `other` needs a detail.
4. `_retryable` unlocks `e2`, `e3` and `e4`: which errors a second look can fix.
5. `_status` unlocks `m1`, `e3` and `e5`: `valid`, `needs_review` or `failed`.
6. `_is_unset` unlocks `e7`: a merged field with no real value yet.
7. `_is_conflict` unlocks `e7`: two chunks that disagree, recorded once.
8. `_report` unlocks `e8`: accuracy over every document and over the validated ones.
9. `_forced_choice` unlocks `e9`: the tool choice for a model that accepts a forced one.
10. `_check_semantics` unlocks `e5`: the sum of the line items and the stated total.

About fifteen lines in all. The sections below describe the whole pipeline, so you can see how your functions are used.

## What to write

A record has the keys `vendor` (a string or `null`), `currency` (`USD`, `EUR`, `GBP`, `other` or `unclear`), `currency_detail` (a string, required when the currency is
`other`), `line_items` (a list of numbers), `stated_total` (a number or `null`), `calculated_total` (a number), `conflict_detected` (a boolean) and `provenance` (an object
that maps a field to the quote of the document that supports it).

- `validate(record, document, required=())` returns a list of `{kind, field, message}`:
  - `syntax`: a missing key (return those alone), or a value of the wrong type or outside its enum, a missing detail for `other`, a `provenance` that is not an object;
  - `semantic`: `calculated_total` is not the sum of the line items (to half a cent), or `stated_total` differs from `calculated_total` while `conflict_detected` is false;
  - `ungrounded`: `vendor`, `currency` or `stated_total` has a value (not `null`, not `unclear`) and no non-empty quote in `provenance` that appears in the document;
  - `absent`: a field in `required` is `null` or `unclear`.
  A value that is `null` or `unclear` needs no quote, and a conflict the model flagged is information and not an error.
- `extract_document(document, call_model, required=(), max_retries=2)` calls the model with `(document, None)`, validates, and retries with `(document, {"previous": record,
  "errors": [...]})` only for errors a second look can fix (`syntax`, `semantic`, `ungrounded`), never for `absent`. It stops after `max_retries` retries. It returns
  `{"status", "record", "attempts", "errors"}`: `valid` with no errors and no conflict, `needs_review` when the only errors are `absent` or the model flagged a conflict
  (neither is retried), and `failed` otherwise.
- `merge_chunks(records)` merges the records of the chunks of one document: for `vendor`, `currency` and `stated_total` keep the first value that is neither `null` nor
  `unclear` together with its quote (and the detail for the currency), record a field in `conflicts` once when a later chunk disagrees, concatenate the line items,
  recompute `calculated_total` (two decimals), and set `conflict_detected` when any chunk flagged one or a conflict was found.
- `accuracy(results, labels)` returns `{"all_documents", "validated_only", "validated", "total"}`: a document is correct when it is `valid` and its vendor and stated total
  equal the label's. `all_documents` divides by every labelled document and `validated_only` by the valid ones (two decimals; zero when the denominator is zero).
- `request_choice(model, tools, forced=None)` returns `{"tool_choice", "strict", "verify_reply"}` with `strict` always true. On the models in `NO_FORCING` use `auto` and set
  `verify_reply` to true. Elsewhere use the forced tool when `forced` is given, `any` when there are several tools, and the one tool by name otherwise; `verify_reply` is false.

## Why each part is there, and what you should see

1. **Nullable fields.** A required field invites an invented value; a nullable one lets the model say the document does not give it. *You should see* an invoice with no
   vendor validate with `vendor` null and no quote, and an invented vendor fail as ungrounded.
2. **Provenance.** A quote that must appear in the document is a check the schema cannot make. *You should see* a made-up quote refused.
3. **Retry only what can improve.** Feedback names the errors a second look can fix; a value the document does not contain stays absent however often it is asked for. *You
   should see* one retry with the document, the failed record and the errors, and no retry for an absent value.
4. **Conflicts are information.** A total that differs from the line items and is flagged goes to review. *You should see* `needs_review` after one attempt.
5. **Honest measurement.** Accuracy on the validated documents alone is higher than the truth because the failures leave the denominator. *You should see* 0.5 on all documents
   and 0.83 on the validated ones for the same results.
6. **The tool choice.** A forced tool call is rejected by some current models and changing `tool_choice` invalidates cached message blocks. *You should see* a forced tool
   where the model allows it and `auto` with a reply check where it does not.

## The cases

| Id | What it checks |
|---|---|
| `m1` | A document with every value present and quoted comes back valid on the first attempt |
| `e1` | A value the document does not give is null and needs no quote, while an invented value fails as ungrounded |
| `e2` | A retry carries the document, the failed record and only the errors a second look can fix |
| `e3` | A required value reported as absent is not retried and goes to review |
| `e4` | Retries stop after the limit and the document is marked failed |
| `e5` | A total that differs from the line items is a semantic error; a flagged conflict goes to review without a retry |
| `e6` | Currency takes `unclear` and `other` with a detail, and anything else is a syntax error |
| `e7` | Chunk results merge by keeping the first value and recording a conflict when two chunks disagree |
| `e8` | Accuracy counts every document and not only the validated ones |
| `e9` | The request forces a tool where the model allows it and falls back to `auto` with a reply check where it does not |

Run the tests with the command in the language folder's `run.sh` (Python, TypeScript) or its build file (Java, Kotlin).
