# Streaming events and assembling a message

**Level:** Developer · **Module 17:** Streaming · **Page 1 of 2**
**Exams:** DV1

**After this page you can** read the server-sent events of a streamed reply, fold them into the message a non-streaming
call would return, and say which fields are cumulative, which are cut into fragments and which events to ignore.

Checked against the Claude API documentation (Streaming messages, the Python and TypeScript SDK pages, API versioning) on
2026-10-02, and by running the example and the practice offline in the course container with `anthropic` 1.11.0 and
`@anthropic-ai/sdk` 0.131.0. The model id is `claude-sonnet-5-5`; the stream is a hand-written sequence labelled
illustrative.

## Why it matters

A user waiting for a long answer looks at a blank screen for ten seconds, or reads the first words after one. Streaming is
how the second experience is built, and it changes the shape of the code: the result is no longer a value returned by one
call, it is a sequence you assemble. The exam asks what is in that sequence and where the usual mistakes are made: adding
what should be replaced, parsing what is not yet complete, and trusting a stream that ended early.

## The idea

### Streaming is the same request with stream set to true

A stream is the same POST request to the same URL with `stream` set to `true`. The response is `text/event-stream`:
**server-sent events**, each with an `event:` name and a `data:` line holding a JSON object whose `type` repeats the name.
The SDKs hide the framing. Python offers `client.messages.stream(...)`, a context manager with `text_stream` and
`get_final_message()`, and `client.messages.create(..., stream=True)` for the raw events. TypeScript offers
`client.messages.stream(...)` with `.on('text', ...)` and `.finalMessage()`, and `create({ stream: true })` for the raw
events. Java and Kotlin use the streaming method of the Java client, which returns the events.

### The shape of a stream

A stream is a sequence of named events, and the message arrives in pieces: a start, blocks, and an end.

| Event | Carries |
|---|---|
| `message_start` | A `message` with `id`, `role`, `model`, an empty `content` and the input token count in `usage` |
| `content_block_start` | The `index` of a block and its opening form: an empty `text`, a `tool_use` with its `id` and `name`, or a `thinking` block |
| `content_block_delta` | A piece of that block: `text_delta`, `input_json_delta`, `thinking_delta` or `signature_delta` |
| `content_block_stop` | The block at `index` is complete |
| `message_delta` | The `stop_reason` and `stop_sequence`, and `usage` with the output token count |
| `message_stop` | The stream finished normally |
| `ping` | A keep-alive with no content |
| `error` | An error, such as `overloaded_error`, in the stream |

Four rules turn that table into code:

1. **Blocks are assembled by index.** `content` lists the blocks in index order, not in the order they stopped. Blocks are
   never sorted by type or by size.
2. **Text and thinking are concatenated.** A `text_delta` appends to the text of its block, and a `thinking_delta` to its
   thinking. A `signature_delta` sets the signature, which must be sent back unchanged with the thinking block.
3. **Tool input is text until the block ends.** The fragments of a `tool_use` input arrive as `partial_json` strings that
   are not valid JSON until the block ends. Join them, and parse once, after the block stops.
4. **Usage is split in two.** Input tokens come from `message_start`; the output count comes from the last `message_delta`.
   The output count in `message_delta` is cumulative: replace the stored one, do not add to it.

Under the versioning policy the API may add new event types, so ignore event types you do not know. `ping` is the first example of such an event: the raw iteration of the Python and TypeScript SDKs drops it before your loop sees it.

A stream that ends without `message_stop` is an incomplete message, not a short one. The stop reason arrives in
`message_delta`, near the end, so a cut stream has none.

### The example: one stream, read three ways

The program feeds a hand-written stream through the real SDK from a scripted transport. The stream has one text block (in
two pieces), then one `tool_use` block whose input arrives in four fragments. It reads the stream with the SDK's helper,
with the raw events, and a second time with an `error` event in the middle.

