# The extraction system: where the pipeline can lie

**Level:** Architect · **Module 75:** Scenario: structured data extraction · **Page 1 of 2**
**Exams:** A4, A5; S6

**After this page you can** describe the exam's extraction system and what it owns that no model call can, read a symptom in a pipeline's figures as a failure shape with its first fix, and follow a run in which a record is retried, a missing value goes to a person and the accuracy is reported on every document.

Checked on 2026-10-04 against the Claude API documentation pages "Define tools" (the `tool_choice` options and the restriction on forced tool use), "Batch processing" and "Structured outputs", and the Architect exam guide (version 1.0, scenario 6). The example runs offline in Python, TypeScript, Java and Kotlin: the model is a table of made-up replies, so the example is about what the pipeline does with a record. This page is a capstone: it uses modules 61, 62, 63 and 68, which treat the schema, the retry, the batch and the measurement in full.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* for the extraction setting it names tool use with a JSON schema as the way to get structured output, `tool_choice` set to a forced tool so that a particular extraction runs first, and `any` when the document type is unknown. *What the current product does (documentation checked 2026-10-04):* "Not every model and setting supports forced tool use." On Claude Opus 5.5, Sonnet 5.5 and Fable 5.1, `any` and `tool` return a 400 error, and the documented replacement is `auto` with strict tool use or structured outputs. On the exam, the forced tool is the keyed answer to a question about guaranteeing which extraction runs; in a pipeline on those models, use the replacement and check the reply.

## Why it matters

Scenario S6 is the setting where a system can look healthy while it is wrong. Every reply parses, the dashboard is green, and the figure that the team reports is high. The failures that the exam asks about all sit outside the model call: in the schema that pushes the model to invent, in the retry that asks again for something the page does not hold, in the check that nobody wrote, and in a measurement that leaves out the documents that went wrong. The architect's habit is to ask what the pipeline claims and what it could be hiding.

## The idea

### The scenario in plain words

A system extracts information from unstructured documents, validates every record against a JSON schema, keeps accuracy high and hands the result to downstream systems. It must treat the odd document gracefully. The domains the exam draws on are the design of prompts and structured output, and the management of context and reliability.

### Read a symptom as a failure shape

| What the figures or the logs show | The failure shape | The first fix | Taught in |
|---|---|---|---|
| Every record parses, and some vendor names are not in their documents | A required field that pushes the model to invent | Make the field nullable and require a quote from the source | Module 62 |
| A retry loop asks ten times for a value that the page does not hold | A retry that cannot improve anything | Retry only the errors a second look can fix, and send an absent value to a person | Module 62 |
| Line items add up to one figure and the total says another, and the record passed | A semantic error that no schema can see | Check the record against itself in code | Module 62 |
| The dashboard says 99 percent and the reviewers say handwriting is hopeless | A figure measured on the survivors, or an average over easy documents | Measure every document, and break the figure down by kind | Modules 62 and 68 |
| High-confidence records are accepted untouched and nobody knows how many are wrong | An automated stream with no sample | Draw a stratified sample of what was accepted | Module 68 |
| A batch of a hundred is resubmitted whole because three entries failed | Paying again for what already succeeded | Resubmit by `custom_id`, and chunk what was too long | Module 63 |
| The model that extracted a record also confirms it | A check made in the generator's own context | A second, independent pass | Module 63 |

The documentation of the batch interface says what the table's sixth row relies on: "Batches expire if processing does not complete within 24 hours." Each request carries a unique `custom_id`, "Must be 1 to 64 characters", and the three results that did not succeed are `errored`, `canceled` and `expired`. A pipeline that resubmits by identifier pays only for what it must.

### What the pipeline owns

A model call returns a record. The pipeline owns everything that is about a document and a run, and four of those duties are where the exam's failures sit.

1. **The status of a document.** A document is `valid` only when every check passed and no conflict was flagged. It is `needs_review` when the only open matters are a value the page lacks or a conflict that the model flagged, and neither is retried. It is `failed` when an error survived the retry. The status is derived from the checks, and the model's own opinion plays no part.
2. **The retry.** One retry that carries the document, the failed record and the errors, and only for errors a second look can fix: an invented value, a total that disagrees, a value of the wrong type. A value that the document does not contain stays absent however often it is asked for.
3. **The measurement.** The accuracy of a run is the share of all documents whose delivered record is correct. A document that failed is a document the customer sent. The figure on the validated ones alone is a different figure and is always at least as high.
4. **The route.** Some documents need a person: the absent, the conflicting, the failed. Which kinds of document may go without one is decided by evidence for each kind, never by the average.

