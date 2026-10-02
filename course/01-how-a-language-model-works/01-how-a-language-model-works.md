# Tokens, the context window and one token at a time

**Level:** Foundations · **Module 1:** How a language model works, for engineers · **Page 1 of 2**
**Exams:** all (AS1, AS3, DV2, DV4)

**After this page you can** explain how Claude turns a prompt into text, say what counts toward the
context window, and predict what happens when a conversation or a request grows too large.

Checked against the Anthropic documentation on 2026-10-02 (models overview, glossary, context windows,
token counting) for `claude-opus-5-5`, `claude-sonnet-5-5`, `claude-fable-5-1` and
`claude-haiku-4-5-20251001`. No live API call was made for this page.

## Why it matters

A support bot answers well for ten turns and then starts to contradict itself. A nightly job that worked on
last month's reports fails on this month's, with a "prompt is too long" error. A bill is double what the
word count predicted. All three are the same fact seen from different sides: Claude reads and writes
**tokens**, and everything it can use for one answer must fit in one **context window**. The exams ask about
this in scenario form, so this page builds the picture from the bottom.

## The idea

### One token at a time

A language model is trained to predict the next piece of text given the text so far. Claude's underlying
model is described in the glossary as an autoregressive model, pretrained "to predict the next word, given
the previous context of text in the document". At run time it does exactly that, in a loop:

1. The prompt is converted to a sequence of tokens.
2. The model computes, for every token it knows, how likely it is to come next.
3. One token is chosen (page 2 explains how) and appended to the sequence.
4. The loop repeats until the model emits a token that means "I am finished", or the output reaches the
   `max_tokens` limit you set, or the window is full.

There is no plan stored somewhere and no lookup of a finished answer. An answer is a long chain of single
choices, each made with everything before it in view. That is why wording early in an answer shapes
everything after it, and why a model can commit to a wrong path and keep going convincingly.

### Tokens are not words

A token is "the smallest individual unit of a language model" and "can correspond to words, subwords,
characters, or even bytes". The glossary gives a rule of thumb for Claude: a token is about **3.5 English
characters**, and it varies with the language. Common words are often one token; rare words, names, code
identifiers and numbers split into several. Module 2 shows why with a toy tokenizer you can run.

Two facts matter for engineers:

- **Tokenizers change between model generations.** The token counting page states that Claude Opus 4.7
  and later models use a newer tokenizer and the same text produces about 30 percent more tokens than on
  earlier models, and that you should recount against the model you plan to use. The models overview says
  the same thing in words: 1M tokens is roughly 555k words on the current tokenizer, where earlier models
  fit about 750k words.
- **Counts before sending are estimates.** The token counting endpoint is free and returns
  `input_tokens`, but the page calls the figure an estimate that "might differ by a small amount" from what
  a real request uses. Treat it as a budget tool, not an invoice.

### The context window is working memory

The context window is, in the glossary's words, the amount of text a model can "look back on and reference
when generating new text", and "a 'working memory' for the model", different from the training data. The
context windows page lists what counts toward it:

> Everything in the request counts toward the context window: the system prompt, every message in
> `messages` (including tool results, images, and documents), and your tool definitions. The output Claude
> generates for the turn, including its extended thinking, counts too.

Source: Context windows, Claude API documentation.

Sizes on 2026-10-02: 1M tokens for Claude Fable 5.1, Opus 5.5 and Sonnet 5.5, 200K for Claude Haiku 4.5; a
single response can be up to 128K tokens on the three large models and 64K on Haiku 4.5.

### The API remembers nothing

The Messages API is stateless. The documentation describes the standard behaviour: each user message and
assistant response accumulates in the window, "previous turns are preserved completely", and each turn's
input is "all previous conversation history plus the current user message". In practice your code resends
the whole history on every request. Chat products such as claude.ai manage this for you, and the page notes
they can manage the window on a rolling first-in, first-out basis; the API does not.

<!-- illustrative -->
This is a hand-scripted pair of requests, not a recorded exchange. The second request carries the first
exchange with it, which is the only reason the model "remembers".

```text
request 1   model: claude-sonnet-5-5   max_tokens: 200
  messages: [ user: "My order number is 4417. Where is it?" ]
reply 1     "Order 4417 shipped on Monday and should arrive Thursday."   (illustrative)

request 2   model: claude-sonnet-5-5   max_tokens: 200
  messages: [ user:      "My order number is 4417. Where is it?",
              assistant: "Order 4417 shipped on Monday and should arrive Thursday.",
              user:      "Can I change the address?" ]
reply 2     "Yes, if it has not left the depot. I can update order 4417 now."  (illustrative)
```
<!-- /illustrative -->

