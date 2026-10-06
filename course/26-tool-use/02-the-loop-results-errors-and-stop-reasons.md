# The loop: results, errors and stop reasons

**Level:** Developer · **Module 26:** Tool use · **Page 2 of 3**
**Exams:** DV5; A1.1, A2.1

**After this page you can** write the loop that runs a model's tool calls, format the user message that carries the results,
report a failure to the model with `is_error` so that it can recover, react to every stop reason, and bound the loop so that it cannot
run for ever.

Checked against the Claude API documentation (How tool use works, Handle tool calls and Handling stop reasons) on 2026-10-03, and by
running the example and the practice offline in the course container (`anthropic` 1.11.0, `@anthropic-ai/sdk` 0.131.0). The replies in
the example are illustrative, scripted bodies in the shape of the Messages API, not captures.

## Why it matters

A model that can call tools cannot be used through a single request. Someone has to run the calls and send the results back, and the
shape of that message is strict. The mistakes are quiet: a result in the wrong place makes the API reject the request, a
swallowed error makes the model invent an answer, and a missing limit lets a confused model call tools until the budget is gone.
The exam describes a failing loop and asks what is wrong with it.

## The idea

### The shape of the loop

The documentation gives "the canonical shape" as a `while` loop keyed on `stop_reason`:

1. Send a request with your `tools` array and the user message.
2. Claude responds with `stop_reason: "tool_use"` and one or more `tool_use` blocks.
3. Execute each tool and format the outputs as `tool_result` blocks.
4. Send a new request with "the original messages, the assistant's response, and a user message with the `tool_result` blocks".
5. Repeat from step 2 while `stop_reason` is `"tool_use"`.

Each `tool_use` block carries an `id`, a `name` and an `input`. Each result carries the `tool_use_id` of its call, an optional
`content` (a string, or a list of text, image, document or `search_result` blocks), and an optional `is_error`. The assistant's
reply goes back unchanged: its text and all its blocks become one `assistant` message, so that the next request has the whole
conversation.

### The formatting rules that cause 400 errors

- "Tool result blocks must immediately follow their corresponding tool use blocks in the message history." No message may sit between
  the assistant turn and the user turn with the results.
- "In the user message containing tool results, the tool_result blocks must come FIRST in the content array. Any text must come AFTER
  all tool results." A sentence such as "Here are the results:" placed before the first result "will cause a 400 error".
- If the assistant turn also called a server tool with no result block yet, the user message "must contain only `tool_result`
  blocks".

A related fault is milder. Text added right after a `tool_result` can lead to an empty reply: "Sometimes Claude returns an empty
response (2–3 tokens with no content) with `stop_reason: "end_turn"`", and the cause listed first is "Adding text blocks immediately
after tool results". Send the results alone.

### Errors are results

A tool can fail. When the handler throws, the loop catches it and sends the message back with `"is_error": true`. The documentation:
"Claude will then incorporate this error into its response to the user." It also gives the rule for the message: "Write instructive
error messages. Instead of generic errors like `"failed"`, include what went wrong and what Claude should try next." The example's
third call asks the weather of a city that its handler does not know, and the result is "No data for 'Atlantis'. Known cities: Oslo,
Rome." In the scripted reply the model answers the part it can and says it has no data for the rest.

An invalid call is the same case. If a tool is called without a required parameter, "you can also continue the conversation forward
with a `tool_result` that indicates the error", and Claude retries "2-3 times with corrections before apologizing to the user". The
practice does the check before the handler runs: a key from the schema's `required` list that is missing becomes an error result and
the handler is not called. With `strict: true` the case disappears: "To eliminate invalid tool calls entirely, use strict tool use".
An unknown tool name gets an error result too (`Unknown tool: NAME`), since the loop should not crash on a hallucinated name.

The loop must not turn an error into success. A call ends in a value or in an error result, and a call that was
never run gets an error result that says so. A result that quietly returns an empty string for a failure teaches the model that the
tool worked.

### Results that are not text

