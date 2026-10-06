# What the SDK does for you, and a raw client

**Level:** Developer · **Module 13:** One REST API under every SDK · **Page 2 of 3**
**Exams:** DV1, DV2

**After this page you can** list what an official SDK adds to the raw HTTP call, show it by comparing the two
requests, and write the raw client yourself against a scripted transport.

Checked against the Claude API documentation (API overview, errors, and the Python, TypeScript and Java SDK pages)
on 2026-10-02, and by running the example below and the practice offline in the course container with
`anthropic` 1.11.0 (Python), `@anthropic-ai/sdk` 0.131.0 (TypeScript) and, for the practice, Java 25 and Kotlin 2.4.
The model id is `claude-sonnet-5-5`; replies are hand-scripted and labelled illustrative.

## Why it matters

"Just use the SDK" is good advice until a call misbehaves. Then the questions are concrete: did the SDK retry, how
long did it wait, which header carried the version, where is the request id? The exam asks what the SDK handles and
what stays your job, and a team that cannot answer ends up with a proxy and a packet capture to find a missing header.
Writing the raw client once, against a transport you control, makes the SDK's behaviour something you can reason
about instead of something you hope for.

## The idea

### The same request, twice

The example makes one Messages call by hand and then through the SDK. Both go to the same scripted transport, so the
program can record exactly what each one put on the wire, and nothing leaves the container. The first reply is a
200; the second is a 429 with a `retry-after` header and a request id.

