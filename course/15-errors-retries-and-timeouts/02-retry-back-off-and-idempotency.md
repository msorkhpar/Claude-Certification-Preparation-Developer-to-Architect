# Retry, back-off and idempotency, and a retry policy

**Level:** Developer · **Module 15:** Errors, retries and timeouts · **Page 2 of 3**
**Exams:** DV1, DV8

**After this page you can** decide which failures to retry, compute a back-off with a cap and jitter that honours
`retry-after`, explain what the SDK retries on its own, and write the policy against scripted failures.

Checked against the Claude API documentation (API errors, rate limits, and the Python, TypeScript and Java SDK pages)
on 2026-10-02, and by running the example and the practice offline in the course container with `anthropic` 1.11.0,
`@anthropic-ai/sdk` 0.131.0, Java 25 and Kotlin 2.4. Failures are scripted and labelled illustrative.

## Why it matters

Retrying is the first reliability feature everyone adds and the one most often added wrongly. Too eager, and a brief
overload becomes a retry storm that keeps the service down; too timid, and a one-second blip fails a customer's
request; indiscriminate, and a request that can never succeed is sent four times. The exam asks which failures to
retry, how to space the attempts, and what makes a retry safe. The answers are short, and each one is a line of
the practice at the end of this page.

## The idea

### What to retry

The rule of page 1, restated as a decision:

| Retry | Do not retry |
|---|---|
| A lost or timed-out connection (no status) | 400, 401, 402, 403, 404, 413: the same request fails again |
| 408, 409 (once the conflict is resolved) | A 429 that is a spend cap: no `retry-after`, lasts until the next month |
| 429 that is a rate limit, after `retry-after` | Anything where a person has to act |
| 500, 504 and 529 | |

The SDKs make the same cut, with one exception that matters. Their documentation says: "Connection errors (for example,
because of a network connectivity problem), 408 Request Timeout, 409 Conflict, 429 Rate Limit, and >=500 Internal errors
are all retried by default", two times, "with a short exponential backoff". That list is by status, so a spend-cap 429
is retried too, and fails each time.

### Back-off: exponential, capped, jittered

Waiting a fixed second between attempts looks reasonable and behaves badly: after an outage, every client retries on
the same schedule, and the recovering service meets all of them at once. Three ideas fix it:

1. **Exponential growth.** Wait `base`, then `2 * base`, then `4 * base`: the delay doubles each time, so a long
   outage is met with fewer and fewer requests.
2. **A cap.** Without one, the delay of the tenth attempt is minutes long. Take the smaller of the doubled delay and a
   cap, so a user is never made to wait longer than the product allows.
3. **Jitter.** Randomise each delay, for example by choosing any time between zero and the computed value. Clients
   that failed together then retry apart, which removes the synchronised spikes.

With a base of 0.5 seconds and a cap of 8, the waits before attempts two, three, four, five and six are 0.5, 1, 2, 4 and
8 seconds, before jitter. The practice takes the jitter as a function you pass in, which is how a test can check
the arithmetic with no randomness.

### retry-after: the server's own number

A 429 for a rate limit carries a `retry-after` header: the rate-limit page says it is "the number of seconds to wait until
you can retry the request. Earlier retries will fail." So it is a floor, not a suggestion: wait the larger of your
back-off and the header. The SDKs honour it when it is present.

Behind it sits the token bucket from module 12: capacity "continuously replenished", so waiting the stated time is
enough, and hammering the endpoint in the meantime only fails.

### Budgets: attempts and time

A retry policy has a budget, and it is part of the latency requirement of module 12. Four attempts with the
delays above spend about 3.5 seconds on waiting alone, before counting how long each failed attempt took. Set two
numbers from the feature's budget: **how many attempts**, and **how long in total**. A user-facing call may allow one retry
and a deadline of two seconds; a nightly job may allow six attempts and a minute. When the budget is spent, fail with
the information a person needs: the last status, the type, the number of attempts and the request id.

### Idempotency: when is a retry safe

A retry is safe when doing the operation twice has the same effect as doing it once. Applied to a Claude call:

