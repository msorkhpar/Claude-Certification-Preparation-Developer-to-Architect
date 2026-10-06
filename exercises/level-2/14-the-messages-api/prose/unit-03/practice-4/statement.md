# Practice: a conversation client, graded on the state it keeps

The Messages API is stateless: every request must carry the whole conversation. Write the client that keeps it.

## What is already written, and what you write

The starter is a working conversation client with eight gaps cut out of it. The plumbing is written and correct: the constructor, `say` itself (it
calls the gaps in order), `totals` and `reset`. Each gap is a small method with its signature, a comment that says what it receives and returns with
one example, and the cases it unlocks. A gap returns a neutral value, so the starter runs and fails the cases on an assertion. To see what a gap
receives, debug it by logging its input with the `log` line at the top of the file; a run shows the lines under the failing case. Write them in this
order (Python names; the TypeScript, Java and Kotlin names are the camel-case forms, `makeReply` for `_reply`):

1. `_check_text` unlocks `e6`: a blank turn is refused.
2. `_request_body` unlocks `m1` and `e5`: the body with a deep copy of the whole history.
3. `_optional_fields` unlocks `e4`: the top-level `system` and the stop sequences.
4. `_send_or_roll_back` unlocks `e2`: no dangling user turn when the call fails.
5. `_assistant_turn` unlocks `m1`: the assistant turn with the content as received.
6. `_add_usage` unlocks `e1`: the running totals.
7. `_reply` unlocks `m1` and `e3`: the text, the stop reason and `truncated`.
8. `history` unlocks `e5`: the turns as a copy.

`m1` needs gaps 2, 5 and 7. A few lines each, about fifteen in all.

## The given types

| Name | Meaning |
|---|---|
| `send` | an injected function: it takes the request body (a map) and returns a Messages API response (a map with `content`, `stop_reason` and `usage`); the tests pass a scripted one |
| `Reply` | `text` (the text blocks joined), `stop_reason` and `truncated`; TypeScript, Java and Kotlin spell them `text`, `stopReason` and `truncated` |

## `Conversation(send, model, max_tokens, system=None, stop_sequences=None)`

- **`say(text)`** adds a user turn, sends the request and returns a `Reply`.
  - The request body has `model`, `max_tokens` and `messages`, where `messages` is the **whole history** so far,
    oldest first, ending with the new user turn. `system` is a **top-level field**, never a message with a system
    role, and it is left out when absent or blank. `stop_sequences` is passed on when given and non-empty.
  - The body handed to `send` is a **snapshot**: later turns must not change a request already sent.
  - The assistant turn is stored as `{"role": "assistant", "content": <the response's content, as received>}`.
  - `usage` is added to the running totals. `truncated` is true when `stop_reason` is `max_tokens`.
  - If `send` fails, the exception propagates and the history is left as it was before the call (no dangling user
    turn, so roles keep alternating on the next call).
  - Blank text (empty or only whitespace) is refused before anything is sent: `ValueError` (TypeScript `Error`,
    Java and Kotlin `IllegalArgumentException`).
- **`history()`** returns a copy of the turns; changing the copy changes nothing.
- **`totals()`** returns `input_tokens` and `output_tokens` summed over every successful turn.
- **`reset()`** clears history and totals.

## The cases

| Id | What it checks |
|---|---|
| `m1` | Every request carries the whole history in order, with assistant content as received |
| `e1` | Usage adds up over the turns |
| `e2` | A failed call leaves no dangling user turn |
| `e3` | The stop reason is reported and `max_tokens` marks the reply truncated |
| `e4` | `system` is top-level (and left out when absent); stop sequences are passed on |
| `e5` | Each request is a snapshot, and `history()` is a copy |
| `e6` | A blank turn is refused before anything is sent |
