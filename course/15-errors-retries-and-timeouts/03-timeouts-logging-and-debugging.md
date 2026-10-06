# Timeouts, logging without secrets, and debugging by type and origin

**Level:** Developer · **Module 15:** Errors, retries and timeouts · **Page 3 of 3**
**Exams:** DV1, DV8

**After this page you can** set a timeout that fits a latency budget, compute the worst-case wait of a retried call,
log a call so that support can find it without exposing a secret, and debug a failing integration by type and origin.

Checked against the Claude API documentation (API errors, rate limits, and the Python, TypeScript and Java SDK pages)
on 2026-10-02, and by running module 15's example and practice offline in the course container. Log lines shown are
hand-written and labelled illustrative.

## Why it matters

A timeout is a decision, and most services never make it: they inherit a default that was chosen for somebody else.
The SDKs default to ten minutes, which is right for a long generation and absurd for a screen a user is waiting at. The
same service logs the failing call at debug level, the log ships to a third-party tool, and the tool now holds customer
messages. The debugging domain of the exam is made of these questions: what does this symptom mean, what do you check
first, and what must the log contain so that someone else can answer the same question at 2 a.m.

## The idea

### Timeouts: three clocks

A timeout is a limit on one of three clocks:

- **Connect**: how long to wait for the connection to open.
- **Read**: how long to wait between bytes of the reply. For a streamed reply the clock restarts with every event; for a
  non-streaming call it must cover the whole generation.
- **Total**: how long the whole call may take, wall clock.

The SDKs expose them with a different shape each. Python accepts a number or an `httpx2.Timeout` with separate
`connect`, `read` and `write` values; TypeScript takes a `timeout` in milliseconds; Java takes a `Duration` on the client
or per request. All three set the default for every call on the client or override it for one request.

The default is **ten minutes**. The SDK pages add two precise rules:

1. For a non-streaming request that the SDK expects to take longer than about ten minutes (the Python, TypeScript, Java
   and Go pages say so), it raises an error before
   sending: Python raises a `ValueError`, which "passing `stream=True` or overriding the `timeout` option" disables.
   That is a hint to stream.
2. When the timeout fires, the SDK raises a timeout error (`APITimeoutError` in Python, `APIConnectionTimeoutError` in
   TypeScript), and "requests that time out are retried twice by default".

### The worst case is a product

The time a caller may wait is not the timeout. It is the timeout times the number of attempts, plus the waits between
them:

```text
worst case = attempts x timeout + sum of the waits between attempts
```

With the SDK defaults, three attempts of ten minutes each, plus about 1.5 seconds of back-off, is thirty minutes before
the user sees an error. For a screen a user is waiting at, you set the **total deadline** from the latency budget of
module 12, then choose a per-attempt timeout and an attempt count that fit under it. A two-second budget with a
one-second timeout allows at most one retry and only with a very short pause.

Streaming changes the arithmetic in your favour: the read clock restarts at every event, so a long generation that is
steadily producing text does not hit a short read timeout, and a stalled one does.

### Logging a call without leaking

A log line has two jobs: let someone find the call, and tell them what happened. Neither job needs a secret or a
customer's text. The fields that do the work:

| Log | Why |
|---|---|
| `request-id` (response header, or `request_id` in an error body) | The one handle the provider's support can use |
| Status, error `type`, attempt number | Classifies the failure by origin (page 1) |
| Model id, `stop_reason`, `usage` | The behaviour and the cost |
| Latency of the attempt and of the whole call | Checks the budgets |
| An id of your own for the user's request | Joins your logs to the call |

<!-- illustrative -->
One log line per attempt, with nothing secret in it. Hand-written for this page.

```text
2026-10-02T09:14:07Z call=c-7d41 attempt=2 model=claude-sonnet-5-5 status=529 type=overloaded_error
  request_id=req_illustrative_0529 latency_ms=812 waited_ms=500 outcome=retry
```
<!-- /illustrative -->