- **The model call itself.** A retry of a failed message request produces a new reply. If the first attempt actually
  succeeded on the server and only the answer was lost (a timeout after processing), you pay for two generations and
  may get two different replies. For a pure question-and-answer call that is acceptable; for anything that acts on the
  reply it is not.
- **The actions your code takes.** A tool that charges a card, sends an email or files a ticket must not run twice
  because a request was retried. Make such tools idempotent: pass your own unique key, check it before acting, and store
  the result under it. The Messages API documentation checked for this page does not describe an idempotency key for
  creating a message, so the key is your design, not a parameter to look up.
- **Batches.** In the Message Batches API each request carries a `custom_id`, and results are matched to requests by it,
  not by order. That is the built-in way to avoid double work when a batch is resubmitted.

A prompt is a request to the model, not a guarantee, so it cannot keep an action from running twice. The rule of
thumb: **retry the call, never the side effect**.

### What the SDK gives you, shown

The example runs the same call through the real SDK in five situations. Read it against the table above.

<!-- example: m15-sdk-retries tabs: python,typescript -->
```python
"""What the SDK retries on its own, and what it does not, against a scripted transport.

The failures are illustrative, hand-written replies shaped like the API's error bodies.
The SDK sleeps a short exponential back-off between attempts (about 0.5 s, then about 1 s).
"""
import anthropic
import httpx2

from harness import ScriptedTransport
from harness.scripted import message, text

MODEL = "claude-sonnet-5-5"
PARAMS = dict(model=MODEL, max_tokens=16, messages=[{"role": "user", "content": "Hi"}])


def error(status, kind, request_id):
    body = {"type": "error", "error": {"type": kind, "message": kind}, "request_id": request_id}
    return (status, body, {"request-id": request_id})


OVERLOADED = error(529, "overloaded_error", "req_illustrative_0529")
BAD_REQUEST = error(400, "invalid_request_error", "req_illustrative_0400")
SPEND_CAP = (429, {"type": "error", "error": {"type": "rate_limit_error", "message": "monthly limit reached",
                                              "details": {"error_code": "enforced_spend_limit_reached"}},
                   "request_id": "req_illustrative_0429"}, {"request-id": "req_illustrative_0429"})


def client_for(transport, max_retries):
    return anthropic.Anthropic(api_key="placeholder", max_retries=max_retries,
                               http_client=httpx2.Client(transport=transport))


def attempt(label, script, max_retries):
    transport = ScriptedTransport(*script)
    try:
        reply = client_for(transport, max_retries).messages.create(**PARAMS)
        outcome = f"ok {reply.content[0].text!r}"
    except anthropic.APIStatusError as err:
        outcome = f"{type(err).__name__} {err.status_code} {err.body['error']['type']} request id {err.request_id}"
    except anthropic.APIConnectionError as err:
        outcome = type(err).__name__
    counts = [h["x-stainless-retry-count"] for h in transport.headers]
    print(f"{label}: {len(transport.requests)} request(s), retry-count header {counts} -> {outcome}")


def main():
    attempt("529, 529, then 200, max_retries=2", [OVERLOADED, OVERLOADED, message([text("Hello.")])], 2)
    attempt("529, 529, then 200, max_retries=0", [OVERLOADED, OVERLOADED, message([text("Hello.")])], 0)
    attempt("400 is never retried, max_retries=2", [BAD_REQUEST, message([text("Hello.")])], 2)
    attempt("spend-cap 429, max_retries=2", [SPEND_CAP] * 3, 2)
    attempt("timeout twice, max_retries=1", [httpx2.ReadTimeout("scripted"), httpx2.ReadTimeout("scripted")], 1)


if __name__ == "__main__":
    main()
```
```text
529, 529, then 200, max_retries=2: 3 request(s), retry-count header ['0', '1', '2'] -> ok 'Hello.'
529, 529, then 200, max_retries=0: 1 request(s), retry-count header ['0'] -> OverloadedError 529 overloaded_error request id req_illustrative_0529
400 is never retried, max_retries=2: 1 request(s), retry-count header ['0'] -> BadRequestError 400 invalid_request_error request id req_illustrative_0400
spend-cap 429, max_retries=2: 3 request(s), retry-count header ['0', '1', '2'] -> RateLimitError 429 rate_limit_error request id req_illustrative_0429
timeout twice, max_retries=1: 2 request(s), retry-count header ['0', '1'] -> APITimeoutError
```
```typescript
// What the SDK retries on its own, and what it does not, against a scripted fetch.
// The failures are illustrative, hand-written replies shaped like the API's error bodies.
// The SDK sleeps a short exponential back-off between attempts (about 0.5 s, then about 1 s).
import Anthropic from "@anthropic-ai/sdk";
import { message, scriptedFetch, text, type Reply } from "../../../harness/ts/scriptedFetch.ts";

const PARAMS = { model: "claude-sonnet-5-5", max_tokens: 16, messages: [{ role: "user" as const, content: "Hi" }] };

function error(status: number, kind: string, requestId: string, details?: object): Reply {
  const err: Record<string, unknown> = { type: kind, message: kind };
  if (details) err.details = details;
  return { status, body: { type: "error", error: err, request_id: requestId }, headers: { "request-id": requestId } };
}

export const OVERLOADED = error(529, "overloaded_error", "req_illustrative_0529");
export const BAD_REQUEST = error(400, "invalid_request_error", "req_illustrative_0400");
export const SPEND_CAP = error(429, "rate_limit_error", "req_illustrative_0429", { error_code: "enforced_spend_limit_reached" });
export const HELLO: Reply = { body: message([text("Hello.")]) };

export function clientFor(script: Reply[], maxRetries: number) {
  const fake = scriptedFetch(script);
  return { fake, client: new Anthropic({ apiKey: "placeholder", maxRetries, fetch: fake.fetch }), params: PARAMS };
}

async function attempt(label: string, script: Reply[], maxRetries: number) {
  const { fake, client } = clientFor(script, maxRetries);
  let outcome: string;
  try {
    const reply = await client.messages.create(PARAMS);
    outcome = `ok '${(reply.content[0] as any).text}'`;
  } catch (err) {
    if (err instanceof Anthropic.APIError && err.status) outcome = `${err.constructor.name} ${err.status} ${(err.error as any).error.type} request id ${err.requestID}`;
    else if (err instanceof Anthropic.APIConnectionError) outcome = err.constructor.name;
    else throw err;
  }
  const counts = fake.seen.map((r) => r.headers["x-stainless-retry-count"]);
  console.log(`${label}: ${fake.seen.length} request(s), retry-count header [${counts.join(", ")}] -> ${outcome}`);
}

async function main() {
  await attempt("529, 529, then 200, maxRetries=2", [OVERLOADED, OVERLOADED, HELLO], 2);
  await attempt("529, 529, then 200, maxRetries=0", [OVERLOADED, OVERLOADED, HELLO], 0);
  await attempt("400 is never retried, maxRetries=2", [BAD_REQUEST, HELLO], 2);
  await attempt("spend-cap 429, maxRetries=2", [SPEND_CAP, SPEND_CAP, SPEND_CAP], 2);
  await attempt("connection fails twice, maxRetries=1", [{ networkError: true }, { networkError: true }], 1);
}

if (import.meta.main) await main();
```
```text
529, 529, then 200, maxRetries=2: 3 request(s), retry-count header [0, 1, 2] -> ok 'Hello.'
529, 529, then 200, maxRetries=0: 1 request(s), retry-count header [0] -> InternalServerError 529 overloaded_error request id req_illustrative_0529
400 is never retried, maxRetries=2: 1 request(s), retry-count header [0] -> BadRequestError 400 invalid_request_error request id req_illustrative_0400
spend-cap 429, maxRetries=2: 3 request(s), retry-count header [0, 1, 2] -> RateLimitError 429 rate_limit_error request id req_illustrative_0429
connection fails twice, maxRetries=1: 2 request(s), retry-count header [0, 1] -> APIConnectionError
```
<!-- /example -->

