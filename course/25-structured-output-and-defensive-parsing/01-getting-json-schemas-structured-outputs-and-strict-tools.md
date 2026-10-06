# Getting JSON: schemas, structured outputs and strict tools

**Level:** Developer · **Module 25:** Structured output and defensive parsing · **Page 1 of 2**
**Exams:** DV1, DV4; A4.3, A4.4

**After this page you can** choose between asking for JSON in a prompt, structured outputs and strict tool use, write a schema the
API accepts, say which constraints the API cannot enforce and your program must, and name the two replies whose body does not follow
the schema.

Checked against the Claude API documentation (Structured outputs, and Define tools) on 2026-10-03, and by running the example
offline in the course container (`anthropic` 1.11.0, `@anthropic-ai/sdk` 0.131.0). The replies in the example are illustrative,
scripted bodies in the shape of the Messages API, not captures.

## Why it matters

An application that reads a model's reply with `json.loads` is one odd reply away from a crash. Anthropic offers two ways to make the
shape reliable, and the exam asks which one fits a task, what each one cannot promise and what the program still has to check. The
second page covers the code that handles what the schema cannot promise; this page covers the request.

## The idea

### Four ways to ask for JSON

| Way | How | What it gives | Course note |
|---|---|---|---|
| Ask in the prompt | words and an example in the prompt | a reply that usually follows the format | no guarantee; the program must parse defensively |
| Structured outputs | `output_config.format` with `type: "json_schema"` | the reply text is JSON that follows the schema | controls "what Claude says" |
| Strict tool use | `strict: true` on a tool definition | the `input` of a tool call follows the tool's schema | validates "how Claude calls your functions" |
| A forced tool | `tool_choice` of `any` or `tool` | one of your tools is called | not accepted by every model, see below |

The documentation puts the split in one sentence: "JSON outputs control Claude's response format (what Claude says)" while "Strict tool
use validates tool parameters (how Claude calls your functions)." The two "solve different problems and work together". A task
that must return a record, such as an invoice, uses structured outputs. A task that lets the model act uses tools, and strict
tools make the call arguments safe to pass on.

The request carries the schema in `output_config.format`, as the documentation says: "The request carries the schema in
`output_config.format` with `type: "json_schema"`." The example below sends it on every call.

### Forced tools are not always available

Forcing a tool used to be the usual trick for getting JSON: define a tool whose schema is the shape you want, set `tool_choice` to
that tool and read the `input`. On some models it no longer works. The documentation says that for "Claude Opus 5.5, Claude Sonnet
5.5, Claude Fable 5.1, and Claude Mythos 5.1", `any` and `tool` "return a 400 error". Its advice for these models is `auto` with
strict tool use to guarantee schema-valid tool inputs, or structured outputs "when you need a response in a fixed JSON shape". The
values `auto` and `none` keep working. The next module covers `tool_choice` in full.

### What a schema may contain

The API accepts a subset of JSON Schema. The documentation lists what it supports: "All basic types: object, array, string,
integer, number, boolean, null", `enum` for strings, numbers, booleans or nulls, `const`, `anyOf` and `allOf` with limits, local
`$ref`, `required`, string formats such as `date` and `email`, and an array `minItems` of 0 or 1. Every object must set
`additionalProperties` to `false`.

It does not support "Recursive schemas", "Numerical constraints (such as `minimum`, `maximum`, `multipleOf`)", "String constraints
(`minLength`, `maxLength`)" or arrays with a `minItems` above 1. An unsupported feature is not ignored: "If you use an unsupported
feature, you'll receive a 400 error with details." A schema can also be too big: the documentation gives limits of 20 strict tools,
24 optional parameters and 16 parameters with union types per request, "combined total across all strict schemas in a single
request", and a larger schema fails with "Schema is too complex for compilation."

### The limits move into your program

A rule such as "the total is at least 0" cannot go in the schema. Most SDK helpers deal with it in a list of five steps, and the second one is
the one to remember. Among them they "Remove unsupported constraints", "Update descriptions by adding each unsupported constraint to the field's
description" and then validate the reply against the original schema "if the helper validates responses". The documentation sums it
up: "Claude receives a simplified schema, but a helper that validates responses still enforces every constraint in your code."

The example does the same by hand. A function `for_api` takes the full schema, drops `minimum` from every property and writes it in the
description, so the model still sees "(minimum 0)"; the program keeps the full schema and checks the value after the reply.

### Capital letters in an enum

