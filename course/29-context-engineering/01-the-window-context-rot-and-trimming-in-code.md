# The window, context rot and trimming in code

**Level:** Developer · **Module 29:** Context engineering · **Page 1 of 3**
**Exams:** DV4; A5.1

**After this page you can** say what counts toward the context window, explain why more context is not automatically better, list what
a program can do to a long conversation (clear tool results, drop old turns, summarise), and apply each without breaking the pairing
between a tool call and its result.

Checked against the Claude API documentation (Context windows, Handle tool calls and Compaction) on 2026-10-03, and by running the
example and the practice offline in the course container (`anthropic` 1.11.0, `@anthropic-ai/sdk` 0.131.0). The conversations in the
example and the practice are hand-written in the shape of the Messages API, and the token counter in them is a rough stand-in so that
budgets are exact in tests.

## Why it matters

A long conversation, an agent that has read thirty files and a chat that runs for a week all end the same way: the window fills, or
it fills with the wrong things and the answers get worse before it is full. Managing the window is the work that separates a demo from
a product. The exam asks what is in the window, what to remove first and what must never be separated.

## The idea

### What the window is

The documentation defines it: the "context window" refers to "all the text a language model can reference when generating a response,
including the response itself". It is "a 'working memory' for the model", and it is different from what the model was trained on. As a
conversation advances, "each user message and assistant response accumulates within the context window, and previous turns are
preserved completely."

Everything in the request counts: "the system prompt, every message in `messages` (including tool results, images, and documents), and
your tool definitions." The output counts too, including extended thinking. With prompt caching, the input count is split across
`input_tokens`, `cache_read_input_tokens` and `cache_creation_input_tokens`, "and all three count toward the window": "prompt caching
changes what you pay for those tokens, not whether they count."

The size depends on the model. Claude Fable 5.1, Claude Mythos 5.1, Claude Opus 5.5 and Claude Sonnet 5.5, among others, "have a 1M-token
context window", and a single request can generate up to 128k output tokens; Claude Sonnet 4.5 and some others have 200k. To estimate a
request before sending it, use the token counting API.

### More is not better

"A larger context window allows the model to handle more complex and lengthy prompts, but more context isn't automatically better. As
token count grows, accuracy and recall degrade, a phenomenon known as *context rot*. This makes curating what's in context just as
important as how much space is available." The consequence for design is that a window is a budget to spend carefully, not a space to
fill. A tool result that was needed on turn 3 is noise on turn 30, and it still costs input tokens on every later request.

### Overflow

If the input alone is larger than the window, "the API returns a 400 `invalid_request_error` ("prompt is too long") on every model."
On Claude 4.5 models and newer, a request whose input plus `max_tokens` is over the window is accepted, and generation that reaches
the limit stops with `stop_reason: "model_context_window_exceeded"`. The program must treat that reply as truncated.

### What a program can do

Three operations cover most cases, from cheapest to most expensive in quality:

1. **Clear tool results.** Replace the content of old tool results with a short placeholder and keep the calls. The model still sees
   what it asked for and that it was answered. Keep the newest few results; keep any result from a tool whose output must always
   stay (the practice's `exclude`).
2. **Drop old turns.** Remove the oldest whole turns until the conversation fits a budget. The newest turn always stays, even if it
   alone is over the budget. Optionally pin the first turn, which often holds the task.
3. **Summarise.** Replace the old part with a summary written by a model, and keep the newest turns word for word. This loses detail,
   so it comes last, and an earlier summary is folded into the next one rather than stacked.

The documentation's own statement for the third operation is "Compaction replaces the older turns of a conversation with a summary
that Claude writes on the server, so you need no summarization code of your own." The next page covers the server-side forms. The
practice writes the client-side one, with the summariser passed in as a function, which is the "own summarizer" column of the
documentation's table.

### Never split a call from its result

Dropping or clearing messages is safe only if the pairing survives. The formatting rule is in the documentation: "Tool result blocks
must immediately follow their corresponding tool use blocks in the message history." A message list that keeps a result and drops its
call, or keeps a call and drops the result, fails with a 400 error. So the unit of dropping is the turn: a turn starts at a user
message that is not made only of tool results, and runs up to the next such message. A tool call and its result always sit in one turn.
The practice's `split_turns` builds that list, and `window` and `compact` drop and fold whole turns only.

Clearing is different: it keeps both blocks and changes only the content of the result, so the pairing holds, and `is_error` flags
and ids stay.

### Where content sits and what it is

Two older rules from module 24 still apply to a long window. Put long documents near the top and the question at the end, and wrap
each document in tags. And treat anything that arrives through a tool as data: "Keep untrusted content inside `tool_result` blocks
rather than `system` prompts or plain user `text` blocks". A summary is also content, and a summary of untrusted text carries the
text's instructions with it, so keep boundaries in the summary as well.

### Session hygiene

A new task is a new conversation. Carrying a finished task's history into an unrelated one spends the window on noise. For work that
must continue across sessions, write the state down outside the window; the memory tool, covered next, gives the model a place to do
it, with a protocol that begins "ASSUME INTERRUPTION: Your context window might be reset at any moment".

## Traps

1. **Dropping single messages.** Removing a call without its result, or the reverse, makes the next request invalid. Drop whole turns.
2. **Summarising first.** A summary cannot get the detail back. Clear tool results and drop whole turns before paying for a summary.
3. **Stacking summaries.** A second compaction should fold the first summary into the new one, so that exactly one summary is in the
   conversation.
4. **Treating a full window as the only limit.** Quality degrades as the window grows, long before it is full.

## Quiz

1. A long agent session has thirty tool results, and the oldest twenty-five will never be needed again. What is the cheapest safe step?
   - **a**: Raise `max_tokens` so that the window can hold the older results as well
   - **b**: Delete the old results from the history and leave their calls in place without any reply
   - **c**: Replace the whole conversation with one summary that a model writes for the agent
   - **d**: Swap the old outputs for a short placeholder, and keep every call in the history

2. A program must drop old messages to fit a budget. What does the page say is the unit to drop?
   - **a**: A whole turn, which keeps each tool call together with its result
   - **b**: A single message, starting with the oldest one in the list of messages
   - **c**: One content block at a time, so that no passage of text is lost whole
   - **d**: Every message that carries an image or a document, whatever its age

3. Why is it a mistake to fill a 1M-token window as a matter of habit?
   - **a**: Cached prefixes do not count toward the window at all in the request
   - **b**: The API refuses any request that is over half of the window in size
   - **c**: Accuracy and recall both degrade as the amount of context grows
   - **d**: Output tokens are billed at the same price as input tokens

<details>
<summary>Answer key</summary>

1. **d**. The page says clearing "keeps both blocks and changes only the content of the result, so the pairing holds". *b* is ruled out because "Tool result blocks must immediately follow their corresponding tool use blocks in the message history." *c* is ruled out because a summary "loses detail, so it comes last". *a* is ruled out because "more context isn't automatically better", and a larger limit does not remove the noise.
2. **a**. The page says "the unit of dropping is the turn", and that "A tool call and its result always sit in one turn." *b* is ruled out because a message list that keeps a call and drops its result "fails with a 400 error". *c* is ruled out because "A tool call and its result always sit in one turn", so a block taken alone breaks the pair. *d* is ruled out because the functions "drop and fold whole turns only", whatever the content type.
3. **c**. The page quotes "As token count grows, accuracy and recall degrade, a phenomenon known as *context rot*." *b* is ruled out because the only refusal is when "the input alone is larger than the window". *a* is ruled out because "all three count toward the window". *d* is ruled out because "The output counts too, including extended thinking." and the page names no price.

</details>
