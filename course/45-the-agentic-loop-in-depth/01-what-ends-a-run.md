# What ends a run: control flow on the stop reason

**Level:** Architect · **Module 45:** The agentic loop, in depth · **Page 1 of 2**
**Exams:** A1.1 (A1)

**After this page you can** say which decisions in an agent loop belong to the model and which to your code, write the loop so that the stop reason alone ends a run, recognise the two loop anti-patterns that the Architect exam describes (stopping on words in the text, and stopping on an iteration count), and give the iteration count its proper job as a backstop with a status of its own.

Checked on 2026-10-03 against the Claude Code documentation page "How the agent loop works", the Claude API pages "Handle tool calls" and "Stop reasons and fallback", and Anthropic's engineering article on building effective agents. The example runs offline in Python (`anthropic` 1.11.0) and TypeScript (`@anthropic-ai/sdk` 0.131.0) against scripted replies in the shape of the Messages API, so it shows no live output. This page deepens module 26, page 2, where the loop was first written, and module 35, page 1, where the Agent SDK ran it for you.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* the loop continues while the stop reason is `tool_use` and ends when it is `end_turn`; the results of the tools are added to the conversation between iterations so that the model can reason about its next action; and three anti-patterns are named, which are reading natural-language signals to decide when to stop, an arbitrary iteration cap as the main stopping mechanism, and treating the presence of assistant text as a sign of completion. *What the current product does (documentation checked 2026-10-03):* the pair is the common case, but the API documents more values (`stop_sequence`, `max_tokens`, `refusal`, and `pause_turn` for server tools), so a loop written for two values mishandles a cut-off or refused reply; and an iteration limit is a documented control (`max_turns` in the Agent SDK, with its own result subtype) whose proper job is the backstop described on this page. On the exam, pick the option that continues on `tool_use` and ends on `end_turn`; in your own code, give every other value a status of its own.

## Why it matters

The first Architect domain is agentic architecture, and its first task is the loop. The exam does not ask you to write one. It shows a loop that looks reasonable and asks what will go wrong: a loop that stops when the model says it is finished, a loop that runs "at most five" times. In production the same mistakes are quieter. A run is cut off in the middle of a task and reported as complete, a tool never runs because the model said "done" a sentence too early, a runaway run is stopped only by the bill. All three come from asking the wrong thing to end the loop.

## The idea

### One loop, two owners

The documentation describes the loop in the same terms on both its pages. The Claude Code page: "Turns continue until Claude produces output with no tool calls, at which point the loop ends and the final result is delivered." The API page: a response with `stop_reason` of `tool_use` carries one or more `tool_use` blocks, you run them, and you send the results back in a user message. The model sends back a reply that has no tool call when it has decided that the task is done. So the loop has one natural end, and the model owns it.

Your code owns everything around that end. The split is the first thing to be able to say:

| Decision | Who owns it | Where it lives in the loop |
|---|---|---|
| Is the task finished? | The model | The stop reason of its reply (`end_turn`, or no tool call in the Agent SDK) |
| Does this tool call run? | Your code | Permissions and hooks, before the tool executes |
| What does the model learn from a failure? | Your code and the model | An error result with a message that says what to try next |
| How long may the run go on? | Your code | A turn limit and a budget, as a backstop |
| What do you do when the run did not finish normally? | Your code | A status for each way out (truncated, refused, over the limit) |

A well-built loop never asks the model to hand back control in a second channel, such as a magic word, a count or a flag in the text. The stop reason is the channel.

### The values that end a run

The "Stop reasons and fallback" page lists what a reply can end with. The loop continues on one of them and leaves on all the others.

| `stop_reason` | The page's meaning | What the loop does |
|---|---|---|
| `tool_use` | "Claude is calling a tool." | Run the calls and send the results; this is the only value that continues |
| `end_turn` | "Claude finished its response naturally." | Leave: the run is done |
| `stop_sequence` | "Claude emitted one of your `stop_sequences`." | Leave: done, and read which sequence fired if it matters |
| `max_tokens` | "The response reached your `max_tokens` limit." | Leave with a status of its own: the reply is cut off, and raising the limit is a decision for your code |
| `refusal` | "Claude declined to respond." | Leave with a status of its own; a retry on a fallback model is a separate option (module 43) |

A value the loop has never seen must not be guessed at. The practice of this module returns the status `unexpected` for it, which a log line and an alert can find, instead of calling the model again or calling the run done. (Module 26 also covers `pause_turn`, for a server-tool loop that hit its own limit.)

