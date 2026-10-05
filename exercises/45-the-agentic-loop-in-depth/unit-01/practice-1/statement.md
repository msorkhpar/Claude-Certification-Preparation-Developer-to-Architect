# Practice: an agent loop that is driven by the stop reason

The loop of module 26 ran until the model stopped asking for tools. This practice builds the loop an architect would ship: one that
ends only when the model's own stop reason says so, treats the turn limit as a backstop and not as the way a run normally ends, and
keeps the conversation in a state the API accepts. Pick your language folder (`python`, `typescript`, `java` or `kotlin`), open
`starter/` and edit the file there. Nothing here touches the network: the model is a function that the tests script.

## The given parts

| Name | Meaning |
|---|---|
| `model` | a function from the list of messages so far to one reply: `{"stop_reason": ..., "content": [blocks]}`; a block is `{"type": "text", "text": ...}` or `{"type": "tool_use", "id": ..., "name": ..., "input": {...}}` |
| `tools` | a map from a tool name to a handler: a function from the input map to a string; it may throw |
| `task` | the first user message, a string |

## What is already written, and what you write

The starter is a working loop with six small gaps cut out. The loop itself, the tool runner and the outcome are written and correct; each gap is a
small function with its signature, a comment that says what it receives and returns with one example, and the cases it unlocks. A gap returns a
neutral value, so the starter runs and fails the cases on an assertion. To debug a gap, log its input with the `log` line at the top of the
file; a run shows the lines under the failing case. Write them in this order (the TypeScript, Java and Kotlin names are the camel-case forms):

1. `_text` unlocks `m1` and `e5`: the text blocks of a reply joined together.
2. `_calls` unlocks `e2` and `e6`: the `tool_use` blocks of a reply, in order.
3. `_tool_result` unlocks `m1`, `e2` and `e3`: one result block, with `is_error` only when flagged.
4. `_status_for` unlocks `m1` and `e5`: the status for each stop reason other than `tool_use`.
5. `_at_limit` unlocks `e4`: whether the turn limit has been reached before the next model call.
6. `_snapshot` unlocks `m1`: the copy of the messages the model is handed.

About ten lines in all. The sections below describe the whole loop.

## What to write

`run_agent(model, tools, task, max_turns=8)` (`runAgent` in TypeScript, Java and Kotlin) returns
`{"status", "text", "turns", "messages"}`.

1. Start with the one user message `{"role": "user", "content": task}`. One **turn** is one call of `model`. Hand the model a copy of the
   messages so far (a list with the same items), so that later changes do not rewrite what it was given.
2. Keep every reply exactly as it came: add `{"role": "assistant", "content": <the reply's content list>}`. `text` is always the text
   blocks of the latest reply joined together (empty when it has none).
3. Decide what happens next from `stop_reason` and from nothing else. Words in the text, such as "done" or "I will call a tool", never
   decide anything.
   - `tool_use`: run every `tool_use` block of the reply, in order, and answer all of them in **one** user message whose content is the
     list of results and nothing else: `{"type": "tool_result", "tool_use_id": <the call's id>, "content": <the handler's string>}`. A
     handler that throws, or a name that is not in `tools`, still gets a result, with `"is_error": true` added: the content is the
     exception's message, or `Unknown tool: NAME`. The run goes on. A result without an error has no `is_error` key.
   - `end_turn` or `stop_sequence`: status `done`.
   - `max_tokens`: status `truncated`. `refusal`: status `refused`.
   - any other value: status `unexpected`. The loop does not guess what a stop reason it has never seen means.
   - `tool_use` with no `tool_use` block in the reply: status `malformed`. There is nothing to answer, and an empty user message would be
     refused by the API, so nothing more is sent.
4. `max_turns` is a backstop. Check it **before** each model call: when `turns` already equals `max_turns` and the model has not ended its
   turn, return status `max_turns` with the messages as they stand (they end with the last tool results, so the run can be resumed).
   A reply that ends the turn on the last allowed call is still `done`. `turns` is the number of model calls made.

## Why each step is there, and what you should see

1. **Keep the conversation as the model's memory** (steps 1 and 2). The exam expects the results of the tools to be appended to the history so that the model can reason about its next action, and the replies to be kept as they came. *You should see* the second model call receive the first reply and its tool results, and a change that you make to your own list leave an earlier call's input as it was.
2. **Decide on the stop reason alone** (step 3, first part). The exam names reading the text for a completion signal as an anti-pattern, because a reply can say "done" and call a tool in the same breath. *You should see* a reply that says "done" with a tool call continue, and a reply that says "I will call a tool" with the end of its turn stop.
3. **Answer every call, once, in one message** (step 3, `tool_use`). The API needs a result for every call of a turn, and a failure is information for the model, not a reason to stop. *You should see* all the results of a turn together, in order, with the failing or unknown tool flagged and the run going on.
4. **Give the turn limit its proper job** (step 4). The exam rejects an arbitrary cap as the main stopper and accepts it as a backstop. *You should see* an endless run end with its own status and the messages intact, and a run that finishes on the last allowed call still report `done`.
5. **Give every other value a status** (step 3, the rest). A cut-off, a refusal and a value you have never seen are different events. *You should see* `truncated`, `refused` and `unexpected` come out as themselves, and nothing more sent after a malformed reply.

## The cases

| Id | What it checks |
|---|---|
| `m1` | A run alternates model and tools until the model ends its turn, and the model sees the growing conversation |
| `e1` | The stop reason decides: a reply that says "done" but calls a tool continues, and one that announces a tool but ends its turn ends |
| `e2` | Every call of a turn is answered in one user message, in order, with the assistant turn kept as it came |
| `e3` | A failing or unknown tool becomes an error result and the run goes on |
| `e4` | The turn limit is a backstop: it ends an endless run with its own status and does not cut off a run that ends on the last turn |
| `e5` | A cut-off, refused, stop-sequence or never-seen stop reason ends the run with the right status |
| `e6` | A `tool_use` reply with no tool call is malformed and nothing more is sent |

Run the tests with the command in the language folder's `run.sh` (Python, TypeScript) or its build file (Java, Kotlin).
