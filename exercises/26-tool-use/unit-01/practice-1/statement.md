# Practice: a tool loop against a scripted model

Claude never runs your tools. It answers a request with `stop_reason` of `tool_use` and one or more `tool_use` blocks;
your code runs them, sends the results back in a single user message, and asks again until the model ends its turn.
Write that loop, with the limits and the error handling that keep it from hanging or lying. Pick your language folder
(`python`, `typescript`, `java` or `kotlin`), open `starter/` and edit the file there. The rules come from the Claude
documentation on tool use, handling tool calls, parallel tool use, stop reasons and forced tool use, read on 2026-10-03; the
lesson pages explain them. Nothing here touches the network: the model is a function that you call, and the tests give it
scripted replies in the shape of a Messages API response.

## The given types

| Name | Meaning |
|---|---|
| `ask(request)` | the model: takes a request `{"model", "max_tokens", "messages", "tools", optionally "tool_choice"}` and returns a reply `{"content": [blocks], "stop_reason": ...}` |
| tool | a name, a description, an `input_schema` and a handler that takes the input map and returns a string or any JSON value |
| `ToolError` | a handler may throw it (or any other error) to say it failed |
| `RequestError` | what your code throws, before any call, for a request the API would reject with a 400; `field` names the part |

## What is already written, and what you write

The starter is a working tool loop with seven small gaps cut out of it. Everything that is plumbing is written and correct: the types, the request that
is built for every turn, the assistant message that is appended after every reply, the error results for an unknown tool and for a handler that throws,
the `pause_turn` continue and the `max_turns` result. Each gap is a small function with its signature, a comment that says what it receives and returns
with one example, and the cases it unlocks. A gap returns a neutral value, so the starter runs and fails every case on an assertion. The TypeScript, Java
and Kotlin names are the camel-case forms (`checkChoice`, `missingInputs`, `resultContent`, ...); `_turn_numbers` is `lastTurn` there. Write them in this order:

1. `_check_choice` unlocks `e5`: the `RequestError` for a `tool_choice` the API would reject.
2. `_missing_inputs` unlocks `e2`: the required keys that a call leaves out.
3. `_result_content` unlocks `e6`: a string as it is, any other value as JSON text.
4. `_tool_results` unlocks `m1` and `e1`: one result per `tool_use` block, in order, none for a server tool block.
5. `_final_status` unlocks `e4`: `done`, `refused` or `truncated` from the stop reason.
6. `_turn_numbers` unlocks `e3`: the turns the loop may use, so that `max_turns` calls are made and no more.
7. `_sent_choice` unlocks `e5`: a forced choice on the first request only.

`m1` needs the fourth gap. About fifteen lines in all. To debug a gap, log its input with the `log` line at the top of the file; a run shows the lines
under the failing case. `run_agent` already logs the user text it receives.

## What to write

`run_agent(ask, tools, user_text, model="claude-sonnet-5-5", max_turns=8, tool_choice=None)` returns
`{"status", "text", "turns", "messages"}`: the final status, the text of the last reply, the number of model calls, and the
conversation.

- The first message has role `user` and the text. Each request lists the tools without their handlers, and carries `model`,
  `max_tokens` (1024) and a snapshot of the messages so far.
- After each reply, append it as an `assistant` message with its content blocks unchanged (text blocks and server tool
  blocks included).
- Stop reason `tool_use`: run every `tool_use` block, in order, and append ONE `user` message that holds one `tool_result` block
  per call (`tool_use_id` of the call, `content`, and `is_error` only on an error result), nothing else. Blocks of type `server_tool_use` are Anthropic's to run and get no
  result. A handler's string is sent as it is and any other value as JSON text.
- A call becomes a result with `is_error: true` and a message, and the loop goes on, when: the tool name is unknown
  (`Unknown tool: NAME`); a key listed in the schema's `required` is missing from the input (the handler is not called, and the message is `Missing required input: KEY`, with several missing keys joined by `, ` in the order of `required`); or the
  handler throws (the error's message).
- `end_turn` or `stop_sequence`: status `done`. `refusal`: status `refused`. `max_tokens` or any other stop reason: status
  `truncated`. `pause_turn` (a server tool's turn that was paused): call again with the assistant message appended and no user
  message.
- After `max_turns` model calls without an end, status `max_turns`.
- `tool_choice` is a map such as `{"type": "auto", "disable_parallel_tool_use": true}`. Check it before the first call and throw
  `RequestError`: field `tool_choice.type` unless the type is `auto`, `any`, `tool` or `none`;
  `tool_choice.disable_parallel_tool_use` if that key is present and is not a boolean; `tool_choice` for type `any` or `tool` on
  `claude-opus-5-5`, `claude-sonnet-5-5`, `claude-fable-5-1` or `claude-mythos-5-1`, which reject forced tool use; and
  `tool_choice.name` for type `tool` when no tool has that name. Send the choice as given on the first request. A forced choice
  (`any` or `tool`) must not repeat, or the model could never give a final answer: from the second request on, send
  `{"type": "auto"}` instead. `auto` and `none` are sent unchanged every time. With no `tool_choice`, send none.

## The cases

| Id | What it checks |
|---|---|
| `m1` | A tool call is run and its result sent back until the model ends its turn |
| `e1` | Parallel calls get one user message with every result in order |
| `e2` | A failing, unknown or malformed call becomes an error result and the loop goes on |
| `e3` | The number of turns is bounded |
| `e4` | Refusal and truncation end the loop, and a paused turn continues |
| `e5` | `tool_choice` is validated for the model, and a forced choice applies to the first request only |
| `e6` | Results that are not text are sent as JSON text |

Run the tests with the command in the language folder's `run.sh` (Python, TypeScript) or its build file.