### Anti-pattern one: stopping on the words

A loop that reads the assistant's text to decide whether the work is finished looks natural, because the text is the thing the user will read. It fails in both directions, and the documentation's own example response shows how. A reply can carry text and a tool call together: "I'll check the current weather in San Francisco for you." sits in front of a `tool_use` block, and the `stop_reason` is `tool_use`. If the model writes "All done with the analysis. Saving it now." in front of a `save_file` call, a loop that looks for "done" stops and the file is never saved. The opposite also happens: a reply that ends its turn can say "Next I will call the lookup tool." and still be the model's last word, and a loop that looks for "I will call" waits for something that never comes.

The text is for the person. It changes with the model, the prompt, the language and the day. The stop reason is part of the protocol: a documented set of values, set by the model's own decision to ask for a tool or not. A control flow that needs the text to contain a particular word also moves the loop's logic into the prompt, where nobody tests it.

### Anti-pattern two: the count as the loop

The other mistake is a loop of fixed length: `for _ in range(5)`, or "call the model up to five times and return the last reply". It is easy to write and it has two faults. A task that needs six steps is cut off in the middle, and the code returns whatever the fifth reply held as though it were an answer. A task that needs two steps is fine, which is why the loop passes its tests and fails in production on the first long task.

The engineering article keeps the count in its proper place: "It's also common to include stopping conditions (such as a maximum number of iterations) to maintain control." The words are *include* and *maintain control*. The loop ends because the model ended it. A count is added so that a run that does not converge is stopped. The count therefore needs three properties that the fixed loop lacks:

