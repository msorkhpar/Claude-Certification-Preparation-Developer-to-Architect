# Practice: diagnose a failure from a trace

A Claude application fails in ways that look alike from outside: "it did not work". The fix depends on two answers. **Where did it
fail:** in your integration (the request you built, the code that parses the output, the tool you run), in the model's output, in the
service or the network, or in your account? **What should happen next:** fix and resend, retry with back-off, wait for a header, use
a fallback model, or hand the problem to a person? In this practice you write the function that reads a trace of what happened and answers
both. It is the course's own decision table, built from the documented error types, stop reasons and loop rules; nothing here calls a model
or the network. Pick your language folder (`python`, `typescript`, `java` or `kotlin`), open `starter/` and edit the file there.

## The given parts

The Java and Kotlin folders give you `Json` (parse text into maps, lists, strings, numbers, booleans and null, and write them back). Python and
TypeScript have JSON built in. A trace is a list of events, and an event is a map with string keys. A number read from JSON is a `Long` in
Java and Kotlin; read numbers as `Number`.

## The events

Every event has a `kind`. Fields not listed for a kind are ignored, and a listed field may be missing.

| Kind | Fields |
|---|---|
| `request` | `model`, `max_tokens`, `tools` (a list of tool names), `last_user_blocks` (the block types of the last user message, in order, for example `["tool_result", "text"]`) |
| `response` | `status` (200), `stop_reason`, `content` (a list of blocks) |
| `error` | `status`, `error_type`, `message`, `headers` (a map), `error_code` |
| `network_error` | `message` |
| `tool_call` | `name`, `input` |
| `tool_result` | `name`, `is_error`, `exception` (text, present when our own tool code raised) |
| `parse` | `ok`, `text` (the model output our code tried to parse) |

## What is already written, and what you write

The starter is a working diagnosis with seven gaps cut out of it. Everything that is plumbing is written and correct: the HTTP and stop reason
tables, the request tracking, the classification of each event kind, the loop that finds the first failure and the final answer. Each gap is a
small function with its signature, a comment that says what it receives and returns with one example, and the cases it unlocks. A gap returns a
neutral value, so the starter runs and fails the cases on an assertion. To debug a gap, log its input with the `log` line at the top of the
file; a run shows the lines under the failing case. Write them in this order (the TypeScript, Java and Kotlin names are the camel-case forms):

1. `_rate_limit` unlocks `e1`: the diagnosis of a 429 (`retry-after` wins, then the spend cap, then the plain rate limit).
2. `_by_status` unlocks `m1` and `e1`: the table row for a status and the fallback by class.
3. `_has_json_object` unlocks `e4`: whether the text holds a JSON object between its first `{` and last `}`.
4. `_empty_origin` unlocks `e3`: integration or model for an empty end turn, from the block order.
5. `_tool_failure` unlocks `e5`: the unknown tool and the tool that raised.
6. `_recovered` unlocks `e6` and `e7`: whether a later good response recovered the run.
7. `_stop_failure` unlocks `e2`: the table row for a response that succeeded but stopped for a bad reason, and nothing for the fine ones.

About twenty lines in all. The sections below describe the whole
function; the parts you do not write are there so you can see how your functions are used.

## What to write

`diagnose(trace)` returns the **first failure** in the trace as `{"index", "type", "origin", "recovery", "recovered"}`. `index` is the
event's position. The origin is one of `integration` (what you built or sent), `model` (what the model produced), `service` (capacity, the
provider or the network) and `account` (credential, billing, access, limits). A trace with no failure returns
`{"index": -1, "type": "ok", "origin": "none", "recovery": "none", "recovered": false}`, and so does an empty trace. The `request` events are
not failures; each one sets the tool list and the last-user blocks that later events are judged against (a trace with no request yet has no
tool list, so no tool call is unknown).

**An `error` event** is classified by status; the result is `type | origin | recovery`:

| Status | Result |
|---|---|
| 400 | `invalid_request` `integration` `fix_request` |
| 401 | `authentication` `account` `fix_credentials` |
| 402 | `billing` `account` `fix_billing` |
| 403 | `permission` `account` `fix_access` |
| 404 | `not_found` `integration` `fix_request` |
| 409 | `conflict` `integration` `resolve_then_retry` |
| 413 | `request_too_large` `integration` `shrink_request` |
| 429 | with a `retry-after` header: `rate_limit` `service` `wait_retry_after`; otherwise with `error_code` equal to `enforced_spend_limit_reached`: `spend_cap` `account` `wait_for_reset`; otherwise `rate_limit` `service` `retry_backoff` |
| 500 | `server_error` `service` `retry_backoff` |
| 504 | `timeout` `service` `stream_or_batch` |
| 529 | `overloaded` `service` `retry_backoff` |

Two more rules. A 400 whose message contains `spend limit`, in any letter case, is `spend_limit` `account` `raise_limit`. Any other status
is classed like the nearest documented one: 500 and above like 500, anything else like 400.

**A `network_error` event** is `network` `service` `retry_backoff`.

**A `response` event with status 200** fails by its `stop_reason`:

| Stop reason | Result |
|---|---|
| `max_tokens` | `truncated` `integration` `raise_max_tokens` |
| `model_context_window_exceeded` | `context_exceeded` `integration` `trim_context` |
| `refusal` | `refusal` `model` `fallback_model` |
| `pause_turn` | `paused` `integration` `continue_turn` |
| `end_turn` with no content blocks | `empty_response`, and the origin depends on the most recent `request`: when its `last_user_blocks` has a `text` block after a `tool_result` block, `integration` `remove_text_after_tool_result`; otherwise `model` `add_continue_prompt` |

Any other stop reason (`end_turn` with content, `stop_sequence`, `tool_use`) is not a failure.

**A `tool_call`** whose `name` is not in the most recent request's `tools` is `unknown_tool` `model` `return_error_result`.
**A `tool_result`** with a non-empty `exception` is `tool_exception` `integration` `fix_tool_code`; one that is flagged `is_error` without an
exception is a tool reporting an error as designed, and is not a failure.
**A `parse` event** with `ok` false is `parse_failure`. When the text holds a JSON object (the text from its first `{` to its last `}` parses
as a JSON object) the integration's parser was too strict: `integration` `extract_json`. Otherwise the model did not produce JSON:
`model` `validate_and_retry`.

`recovered` is true when a later event is a `response` with status 200, stop reason `end_turn` and at least one content block.

## The cases

| Id | What it checks |
|---|---|
| `m1` | Each documented HTTP error maps to its type, origin and recovery |
| `e1` | The two kinds of 429, the 400 spend limit, and the fallback by class for statuses the table does not list |
| `e2` | A 200 response with a failing stop reason, and the stop reasons that are fine |
| `e3` | An empty `end_turn`: our message structure or the model, by the last user message's blocks |
| `e4` | A parse failure: our parser or the model, by whether a JSON object is in the text |
| `e5` | A tool name the model invented against our own tool raising; a tool-reported error is not a failure |
| `e6` | The first failure wins, with its index; a later good response means it recovered; a clean or empty trace is `ok` |
| `e7` | A dropped connection has no status and belongs to the service side, and may recover |

Run the tests with the command in the language folder's `run.sh` (Python, TypeScript) or its build file (Java, Kotlin).
