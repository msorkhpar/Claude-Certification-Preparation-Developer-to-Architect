# What a schema guarantees and what it does not

**Level:** Architect · **Module 62:** Structured output at the architect level · **Page 1 of 2**
**Exams:** A4.3; S6

**After this page you can** separate the errors a schema removes from the ones it cannot, design the fields of an extraction record so that the model is never pushed to invent a value, use `unclear` and `other` with a detail for values that do not fit an enum, and require a quote from the source as provenance for every extracted value.

Checked on 2026-10-03 against the Claude API documentation pages "Structured outputs", "Strict tool use" and "Define tools", the Agent SDK page on structured outputs and the exam guide's task statement 4.3. Nothing here called a model: the example is a pipeline's decisions, written as plain code over a scripted model. This page deepens module 25 (structured output and defensive parsing) and module 26 (tool use) and does not repeat them. Retries, conflicts, chunks and the measurement are the second page.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* for structured output (4.3) it says that tool use with a JSON schema is the most reliable way to get schema-compliant output and "eliminates JSON syntax errors", and that strict schemas "do not prevent semantic errors" such as line items that do not sum to the total or a value in the wrong field. It prescribes optional or nullable fields for information that may be absent, so that the model does not fabricate a value, `unclear` in enums for ambiguous cases, and "other" plus a detail field for extensible categories. It names `tool_choice` as `auto`, `any` or a forced tool. *What the current product does (documentation checked 2026-10-03):* constrained decoding guarantees "schema-compliant responses" with "Guaranteed field types and required fields"; the guarantee is about shape. The `tool_choice` modes are as the guide says, with a restriction: on Claude Opus 5.5, Sonnet 5.5, Fable 5.1 and Mythos 5.1, `any` and `tool` "return a 400 error", and the replacement is `auto` with strict tool use or structured outputs. On the exam, the guide's `tool_choice` answers are the keyed ones for a question about forcing; for a real pipeline on those models, use the replacement.

## Why it matters

A pipeline extracts invoices. Its schema is strict: every field is required, the currency is an enum, the totals are numbers. The JSON never fails to parse. Still, invoices with no purchase order number come back with `PO-0000`, an invoice whose line items add up to 120.50 comes back with a total of 130.00, and a vendor name is a plausible company that does not appear in the document. The schema did what it promised, and nothing it promised was this. Scenario S6 tests whether you know the boundary: syntax belongs to the schema, and meaning belongs to the checks you write.

## The idea

### Two kinds of error

| Kind | Example | Who removes it |
|---|---|---|
| Syntax | A missing key, `"2"` where a number belongs, a currency outside the enum | The schema: constrained decoding or strict tool use |
| Semantic | Items that do not add up to the total, a vendor in the currency field, a value that the source does not contain | Your code: checks that read the record against the source |

A strict schema makes the first kind rare and does nothing about the second. A pipeline that stops at schema validation reports a high success rate and passes wrong records on. The exam's question is almost always "which problem remains", and the answer is the second row.

### A field that may be absent is nullable

When a field is required and the document does not hold the value, the model has two choices: refuse, which a schema cannot express, or fill the field. It fills the field, and the value is plausible. The remedy is in the schema: a field that may be missing from the source is optional or nullable, so that `null` is a valid and honest answer. The example's `scripted_value` shows the pressure: a required string is filled with a default, and a nullable one stays null. A nullable field must come with a way to tell absent from missing-by-error, which is what the required list of the practice does: some fields must have a value for the record to be useful, and a null in one of them sends the document to review instead of passing it.

A length limit such as `minLength` is not the answer: constrained decoding does not enforce it (it is on the list of unsupported constraints), and it would not tell a real number from an invented one.

Two corollaries. Do not make every field nullable; nullability is for values that can really be absent, since a required field with a valid value is a stronger check. And do not give a default that looks like data (`PO-0000`, `unknown`): downstream code cannot tell it from a real value.

### Enums that admit ambiguity and growth

A closed enum forces a choice even when the document is silent or the value is new. Two additions keep it honest:

