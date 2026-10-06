# One Messages API, three front doors

**Level:** Developer · **Module 22:** Claude on the cloud platforms · **Page 1 of 2**
**Exams:** DV2

**After this page you can** name the ways to reach Claude from a cloud account, write the request for the direct API, Amazon
Bedrock and Google Cloud, say where the model and the version go on each, and choose the authentication path that fits a security
policy.

Checked against the Claude API documentation (Claude in Amazon Bedrock, Claude on Google Cloud) on 2026-10-02, and by running the
example offline in the course container (`anthropic` 1.11.0, `@anthropic-ai/sdk` 0.131.0). Nothing in the example is signed or sent: the
replies are scripted, and the credentials a platform needs are described, not used.

## Why it matters

Many companies cannot call a third party's API directly. Their security rules, their billing and their data location are bound to
a cloud account, so they reach Claude through AWS or Google Cloud. The Messages API is close to the same everywhere, but the
differences are exactly the ones that break a program on the first call: the address, the model name, the place of the version, the
credentials. The exam asks which differences exist and what each platform lacks.

## The idea

### The doors

There are three doors you build requests for in this module. The **direct API** is Anthropic's own endpoint, with an API key.
**Claude in Amazon Bedrock** serves Claude on AWS-managed infrastructure; its page says it "runs on AWS-managed infrastructure with zero
operator access (Anthropic personnel have no access to the inference infrastructure)". **Claude on Google Cloud** serves Claude
through Google's Agent Platform; the SDKs and the example call this door Vertex. The docs mention a fourth, Claude Platform on AWS, described as "an
Anthropic-operated alternative on AWS with AWS Marketplace billing and typically same-day feature access". The practice and the
examples cover the first three.

On every door the request body is the Messages body of module 14, and the response keeps the shape of the direct API. A parser
written once works for all three. What changes is around the body.

### Bedrock

The endpoint follows a fixed pattern: "The endpoint follows the pattern `https://bedrock-mantle.{region}.api.aws/anthropic/v1/messages`."
It sends standard server-sent events for streaming "and the same request body shape as Anthropic's first-party API". Two things
differ from the direct call. The model id carries a prefix: "Model IDs in Claude in Amazon Bedrock carry an `anthropic.` provider
prefix", so `claude-opus-5-5` becomes `anthropic.claude-opus-5-5`. And the call is signed with AWS credentials instead of an API
key, using SigV4. The version header, `anthropic-version: 2023-06-01`, stays a header, as on the direct API.

Bedrock has its own access criteria per model, so a model must be enabled in the AWS account before it can be called. The older
Bedrock integration, based on `InvokeModel` and `Converse` with ARN-versioned ids, still exists on a separate page for Opus 4.6 and earlier;
this course uses the Messages endpoint above.

### Google Cloud

The Google Cloud page lists the two differences in two bullets:

> On Agent Platform, `model` is not passed in the request body. Instead, it is specified in the Google Cloud endpoint URL.
> On Agent Platform, `anthropic_version` is passed in the request body (rather than as a header), and must be set to the value `vertex-2023-10-16`.

Source: Claude on Google Cloud.

The call goes to a `rawPredict` URL of the form `https://aiplatform.googleapis.com/v1/projects/<project>/locations/global/publishers/anthropic/models/<model>:rawPredict`.
The model id there is the plain name, such as `claude-sonnet-5-5`, with a dated form for some models (`claude-haiku-4-5@20251001`).
The version is the body field `anthropic_version`, and the call carries a Google access token, for example from
`gcloud auth print-access-token`.

### Side by side

| | Direct API | Bedrock | Google Cloud |
|---|---|---|---|
| Address | `api.anthropic.com/v1/messages` | `bedrock-mantle.<region>.api.aws/anthropic/v1/messages` | `aiplatform.googleapis.com/v1/projects/<project>/locations/<endpoint>/publishers/anthropic/models/<model>:rawPredict` |
| Model | in the body | in the body, `anthropic.` prefix | in the URL |
| Version | header `anthropic-version` | header `anthropic-version` | body field `anthropic_version` |
| Credential | API key | AWS SigV4 or a bearer token | Google access token |

### Credentials on Bedrock

The Bedrock page gives three paths and a preference order. The first is the recommended one: "Use a Bedrock service role with AWS-managed
keys for the most secure, long-lived access". An administrator provisions the role and the developer passes it, so Bedrock assumes the role
on the caller's behalf. The second is an IAM assumed role, for federated access "with a 12-hour maximum session": the trust policy names
your identity provider, and the permissions policy grants `bedrock-mantle:CreateInference` only on the allowed model ARNs (module 23
writes such a policy). The third is a bearer token, "(12-hour maximum, least preferred)", minted with a token generator and sent in
the `x-api-key` header, where an administrator can deny long-term keys.