<!-- example: m13-raw-vs-sdk tabs: python,typescript,java,kotlin -->
```python
"""One Messages call written by hand with httpx2, then the same call through the SDK.

Both go through the same scripted transport, so nothing leaves the container. The reply is an
illustrative, hand-written Messages response (claude-sonnet-5-5), not a capture.
"""
import logging
import anthropic
import httpx2

from harness import ScriptedTransport
from harness.scripted import message, text

log = logging.getLogger(__name__)

MODEL = "claude-sonnet-5-5"
URL = "https://api.anthropic.com/v1/messages"
PAYLOAD = {"model": MODEL, "max_tokens": 64, "messages": [{"role": "user", "content": "Capital of France?"}]}
OK = message([text("Paris.")])
LIMITED = (429, {"type": "error", "error": {"type": "rate_limit_error", "message": "Rate limited"},
                 "request_id": "req_illustrative_0001"}, {"retry-after": "7", "request-id": "req_illustrative_0001"})


def raw_call(transport):
    """The HTTP request the SDK would build, written out: three headers and a JSON body."""
    headers = {"x-api-key": "placeholder", "anthropic-version": "2023-06-01", "content-type": "application/json"}
    with httpx2.Client(transport=transport) as http:
        return http.post(URL, headers=headers, json=PAYLOAD)


def sdk_call(transport):
    client = anthropic.Anthropic(api_key="placeholder", max_retries=0,
                                 http_client=httpx2.Client(transport=transport))
    return client.messages.create(**PAYLOAD)


def main():
    t_raw, t_sdk = ScriptedTransport(OK, LIMITED), ScriptedTransport(OK, LIMITED)

    reply = raw_call(t_raw)
    print("raw :", reply.request.method, t_raw.urls[0], "->", reply.status_code, repr(reply.json()["content"][0]["text"]))
    message_ = sdk_call(t_sdk)
    print("sdk :", "POST", t_sdk.urls[0], "->", "200", repr(message_.content[0].text))
    print("same URL:", t_raw.urls[0] == t_sdk.urls[0], "| same body:", t_raw.requests[0] == t_sdk.requests[0])
    for name in ("anthropic-version", "content-type"):
        print(f"{name}: raw {t_raw.headers[0][name]} | sdk {t_sdk.headers[0][name]}")
    print("headers only the SDK adds:", ", ".join(sorted(set(t_sdk.headers[0]) - set(t_raw.headers[0]))))

    limited = raw_call(t_raw)
    print("raw 429 :", limited.status_code, limited.json()["error"]["type"], "retry-after", limited.headers["retry-after"])
    try:
        sdk_call(t_sdk)
    except anthropic.RateLimitError as err:
        print("sdk 429 :", type(err).__name__, err.status_code, "request id", err.request_id)


if __name__ == "__main__":
    main()
```
```text
raw : POST https://api.anthropic.com/v1/messages -> 200 'Paris.'
sdk : POST https://api.anthropic.com/v1/messages -> 200 'Paris.'
same URL: True | same body: True
anthropic-version: raw 2023-06-01 | sdk 2023-06-01
content-type: raw application/json | sdk application/json
headers only the SDK adds: x-stainless-arch, x-stainless-async, x-stainless-lang, x-stainless-os, x-stainless-package-version, x-stainless-read-timeout, x-stainless-retry-count, x-stainless-runtime, x-stainless-runtime-version, x-stainless-timeout
raw 429 : 429 rate_limit_error retry-after 7
sdk 429 : RateLimitError 429 request id req_illustrative_0001
```
```typescript
// One Messages call written by hand with fetch, then the same call through the SDK.
// Both go through the same scripted fetch, so nothing leaves the container. The reply is an
// illustrative, hand-written Messages response (claude-sonnet-5-5), not a capture.
import Anthropic from "@anthropic-ai/sdk";
import { message, scriptedFetch, text } from "../../../harness/ts/scriptedFetch.ts";
import { logger } from "./logger.ts";
const log = logger("raw_vs_sdk");

const MODEL = "claude-sonnet-5-5";
const URL = "https://api.anthropic.com/v1/messages";
const PAYLOAD = { model: MODEL, max_tokens: 64, messages: [{ role: "user" as const, content: "Capital of France?" }] };
const OK = { body: message([text("Paris.")]) };
const LIMITED = {
  status: 429,
  body: { type: "error", error: { type: "rate_limit_error", message: "Rate limited" }, request_id: "req_illustrative_0001" },
  headers: { "retry-after": "7", "request-id": "req_illustrative_0001" },
};

// The HTTP request the SDK would build, written out: three headers and a JSON body.
export function rawCall(fetchFn: typeof fetch) {
  const headers = { "x-api-key": "placeholder", "anthropic-version": "2023-06-01", "content-type": "application/json" };
  return fetchFn(URL, { method: "POST", headers, body: JSON.stringify(PAYLOAD) });
}

export function sdkCall(fetchFn: typeof fetch) {
  const client = new Anthropic({ apiKey: "placeholder", maxRetries: 0, fetch: fetchFn });
  return client.messages.create(PAYLOAD);
}

async function main() {
  const raw = scriptedFetch([OK, LIMITED]);
  const sdk = scriptedFetch([OK, LIMITED]);

  const reply = await rawCall(raw.fetch);
  console.log("raw :", raw.seen[0].method, raw.seen[0].url, "->", reply.status, JSON.stringify(((await reply.json()) as any).content[0].text));
  const msg = await sdkCall(sdk.fetch);
  console.log("sdk :", sdk.seen[0].method, sdk.seen[0].url, "->", "200", JSON.stringify((msg.content[0] as any).text));
  console.log("same URL:", raw.seen[0].url === sdk.seen[0].url, "| same body:", JSON.stringify(raw.seen[0].body) === JSON.stringify(sdk.seen[0].body));
  for (const name of ["anthropic-version", "content-type"]) {
    console.log(`${name}: raw ${raw.seen[0].headers[name]} | sdk ${sdk.seen[0].headers[name]}`);
  }
  const onlySdk = Object.keys(sdk.seen[0].headers).filter((h) => !(h in raw.seen[0].headers)).sort();
  console.log("headers only the SDK adds:", onlySdk.join(", "));

  const limited = await rawCall(raw.fetch);
  console.log("raw 429 :", limited.status, ((await limited.json()) as any).error.type, "retry-after", limited.headers.get("retry-after"));
  try {
    await sdkCall(sdk.fetch);
  } catch (err) {
    if (err instanceof Anthropic.RateLimitError) console.log("sdk 429 :", err.constructor.name, err.status, "request id", err.requestID);
    else throw err;
  }
}

if (import.meta.main) await main();
```
```text
raw : POST https://api.anthropic.com/v1/messages -> 200 "Paris."
sdk : POST https://api.anthropic.com/v1/messages -> 200 "Paris."
same URL: true | same body: true
anthropic-version: raw 2023-06-01 | sdk 2023-06-01
content-type: raw application/json | sdk application/json
headers only the SDK adds: accept, user-agent, x-stainless-arch, x-stainless-lang, x-stainless-os, x-stainless-package-version, x-stainless-retry-count, x-stainless-runtime, x-stainless-runtime-version, x-stainless-timeout
raw 429 : 429 rate_limit_error retry-after 7
sdk 429 : RateLimitError 429 request id req_illustrative_0001
```
```java
import static harness.Scripted.map;
import static harness.Scripted.message;
import static harness.Scripted.text;

import com.anthropic.client.AnthropicClient;
import com.anthropic.errors.RateLimitException;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.fasterxml.jackson.databind.JsonNode;
import harness.Reply;
import harness.Scripted;
import harness.ScriptedHttp;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * One Messages call written by hand, then the same call through the SDK.
 *
 * <p>Both go through the same kind of scripted transport, so nothing leaves the container. The reply is an
 * illustrative, hand-written Messages response (claude-sonnet-5-5), not a capture.
 * (`harness` is the course's stand-in: a scripted transport plugged into the SDK's own HttpClient hook.)
 */
public final class RawVsSdk {
    private static final System.Logger LOG = System.getLogger(RawVsSdk.class.getName());
    static final String MODEL = "claude-sonnet-5-5";
    static final String URL = "https://api.anthropic.com/v1/messages";
    static final Map<String, Object> PAYLOAD = map("model", MODEL, "max_tokens", 64, "messages", List.of(map("role", "user", "content", "Capital of France?")));
    static final Map<String, Object> OK = message(List.of(text("Paris.")));
    static final Reply LIMITED = Reply.json(429, map("type", "error", "error", map("type", "rate_limit_error", "message", "Rate limited"), "request_id", "req_illustrative_0001"),
        "retry-after", "7", "request-id", "req_illustrative_0001");

    /** The HTTP request the SDK would build, written out: three headers and a JSON body. */
    static Reply rawCall(ScriptedHttp transport) {
        Map<String, String> headers = Map.of("x-api-key", "placeholder", "anthropic-version", "2023-06-01", "content-type", "application/json");
        return transport.send("POST", URL, headers, PAYLOAD);
    }

    static Message sdkCall(ScriptedHttp transport) {
        AnthropicClient client = Scripted.clientOn(transport, 0);
        return client.messages().create(MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(64).addUserMessage("Capital of France?").build());
    }

    private static String repr(String s) {
        return "'" + s + "'";
    }

    public static void main(String[] args) {
        ScriptedHttp raw = Scripted.http(OK, LIMITED);
        ScriptedHttp sdk = Scripted.http(OK, LIMITED);

        Reply reply = rawCall(raw);
        System.out.println("raw : " + raw.methods.get(0) + " " + raw.urls.get(0) + " -> " + reply.status() + " " + repr(reply.json().at("/content/0/text").asText()));
        Message message = sdkCall(sdk);
        System.out.println("sdk : POST " + sdk.urls.get(0) + " -> 200 " + repr(message.content().get(0).asText().text()));
        System.out.println("same URL: " + py(raw.urls.get(0).equals(sdk.urls.get(0))) + " | same body: " + py(raw.requests.get(0).equals(sdk.requests.get(0))));
        for (String name : List.of("anthropic-version", "content-type")) {
            System.out.println(name + ": raw " + raw.headers.get(0).get(name) + " | sdk " + sdk.headers.get(0).get(name));
        }
        TreeSet<String> onlySdk = new TreeSet<>(sdk.headers.get(0).keySet());
        onlySdk.removeAll(raw.headers.get(0).keySet());
        System.out.println("headers only the SDK adds: " + String.join(", ", onlySdk));

        Reply limited = rawCall(raw);
        JsonNode body = limited.json();
        System.out.println("raw 429 : " + limited.status() + " " + body.at("/error/type").asText() + " retry-after " + limited.headers().get("retry-after"));
        try {
            sdkCall(sdk);
        } catch (RateLimitException err) {
            System.out.println("sdk 429 : " + err.getClass().getSimpleName() + " " + err.statusCode() + " request id " + err.headers().values("request-id").get(0));
        }
    }

    private static String py(boolean value) {
        return value ? "True" : "False";
    }
}
```
```text
raw : POST https://api.anthropic.com/v1/messages -> 200 'Paris.'
sdk : POST https://api.anthropic.com/v1/messages -> 200 'Paris.'
same URL: True | same body: True
anthropic-version: raw 2023-06-01 | sdk 2023-06-01
content-type: raw application/json | sdk application/json
headers only the SDK adds: accept, user-agent, x-stainless-arch, x-stainless-kotlin-version, x-stainless-lang, x-stainless-os, x-stainless-os-version, x-stainless-package-version, x-stainless-retry-count, x-stainless-runtime, x-stainless-runtime-version
raw 429 : 429 rate_limit_error retry-after 7
sdk 429 : RateLimitException 429 request id req_illustrative_0001
```
```kotlin
import com.anthropic.errors.RateLimitException
import com.anthropic.models.messages.Message
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.Model
import harness.Reply
import harness.Scripted
import harness.Scripted.map
import harness.Scripted.message
import harness.Scripted.text
import harness.ScriptedHttp

private val log = System.getLogger("raw_vs_sdk")

/**
 * One Messages call written by hand, then the same call through the SDK.
 *
 * Both go through the same kind of scripted transport, so nothing leaves the container. The reply is an
 * illustrative, hand-written Messages response (claude-sonnet-5-5), not a capture.
 * (`harness` is the course's stand-in: a scripted transport plugged into the SDK's own HttpClient hook.)
 */
const val MODEL = "claude-sonnet-5-5"
const val URL = "https://api.anthropic.com/v1/messages"
val PAYLOAD = map("model", MODEL, "max_tokens", 64, "messages", listOf(map("role", "user", "content", "Capital of France?")))
val OK = message(listOf(text("Paris.")))
val LIMITED = Reply.json(
    429, map("type", "error", "error", map("type", "rate_limit_error", "message", "Rate limited"), "request_id", "req_illustrative_0001"),
    "retry-after", "7", "request-id", "req_illustrative_0001",
)

/** The HTTP request the SDK would build, written out: three headers and a JSON body. */
fun rawCall(transport: ScriptedHttp): Reply =
    transport.send("POST", URL, mapOf("x-api-key" to "placeholder", "anthropic-version" to "2023-06-01", "content-type" to "application/json"), PAYLOAD)

fun sdkCall(transport: ScriptedHttp): Message =
    Scripted.clientOn(transport, 0).messages().create(MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(64).addUserMessage("Capital of France?").build())

private fun py(value: Boolean) = if (value) "True" else "False"

fun main() {
    val raw = Scripted.http(OK, LIMITED)
    val sdk = Scripted.http(OK, LIMITED)

    val reply = rawCall(raw)
    println("raw : ${raw.methods[0]} ${raw.urls[0]} -> ${reply.status()} '${reply.json().at("/content/0/text").asText()}'")
    val message = sdkCall(sdk)
    println("sdk : POST ${sdk.urls[0]} -> 200 '${message.content()[0].asText().text()}'")
    println("same URL: ${py(raw.urls[0] == sdk.urls[0])} | same body: ${py(raw.requests[0] == sdk.requests[0])}")
    for (name in listOf("anthropic-version", "content-type")) println("$name: raw ${raw.headers[0][name]} | sdk ${sdk.headers[0][name]}")
    println("headers only the SDK adds: " + (sdk.headers[0].keys - raw.headers[0].keys).sorted().joinToString(", "))

    val limited = rawCall(raw)
    println("raw 429 : ${limited.status()} ${limited.json().at("/error/type").asText()} retry-after ${limited.headers()["retry-after"]}")
    try {
        sdkCall(sdk)
    } catch (err: RateLimitException) {
        println("sdk 429 : ${err.javaClass.simpleName} ${err.statusCode()} request id ${err.headers().values("request-id")[0]}")
    }
}
```
```text
raw : POST https://api.anthropic.com/v1/messages -> 200 'Paris.'
sdk : POST https://api.anthropic.com/v1/messages -> 200 'Paris.'
same URL: True | same body: True
anthropic-version: raw 2023-06-01 | sdk 2023-06-01
content-type: raw application/json | sdk application/json
headers only the SDK adds: accept, user-agent, x-stainless-arch, x-stainless-kotlin-version, x-stainless-lang, x-stainless-os, x-stainless-os-version, x-stainless-package-version, x-stainless-retry-count, x-stainless-runtime, x-stainless-runtime-version
raw 429 : 429 rate_limit_error retry-after 7
sdk 429 : RateLimitException 429 request id req_illustrative_0001
```
<!-- /example -->

