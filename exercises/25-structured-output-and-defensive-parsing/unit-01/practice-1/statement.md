# Practice: an extractor with validation and a re-prompt

A model can be asked for JSON that follows a schema, and the reply can still be wrong: cut off, wrapped in prose, a
number where a string belongs, or a quotation the document never contained. Write the code around the model that catches
this and asks again, a bounded number of times. Pick your language folder (`python`, `typescript`, `java` or `kotlin`),
open `starter/` and edit the file there. The rules come from the Claude documentation on structured outputs, tool use
and stop reasons, read on 2026-10-03; the lesson pages explain them. Nothing here touches the network: the model is a
function that you call, and the tests give it scripted replies in the shape of a Messages API response.

## The given types

| Name | Meaning |
|---|---|
| schema | a JSON Schema as a map, using only `type` (`string`, `integer`, `number`, `boolean`, `array`, `object`, `null`), `enum`, `minimum`, `maximum`, `required`, `properties`, `additionalProperties: false` and `items` |
| `ask(messages)` | the model: takes the list of `{"role", "content"}` messages and returns a reply `{"content": [blocks], "stop_reason": ...}`; the text blocks hold the model's words |
| problem | `{"path", "message"}`; the path is `$` for the whole value, `$.total` for a field, `$.items[1].qty` for an array element |
| `ParseError` | raised when no JSON object can be read from a reply |

## What to write

- `parse_json(text)` returns the JSON value in a reply. If the text holds a code fence, use the body of the first fence;
  then take the span from the first `{` to the last `}` and parse it. Raise `ParseError` when there is no such span or it
  is not valid JSON.
- `validate(schema, value)` returns the list of problems, in this order for one value: a wrong type stops the checks for
  that value (one problem, no more); then `enum`, then `minimum` and `maximum`; for an object, one problem per missing
  `required` key (path `$.key`, in the schema's order), then the checks of every property in the schema's order, then one
  problem per key that the schema does not list when `additionalProperties` is false; for an array, the checks of each
  element. An integer is a number with no fraction (`2.0` is an integer, `2.5` is not); a boolean is never a number or an
  integer. A conforming value gives an empty list.
- `extract(ask, document, schema, max_attempts=3, evidence_fields=())` builds the first message (role `user`, containing
  the document), calls `ask`, and reads the reply:
  - `stop_reason` of `refusal`: stop, status `refused`, no further call. `max_tokens`: stop, status `truncated`, no
    further call (asking again with the error list cannot fix a cut-off reply).
  - Otherwise parse the text and validate it. For every name in `evidence_fields`, a string field of that name must occur
    in the document; if it does not, add the problem `$.name`. A reply that parses and has no problems gives status `ok`.
  - On a problem and with attempts left, call `ask` again with the earlier messages, then the model's reply as an
    `assistant` message with exactly its text, then a `user` message that lists every problem with its path and message.
    A parse failure is the single problem at path `$`.
  - After `max_attempts` calls in all, give status `failed` with the problems of the last attempt.
  - The result is `{"status", "value", "attempts", "errors"}`; `value` is the parsed object for `ok` and nothing otherwise.

## The cases

| Id | What it checks |
|---|---|
| `m1` | A valid reply is returned after one call |
| `e1` | JSON is found inside fences and prose, and text with no JSON is retried |
| `e2` | Every schema violation is listed and sent back to the model |
| `e3` | The number of attempts is bounded |
| `e4` | A refusal or a cut-off reply is not retried |
| `e5` | A quotation that is not in the document is rejected |
| `e6` | Types are exact: booleans are not numbers and integers have no fraction |

Run the tests with the command in the language folder's `run.sh` (Python, TypeScript) or its build file.