On Google Cloud, a developer runs `gcloud auth application-default login` or uses a service account, and the SDK adds the token.
Neither platform needs an Anthropic API key. Data handling follows the cloud: "Data handling for this offering is governed by
Amazon Bedrock", and Google Cloud's page says the same of Google.

### The example

The example builds the same question for the three doors, sends each through a scripted transport and prints what was sent: the
URL, the version header, the model and the body version. Then it parses the reply with one function.

<!-- example: m22-three-front-doors tabs: python,typescript,java,kotlin -->
```python
"""The same question sent to the direct API, to Claude in Amazon Bedrock and to Claude on Google Vertex AI.

Each request is written out by hand and sent through a scripted transport, so nothing leaves the container and nothing
is signed: AWS SigV4 (Bedrock), a Google access token (Vertex) or an API key (direct) is added by an SDK or a proxy. The
reply is the same illustrative, hand-written Messages response for all three, because the response body keeps the same shape.
"""
import logging
import httpx2

from harness import ScriptedTransport
from harness.scripted import message, text

log = logging.getLogger(__name__)

BASE = {"max_tokens": 64, "messages": [{"role": "user", "content": "Capital of France?"}]}
PROJECT, REGION = "example-project", "us-east-1"
DOORS = {
    "anthropic": ("https://api.anthropic.com/v1/messages",
                  {"anthropic-version": "2023-06-01", "content-type": "application/json"},
                  {"model": "claude-sonnet-5-5", **BASE}),
    "bedrock": (f"https://bedrock-mantle.{REGION}.api.aws/anthropic/v1/messages",
                {"anthropic-version": "2023-06-01", "content-type": "application/json"},
                {"model": "anthropic.claude-sonnet-5-5", **BASE}),
    "vertex": (f"https://aiplatform.googleapis.com/v1/projects/{PROJECT}/locations/global/publishers/anthropic/models/claude-sonnet-5-5:rawPredict",
               {"content-type": "application/json"},
               {"anthropic_version": "vertex-2023-10-16", **BASE}),
}


def send(name):
    url, headers, body = DOORS[name]
    transport = ScriptedTransport(message([text("Paris.")]))
    with httpx2.Client(transport=transport) as http:
        reply = http.post(url, headers=headers, json=body)
    return transport, reply


def main():
    for name in DOORS:
        transport, reply = send(name)
        sent, headers = transport.requests[0], transport.headers[0]
        print(f"{name:9} {transport.urls[0]}")
        print(f"          version header: {headers.get('anthropic-version', 'none')} | body model: {sent.get('model', 'none')} | body version: {sent.get('anthropic_version', 'none')}")
        print(f"          reply: {reply.status_code} {reply.json()['content'][0]['text']!r} (same parser for every door)")


if __name__ == "__main__":
    main()
```
```text
anthropic https://api.anthropic.com/v1/messages
          version header: 2023-06-01 | body model: claude-sonnet-5-5 | body version: none
          reply: 200 'Paris.' (same parser for every door)
bedrock   https://bedrock-mantle.us-east-1.api.aws/anthropic/v1/messages
          version header: 2023-06-01 | body model: anthropic.claude-sonnet-5-5 | body version: none
          reply: 200 'Paris.' (same parser for every door)
vertex    https://aiplatform.googleapis.com/v1/projects/example-project/locations/global/publishers/anthropic/models/claude-sonnet-5-5:rawPredict
          version header: none | body model: none | body version: vertex-2023-10-16
          reply: 200 'Paris.' (same parser for every door)
```
```typescript
// The same question sent to the direct API, to Claude in Amazon Bedrock and to Claude on Google Vertex AI.
// Each request is written out by hand and sent through a scripted fetch, so nothing leaves the container and nothing
// is signed: AWS SigV4 (Bedrock), a Google access token (Vertex) or an API key (direct) is added by an SDK or a proxy. The
// reply is the same illustrative, hand-written Messages response for all three, because the response body keeps the same shape.
import { message, scriptedFetch, text } from "../../../harness/ts/scriptedFetch.ts";
import { logger } from "./logger.ts";
const log = logger("front_doors");

const BASE = { max_tokens: 64, messages: [{ role: "user", content: "Capital of France?" }] };
const PROJECT = "example-project";
const REGION = "us-east-1";
export const DOORS: Record<string, [string, Record<string, string>, Record<string, unknown>]> = {
  anthropic: ["https://api.anthropic.com/v1/messages", { "anthropic-version": "2023-06-01", "content-type": "application/json" }, { model: "claude-sonnet-5-5", ...BASE }],
  bedrock: [`https://bedrock-mantle.${REGION}.api.aws/anthropic/v1/messages`, { "anthropic-version": "2023-06-01", "content-type": "application/json" }, { model: "anthropic.claude-sonnet-5-5", ...BASE }],
  vertex: [
    `https://aiplatform.googleapis.com/v1/projects/${PROJECT}/locations/global/publishers/anthropic/models/claude-sonnet-5-5:rawPredict`,
    { "content-type": "application/json" },
    { anthropic_version: "vertex-2023-10-16", ...BASE },
  ],
};

