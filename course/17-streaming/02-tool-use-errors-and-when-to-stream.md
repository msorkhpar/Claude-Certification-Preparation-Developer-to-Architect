# Streaming tool use, errors in the middle, and when to stream

**Level:** Developer · **Module 17:** Streaming · **Page 2 of 2**
**Exams:** DV1

**After this page you can** act on a streamed tool call at the right moment, handle an error that arrives after the
response began, and decide when streaming is worth its complexity.

Checked against the Claude API documentation (Streaming messages, API errors, the Python and TypeScript SDK pages) on
2026-10-02, and by running the example of the previous page offline in the course container with `anthropic` 1.11.0 and
`@anthropic-ai/sdk` 0.131.0. The error and the tool call
in this page are the ones that example produced.

## Why it matters

Streaming moves failure to a new place. A non-streaming call fails before it returns anything, and the status code says
so. A stream has already said "success" by the time the first event arrives, and everything after that is the client's
job to judge. The exam asks what a client must do when a stream goes wrong, and when the added work is justified.

## The idea

### Tool calls in a stream

A `tool_use` block starts with its `id` and `name` and an empty `input`. The input arrives in `input_json_delta` events as
text fragments, and the fragments are joined and parsed once, after the block stops. The input is incomplete until the
block stops, and the same is true of a block that is cut off by an error or by `max_tokens`. So the rule for an
application that runs tools is short: **do not act on a tool call until its block has stopped and its input has parsed**.

Showing partial input to a user is allowed, if you treat it as text and never as data. Running a tool on a partial input,
such as the first fragment of `{"city": "Par`, is the mistake: the fragment is not an object, and the object that
comes is not final until the block ends. The stop reason `tool_use`, which arrives in `message_delta`, tells you the model
wants the tool results, as in the non-streaming loop.

### An error after the 200

The status line of a streaming response is sent before the model has produced anything. If the service is overloaded
partway, it cannot change that status; it sends an `error` event in the stream, with a body shaped like the usual error
body. That is why an error can arrive after the 200 status line. The example of the previous page shows the SDK turning
that event into an exception.

Three consequences:

- **A status check alone cannot tell you a stream succeeded.** Read until `message_stop`; an `error` event or an early end
  is a failure.
- **Retrying starts from the beginning.** A stream that failed midway cannot be resumed from the last byte, so discard the
  partial output first and retry the whole request. The retry rules of module 15 apply: back off, honour `retry-after`
  where it is present, and cap the attempts.
- **The user may already have read the partial text.** Decide whether to clear it, to mark it, or to keep it and say that
  the answer was cut short.

### When to stream

Streaming changes when the bytes arrive, not what the tokens cost or what the limits allow. Streamed calls count against
the same limits as the others. It pays off in three situations:

1. **A person is waiting for a long answer.** The first words appear at once, and the answer is read as it grows.
2. **The answer is very long.** For large `max_tokens` values, a non-streaming call is silent on the wire until the whole
   answer exists, and the SDKs refuse a non-streaming request that they expect to take longer than about ten minutes.
3. **A proxy or load balancer drops idle connections.** Events keep arriving, so a proxy never sees an idle connection.

When none of these applies, such as a batch job that classifies short texts, the plain call is simpler and has fewer
failure modes. The SDKs can also give you the best of both: Python's `stream.get_final_message()` and TypeScript's
`finalMessage()` stream underneath and return one message, so a long call does not need the event handling in your own
code.

## Traps

1. **Running a tool on a partial input.** Wait for the block to stop and the JSON to parse.
2. **Reading only the status code.** A stream that began with 200 can still end in an error event, or in no `message_stop`.
3. **Streaming by default.** It adds a failure mode and a state machine. Use it for the three cases above, and use the
   final-message helper when you only need the length benefit.

## Quiz