<!-- example: m17-streaming tabs: python,typescript,java,kotlin -->
```python
"""A streamed reply read three ways, from a scripted server-sent-event body.

The stream is an illustrative, hand-written sequence of events in the API's framing
(claude-sonnet-5-5): one text block, then one tool_use block whose input arrives in fragments.
"""
import logging
import anthropic
import httpx2

from harness import ScriptedTransport, sse_response

log = logging.getLogger(__name__)

MODEL = "claude-sonnet-5-5"
PARAMS = dict(model=MODEL, max_tokens=128, messages=[{"role": "user", "content": "Weather in Paris?"}],
              tools=[{"name": "get_weather", "description": "Weather for a city.",
                      "input_schema": {"type": "object", "properties": {"city": {"type": "string"}}, "required": ["city"]}}])


def delta(index, kind, key, value):
    return {"type": "content_block_delta", "index": index, "delta": {"type": kind, key: value}}


EVENTS = [
    {"type": "message_start", "message": {"id": "msg_illustrative", "type": "message", "role": "assistant",
     "model": MODEL, "content": [], "stop_reason": None, "stop_sequence": None,
     "usage": {"input_tokens": 52, "output_tokens": 1}}},
    {"type": "content_block_start", "index": 0, "content_block": {"type": "text", "text": ""}},
    {"type": "ping"},
    delta(0, "text_delta", "text", "Let me "),
    delta(0, "text_delta", "text", "check."),
    {"type": "content_block_stop", "index": 0},
    {"type": "content_block_start", "index": 1,
     "content_block": {"type": "tool_use", "id": "toolu_illustrative_1", "name": "get_weather", "input": {}}},
    delta(1, "input_json_delta", "partial_json", ""),
    delta(1, "input_json_delta", "partial_json", '{"ci'),
    delta(1, "input_json_delta", "partial_json", 'ty": "Par'),
    delta(1, "input_json_delta", "partial_json", 'is"}'),
    {"type": "content_block_stop", "index": 1},
    {"type": "message_delta", "delta": {"stop_reason": "tool_use", "stop_sequence": None}, "usage": {"output_tokens": 38}},
    {"type": "message_stop"},
]
FAILING = EVENTS[:5] + [{"type": "error", "error": {"type": "overloaded_error", "message": "Overloaded"}}]


def run_length(names):
    """['a', 'b', 'b'] -> 'a, b x2'"""
    out = []
    for name in names:
        if out and out[-1][0] == name:
            out[-1][1] += 1
        else:
            out.append([name, 1])
    return ", ".join(n if c == 1 else f"{n} x{c}" for n, c in out)


def client_for(events):
    transport = ScriptedTransport(sse_response(events))
    return anthropic.Anthropic(api_key="placeholder", max_retries=0, http_client=httpx2.Client(transport=transport)), transport


def main():
    client, transport = client_for(EVENTS)
    print("request sets stream:", end=" ")
    with client.messages.stream(**PARAMS) as stream:
        print(transport.requests[0]["stream"])
        print("text pieces:", list(stream.text_stream))
        final = stream.get_final_message()
    print("final stop_reason:", final.stop_reason, "| usage:", final.usage.input_tokens, "in,", final.usage.output_tokens, "out")
    print("blocks:", [b.type for b in final.content], "| tool input:", final.content[1].input)

    client, _ = client_for(EVENTS)
    print("raw events:", run_length(e.type for e in client.messages.create(**PARAMS, stream=True)))

    client, _ = client_for(FAILING)
    try:
        with client.messages.stream(**PARAMS) as stream:
            for _ in stream.text_stream:
                pass
    except anthropic.APIStatusError as err:
        print("mid-stream error:", type(err).__name__, err.body["error"]["type"] if isinstance(err.body, dict) and "error" in err.body else err.body)


if __name__ == "__main__":
    main()
```
```text
request sets stream: True
text pieces: ['Let me ', 'check.']
final stop_reason: tool_use | usage: 52 in, 38 out
blocks: ['text', 'tool_use'] | tool input: {'city': 'Paris'}
raw events: message_start, content_block_start, content_block_delta x2, content_block_stop, content_block_start, content_block_delta x4, content_block_stop, message_delta, message_stop
mid-stream error: APIStatusError overloaded_error
```
```typescript
// A streamed reply read three ways, from a scripted server-sent-event body.
// The stream is an illustrative, hand-written sequence of events in the API's framing
// (claude-sonnet-5-5): one text block, then one tool_use block whose input arrives in fragments.
import Anthropic from "@anthropic-ai/sdk";
import { scriptedFetch } from "../../../harness/ts/scriptedFetch.ts";
import { logger } from "./logger.ts";
const log = logger("streaming");

const MODEL = "claude-sonnet-5-5";
export const PARAMS = {
  model: MODEL,
  max_tokens: 128,
  messages: [{ role: "user" as const, content: "Weather in Paris?" }],
  tools: [{ name: "get_weather", description: "Weather for a city.", input_schema: { type: "object" as const, properties: { city: { type: "string" } }, required: ["city"] } }],
};

const delta = (index: number, kind: string, key: string, value: string) => ({ type: "content_block_delta", index, delta: { type: kind, [key]: value } });

export const EVENTS = [
  { type: "message_start", message: { id: "msg_illustrative", type: "message", role: "assistant", model: MODEL, content: [], stop_reason: null, stop_sequence: null, usage: { input_tokens: 52, output_tokens: 1 } } },
  { type: "content_block_start", index: 0, content_block: { type: "text", text: "" } },
  { type: "ping" },
  delta(0, "text_delta", "text", "Let me "),
  delta(0, "text_delta", "text", "check."),
  { type: "content_block_stop", index: 0 },
  { type: "content_block_start", index: 1, content_block: { type: "tool_use", id: "toolu_illustrative_1", name: "get_weather", input: {} } },
  delta(1, "input_json_delta", "partial_json", ""),
  delta(1, "input_json_delta", "partial_json", '{"ci'),
  delta(1, "input_json_delta", "partial_json", 'ty": "Par'),
  delta(1, "input_json_delta", "partial_json", 'is"}'),
  { type: "content_block_stop", index: 1 },
  { type: "message_delta", delta: { stop_reason: "tool_use", stop_sequence: null }, usage: { output_tokens: 38 } },
  { type: "message_stop" },
];
export const FAILING = [...EVENTS.slice(0, 5), { type: "error", error: { type: "overloaded_error", message: "Overloaded" } }];

export function clientFor(events: Array<{ type: string }>) {
  const fake = scriptedFetch([{ sse: events }]);
  return { fake, client: new Anthropic({ apiKey: "placeholder", maxRetries: 0, fetch: fake.fetch }) };
}

function runLength(names: string[]): string {
  const out: Array<[string, number]> = [];
  for (const name of names) {
    if (out.length && out[out.length - 1][0] === name) out[out.length - 1][1]++;
    else out.push([name, 1]);
  }
  return out.map(([n, c]) => (c === 1 ? n : `${n} x${c}`)).join(", ");
}

async function main() {
  let { fake, client } = clientFor(EVENTS);
  const pieces: string[] = [];
  const stream = client.messages.stream(PARAMS).on("text", (t) => pieces.push(t));
  const final = await stream.finalMessage();
  console.log("request sets stream:", fake.seen[0].body.stream);
  console.log("text pieces:", JSON.stringify(pieces));
  console.log("final stop_reason:", final.stop_reason, "| usage:", final.usage.input_tokens, "in,", final.usage.output_tokens, "out");
  console.log("blocks:", final.content.map((b) => b.type), "| tool input:", JSON.stringify((final.content[1] as any).input));

  ({ client } = clientFor(EVENTS));
  const types: string[] = [];
  for await (const event of await client.messages.create({ ...PARAMS, stream: true })) types.push(event.type);
  console.log("raw events:", runLength(types));

  ({ client } = clientFor(FAILING));
  try {
    await client.messages.stream(PARAMS).finalMessage();
  } catch (err) {
    if (err instanceof Anthropic.APIError) console.log("mid-stream error:", err.constructor.name, (err.error as any)?.error?.type ?? err.message);
    else throw err;
  }
}

if (import.meta.main) await main();
```
```text
request sets stream: true
text pieces: ["Let me ","check."]
final stop_reason: tool_use | usage: 52 in, 38 out
blocks: [ 'text', 'tool_use' ] | tool input: {"city":"Paris"}
raw events: message_start, content_block_start, content_block_delta x2, content_block_stop, content_block_start, content_block_delta x4, content_block_stop, message_delta, message_stop
mid-stream error: APIError overloaded_error
```
```java
import static harness.Scripted.map;

import com.anthropic.client.AnthropicClient;
import com.anthropic.core.ObjectMappers;
import com.anthropic.core.http.StreamResponse;
import com.anthropic.errors.SseException;
import com.anthropic.helpers.MessageAccumulator;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.anthropic.models.messages.RawMessageStreamEvent;
import com.anthropic.models.messages.Tool;
import com.anthropic.core.JsonValue;
import harness.Reply;
import harness.Scripted;
import harness.ScriptedHttp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A streamed reply read three ways, from a scripted server-sent-event body.
 *
 * <p>The stream is an illustrative, hand-written sequence of events in the API's framing
 * (claude-sonnet-5-5): one text block, then one tool_use block whose input arrives in fragments.
 * The Java SDK reads the events itself and drops `ping` events, so a raw event list has no `ping` in it.
 */
public final class Streaming {
    private static final System.Logger LOG = System.getLogger(Streaming.class.getName());
    static final String MODEL = "claude-sonnet-5-5";

    static MessageCreateParams params() {
        Tool weather = Tool.builder().name("get_weather").description("Weather for a city.")
            .inputSchema(Tool.InputSchema.builder().properties(JsonValue.from(map("city", map("type", "string")))).required(List.of("city")).build()).build();
        return MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(128).addUserMessage("Weather in Paris?").addTool(weather).build();
    }

    static Map<String, Object> delta(int index, String kind, String key, String value) {
        return map("type", "content_block_delta", "index", index, "delta", map("type", kind, key, value));
    }

    static final List<Map<String, Object>> EVENTS = List.of(
        map("type", "message_start", "message", map("id", "msg_illustrative", "type", "message", "role", "assistant", "model", MODEL, "content", List.of(),
            "stop_reason", null, "stop_sequence", null, "usage", map("input_tokens", 52, "output_tokens", 1))),
        map("type", "content_block_start", "index", 0, "content_block", map("type", "text", "text", "")),
        map("type", "ping"),
        delta(0, "text_delta", "text", "Let me "),
        delta(0, "text_delta", "text", "check."),
        map("type", "content_block_stop", "index", 0),
        map("type", "content_block_start", "index", 1, "content_block", map("type", "tool_use", "id", "toolu_illustrative_1", "name", "get_weather", "input", map())),
        delta(1, "input_json_delta", "partial_json", ""),
        delta(1, "input_json_delta", "partial_json", "{\"ci"),
        delta(1, "input_json_delta", "partial_json", "ty\": \"Par"),
        delta(1, "input_json_delta", "partial_json", "is\"}"),
        map("type", "content_block_stop", "index", 1),
        map("type", "message_delta", "delta", map("stop_reason", "tool_use", "stop_sequence", null), "usage", map("output_tokens", 38)),
        map("type", "message_stop"));

    static final List<Map<String, Object>> FAILING = failing();

    private static List<Map<String, Object>> failing() {
        List<Map<String, Object>> events = new ArrayList<>(EVENTS.subList(0, 5));
        events.add(map("type", "error", "error", map("type", "overloaded_error", "message", "Overloaded")));
        return events;
    }

    /** The pieces of a streamed reply: the text deltas, and the message the SDK's accumulator assembled from all the events. */
    record Streamed(List<String> textPieces, Message message) {}

    /** The name of a raw stream event, as it is on the wire. */
    static String typeName(RawMessageStreamEvent e) {
        if (e.isMessageStart()) return "message_start";
        if (e.isMessageDelta()) return "message_delta";
        if (e.isMessageStop()) return "message_stop";
        if (e.isContentBlockStart()) return "content_block_start";
        if (e.isContentBlockDelta()) return "content_block_delta";
        return "content_block_stop";
    }

    static Streamed readText(AnthropicClient client) {
        MessageAccumulator accumulator = MessageAccumulator.create();
        List<String> pieces = new ArrayList<>();
        try (StreamResponse<RawMessageStreamEvent> stream = client.messages().createStreaming(params())) {
            stream.stream().forEach(event -> {
                accumulator.accumulate(event);
                event.contentBlockDelta().flatMap(d -> d.delta().text()).ifPresent(t -> pieces.add(t.text()));
            });
        }
        return new Streamed(pieces, accumulator.message());
    }

    static List<String> rawEventNames(AnthropicClient client) {
        try (StreamResponse<RawMessageStreamEvent> stream = client.messages().createStreaming(params())) {
            return stream.stream().map(Streaming::typeName).toList();
        }
    }

    /** ['a', 'b', 'b'] becomes "a, b x2". */
    static String runLength(List<String> names) {
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < names.size(); ) {
            int j = i;
            while (j < names.size() && names.get(j).equals(names.get(i))) j++;
            parts.add(j - i == 1 ? names.get(i) : names.get(i) + " x" + (j - i));
            i = j;
        }
        return String.join(", ", parts);
    }

    static Scripted.Rig clientFor(List<Map<String, Object>> events) {
        return Scripted.client(Reply.sse(events));
    }

    /** The tool input the stream assembled, as a map. */
    @SuppressWarnings("unchecked")
    static Map<String, Object> toolInput(ContentBlock block) {
        return ObjectMappers.jsonMapper().convertValue(block.asToolUse()._input(), Map.class);
    }

    private static String py(Object v) {
        if (v instanceof String s) return "'" + s + "'";
        if (v instanceof Map<?, ?> m) return m.entrySet().stream().map(e -> py(e.getKey()) + ": " + py(e.getValue())).collect(Collectors.joining(", ", "{", "}"));
        if (v instanceof List<?> l) return l.stream().map(Streaming::py).collect(Collectors.joining(", ", "[", "]"));
        return String.valueOf(v);
    }

    public static void main(String[] args) {
        Scripted.Rig rig = clientFor(EVENTS);
        Streamed streamed = readText(rig.client());
        System.out.println("request sets stream: " + (rig.http().requests.get(0).get("stream").asBoolean() ? "True" : "False"));
        System.out.println("text pieces: " + py(streamed.textPieces()));
        Message finalMessage = streamed.message();
        System.out.println("final stop_reason: " + finalMessage.stopReason().get().asString() + " | usage: " + finalMessage.usage().inputTokens() + " in, " + finalMessage.usage().outputTokens() + " out");
        System.out.println("blocks: " + py(finalMessage.content().stream().map(b -> b.isText() ? "text" : "tool_use").toList()) + " | tool input: " + py(toolInput(finalMessage.content().get(1))));

        System.out.println("raw events: " + runLength(rawEventNames(clientFor(EVENTS).client())));

        try {
            readText(clientFor(FAILING).client());
        } catch (SseException err) {
            System.out.println("mid-stream error: " + err.getClass().getSimpleName() + " " + err.errorType().map(t -> t.asString()).orElse("?"));
        }
    }
}
```
```text
request sets stream: True
text pieces: ['Let me ', 'check.']
final stop_reason: tool_use | usage: 52 in, 38 out
blocks: ['text', 'tool_use'] | tool input: {'city': 'Paris'}
raw events: message_start, content_block_start, content_block_delta x2, content_block_stop, content_block_start, content_block_delta x4, content_block_stop, message_delta, message_stop
mid-stream error: SseException overloaded_error
```
```kotlin
import com.anthropic.core.JsonValue
import com.anthropic.core.jsonMapper
import com.anthropic.errors.SseException
import com.anthropic.helpers.MessageAccumulator
import com.anthropic.client.AnthropicClient
import com.anthropic.models.messages.ContentBlock
import com.anthropic.models.messages.Message
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.Model
import com.anthropic.models.messages.RawMessageStreamEvent
import com.anthropic.models.messages.Tool
import harness.Reply
import harness.Scripted
import harness.Scripted.map

private val log = System.getLogger("streaming")

/**
 * A streamed reply read three ways, from a scripted server-sent-event body.
 *
 * The stream is an illustrative, hand-written sequence of events in the API's framing
 * (claude-sonnet-5-5): one text block, then one tool_use block whose input arrives in fragments.
 * The Java SDK (used from Kotlin) reads the events itself and drops `ping` events, so a raw event list has no `ping` in it.
 */
const val MODEL = "claude-sonnet-5-5"

fun params(): MessageCreateParams {
    val weather = Tool.builder().name("get_weather").description("Weather for a city.")
        .inputSchema(Tool.InputSchema.builder().properties(JsonValue.from(map("city", map("type", "string")))).required(listOf("city")).build()).build()
    return MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(128).addUserMessage("Weather in Paris?").addTool(weather).build()
}

fun delta(index: Int, kind: String, key: String, value: String) = map("type", "content_block_delta", "index", index, "delta", map("type", kind, key, value))

val EVENTS = listOf(
    map(
        "type", "message_start",
        "message", map("id", "msg_illustrative", "type", "message", "role", "assistant", "model", MODEL, "content", listOf<Any>(), "stop_reason", null, "stop_sequence", null, "usage", map("input_tokens", 52, "output_tokens", 1)),
    ),
    map("type", "content_block_start", "index", 0, "content_block", map("type", "text", "text", "")),
    map("type", "ping"),
    delta(0, "text_delta", "text", "Let me "),
    delta(0, "text_delta", "text", "check."),
    map("type", "content_block_stop", "index", 0),
    map("type", "content_block_start", "index", 1, "content_block", map("type", "tool_use", "id", "toolu_illustrative_1", "name", "get_weather", "input", map())),
    delta(1, "input_json_delta", "partial_json", ""),
    delta(1, "input_json_delta", "partial_json", "{\"ci"),
    delta(1, "input_json_delta", "partial_json", "ty\": \"Par"),
    delta(1, "input_json_delta", "partial_json", "is\"}"),
    map("type", "content_block_stop", "index", 1),
    map("type", "message_delta", "delta", map("stop_reason", "tool_use", "stop_sequence", null), "usage", map("output_tokens", 38)),
    map("type", "message_stop"),
)
val FAILING = EVENTS.take(5) + map("type", "error", "error", map("type", "overloaded_error", "message", "Overloaded"))

/** The pieces of a streamed reply: the text deltas, and the message the SDK's accumulator assembled from all the events. */
data class Streamed(val textPieces: List<String>, val message: Message)

/** The name of a raw stream event, as it is on the wire. */
fun typeName(e: RawMessageStreamEvent) = when {
    e.isMessageStart() -> "message_start"
    e.isMessageDelta() -> "message_delta"
    e.isMessageStop() -> "message_stop"
    e.isContentBlockStart() -> "content_block_start"
    e.isContentBlockDelta() -> "content_block_delta"
    else -> "content_block_stop"
}

fun readText(client: AnthropicClient): Streamed {
    val accumulator = MessageAccumulator.create()
    val pieces = mutableListOf<String>()
    client.messages().createStreaming(params()).use { stream ->
        stream.stream().forEach { event ->
            accumulator.accumulate(event)
            event.contentBlockDelta().flatMap { it.delta().text() }.ifPresent { pieces += it.text() }
        }
    }
    return Streamed(pieces, accumulator.message())
}

fun rawEventNames(client: AnthropicClient): List<String> = client.messages().createStreaming(params()).use { stream -> stream.stream().map(::typeName).toList() }

/** ['a', 'b', 'b'] becomes "a, b x2". */
fun runLength(names: List<String>): String {
    val parts = mutableListOf<String>()
    var i = 0
    while (i < names.size) {
        var j = i
        while (j < names.size && names[j] == names[i]) j++
        parts += if (j - i == 1) names[i] else "${names[i]} x${j - i}"
        i = j
    }
    return parts.joinToString(", ")
}

fun clientFor(events: List<Map<String, Any?>>) = Scripted.client(Reply.sse(events))

/** The tool input the stream assembled, as a map. */
fun toolInput(block: ContentBlock): Map<*, *> = jsonMapper().convertValue(block.asToolUse()._input(), Map::class.java)

private fun py(v: Any?): String = when (v) {
    is String -> "'$v'"
    is Map<*, *> -> v.entries.joinToString(", ", "{", "}") { "${py(it.key)}: ${py(it.value)}" }
    is List<*> -> v.joinToString(", ", "[", "]") { py(it) }
    else -> v.toString()
}

fun main() {
    val rig = clientFor(EVENTS)
    val streamed = readText(rig.client())
    println("request sets stream: ${if (rig.http().requests[0]["stream"].asBoolean()) "True" else "False"}")
    println("text pieces: ${py(streamed.textPieces)}")
    val message = streamed.message
    println("final stop_reason: ${message.stopReason().get().asString()} | usage: ${message.usage().inputTokens()} in, ${message.usage().outputTokens()} out")
    println("blocks: ${py(message.content().map { if (it.isText()) "text" else "tool_use" })} | tool input: ${py(toolInput(message.content()[1]))}")

    println("raw events: ${runLength(rawEventNames(clientFor(EVENTS).client()))}")

    try {
        readText(clientFor(FAILING).client())
    } catch (err: SseException) {
        println("mid-stream error: ${err.javaClass.simpleName} ${err.errorType().map { it.asString() }.orElse("?")}")
    }
}
```
```text
request sets stream: True
text pieces: ['Let me ', 'check.']
final stop_reason: tool_use | usage: 52 in, 38 out
blocks: ['text', 'tool_use'] | tool input: {'city': 'Paris'}
raw events: message_start, content_block_start, content_block_delta x2, content_block_stop, content_block_start, content_block_delta x4, content_block_stop, message_delta, message_stop
mid-stream error: SseException overloaded_error
```
<!-- /example -->