A tool handler may return a number, a list or a record. The `content` of a result is a string or a list of content blocks, so the
loop converts: a string goes as it is, and any other value goes as JSON text. The practice's case `e6` checks this.

### Every stop reason, one reaction

The loop exits on any stop reason other than `tool_use`. The documentation's table decides the reaction:

| Value | Meaning | Reaction |
|---|---|---|
| `end_turn` | "Claude finished its response naturally." | use the reply: status `done` |
| `stop_sequence` | one of your `stop_sequences` was emitted | use the reply: status `done` |
| `tool_use` | Claude is calling a tool | run the tools and call again |
| `max_tokens` | the response reached your limit | status `truncated`; a cut-off `tool_use` block needs a higher `max_tokens` |
| `refusal` | Claude declined | status `refused`; the same request is not repeated (a fallback model is a separate option) |
| `pause_turn` | "A server-tool loop reached its iteration limit" | send the assistant content back and call again |

For `pause_turn` the documentation's key points are to send the assistant response back as it is, keep the same `tools` array, and remember
that this "is different from `tool_use`, which requires `tool_result` blocks". The default iteration limit of the server-side
loop is 10.

### Bound the loop

A loop keyed on `stop_reason` ends when the model decides to end it. A model that keeps calling a tool that keeps failing never decides.
The loop needs its own limit of model calls, and a status for reaching it. The practice calls the limit `max_turns` and reports
`max_turns`, so that the caller can log the conversation and stop. The SDK's tool runner has the same idea: it "loops until Claude
returns a message without a tool use, or until it reaches `max_iterations`".

### The example

The example runs the loop of this page on the official SDK against a scripted model. The first reply holds three calls: a weather
lookup, a time lookup and a weather lookup for a city that does not exist. All three results go back in one user message, in the
order of the calls, and the third one carries `is_error`.