### The example

The example runs six documents through a scripted model. Two are typed, two are scanned and two are handwritten. The first typed invoice comes back right. The second comes back with a total that does not match its lines, is retried with the error attached, and is right the second time. The first scanned invoice has no vendor, and the model invents one that the document never names; the retry returns null for the vendor, which is honest, and the document is valid. The second scanned invoice has no total anywhere, so there is nothing to retry and it goes to a person. The first handwritten form carries a total that disagrees with its lines, and the model flags the conflict, so it goes to a person without a retry. The last form keeps an invented vendor on the retry and fails after two attempts. The run ends with two figures and a line for each kind.

<!-- example: m75-extraction-run tabs: python,typescript,java,kotlin -->
```python
```
<!-- /example -->

The point is the pair of figures. Three documents are valid and all three are right, so the pipeline could report 100 percent. Six documents were sent and three are right, so the honest figure is 50 percent. The by-kind line says where the other half went: nothing was lost in the typed documents, one of the scanned ones went to a person, and neither handwritten form was delivered. Only typed documents have earned automation. All four languages print the same lines.

## Traps

These are the wrong answers that the exam's options for this scenario offer, each with the reason it is rejected.

1. **"Raise the retry limit to recover the documents that still fail."** It is tempting because failures are visible and retries are cheap. The exam rejects it: retries fix errors a second look can fix, and a value that the document does not contain stays absent however many times it is requested.
2. **"Report the accuracy of the records that passed validation."** It is tempting because it is the figure the pipeline can compute without labels for the rest. The exam rejects it: the failures leave the denominator, so the figure flatters the system and hides the kinds of document that fail.
3. **"Accept anything the model scores 95 or more."** It is tempting because the score comes for free. The exam rejects it: a self-reported score is poorly calibrated, and a stream accepted on it needs a sample that someone checks.

## Quiz

1. In the example run, a scanned invoice names its vendor but holds no total anywhere in its text, and the model returns the total as null. What does the pipeline do with that document?
   - **a**: Retry it once with the error attached, since a second look may still find a figure there
   - **b**: Send it to a person after one attempt, as no retry supplies what the page lacks
   - **c**: Compute the figure from the line items instead and mark the record as valid
   - **d**: Fail it at once and discard the vendor that the scan did manage to read

2. The example run reports three valid documents, all correct, out of six that were sent. Which pair of figures does an honest report give?
   - **a**: Everything delivered was right, so the run scores a full hundred, and nothing more is reported
   - **b**: Half of everything received and all of the accepted ones, with the difference stated
   - **c**: Half of everything received, with the accepted-only figure left out as beside the point
   - **d**: Three quarters, as the mean of the two figures, so that both are treated fairly

3. A pipeline on Claude Sonnet 5.5 must make sure that an extraction tool is used on every request, and it forces the tool through `tool_choice`. What happens, and what does the documentation recommend instead?
   - **a**: The call works, but the cached message blocks are then processed again from the start of the prompt
   - **b**: The tool is used and the text before it is dropped, which does no harm in a pipeline that only extracts
   - **c**: The call fails with a 400 error, and `auto` with strict schemas is the published replacement
   - **d**: The setting is ignored, and the model goes on to choose whichever tool it likes best

<details>
<summary>Answer key</summary>

1. **b**. A missing value is routed, not retried. *a* is ruled out because the retry covers only what a second look can fix: "A value that the document does not contain stays absent however often it is asked for." *c* is ruled out because the status comes from the checks and not from a guess: "The status is derived from the checks, and the model's own opinion plays no part." *d* is ruled out because a missing value is not a failure of the document: "It is `needs_review` when the only open matters are a value the page lacks or a conflict that the model flagged, and neither is retried."
2. **b**. The two figures and the gap are the report. *a* is ruled out because the denominator is every document: "A document that failed is a document the customer sent." *c* is ruled out because the validated figure is a different figure and not an irrelevant one: "The figure on the validated ones alone is a different figure and is always at least as high." *d* is ruled out because the figures are not blended: "The accuracy of a run is the share of all documents whose delivered record is correct."
3. **c**. On the models in the documentation's table, forcing is refused and the replacement is named. *a* is ruled out because the request does not get as far as the cache: "Not every model and setting supports forced tool use." *b* is ruled out because the request does not run: "On Claude Opus 5.5, Sonnet 5.5 and Fable 5.1, `any` and `tool` return a 400 error". *d* is ruled out because a setting that the API cannot honour is rejected and not ignored: "the documented replacement is `auto` with strict tool use or structured outputs".

</details>