export async function send(name: string) {
  const [url, headers, body] = DOORS[name];
  const fake = scriptedFetch([{ body: message([text("Paris.")]) }]);
  const reply = await fake.fetch(url, { method: "POST", headers, body: JSON.stringify(body) });
  return { seen: fake.seen[0], status: reply.status, json: (await reply.json()) as any };
}

async function main() {
  for (const name of Object.keys(DOORS)) {
    const { seen, status, json } = await send(name);
    console.log(`${name.padEnd(9)} ${seen.url}`);
    console.log(`          version header: ${seen.headers["anthropic-version"] ?? "none"} | body model: ${seen.body.model ?? "none"} | body version: ${seen.body.anthropic_version ?? "none"}`);
    console.log(`          reply: ${status} '${json.content[0].text}' (same parser for every door)`);
  }
}

if (import.meta.main) await main();
```
```text
anthropic https://api.anthropic.com/v1/messages
          version header: 2023-06-01 | body model: claude-sonnet-5-5 | body version: none
          reply: 200 'Paris.' (same parser for every door)
bedrock   https://bedrock-mantle.us-east-1.api.aws/anthropic/v1/messages
          version header: 2023-06-01 | body model: anthropic.claude-sonnet-5-5 | body version: none
          reply: 200 'Paris.' (same parser for every door)
vertex    https://aiplatform.googleapis.com/v1/projects/example-project/locations/global/publishers/anthropic/models/claude-sonnet-5-5:rawPredict
          version header: none | body model: none | body version: vertex-2023-10-16
          reply: 200 'Paris.' (same parser for every door)
```
```java
import static harness.Scripted.map;
import static harness.Scripted.message;
import static harness.Scripted.text;

import com.fasterxml.jackson.databind.JsonNode;
import harness.Reply;
import harness.ScriptedHttp;
import harness.Scripted;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The same question sent to the direct API, to Claude in Amazon Bedrock and to Claude on Google Vertex AI.
 *
 * <p>Each request is written out by hand and sent through a scripted transport, so nothing leaves the container and nothing
 * is signed: AWS SigV4 (Bedrock), a Google access token (Vertex) or an API key (direct) is added by an SDK or a proxy. The
 * reply is the same illustrative, hand-written Messages response for all three, because the response body keeps the same shape.
 */
public final class FrontDoors {
    private static final System.Logger LOG = System.getLogger(FrontDoors.class.getName());
    static final List<Map<String, Object>> MESSAGES = List.of(map("role", "user", "content", "Capital of France?"));
    static final String PROJECT = "example-project", REGION = "us-east-1";

    /** One front door: where the request goes, its headers and its body. */
    record Door(String url, Map<String, String> headers, Map<String, Object> body) {}

    static final Map<String, Door> DOORS = new LinkedHashMap<>();

    static {
        DOORS.put("anthropic", new Door("https://api.anthropic.com/v1/messages",
            Map.of("anthropic-version", "2023-06-01", "content-type", "application/json"),
            map("model", "claude-sonnet-5-5", "max_tokens", 64, "messages", MESSAGES)));
        DOORS.put("bedrock", new Door("https://bedrock-mantle." + REGION + ".api.aws/anthropic/v1/messages",
            Map.of("anthropic-version", "2023-06-01", "content-type", "application/json"),
            map("model", "anthropic.claude-sonnet-5-5", "max_tokens", 64, "messages", MESSAGES)));
        DOORS.put("vertex", new Door("https://aiplatform.googleapis.com/v1/projects/" + PROJECT + "/locations/global/publishers/anthropic/models/claude-sonnet-5-5:rawPredict",
            Map.of("content-type", "application/json"),
            map("anthropic_version", "vertex-2023-10-16", "max_tokens", 64, "messages", MESSAGES)));
    }

