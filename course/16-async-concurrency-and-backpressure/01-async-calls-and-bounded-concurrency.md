# Async calls and bounded concurrency

**Level:** Developer · **Module 16:** Async, concurrency and backpressure · **Page 1 of 2**
**Exams:** DV1

**After this page you can** run many API calls concurrently with the async clients, bound how many are in flight, keep
one failure from hiding the other results, and say what blocks an event loop.

Checked against the Claude API documentation (the Python, TypeScript and Java SDK pages, rate limits) on 2026-10-02,
and by running the example offline in the course container with `anthropic` 1.11.0 and `@anthropic-ai/sdk` 0.131.0. The
model id is `claude-sonnet-5-5`; replies are hand-scripted and labelled illustrative.

## Why it matters

A model call takes seconds, and almost all of that time is waiting. Classifying a thousand tickets one after another at
three seconds each takes fifty minutes; classifying them with sixteen calls in flight takes about three. The same
engineer who learns that lesson and writes `gather` over all thousand at once meets the opposite lesson: a wall of 429
errors, a connection pool that falls over, and a bill for the calls that were retried. The exam asks how an application
makes many calls well, and the answer has two halves: concurrency, and a bound on it.

## The idea

### Waiting is the cost, so overlap it

Calls to a remote model are I/O-bound: your program sends a request, then waits for the reply while the CPU is idle.
Concurrency means starting the next request during that wait. It is not parallelism (several CPUs working at once);
one thread with an event loop overlaps thousands of waits. Each SDK offers it in the idiom of its language:

| Language | The concurrent client | How calls overlap |
|---|---|---|
| Python | `AsyncAnthropic`, with `await client.messages.create(...)` | `asyncio` tasks; `asyncio.gather` and `asyncio.Semaphore` |
| TypeScript | `Anthropic`, whose methods return promises | `Promise.all` or `Promise.allSettled`; a small pool of workers |
| Java | `client.async()` or `AnthropicOkHttpClientAsync`, methods return `CompletableFuture` | futures composed with `thenCombine`, `allOf`; or threads over the blocking client |
| Kotlin | The same Java SDK | Threads or coroutines over the Java client; the practice uses threads |

The Python SDK page notes an optional faster HTTP backend for the async client, `aiohttp`, installed with the
`anthropic[aiohttp]` extra and selected with `DefaultAioHttpClient`; the default is `httpx2`. The Java SDK page adds
a rule that matters here: "Don't create more than one client in the same application", because each client has a
connection pool and thread pools that are better shared between requests.

### What blocks an event loop

An async program has one thread running a loop. A call that waits **without yielding** stops the loop, and every other
task waits with it. The classic mistake is an async web handler that calls the **synchronous** client: while that call
waits, the server answers nobody else. In Python, use `AsyncAnthropic` inside `async def`; in TypeScript, a promise
that you `await` yields, while a busy `for` loop over a large array does not.

### One failure must not hide the rest

When you start many calls together, some will fail. How you collect the results decides what you learn:

- Python `asyncio.gather(*tasks)` raises the first exception it sees, and the results of the others are not returned to
  you. With `return_exceptions=True` it returns a list in input order in which a failure is an exception object in its
  slot.
- TypeScript `Promise.all` rejects on the first rejection. `Promise.allSettled` waits for every promise and gives
  each a `fulfilled` or `rejected` record, in order.
- Java `CompletableFuture.allOf` completes when all are done and leaves each future to be read for its value or failure.

The rule for batches of independent work is the same everywhere: **collect every outcome, in input order, and decide
about the failures afterwards**. The practice on the next page asks for exactly that.

### The example: twelve calls, bounded or not

The program classifies twelve tickets with the async client. The transport is scripted: each request takes 50 ms
inside it, the reply is a label built from the ticket in the request, and ticket 7 is answered with a 429. It runs the
twelve twice, once unbounded and once behind a semaphore (a pool of workers in TypeScript) of four.