Drop the first two messages from request 2 and the model has no order number to talk about.

### More context is not automatically better

The context windows page is direct about this: "more context isn't automatically better. As token count
grows, accuracy and recall degrade, a phenomenon known as *context rot*. This makes curating what's in
context just as important as how much space is available." Larger windows raise the ceiling; they do not
remove the need to decide what goes in. Modules 29 and 64 teach the techniques; for now, keep the principle.

### What happens at the limit

- If the **input alone** exceeds the window, the API returns a 400 `invalid_request_error` ("prompt is too
  long") on every model.
- On Claude 4.5 models and newer, if input plus `max_tokens` is larger than the window, the request is
  still accepted; if generation then reaches the window limit it stops with
  `stop_reason: "model_context_window_exceeded"`. Older models returned a validation error instead.
- `max_tokens` is a cap on **output**, not on context. Raising it does not enlarge the window.

Because thinking tokens are part of the output and "billed as output tokens", a request that thinks a lot
leaves less room for the visible answer inside the same `max_tokens`. Module 19 returns to this.

## Examples

The two examples in this module run offline. Page 2 has the sampler. Here the example is the arithmetic of
a budget, in the form an exam scenario gives it.

A request has a 6,000-token system prompt, a 40,000-token document, a 2,000-token question and
`max_tokens` of 4,000, sent to a model with a 200K window (Haiku 4.5): input is 48,000 tokens, plus up to
4,000 of output, so 52,000 of 200,000. It fits with room for a long conversation. If the same document is re-sent on every turn of that
conversation, its cost is paid again each time, which is the problem prompt caching (module 20) was made
for, and the reason the window sizes in the model table of module 3 matter.

## Traps

1. **Counting words, not tokens.** A 10,000-word document is not 10,000 tokens, and the ratio differs by
   language, by content (code and numbers split more) and by model generation. Count with the endpoint for
   the model you will call.
2. **Believing the model remembers between calls.** It does not; your code resends the history. A "new
   chat" or a new request without history is a model with no memory of you.
3. **Reading the window size as a promise of quality.** A request that fits can still answer badly because
   the relevant sentence is buried. Fit is a hard limit; relevance is the real budget.

## Quiz

1. A nightly job sends one request per report to a model with a one-million context window. The reports
   average about 650,000 words and are accepted. After the vendor moves the job to Claude Opus 5.5, which has
   the same window, the largest reports fail with "prompt is too long". The reports have not changed. What is
   the most likely cause?
   - **a**: The newer tokenizer turns identical prose into noticeably more tokens than before
   - **b**: Earlier requests in the same batch are kept and slowly fill the window over time
   - **c**: The output cap is now added into the size check before any request can be accepted
   - **d**: Always-on thinking now fills part of the window before the input is even counted

2. A support chat built on the Messages API passes only the customer's latest question to the model. The
   customer gave an order number two exchanges ago, and the assistant now asks for it again. Which change
   fixes this at the right layer?
   - **a**: Add a system line telling the model to remember each customer who sends a message
   - **b**: Move to the model with the largest context window that is available
   - **c**: Keep the dialogue in the application and replay it with every call
   - **d**: Raise `max_tokens` so the assistant can hold earlier details of the chat

<details>
<summary>Answer key</summary>

1. **a**. Claude Opus 4.7 and later cut text with a newer tokenizer that yields about 30 percent more tokens for the same text, and 1M tokens is roughly 555k words on it against about 750k earlier, so a 650,000-word report fit before and does not now (the tokenizers section). *c* is ruled out because on Claude 4.5 models and newer an input plus an output cap beyond the window is not refused: "the request is still accepted" (the limit section). *b* is ruled out because the API keeps no state between requests, and "your code resends the whole history on every request" (the stateless section). *d* is ruled out because the "prompt is too long" error is the input-only check: "If the input alone exceeds the window" the API refuses it, so output and thinking cannot be the cause (the limit section).
2. **c**. The API remembers nothing, so memory is the history your code sends again with each request (the scripted pair of requests). *a* is ruled out because a model sees only what the request carries, and without the earlier messages "the model has no order number to talk about"; a system line adds no history. *b* is ruled out because each turn's input is "all previous conversation history plus the current user message", so a larger window holds nothing that was never sent. *d* is ruled out because "max_tokens is a cap on output, not on context", so raising it holds no earlier details.

</details>