An enum is not safe from a small change. "Structured outputs don't guarantee the capitalization of string `enum` and `const` values:
Claude may return a value that differs from your schema only in capitalization, typically in the first letter of a word following
a space." The response completes normally, "with no error and no special `stop_reason`", and this holds for strict tool use as well.
The documentation's advice is to "Compare enum values case-insensitively, and avoid enum values that differ only in
capitalization." The example's `normalise_enum` maps `Eur` back to `EUR` before it validates.

### Two replies that do not follow the schema

A guarantee about the shape of the reply has two exceptions, both visible in `stop_reason`.

- **`refusal`.** "If Claude refuses a request for safety reasons", the response has `stop_reason: "refusal"`, a 200 status code and
  billed tokens, and "The output may not match your schema because the refusal message takes precedence over schema constraints."
- **`max_tokens`.** When the reply is cut off at the limit, "The output may be incomplete and not match your schema". The documented
  remedy is to "Retry with a higher `max_tokens` value to get the complete structured output."

Read `stop_reason` before the body. A refusal is a result to report, not a malformed reply to repeat. The practice on the next page
makes this its fourth case.

### First request and the cache

The first use of a schema is slower. "The first time you use a specific schema, there is additional latency while the grammar
compiles", and "Compiled grammars are cached for 24 hours from last use". The cache is invalidated when the schema's structure
changes or the set of tools changes, and "Changing only `name` or `description` fields does not invalidate the cache." A schema that
changes on every request therefore never benefits from it.

### The example

The example sends one invoice document through structured outputs five times, with scripted replies. It sends the schema without
`minimum`, repairs the enum's capital letters, rejects an `evidence` quotation that is not in the document and asks again, and
stops at a refusal and at a cut-off reply without a second request.

