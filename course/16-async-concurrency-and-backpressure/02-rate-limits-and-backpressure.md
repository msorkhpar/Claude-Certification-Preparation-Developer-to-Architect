# Rate limits as a client concern, backpressure, and a worker pool

**Level:** Developer · **Module 16:** Async, concurrency and backpressure · **Page 2 of 2**
**Exams:** DV1

**After this page you can** size a client-side bound from the documented rate limits, read the rate-limit headers, say
what backpressure is and where it is applied, and write a bounded worker pool that pulls its input lazily.

Checked against the Claude API documentation (rate limits, API errors, batch processing, Streaming messages) on
2026-10-02, and by running the practice offline in the course container (Python, TypeScript, Java and Kotlin; the SDK
versions are `anthropic` 1.11.0 and `@anthropic-ai/sdk` 0.131.0 for the example of page 1). Limits
are the documentation's on that date and are re-checked at release.

## Why it matters

A rate limit is a contract between your client and the service, and the service enforces only its half. If the client
does not hold up its half, every burst comes back as a 429, every 429 as a retry, and every retry as more load. The exam
asks where the limit is handled and how a pipeline behaves when one stage is slower than the next. Both answers are
about who waits: the client that sends too fast, or the stage that cannot keep up.

## The idea

### What the limits are

The documentation measures Messages API limits in three units, per organisation and per model class: **requests per
minute (RPM)**, **input tokens per minute (ITPM)** and **output tokens per minute (OTPM)**. Exceeding any of them gives a
429 that "describes which rate limit was exceeded, along with a `retry-after` header". A few details change how you
plan:

- **Token bucket.** The limit is enforced as a bucket that refills continuously, not a counter that resets on the
  minute. A burst can empty it even when the minute's total is low, and a rate of 60 requests a minute "might be
  enforced as 1 request per second".
- **Only uncached input counts toward ITPM** for most models: `input_tokens` and `cache_creation_input_tokens` count,
  `cache_read_input_tokens` does not. Prompt caching is a throughput tool.
- **OTPM counts what was generated.** The `max_tokens` setting "does not factor into OTPM rate limit calculations".
- **Acceleration limits.** A sharp rise in your usage can produce 429 errors even under the steady limits, so "ramp up your
  traffic gradually".
- **One pool across geographies.** Rate limits are shared across all `inference_geo` values.
- **Workspaces.** You can set lower limits per workspace to protect one team's traffic from another's; organisation limits
  always apply.

### The headers that tell you where you are

Every response carries the state of the bucket, which a client can read to slow down before the first 429:

| Header | Meaning |
|---|---|
| `anthropic-ratelimit-requests-remaining` | Requests left before you are limited |
| `anthropic-ratelimit-input-tokens-remaining`, `anthropic-ratelimit-output-tokens-remaining` | The same for input and output tokens, rounded to the nearest thousand |
| `anthropic-ratelimit-requests-reset` and its token twins | When the bucket will be full again, in RFC 3339 format |
| `retry-after` | On a 429: the seconds to wait; "earlier retries will fail" |

The `anthropic-ratelimit-tokens-*` family reports "the most restrictive limit currently in effect", which is the one to
watch. Use `with_raw_response` (Python), `.withResponse()` (TypeScript) or `withRawResponse()` (Java) to read them from an
SDK (module 13).

### Three client-side controls, cheapest first

1. **Bound the calls in flight** (page 1). It needs no knowledge of the limits and removes the worst bursts.
2. **Pace the calls** with a client-side token bucket sized from the limit you hold, so that a steady stream stays under it,
   and read the remaining-requests header to adapt.
3. **Retry with back-off on a 429**, honouring `retry-after` (module 15). This is the last line, not the first.

When the work does not need an answer now, the right answer to a limit is a different API: the **Message Batches API**
is "a powerful, cost-effective way to asynchronously process large volumes of Messages requests", with its own queue
limits separate from the interactive ones.

### Backpressure: the slow stage holds back the fast one

Backpressure is what makes a pipeline stable. When a stage cannot keep up, it must slow the stage in front of it instead of
letting work pile up. Without it, a fast producer and a slow consumer meet in an unbounded buffer, and the buffer is your
memory.

Three places it appears in a Claude application:

- **Reading input.** A job that reads a million lines from a file should take a line only when a worker is free. Reading
  them all first puts the file in memory and starts every call at once.
- **Between stages.** If the model stage is faster than the database stage behind it, bound the queue between them: a full
  queue makes the model stage wait.
- **Consuming a stream.** The reply arrives as events (module 17). If your handler is slower than the stream, either something
  buffers the events or the connection stalls, and you should decide which on purpose: process each event as it arrives, and
  keep nothing you do not need.

The mechanism is the same in all three: **take the next item only when there is room for it**. A worker pool does
exactly that. Each worker loops: take the next item from the source, run the work, store the result, repeat. There are at
most `limit` workers, so at most `limit` items are in flight and at most `limit` items have been taken from the
source.

## The practice: a bounded worker pool

You write `map_bounded(items, work, limit)`: a function that runs work over an input with at most `limit` calls in flight,
returns one outcome per item in input order, lets one failure stand alone, and takes the input one item at a time. It
is asynchronous in Python and TypeScript, and thread-based in Java and Kotlin. The tests use probe functions that count
what is in flight and a generator that counts what has been pulled. The statement, with the exact contract, is in
`exercises/16-async-concurrency-and-backpressure/unit-01/practice-1/statement.md`; each language folder has a `starter`,
the `tests` and a `run.sh` or build file. The starter fails every test.