    /** What went out and what came back for one door. */
    record Sent(ScriptedHttp transport, Reply reply) {}

    static Sent send(String name) {
        Door door = DOORS.get(name);
        ScriptedHttp transport = Scripted.http(message(List.of(text("Paris."))));
        Reply reply = transport.send("POST", door.url(), door.headers(), door.body());
        return new Sent(transport, reply);
    }

    static String orNone(JsonNode node, String field) {
        return node.has(field) ? node.get(field).asText() : "none";
    }

    public static void main(String[] args) {
        for (String name : DOORS.keySet()) {
            Sent s = send(name);
            JsonNode sent = s.transport().requests.get(0);
            Map<String, String> headers = s.transport().headers.get(0);
            System.out.println(String.format("%-9s %s", name, s.transport().urls.get(0)));
            System.out.println("          version header: " + headers.getOrDefault("anthropic-version", "none") + " | body model: " + orNone(sent, "model")
                + " | body version: " + orNone(sent, "anthropic_version"));
            System.out.println("          reply: " + s.reply().status() + " '" + s.reply().json().at("/content/0/text").asText() + "' (same parser for every door)");
        }
    }
}
```
```text
anthropic https://api.anthropic.com/v1/messages
          version header: 2023-06-01 | body model: claude-sonnet-5-5 | body version: none
          reply: 200 'Paris.' (same parser for every door)
bedrock   https://bedrock-mantle.us-east-1.api.aws/anthropic/v1/messages
          version header: 2023-06-01 | body model: anthropic.claude-sonnet-5-5 | body version: none
          reply: 200 'Paris.' (same parser for every door)
vertex    https://aiplatform.googleapis.com/v1/projects/example-project/locations/global/publishers/anthropic/models/claude-sonnet-5-5:rawPredict
          version header: none | body model: none | body version: vertex-2023-10-16
          reply: 200 'Paris.' (same parser for every door)
```
```kotlin
import com.fasterxml.jackson.databind.JsonNode
import harness.Reply
import harness.Scripted
import harness.Scripted.map
import harness.Scripted.message
import harness.Scripted.text
import harness.ScriptedHttp

private val log = System.getLogger("front_doors")

/**
 * The same question sent to the direct API, to Claude in Amazon Bedrock and to Claude on Google Vertex AI.
 *
 * Each request is written out by hand and sent through a scripted transport, so nothing leaves the container and nothing
 * is signed: AWS SigV4 (Bedrock), a Google access token (Vertex) or an API key (direct) is added by an SDK or a proxy. The
 * reply is the same illustrative, hand-written Messages response for all three, because the response body keeps the same shape.
 */
val MESSAGES = listOf(map("role", "user", "content", "Capital of France?"))
const val PROJECT = "example-project"
const val REGION = "us-east-1"

/** One front door: where the request goes, its headers and its body. */
data class Door(val url: String, val headers: Map<String, String>, val body: Map<String, Any?>)

val DOORS = linkedMapOf(
    "anthropic" to Door(
        "https://api.anthropic.com/v1/messages",
        mapOf("anthropic-version" to "2023-06-01", "content-type" to "application/json"),
        map("model", "claude-sonnet-5-5", "max_tokens", 64, "messages", MESSAGES),
    ),
    "bedrock" to Door(
        "https://bedrock-mantle.$REGION.api.aws/anthropic/v1/messages",
        mapOf("anthropic-version" to "2023-06-01", "content-type" to "application/json"),
        map("model", "anthropic.claude-sonnet-5-5", "max_tokens", 64, "messages", MESSAGES),
    ),
    "vertex" to Door(
        "https://aiplatform.googleapis.com/v1/projects/$PROJECT/locations/global/publishers/anthropic/models/claude-sonnet-5-5:rawPredict",
        mapOf("content-type" to "application/json"),
        map("anthropic_version", "vertex-2023-10-16", "max_tokens", 64, "messages", MESSAGES),
    ),
)

/** What went out and what came back for one door. */
data class Sent(val transport: ScriptedHttp, val reply: Reply)

fun send(name: String): Sent {
    val door = DOORS.getValue(name)
    val transport = Scripted.http(message(listOf(text("Paris."))))
    return Sent(transport, transport.send("POST", door.url, door.headers, door.body))
}

fun orNone(node: JsonNode, field: String): String = if (node.has(field)) node[field].asText() else "none"