What stays out: **the API key** and the `authorization` or `x-api-key` header, **prompts and replies** unless the spec
says they may be stored, and **error messages you have not checked**, because a server can echo back what it rejected.
Case `e6` of module 13's practice is the fix at its source: replace the key in any error text with a marker before the
text goes anywhere.

The SDKs' own logging deserves the same care. Their documentation says the log level comes from the `ANTHROPIC_LOG`
environment variable or a client option, and that at `debug` "all HTTP requests and responses are logged, including
headers and bodies", with some authentication headers redacted but "sensitive data in request and response bodies may
still be visible". Debug logging is a development tool.

### Debugging by type and origin

The method of page 1, with the checks that tell the origins apart:

| Symptom | Likely origin | First check | Typical cure |
|---|---|---|---|
| 400 on every call after an upgrade | Your request | Read the message; compare with the migration notes | Remove a rejected setting (sampling parameters, a prefilled turn) |
| 401 or 403 from one environment only | Your credential | Which key does that environment load? | Fix configuration, rotate the key |
| 429 with `retry-after` | Capacity | The rate-limit headers: which limit, how much left? | Smooth load, lower concurrency (module 16) |
| 429 without `retry-after`, month-end | Account | `error.details.error_code` | Raise the limit or wait |
| 529 in bursts | Provider capacity | Is your traffic ramping fast? | Back off; ramp up gradually |
| Timeouts only on long outputs | Network or timeout setting | Is it streaming? Is `max_tokens` large? | Stream, or raise the timeout on purpose |
| Replies cut mid-sentence, status 200 | Your `max_tokens` | `stop_reason` | Raise the limit or continue the turn |
| Works locally, fails behind the proxy | The path | Is the body still JSON? Is the stream buffered? | Fix the proxy, or read the raw text |

The most valuable debugging tool in this course is the one the practices use: **reproduce the failure offline**. A
scripted transport that returns the exact status, headers and body that production logged turns an incident into a unit
test, and the test stays in the suite as the regression guard. If you cannot script it, you do not yet know the cause.

## Traps

1. **Inheriting the ten-minute default for a user-facing call.** Together with two default retries, a hung connection
   makes the user wait for half an hour. Set a total deadline from the budget.
2. **Logging the whole exchange.** Full request and reply bodies carry customer text, and a rejected key can be echoed in
   an error message. Log ids, statuses and counts.
3. **Debugging from the message text.** Branch on the status, the type and the error code, and keep the request id; the
   message can be reworded.

## Quiz

1. A screen that a user watches calls the API with the SDK defaults. The connection opens at once but the reply never
   arrives, and users are now stuck for about half an hour. What is the best first change?
   - **a**: Raise the number of retries so that a stuck call is more likely to succeed in the end
   - **b**: Cap the whole call at the latency budget, since each try adds a timeout to the wait
   - **c**: Turn on debug logging so that the stuck call is recorded in full for later reading
   - **d**: Stream the reply instead, since a streamed reply restarts the read clock at each event

2. A team opens a ticket with the provider about one call that failed last night. Which item should their log have
   kept for it?
   - **a**: The reply text, so that the provider can see what the model wrote in answer
   - **b**: The complete request body, so that the call can be replayed from the log later
   - **c**: The key used for the call, so that the provider can match the account at once
   - **d**: The request id from the response, so that support can trace that exchange

<details>
<summary>Answer key</summary>

1. **b**. The page says to "set the total deadline from the latency budget of module 12, then choose a per-attempt timeout and an attempt count that fit under it". *a* is ruled out because "three attempts of ten minutes each" is the cause of the half hour, and more retries lengthen it. *c* is ruled out because logging records the wait and does not shorten it, and "debug logging is a development tool". *d* is ruled out because streaming is the cure for "Timeouts only on long outputs", and a reply that sends no event never restarts the clock, so each try still waits out the default: "The SDKs default to ten minutes".
2. **d**. The page calls the request id "the one handle the provider's support can use". *b* is ruled out because "prompts and replies unless the spec says they may be stored" stay out of the log. *c* is ruled out because the log must not hold "the API key and the authorization or x-api-key header". *a* is ruled out because "neither job needs a secret or a customer's text", and the reply is customer-facing text.