<!-- example: m26-tool-loop tabs: python,typescript,java,kotlin -->
```python
"""A tool loop on the official SDK, against a scripted model: parallel calls, one failing tool and a tool_choice that is kept.

The replies are illustrative, hand-written bodies in the shape of the Messages API (claude-sonnet-5-5), not captures.
"""
import logging
import json

from harness import scripted_client
from harness.scripted import message, text, tool_use

log = logging.getLogger(__name__)

MODEL = "claude-sonnet-5-5"
TOOLS = [
    {"name": "get_weather", "description": "Current weather for one city. Use it when the user asks about weather now. Returns a short sentence; it knows nothing about forecasts.",
     "input_schema": {"type": "object", "properties": {"city": {"type": "string", "description": "City name, for example Oslo"}}, "required": ["city"]}},
    {"name": "get_time", "description": "Local time for one city, as HH:MM on a 24 hour clock. Use it when the user asks what time it is somewhere.",
     "input_schema": {"type": "object", "properties": {"city": {"type": "string", "description": "City name, for example Oslo"}}, "required": ["city"]}},
]
HANDLERS = {"get_weather": lambda a: {"Oslo": "Oslo: 4 C, light rain"}[a["city"]], "get_time": lambda a: {"Oslo": "09:15", "Rome": "09:15"}[a["city"]]}


def run_tool(block):
    try:
        return {"type": "tool_result", "tool_use_id": block.id, "content": HANDLERS[block.name](block.input)}
    except KeyError as err:
        return {"type": "tool_result", "tool_use_id": block.id, "content": f"No data for {err.args[0]!r}. Known cities: Oslo, Rome.", "is_error": True}


def loop(client, question, **extra):
    messages = [{"role": "user", "content": question}]
    while True:
        reply = client.messages.create(model=MODEL, max_tokens=500, tools=TOOLS, messages=messages, **extra)
        messages.append({"role": "assistant", "content": reply.content})
        if reply.stop_reason != "tool_use":
            return reply, messages
        messages.append({"role": "user", "content": [run_tool(b) for b in reply.content if b.type == "tool_use"]})
        if extra.get("tool_choice", {}).get("type") in ("any", "tool"):
            extra = {k: v for k, v in extra.items() if k != "tool_choice"}  # a forced choice applies to the first request only; auto and none stay


REPLIES = [
    message([text("Checking all three."), tool_use("toolu_01", "get_weather", city="Oslo"), tool_use("toolu_02", "get_time", city="Oslo"), tool_use("toolu_03", "get_weather", city="Atlantis")],
            stop_reason="tool_use", model=MODEL),
    message([text("In Oslo it is 09:15 and 4 C with light rain. I have no weather data for Atlantis.")], model=MODEL),
]


def main():
    client, transport = scripted_client(*REPLIES)
    final, messages = loop(client, "Weather and time in Oslo, and the weather in Atlantis?", tool_choice={"type": "auto", "disable_parallel_tool_use": False})
    print("model calls:", len(transport.requests), "| stop reasons:", ["tool_use", final.stop_reason])
    print("roles after the first reply:", [m["role"] for m in messages])
    results = messages[2]["content"]
    print("tool results in ONE user message:", len(results), "| ids in order:", [r["tool_use_id"] for r in results])
    for r in results:
        print(f"  {r['tool_use_id']}: is_error={r.get('is_error', False)} content={r['content']!r}")
    print("tool_choice sent on requests 1 and 2:", [r.get("tool_choice") for r in transport.requests])
    print("tool definitions sent carry no handler:", all(set(t) == {"name", "description", "input_schema"} for t in transport.requests[0]["tools"]))
    print("final text:", final.content[0].text)


if __name__ == "__main__":
    main()
```
```text
model calls: 2 | stop reasons: ['tool_use', 'end_turn']
roles after the first reply: ['user', 'assistant', 'user', 'assistant']
tool results in ONE user message: 3 | ids in order: ['toolu_01', 'toolu_02', 'toolu_03']
  toolu_01: is_error=False content='Oslo: 4 C, light rain'
  toolu_02: is_error=False content='09:15'
  toolu_03: is_error=True content="No data for 'Atlantis'. Known cities: Oslo, Rome."
tool_choice sent on requests 1 and 2: [{'type': 'auto', 'disable_parallel_tool_use': False}, {'type': 'auto', 'disable_parallel_tool_use': False}]
tool definitions sent carry no handler: True
final text: In Oslo it is 09:15 and 4 C with light rain. I have no weather data for Atlantis.
```
```typescript
// A tool loop on the official SDK, against a scripted model: parallel calls, one failing tool and a tool_choice that is kept.
// The replies are illustrative, hand-written bodies in the shape of the Messages API (claude-sonnet-5-5), not captures.
import Anthropic from "@anthropic-ai/sdk";
import { message, scriptedFetch, text } from "../../../harness/ts/scriptedFetch.ts";
import { logger } from "./logger.ts";
const log = logger("tool_loop");

export const MODEL = "claude-sonnet-5-5";
export const TOOLS: Anthropic.Tool[] = [
  { name: "get_weather", description: "Current weather for one city. Use it when the user asks about weather now. Returns a short sentence; it knows nothing about forecasts.",
    input_schema: { type: "object", properties: { city: { type: "string", description: "City name, for example Oslo" } }, required: ["city"] } },
  { name: "get_time", description: "Local time for one city, as HH:MM on a 24 hour clock. Use it when the user asks what time it is somewhere.",
    input_schema: { type: "object", properties: { city: { type: "string", description: "City name, for example Oslo" } }, required: ["city"] } },
];
const DATA: Record<string, Record<string, string>> = { get_weather: { Oslo: "Oslo: 4 C, light rain" }, get_time: { Oslo: "09:15", Rome: "09:15" } };

export function runTool(block: { id: string; name: string; input: any }) {
  const content = DATA[block.name]?.[block.input.city];
  if (content === undefined) return { type: "tool_result" as const, tool_use_id: block.id, content: `No data for '${block.input.city}'. Known cities: Oslo, Rome.`, is_error: true };
  return { type: "tool_result" as const, tool_use_id: block.id, content };
}

export async function loop(client: Anthropic, question: string, toolChoice?: Anthropic.ToolChoice) {
  const messages: Anthropic.MessageParam[] = [{ role: "user", content: question }];
  let choice = toolChoice;
  for (;;) {
    const reply = await client.messages.create({ model: MODEL, max_tokens: 500, tools: TOOLS, messages, ...(choice ? { tool_choice: choice } : {}) });
    messages.push({ role: "assistant", content: reply.content });
    if (reply.stop_reason !== "tool_use") return { reply, messages };
    messages.push({ role: "user", content: reply.content.filter((b): b is Anthropic.ToolUseBlock => b.type === "tool_use").map((b) => runTool(b)) });
    if (choice && (choice.type === "any" || choice.type === "tool")) choice = undefined; // a forced choice applies to the first request only; auto and none stay
  }
}

const usage = { input_tokens: 1, output_tokens: 1 };
export const REPLIES = () => [
  { body: message([text("Checking all three."), { type: "tool_use", id: "toolu_01", name: "get_weather", input: { city: "Oslo" } }, { type: "tool_use", id: "toolu_02", name: "get_time", input: { city: "Oslo" } },
    { type: "tool_use", id: "toolu_03", name: "get_weather", input: { city: "Atlantis" } }], "tool_use", usage, MODEL) },
  { body: message([text("In Oslo it is 09:15 and 4 C with light rain. I have no weather data for Atlantis.")], "end_turn", usage, MODEL) },
];

export function clientFor(replies: Array<Record<string, unknown>>) {
  const fake = scriptedFetch(replies as any);
  return { fake, client: new Anthropic({ apiKey: "placeholder", maxRetries: 0, fetch: fake.fetch }) };
}

/** Python's repr of a small value, so that both languages print the same lines. */
function py(v: any): string {
  if (v === null || v === undefined) return "None";
  if (typeof v === "boolean") return v ? "True" : "False";
  if (typeof v === "string") return v.includes("'") && !v.includes('"') ? `"${v}"` : `'${v}'`;
  if (Array.isArray(v)) return `[${v.map(py).join(", ")}]`;
  if (typeof v === "object") return `{${Object.entries(v).map(([k, x]) => `${py(k)}: ${py(x)}`).join(", ")}}`;
  return String(v);
}

async function main() {
  const { fake, client } = clientFor(REPLIES());
  const { reply, messages } = await loop(client, "Weather and time in Oslo, and the weather in Atlantis?", { type: "auto", disable_parallel_tool_use: false });
  console.log("model calls:", fake.seen.length, "| stop reasons:", py(["tool_use", reply.stop_reason]));
  console.log("roles after the first reply:", py(messages.map((m) => m.role)));
  const results = messages[2].content as any[];
  console.log("tool results in ONE user message:", results.length, "| ids in order:", py(results.map((r) => r.tool_use_id)));
  for (const r of results) console.log(`  ${r.tool_use_id}: is_error=${py(r.is_error ?? false)} content=${py(r.content)}`);
  console.log("tool_choice sent on requests 1 and 2:", py(fake.seen.map((r) => r.body.tool_choice ?? null)));
  console.log("tool definitions sent carry no handler:", py(fake.seen[0].body.tools.every((t: any) => Object.keys(t).sort().join() === "description,input_schema,name")));
  console.log("final text:", (reply.content[0] as { text: string }).text);
}

if (import.meta.main) await main();
```
```text
model calls: 2 | stop reasons: ['tool_use', 'end_turn']
roles after the first reply: ['user', 'assistant', 'user', 'assistant']
tool results in ONE user message: 3 | ids in order: ['toolu_01', 'toolu_02', 'toolu_03']
  toolu_01: is_error=False content='Oslo: 4 C, light rain'
  toolu_02: is_error=False content='09:15'
  toolu_03: is_error=True content="No data for 'Atlantis'. Known cities: Oslo, Rome."
tool_choice sent on requests 1 and 2: [{'type': 'auto', 'disable_parallel_tool_use': False}, {'type': 'auto', 'disable_parallel_tool_use': False}]
tool definitions sent carry no handler: True
final text: In Oslo it is 09:15 and 4 C with light rain. I have no weather data for Atlantis.
```
```java
import static harness.Scripted.map;
import static harness.Scripted.message;
import static harness.Scripted.text;
import static harness.Scripted.toolUse;
import static harness.Show.py;

import com.anthropic.client.AnthropicClient;
import com.anthropic.core.JsonValue;
import com.anthropic.core.ObjectMappers;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.MessageParam;
import com.anthropic.models.messages.Model;
import com.anthropic.models.messages.Tool;
import com.anthropic.models.messages.ToolChoice;
import com.anthropic.models.messages.ToolChoiceAuto;
import com.anthropic.models.messages.ToolResultBlockParam;
import com.anthropic.models.messages.ToolUnion;
import com.anthropic.models.messages.ToolUseBlock;
import harness.Scripted;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A tool loop on the official SDK, against a scripted model: parallel calls, one failing tool and a tool_choice that is kept.
 *
 * <p>The replies are illustrative, hand-written bodies in the shape of the Messages API (claude-sonnet-5-5), not captures.
 */
public final class ToolLoop {
    private static final System.Logger LOG = System.getLogger(ToolLoop.class.getName());
    static final String MODEL = "claude-sonnet-5-5";

    static Tool tool(String name, String description) {
        return Tool.builder().name(name).description(description)
            .inputSchema(Tool.InputSchema.builder()
                .properties(JsonValue.from(map("city", map("type", "string", "description", "City name, for example Oslo"))))
                .required(List.of("city")).build())
            .build();
    }

    static final List<Tool> TOOLS = List.of(
        tool("get_weather", "Current weather for one city. Use it when the user asks about weather now. Returns a short sentence; it knows nothing about forecasts."),
        tool("get_time", "Local time for one city, as HH:MM on a 24 hour clock. Use it when the user asks what time it is somewhere."));

    static final Map<String, Function<Map<String, Object>, String>> HANDLERS = Map.of(
        "get_weather", a -> Map.of("Oslo", "Oslo: 4 C, light rain").get(a.get("city")),
        "get_time", a -> Map.of("Oslo", "09:15", "Rome", "09:15").get(a.get("city")));

    /** One tool call answered: the result block that goes back to the model. */
    @SuppressWarnings("unchecked")
    static ToolResultBlockParam runTool(ToolUseBlock block) {
        Map<String, Object> input = ObjectMappers.jsonMapper().convertValue(block._input(), Map.class);
        String answer = HANDLERS.get(block.name()).apply(input);
        if (answer == null) {
            return ToolResultBlockParam.builder().toolUseId(block.id()).content("No data for '" + input.get("city") + "'. Known cities: Oslo, Rome.").isError(true).build();
        }
        return ToolResultBlockParam.builder().toolUseId(block.id()).content(answer).build();
    }

    /** The last reply and the whole transcript, as the requests carried it. */
    record Loop(Message reply, List<MessageParam> messages) {}

    static Loop loop(AnthropicClient client, String question, ToolChoice choice) {
        List<MessageParam> messages = new ArrayList<>();
        messages.add(MessageParam.builder().role(MessageParam.Role.USER).content(question).build());
        ToolChoice active = choice;
        while (true) {
            MessageCreateParams.Builder request = MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(500).messages(messages);
            TOOLS.forEach(request::addTool);
            if (active != null) request.toolChoice(active);
            Message reply = client.messages().create(request.build());
            messages.add(reply.toParam());
            if (!reply.stopReason().get().asString().equals("tool_use")) return new Loop(reply, messages);
            List<ContentBlockParam> results = new ArrayList<>();
            for (ContentBlock b : reply.content()) if (b.isToolUse()) results.add(ContentBlockParam.ofToolResult(runTool(b.asToolUse())));
            messages.add(MessageParam.builder().role(MessageParam.Role.USER).contentOfBlockParams(results).build());
            if (active != null && (active.isAny() || active.isTool())) active = null; // a forced choice applies to the first request only; auto and none stay
        }
    }

    static final List<Object> REPLIES = List.of(
        message(List.of(text("Checking all three."), toolUse("toolu_01", "get_weather", map("city", "Oslo")), toolUse("toolu_02", "get_time", map("city", "Oslo")),
            toolUse("toolu_03", "get_weather", map("city", "Atlantis"))), "tool_use"),
        message(List.of(text("In Oslo it is 09:15 and 4 C with light rain. I have no weather data for Atlantis."))));

    public static void main(String[] args) {
        Scripted.Rig rig = Scripted.client(REPLIES.toArray());
        Loop run = loop(rig.client(), "Weather and time in Oslo, and the weather in Atlantis?", ToolChoice.ofAuto(ToolChoiceAuto.builder().disableParallelToolUse(false).build()));
        System.out.println("model calls: " + rig.http().requests.size() + " | stop reasons: " + py(List.of("tool_use", run.reply().stopReason().get().asString())));
        System.out.println("roles after the first reply: " + py(run.messages().stream().map(m -> m.role().asString()).toList()));
        List<ContentBlockParam> results = run.messages().get(2).content().blockParams().get();
        System.out.println("tool results in ONE user message: " + results.size() + " | ids in order: " + py(results.stream().map(r -> r.asToolResult().toolUseId()).toList()));
        for (ContentBlockParam r : results) {
            ToolResultBlockParam result = r.asToolResult();
            System.out.println("  " + result.toolUseId() + ": is_error=" + py(result.isError().orElse(false)) + " content=" + py(result.content().get().string().get()));
        }
        System.out.println("tool_choice sent on requests 1 and 2: " + py(rig.http().requests.stream().map(r -> r.get("tool_choice")).toList()));
        boolean noHandler = true;
        for (var t : rig.http().requests.get(0).get("tools")) {
            java.util.Set<String> fields = new java.util.HashSet<>();
            t.fieldNames().forEachRemaining(fields::add);
            noHandler &= fields.equals(java.util.Set.of("name", "description", "input_schema"));
        }
        System.out.println("tool definitions sent carry no handler: " + py(noHandler));
        System.out.println("final text: " + run.reply().content().get(0).asText().text());
    }
}
```
```text
model calls: 2 | stop reasons: ['tool_use', 'end_turn']
roles after the first reply: ['user', 'assistant', 'user', 'assistant']
tool results in ONE user message: 3 | ids in order: ['toolu_01', 'toolu_02', 'toolu_03']
  toolu_01: is_error=False content='Oslo: 4 C, light rain'
  toolu_02: is_error=False content='09:15'
  toolu_03: is_error=True content="No data for 'Atlantis'. Known cities: Oslo, Rome."
tool_choice sent on requests 1 and 2: [{'type': 'auto', 'disable_parallel_tool_use': False}, {'type': 'auto', 'disable_parallel_tool_use': False}]
tool definitions sent carry no handler: True
final text: In Oslo it is 09:15 and 4 C with light rain. I have no weather data for Atlantis.
```
```kotlin
import com.anthropic.client.AnthropicClient
import com.anthropic.core.JsonValue
import com.anthropic.core.jsonMapper
import com.anthropic.models.messages.ContentBlockParam
import com.anthropic.models.messages.Message
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.MessageParam
import com.anthropic.models.messages.Model
import com.anthropic.models.messages.Tool
import com.anthropic.models.messages.ToolChoice
import com.anthropic.models.messages.ToolChoiceAuto
import com.anthropic.models.messages.ToolResultBlockParam
import com.anthropic.models.messages.ToolUseBlock
import harness.Scripted
import harness.Scripted.map
import harness.Scripted.message
import harness.Scripted.text
import harness.Scripted.toolUse
import harness.Show.py

private val log = System.getLogger("tool_loop")

/**
 * A tool loop on the official SDK, against a scripted model: parallel calls, one failing tool and a tool_choice that is kept.
 *
 * The replies are illustrative, hand-written bodies in the shape of the Messages API (claude-sonnet-5-5), not captures.
 */
const val MODEL = "claude-sonnet-5-5"

fun tool(name: String, description: String): Tool = Tool.builder().name(name).description(description)
    .inputSchema(
        Tool.InputSchema.builder()
            .properties(JsonValue.from(map("city", map("type", "string", "description", "City name, for example Oslo"))))
            .required(listOf("city")).build(),
    ).build()

val TOOLS = listOf(
    tool("get_weather", "Current weather for one city. Use it when the user asks about weather now. Returns a short sentence; it knows nothing about forecasts."),
    tool("get_time", "Local time for one city, as HH:MM on a 24 hour clock. Use it when the user asks what time it is somewhere."),
)

val HANDLERS: Map<String, (Map<*, *>) -> String?> = mapOf(
    "get_weather" to { a -> mapOf("Oslo" to "Oslo: 4 C, light rain")[a["city"]] },
    "get_time" to { a -> mapOf("Oslo" to "09:15", "Rome" to "09:15")[a["city"]] },
)

/** One tool call answered: the result block that goes back to the model. */
fun runTool(block: ToolUseBlock): ToolResultBlockParam {
    val input = jsonMapper().convertValue(block._input(), Map::class.java)
    val answer = HANDLERS.getValue(block.name())(input)
        ?: return ToolResultBlockParam.builder().toolUseId(block.id()).content("No data for '${input["city"]}'. Known cities: Oslo, Rome.").isError(true).build()
    return ToolResultBlockParam.builder().toolUseId(block.id()).content(answer).build()
}

/** The last reply and the whole transcript, as the requests carried it. */
data class Loop(val reply: Message, val messages: List<MessageParam>)

fun loop(client: AnthropicClient, question: String, choice: ToolChoice? = null): Loop {
    val messages = mutableListOf(MessageParam.builder().role(MessageParam.Role.USER).content(question).build())
    var active = choice
    while (true) {
        val request = MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(500).messages(messages.toList()).apply { TOOLS.forEach { addTool(it) } }
        active?.let { request.toolChoice(it) }
        val reply = client.messages().create(request.build())
        messages += reply.toParam()
        if (reply.stopReason().get().asString() != "tool_use") return Loop(reply, messages)
        val results = reply.content().filter { it.isToolUse() }.map { ContentBlockParam.ofToolResult(runTool(it.asToolUse())) }
        messages += MessageParam.builder().role(MessageParam.Role.USER).contentOfBlockParams(results).build()
        if (active != null && (active.isAny() || active.isTool())) active = null // a forced choice applies to the first request only; auto and none stay
    }
}

val REPLIES = listOf<Any>(
    message(
        listOf(text("Checking all three."), toolUse("toolu_01", "get_weather", map("city", "Oslo")), toolUse("toolu_02", "get_time", map("city", "Oslo")), toolUse("toolu_03", "get_weather", map("city", "Atlantis"))),
        "tool_use",
    ),
    message(listOf(text("In Oslo it is 09:15 and 4 C with light rain. I have no weather data for Atlantis."))),
)

fun main() {
    val rig = Scripted.client(*REPLIES.toTypedArray())
    val run = loop(rig.client(), "Weather and time in Oslo, and the weather in Atlantis?", ToolChoice.ofAuto(ToolChoiceAuto.builder().disableParallelToolUse(false).build()))
    println("model calls: ${rig.http().requests.size} | stop reasons: ${py(listOf("tool_use", run.reply.stopReason().get().asString()))}")
    println("roles after the first reply: ${py(run.messages.map { it.role().asString() })}")
    val results = run.messages[2].content().blockParams().get()
    println("tool results in ONE user message: ${results.size} | ids in order: ${py(results.map { it.asToolResult().toolUseId() })}")
    for (r in results) {
        val result = r.asToolResult()
        println("  ${result.toolUseId()}: is_error=${py(result.isError().orElse(false))} content=${py(result.content().get().string().get())}")
    }
    println("tool_choice sent on requests 1 and 2: ${py(rig.http().requests.map { it["tool_choice"] })}")
    println("tool definitions sent carry no handler: ${py(rig.http().requests[0]["tools"].all { t -> t.fieldNames().asSequence().toSet() == setOf("name", "description", "input_schema") })}")
    println("final text: ${run.reply.content()[0].asText().text()}")
}
```
```text
model calls: 2 | stop reasons: ['tool_use', 'end_turn']
roles after the first reply: ['user', 'assistant', 'user', 'assistant']
tool results in ONE user message: 3 | ids in order: ['toolu_01', 'toolu_02', 'toolu_03']
  toolu_01: is_error=False content='Oslo: 4 C, light rain'
  toolu_02: is_error=False content='09:15'
  toolu_03: is_error=True content="No data for 'Atlantis'. Known cities: Oslo, Rome."
tool_choice sent on requests 1 and 2: [{'type': 'auto', 'disable_parallel_tool_use': False}, {'type': 'auto', 'disable_parallel_tool_use': False}]
tool definitions sent carry no handler: True
final text: In Oslo it is 09:15 and 4 C with light rain. I have no weather data for Atlantis.
```
<!-- /example -->

