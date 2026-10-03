# The HTTP contract under every SDK

**Level:** Developer · **Module 13:** One REST API under every SDK · **Page 1 of 3**
**Exams:** DV1, DV2

**After this page you can** write a Messages API request by hand (URL, headers, JSON body), read a response and an
error body, and say which parts of the contract may change and which may not.

Checked against the Claude API documentation (API overview, API versions, errors, Using the Messages API) on
2026-10-02. The request and reply shown are the documented forms, hand-written with placeholder values and
labelled illustrative; nothing on this page was sent to a server, and no key was used.

## Why it matters

Every SDK the course uses (Python, TypeScript, Java, and Kotlin through the Java SDK) is a layer over one HTTP API.
When something goes wrong, the layer hides the thing you need: which header was sent, what the server answered,
whether a proxy rewrote the reply. The Developer exam asks about the contract in scenario form, for example which
header is mandatory, what a 413 means, or why a client that worked yesterday broke when a field appeared. The
engineer who knows the raw contract can debug any SDK and write a client for a language that has none.

## The idea

### One endpoint carries the conversation

The documentation describes the Claude API as "a RESTful API at `https://api.anthropic.com`". The call this course
uses everywhere is one request:

| Part | Value |
|---|---|
| Method and path | `POST /v1/messages` |
| Authentication | `Authorization: Bearer <token>`, where the token is your API key or a short-lived access token; the legacy `x-api-key` header is still supported (the examples on this page use it, as the fallback the overview allows) |
| Version | `anthropic-version: 2023-06-01`, which the overview lists as required |
| Body type | `content-type: application/json` |
| Body | JSON with at least `model`, `max_tokens` and `messages` |
| Workspace | `anthropic-workspace-id: <id>`, only when the key spans several workspaces |

The same host serves the other APIs the course meets later: Message Batches (`POST /v1/messages/batches`), token
counting (`POST /v1/messages/count_tokens`), models (`GET /v1/models`), files (`POST /v1/files`) and skills. Requests
to the Messages and token-counting endpoints may be at most 32 MB; a larger request is refused with a 413
`request_too_large`, and on the direct API, "Cloudflare returns this error before the request reaches the API
servers".

<!-- illustrative -->
The documented request, as the wire carries it. The key is a placeholder read from an environment variable in real
use; the model id is the one this course pins.

```text
POST /v1/messages HTTP/1.1
Host: api.anthropic.com
x-api-key: $ANTHROPIC_API_KEY
anthropic-version: 2023-06-01
content-type: application/json

{"model": "claude-sonnet-5-5", "max_tokens": 64,
 "messages": [{"role": "user", "content": "Capital of France?"}]}
```
<!-- /illustrative -->

Authentication, version and body type are all a plain request needs: the overview marks no `Accept` or `User-Agent` header as required. The SDKs send these headers for you. The Python and TypeScript pages say the SDK "automatically sends the
`anthropic-version` header set to `2023-06-01`", and the overview lists header management first among the benefits of
the SDKs. The next page measures it.

### What comes back

A successful reply is a JSON object, the **message**. The documented fields you will use in every module are `id`,
`type` (always `message`), `role` (`assistant`), `content` (a list of blocks), `model`, `stop_reason`,
`stop_sequence` and `usage`.

<!-- illustrative -->
A reply in the documented shape. The id is a placeholder and the text is hand-written.

```text
HTTP/1.1 200 OK
request-id: req_illustrative_0001
content-type: application/json

{"id": "msg_illustrative", "type": "message", "role": "assistant", "model": "claude-sonnet-5-5",
 "content": [{"type": "text", "text": "Paris."}], "stop_reason": "end_turn", "stop_sequence": null,
 "usage": {"input_tokens": 12, "output_tokens": 6}}
```
<!-- /illustrative -->

Two things outside the body matter. The **`request-id` response header** is "a globally unique identifier for the
request", and the documentation says to include it when you contact support about a specific request. And the
response carries **rate-limit headers** such as `anthropic-ratelimit-requests-remaining` and `retry-after`, which
module 16 uses.