Read the output as a list:

- **The contract is the same.** Same URL, same JSON body, same `anthropic-version`. The SDK adds nothing the API
  requires beyond what the raw call already had.
- **The SDK adds identifying headers** (the `x-stainless-*` family in these runs): the platform, the runtime version,
  the package version, the retry count and the timeout. They help debugging and are not part of your contract.
  The exact list differs between languages because the HTTP stacks differ.
- **A 429 is a status for raw code and a typed exception for the SDK.** The Python and TypeScript SDKs raised a `RateLimitError`, and both carry the status and the request id read from the `request-id` header.
  Java and Kotlin: the Java SDK page maps a 429 to `RateLimitException` and offers `withRawResponse()` for the
  headers and `requestId()`.

The Java and Kotlin tabs use the Java SDK (`com.anthropic:anthropic-java`, pinned 2.68.0; Kotlin uses the same artifact). It
builds the same request from `MessageCreateParams` and sends it with `client.messages().create(params)`; in a program that talks to
the real API the client comes from `AnthropicOkHttpClient.fromEnv()`, and here it runs on the scripted transport. Two lines of the
output name things that belong to the SDK and so read differently in these tabs: the headers only the SDK adds (the Java SDK adds
`accept` and `user-agent`, and reports its Kotlin version) and the class of the rate-limit error (`RateLimitException`, where the
Python SDK has `RateLimitError`). The rest of the output is the same text. The practice below runs in all four languages.