fun main() {
    for (name in DOORS.keys) {
        val (transport, reply) = send(name)
        val sent = transport.requests[0]
        println("${name.padEnd(9)} ${transport.urls[0]}")
        println("          version header: ${transport.headers[0]["anthropic-version"] ?: "none"} | body model: ${orNone(sent, "model")} | body version: ${orNone(sent, "anthropic_version")}")
        println("          reply: ${reply.status()} '${reply.json().at("/content/0/text").asText()}' (same parser for every door)")
    }
}
```
```text
anthropic https://api.anthropic.com/v1/messages
          version header: 2023-06-01 | body model: claude-sonnet-5-5 | body version: none
          reply: 200 'Paris.' (same parser for every door)
bedrock   https://bedrock-mantle.us-east-1.api.aws/anthropic/v1/messages
          version header: 2023-06-01 | body model: anthropic.claude-sonnet-5-5 | body version: none
          reply: 200 'Paris.' (same parser for every door)
vertex    https://aiplatform.googleapis.com/v1/projects/example-project/locations/global/publishers/anthropic/models/claude-sonnet-5-5:rawPredict
          version header: none | body model: none | body version: vertex-2023-10-16
          reply: 200 'Paris.' (same parser for every door)
```
<!-- /example -->

Read the output. All three replies parse as 200 with the text "Paris.". The Bedrock row shows the model `anthropic.claude-sonnet-5-5`
in the body and the version in the header. The Vertex row shows no version header and no body model, but a body version
`vertex-2023-10-16`, and the model sits in the URL before `:rawPredict`. The example's project name is a placeholder.

## Traps

1. **Sending the direct-API request body to Vertex unchanged.** The model in the body and the version header are both wrong
   there. Vertex wants the model in the URL and `anthropic_version` in the body.
2. **Using the direct model id on Bedrock.** The id needs the `anthropic.` prefix, and the version header stays.
3. **Reaching for an API key on a cloud platform.** The cloud's own credentials sign the call. A key that works on the direct API
   does nothing here.

## Quiz

1. A team moves a working direct-API request to Google Cloud, swaps the API key for a Google access token and keeps the body
   unchanged. It is refused. Which changes does the page require?
   - **a**: Prefix the model with the provider name and keep the version as a header
   - **b**: Keep the model in the payload and move the version into the payload too
   - **c**: Move both the model and the version into the query string of the address
   - **d**: Name the model in the address and carry the version in the payload

2. A developer calls Claude Opus 5.5 through Bedrock with the identifier `claude-opus-5-5` and gets an error. What fixes it?
   - **a**: Keep the id in the body and put the anthropic. prefix in front of it
   - **b**: Use the dated id that the direct API lists for this same model
   - **c**: Use the Google Cloud form of the id, with an at sign and a date
   - **d**: Move the id into the endpoint address and leave it out of the body

3. A security team wants no long-lived keys on developer laptops when calling Claude through Bedrock. Which path does the page
   favour?
   - **a**: An Anthropic API key per developer, rotated by the security team monthly
   - **b**: Bearer tokens kept in each laptop's configuration file for daily use
   - **c**: A provisioned service role that the platform assumes for the caller
   - **d**: An assumed role with a session that lasts for a full working week

<details>
<summary>Answer key</summary>

1. **d**. The page says "`model` is not passed in the request body" and that it "is specified in the Google Cloud endpoint URL", while `anthropic_version` goes "in the request body". *b* is ruled out because moving the version is right but the model must leave the body: "Vertex wants the model in the URL". *c* is ruled out because the model sits in the address path, "before `:rawPredict`", and "The version is the body field" `anthropic_version`, not a query parameter. *a* is ruled out because "The model id there is the plain name", with no prefix, and the version is passed "rather than as a header".
2. **a**. The page says "Model IDs in Claude in Amazon Bedrock carry an `anthropic.` provider prefix", and the model stays in the body. *b* is ruled out because the id gains a prefix, not a date: "so `claude-opus-5-5` becomes `anthropic.claude-opus-5-5`". *c* is ruled out because the at-sign form is the Google Cloud one, "with a dated form for some models", and there the model sits in the URL. *d* is ruled out because the model stays in the body on Bedrock, "the same request body shape as Anthropic's first-party API".
3. **c**. The page marks the service role as the recommended path: "Use a Bedrock service role with AWS-managed keys for the most secure, long-lived access". *b* is ruled out because bearer tokens are "(12-hour maximum, least preferred)". *a* is ruled out because "Neither platform needs an Anthropic API key", however often it is rotated. *d* is ruled out because an assumed role has "a 12-hour maximum session", not a week.

</details>
