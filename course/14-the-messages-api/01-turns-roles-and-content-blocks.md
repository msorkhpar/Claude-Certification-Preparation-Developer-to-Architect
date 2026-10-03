# Turns, roles, the system prompt and content blocks

**Level:** Developer · **Module 14:** The Messages API · **Page 1 of 3**
**Exams:** DV1; A1.1

**After this page you can** build a valid `messages` list for a multi-turn conversation, say where the system prompt
goes, and name the content blocks a request and a reply can hold.

Checked against the Claude API documentation (Using the Messages API, API errors, Handling stop reasons) on
2026-10-02 for `claude-sonnet-5-5`, `claude-opus-5-5` and `claude-haiku-4-5-20251001`. Replies shown are
hand-scripted and labelled illustrative.

## Why it matters

Every Claude feature in the rest of the course is a `messages` list with something added. Tool use adds blocks to it,
agents loop over it, caching marks a prefix of it. The agentic loop of exam topic A1.1 is nothing more than "append the
model's turn, append the result, call again". A developer who has the shape of the list wrong writes bugs that look
like model failures: a bot that "forgets", a call that returns a 400 after a model upgrade, a conversation that cannot
be resumed because a block was dropped on the way into the database.

## The idea

### The API is stateless

The documentation states it in one sentence: "The Messages API is stateless, which means that you always send the
full conversational history to the API." The model keeps nothing between requests. A conversation exists only as the
list your code sends each time, and the API does not care where the earlier turns came from: "Earlier conversational
turns don't necessarily need to actually originate from Claude. You can use synthetic `assistant` messages."

So a three-turn exchange is three requests, each longer than the last:

<!-- illustrative -->
The `messages` list at each of three requests. Hand-written for this page.

```text
request 1   [user: "Capital of France?"]
request 2   [user: "Capital of France?", assistant: "Paris.", user: "Since when?"]
request 3   [user: ..., assistant: ..., user: ..., assistant: "...", user: "Name its river."]
```
<!-- /illustrative -->

### Roles

A message has a `role` and `content`. The two roles you send are **`user`** and **`assistant`**, and a conversation
alternates between them, starting with a user turn. The reply to a request is always an assistant message.

The **system prompt** is not a message. It is a **top-level `system` field** of the request, next to `messages`, for
"instructions that apply from the start" in the documentation's words. This is a common source of bugs for people who
know other chat APIs where the system prompt is the first entry of the list. Here it is a separate field, and a
message with the role `system` as the first entry is not allowed.

One newer feature changes the picture slightly: on Claude Fable 5.1, Opus 5.5 and Sonnet 5.5 (and some others named
on the page), you can include a message with `"role": "system"` **after a user turn** to add an instruction part-way
through a conversation. It is applied as a system instruction, takes precedence over the top-level field when the two conflict, and because it
is appended at the end it does not invalidate a cached prefix that came before it. It must follow a user turn and cannot
be the first entry in `messages`. Use the top-level field for instructions that hold from the
first turn, and the mid-conversation message for instructions that only become relevant later.

### Content: a string or a list of blocks

`content` can be a plain string, which is shorthand for one text block, or a list of **content blocks**, each with a
`type`. The blocks you will meet in this course:

| Block type | Appears in | Meaning |
|---|---|---|
| `text` | requests and replies | Text |
| `image` | requests | An image, from `base64`, a `url` or an uploaded `file`; the documented media types are `image/jpeg`, `image/png`, `image/gif` and `image/webp` |
| `tool_use` | replies | The model asks your code to run a tool: an `id`, a `name`, an `input` object (module 22) |
| `tool_result` | requests | Your answer to a `tool_use`, matched by `tool_use_id` (module 22) |
| `thinking`, `redacted_thinking` | replies | The model's reasoning, when thinking is on (module 18) |

A reply's `content` is always a list, because it can hold several blocks: text, then a `tool_use`, for instance. The
reply you store and send back next time is that list, **as received**.

<!-- illustrative -->
An assistant turn that holds two blocks, and the user turn that answers the tool request. Hand-written for this page.

```text
assistant  content: [ {type: text, text: "Let me check."},
                      {type: tool_use, id: "toolu_1", name: "get_weather", input: {city: "Paris"}} ]
user       content: [ {type: tool_result, tool_use_id: "toolu_1", content: "14 degrees, clear"} ]
```
<!-- /illustrative -->

### Rules that return a 400

Three rules about the list appear in the documentation as errors, and the exam asks about all three:

1. **No prefill on current models.** Putting an assistant message last used to steer the start of a reply. "Claude 4.6
   and later models" do not support it, and a request that does returns a 400 `invalid_request_error` saying the
   conversation "must end with a user message". Use structured outputs where the model supports them, or system-prompt
   instructions.
2. **Thinking blocks come back unchanged.** If the latest assistant message holds `thinking` or `redacted_thinking`
   blocks and you edit, reorder or drop them before sending it back, the request returns a 400. With tool use, every
   such block from the turn must be passed back exactly as received.
3. **After a tool result, send only the result.** The stop-reasons page warns that adding text blocks right after tool
   results can teach the model to expect user input after every tool call, and produce an empty reply. Send the
   `tool_result` blocks alone.

All three have one root: the list is a record of what happened, and the API checks it against what it produced.

## Traps

1. **Assuming the API remembers.** A bot that sends only the newest message gets answers with no memory of the
   conversation. Nothing is wrong with the model; the history was never sent.
2. **Prefilling on a current model.** Code written for an older model that ends the list with `{"role": "assistant",
   "content": "{"}` returns a 400 after the upgrade. Move the format request into structured outputs or the prompt.
3. **Rebuilding assistant turns from text.** Storing only the text of a reply and sending that back drops `tool_use`
   and `thinking` blocks, and the next request can fail or lose the model's own context. Keep the content list as
   received.

## Quiz

1. A support bot works well in testing, where the developer types fast, but real users report that it answers each
   message as though it were the first. The code sends the newest user message and the system prompt each time. What
   is the cause?
   - **a**: The history of earlier turns is not part of the requests
   - **b**: The system prompt should hold a summary of the earlier talk
   - **c**: The output limit is too low for the model to recall earlier turns
   - **d**: The model needs a different tier for multi-turn memory

2. After a move from Claude Sonnet 4.5 to Claude Sonnet 5.5, every request of a JSON-extraction job returns a 400.
   The code ends each list with a message from the model's side that holds an opening brace. Which fix is best?
   - **a**: Put the brace in a message with the role system at the end of the list
   - **b**: Keep the trailing message but lower the sampling temperature to zero
   - **c**: Drop the final entry and ask for the format through structured outputs
   - **d**: Move the brace into the system field so that the reply still begins with it

<details>
<summary>Answer key</summary>

1. **a**. The page says "the Messages API is stateless", so the client has to send the whole history each time. *b* is ruled out because the system prompt is a top-level field for "instructions that apply from the start", and a hand-made summary is not how history is kept. *c* is ruled out because "a conversation exists only as the list your code sends each time", and no output limit supplies a missing list. *d* is ruled out because "the model keeps nothing between requests", whichever tier answers.
2. **c**. Prefill is not supported on current models, and the page names structured outputs and system-prompt instructions as the replacements. *b* is ruled out because "a request that does returns a 400 invalid_request_error", whatever the temperature. *a* is ruled out because a system-role message is for "instructions that only become relevant later" and does not pre-fill a reply. *d* is ruled out because the field is for "instructions that hold from the first turn", and an instruction does not start a reply with a given character.

</details>