### What the SDK does for you

The overview lists the benefits; the SDK pages confirm each one. In the order you meet them:

| The SDK handles | Raw code must |
|---|---|
| Authentication, version and content-type headers | Set the three headers on every request |
| Building the JSON body from typed parameters | Serialise the body and keep field names exact |
| Parsing the reply into typed objects (a `Message` with `content` blocks) | Parse JSON and navigate untyped maps |
| Typed exceptions per status (`BadRequestError`, `RateLimitError`, `InternalServerError`...) | Branch on the status and read the body |
| Retries: "certain errors are automatically retried 2 times by default, with a short exponential backoff" | Write the retry policy (module 15) |
| A default timeout of 10 minutes, and a check on non-streaming requests that are expected to take longer | Pick and enforce timeouts |
| The request id on every response object (`_request_id` in Python and TypeScript) | Read the `request-id` header |
| Streaming helpers that assemble the final message (module 17) | Parse server-sent events |
| Pagination iterators for list endpoints | Follow cursors |

### What the SDK does not do

The same pages are as clear about the limits, and the exam likes these:

- **It does not choose your retry budget.** Two retries is a default for a generic caller, and it applies to every
  retryable failure, including ones that cannot succeed. A spend-cap 429 stays in force until the next month begins,
  so every retry of it fails (module 15 shows the SDK retrying one).