1. A client prints text as it arrives. Midway, an `error` event with `overloaded_error` appears although the HTTP status was
   200. What is the right handling?
   - **a**: Keep the partial text as the final answer and finish the turn without more work
   - **b**: Treat the call as failed, drop the partial output and send the whole request again
   - **c**: Ignore the event, since the status line already reported that the call went well
   - **d**: Resume the stream from the last received byte once the service has recovered

2. A team streams a long report only because they believe it lowers the bill. According to the page, what is the better
   reason to stream?
   - **a**: Fewer errors, since a started call cannot fail after its first byte
   - **b**: A lower price for each of the output tokens
   - **c**: An exemption from the limits that apply to other calls
   - **d**: Readers see words while the rest is still being written

<details>
<summary>Answer key</summary>

1. **b**. The page says "discard the partial output first and retry the whole request". *a* is ruled out because the partial text is the loss: "the user may already have read the partial text" and the answer is cut short, so it is not final. *c* is ruled out because "A status check alone cannot tell you a stream succeeded". *d* is ruled out because a stream that failed midway "cannot be resumed from the last byte".
2. **d**. The page's first case is "A person is waiting for a long answer", and "The first words appear at once". *b* is ruled out because "Streaming changes when the bytes arrive, not what the tokens cost". *c* is ruled out because "Streamed calls count against the same limits as the others". *a* is ruled out because "That is why an error can arrive after the 200 status line".

</details>

## Module quiz

This quiz covers both pages of the module.

1. A support app shows a streamed reply that includes a tool call. When may it run the tool?
   - **a**: After the block holding it has stopped and its joined fragments have parsed
   - **b**: As soon as the tool name appears in the opening event of the block
   - **c**: Whenever the first fragment turns out to be a complete object
   - **d**: Before the text, because tool blocks are always listed in front

2. Which pair of behaviours does a correct assembler show for an unrecognised event type and for a stream that ends early?
   - **a**: It skips both and returns the content it holds
   - **b**: It raises on the first and returns what the second gave
   - **c**: It skips the first and raises on the second
   - **d**: It raises on both so that nothing is guessed

3. A team adds streaming to a feature. Which of the problems below does it leave unchanged?
   - **a**: A proxy that drops quiet connections during long generations
   - **b**: The share of the rate limit that each reply uses
   - **c**: A visitor who sees no text during the first seconds
   - **d**: A very long answer that a plain call would refuse to start

4. A stream holds a thinking block, a text block and a tool block. In which order does an assembler list them?
   - **a**: By how many fragments each one received
   - **b**: By the moment at which each one finished
   - **c**: With thinking first, text next and tools last
   - **d**: By the index that the events give each one

<details>
<summary>Answer key</summary>

1. **a**. The page says "do not act on a tool call until its block has stopped and its input has parsed". *b* is ruled out because "The input is incomplete until the block stops". *c* is ruled out because a fragment is not an object: "the fragment is not an object, and the object that comes is not final until the block ends". *d* is ruled out because "content lists the blocks in index order", not by kind.
2. **c**. The page says to "ignore event types you do not know" and that a stream without `message_stop` "is an incomplete message, not a short one". *b* is ruled out because the first is skipped "because the API may add new ones". *a* is ruled out because a cut stream "is an incomplete message, not a short one". *d* is ruled out because the first is skipped: "ignore event types you do not know".
3. **b**. The page says "Streamed calls count against the same limits as the others", so the share of the limit is unchanged. *a* is ruled out because "Events keep arriving, so a proxy never sees an idle connection". *c* is ruled out because "The first words appear at once". *d* is ruled out because "the SDKs refuse a non-streaming request that they expect to take longer than about ten minutes", which streaming avoids.
4. **d**. The page says "content lists the blocks in index order". *b* is ruled out because the order is "not in the order they stopped". *c* is ruled out because "Blocks are never sorted by type". *a* is ruled out because blocks are "never sorted by type or by size", and a fragment count is a size.

</details>