The output shows, in order:

1. Two overloads and then a success with `max_retries` of 2: three requests, and the `x-stainless-retry-count` header
   the SDK adds reads 0, 1 and 2.
2. The same script with `max_retries` of 0: one request, and the failure is an exception that carries the status, the
   error type and the request id. (The class is `OverloadedError` in the Python run and `InternalServerError` in the
   TypeScript run, for the same 529.)
3. A 400 with two retries allowed: still one request. The SDK does not retry a request that is wrong.
4. A spend-cap 429 with two retries allowed: **three requests**, all failing. This is the default doing work that
   cannot succeed.
5. Timeouts: with one retry allowed, two requests, then a timeout error (`APITimeoutError` in Python; the TypeScript
   run scripts a lost connection and raises `APIConnectionError`).

So the SDK default is a good floor for a script and a poor policy for a product. The product needs its own budget,
its own handling of the spend cap and its own idempotency, which is what the practice builds.

Java and Kotlin have no example tab on this page. The Java SDK documents the same defaults (two retries; connection
errors, 408, 409, 429 and 5xx retried) and the builder method `maxRetries`, so `AnthropicOkHttpClient.builder().fromEnv()
.maxRetries(4).build()` changes them. The practice below runs in all four languages.

## The practice: a retry policy

You write `call_with_retry`, in Python, TypeScript, Java or Kotlin. It takes a `send` function that returns a response
or raises a connection error, and a `sleep` function; the tests script the failures and record the waits, so no test
ever sleeps. The statement, with the exact contract, is in
`exercises/15-errors-retries-and-timeouts/unit-01/practice-1/statement.md`; each language folder has a `starter`, the
`tests` and a `run.sh` or build file. The starter fails every test.

