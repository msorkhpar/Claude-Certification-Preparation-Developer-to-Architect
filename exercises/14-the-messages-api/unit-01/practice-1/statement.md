# Practice: a conversation client, graded on the state it keeps

The Messages API is stateless: every request must carry the whole conversation. Write the client that keeps it.
Pick your language folder (`python`, `typescript`, `java` or `kotlin`), open `starter/` and edit the file there.

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

Run the tests with the command in the language folder's `run.sh` (Python, TypeScript) or its build file.
