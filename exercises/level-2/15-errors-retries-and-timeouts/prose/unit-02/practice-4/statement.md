# Practice: a retry policy, graded on scripted failures

Write the function that decides whether to try an API call again, and for how long to wait. No test waits: the
sleep is injected and the failures are scripted.

## What is already written, and what you write

The starter is a working retry policy with seven gaps cut out of it. The plumbing is written and correct: the given types, `call_with_retry` itself
(the loop, the attempt count, the status check, the sleep and the decision to give up), and the way a retryable status and a spend cap combine. Each
gap is a small function with its signature, a comment that says what it receives and returns with one example, and the cases it unlocks. A gap
returns a neutral value, so the starter runs and fails the cases on an assertion. To see what a gap receives, debug it by logging its input with the
`log` line at the top of the file; a run shows the lines under the failing case. Write them in this order (Python names; the TypeScript, Java and
Kotlin names are the camel-case forms, with `delayFor` for `_delay`):

1. `_status_retryable` unlocks `m1` and `e1`: which statuses may be tried again.
2. `_spend_cap` unlocks `e4`: a 429 that says the spend limit is reached.
3. `_error_type` unlocks `e1`, `e4` and `e6`: the API's error type, or `unknown`.
4. `_request_id` unlocks `e1`, `e4` and `e6`: the `request-id` header.
5. `_connection_failure` unlocks `e5`: status 0 and `connection_error`.
6. `_retry_after` unlocks `e2`: the header as seconds.
7. `_delay` unlocks `m1`, `e3` and `e5`: the doubling delay, the cap and the jitter applied last.

`m1` needs gaps 1 and 7. A few lines each, about fifteen in all.

## The given types

| Name | Meaning |
|---|---|
| `Response` | `status`, `headers` (lower-case names) and `body` (the parsed JSON object, or empty) |
| `send` | a function with no arguments that returns a `Response` or raises `TransportError` (the connection failed or timed out, so no reply came) |
| `sleep` | a function that takes the seconds to wait; the tests record them |
| `CallFailed` | the final failure: `status`, `error_type`, `attempts`, `request_id` (TypeScript, Java and Kotlin: `errorType`, `requestId`) |
| policy | `max_attempts` (default 4), `base_delay` (0.5), `cap` (8.0) and `jitter`, a function applied to each computed delay (default: the delay itself). Python takes them as keyword arguments, TypeScript as an options object, Java and Kotlin as a `Policy` |

## `call_with_retry(send, sleep, ...)` (TypeScript `callWithRetry`, Java `Retry.callWithRetry`, Kotlin `callWithRetry`)

Call `send` up to `max_attempts` times.

1. A reply with a status **below 400** is returned at once.
2. **Retry** on a connection failure and on the statuses 408, 409, 429 and every 500 and above (529 included).
   Every other status of 400 and above is **not** retried: raise `CallFailed` with that attempt count.
3. A **429 whose body says `error.details.error_code` is `enforced_spend_limit_reached`** is a spend cap, not a
   rate limit: retrying cannot succeed, so do not retry it.
4. The wait before attempt *n + 1* is `jitter(min(cap, base_delay * 2^(n-1)))`, where *n* is the attempt that just
   failed (`n` starts at 1).
5. If the failed reply has a **`retry-after`** header (seconds), the wait is at least that long: wait the larger of
   the jittered delay and the header.
6. **Never wait after the last attempt.** When the attempts run out, raise `CallFailed` with the last reply's status,
   its `error.type` (`unknown` when the body has none), the number of attempts made and the `request-id` header.
   When the last failure was a lost connection: status `0`, error type `connection_error`, no request id.

## The cases

| Id | What it checks |
|---|---|
| `m1` | Overloaded twice, then success: the good reply comes back after waits of 0.5 and 1.0 |
| `e1` | 400, 401, 404 and 413 are not retried |
| `e2` | `retry-after` is a floor for the wait |
| `e3` | The delay doubles up to the cap, and the jitter is applied last |
| `e4` | A spend-cap 429 is not retried |
| `e5` | Connection errors are retried like server errors, and give `connection_error` at the end |
| `e6` | Giving up reports the last reply, makes exactly `max_attempts` calls and waits one time fewer |