Read the output. The model was called twice, with stop reasons `tool_use` and `end_turn`. After the first reply the message list is
user, assistant, user, assistant, and the three results sit in one user message with the ids `toolu_01` to `toolu_03` in order. The
tool definitions sent carry no handler: the model sees the schema, and "never sees your implementation". `auto` was sent unchanged on both requests; a forced choice would be dropped here after the first request (the practice sends
`auto` instead), which is the course's own rule and is covered on the next page.

Java and Kotlin readers: the practice on the third page builds this loop in your language.

## Traps

1. **Text before the first result.** The user message that carries results starts with the `tool_result` blocks; text comes after.
2. **Sending results one message at a time.** Return every result together in the next user message.
3. **Swallowing a failure.** An exception that becomes an empty string tells the model the call worked. Send `is_error` with a message
   that says what went wrong and what to try.
4. **No turn limit.** Keying only on `stop_reason` leaves the loop at the mercy of the model. Count the calls.

## Quiz

1. A handler for a database lookup throws because the row does not exist. What should the loop do?
   - **a**: End the conversation and return the exception to the caller
   - **b**: Send a result flagged as an error whose message says what to try
   - **c**: Return an empty string, so the loop can carry on with the turn
   - **d**: Retry the lookup inside the handler until the row turns up