- **It does not bound your concurrency.** Fifty parallel calls are fifty parallel calls (module 16).
- **It does not keep your conversation.** The Messages API is stateless, so the history is yours (module 14).
- **It does not keep secrets out of your logs.** At debug level the TypeScript SDK's documentation says "all HTTP
  requests and responses are logged, including headers and bodies", with some authentication headers redacted but
  "sensitive data in request and response bodies may still be visible".
- **It does not validate your business rules.** A reply that parses is a valid message, not a correct answer.

### Reaching the HTTP layer from an SDK

Sometimes the SDK is right and you still need the raw facts. Each SDK has an escape hatch, and each is named on its
documentation page: `with_raw_response` in Python (headers, then `.parse()` for the object), `.withResponse()` or
`.asResponse()` in TypeScript, `withRawResponse()` in Java. You can also replace the transport: Python takes an
`http_client` that must be an `httpx2` client (the SDK depends on `httpx2` and rejects a client from the older `httpx`
package with a `TypeError`), TypeScript takes a `fetch` function, and the Java SDK lets you implement its `HttpClient`
interface. The course's stand-in for the API is exactly that hook: the examples and practices run the real SDK against
a scripted transport.

## The practice: a raw client

The practice is the first column of the table above, written by hand. You build the request, send it through an
injected transport, and turn the reply into a message or an error, in Python, TypeScript, Java or Kotlin. The
statement, with the exact contract, is in
`exercises/13-one-rest-api-under-every-sdk/unit-01/practice-1/statement.md`; each language folder has a `starter`,
the `tests` and a `run.sh` or build file. The starter fails every test.