| Id | What it checks |
|---|---|
| `m1` | Overloaded twice, then success: the good reply comes back after waits of 0.5 and 1.0 |
| `e1` | 400, 401, 404 and 413 are not retried |
| `e2` | `retry-after` is a floor for the wait |
| `e3` | The delay doubles up to the cap, and the jitter is applied last |
| `e4` | A spend-cap 429 is not retried |
| `e5` | Connection errors are retried like server errors, and end as `connection_error` |
| `e6` | Giving up reports the last reply, makes exactly `max_attempts` calls and waits one time fewer |

Case `e6` encodes a detail that is easy to get wrong: the loop that sleeps "after every failure" waits once more
after the last attempt, spending time to learn nothing.

## Traps

1. **Retrying with no jitter.** Synchronised clients retry in waves and keep an overloaded service down.
2. **Letting the default retries stand in for a policy.** The SDK retries a spend-cap 429 twice and a user-facing call
   can wait for ten seconds of retries nobody budgeted.
3. **Retrying a side effect.** A retried tool call that charges a card is a double charge. Retry the call, give the
   action its own idempotency key.

## Quiz

1. A service retries every 5xx and 429 at once, three times in a row. During a provider overload its own traffic
   triples and the errors last longer. What is the best change?
   - **a**: Retry five times in a row instead of three, so that one attempt lands after the overload
   - **b**: Wait with a doubling, capped pause plus jitter, and honour any retry-after value
   - **c**: Retry only the 429 responses and treat every 5xx response as final, since those are the provider's fault
   - **d**: Add a fixed one-second pause between the attempts, so that every client waits the same short time

2. A tool in an agent files a support ticket. A timeout makes the surrounding call retry, and customers receive two
   tickets. Which design fixes it?
   - **a**: Wait longer between the attempts so that the first one can finish
   - **b**: Lower the retry count of the call to zero for all tools
   - **c**: Ask the model in the prompt not to call the tool a second time
   - **d**: Give each filing its own key and check it before the action runs

<details>
<summary>Answer key</summary>

1. **b**. The page's three ideas are exponential growth, a cap and jitter, and "wait the larger of your back-off and the header". *a* is ruled out because immediate attempts add load, while the doubling means "a long outage is met with fewer and fewer requests". *c* is ruled out because the table lists "500, 504 and 529" under "Retry". *d* is ruled out because "after an outage, every client retries on the same schedule, and the recovering service meets all of them at once".
2. **d**. The page says "make such tools idempotent: pass your own unique key, check it before acting, and store the result under it". *b* is ruled out because without retries a transient failure fails a customer, and "retry the call, never the side effect" separates the two. *c* is ruled out because "a prompt is a request to the model, not a guarantee", and the retry happens in code. *a* is ruled out because the problem is that the first attempt may already have succeeded, and "if the first attempt actually succeeded on the server and only the answer was lost", waiting does not undo it.

</details>