<!-- example: m16-bounded-concurrency tabs: python,typescript -->
```python
"""Twelve classification calls with the async SDK: unbounded, then bounded by a semaphore.

The transport is scripted: every request takes 50 ms inside it, the reply is a label built from
the ticket in the request, and ticket 7 is answered with a 429. The labels are illustrative.
"""
import asyncio


from harness import scripted_async_client
from harness.scripted import message, text

MODEL = "claude-sonnet-5-5"
TICKETS = [f"ticket {n}" for n in range(1, 13)]


def responder(body):
    ticket = body["messages"][0]["content"]
    if ticket == "ticket 7":
        return (429, {"type": "error", "error": {"type": "rate_limit_error", "message": "slow down"}})
    return message([text("label for " + ticket)])


async def classify(client, ticket):
    reply = await client.messages.create(model=MODEL, max_tokens=16, messages=[{"role": "user", "content": ticket}])
    return reply.content[0].text


async def run_all(limit):
    client, transport = scripted_async_client(*[responder] * len(TICKETS), delay=0.05)
    gate = asyncio.Semaphore(limit) if limit else None

    async def one(ticket):
        if gate is None:
            return await classify(client, ticket)
        async with gate:
            return await classify(client, ticket)

    results = await asyncio.gather(*(one(t) for t in TICKETS), return_exceptions=True)
    return results, transport.max_in_flight


async def main():
    for label, limit in (("unbounded", None), ("bounded by 4", 4)):
        results, peak = await run_all(limit)
        failed = [i + 1 for i, r in enumerate(results) if isinstance(r, Exception)]
        print(f"{label}: peak in flight {peak}, {len(results) - len(failed)} answered, failed tickets {failed}")
    print("results keep input order:", results[0], "|", results[5], "|", type(results[6]).__name__, "|", results[7])


if __name__ == "__main__":
    asyncio.run(main())
```
```text
unbounded: peak in flight 12, 11 answered, failed tickets [7]
bounded by 4: peak in flight 4, 11 answered, failed tickets [7]
results keep input order: label for ticket 1 | label for ticket 6 | RateLimitError | label for ticket 8
```
```typescript
// Twelve classification calls with the SDK: unbounded, then bounded by a small worker pool.
// The fetch is scripted: every request takes 50 ms inside it, the reply is a label built from the
// ticket in the request, and ticket 7 is answered with a 429. The labels are illustrative.
import Anthropic from "@anthropic-ai/sdk";
import { message, scriptedFetch, text, type Reply } from "../../../harness/ts/scriptedFetch.ts";

const MODEL = "claude-sonnet-5-5";
export const TICKETS = Array.from({ length: 12 }, (_, i) => `ticket ${i + 1}`);

function responder(body: any): Reply {
  const ticket = body.messages[0].content as string;
  if (ticket === "ticket 7") return { status: 429, body: { type: "error", error: { type: "rate_limit_error", message: "slow down" } } };
  return { body: message([text("label for " + ticket)]) };
}

async function classify(client: Anthropic, ticket: string): Promise<string> {
  const reply = await client.messages.create({ model: MODEL, max_tokens: 16, messages: [{ role: "user", content: ticket }] });
  return (reply.content[0] as any).text;
}

// A pool of `limit` workers that pull the next ticket when they are free.
export async function runAll(limit: number | null) {
  const fake = scriptedFetch(TICKETS.map(() => responder), { delayMs: 50 });
  const client = new Anthropic({ apiKey: "placeholder", maxRetries: 0, fetch: fake.fetch });
  const results: PromiseSettledResult<string>[] = new Array(TICKETS.length);
  let next = 0;
  const worker = async () => {
    while (next < TICKETS.length) {
      const i = next++;
      results[i] = await classify(client, TICKETS[i]).then(
        (value) => ({ status: "fulfilled" as const, value }),
        (reason) => ({ status: "rejected" as const, reason }),
      );
    }
  };
  await Promise.all(Array.from({ length: limit ?? TICKETS.length }, worker));
  return { results, peak: fake.state.maxInFlight };
}

async function main() {
  let last: PromiseSettledResult<string>[] = [];
  for (const [label, limit] of [["unbounded", null], ["bounded by 4", 4]] as const) {
    const { results, peak } = await runAll(limit);
    last = results;
    const failed = results.flatMap((r, i) => (r.status === "rejected" ? [i + 1] : []));
    console.log(`${label}: peak in flight ${peak}, ${results.length - failed.length} answered, failed tickets [${failed.join(", ")}]`);
  }
  const v = (r: PromiseSettledResult<string>) => (r.status === "fulfilled" ? r.value : (r.reason as Error).constructor.name);
  console.log("results keep input order:", v(last[0]), "|", v(last[5]), "|", v(last[6]), "|", v(last[7]));
}

if (import.meta.main) await main();
```
```text
unbounded: peak in flight 12, 11 answered, failed tickets [7]
bounded by 4: peak in flight 4, 11 answered, failed tickets [7]
results keep input order: label for ticket 1 | label for ticket 6 | RateLimitError | label for ticket 8
```
<!-- /example -->