1. It is **generous**: set above what real tasks need (in this course's advice, taken from the number of turns your evaluation set actually uses, with room above it), so that a normal run never meets it.
2. It has **its own status**. A run that meets the count reports `max_turns`, and the caller can tell it from `done`. The Agent SDK does the same: its `max_turns` "counts tool-use turns only", and a run that reaches it ends with the result subtype `error_max_turns`, which has no `result` text.
3. It is **checked before the next call**, so that a reply that ends the turn on the very last allowed call is still `done`.

When the status `max_turns` shows up often, the count is not the thing to change. A run that does not converge usually has a tool that keeps failing, a description the model cannot use or a task that is too big. The status is a signal to read the conversation, which the practice keeps whole for that reason.

### The example

The example runs three loops over the same scripted replies. In scenario A the first reply says "All done" in its text and also calls `save_file`. In scenario B a task needs four lookups before the model ends its turn. The stop-reason loop is run with two backstops: a generous one and one that is too small.

<!-- example: m45-loop-control-flow tabs: python,typescript,java,kotlin -->
```python
"""Three loops over the same scripted replies: one ends on stop_reason, one on a word in the text, one after a fixed count.

The replies are illustrative, hand-written bodies in the shape of the Messages API (claude-sonnet-5-5), not captures.
Only the first loop is right; the other two show what the two anti-patterns of the Architect exam do to a run.
"""
import logging
from harness import scripted_client
from harness.scripted import message, text, tool_use

log = logging.getLogger(__name__)

MODEL = "claude-sonnet-5-5"
TOOLS = [
    {"name": "save_file", "description": "Save the report under a file name. Use it once the report is written.",
     "input_schema": {"type": "object", "properties": {"file": {"type": "string"}}, "required": ["file"]}},
    {"name": "lookup", "description": "Look up item number n and return its record.",
     "input_schema": {"type": "object", "properties": {"n": {"type": "integer"}}, "required": ["n"]}},
]


def run_tools(reply, ran):
    results = []
    for block in reply.content:
        if block.type == "tool_use":
            ran.append(block.name)
            results.append({"type": "tool_result", "tool_use_id": block.id, "content": f"{block.name} ok"})
    return results


def text_of(reply):
    return "".join(b.text for b in reply.content if b.type == "text")


def by_stop_reason(client, task, backstop=10):
    """Right: the model's own stop_reason decides. The count is only a backstop with a status of its own."""
    messages, ran = [{"role": "user", "content": task}], []
    for call in range(1, backstop + 1):
        reply = client.messages.create(model=MODEL, max_tokens=500, tools=TOOLS, messages=messages)
        messages.append({"role": "assistant", "content": reply.content})
        if reply.stop_reason != "tool_use":
            return "done", call, ran, text_of(reply)
        messages.append({"role": "user", "content": run_tools(reply, ran)})
    return "max_turns", backstop, ran, text_of(reply)


def by_text_marker(client, task):
    """Wrong: it reads the words. A reply that says 'done' ends the run, even with a tool call in the same reply."""
    messages, ran, call = [{"role": "user", "content": task}], [], 0
    while True:
        call += 1
        reply = client.messages.create(model=MODEL, max_tokens=500, tools=TOOLS, messages=messages)
        messages.append({"role": "assistant", "content": reply.content})
        if "done" in text_of(reply).lower():
            return "done", call, ran, text_of(reply)
        messages.append({"role": "user", "content": run_tools(reply, ran)})


def by_fixed_count(client, task, turns=3):
    """Wrong: the count is the loop. Whatever the third reply holds is returned as the answer."""
    messages, ran = [{"role": "user", "content": task}], []
    for call in range(1, turns + 1):
        reply = client.messages.create(model=MODEL, max_tokens=500, tools=TOOLS, messages=messages)
        messages.append({"role": "assistant", "content": reply.content})
        if reply.stop_reason == "tool_use":
            messages.append({"role": "user", "content": run_tools(reply, ran)})
    return "done", turns, ran, text_of(reply)


def scenario_a():
    return [message([text("All done with the analysis. Saving it now."), tool_use("toolu_01", "save_file", file="report.txt")], stop_reason="tool_use", model=MODEL),
            message([text("Saved report.txt.")], model=MODEL)]


def scenario_b():
    return [message([tool_use(f"toolu_0{n}", "lookup", n=n)], stop_reason="tool_use", model=MODEL) for n in range(1, 5)] + [message([text("Looked up 4 items.")], model=MODEL)]


def show(label, outcome):
    status, calls, ran, answer = outcome
    print(f"  {label:<26} status={status:<9} model calls={calls}  tools run={len(ran)}  text={answer!r}")


def main():
    print("A: the reply says 'All done' and also calls save_file")
    show("stop_reason loop", by_stop_reason(scripted_client(*scenario_a())[0], "Write and save the report."))
    show("text-marker loop", by_text_marker(scripted_client(*scenario_a())[0], "Write and save the report."))
    print("B: the task needs four lookups, then the model ends its turn")
    show("stop_reason loop, cap 10", by_stop_reason(scripted_client(*scenario_b())[0], "Look up items 1 to 4."))
    show("stop_reason loop, cap 3", by_stop_reason(scripted_client(*scenario_b())[0], "Look up items 1 to 4.", backstop=3))
    show("fixed-count loop of 3", by_fixed_count(scripted_client(*scenario_b())[0], "Look up items 1 to 4."))


if __name__ == "__main__":
    main()
```
```text
A: the reply says 'All done' and also calls save_file
  stop_reason loop           status=done      model calls=2  tools run=1  text='Saved report.txt.'
  text-marker loop           status=done      model calls=1  tools run=0  text='All done with the analysis. Saving it now.'
B: the task needs four lookups, then the model ends its turn
  stop_reason loop, cap 10   status=done      model calls=5  tools run=4  text='Looked up 4 items.'
  stop_reason loop, cap 3    status=max_turns model calls=3  tools run=3  text=''
  fixed-count loop of 3      status=done      model calls=3  tools run=3  text=''
```
```typescript
// Three loops over the same scripted replies: one ends on stop_reason, one on a word in the text, one after a fixed count.
//
// The replies are illustrative, hand-written bodies in the shape of the Messages API (claude-sonnet-5-5), not captures.
// Only the first loop is right; the other two show what the two anti-patterns of the Architect exam do to a run.
import Anthropic from "@anthropic-ai/sdk";
import { message, scriptedFetch, text } from "../../../harness/ts/scriptedFetch.ts";
import { logger } from "./logger.ts";
const log = logger("loops");

export const MODEL = "claude-sonnet-5-5";
const TOOLS: Anthropic.Tool[] = [
  { name: "save_file", description: "Save the report under a file name. Use it once the report is written.",
    input_schema: { type: "object", properties: { file: { type: "string" } }, required: ["file"] } },
  { name: "lookup", description: "Look up item number n and return its record.",
    input_schema: { type: "object", properties: { n: { type: "integer" } }, required: ["n"] } },
];

type Outcome = [status: string, calls: number, ran: string[], answer: string];

function runTools(reply: Anthropic.Message, ran: string[]): Anthropic.ToolResultBlockParam[] {
  const results: Anthropic.ToolResultBlockParam[] = [];
  for (const block of reply.content) {
    if (block.type === "tool_use") {
      ran.push(block.name);
      results.push({ type: "tool_result", tool_use_id: block.id, content: `${block.name} ok` });
    }
  }
  return results;
}

const textOf = (reply: Anthropic.Message) => reply.content.map((b) => (b.type === "text" ? b.text : "")).join("");
const ask = (client: Anthropic, messages: Anthropic.MessageParam[]) => client.messages.create({ model: MODEL, max_tokens: 500, tools: TOOLS, messages });

/** Right: the model's own stop_reason decides. The count is only a backstop with a status of its own. */
export async function byStopReason(client: Anthropic, task: string, backstop = 10): Promise<Outcome> {
  const messages: Anthropic.MessageParam[] = [{ role: "user", content: task }];
  const ran: string[] = [];
  let reply!: Anthropic.Message;
  for (let call = 1; call <= backstop; call++) {
    reply = await ask(client, messages);
    messages.push({ role: "assistant", content: reply.content });
    if (reply.stop_reason !== "tool_use") return ["done", call, ran, textOf(reply)];
    messages.push({ role: "user", content: runTools(reply, ran) });
  }
  return ["max_turns", backstop, ran, textOf(reply)];
}

/** Wrong: it reads the words. A reply that says 'done' ends the run, even with a tool call in the same reply. */
export async function byTextMarker(client: Anthropic, task: string): Promise<Outcome> {
  const messages: Anthropic.MessageParam[] = [{ role: "user", content: task }];
  const ran: string[] = [];
  for (let call = 1; ; call++) {
    const reply = await ask(client, messages);
    messages.push({ role: "assistant", content: reply.content });
    if (textOf(reply).toLowerCase().includes("done")) return ["done", call, ran, textOf(reply)];
    messages.push({ role: "user", content: runTools(reply, ran) });
  }
}

/** Wrong: the count is the loop. Whatever the third reply holds is returned as the answer. */
export async function byFixedCount(client: Anthropic, task: string, turns = 3): Promise<Outcome> {
  const messages: Anthropic.MessageParam[] = [{ role: "user", content: task }];
  const ran: string[] = [];
  let reply!: Anthropic.Message;
  for (let call = 1; call <= turns; call++) {
    reply = await ask(client, messages);
    messages.push({ role: "assistant", content: reply.content });
    if (reply.stop_reason === "tool_use") messages.push({ role: "user", content: runTools(reply, ran) });
  }
  return ["done", turns, ran, textOf(reply)];
}

const usage = { input_tokens: 1, output_tokens: 1 };
export const scenarioA = () => [
  { body: message([text("All done with the analysis. Saving it now."), { type: "tool_use", id: "toolu_01", name: "save_file", input: { file: "report.txt" } }], "tool_use", usage, MODEL) },
  { body: message([text("Saved report.txt.")], "end_turn", usage, MODEL) },
];
export const scenarioB = () => [1, 2, 3, 4].map((n) => ({ body: message([{ type: "tool_use", id: `toolu_0${n}`, name: "lookup", input: { n } }], "tool_use", usage, MODEL) }))
  .concat([{ body: message([text("Looked up 4 items.")], "end_turn", usage, MODEL) }]);

export function clientFor(replies: Array<Record<string, unknown>>) {
  const fake = scriptedFetch(replies as any);
  return { fake, client: new Anthropic({ apiKey: "placeholder", maxRetries: 0, fetch: fake.fetch }) };
}

/** Python's repr of a string, so that both languages print the same lines. */
const py = (s: string) => (s.includes("'") && !s.includes('"') ? `"${s}"` : `'${s}'`);

function show(label: string, [status, calls, ran, answer]: Outcome) {
  console.log(`  ${label.padEnd(26)} status=${status.padEnd(9)} model calls=${calls}  tools run=${ran.length}  text=${py(answer)}`);
}

async function main() {
  console.log("A: the reply says 'All done' and also calls save_file");
  show("stop_reason loop", await byStopReason(clientFor(scenarioA()).client, "Write and save the report."));
  show("text-marker loop", await byTextMarker(clientFor(scenarioA()).client, "Write and save the report."));
  console.log("B: the task needs four lookups, then the model ends its turn");
  show("stop_reason loop, cap 10", await byStopReason(clientFor(scenarioB()).client, "Look up items 1 to 4."));
  show("stop_reason loop, cap 3", await byStopReason(clientFor(scenarioB()).client, "Look up items 1 to 4.", 3));
  show("fixed-count loop of 3", await byFixedCount(clientFor(scenarioB()).client, "Look up items 1 to 4."));
}

if (import.meta.main) await main();
```
```text
A: the reply says 'All done' and also calls save_file
  stop_reason loop           status=done      model calls=2  tools run=1  text='Saved report.txt.'
  text-marker loop           status=done      model calls=1  tools run=0  text='All done with the analysis. Saving it now.'
B: the task needs four lookups, then the model ends its turn
  stop_reason loop, cap 10   status=done      model calls=5  tools run=4  text='Looked up 4 items.'
  stop_reason loop, cap 3    status=max_turns model calls=3  tools run=3  text=''
  fixed-count loop of 3      status=done      model calls=3  tools run=3  text=''
```
```java
import static harness.Scripted.map;
import static harness.Scripted.message;
import static harness.Scripted.text;
import static harness.Scripted.toolUse;
import static harness.Show.py;

import com.anthropic.client.AnthropicClient;
import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.MessageParam;
import com.anthropic.models.messages.Model;
import com.anthropic.models.messages.Tool;
import com.anthropic.models.messages.ToolResultBlockParam;
import harness.Scripted;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Three loops over the same scripted replies: one ends on stop_reason, one on a word in the text, one after a fixed count.
 *
 * <p>The replies are illustrative, hand-written bodies in the shape of the Messages API (claude-sonnet-5-5), not captures.
 * Only the first loop is right; the other two show what the two anti-patterns of the Architect exam do to a run.
 */
public final class Loops {
    private static final System.Logger LOG = System.getLogger(Loops.class.getName());
    static final String MODEL = "claude-sonnet-5-5";

    static Tool tool(String name, String description, String property, String type) {
        return Tool.builder().name(name).description(description)
            .inputSchema(Tool.InputSchema.builder().properties(JsonValue.from(map(property, map("type", type)))).required(List.of(property)).build())
            .build();
    }

    static final List<Tool> TOOLS = List.of(
        tool("save_file", "Save the report under a file name. Use it once the report is written.", "file", "string"),
        tool("lookup", "Look up item number n and return its record.", "n", "integer"));

    /** What a loop returns: the status, the number of model calls, the tools that ran, and the last reply's text. */
    record Outcome(String status, int calls, List<String> ran, String answer) {}

    static Message call(AnthropicClient client, List<MessageParam> messages) {
        MessageCreateParams.Builder request = MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(500).messages(messages);
        TOOLS.forEach(request::addTool);
        Message reply = client.messages().create(request.build());
        messages.add(reply.toParam());
        return reply;
    }

    static List<ContentBlockParam> runTools(Message reply, List<String> ran) {
        List<ContentBlockParam> results = new ArrayList<>();
        for (ContentBlock b : reply.content()) {
            if (!b.isToolUse()) continue;
            ran.add(b.asToolUse().name());
            results.add(ContentBlockParam.ofToolResult(ToolResultBlockParam.builder().toolUseId(b.asToolUse().id()).content(b.asToolUse().name() + " ok").build()));
        }
        return results;
    }

    static String textOf(Message reply) {
        StringBuilder out = new StringBuilder();
        for (ContentBlock b : reply.content()) if (b.isText()) out.append(b.asText().text());
        return out.toString();
    }

    static MessageParam user(String task) {
        return MessageParam.builder().role(MessageParam.Role.USER).content(task).build();
    }

    static MessageParam results(List<ContentBlockParam> blocks) {
        return MessageParam.builder().role(MessageParam.Role.USER).contentOfBlockParams(blocks).build();
    }

    /** Right: the model's own stop_reason decides. The count is only a backstop with a status of its own. */
    static Outcome byStopReason(AnthropicClient client, String task, int backstop) {
        List<MessageParam> messages = new ArrayList<>(List.of(user(task)));
        List<String> ran = new ArrayList<>();
        Message reply = null;
        for (int call = 1; call <= backstop; call++) {
            reply = call(client, messages);
            if (!reply.stopReason().get().asString().equals("tool_use")) return new Outcome("done", call, ran, textOf(reply));
            messages.add(results(runTools(reply, ran)));
        }
        return new Outcome("max_turns", backstop, ran, textOf(reply));
    }

    static Outcome byStopReason(AnthropicClient client, String task) {
        return byStopReason(client, task, 10);
    }

    /** Wrong: it reads the words. A reply that says 'done' ends the run, even with a tool call in the same reply. */
    static Outcome byTextMarker(AnthropicClient client, String task) {
        List<MessageParam> messages = new ArrayList<>(List.of(user(task)));
        List<String> ran = new ArrayList<>();
        int call = 0;
        while (true) {
            call++;
            Message reply = call(client, messages);
            if (textOf(reply).toLowerCase().contains("done")) return new Outcome("done", call, ran, textOf(reply));
            messages.add(results(runTools(reply, ran)));
        }
    }

    /** Wrong: the count is the loop. Whatever the third reply holds is returned as the answer. */
    static Outcome byFixedCount(AnthropicClient client, String task, int turns) {
        List<MessageParam> messages = new ArrayList<>(List.of(user(task)));
        List<String> ran = new ArrayList<>();
        Message reply = null;
        for (int call = 1; call <= turns; call++) {
            reply = call(client, messages);
            if (reply.stopReason().get().asString().equals("tool_use")) messages.add(results(runTools(reply, ran)));
        }
        return new Outcome("done", turns, ran, textOf(reply));
    }

    static Outcome byFixedCount(AnthropicClient client, String task) {
        return byFixedCount(client, task, 3);
    }

    static Object[] scenarioA() {
        return new Object[] {
            message(List.of(text("All done with the analysis. Saving it now."), toolUse("toolu_01", "save_file", map("file", "report.txt"))), "tool_use"),
            message(List.of(text("Saved report.txt.")))};
    }

    static Object[] scenarioB() {
        List<Object> replies = new ArrayList<>();
        for (int n = 1; n <= 4; n++) replies.add(message(List.of(toolUse("toolu_0" + n, "lookup", map("n", n))), "tool_use"));
        replies.add(message(List.of(text("Looked up 4 items."))));
        return replies.toArray();
    }

    static void show(String label, Outcome o) {
        System.out.println(String.format("  %-26s status=%-9s model calls=%d  tools run=%d  text=%s", label, o.status(), o.calls(), o.ran().size(), py(o.answer())));
    }

    public static void main(String[] args) {
        System.out.println("A: the reply says 'All done' and also calls save_file");
        show("stop_reason loop", byStopReason(Scripted.client(scenarioA()).client(), "Write and save the report."));
        show("text-marker loop", byTextMarker(Scripted.client(scenarioA()).client(), "Write and save the report."));
        System.out.println("B: the task needs four lookups, then the model ends its turn");
        show("stop_reason loop, cap 10", byStopReason(Scripted.client(scenarioB()).client(), "Look up items 1 to 4."));
        show("stop_reason loop, cap 3", byStopReason(Scripted.client(scenarioB()).client(), "Look up items 1 to 4.", 3));
        show("fixed-count loop of 3", byFixedCount(Scripted.client(scenarioB()).client(), "Look up items 1 to 4."));
    }
}
```
```text
A: the reply says 'All done' and also calls save_file
  stop_reason loop           status=done      model calls=2  tools run=1  text='Saved report.txt.'
  text-marker loop           status=done      model calls=1  tools run=0  text='All done with the analysis. Saving it now.'
B: the task needs four lookups, then the model ends its turn
  stop_reason loop, cap 10   status=done      model calls=5  tools run=4  text='Looked up 4 items.'
  stop_reason loop, cap 3    status=max_turns model calls=3  tools run=3  text=''
  fixed-count loop of 3      status=done      model calls=3  tools run=3  text=''
```
```kotlin
import com.anthropic.client.AnthropicClient
import com.anthropic.core.JsonValue
import com.anthropic.models.messages.ContentBlockParam
import com.anthropic.models.messages.Message
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.MessageParam
import com.anthropic.models.messages.Model
import com.anthropic.models.messages.Tool
import com.anthropic.models.messages.ToolResultBlockParam
import harness.Scripted
import harness.Scripted.map
import harness.Scripted.message
import harness.Scripted.text
import harness.Scripted.toolUse
import harness.Show.py

private val log = System.getLogger("loops")

/**
 * Three loops over the same scripted replies: one ends on stop_reason, one on a word in the text, one after a fixed count.
 *
 * The replies are illustrative, hand-written bodies in the shape of the Messages API (claude-sonnet-5-5), not captures.
 * Only the first loop is right; the other two show what the two anti-patterns of the Architect exam do to a run.
 */
const val MODEL = "claude-sonnet-5-5"

fun tool(name: String, description: String, property: String, type: String): Tool = Tool.builder().name(name).description(description)
    .inputSchema(Tool.InputSchema.builder().properties(JsonValue.from(map(property, map("type", type)))).required(listOf(property)).build()).build()

val TOOLS = listOf(
    tool("save_file", "Save the report under a file name. Use it once the report is written.", "file", "string"),
    tool("lookup", "Look up item number n and return its record.", "n", "integer"),
)

/** What a loop returns: the status, the number of model calls, the tools that ran, and the last reply's text. */
data class Outcome(val status: String, val calls: Int, val ran: List<String>, val answer: String)

fun ask(client: AnthropicClient, messages: MutableList<MessageParam>): Message {
    val request = MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(500).messages(messages.toList()).apply { TOOLS.forEach { addTool(it) } }
    val reply = client.messages().create(request.build())
    messages += reply.toParam()
    return reply
}

fun runTools(reply: Message, ran: MutableList<String>): List<ContentBlockParam> = reply.content().filter { it.isToolUse() }.map {
    val use = it.asToolUse()
    ran += use.name()
    ContentBlockParam.ofToolResult(ToolResultBlockParam.builder().toolUseId(use.id()).content("${use.name()} ok").build())
}

fun textOf(reply: Message): String = reply.content().filter { it.isText() }.joinToString("") { it.asText().text() }

fun user(task: String): MessageParam = MessageParam.builder().role(MessageParam.Role.USER).content(task).build()

fun results(blocks: List<ContentBlockParam>): MessageParam = MessageParam.builder().role(MessageParam.Role.USER).contentOfBlockParams(blocks).build()

/** Right: the model's own stop_reason decides. The count is only a backstop with a status of its own. */
fun byStopReason(client: AnthropicClient, task: String, backstop: Int = 10): Outcome {
    val messages = mutableListOf(user(task))
    val ran = mutableListOf<String>()
    var reply: Message? = null
    for (call in 1..backstop) {
        reply = ask(client, messages)
        if (reply.stopReason().get().asString() != "tool_use") return Outcome("done", call, ran, textOf(reply))
        messages += results(runTools(reply, ran))
    }
    return Outcome("max_turns", backstop, ran, textOf(reply!!))
}

/** Wrong: it reads the words. A reply that says 'done' ends the run, even with a tool call in the same reply. */
fun byTextMarker(client: AnthropicClient, task: String): Outcome {
    val messages = mutableListOf(user(task))
    val ran = mutableListOf<String>()
    var call = 0
    while (true) {
        call++
        val reply = ask(client, messages)
        if ("done" in textOf(reply).lowercase()) return Outcome("done", call, ran, textOf(reply))
        messages += results(runTools(reply, ran))
    }
}

/** Wrong: the count is the loop. Whatever the third reply holds is returned as the answer. */
fun byFixedCount(client: AnthropicClient, task: String, turns: Int = 3): Outcome {
    val messages = mutableListOf(user(task))
    val ran = mutableListOf<String>()
    var reply: Message? = null
    for (call in 1..turns) {
        reply = ask(client, messages)
        if (reply.stopReason().get().asString() == "tool_use") messages += results(runTools(reply, ran))
    }
    return Outcome("done", turns, ran, textOf(reply!!))
}

fun scenarioA(): Array<Any> = arrayOf(
    message(listOf(text("All done with the analysis. Saving it now."), toolUse("toolu_01", "save_file", map("file", "report.txt"))), "tool_use"),
    message(listOf(text("Saved report.txt."))),
)

fun scenarioB(): Array<Any> = (1..4).map<Int, Any> { n -> message(listOf(toolUse("toolu_0$n", "lookup", map("n", n))), "tool_use") }
    .plus(message(listOf(text("Looked up 4 items.")))).toTypedArray()

fun show(label: String, o: Outcome) {
    println("  %-26s status=%-9s model calls=%d  tools run=%d  text=%s".format(label, o.status, o.calls, o.ran.size, py(o.answer)))
}

fun main() {
    println("A: the reply says 'All done' and also calls save_file")
    show("stop_reason loop", byStopReason(Scripted.client(*scenarioA()).client(), "Write and save the report."))
    show("text-marker loop", byTextMarker(Scripted.client(*scenarioA()).client(), "Write and save the report."))
    println("B: the task needs four lookups, then the model ends its turn")
    show("stop_reason loop, cap 10", byStopReason(Scripted.client(*scenarioB()).client(), "Look up items 1 to 4."))
    show("stop_reason loop, cap 3", byStopReason(Scripted.client(*scenarioB()).client(), "Look up items 1 to 4.", backstop = 3))
    show("fixed-count loop of 3", byFixedCount(Scripted.client(*scenarioB()).client(), "Look up items 1 to 4."))
}
```
```text
A: the reply says 'All done' and also calls save_file
  stop_reason loop           status=done      model calls=2  tools run=1  text='Saved report.txt.'
  text-marker loop           status=done      model calls=1  tools run=0  text='All done with the analysis. Saving it now.'
B: the task needs four lookups, then the model ends its turn
  stop_reason loop, cap 10   status=done      model calls=5  tools run=4  text='Looked up 4 items.'
  stop_reason loop, cap 3    status=max_turns model calls=3  tools run=3  text=''
  fixed-count loop of 3      status=done      model calls=3  tools run=3  text=''
```
<!-- /example -->

Read the output line by line. In scenario A the stop-reason loop makes two model calls and runs the tool, and the text-marker loop stops after one call with no tool run, having reported `done` for work it never did. In scenario B the stop-reason loop finishes in five calls under a generous cap. With a cap of three it stops with `max_turns` and an empty text, which tells the caller that it is not an answer. The fixed-count loop of three also stops after three calls, but it says `done`, and its text is empty: nothing in its report says that it was cut off. Both languages print the same lines.

## Traps

These are the wrong answers that the exam's options for this task statement offer, each with the reason it is rejected.

1. **"Stop when the reply says it is done."** It is tempting because the text is what the user reads. The exam rejects it: a reply can say "done" in front of a tool call, and a reply that ends the turn can say "next I will call a tool". Decide on `stop_reason`, and run every tool call the reply holds.
2. **"Loop at most five times and return the last reply."** It is tempting because it cannot run away. The exam rejects a count as the primary stopper: a long task is cut off half done and a partial reply is reported as the answer. Keep the count as a backstop, above the real need, with a status that is not `done`.
3. **"Return the text of the reply as the answer."** It is tempting because every reply has text. The exam rejects it: the text in front of a `tool_use` block is narration, and only the reply that ends the turn holds the answer.

## Quiz

1. A support agent's loop ends when the reply text contains "Case closed", a phrase the prompt tells the model to write last. In testing, some refunds are never issued, because the model writes the phrase in the same reply as the refund call. Which redesign fits best?
   - **a**: Let the stop value of each response decide, and run every tool request it holds
   - **b**: Tell the model in the prompt to write its closing phrase only in a final message
   - **c**: Search the text for more completion phrases, such as "finished" and "complete"
   - **d**: Call the model a fixed number of times, large enough for the longest case

2. An insurance agent handles claims that take between three and nine tool calls. Its loop calls the model four times and returns whatever the fourth reply says. Longer claims are returned as unfinished half-answers marked as complete. Which change fits best?
   - **a**: Remove every limit so that no claim is ever cut off before it has finished
   - **b**: Raise the count to nine so that even the longest claim fits inside the loop itself
   - **c**: End on each response's stop value, and add a roomy ceiling with its own outcome
   - **d**: Ask the model to write the number of steps still needed at the end of each reply

<details>
<summary>Answer key</summary>

1. **a**. The model ends its turn by not asking for a tool, and the stop reason reports exactly that, so a reply that holds a tool call is not the end whatever its text says. *b* is ruled out because a closing phrase in the prompt keeps the control flow in the text: a control flow that needs a particular word "moves the loop's logic into the prompt, where nobody tests it." *c* is ruled out because more phrases do not make the text a protocol: "It changes with the model, the prompt, the language and the day." *d* is ruled out because a fixed count is the second anti-pattern: "A task that needs six steps is cut off in the middle".
2. **c**. The model's own stop value ends a normal run, and the ceiling is only a backstop with a status that is not `done`. *b* is ruled out because the count would still be the loop: "The loop ends because the model ended it." *a* is ruled out because a run that never converges needs something to stop it: "A count is added so that a run that does not converge is stopped." *d* is ruled out because control must not move into the text: "Only the reply that ends the turn holds the answer."

</details>
