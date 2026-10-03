# Error classes by type and origin

**Level:** Developer · **Module 15:** Errors, retries and timeouts · **Page 1 of 3**
**Exams:** DV1, DV8

**After this page you can** name the status and type of every documented API error, say whether the cause is your
request, your account, the provider's capacity or the network, and pick the right first action for each.

Checked against the Claude API documentation (API errors, rate limits, Streaming messages, and the Python,
TypeScript and Java SDK pages) on 2026-10-02, and by running the module's example offline with `anthropic` 1.11.0 and
`@anthropic-ai/sdk` 0.131.0. Error replies shown are hand-scripted, shaped like the documented bodies, and labelled
illustrative.

## Why it matters

The pager goes off at 2 a.m. with "API errors up". Whether the right response is "fix our code", "call billing",
"wait", "shed load" or "nothing, it is the network" depends on one number and one string in the error. The exam's
debugging domain asks exactly this: given a status, a type or a log line, what is the cause and what is the first
action? The right answers are mechanical once the classes are in your head, and a team that treats all of them as "the
API is flaky" retries requests that can never succeed and hides faults that are theirs.

## The idea

### The status table

The API "follows a predictable HTTP error code format". Each status has an error `type` string, and each type has a
usual origin:

| Status | `type` | Origin | Retry? | First action |
|---|---|---|---|---|
| 400 | `invalid_request_error` | Your request (or a limit you set) | No | Fix the request; read the message |
| 401 | `authentication_error` | Your credential | No | Fix or rotate the key |
| 402 | `billing_error` | Your account | No | Fix payment details |
| 403 | `permission_error` | Your access | No | Check workspace and organisation access |
| 404 | `not_found_error` | Your path or id | No | Check the endpoint and resource ids |
| 409 | `conflict_error` | State of a resource | After resolving the conflict | Resolve it, then retry |
| 413 | `request_too_large` | Your request size | No | Shrink it (32 MB limit on Messages) |
| 429 | `rate_limit_error` | Capacity or quota | Yes, after `retry-after`, except a spend cap | Slow down |
| 500 | `api_error` | The provider | Yes, with back-off | Retry; give the request id to support if it persists |
| 504 | `timeout_error` | The provider took too long | Yes, but change the call | Stream long requests |
| 529 | `overloaded_error` | The provider's capacity | Yes, with back-off | Retry; ramp traffic up gradually |

Read the table as four origins. **Your request** (400, 404, 413): the same request will fail again, so retrying is
waste. **Your credential or account** (401, 402, 403, and the spend limits): a person has to act. **Capacity**
(429, 529): the request was fine and the service could not take it now. **The provider** (500, 504): something broke on
their side. A fifth origin has no status at all: **the network**, where a connection drops or times out and the SDK
raises a connection error instead of a status error.

### Three cases that are easy to confuse

**Two kinds of 429.** The ordinary one is a rate limit: capacity refills continuously, and a `retry-after` header says
how many seconds to wait. The other is a **spend cap**. The rate-limit page says that once an organisation reaches its
tier's monthly spend cap, "API usage pauses until 00:00 UTC on the first day of the next month", and requests return a
429 with the same `rate_limit_error` type but **no `retry-after` header**, and with `error.details.error_code` set to
`enforced_spend_limit_reached`. Retrying fails until access resumes, and the SDK's automatic retries do not know
that. Read the error code before you decide to wait. Both kinds are set for the whole organisation, so no setting on
one request changes them.

**A 400 can be a spend limit.** A limit you set yourself, below the tier's cap, also stops requests, and it returns a
400 `invalid_request_error` whose message begins "You have reached your specified API usage limits". Lifting the limit
is the cure, not changing the request.

**429 and 529 for the same underlying cause.** A sudden jump in traffic can produce 429 errors from "acceleration
limits" even when you are under your steady limits, and 529 happens when the whole service is busy. The advice for
both is the same: "ramp up your traffic gradually and maintain consistent usage patterns".

### The body and the request id

Every error is JSON with a top-level `error` that carries a `type` and a `message`, and a `request_id`:

<!-- illustrative -->
The documented shape, with the spend-cap detail. Ids and dates are placeholders.

```text
{"type": "error",
 "error": {"type": "rate_limit_error",
           "message": "You have reached your API usage limits ... You will regain access on <date> at 00:00 UTC.",
           "details": {"error_code": "enforced_spend_limit_reached"}},
 "request_id": "req_illustrative_0429"}
```
<!-- /illustrative -->

