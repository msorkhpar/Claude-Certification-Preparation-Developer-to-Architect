# Practice: assemble a streamed message

A streamed reply arrives as events. Write the function that folds them into the message a non-streaming call would
have returned. Pick your language folder (`python`, `typescript`, `java` or `kotlin`), open `starter/` and edit the
file there. The events are the parsed `data:` objects, in order; the tests supply them.

## The function

**`assemble(events)`** returns a message (a map) with `id`, `type`, `role`, `model`, `content`, `stop_reason`,
`stop_sequence` and `usage`. `StreamError` is given in the starter (`error_type` and `message`; TypeScript, Java and
Kotlin spell them `errorType` and `detail`). Java and Kotlin also get the `Json` helper.

| Event | What to do |
|---|---|
| `message_start` | Take the message from it (`id`, `role`, `model`, `usage`...) with an empty `content` |
| `content_block_start` | Start the block at `index` from its `content_block` |
| `content_block_delta` | `text_delta`: append `text`. `thinking_delta`: append `thinking`. `signature_delta`: set `signature`. `input_json_delta`: keep the `partial_json` fragment for the block |
| `content_block_stop` | For a `tool_use` block, join its fragments and parse them as the block's `input`; no fragments (or only empty ones) mean `{}` |
| `message_delta` | Set `stop_reason` and `stop_sequence` from its `delta`; each key in its `usage` replaces the same key in the message's usage (the output count is **cumulative**: replace, do not add) |
| `message_stop` | The stream finished normally |
| `error` | Raise `StreamError` with the error's `type` and `message` |
| `ping`, and any other type | Ignore it: the API may add event types |

`content` lists the blocks in **index order**. A stream that ends **without `message_stop`** raises `StreamError`
with error type `incomplete_stream`.

## The cases

| Id | What it checks |
|---|---|
| `m1` | Text deltas are joined and the message is complete |
| `e1` | Tool input is the fragments joined, then parsed; empty input is `{}` |
| `e2` | `ping` and unknown event types are ignored |
| `e3` | An error event raises with its type and message |
| `e4` | A stream that ends before `message_stop` is an error, not a short message |
| `e5` | Usage: input tokens from the start, the cumulative output tokens from the end |
| `e6` | Blocks keep their index order; thinking fields are assembled |

Run the tests with the command in the language folder's `run.sh` (Python, TypeScript) or its build file.