Read the output for four facts:

1. **The request carries `stream: true`.** It is the same call with one more field.
2. **The helper assembled the message.** The text came in two pieces, and the final message holds the two blocks, the tool
   input `{'city': 'Paris'}` parsed from four fragments, and usage of 52 in and 38 out: the input count from the start and
   the output count from the end.
3. **The raw events omit `ping`.** The stream held one, and the run-length summary does not show it, because the SDK
   drops it.
4. **An error in the middle is an exception.** The SDK raises when it meets the `error` event, with the type from the
   event, although the status of the response was a success.

The Java and Kotlin tabs read the same scripted event stream with the SDK's `MessageAccumulator`, which assembles the message from the
events as they arrive. The SDK drops `ping` events before the program sees them, and an `error` event ends the stream with an
`SseException` (the Python SDK raises `APIStatusError`), so the mid-stream error line reads `SseException overloaded_error`.
The practice below runs in all four languages.

## The practice: assemble a streamed message

You write `assemble(events)`, which folds the parsed `data:` objects into the message a non-streaming call would have
returned. The statement, with the exact contract, is in
`exercises/17-streaming/unit-01/practice-1/statement.md`; each language folder has a `starter`, the `tests` and a `run.sh`
or build file. The starter fails every test.

| Id | What it checks |
|---|---|
| `m1` | Text deltas are joined and the message is complete |
| `e1` | Tool input is the fragments joined, then parsed; empty input is `{}` |
| `e2` | `ping` and unknown event types are ignored |
| `e3` | An error event raises with its type and message |
| `e4` | A stream that ends before `message_stop` is an error, not a short message |
| `e5` | Usage: input tokens from the start, the cumulative output tokens from the end |
| `e6` | Blocks keep their index order; thinking fields are assembled |

