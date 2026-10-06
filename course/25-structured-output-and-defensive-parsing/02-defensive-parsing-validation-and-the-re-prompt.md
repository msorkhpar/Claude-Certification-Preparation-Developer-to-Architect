# Defensive parsing, validation and the re-prompt

**Level:** Developer · **Module 25:** Structured output and defensive parsing · **Page 2 of 2**
**Exams:** DV1, DV4; A4.3, A4.4

**After this page you can** read JSON out of a reply that wraps it in prose, validate a value against a schema and report every
problem with its path, check that a quotation really occurs in the source, ask the model again with the problems listed, bound
the loop, and say which stop reasons must never be retried.

Checked against the Claude API documentation (Structured outputs, Handling stop reasons and Handle tool calls) on 2026-10-03, and
by running the practice and the example offline in the course container (`anthropic` 1.11.0, `@anthropic-ai/sdk` 0.131.0). The model in
the practice is a function that returns scripted replies in the shape of a Messages API response.

## Why it matters

Structured outputs fix the shape of a reply on the models that support them, and nothing else. A schema cannot say that a number
is at least 0, that a quotation occurs in the document, or that the answer is right. Applications also run on models and paths
where the shape is not enforced, such as a prompt that asks for JSON and a tool call that is not strict. Code that parses with one
`json.loads` and trusts the result fails on the first fence, the first cut-off reply and the first invented quotation. The exam asks
what a careful extractor does at each of these.

## The idea

### Read before you trust

A reply is a list of content blocks. The text blocks hold the model's words, and they may hold more than JSON: a sentence before
it, a code fence around it, a closing remark. A small reader handles the common shapes in a fixed order:

1. If the text has a code fence, take the body of the first fence.
2. Take the span from the first `{` to the last `}`.
3. Parse it. If there is no such span, or it is not valid JSON, raise a parse error and do not guess.

The reader repairs a harmless habit. It does not repair a broken value: guessing missing braces turns a cut-off reply into a
wrong record.

### Validate and report everything

A parse that succeeds says the text is JSON. It says nothing about the content. The practice's `validate(schema, value)` walks a
value and returns a list of problems, each with a path: `$` for the whole value, `$.total` for a field, `$.items[1].qty` for an array
element. Four decisions make the list useful.

- **One problem per fault, all of them.** Report every missing key and every wrong field in one pass. A re-prompt that lists one
  problem per call spends an attempt on each.
- **A wrong type stops the checks of that value.** A string where a number belongs is one problem. Asking whether the string is
  at least 0 would add noise.
- **Types are exact.** A boolean is never a number or an integer, and an integer is a number with no fraction: `2.0` is an integer and
  `2.5` is not. Languages disagree here. In Python `True` is an `int`, and in JavaScript every number is a double. The validator has to
  decide, and the schema's meaning decides: `true` is not a quantity.
- **Unknown keys are a problem when the schema closes the object.** With `additionalProperties: false`, an extra key means the
  model invented a field.

The checks that come after the type are the ones the schema of the previous page could not carry to the API: `enum`, `minimum` and
`maximum`. They are the program's job, as the documentation says: "a helper that validates responses still enforces every constraint
in your code."

### A quotation must occur in the source

A task that asks the model for evidence, such as the sentence that shows the invoice total, has a check that needs no model. If the
string is not in the document, the model made it up or changed it. The practice calls these `evidence_fields`: for each name in the
list, a string field of that name must be found in the document, and if it is not, the problem is `$.name`. An invented quotation
is the commonest way a structured reply is well formed and false. Exact match is deliberate; a looser match would accept a quotation
with a changed number.

### Ask again, with the problems

When the reply fails, the next call needs the whole conversation: the first message, the model's reply as an `assistant` message
with exactly its text, and a `user` message that lists each problem with its path and message. The model sees what it wrote and
what is wrong with it. A repair that restarts with the original prompt and no error list tends to repeat the mistake.

The loop has a bound. `max_attempts` calls in all, and then the result is `failed` with the problems of the last attempt, so
the caller can log it or route the document to a person. An unbounded retry on a hard document is a cost leak.

### Stop reasons that are not retried