<!-- example: m25-structured-extraction tabs: python,typescript,java,kotlin -->
```python
"""Structured outputs plus the checks a schema cannot make, against a scripted model.

The replies are illustrative, hand-written bodies in the shape of the Messages API (claude-sonnet-5-5), not captures.
The API gets a schema without numeric constraints (structured outputs do not support them); the program enforces them.
"""
import logging
import copy
import json

from harness import scripted_client
from harness.scripted import message, text

log = logging.getLogger(__name__)

MODEL = "claude-sonnet-5-5"
LOCAL_SCHEMA = {
    "type": "object",
    "properties": {
        "vendor": {"type": "string"},
        "total": {"type": "number", "minimum": 0},
        "currency": {"type": "string", "enum": ["USD", "EUR", "GBP"]},
        "evidence": {"type": "string"},
    },
    "required": ["vendor", "total", "currency", "evidence"],
    "additionalProperties": False,
}
UNSUPPORTED = ("minimum", "maximum", "multipleOf", "minLength", "maxLength")


def for_api(schema):
    """The schema without the constraints that structured outputs reject; they move to the field's description."""
    out = copy.deepcopy(schema)

    def walk(node):
        notes = [f"{k} {node.pop(k)}" for k in UNSUPPORTED if k in node]
        if notes:
            node["description"] = (node.get("description", "") + " (" + ", ".join(notes) + ")").strip()
        for sub in node.get("properties", {}).values():
            walk(sub)
    walk(out)
    return out


def problems(value, document):
    """What the API cannot promise: numeric limits, the enum's capital letters, and a quotation that is really in the document."""
    found = []
    if not isinstance(value.get("total"), (int, float)) or value["total"] < 0:
        found.append("$.total: must be a number of at least 0")
    if value.get("currency") not in LOCAL_SCHEMA["properties"]["currency"]["enum"]:
        found.append(f"$.currency: must be one of USD, EUR, GBP, not {value.get('currency')!r}")
    if value.get("evidence") not in document:
        found.append("$.evidence: is not found in the document")
    return found


def normalise_enum(value):
    """Structured outputs may change the capital letters of an enum value; compare without them."""
    allowed = {e.lower(): e for e in LOCAL_SCHEMA["properties"]["currency"]["enum"]}
    if isinstance(value.get("currency"), str) and value["currency"].lower() in allowed:
        value = {**value, "currency": allowed[value["currency"].lower()]}
    return value


def extract(client, document, max_attempts=2):
    messages = [{"role": "user", "content": f"Extract the invoice data.\n<document>\n{document}\n</document>"}]
    for attempt in range(1, max_attempts + 1):
        reply = client.messages.create(model=MODEL, max_tokens=300, messages=messages,
                                       output_config={"format": {"type": "json_schema", "schema": for_api(LOCAL_SCHEMA)}})
        if reply.stop_reason in ("refusal", "max_tokens"):
            return {"status": "refused" if reply.stop_reason == "refusal" else "truncated", "attempts": attempt}
        raw = reply.content[0].text
        value = normalise_enum(json.loads(raw))
        errors = problems(value, document)
        if not errors:
            return {"status": "ok", "attempts": attempt, "value": value}
        messages += [{"role": "assistant", "content": raw}, {"role": "user", "content": "Rejected:\n" + "\n".join(errors) + "\nReturn corrected JSON."}]
    return {"status": "failed", "attempts": max_attempts, "errors": errors}


DOC = "Invoice from Acme Tools. Total due: 120.50 EUR. Thank you for your business."


def body(**fields):
    return json.dumps({"vendor": "Acme Tools", "total": 120.5, "currency": "EUR", "evidence": "Total due: 120.50 EUR", **fields})


REPLIES = [
    message([text(body(currency="Eur"))], model=MODEL),
    message([text(body(evidence="Total due: 999.00 USD"))], model=MODEL),
    message([text(body())], model=MODEL),
    message([text("I can't help with that.")], stop_reason="refusal", model=MODEL),
    message([text('{"vendor": "Acme')], stop_reason="max_tokens", model=MODEL),
]


def main():
    client, transport = scripted_client(*REPLIES)
    print("schema sent to the API:", json.dumps(for_api(LOCAL_SCHEMA)["properties"]["total"], separators=(",", ":")))
    for n in (1, 2):
        print(f"document {n}:", extract(client, DOC))
    print("document 3:", extract(client, DOC))
    print("document 4:", extract(client, DOC))
    print("requests sent:", len(transport.requests), "| each carried output_config.format.type:", {r["output_config"]["format"]["type"] for r in transport.requests})
    print("second call of document 2 sent:", [m["role"] for m in transport.requests[2]["messages"]], "| feedback:", transport.requests[2]["messages"][-1]["content"].splitlines()[1])


if __name__ == "__main__":
    main()
```
```text
schema sent to the API: {"type":"number","description":"(minimum 0)"}
document 1: {'status': 'ok', 'attempts': 1, 'value': {'vendor': 'Acme Tools', 'total': 120.5, 'currency': 'EUR', 'evidence': 'Total due: 120.50 EUR'}}
document 2: {'status': 'ok', 'attempts': 2, 'value': {'vendor': 'Acme Tools', 'total': 120.5, 'currency': 'EUR', 'evidence': 'Total due: 120.50 EUR'}}
document 3: {'status': 'refused', 'attempts': 1}
document 4: {'status': 'truncated', 'attempts': 1}
requests sent: 5 | each carried output_config.format.type: {'json_schema'}
second call of document 2 sent: ['user', 'assistant', 'user'] | feedback: $.evidence: is not found in the document
```
```typescript
// Structured outputs plus the checks a schema cannot make, against a scripted model.
// The replies are illustrative, hand-written bodies in the shape of the Messages API (claude-sonnet-5-5), not captures.
// The API gets a schema without numeric constraints (structured outputs do not support them); the program enforces them.
import Anthropic from "@anthropic-ai/sdk";
import { message, scriptedFetch, text } from "../../../harness/ts/scriptedFetch.ts";
import { logger } from "./logger.ts";
const log = logger("structured_extraction");

export const MODEL = "claude-sonnet-5-5";
export const LOCAL_SCHEMA: any = {
  type: "object",
  properties: {
    vendor: { type: "string" },
    total: { type: "number", minimum: 0 },
    currency: { type: "string", enum: ["USD", "EUR", "GBP"] },
    evidence: { type: "string" },
  },
  required: ["vendor", "total", "currency", "evidence"],
  additionalProperties: false,
};
const UNSUPPORTED = ["minimum", "maximum", "multipleOf", "minLength", "maxLength"];

/** The schema without the constraints that structured outputs reject; they move to the field's description. */
export function forApi(schema: any): any {
  const out = structuredClone(schema);
  const walk = (node: any) => {
    const notes = UNSUPPORTED.filter((k) => k in node).map((k) => {
      const note = `${k} ${node[k]}`;
      delete node[k];
      return note;
    });
    if (notes.length) node.description = `${node.description ?? ""} (${notes.join(", ")})`.trim();
    for (const sub of Object.values(node.properties ?? {})) walk(sub);
  };
  walk(out);
  return out;
}

/** What the API cannot promise: numeric limits, the enum's capital letters, and a quotation that is really in the document. */
export function problems(value: any, document: string): string[] {
  const found: string[] = [];
  if (typeof value.total !== "number" || value.total < 0) found.push("$.total: must be a number of at least 0");
  if (!LOCAL_SCHEMA.properties.currency.enum.includes(value.currency)) found.push(`$.currency: must be one of USD, EUR, GBP, not ${JSON.stringify(value.currency)}`);
  if (!document.includes(value.evidence)) found.push("$.evidence: is not found in the document");
  return found;
}

/** Structured outputs may change the capital letters of an enum value; compare without them. */
export function normaliseEnum(value: any): any {
  const allowed = new Map<string, string>((LOCAL_SCHEMA.properties.currency.enum as string[]).map((e) => [e.toLowerCase(), e]));
  if (typeof value.currency === "string" && allowed.has(value.currency.toLowerCase())) return { ...value, currency: allowed.get(value.currency.toLowerCase()) };
  return value;
}

export async function extract(client: Anthropic, document: string, maxAttempts = 2): Promise<Record<string, unknown>> {
  const messages: Anthropic.MessageParam[] = [{ role: "user", content: `Extract the invoice data.\n<document>\n${document}\n</document>` }];
  let errors: string[] = [];
  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    const reply = await client.messages.create({ model: MODEL, max_tokens: 300, messages, output_config: { format: { type: "json_schema", schema: forApi(LOCAL_SCHEMA) } } });
    if (reply.stop_reason === "refusal" || reply.stop_reason === "max_tokens") return { status: reply.stop_reason === "refusal" ? "refused" : "truncated", attempts: attempt };
    const raw = (reply.content[0] as { text: string }).text;
    const value = normaliseEnum(JSON.parse(raw));
    errors = problems(value, document);
    if (errors.length === 0) return { status: "ok", attempts: attempt, value };
    messages.push({ role: "assistant", content: raw }, { role: "user", content: `Rejected:\n${errors.join("\n")}\nReturn corrected JSON.` });
  }
  return { status: "failed", attempts: maxAttempts, errors };
}

export const DOC = "Invoice from Acme Tools. Total due: 120.50 EUR. Thank you for your business.";

export const body = (fields: Record<string, unknown> = {}) => JSON.stringify({ vendor: "Acme Tools", total: 120.5, currency: "EUR", evidence: "Total due: 120.50 EUR", ...fields });

const usage = { input_tokens: 1, output_tokens: 1 };
export const REPLIES = () => [
  { body: message([text(body({ currency: "Eur" }))], "end_turn", usage, MODEL) },
  { body: message([text(body({ evidence: "Total due: 999.00 USD" }))], "end_turn", usage, MODEL) },
  { body: message([text(body())], "end_turn", usage, MODEL) },
  { body: message([text("I can't help with that.")], "refusal", usage, MODEL) },
  { body: message([text('{"vendor": "Acme')], "max_tokens", usage, MODEL) },
];

export function clientFor(replies: Array<Record<string, unknown>>) {
  const fake = scriptedFetch(replies as any);
  return { fake, client: new Anthropic({ apiKey: "placeholder", maxRetries: 0, fetch: fake.fetch }) };
}

/** Python's repr of a result dict, so that both languages print the same line. */
const show = (r: Record<string, unknown>) => `{${Object.entries(r).map(([k, v]) => `'${k}': ${typeof v === "object" ? pyRepr(v) : typeof v === "string" ? `'${v}'` : v}`).join(", ")}}`;
function pyRepr(v: any): string {
  if (Array.isArray(v)) return `[${v.map(pyRepr).join(", ")}]`;
  if (v && typeof v === "object") return `{${Object.entries(v).map(([k, x]) => `'${k}': ${pyRepr(x)}`).join(", ")}}`;
  return typeof v === "string" ? `'${v}'` : String(v);
}

async function main() {
  const { fake, client } = clientFor(REPLIES());
  console.log("schema sent to the API:", JSON.stringify(forApi(LOCAL_SCHEMA).properties.total));
  for (const n of [1, 2]) console.log(`document ${n}:`, show(await extract(client, DOC)));
  console.log("document 3:", show(await extract(client, DOC)));
  console.log("document 4:", show(await extract(client, DOC)));
  console.log("requests sent:", fake.seen.length, "| each carried output_config.format.type:", `{'${[...new Set(fake.seen.map((r) => r.body.output_config.format.type))].join("', '")}'}`);
  console.log("second call of document 2 sent:", pyRepr(fake.seen[2].body.messages.map((m: any) => m.role)), "| feedback:", fake.seen[2].body.messages.at(-1).content.split("\n")[1]);
}

if (import.meta.main) await main();
```
```text
schema sent to the API: {"type":"number","description":"(minimum 0)"}
document 1: {'status': 'ok', 'attempts': 1, 'value': {'vendor': 'Acme Tools', 'total': 120.5, 'currency': 'EUR', 'evidence': 'Total due: 120.50 EUR'}}
document 2: {'status': 'ok', 'attempts': 2, 'value': {'vendor': 'Acme Tools', 'total': 120.5, 'currency': 'EUR', 'evidence': 'Total due: 120.50 EUR'}}
document 3: {'status': 'refused', 'attempts': 1}
document 4: {'status': 'truncated', 'attempts': 1}
requests sent: 5 | each carried output_config.format.type: {'json_schema'}
second call of document 2 sent: ['user', 'assistant', 'user'] | feedback: $.evidence: is not found in the document
```
```java
import static harness.Scripted.map;
import static harness.Scripted.message;
import static harness.Scripted.text;
import static harness.Show.py;

import com.anthropic.client.AnthropicClient;
import com.anthropic.core.JsonValue;
import com.anthropic.core.ObjectMappers;
import com.anthropic.models.messages.JsonOutputFormat;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.anthropic.models.messages.OutputConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import harness.Scripted;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Structured outputs plus the checks a schema cannot make, against a scripted model.
 *
 * <p>The replies are illustrative, hand-written bodies in the shape of the Messages API (claude-sonnet-5-5), not captures.
 * The API gets a schema without numeric constraints (structured outputs do not support them); the program enforces them.
 */
public final class StructuredExtraction {
    private static final System.Logger LOG = System.getLogger(StructuredExtraction.class.getName());
    static final String MODEL = "claude-sonnet-5-5";
    private static final ObjectMapper JSON = new ObjectMapper();
    static final JsonNode LOCAL_SCHEMA = Scripted.tree("""
        {"type": "object",
         "properties": {"vendor": {"type": "string"}, "total": {"type": "number", "minimum": 0},
                        "currency": {"type": "string", "enum": ["USD", "EUR", "GBP"]}, "evidence": {"type": "string"}},
         "required": ["vendor", "total", "currency", "evidence"],
         "additionalProperties": false}""");
    static final List<String> UNSUPPORTED = List.of("minimum", "maximum", "multipleOf", "minLength", "maxLength");
    static final List<String> CURRENCIES = List.of("USD", "EUR", "GBP");

    /** The schema without the constraints that structured outputs reject; they move to the field's description. */
    static ObjectNode forApi(JsonNode schema) {
        ObjectNode out = schema.deepCopy();
        walk(out);
        return out;
    }

    private static void walk(ObjectNode node) {
        List<String> notes = new ArrayList<>();
        for (String k : UNSUPPORTED) if (node.has(k)) notes.add(k + " " + node.remove(k).asText());
        if (!notes.isEmpty()) node.put("description", ((node.has("description") ? node.get("description").asText() : "") + " (" + String.join(", ", notes) + ")").strip());
        if (node.has("properties")) node.get("properties").forEach(sub -> walk((ObjectNode) sub));
    }

    /** What the API cannot promise: numeric limits, the enum's capital letters, and a quotation that is really in the document. */
    static List<String> problems(Map<String, Object> value, String document) {
        List<String> found = new ArrayList<>();
        if (!(value.get("total") instanceof Number n) || n.doubleValue() < 0) found.add("$.total: must be a number of at least 0");
        if (!CURRENCIES.contains(value.get("currency"))) found.add("$.currency: must be one of USD, EUR, GBP, not " + py(value.get("currency")));
        if (!(value.get("evidence") instanceof String e) || !document.contains(e)) found.add("$.evidence: is not found in the document");
        return found;
    }

    /** Structured outputs may change the capital letters of an enum value; compare without them. */
    static Map<String, Object> normaliseEnum(Map<String, Object> value) {
        if (value.get("currency") instanceof String c) {
            for (String allowed : CURRENCIES) {
                if (allowed.equalsIgnoreCase(c)) {
                    Map<String, Object> out = new LinkedHashMap<>(value);
                    out.put("currency", allowed);
                    return out;
                }
            }
        }
        return value;
    }

    private static OutputConfig outputConfig() {
        Map<String, JsonValue> schema = new LinkedHashMap<>();
        forApi(LOCAL_SCHEMA).fields().forEachRemaining(e -> schema.put(e.getKey(), JsonValue.from(JSON.convertValue(e.getValue(), Object.class))));
        return OutputConfig.builder().format(JsonOutputFormat.builder().schema(JsonOutputFormat.Schema.builder().additionalProperties(schema).build()).build()).build();
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> extract(AnthropicClient client, String document, int maxAttempts) {
        MessageCreateParams.Builder messages = MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(300).outputConfig(outputConfig())
            .addUserMessage("Extract the invoice data.\n<document>\n" + document + "\n</document>");
        List<String> errors = List.of();
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            Message reply = client.messages().create(messages.build());
            String stop = reply.stopReason().get().asString();
            if (stop.equals("refusal") || stop.equals("max_tokens")) {
                return map("status", stop.equals("refusal") ? "refused" : "truncated", "attempts", attempt);
            }
            String raw = reply.content().get(0).asText().text();
            Map<String, Object> value;
            try {
                value = normaliseEnum(JSON.readValue(raw, LinkedHashMap.class));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            errors = problems(value, document);
            if (errors.isEmpty()) return map("status", "ok", "attempts", attempt, "value", value);
            messages.addAssistantMessage(raw).addUserMessage("Rejected:\n" + String.join("\n", errors) + "\nReturn corrected JSON.");
        }
        return map("status", "failed", "attempts", maxAttempts, "errors", errors);
    }

    static Map<String, Object> extract(AnthropicClient client, String document) {
        return extract(client, document, 2);
    }

    static final String DOC = "Invoice from Acme Tools. Total due: 120.50 EUR. Thank you for your business.";

    static String body(Map<String, Object> overrides) {
        Map<String, Object> fields = map("vendor", "Acme Tools", "total", 120.5, "currency", "EUR", "evidence", "Total due: 120.50 EUR");
        fields.putAll(overrides);
        try {
            return JSON.writeValueAsString(fields);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static final List<Object> REPLIES = List.of(
        message(List.of(text(body(map("currency", "Eur"))))),
        message(List.of(text(body(map("evidence", "Total due: 999.00 USD"))))),
        message(List.of(text(body(map())))),
        message(List.of(text("I can't help with that.")), "refusal"),
        message(List.of(text("{\"vendor\": \"Acme")), "max_tokens"));

    public static void main(String[] args) {
        Scripted.Rig rig = Scripted.client(REPLIES.toArray());
        System.out.println("schema sent to the API: " + forApi(LOCAL_SCHEMA).get("properties").get("total"));
        for (int n : new int[] {1, 2}) System.out.println("document " + n + ": " + py(extract(rig.client(), DOC)));
        System.out.println("document 3: " + py(extract(rig.client(), DOC)));
        System.out.println("document 4: " + py(extract(rig.client(), DOC)));
        List<JsonNode> sent = rig.http().requests;
        System.out.println("requests sent: " + sent.size() + " | each carried output_config.format.type: "
            + sent.stream().map(r -> "'" + r.at("/output_config/format/type").asText() + "'").distinct().collect(Collectors.joining(", ", "{", "}")));
        JsonNode third = sent.get(2).get("messages");
        List<String> roles = new ArrayList<>();
        third.forEach(m -> roles.add(m.get("role").asText()));
        System.out.println("second call of document 2 sent: " + py(roles) + " | feedback: " + third.get(third.size() - 1).get("content").asText().split("\n")[1]);
    }
}
```
```text
schema sent to the API: {"type":"number","description":"(minimum 0)"}
document 1: {'status': 'ok', 'attempts': 1, 'value': {'vendor': 'Acme Tools', 'total': 120.5, 'currency': 'EUR', 'evidence': 'Total due: 120.50 EUR'}}
document 2: {'status': 'ok', 'attempts': 2, 'value': {'vendor': 'Acme Tools', 'total': 120.5, 'currency': 'EUR', 'evidence': 'Total due: 120.50 EUR'}}
document 3: {'status': 'refused', 'attempts': 1}
document 4: {'status': 'truncated', 'attempts': 1}
requests sent: 5 | each carried output_config.format.type: {'json_schema'}
second call of document 2 sent: ['user', 'assistant', 'user'] | feedback: $.evidence: is not found in the document
```
```kotlin
import com.anthropic.client.AnthropicClient
import com.anthropic.core.JsonValue
import com.anthropic.models.messages.JsonOutputFormat
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.Model
import com.anthropic.models.messages.OutputConfig
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import harness.Scripted
import harness.Scripted.map
import harness.Scripted.message
import harness.Scripted.text
import harness.Show.py

private val log = System.getLogger("structured_extraction")

/**
 * Structured outputs plus the checks a schema cannot make, against a scripted model.
 *
 * The replies are illustrative, hand-written bodies in the shape of the Messages API (claude-sonnet-5-5), not captures.
 * The API gets a schema without numeric constraints (structured outputs do not support them); the program enforces them.
 */
const val MODEL = "claude-sonnet-5-5"
private val JSON = ObjectMapper()
val LOCAL_SCHEMA: JsonNode = Scripted.tree(
    """
    {"type": "object",
     "properties": {"vendor": {"type": "string"}, "total": {"type": "number", "minimum": 0},
                    "currency": {"type": "string", "enum": ["USD", "EUR", "GBP"]}, "evidence": {"type": "string"}},
     "required": ["vendor", "total", "currency", "evidence"],
     "additionalProperties": false}""",
)
val UNSUPPORTED = listOf("minimum", "maximum", "multipleOf", "minLength", "maxLength")
val CURRENCIES = listOf("USD", "EUR", "GBP")

/** The schema without the constraints that structured outputs reject; they move to the field's description. */
fun forApi(schema: JsonNode): ObjectNode = schema.deepCopy<ObjectNode>().also { walk(it) }

private fun walk(node: ObjectNode) {
    val notes = UNSUPPORTED.filter { node.has(it) }.map { k -> "$k ${node.remove(k).asText()}" }
    if (notes.isNotEmpty()) node.put("description", ((node["description"]?.asText() ?: "") + " (" + notes.joinToString(", ") + ")").trim())
    node["properties"]?.forEach { walk(it as ObjectNode) }
}

/** What the API cannot promise: numeric limits, the enum's capital letters, and a quotation that is really in the document. */
fun problems(value: Map<String, Any?>, document: String): List<String> {
    val found = mutableListOf<String>()
    val total = value["total"]
    if (total !is Number || total.toDouble() < 0) found += "$.total: must be a number of at least 0"
    if ((value["currency"] as? String) !in CURRENCIES) found += "$.currency: must be one of USD, EUR, GBP, not ${py(value["currency"])}"
    val evidence = value["evidence"]
    if (evidence !is String || evidence !in document) found += "$.evidence: is not found in the document"
    return found
}

/** Structured outputs may change the capital letters of an enum value; compare without them. */
fun normaliseEnum(value: Map<String, Any?>): Map<String, Any?> {
    val currency = value["currency"] as? String ?: return value
    val allowed = CURRENCIES.firstOrNull { it.equals(currency, ignoreCase = true) } ?: return value
    return value + ("currency" to allowed)
}

private fun outputConfig(): OutputConfig {
    val schema = forApi(LOCAL_SCHEMA).fields().asSequence().associate { (k, v) -> k to JsonValue.from(JSON.convertValue(v, Any::class.java)) }
    return OutputConfig.builder().format(JsonOutputFormat.builder().schema(JsonOutputFormat.Schema.builder().additionalProperties(schema).build()).build()).build()
}

@Suppress("UNCHECKED_CAST")
fun extract(client: AnthropicClient, document: String, maxAttempts: Int = 2): Map<String, Any?> {
    val messages = MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(300).outputConfig(outputConfig())
        .addUserMessage("Extract the invoice data.\n<document>\n$document\n</document>")
    var errors = emptyList<String>()
    for (attempt in 1..maxAttempts) {
        val reply = client.messages().create(messages.build())
        val stop = reply.stopReason().get().asString()
        if (stop == "refusal" || stop == "max_tokens") return map("status", if (stop == "refusal") "refused" else "truncated", "attempts", attempt)
        val raw = reply.content()[0].asText().text()
        val value = normaliseEnum(JSON.readValue(raw, LinkedHashMap::class.java) as Map<String, Any?>)
        errors = problems(value, document)
        if (errors.isEmpty()) return map("status", "ok", "attempts", attempt, "value", value)
        messages.addAssistantMessage(raw).addUserMessage("Rejected:\n" + errors.joinToString("\n") + "\nReturn corrected JSON.")
    }
    return map("status", "failed", "attempts", maxAttempts, "errors", errors)
}

const val DOC = "Invoice from Acme Tools. Total due: 120.50 EUR. Thank you for your business."

fun body(overrides: Map<String, Any?> = emptyMap()): String =
    JSON.writeValueAsString(map("vendor", "Acme Tools", "total", 120.5, "currency", "EUR", "evidence", "Total due: 120.50 EUR") + overrides)

val REPLIES = listOf<Any>(
    message(listOf(text(body(map("currency", "Eur"))))),
    message(listOf(text(body(map("evidence", "Total due: 999.00 USD"))))),
    message(listOf(text(body()))),
    message(listOf(text("I can't help with that.")), "refusal"),
    message(listOf(text("{\"vendor\": \"Acme")), "max_tokens"),
)

fun main() {
    val rig = Scripted.client(*REPLIES.toTypedArray())
    println("schema sent to the API: ${forApi(LOCAL_SCHEMA)["properties"]["total"]}")
    for (n in 1..2) println("document $n: ${py(extract(rig.client(), DOC))}")
    println("document 3: ${py(extract(rig.client(), DOC))}")
    println("document 4: ${py(extract(rig.client(), DOC))}")
    val sent = rig.http().requests
    println("requests sent: ${sent.size} | each carried output_config.format.type: ${sent.map { "'${it.at("/output_config/format/type").asText()}'" }.distinct().joinToString(", ", "{", "}")}")
    val third = sent[2]["messages"]
    println("second call of document 2 sent: ${py(third.map { it["role"].asText() })} | feedback: ${third.last()["content"].asText().split("\n")[1]}")
}
```
```text
schema sent to the API: {"type":"number","description":"(minimum 0)"}
document 1: {'status': 'ok', 'attempts': 1, 'value': {'vendor': 'Acme Tools', 'total': 120.5, 'currency': 'EUR', 'evidence': 'Total due: 120.50 EUR'}}
document 2: {'status': 'ok', 'attempts': 2, 'value': {'vendor': 'Acme Tools', 'total': 120.5, 'currency': 'EUR', 'evidence': 'Total due: 120.50 EUR'}}
document 3: {'status': 'refused', 'attempts': 1}
document 4: {'status': 'truncated', 'attempts': 1}
requests sent: 5 | each carried output_config.format.type: {'json_schema'}
second call of document 2 sent: ['user', 'assistant', 'user'] | feedback: $.evidence: is not found in the document
```
<!-- /example -->