| Id | What it checks |
|---|---|
| `m1` | The request has the right method, URL, exactly three headers and a JSON body |
| `e1` | A blank or absent `system` is left out of the body |
| `e2` | Bad input is refused before the transport is called |
| `e3` | A good reply is parsed, and the text of the message joins its text blocks only |
| `e4` | An error reply becomes an error value; the header's request id wins over the body's |
| `e5` | A reply that is not JSON still gives an error value |
| `e6` | The API key never appears in an error |

Case `e6` is the operational habit of module 15 in miniature: an error message that is logged is an error message
that is read by people who must not see a key, and a server that echoes a rejected key back in its message is exactly
how a key reaches a log.

## Traps

1. **Assuming the SDK removed the need to understand HTTP.** It removed the typing, not the failure modes. Know the
   headers, the statuses and the request id.
2. **Treating the SDK's retries as a design.** Two retries with a short back-off is a default. Your timeout, your
   budget and your idempotency still need a decision.
3. **Logging requests at debug level in production.** Headers are partly redacted, bodies are not. Customer text
   does not belong in a log by default.

## Quiz

1. Verbose diagnostics are on in a deployed service. Credentials look masked in the output, but customers' messages show
   up in it. What should the team do?
   - **a**: Keep it on, because the SDK masks sensitive data in request and response bodies
   - **b**: Switch that mode off for live traffic and record request ids instead
   - **c**: Redact the headers further so that the output holds no credentials at all
   - **d**: Log only the failing calls so that less customer text reaches the output

2. A service sets nothing on its SDK client and calls the API in a month where the organisation has already reached
   its monthly spend cap. What happens to each call?
   - **a**: It is attempted once, because the SDK reads the cap and stops early
   - **b**: It is attempted once, because a 429 is never retried by the SDK
   - **c**: It is attempted three times, and the third attempt succeeds after a pause
   - **d**: It is attempted three times in all, and every attempt fails

<details>
<summary>Answer key</summary>

1. **b**. In that mode "all HTTP requests and responses are logged, including headers and bodies", so it does not belong on live traffic, and request ids are the safe handle for a support ticket. *a* is ruled out because "sensitive data in request and response bodies may still be visible". *c* is ruled out because "headers are partly redacted, bodies are not", and the leak is in the bodies. *d* is ruled out because failing calls carry customer text too, and "customer text does not belong in a log by default".
2. **d**. Two retries after the first attempt make three, and for a spend-cap 429 "every retry of it fails". *b* is ruled out because "certain errors are automatically retried 2 times by default". *c* is ruled out because the cap "stays in force until the next month begins", so no pause helps. *a* is ruled out because the retry default "applies to every retryable failure, including ones that cannot succeed", and the SDK does not inspect the cap.

</details>