The same id is in the `request-id` response header. Log it, and give it to support when an error persists. The
documentation notes that the values inside these objects "may expand", so branch on the status first and treat an
unknown `type` as the generic case of its status.

### Errors after a 200

Streaming adds a case that no status code can express. A streamed reply starts with a 200, and an error can occur
later, in the middle of the stream, as an `error` event. The errors page says that in this case "error handling doesn't
follow these standard mechanisms". The client reads the stream to its end and treats an `error` event or a missing
`message_stop` as a failure. Module 17 builds that.

### What the SDKs raise

The SDKs turn each class into a typed exception, and the documentation tells you to "catch the SDK's typed classes
rather than string-matching error messages, handling the most specific classes first".

| Status | Python (`anthropic`) | TypeScript (`Anthropic.`) | Java (`com.anthropic.errors`) |
|---|---|---|---|
| 400 | `BadRequestError` | `BadRequestError` | `BadRequestException` |
| 401 | `AuthenticationError` | `AuthenticationError` | `UnauthorizedException` |
| 403 | `PermissionDeniedError` | `PermissionDeniedError` | `PermissionDeniedException` |
| 404 | `NotFoundError` | `NotFoundError` | `NotFoundException` |
| 429 | `RateLimitError` | `RateLimitError` | `RateLimitException` |
| 500 and above | `InternalServerError` | `InternalServerError` | `InternalServerException` |
| No status | `APIConnectionError` (`APITimeoutError` for a timeout) | `APIConnectionError` (`APIConnectionTimeoutError`) | `AnthropicIoException` |

Two precise observations from the course's runs. In `anthropic` 1.11.0 a 529 raised a class named `OverloadedError`, and
in `@anthropic-ai/sdk` 0.131.0 the same 529 raised `InternalServerError`; the documentation's table is a floor, not a
complete list, so catch the base class (`APIStatusError` in Python, `Anthropic.APIError` in TypeScript,
`AnthropicServiceException` in Java) as a last resort. And the Java SDK raises `SseException` for an error "encountered
during SSE streaming after a successful initial HTTP response", the case above.

### From symptom to first action

The debugging routine that the exam rewards is short:

1. **Is there a status?** If not, it is the network, or a timeout: module 15's third page.
2. **Which origin does the status say?** Your request, your account, capacity or the provider.
3. **Read `type` and `message`**, and for a 429 the `error_code` and `retry-after`.
4. **Was the 200 really a success?** Check `stop_reason` (module 14).
5. **Keep the `request-id`.**

## Traps

1. **Retrying every error.** A 400, 401, 404 or 413 returns the same answer each time. Retrying them costs time and
   hides the real fault.
2. **Treating every 429 as "wait a minute".** A spend cap has no `retry-after` and lasts until the next month; waiting
   changes nothing. Read `error.details.error_code`.
3. **String-matching messages.** The documentation says to catch typed classes; a message can be reworded without
   notice.

## Quiz

1. After a model upgrade a nightly job gets a 400 `invalid_request_error` on every call, with a message saying the
   conversation must end with a user message. What is the origin, and what is the first action?
   - **a**: Your request is at fault, so fix the request and do not retry
   - **b**: The provider is at fault, so retry with back-off until it clears
   - **c**: Capacity is at fault, so wait for the retry-after interval
   - **d**: The account is at fault, so ask billing to restore access

2. A batch job receives a 429 `rate_limit_error` on every call from the first day of a month, with no `retry-after`
   header and an error code that says the monthly limit is enforced. What should the job do?
   - **a**: Add jitter to the delays so that the calls spread out
   - **b**: Retry with longer back-off, since every 429 is a rate limit
   - **c**: Stop retrying and raise the ceiling or wait for access to resume
   - **d**: Switch the residency setting so that another pool is used

<details>
<summary>Answer key</summary>

1. **a**. The table puts 400 under "your request" and says the same request fails again, so "retrying is waste". *b* is ruled out because the provider's statuses are 500 and 504, and the 400 origin is "your request (or a limit you set)". *c* is ruled out because capacity means "the request was fine and the service could not take it now", and the 400 names a request defect. *d* is ruled out because the account origin is "401, 402, 403, and the spend limits", and a message about the list order is not one of them.
2. **c**. The page says a spend-cap 429 has no `retry-after`, and that "retrying fails until access resumes". *b* is ruled out because "the SDK's automatic retries do not know that", and a longer back-off still fails until the next month. *a* is ruled out because jitter spreads load, and the page says to read the error code "before you decide to wait". *d* is ruled out because both kinds are "set for the whole organisation", so no setting on one request gives another pool.

</details>