- `unclear` for the case in which the source does not settle the value, which the pipeline can route to review;
- `other` plus a detail field for a value the enum did not foresee: the currency `other` with `currency_detail: "CHF"`. A record with `other` and no detail is a syntax error, because the pair is the contract.

Both beat the alternative of widening the enum for every new value, which hides the question of whether the value matters.

### Provenance: the quote that proves it

A semantic check needs something to check against. For an extracted value, the most useful is the sentence it came from: a required `provenance` object that maps each field to a quote, and a check that the quote appears in the document. Three things follow. An invented value has no quote that appears in the source, so it fails the check. A reviewer sees where each value came from. And the quote costs little, because the model has the text at hand. A `null` or `unclear` value needs no quote, since there is nothing to ground.

The check is a plain substring test in code: it does not ask the model whether it is right. Provenance does not prove a value is correct, only that it is grounded; the sum check and the review of conflicts cover the rest.

### Checks that read the record

The semantic checks of the example are small: the line items add up to the calculated total, to half a cent; the stated total either equals the calculated one or the record says `conflict_detected: true`, so a difference that the model noticed is information to review and not an error to retry. These are the checks the guide names (items that do not sum to the stated total) written as functions. What to do when a check fails is the next page.

### The example

<!-- example: m62-extraction-checks tabs: python,typescript -->
```python
"""What a schema does not give an extraction pipeline."""
```
<!-- /example -->

## Traps

1. **"A strict schema makes the extraction correct."** It is tempting because the output always parses. The exam rejects it: a schema "eliminates JSON syntax errors" and "do[es] not prevent semantic errors" such as items that do not sum to the total.
2. **"Make every field required so that nothing is missed."** It is tempting because a complete record looks better. The exam rejects it: for information that may be absent, a required field pushes the model to fabricate a value. Nullable fields let it say the document does not give one.
3. **"Add a default such as `unknown` for a missing value."** It is tempting because the field is never empty. It fails because the default looks like data to downstream code; `null`, with a required list for fields that must have a value, is explicit.
4. **"Add every new currency to the enum as it appears."** It is tempting because the enum stays closed. The exam prefers `other` with a detail field, and `unclear` for ambiguity, which keep the schema stable and make the new values visible.

## Quiz

1. An extraction schema marks the purchase order number as required, and invoices lacking one come back with believable numbers that appear nowhere in the documents. What fixes it?
   - **a**: Add a minimum length so that short numbers are refused
   - **b**: Allow a null for that field when the source gives none
   - **c**: Tell the model never to leave a required field empty
   - **d**: Set a default of `unknown` in the schema for that field

2. A strict schema is in force, and one record lists line items that add up to 120.50 beside a total of 130.00. Which statement holds?
   - **a**: A type check catches it, because a total is just a number in the record
   - **b**: Strict mode catches it as soon as the tool definition sets `strict`
   - **c**: Required fields must agree with each other, so the validator rejects it
   - **d**: Nothing in the contract catches it: shape is promised, meaning is not

<details>
<summary>Answer key</summary>

1. **b**. A field that may be missing from the source is nullable, so that the model is not pushed to fabricate a value. *a* is ruled out because a length limit "would not tell a real number from an invented one". *c* is ruled out because "a required field pushes the model to fabricate a value", and the instruction adds to the pressure. *d* is ruled out because a default that looks like data is a problem because "downstream code cannot tell it from a real value".
2. **d**. The guide says strict schemas do not prevent semantic errors, and a sum is one. *a* is ruled out because "A strict schema makes the first kind rare and does nothing about the second", and both numbers pass a type check. *b* is ruled out because strict schemas "do not prevent semantic errors", and this record already matches its shape. *c* is ruled out because "the guarantee is about shape", which covers presence of fields and not agreement between them.

</details>

Adapted from the Claude Certified Architect - Foundations exam preparation guide by Daron Yondem (CC BY 4.0, https://creativecommons.org/licenses/by/4.0/), changed for this course.