Read the output. Every request carried `output_config.format.type` of `json_schema`, and the schema sent has no `minimum` in it. Document 2
needed two attempts: the first reply quoted a total that is not in the document, and the second request holds the user, assistant
and user messages in order, ending with the feedback `$.evidence: is not found in the document`. Documents 3 and 4 stopped at once
with `refused` and `truncated`.

Java and Kotlin readers: the practice of the next page implements the parser and the validator in your language.

## Traps

1. **Trusting the schema for everything.** The API cannot enforce `minimum`, `maxLength` or a quotation that must occur in the
   document. Check them in code.
2. **Parsing the body of a refusal or a cut-off reply.** Both have a 200 status. Check `stop_reason` first.
3. **Forcing a tool on a model that rejects it.** On Claude Opus 5.5 and the other models named above, `any` and `tool` return a 400
   error; use structured outputs or strict tools.

## Quiz

1. A team moves an invoice extractor to Claude Sonnet 5.5. The old code set `tool_choice` to an invoice tool and read the call's `input`. What should the request use now?
   - **a**: A tool choice of `any`, so that the invoice tool is still always called
   - **b**: The invoice tool marked `strict: true` and named in a tool choice of `tool`
   - **c**: A prompt that shows the invoice record and asks for the same shape
   - **d**: Structured outputs, with the record's schema in `output_config.format`