Read the output for three facts:

1. **Unbounded, all twelve were in flight at once.** The transport's own counter says so. Against the real API,
   that is twelve requests in the same instant, and at a thousand tickets it is a thousand.
2. **Bounded by four, the peak was four.** The same twelve calls finished with the same answers, in a few more
   batches of 50 ms. The bound costs a little latency and protects the connection pool and the rate limit.
3. **A failure kept its place.** Ticket 7 came back as a `RateLimitError`, the eleven others as labels, and the results
   stayed in the order of the tickets. Nobody lost an answer because of a neighbour's failure.

Java and Kotlin have no example tab on this page. The Java SDK's `client.async()` gives `CompletableFuture<Message>`
for the same pattern, and the practice on the next page runs in all four languages with worker threads.

### Choosing the bound

Start from the rate limit, not from guesswork. The limits of module 12 are per minute; a bound on in-flight calls
is a per-moment number. **Little's law** joins them: the number of requests in flight equals the request rate times the
time each takes. To send 1,000 requests a minute (about 16.7 a second) when each call takes 6 seconds, you need about 100 in
flight. A bound of 20 would cap you at about 200 a minute; a bound of 400 would only invite 429 errors. Measure your
real latency, compute the bound from the limit you must stay under, and leave headroom for retries.

## Traps

1. **Unbounded fan-out.** `gather` over an entire queue turns a batch into a burst. Bound the calls in flight.
2. **A synchronous call inside async code.** One blocking call stalls every task on the loop. Use the async client.
3. **Losing results to the first error.** `Promise.all` and a bare `gather` give up on the others when one fails. For
   independent work, collect every outcome and judge afterwards.

## Quiz

1. A web service handles each request in an async function that calls the synchronous SDK client, and under load every
   endpoint, including those that never ask the model anything, becomes slow together. What is the cause?
   - **a**: A blocking wait holds the event loop, so other tasks cannot run until it returns
   - **b**: The model slows down for every caller whenever one caller is waiting for a reply from it
   - **c**: The client retries too many times, and those retries delay the unrelated endpoints as well
   - **d**: The async function needs a larger connection pool for each endpoint that it serves

2. A nightly job starts one call for each of 8,000 tickets at once with a bare gather. It sees a wave of 429 errors and
   loses the labels of the tickets that succeeded because the gather raised. Which change is best?
   - **a**: Split the tickets into two gathers of 4,000 and run them in turn, one after the other
   - **b**: Rerun the whole gather until it comes back without raising an exception at all
   - **c**: Bound the calls in flight and collect every outcome in input order
   - **d**: Lower the timeout on each request so that the failing calls give up much sooner

<details>
<summary>Answer key</summary>

1. **a**. The page says a call that waits "without yielding stops the loop, and every other task waits with it". *b* is ruled out because "one thread with an event loop overlaps thousands of waits", so callers do not hold each other up at the model. *c* is ruled out because the page names the cause as "an async web handler that calls the synchronous client", not the retry count. *d* is ruled out because a bigger pool does not make a waiting call yield: "a call that waits without yielding stops the loop".
2. **c**. The page asks to "bound the calls in flight" and to "collect every outcome, in input order, and decide about the failures afterwards". *b* is ruled out because a bare gather over a queue "turns a batch into a burst", and a rerun repeats the burst. *a* is ruled out because two gathers of 4,000 are still two bursts, and in the page's words "Promise.all and a bare gather give up on the others when one fails", so each half repeats the loss. *d* is ruled out because "the bound costs a little latency and protects the connection pool and the rate limit", and a shorter timeout does neither.

</details>
