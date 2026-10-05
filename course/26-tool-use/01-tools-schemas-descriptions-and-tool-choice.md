# Tools: where they run, schemas, descriptions and tool_choice

**Level:** Developer · **Module 26:** Tool use · **Page 1 of 3**
**Exams:** DV5; A1.1, A2.1

**After this page you can** say where each kind of tool runs and what your code is responsible for, write a tool definition that
the model can use well, decide when to add `strict: true`, choose a `tool_choice` value, and name the models that reject a forced
choice.

Checked against the Claude API documentation (Tool use overview, How tool use works, Define tools and Strict tool use) on
2026-10-03. The loop on the next page was run offline in the course container (`anthropic` 1.11.0, `@anthropic-ai/sdk` 0.131.0).

## Why it matters

Tool use is how a model reaches anything outside the conversation: your database, a clock, a file, the web. Every agent, every
retrieval system and every coding assistant is built on it. The exam asks who runs a tool, what the model sees of it, how a
call is shaped, and what to do when a call is wrong. It also tests small facts with large effects, such as which `tool_choice`
values still work on the newest models.

## The idea

### The contract

"The model never executes anything on its own." The documentation calls tool use a contract: you specify the operations and the
shape of their inputs and outputs, and Claude determines when and how to call them. The model emits a structured request; code
runs it; the result flows back into the conversation. Claude "never sees your implementation; it only sees the schema you provided
and the result you returned."

### Where tools run

| Kind | Who runs it | Examples | What your code does |
|---|---|---|---|
| User-defined tools | your application | a database query, an internal API | write the schema, run the call, return a `tool_result` |
| Anthropic-schema client tools | your application | `memory`, `bash`, `text_editor`, `computer`, `browser` | run the call; the schema is trained in |
| Server tools | Anthropic's infrastructure | `web_search`, `web_fetch`, `code_execution`, `tool_search` | enable the tool and read the answer |

The first two are client tools. A client tool call ends the response with `stop_reason: "tool_use"` and one or more `tool_use`
blocks, and your code drives a loop. For a server tool, "you never construct a `tool_result` block"; the server runs the loop and the
result appears in the same response, unless the loop pauses or the same group of parallel calls holds one of your client tools. An
Anthropic-schema tool is worth using instead of an equivalent of your own because "Claude has been optimized on thousands of
successful trajectories that use these exact tool signatures".

### When a tool is the right answer

Tool use fits "Actions with side effects", "Fresh or external data", "Structured, guaranteed-shape outputs" and "Calling into
existing systems". It does not fit when the model can answer from training alone, when there is nothing to execute, or when the
round trip would cost more than the work. One sign from the documentation is worth remembering: "if you're writing a regex to
extract a decision from model output, that decision should have been a tool call."

### A tool definition

A user-defined tool has a `name` that matches the regex `^[a-zA-Z0-9_-]{1,128}$`, a `description`, and an `input_schema` that is a
JSON Schema object. An optional `input_examples` field holds example inputs, each valid for the schema; an invalid example returns a
400 error. The loop example on the next page defines two tools with a one-city input each.

The documentation says of the description: "Provide extremely detailed descriptions. This is by far the most important factor in tool
performance." A description explains what the tool does, when to use it and when not to, what each parameter means, and what the tool
does not return. It asks for "at least 3–4 sentences for each tool description". Four more rules shape a set of tools:

- **Fewer, broader tools.** Group related operations into one tool with an `action` parameter, instead of one tool per action.
- **Namespaced names.** Prefix names with the service, for example `github_list_prs` and `slack_send_message`.
- **High-signal results.** Return stable identifiers and only the fields the model needs for its next step, since bloated results
  waste context.
- **Ask for an explanation, not reasoning.** A parameter that asks for the model's thinking "may lead to a `reasoning_extraction`
  refusal"; ask for a short explanation or the supporting evidence.

The documentation also notes that when the model lacks a required parameter, "Claude Opus is much more likely to recognize that a
parameter is missing and ask for it", while a smaller model "might also infer a reasonable value". Description wording and schema
`required` lists are how you reduce guessing.

### Strict tool use

With `strict: true` as a top-level property of the tool, "Tool `input` strictly follows the `input_schema`", and the tool name is
always valid. The documentation's example: a booking tool that needs `passengers: int` could receive `"two"` or `"2"` without strict
mode; with it, "the response always contains `passengers: 2`". The schema uses the subset described in module 25, with
`additionalProperties` set to `false`. Strict mode guarantees the shape of the call and nothing about whether the call is sensible:
a valid city name that your database lacks is still your handler's problem, and the next page shows the error result for it.

### The four values of tool_choice

| Value | Meaning |
|---|---|
| `auto` | "allows Claude to decide whether to call any provided tools or not"; the default when `tools` are given |
| `any` | "tells Claude that it must use one of the provided tools, but doesn't force a particular tool" |
| `tool` | "forces Claude to always use a particular tool", with a `name` |
| `none` | "prevents Claude from using any tools"; the default when no `tools` are given |