The loop reads `stop_reason` before the body. The documentation's table gives the reaction to each value, and two of them end the
loop. A reply that reached the `max_tokens` limit has the action "Raise `max_tokens` or continue the response", and a `refusal` has
the action "Read `stop_details` and retry on a fallback model." Asking again with an error list cannot fix either: a cut-off reply
needs a larger limit, and a refusal is a decision, not a malformed answer. In the practice the statuses are `truncated` and
`refused`, with no further call: this course's loop stops and reports, and the documented fallback model is a separate request, not a repeat of the same one. A reply that ends with `end_turn` goes on to the parse.

For tool calls the same discipline gives a matching rule. A tool call whose input is invalid can be answered with a `tool_result`
that has `is_error` set to `true`; the documentation says Claude "will retry 2-3 times with corrections before apologizing to the
user", and that "To eliminate invalid tool calls entirely, use strict tool use". The next module builds this loop.

### The example

The example from the previous page is this loop with the API in place. Run it again with the loop in mind: document 2 is the
re-prompt (two attempts, an invented quotation, the problem sent back as the last user message), and documents 3 and 4 are the two
stop reasons that end the loop without a second call.

## The practice: an extractor with validation and a re-prompt

You write `parse_json`, `validate` and `extract`. The statement is in
`exercises/25-structured-output-and-defensive-parsing/unit-01/practice-1/statement.md`; each language folder has a `starter`, the
tests and a build file, and the starter fails every test. The model is a function you call; the tests give it scripted replies.

| Id | What it checks |
|---|---|
| `m1` | A valid reply is returned after one call |
| `e1` | JSON is found inside fences and prose, and text with no JSON is retried |
| `e2` | Every schema violation is listed and sent back to the model |
| `e3` | The number of attempts is bounded |
| `e4` | A refusal or a cut-off reply is not retried |
| `e5` | A quotation that is not in the document is rejected |
| `e6` | Types are exact: booleans are not numbers and integers have no fraction |

Case `e6` is the rule about types in code: `true` for a `qty` of type `integer` is one problem, `2.0` passes, and `2.5` does not.

## Traps

1. **Retrying a refusal.** The same request gets the same decision, and each call is billed. Stop and report `refused`.
2. **Asking again with only the original prompt.** The model has no way to know what was wrong. Send its own reply and the list of
   problems.
3. **Stopping at the first problem.** One re-prompt then fixes one field. Collect all the problems in one pass, and bound the attempts.

## Quiz

1. A reply arrives as a friendly sentence, then JSON inside a code block, then a closing remark. What should the extractor do with it?
   - **a**: Ask the model again for bare JSON, without the words around it
   - **b**: Parse only what the fence holds and leave the prose around it
   - **c**: Strip the leading sentence and parse all of the text that follows
   - **d**: Raise a parse error, since the reply as a whole is not valid JSON

2. In this module's validator, a value of `true` arrives for `qty`, which the schema declares an integer of at least 1. What does the problem list hold for `qty`?
   - **a**: No entry, since in Python `True` is an `int` and equals 1
   - **b**: Nothing, once the value is converted to 1 and passes the minimum
   - **c**: An entry for the wrong type and a second one for the minimum
   - **d**: A single entry for the type, and the minimum is not checked

3. In this module's extractor, an answer from the model parses but fails two of the validator's checks. What should the next call carry?
   - **a**: The first message alone, sent again unchanged as a new request
   - **b**: The first message, the reply as an assistant turn, and both problems
   - **c**: The first message and both problems, with the reply left out of it
   - **d**: One user turn that pastes the reply and both problems into the prompt

<details>
<summary>Answer key</summary>

