# Practice: a raw Messages API client, graded on the HTTP contract

Write a small client that speaks the Messages API by hand: no SDK, one injected transport. Pick your language
folder (`python`, `typescript`, `java` or `kotlin`), open `starter/` and edit the file there. The tests check the
request that would go on the wire and the way a reply, good or bad, becomes a value or an error.

## The given types (in the starter, do not change them)

| Name | Meaning |
|---|---|
| `Request` | `method`, `url`, `headers` (a map), `body` (JSON **text**) |
| `Response` | `status`, `headers` (a map, **lower-case names**), `body` (text) |
| transport | a function from `Request` to `Response`; the tests pass a scripted one, a real client would do the network call |
| `ApiError` | a non-2xx reply: `status`, `error_type`, `detail`, `request_id` (nothing when there is none). TypeScript, Java and Kotlin spell the fields `status`, `errorType`, `detail` and `requestId` |

Java and Kotlin also get a small `Json` helper (`parse` and `stringify`) in the starter; Python and TypeScript use
their standard JSON.

## The three functions

**`build_request(api_key, model, messages, max_tokens, system=None)`** (TypeScript `buildRequest`, Java
`RawClient.buildRequest`, Kotlin `buildRequest`) returns a `Request`:

- method `POST`, URL `https://api.anthropic.com/v1/messages`;
- exactly three headers: `x-api-key` (the key), `anthropic-version` (`2023-06-01`) and `content-type`
  (`application/json`);
- a JSON body with `model`, `max_tokens` and `messages`, plus `system` **only when it is present and not blank**.

It refuses bad input with `ValueError` (TypeScript `Error`, Java and Kotlin `IllegalArgumentException`): a blank
key, no messages, or `max_tokens` below 1.

**`send_messages(transport, api_key, model, messages, max_tokens, system=None)`** builds the request, calls the
transport and:

- on a status from 200 to 299, returns the parsed JSON message;
- on any other status, raises `ApiError`:
  - for a JSON body shaped like `{"type": "error", "error": {"type": ..., "message": ...}}`, the error's `type`
    and `message`;
  - for anything else (a proxy's HTML page, an empty body), `error_type` `unknown` and the first 200 characters of
    the text, trimmed, as the detail;
  - the **request id** comes from the `request-id` response header; when the header is absent, from the body's
    `request_id` field; otherwise nothing;
  - the API key never appears in the error: replace every occurrence of it in the detail with `[redacted]`.

**`text_of(message)`** returns the `text` of the message's text blocks joined, ignoring every other block type.

## The cases

| Id | What it checks |
|---|---|
| `m1` | The request has the right method, URL, exactly three headers and a JSON body |
| `e1` | A blank or absent `system` is left out of the body |
| `e2` | Bad input is refused before the transport is called |
| `e3` | A good reply is parsed, and `text_of` joins text blocks only |
| `e4` | An error reply becomes an `ApiError`; the header's request id wins over the body's |
| `e5` | A reply that is not JSON still gives an `ApiError` (`unknown`) |
| `e6` | The API key never appears in an error |

Run the tests with the command in the language folder's `run.sh` (Python, TypeScript) or its build file.
