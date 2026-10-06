# Practice: assemble a streamed message

A streamed reply arrives as events. Write the function that folds them into the message a non-streaming call would
have returned. The events are the parsed `data:` objects, in order; the tests supply them.

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

## What is already written, and what you write

The starter is a working assembler with six gaps cut out of it. The event loop, the block start, the stop flag, the index order of `content` and the `StreamError` type are written and
correct. Each gap is one small function with its signature, a comment that says what it receives and returns with one example, and the cases it unlocks. A gap returns a neutral
value, so the starter runs and fails every case on an assertion. To see what a gap receives, log its input with the `log` line at the top of the file (`log.debug(...)`); a run shows
the lines under the failing case. The names below are Python's; TypeScript, Java and Kotlin use the camel-case forms (`startMessage`, `applyDelta`, `finishBlock`, `applyMessageDelta`, `raiseError`, `checkComplete`).

1. `_start_message` unlocks `m1` and `e5`: the message a `message_start` begins, with an empty `content` and a copy of its usage.
2. `_apply_delta` unlocks `m1` and `e6`: fold one delta (text, thinking, signature or a JSON fragment) into its block.
3. `_finish_block` unlocks `e1`: join and parse a tool block's fragments, `{}` when there are none.
4. `_apply_message_delta` unlocks `m1` and `e5`: the stop reason and sequence, and the cumulative usage keys.
5. `_raise_error` unlocks `e3`: an `error` event raises `StreamError`.
6. `_check_complete` unlocks `e4`: a stream that never reached `message_stop` raises `StreamError`.

`e2` needs the loop to leave `ping` and unknown events alone, and it is written, so it passes once the others do. About twenty lines in all.

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
