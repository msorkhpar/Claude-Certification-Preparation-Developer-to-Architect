# Retries, conflicts, chunks, honest accuracy and the practice

**Level:** Architect · **Module 62:** Structured output at the architect level · **Page 2 of 2**
**Exams:** A4.4; S6

**After this page you can** retry a failed extraction with the document, the failed record and the specific errors, tell the errors a retry can fix from the ones it cannot, route absent values and flagged conflicts to review, merge the results of the chunks of a long document, report accuracy that does not hide the failures, choose `tool_choice` for the request, and write the module's practice.

Checked on 2026-10-03 against the exam guide's task statement 4.4, the Claude API documentation pages "Define tools" (`tool_choice`) and "Prompt caching" (what invalidates the cache) and the Agent SDK page on structured outputs (result subtypes). Nothing here called a model: the practice is graded by Python, TypeScript, Java and Kotlin test suites, offline, on scripted model replies (`examples/62-extraction-checks` holds the checks the page describes). This page deepens module 25 and module 54 (where forcing a tool was first treated) and does not repeat them. The schema's limits and the checks are the first page.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* for validation and retry (4.4) it prescribes retry with error feedback, appending the specific validation errors to the prompt, and notes that retries are ineffective when the information is simply absent from the source; it names self-correction checks, such as extracting a `calculated_total` beside the `stated_total` and a `conflict_detected` flag for inconsistent source data, and a `detected_pattern` field in findings so that false positives can be analysed. *What the current product does (documentation checked 2026-10-03):* a result can end with a subtype `success` and no structured output, or with `error_max_structured_output_retries` ("No valid output remained after multiple attempts"); both are failures to be handled by the caller. On a model that supports forced tool use, "changes to the `tool_choice` parameter will invalidate cached message blocks. Tool definitions and system prompts remain cached, but message content must be reprocessed." On the models that reject forcing, `auto` with strict tool use is the documented replacement. On the exam, a retry question has the answer "retry with the errors, unless the value is absent from the source"; a measurement question has the answer "count every document".

## Why it matters

The pipeline from the first page now has nullable fields, provenance and checks. A document fails one of them. What now? The first reflex is to ask again, the same way. That works for one kind of failure and is wasted on another. A model asked again for a purchase order number that is not in the document will, at best, say again that it is not there; at worst it invents one. And when the team finally measures the pipeline, the report counts only the documents that passed validation, which leaves out exactly the documents that did not. Scenario S6 tests both: when does a retry help, and what is the accuracy.

## The idea

### Retry with the evidence

A retry that repeats the request gets the same answer with the same probability. A retry that carries what went wrong gives the model something to correct. The request on the second attempt holds three things: the original document, the failed record, and the specific errors, each with its field and a message such as "`calculated_total` 100.0 is not the sum of the line items, 120.5". The model can then fix the field that failed and keep the rest. The number of retries is bounded (two in the practice), and a document that is still invalid after the last one is marked failed: a loop without a limit is a cost the pipeline did not choose.

### What a retry can and cannot fix

| Error | Retry helps | Why |
|---|---|---|
| Syntax (wrong format, enum value outside the list) | Yes | The information is in the document; the form was wrong |
| Semantic (the items do not sum) | Yes | A second look at the document can correct the arithmetic or the field |
| Ungrounded (a quote not in the document) | Yes | The model can find the real passage or return null |
| Absent (a required value the document does not contain) | No | No second look will find what is not there |

The last row is the exam's favourite. A required value that the model reports as absent is not retried: the document goes to review, since asking again either repeats the answer or pushes the model to invent a value, which is the failure that nullable fields were introduced to prevent. The example's pipeline separates the two kinds of error, and its retry feedback lists only the first three.

### Conflicts are information

Source documents are sometimes inconsistent: the invoice states a total that is not the sum of its lines. Ask for both numbers, `stated_total` as printed and `calculated_total` as computed, and a boolean `conflict_detected`. A record that carries a difference and says so is a correct record of a bad document: it is not an error to retry, because a second look will find the same difference. It goes to a person. A record that carries a difference and says `conflict_detected: false` is a semantic error, and is retried.

### A long document in chunks

A document too long for one request is cut into chunks and each is extracted, and the records are merged. The merge is a function, and its rules are decisions: keep the first value that is neither null nor unclear, with the quote it came with; concatenate the line items; recompute the total in code, not by asking a model to add; and when a later chunk gives a different vendor or total, do not pick silently: record the field in a `conflicts` list and set `conflict_detected`, so that the document goes to review. A merge that always takes the last value or the longest is a guess presented as data.

### The accuracy that hides the failures

A pipeline has validation, review and failure outcomes. A report of "accuracy on validated documents" divides by the documents that passed, which are the easy ones, and leaves out the documents that went to review or failed. With ten documents, five correct and valid, one valid and wrong, two in review and one failed, accuracy over the validated is 5 of 6, 0.83, and accuracy over all documents is 5 of 10, 0.5. The first number is true and answers a different question. The one to report is the share of all documents extracted correctly, with the validated-only figure beside it and the counts that explain the gap. The same bias appears in any metric whose denominator is filtered by the pipeline's own judgement.

### The tool choice of the request

For extraction, the request forces the model to answer through a tool whose input is the schema. The modes are `auto`, `any` (one of the tools) and a named tool. On a model that supports forcing, a pipeline with several document types uses `any`, and a pipeline with one type may name it. On Claude Opus 5.5, Sonnet 5.5, Fable 5.1 and Mythos 5.1 the API rejects `any` and `tool`; the replacement is `auto` with `strict` on the tool, plus a check in code that the reply made the call, since `auto` allows a plain-text answer. Two cost facts for any model: forcing a tool means the model gives no sentence before the call, and changing `tool_choice` between requests invalidates cached message blocks (tools and system prompt stay cached), so a pipeline that alternates modes on a long cached document pays to reprocess the document.