## Traps

1. **Adding the output count.** Each `message_delta` restates the total so far. A client that sums them reports a multiple
   of the real cost.
2. **Parsing each fragment.** A fragment of tool input is a piece of text, not an object. Parse after the block stops.
3. **Accepting a stream that just stopped.** Without `message_stop` there is no stop reason and the content may end in the
   middle of a word. Raise, so that the caller can decide.

## Quiz

1. A handler adds up the output token count of every `message_delta` it receives and reports three times the real cost.
   What does the page say it should do instead?
   - **a**: Overwrite the earlier figure, since each event restates the running tally
   - **b**: Take the input count from the final event and the output count from the first
   - **c**: Sum them all and then subtract the figure that the opening event gave
   - **d**: Drop the field and measure the pieces of text that arrived in the stream

2. A stream breaks in the middle of the fragments of a tool call's input. What should the code do?
   - **a**: Join the fragments received so far and parse them as the input
   - **b**: Run the tool with whatever the first fragment held
   - **c**: Throw the piece away and report the whole message as failed
   - **d**: Keep the block and mark the message as a short one

<details>
<summary>Answer key</summary>

1. **a**. The page says "The output count in message_delta is cumulative: replace the stored one, do not add to it". *b* is ruled out because "the output count comes from the last message_delta" and the input count from `message_start`. *c* is ruled out because the page says "do not add to it". *d* is ruled out because usage is read from the events: "Input tokens come from message_start".
2. **c**. The page says "Raise, so that the caller can decide" when a stream just stopped, because it "is an incomplete message, not a short one". *a* is ruled out because the fragments "are not valid JSON until the block ends". *b* is ruled out because "A fragment of tool input is a piece of text, not an object". *d* is ruled out because the page says "an incomplete message, not a short one".

</details>