| Id | What it checks |
|---|---|
| `m1` | Every item is processed, and never more than `limit` run at once |
| `e1` | Results keep the input order even when later items finish first |
| `e2` | A failing item is reported and the others still finish |
| `e3` | A limit above the item count and an empty input both work |
| `e4` | A limit below one is refused |
| `e5` | Items are pulled lazily: while all workers are blocked, only `limit` items have been taken |

Case `e5` is the backpressure case. A solution that copies the input into a list first passes every other test and
fails this one, and in production it is the version that runs out of memory on the large file.

## Traps

1. **Sizing the bound by feel.** Use the limit and your measured latency: requests in flight equal the rate times the call
   duration.
2. **Reading all the input before starting.** The producer then never waits for the consumer. Take items lazily.
3. **Treating a 429 as the plan.** Retrying a burst you could have avoided spends the time and the quota twice. Bound and pace
   first; retry last.

## Quiz

1. A team's client sends bursts of requests at the start of every minute and gets 429 errors, although the daily total is
   far below the limit. Why does the page say this happens?
   - **a**: Capacity is restored at the start of each minute, so late requests are refused
   - **b**: Capacity refills steadily, so a sudden spike drains the allowance
   - **c**: The provider counts the `max_tokens` setting of each request toward output tokens
   - **d**: Capacity is shared with other organisations, so peaks collide with theirs

2. A job reads a very large file and starts one call per row before it processes any result, and the
   process runs out of memory. Which design is best?
   - **a**: Add retries so that the rows that failed to start are begun again later
   - **b**: Read all the rows first but start the calls in groups of a hundred rows
   - **c**: Compress the rows in memory so that more of them fit before the calls start
   - **d**: Pull the next item only when a worker is free, under a fixed bound on parallel work

<details>
<summary>Answer key</summary>

1. **b**. The page says the limit is "a bucket that refills continuously, not a counter that resets on the minute", so a spike drains it. *a* is ruled out because the bucket "refills continuously, not a counter that resets on the minute". *c* is ruled out because the `max_tokens` setting "does not factor into OTPM rate limit calculations". *d* is ruled out because the limits are set per organisation: "per organisation and per model class".
2. **d**. The page's mechanism is "take the next item only when there is room for it", with a worker pool that has "at most limit workers". *b* is ruled out because "reading them all first puts the file in memory", and groups do not change that. *c* is ruled out because compression keeps every row in memory and leaves "starts every call at once" in place. *a* is ruled out because "retrying a burst you could have avoided spends the time and the quota twice".

</details>

## Module quiz

This quiz covers both pages of the module.

1. Twelve tickets are sent together, and the provider refuses one with a 429. The team wants fewer refusals. What comes first?
   - **a**: Cap the parallel work with a semaphore, then pace what remains
   - **b**: Raise the retry count so that each refusal is repeated sooner than before
   - **c**: Send the work through more clients so that each one carries fewer calls
   - **d**: Raise the timeout on every request so that slow calls are not abandoned

2. A pipeline's first stage outpaces the stage behind it, and memory grows during long runs. Which fix is best?
   - **a**: Give the process more memory so that the buffer can hold the whole run
   - **b**: Add a second producer so that the buffer fills twice as fast as before
   - **c**: Limit the buffer between the two so that a full one slows the producer
   - **d**: Retry the slow writes so that fewer of them fail during the long run

3. Each call takes two seconds, and the account may send ten requests a second. About how many calls run at once at that rate?
   - **a**: Five, found by dividing the latency by the throughput of the account
   - **b**: Twenty, found by multiplying throughput by latency
   - **c**: Ten, the same as the sends that the account is allowed each second
   - **d**: Two, the duration of a single call measured in whole seconds

4. One ticket in a gathered batch fails with a bad request while the others succeed. What does the page's rule produce?
   - **a**: Only the successes, listed in the order in which they finished running
   - **b**: Cancelled siblings and the first error raised to the caller of the batch
   - **c**: A repeat of the failing item that continues until it finally succeeds
   - **d**: An error record in its slot, with every other answer kept in order

<details>
<summary>Answer key</summary>

1. **a**. The page's order is "Bound and pace first; retry last". *b* is ruled out because "retrying a burst you could have avoided spends the time and the quota twice". *c* is ruled out because the page says "Don't create more than one client in the same application", since each has its own pools. *d* is ruled out because the bound is what "protects the connection pool and the rate limit", and a timeout does not bound the burst.
2. **c**. The page says "bound the queue between them: a full queue makes the model stage wait". *b* is ruled out because "a fast producer and a slow consumer meet in an unbounded buffer". *a* is ruled out because "the buffer is your memory". *d* is ruled out because retries only repeat load, and "spends the time and the quota twice".
3. **b**. The page's relation is "requests in flight equal the rate times the call duration", so ten times two gives twenty. *a* is ruled out because "requests in flight equal the rate times" the duration, with no division. *c* is ruled out because the page says "Use the limit and your measured latency". *d* is ruled out because "equal the rate times the call duration" keeps the rate in the product.
4. **d**. The pool "returns one outcome per item in input order" and "lets one failure stand alone". *b* is ruled out because that is what a bare gather does, and "rejects on the first rejection" loses the rest. *c* is ruled out because the pool "lets one failure stand alone" and does not loop on it. *a* is ruled out because the pool "returns one outcome per item in input order".

</details>