2. The schema says a field named `total` has a `minimum` of 0, and the request is sent with that schema unchanged. What happens?
   - **a**: The API rejects the call as invalid and sends back no reply
   - **b**: The reply is checked against the limit and retried for the caller
   - **c**: The limit is read as a hint only, and the call goes on as usual
   - **d**: The total is kept at 0 or above while the reply is generated

3. A reply arrives with a 200 status and `stop_reason` of `refusal`. What should the program assume about the body?
   - **a**: It becomes whole once the call is repeated with more tokens
   - **b**: It keeps the shape, because the status code says success
   - **c**: The model's decline takes the place of the promised shape
   - **d**: It is a malformed record that a re-prompt can repair

<details>
<summary>Answer key</summary>

1. **d**. A forced tool no longer works on this model, and the documentation's advice is structured outputs "when you need a response in a fixed JSON shape". *b* is ruled out because `strict: true` does not change the forcing, and of a forced tool the page says "On some models it no longer works". *c* is ruled out because a prompt gives "no guarantee; the program must parse defensively". *a* is ruled out because `any` is one of the values that "return a 400 error" on this model.
2. **a**. The page says "If you use an unsupported feature, you'll receive a 400 error with details." *b* is ruled out because "a helper that validates responses still enforces every constraint in your code", so the check is the program's job. *c* is ruled out because "If you use an unsupported feature" the outcome is an error, with no hint read. *d* is ruled out because the API does not enforce the limit: it does not support "Numerical constraints (such as `minimum`, `maximum`, `multipleOf`)".
3. **c**. The page says "The output may not match your schema because the refusal message takes precedence over schema constraints." *b* is ruled out because a refusal has "a 200 status code and billed tokens" and still breaks the shape. *a* is ruled out because the remedy of more tokens belongs to `max_tokens`: "Retry with a higher `max_tokens` value to get the complete structured output." *d* is ruled out because "A refusal is a result to report, not a malformed reply to repeat".

</details>
