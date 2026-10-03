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