For `any` and `tool`, "the API prefills the assistant message to force a tool to be used", so the model writes no explanation before the
`tool_use` block. To get both a tool call and an explanation, use `auto` and say in a user message to use the tool.

### Forced choice is not available everywhere

Where forced tool use "isn't supported, `tool_choice: {"type": "any"}` and `tool_choice: {"type": "tool", "name": "..."}` fail, while
`tool_choice: {"type": "auto"}` (the default) and `tool_choice: {"type": "none"}` still work". The restrictions in the documentation:

- Manual extended thinking (`thinking: {type: "enabled"}`): `any` and `tool` result in an error.
- Claude Opus 5.5, Claude Sonnet 5.5, Claude Fable 5.1 and Claude Mythos 5.1: `any` and `tool` return a 400 error, "regardless of
  thinking settings". Use `auto` with strict tool use, or structured outputs "when you need a response in a fixed JSON shape".

Changing `tool_choice` between requests also has a cost: it invalidates cached message blocks, while tool definitions and system
prompts stay cached.

### What tools cost

"Tool use requests are priced based on" the input tokens sent, "including in the `tools` parameter", the output tokens, and for server
tools usage-based charges. The API adds a special system prompt when `tools` is present. For Claude Opus 5.5 and Claude Sonnet 5.5 the
documentation lists 286 tokens for `auto` and `none`. Long descriptions and big schemas are paid for on every request, which
is a reason to keep results small and to cache the definitions.

### Steering when the model calls a tool

With `auto` the model decides each turn. A light instruction such as "Use the tools to investigate before responding." increases tool use;
"Use your judgment about whether to call a tool or respond directly." keeps it conservative. If you need a guarantee and the model
accepts a forced choice, set `tool_choice`; otherwise prompt for it.

## Traps

1. **Forcing a tool on a model that rejects it.** On Claude Opus 5.5, Claude Sonnet 5.5, Claude Fable 5.1 and Claude Mythos 5.1 the
   request returns a 400 error. Use `auto` with strict tools or structured outputs.
2. **A one-line description.** The model chooses a tool and fills its parameters from the description and the schema alone.
   Say when to use the tool, when not to, and what it returns.
3. **Believing strict mode checks meaning.** It fixes types and required fields. It does not know whether a city exists, whether a
   user may call the tool, or whether the call is wise.

## Quiz

1. A team wants Claude Opus 5.5 to call a function that they wrote. Who executes it?
   - **a**: Anthropic's servers, after the tool is named in the request
   - **b**: The SDK client, without any involvement from their program at all
   - **c**: The model, inside the reply that it is generating for the user
   - **d**: Their program, after it reads the `tool_use` block in the reply

2. Which description follows the documentation's advice for a tool named `get_stock_price`?
   - **a**: Returns the latest USD quote for a listed ticker. Use it when asked what one share costs now; no history comes back.
   - **b**: A general finance tool that the model may call for anything about companies, markets, prices or news anywhere in the world
   - **c**: Gets the stock price for a ticker symbol, so the model can call it whenever any ticker is named in a message
   - **d**: Price lookup tool, which the model should call on every single message that it receives from any user of the application

3. A request to Claude Sonnet 5.5 sets `tool_choice` to `{"type": "any"}`. What is the outcome?
   - **a**: A reply that explains the plan and then calls a tool
   - **b**: A call to the first tool in the list, whatever the user said
   - **c**: A 400 error, because the model does not support that value
   - **d**: A silent fall back to `auto`, with a warning in the response header

<details>
<summary>Answer key</summary>

1. **d**. The page says a client tool call ends with `stop_reason: "tool_use"` and "your code drives a loop". *a* is ruled out because Anthropic's servers run only the server tools, where "you never construct a `tool_result` block". *c* is ruled out because "The model never executes anything on its own." *b* is ruled out because Claude "never sees your implementation", and the SDK has no handler for an operation that only the program defines.
2. **a**. The page asks for a description that explains what the tool does, when to use it and when not to, and what it does not return, and this one says all of that. *c* is ruled out because a short description is the pattern to avoid: "Provide extremely detailed descriptions." *b* is ruled out because it never says "what the tool does not return". *d* is ruled out because the page asks for "when to use it and when not to", not for a call on every message.
3. **c**. The page says that for Claude Opus 5.5, Claude Sonnet 5.5, Claude Fable 5.1 and Claude Mythos 5.1, "`any` and `tool` return a 400 error". *b* is ruled out because `any` "doesn't force a particular tool", and the request is rejected before any tool is chosen. *a* is ruled out because for the values that work, "the API prefills the assistant message to force a tool to be used", so no explanation comes first. *d* is ruled out because the page's advice is to choose it yourself: "Use `auto` with strict tools or structured outputs."

</details>