</details>

## Module quiz

This quiz covers all three pages of the module.

1. A job's calls sometimes fail with a lost network link, and sometimes with a 409 because two workers touched the same
   resource. How should the job handle the two kinds?
   - **a**: Repeat both at once, since each is a passing fault that clears by itself
   - **b**: Repeat both, since the second will succeed too once the clash is resolved
   - **c**: Repeat neither until a person has checked, since each needs someone to act first
   - **d**: Repeat only the second, since the first has no status that the code could read

2. A user-facing summary call has a budget of 2,000 ms. Its client allows three attempts with a 1,000 ms timeout and
   waits 500 ms, then 1,000 ms, between them. What is the worst case for the caller?
   - **a**: Four and a half seconds, because the pauses add to the three tries
   - **b**: One second, because the timeout bounds the whole call from start to finish
   - **c**: Three seconds, because the waits between the attempts are not counted
   - **d**: Two seconds, because the budget itself stops the call at that point

3. Only users in one office report that streamed answers arrive in one lump and, now and then, as an HTML page; the
   same app runs smoothly from home. What is the most likely origin?
   - **a**: The network path, because a hop on the way alters what comes back
   - **b**: Their credential, because that office's setup strips the key from calls
   - **c**: Capacity, because the provider is too busy to take every request
   - **d**: The output limit, because the replies are cut off before the end

4. Errors with status 529 appear in bursts each time a marketing campaign multiplies the job's traffic. What is the
   best response?
   - **a**: Request a higher rate limit, since the campaign outgrows the account's quota
   - **b**: Retry each failed call at once, since a provider overload clears in seconds
   - **c**: Ramp the load up in steps, since sudden jumps outpace the provider
   - **d**: Raise the client timeout, since the provider answers slowly under load

<details>
<summary>Answer key</summary>

1. **b**. The page's table lists "A lost or timed-out connection (no status)" and "408, 409 (once the conflict is resolved)" under "Retry". *a* is ruled out because a 409 is retried only "once the conflict is resolved". *c* is ruled out because a lost link needs no person: "A lost or timed-out connection (no status)" is the first row under "Retry". *d* is ruled out because the SDKs retry "because of a network connectivity problem" as well.
2. **a**. The page's formula is "attempts x timeout + sum of the waits between attempts": three seconds of attempts and one and a half seconds of waits make four and a half. *b* is ruled out because "the time a caller may wait is not the timeout". *c* is ruled out because the formula adds "the waits between attempts". *d* is ruled out because a budget bounds nothing until you "Set a total deadline from the budget", and this client only sets a per-attempt timeout.
3. **a**. The page's table puts "works locally, fails behind the proxy" under the path, with the checks "Is the body still JSON? Is the stream buffered?"; an HTML page fails the first and a lumped stream the second, in one office only. *b* is ruled out because a credential fault shows as a "401 or 403 from one environment only", an error status rather than a buffered stream or an HTML page. *c* is ruled out because capacity means "the request was fine and the service could not take it now", with a 429 or 529 status. *d* is ruled out because a cut reply is "replies cut mid-sentence, status 200" and does not come as HTML.
4. **c**. The table lists "529 in bursts" with the check "Is your traffic ramping fast?" and the cure "Back off; ramp up gradually". *b* is ruled out because immediate retries make it worse: "a brief overload becomes a retry storm that keeps the service down". *a* is ruled out because a 529 is not the account's quota: "529 happens when the whole service is busy". *d* is ruled out because raising the timeout is a cure for "Timeouts only on long outputs", while a 529 comes from "The provider's capacity".

</details>