### Errors are JSON too, mostly

A failed request uses an HTTP status, and the body is always a JSON object with a top-level `error` that "always
includes a `type` and `message` value", plus a `request_id`. The statuses and types are the subject of module 15; the
point here is the shape.

<!-- illustrative -->
The documented error shape for a 404. The id is a placeholder.

```text
{"type": "error",
 "error": {"type": "not_found_error", "message": "The requested resource could not be found."},
 "request_id": "req_illustrative_0404"}
```
<!-- /illustrative -->

Always JSON is the API's promise about its own errors. Anything between you and the API, such as a corporate proxy,
a gateway or the edge in front of the service, can answer with a body that is not that JSON. A hand-written client
must keep working when the body is HTML, and the practice on the next page makes you handle it. Retrying does not
help: the proxy answers the same page again.

### What may change, and what may not

The versioning page is the contract about change. For a given version Anthropic preserves "existing input
parameters" and "existing output parameters". It also reserves the right to add optional inputs, "add additional
values to the output", change the conditions for specific error types and "add new variants to enum-like output
values (for example, streaming event types)". The only current value of `anthropic-version` is `2023-06-01`; the page
calls earlier versions deprecated and says they "may be unavailable for new users".

Three habits follow, and each is a line you will see in a code review:

1. **Read what you need and ignore the rest.** An unknown field is not an error.
2. **Treat enum-like values as open.** A `stop_reason`, a content block type or an event type you do not know is a
   case to handle, not a crash.
3. **Be strict only on what you send.** Your request has to match the documented shape exactly.

### Where the contract is not the same

The same models are also served through cloud platforms. On Amazon Bedrock and Google Cloud the endpoint, the
authentication and some features differ: the overview says that with a cloud platform, "authentication is integrated
with the cloud provider's IAM system". The Claude API page describes the direct service; a question that names a
platform is a question about that platform's documentation.

## Traps

1. **Putting the key where a browser can read it.** A browser app that calls the API directly exposes the key to every
   visitor. The TypeScript SDK refuses to run in a browser unless a flag is set, and its own documentation calls the
   flag `dangerouslyAllowBrowser`. Keep the key on a server and read it from the environment.
2. **Reading the status and stopping there.** A 200 does not mean the answer is complete: `stop_reason` can be
   `max_tokens`. A streamed reply can start with a 200 and still end with an error event. Check both layers.
3. **Parsing the contract strictly.** A client that fails on an unknown field or an unknown enum value breaks on a
   change the versioning page allows.

## Quiz

1. A team hand-writes a client in a language that has no SDK. The request carries the content type and the key, and
   the body copies the documentation's example, yet every call fails with a 400. Which addition is most likely
   missing?
   - **a**: The version header that names the API revision in use by the caller
   - **b**: An accept header that names the media type the client wants back
   - **c**: A user-agent header that names the client library and its version
   - **d**: A workspace header that names the workspace this call should run under

2. A gateway between a service and the provider starts answering some failures with web pages. The service's
   failure handler throws while decoding them. Which design is best?
   - **a**: Decode the body as JSON first and stop the process whenever that decoding fails
   - **b**: Ask the gateway team to promise that every reply stays JSON in the provider's own format
   - **c**: Map each non-2xx status to a typed fault, using the raw text if the body is not JSON
   - **d**: Retry each failing call until the body decodes cleanly into the expected shape

<details>
<summary>Answer key</summary>

1. **a**. The overview lists the version header as required, and a hand-written client has to send it itself. *b* is ruled out because "the overview marks no Accept or User-Agent header as required". *c* is ruled out for the same reason: "the overview marks no Accept or User-Agent header as required". *d* is ruled out because the workspace header is needed "only when the key spans several workspaces".
2. **c**. A hand-written client has to cope with a non-JSON body, so the handler falls back to the text. *b* is ruled out because "always JSON is the API's promise about its own errors", and a gateway is not covered by it. *a* is ruled out because "a hand-written client must keep working when the body is HTML". *d* is ruled out because "the proxy answers the same page again".

</details>
