# Submitting a batch: shape, limits and price

**Level:** Developer · **Module 21:** Message Batches · **Page 1 of 2**
**Exams:** DV2

**After this page you can** decide whether a workload belongs in a batch, build a batch request with safe `custom_id` values, keep
a job inside the batch limits, name the parameters a batch refuses, and work out what the 50 percent discount saves.

Checked against the Claude API documentation (Batch processing, Pricing) on 2026-10-02, and by running the practice offline in the
course container in Python, TypeScript, Java and Kotlin.

## Why it matters

A batch gives up speed for price. Work that nobody is waiting for, such as evaluations, bulk classification and overnight
summaries, costs half as much through the Message Batches API. The price is a delay of up to a day, a stricter request shape and
a result file you must read carefully. The exam asks which workloads fit, which parameters are refused and what the limits are.

## The idea

### What a batch is

You send many Messages requests in one call. The system "creates a new Message Batch with the provided Messages requests", processes
them asynchronously, "with each request handled independently", and lets you poll the batch and read the results when processing
has ended. Nothing is streamed: the answers come back as a single file when the work is done.

Each element of the batch has two parts: a `custom_id` you choose, and `params`, which is an ordinary Messages request body. The
`custom_id` "Must be 1 to 64 characters and contain only alphanumeric characters, hyphens, and underscores", and it must be
unique in the batch. Because the results come back in any order (page 2), the `custom_id` is the only way to tie an answer to the
question that produced it. Choose meaningful ones, such as a database key.

The page recommends a dry run: "Dry run a single request shape with the Messages API to avoid validation errors." A shape that
works synchronously will work in a batch, with the exceptions below.

### What a batch can carry

"Almost any request you can make to the Messages API can be included in a batch": vision, tool use including server tools, system
messages, multi-turn conversations, extended thinking and most beta features. Because each request is independent, one batch can
mix kinds of request. Three parameters are refused with a validation error:

| Parameter | Why the page gives |
|---|---|
| `stream: true` | "Batch results come back as a single file, not a stream." |
| `speed` | "Fast mode tunes synchronous latency, which doesn't apply to asynchronous batch processing." |
| `max_tokens: 0` | pre-warming a cache is not supported, since an entry written in a batch would likely expire before the follow-up |

Each batched request needs a `max_tokens` of at least 1. This is why module 19 and module 20 both end their lists of options with a
batch exception.

### The limits

The page lists them in one place:

> A Message Batch is limited to either 100,000 Message requests or 256 MB in size, whichever is reached first.

Source: Batch processing.

Three more facts complete the picture. A batch expires if it is not finished in 24 hours: "Batches expire if processing does not
complete within 24 hours." Most batches finish in under an hour, but there is no promise. Results are kept for a fixed time:
"Batch results are available for 29 days after creation." The clock starts at `created_at`, not when processing ended. And a batch
cannot be edited after submission: "once a batch has been submitted, it cannot be modified." To change it, cancel and resubmit.

A job larger than one batch is cut into several. A batch above 256 MB returns a 413 `request_too_large` error. The practice at
the end of the next page does the cutting.

### The price

"All usage is charged at 50% of the standard API prices." The discount covers input, output and special tokens. The page's table
gives the batch price for each model; for the course models, per million tokens:

| Model | Batch input | Batch output |
|---|---|---|
| Claude Fable 5.1 | $5 | $25 |
| Claude Opus 5.5 | $2 | $10 |
| Claude Sonnet 5.5 | $1 | $5 |
| Claude Haiku 4.5 | $0.50 | $2.50 |

A worked case: 10,000 requests of 2,000 input tokens and 500 output tokens on Claude Sonnet 5.5. That is 20 million input tokens
and 5 million output tokens. In a batch they cost 20 x $1 + 5 x $5 = $45. At the standard $2 and $10 they cost $90. The saving is
$45, and it is the same ratio at any size.

The discount stacks with caching: "The pricing discounts from prompt caching and Message Batches can stack". The stacking comes
with a caveat: "cache hits are provided on a best-effort basis", with typical hit rates "ranging from 30% to 98%". The page's three
steps to raise them are the ones from module 20: identical `cache_control` blocks in every request, a steady stream of requests so
entries do not lapse, and as much shared content as possible. Since a batch can run longer than five minutes, it suggests the
one-hour lifetime for shared context.

One caution on spend: "batches may go slightly over your Workspace's configured spend limit". The limit is not a hard stop for a
batch.

### Choosing a batch

The page's own list of when a batch fits starts with "Immediate responses are not required". In practice:

| Fits a batch | Does not fit |
|---|---|
| A nightly evaluation over thousands of cases | A chat reply someone is waiting for |
| Classifying a backlog of tickets | A step inside a user-facing agent loop |
| Summaries generated for a catalogue | Work needed within the hour |
| Anything where hours of delay cost nothing | A job needing `stream`, fast mode or cache pre-warming |

## Traps

1. **Assuming results come back in input order.** They do not. Match on `custom_id`, and never on position.
2. **Sending `stream`, `speed` or `max_tokens: 0` in a batch request.** Each is a validation error. A request body copied from a
   live path may carry one of them.
3. **Planning on the full 29 days from the end of processing.** The 29 days count from creation, so a batch that ran for a day
   leaves 28 for reading.

## Quiz

1. A team has 40,000 classification requests, none needed before tomorrow, on Claude Sonnet 5.5. What does the page support?
   - **a**: Send them as a batch with `stream: true`, so results arrive as they finish
   - **b**: Send them as a batch at the standard price, since a batch only helps throughput
   - **c**: Send them one by one, since a batch cannot take more than a few hundred
   - **d**: Send them as a batch, at 50% of the standard price, and read the results later

2. A batch request body copied from a live endpoint contains `speed` set to fast. What happens at submission?
   - **a**: A validation error comes back, since that parameter is unsupported here
   - **b**: The batch runs at the fast rate, since the parameter is passed through unchanged
   - **c**: The parameter is dropped silently, and the batch runs at the standard rate
   - **d**: The batch is accepted, but the fast rate is billed in place of the discount

3. A job has 250,000 small requests. How must it be submitted?
   - **a**: In two batches, since each holds up to 128,000 requests
   - **b**: In one batch, since the size limit is counted in bytes and not in requests
   - **c**: In at least three parts, since one batch holds 100,000 items at most
   - **d**: In one batch, and the system splits it into parts by itself

<details>
<summary>Answer key</summary>

1. **d**. The page says "All usage is charged at 50% of the standard API prices" and that the batch suits work where "Immediate responses are not required". *b* is ruled out because the discount covers "input, output and special tokens" at half price. *c* is ruled out because "A Message Batch is limited to either 100,000 Message requests or 256 MB in size", which is far above a few hundred. *a* is ruled out because "Batch results come back as a single file, not a stream."
2. **a**. The page says these parameters are "refused with a validation error". *b* is ruled out because "Fast mode tunes synchronous latency, which doesn't apply to asynchronous batch processing." *c* is ruled out because the page says they are "refused with a validation error", not dropped. *d* is ruled out because "All usage is charged at 50% of the standard API prices", with no fast rate in a batch.
3. **c**. The page says "A Message Batch is limited to either 100,000 Message requests or 256 MB in size, whichever is reached first", so 250,000 needs three. *b* is ruled out because "whichever is reached first" means the request count limit applies as well as the size. *a* is ruled out because the stated limit is "either 100,000 Message requests or 256 MB", not 128,000. *d* is ruled out because a batch cannot be edited: "once a batch has been submitted, it cannot be modified."

</details>