1. **b**. The reply has a fence, and the reader's first step is "If the text has a code fence, take the body of the first fence". *a* is ruled out because the reader "repairs a harmless habit", and only "text with no JSON is retried". *c* is ruled out because the closing remark would reach the parser, while even without a fence the reader keeps only "the span from the first `{` to the last `}`". *d* is ruled out because a parse error comes only "If there is no such span, or it is not valid JSON", and this reply has one.
2. **d**. The page's own case is that "`true` for a `qty` of type `integer` is one problem", and the type comes before the minimum. *a* is ruled out because "A boolean is never a number or an integer", whatever Python's types say. *b* is ruled out because nothing is converted: "the schema's meaning decides: `true` is not a quantity". *c* is ruled out because "A wrong type stops the checks of that value", so the minimum adds no second problem.
3. **b**. The page says "the next call needs the whole conversation", which ends with "a `user` message that lists each problem with its path and message". *a* is ruled out because "A repair that restarts with the original prompt and no error list tends to repeat the mistake." *d* is ruled out because the reply goes back as "the model's reply as an `assistant` message with exactly its text", not inside the user's prompt. *c* is ruled out because the reply is part of the repair: "The model sees what it wrote and what is wrong with it."

</details>

## Module quiz

This quiz covers both pages of the module.

1. An invoice extractor runs on a model that supports structured outputs. The reply has the right fields and types, yet the cited sentence is one the document never contained. Which change catches such replies?
   - **a**: Describe the evidence field in the schema as an exact quotation from the document
   - **b**: Look up the evidence string in the source text and reject it when absent
   - **c**: Accept the evidence when it closely matches some sentence of the document
   - **d**: Read `stop_reason` first and re-prompt whenever it is not `end_turn`

2. A team's extractor retries every failed reply up to five times. A document triggers a `refusal`. Following this module's loop, what is the right change?
   - **a**: End the attempts there and hand the refused status back to the caller
   - **b**: Add the refusal text to the next prompt, for the model to explain itself
   - **c**: Treat it like a cut-off reply and raise `max_tokens` before retrying
   - **d**: Raise the attempts to ten, so that one of the later calls gets past it

3. A schema has an enum of `USD`, `EUR` and `GBP`, and a reply returns `Eur`. How should the program treat it?
   - **a**: As the value `EUR`, since only its capital letters are different
   - **b**: As a schema violation, since it is not one of the allowed values
   - **c**: As a slip to fix by re-prompting the model with the error list
   - **d**: As a variant to accept by adding `Eur` to the enum's values

4. A reply with `stop_reason` of `max_tokens` holds `{"vendor": "Acme`. Which step should the program take?
   - **a**: Close the braces itself and validate the value it guessed from the text
   - **b**: Re-prompt with the error list, so that the model finishes the missing text
   - **c**: Report the status as truncated, and raise the limit before asking again
   - **d**: Treat it as a refusal and switch over to a fallback model for the retry

<details>
<summary>Answer key</summary>

1. **b**. The page says "If the string is not in the document, the model made it up or changed it", and the check "needs no model". *a* is ruled out because a description is still part of the schema, and "A schema cannot say that a number is at least 0, that a quotation occurs in the document". *c* is ruled out because "a looser match would accept a quotation with a changed number". *d* is ruled out because this reply completes normally, and "A reply that ends with `end_turn` goes on to the parse".
2. **a**. The page says "this course's loop stops and reports" with the status `refused`, and the documented fallback model is a different request, not a repeat. *d* is ruled out because "The same request gets the same decision, and each call is billed." *b* is ruled out because the refusal text is not a fault to explain away: "a refusal is a decision, not a malformed answer". *c* is ruled out because a larger limit is the remedy for the other stop reason: "a cut-off reply needs a larger limit".
3. **a**. The page's advice is to "Compare enum values case-insensitively", and compared that way `Eur` is `EUR`. *b* is ruled out because the value is fixed before the check: "The example's `normalise_enum` maps `Eur` back to `EUR` before it validates". *c* is ruled out because no second call is needed: the example "repairs the enum's capital letters" itself, and document 1, whose reply held `Eur`, ended after one attempt. *d* is ruled out because the advice is to "avoid enum values that differ only in capitalization".
4. **c**. The page says "Raise `max_tokens` or continue the response", and "The reader repairs a harmless habit. It does not repair a broken value". *a* is ruled out because "guessing missing braces turns a cut-off reply into a wrong record". *b* is ruled out because "Asking again with an error list cannot fix either: a cut-off reply needs a larger limit". *d* is ruled out because "a refusal is a decision, not a malformed answer", and a cut-off reply is a different stop reason.

</details>