2. The assistant turn holds two `tool_use` blocks. How does the next request carry the outputs?
   - **a**: Two user messages, one result in each, in the order of the calls
   - **b**: One assistant message in which both outputs are recorded as text
   - **c**: One user message that opens with a sentence and then lists the results
   - **d**: One user message with both results first and any text after them

3. A request that enables a server tool gets a reply with `stop_reason` of `pause_turn`. What should the loop do next?
   - **a**: Treat the reply as a refusal and stop the loop with that status
   - **b**: Pass the assistant content back untouched, and call once more
   - **c**: Run the missing tool and return its output in a `tool_result` block
   - **d**: Raise `max_tokens` and repeat the request that produced the pause

<details>
<summary>Answer key</summary>

1. **b**. The page says "Write instructive error messages", with "what went wrong and what Claude should try next". *a* is ruled out because the loop is to "continue the conversation forward with a `tool_result` that indicates the error", not to end it. *c* is ruled out because "An exception that becomes an empty string tells the model the call worked." *d* is ruled out because the row does not exist, and when the handler throws "the loop catches it and sends the message back with `"is_error": true`", so the model learns of the failure at once.
2. **d**. The page says "In the user message containing tool results, the tool_result blocks must come FIRST in the content array." *a* is ruled out because the trap is "Sending results one message at a time", and the rule is to "Return every result together in the next user message". *c* is ruled out because a sentence before the first result "will cause a 400 error". *b* is ruled out because results are sent by the user: "a user message with the `tool_result` blocks".
3. **b**. The page says to send "the assistant response back as it is, keep the same `tools` array". *a* is ruled out because the table gives a refusal its own reaction: "status `refused`; the same request is not repeated". *c* is ruled out because a paused turn "is different from `tool_use`, which requires `tool_result` blocks". *d* is ruled out because a raised limit belongs to a cut-off reply: "a cut-off `tool_use` block needs a higher `max_tokens`".

</details>