### The example

<!-- example: m62-extraction-checks tabs: python,typescript -->
```python
"""What a schema does not give an extraction pipeline."""
```
<!-- /example -->

### The practice: an extraction pipeline

The practice is in [`exercises/62-structured-output-at-the-architect-level`](../../exercises/62-structured-output-at-the-architect-level/unit-01/practice-1/statement.md). You write the validation of a record (syntax, semantic, ungrounded and absent errors), the extraction loop with feedback retries and a status, the merge of the chunks, the accuracy over all documents, and the request's tool choice. It is graded in Python, TypeScript, Java and Kotlin; the statement lists ten cases, each saying what you should see when it works.

## Traps

1. **"Retry the extraction until the field is filled."** It is tempting because the field is required. The exam rejects it for a value that is absent from the source: a retry cannot supply it and may fabricate it. The document goes to review.
2. **"On failure, send the same request again."** It is tempting because it is simple. The exam rejects it: the retry that works carries the document, the failed record and the specific errors.
3. **"Report accuracy on the documents that passed validation."** It is tempting because those documents have a clean verdict. The exam rejects it: the denominator leaves out the failures, so the figure overstates the pipeline. Count every document.
4. **"When two chunks disagree about the vendor, take the later one."** It is tempting because it keeps the merge simple. The exam rejects it: a disagreement is a conflict to record and send to review, and not a choice to make silently.

## Quiz

1. An extraction fails a check because the printed total differs from the sum of the lines, and the record has `conflict_detected` set to true. What should the pipeline do?
   - **a**: Retry with the error message appended to the prompt
   - **b**: Replace the printed total with the computed one
   - **c**: Route it to a person with the discrepancy noted
   - **d**: Mark the document failed once the retry limit is reached

2. A pipeline reports 98 percent accuracy over the documents that passed its checks, while 30 percent of all documents did not pass. What is wrong with the figure?
   - **a**: It leaves out the failures, so it flatters the system
   - **b**: It is too low, because a passed document is always correct
   - **c**: It counts each document twice, once per validation rule
   - **d**: It is right, since only passed documents have a verdict

<details>
<summary>Answer key</summary>

1. **c**. A flagged conflict is information about a bad source, and a second look finds the same difference. *a* is ruled out because "a second look will find the same difference", so the retry cannot change the outcome. *b* is ruled out because "A record that carries a difference and says so is a correct record of a bad document", and replacing the printed value hides what it says. *d* is ruled out because this record is not invalid, since "a document that is still invalid after the last one is marked failed" applies to errors that are retried.
2. **a**. The denominator is filtered by the pipeline's own verdict, which drops the documents most likely to be wrong. *b* is ruled out because the report "divides by the documents that passed", so it overstates and does not understate. *c* is ruled out because it "leaves out the documents that went to review or failed", and nothing is counted twice. *d* is ruled out because "The first number is true and answers a different question".

</details>

## Module quiz

This quiz covers both pages of the module.

1. Scenario S6, structured data extraction. A 300-page contract is cut into chunks, and the per-chunk results are merged. Two chunks give different vendor names. Which merge rule fits?
   - **a**: Take the later value, since it was read with more context
   - **b**: Keep the first value, note the clash and send it to a person
   - **c**: Take the longer name, since it carries more detail
   - **d**: Drop the field from the merged result to avoid a wrong value

2. Scenario S6, structured data extraction. A team extracts invoice fields. Each failed extraction gets two more attempts. One failure is a currency written outside the allowed list, and another is a purchase order number that the document does not contain but downstream needs. Which handling is right?
   - **a**: Send the first back with its error, and route the second to review untried
   - **b**: Send both back with their errors until each field has a value
   - **c**: Send neither back and mark both documents failed at once
   - **d**: Route the first to review and send the second back with a stricter, firmer prompt

3. Scenario S6, structured data extraction. A team extracts invoice fields with a strict schema through a forced tool, and caches a long instruction block and a 200-page document. For some requests it switches `tool_choice` from `any` to `auto` and back, and costs run above plan. What explains it?
   - **a**: Each change also discards the stored tool definitions and the system prompt text
   - **b**: Moving between modes turns off structured output for the next request
   - **c**: Each change discards stored conversation content, which is processed again
   - **d**: Requests in automatic mode are billed at a higher rate than the others

<details>
<summary>Answer key</summary>

1. **b**. A disagreement between chunks is a conflict to record and review, and not a choice to make silently. *a* is ruled out because "A merge that always takes the last value or the longest is a guess presented as data". *c* is ruled out because "A merge that always takes the last value or the longest is a guess presented as data". *d* is ruled out because the rule is to record the clash "so that the document goes to review", and dropping the field hides it.
2. **a**. The currency error is fixable by a second look and the missing number is absent from the source. *b* is ruled out because for an absent value "No second look will find what is not there". *c* is ruled out because a format error is what feedback is for, since "The information is in the document; the form was wrong". *d* is ruled out because it inverts the two, while "A required value that the model reports as absent is not retried".
3. **c**. Changing the mode invalidates cached message blocks, so the content must be reprocessed. *a* is ruled out because "Tool definitions and system prompts remain cached". *b* is ruled out because the documented effect of a change is on the cache: "changing tool_choice between requests invalidates cached message blocks". *d* is ruled out because the cost described is reprocessing, since a pipeline that alternates modes "pays to reprocess the document".

</details>

Adapted from the Claude Certified Architect - Foundations exam preparation guide by Daron Yondem (CC BY 4.0, https://creativecommons.org/licenses/by/4.0/), changed for this course.
