# Prompt or code: where a guarantee lives

**Level:** Architect · **Module 48:** Multi-step workflows with guarantees · **Page 1 of 2**
**Exams:** A1.4; S1

**After this page you can** say why an instruction in a prompt is not a guarantee and where one lives instead, write the gate that enforces a prerequisite between two tools in the code that runs them, choose what the gate sends back to the model when it refuses, and explain why a forced `tool_choice` does not stand in for the gate.

Checked on 2026-10-03 against the Claude Code documentation pages "Automate actions with hooks" and "Subagents in the SDK", the Claude API pages "Handle tool calls" and "Define tools", and Anthropic's engineering article on building effective agents. The example runs offline in Python (`anthropic` 1.11.0) and TypeScript (`@anthropic-ai/sdk` 0.131.0) against scripted replies in the shape of the Messages API, so it shows no live output. The scenario is the exam's S1, a customer support resolution agent. This page builds on module 26 (the tool loop), module 41 (least privilege and hooks) and module 45 (what the loop owns).

## Why it matters

Scenario S1 of the Architect exam is a support agent that can look up customers and orders and issue refunds. Its questions share one shape: the agent sometimes does something in the wrong order, and the team asks how to stop it. The wrong answers all add words to the prompt. The right answer moves the rule out of the prompt and into the code that runs the tools, and the reason is arithmetic. As an illustration (these are not measured figures), a model that follows an instruction 99.5 percent of the time still skips it on one conversation in two hundred, and a company that handles ten thousand conversations a month has fifty refunds that moved without a check.

## The idea

### An instruction is a request

The Claude Code documentation draws the line in one sentence. Hooks give "deterministic control: certain actions always happen rather than relying on the LLM to choose to run them." The other side of the line is a prompt, a `CLAUDE.md` file, a skill: text that the model reads and weighs with everything else in its context. It usually complies. "Usually" is the property, not a flaw to be tuned away, because the model is a probabilistic component whose output depends on the whole conversation.

Two kinds of requirement therefore need two kinds of mechanism.

| The requirement | A prompt is enough when | Code is needed when |
|---|---|---|
| Style, tone, how to phrase a refusal | A slip costs little and a person can see it | Never: judgement and wording are what the model is for |
| Which tool suits a request | A wrong pick is cheap and the loop can recover | The wrong pick must be impossible, for example by not offering the tool |
| An order between steps, such as identity before money | Never, when the later step cannot be undone | The consequence is financial, legal or about security, and "usually" is not an acceptable rate |
| A limit, such as refunds above an amount | Never, for the same reason | A person must decide above the limit |

The building-effective-agents article places the actions in the same bucket for customer support: "Actions such as issuing refunds or updating tickets can be handled programmatically". Programmatic means the code decides whether the action runs. The model proposes it.

### The gate

A gate is a function in the code that runs the tools. For every tool call the model asks for, it checks the prerequisites, and only then does it call the backend. The practice of this module is exactly that: a desk with four tools and a rule for each step.

1. **Verify identity first.** Until a verification has succeeded in this session, only `verify_identity` and `escalate` run. Every other call is refused.
2. **The order must be the customer's.** A looked-up order that belongs to someone else is refused and not shown, even though the backend returned it, because the gate compares it with the verified customer.
3. **A refund is checked against the order.** The order must have been looked up, the amount must be a positive whole number, and it must not exceed what is left on the order.
4. **A refund over the limit does not run.** It is refused with a code that says a person must decide, and the case is handed over (page 2).

Three properties make a gate a guarantee and not a second prompt.

- **It runs on every call.** There is no path from the model's output to the backend that does not go through it. The loop of module 45 has one place where tools run; the gate sits there.
- **It keeps its own state.** The gate remembers that verification succeeded, in a variable, and does not ask the model whether it did. A sentence in the model's text such as "the customer has been verified" is not evidence, and a failed verification clears the state, so that an earlier success cannot be reused.
- **A refused call never reaches the backend.** The backend log is the proof: after a refused refund there is no `process_refund` in it.

### What the model gets back

A refused call still needs an answer. The API requires one: "Tool result blocks must immediately follow their corresponding tool use blocks in the message history." The loop sends a `tool_result` with `is_error` set, as for any failing tool, and the content decides what happens next. The documentation's advice for errors applies word for word: "include what went wrong and what Claude should try next". The practice's text is `BLOCKED identity_required: Verify the customer's identity before this action.`, a code that code and logs can match, and a sentence that tells the model its next step.

The gate does not repair the call. It would be tempting to run the missing verification itself, but the customer's identity code is something the model must obtain from the customer, so the gate refuses and says what is missing. That keeps the conversation honest and keeps the gate small. In the example below, the model's recovery after the refusal takes three more calls and no extra code: it asks for the verification, looks up the order and tries the refund again.

The refusal must also not leak what it protects. A lookup of someone else's order says only that the order does not belong to the verified customer. It does not say whose it is.

### Where the gate sits in each tool

| Where the model runs | The gate is |
|---|---|
| Your own loop on the Messages API | The function that executes a `tool_use` block, before it calls the backend |
| The Agent SDK or Claude Code | A `PreToolUse` hook (module 49), which runs before every other step of the permission decision and whose denial holds even in `bypassPermissions` |
| A tool implemented as an MCP server | The server itself, which should refuse a call whose prerequisites it can check, because it cannot trust every client to have a gate |

In this course's judgement, putting the check in the tool is the most robust of the three, because the rule travels with the tool. The cost is that the tool needs the state (who is verified), which a stateless server has to receive as an argument that it can check, such as a session token that only a successful verification could have produced.

### `tool_choice` is not the gate

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* for the order of steps (task 1.4) it keys programmatic enforcement, hooks and prerequisite gates such as blocking the refund until the customer lookup has returned a verified id, over prompt instructions, which have a non-zero failure rate. Separately, in its tool-design and structured-output tasks (2.3 and 4.3) it describes forcing a named tool with `tool_choice` on the first request and doing the later steps in follow-up turns, and using `any` to guarantee that a tool is called. *What works on current models (the "Define tools" page, checked 2026-10-03):* `any` and `tool` "return a 400 error" on Claude Opus 5.5, Claude Sonnet 5.5, Claude Fable 5.1 and Claude Mythos 5.1, and they are also not supported with manual extended thinking; the page names `auto` with strict tool use, or structured outputs, as what to use instead, and says that prompting still influences which tool `auto` picks. It shows forced use working on other models (it names Claude Opus 5 as supporting it, with adaptive thinking on). Where forcing works, it applies to the request that sets it, and "changes to the `tool_choice` parameter will invalidate cached message blocks", while tool definitions and system prompts remain cached. So the working design on current models is the gate in the loop, and a first call that must be a particular tool is made by code (the dispatcher refuses everything else until it has happened). On the exam, answer an ordering question with enforcement in code, and answer a question that asks how to make the first request call a named tool with the forced choice the guide describes. Where a schema in this course has a field whose data may be absent, it is optional and nullable so that the model is not pushed to invent a value (module 62); the input schemas of this module's tools have only required fields, since each value is always known at the call.

A common half-answer is to force the first tool with `tool_choice`, so that `verify_identity` always comes first. It fails twice. Where it is available, a forced choice works on the request that sets it: "the API prefills the assistant message to force a tool to be used". It says nothing about the checks that follow, so the model can still call the refund on turn three after a failed verification. And on the models of this course it is not available: forced tool use returns a 400 error on Claude Opus 5.5, Claude Sonnet 5.5, Claude Fable 5.1 and Claude Mythos 5.1 (module 26). The documentation's alternative for a guaranteed call is `auto` with strict tool use for a valid input, which guarantees the shape of the call and not its place in a sequence. An order between steps is a property of the program that runs them.

### The example

The example runs the same scripted model through two loops. The model is scripted to skip identity verification: its first call is `process_refund`. The system prompt asks for verification first in both runs. In the first run the loop trusts the prompt and runs whatever is asked. In the second, the dispatcher is the gate above.

<!-- example: m48-identity-gate tabs: python,typescript,java,kotlin -->
```python
"""The same scripted model, which skips identity verification, run against a loop that trusts the prompt and a loop that enforces the prerequisite in code.

The replies are illustrative, hand-written bodies in the shape of the Messages API (claude-sonnet-5-5), not captures. The system prompt asks for
verification first in both runs: what differs is whether the code that runs the tools checks it.
"""
from harness import scripted_client
from harness.scripted import message, text, tool_use
import logging

log = logging.getLogger(__name__)

MODEL = "claude-sonnet-5-5"
SYSTEM = "You are a support agent. Verify the customer's identity before any refund."
TOOLS = [{"name": n, "description": d, "input_schema": {"type": "object", "properties": p, "required": list(p)}} for n, d, p in [
    ("verify_identity", "Check the customer's identity code. Use it before any order or refund action.", {"code": {"type": "string"}}),
    ("lookup_order", "Look up one order of the verified customer.", {"order_id": {"type": "string"}}),
    ("process_refund", "Refund an amount in cents on a looked-up order.", {"order_id": {"type": "string"}, "amount_cents": {"type": "integer"}})]]


class Backend:
    def __init__(self):
        self.log, self.verified = [], False

    def call(self, name, args):
        self.log.append(name)
        if name == "verify_identity":
            self.verified = args["code"] == "1234"
            return "verified=yes" if self.verified else "verified=no"
        return {"lookup_order": "order_id=O1; total_cents=5000", "process_refund": "refund_id=R1"}[name]


def gate(backend, name, args):
    """The prerequisite, in code: nothing but verification runs before the customer is verified."""
    if name != "verify_identity" and not backend.verified:
        return "BLOCKED identity_required: Verify the customer's identity before this action.", True
    return backend.call(name, args), False


def run(client, backend, gated):
    messages = [{"role": "user", "content": "Please refund 20.00 on order O1. My code is 1234."}]
    results = []
    while True:
        reply = client.messages.create(model=MODEL, max_tokens=500, system=SYSTEM, tools=TOOLS, messages=messages)
        messages.append({"role": "assistant", "content": reply.content})
        if reply.stop_reason != "tool_use":
            return results
        out = []
        for block in reply.content:
            if block.type == "tool_use":
                content, is_error = gate(backend, block.name, block.input) if gated else (backend.call(block.name, block.input), False)
                results.append((block.name, content, is_error))
                out.append({"type": "tool_result", "tool_use_id": block.id, "content": content, **({"is_error": True} if is_error else {})})
        messages.append({"role": "user", "content": out})


def replies(gated):
    first = message([text("Refunding now."), tool_use("toolu_01", "process_refund", order_id="O1", amount_cents=2000)], stop_reason="tool_use", model=MODEL)
    if not gated:
        return [first, message([text("Refund issued.")], model=MODEL)]
    return [first,
            message([tool_use("toolu_02", "verify_identity", code="1234")], stop_reason="tool_use", model=MODEL),
            message([tool_use("toolu_03", "lookup_order", order_id="O1")], stop_reason="tool_use", model=MODEL),
            message([tool_use("toolu_04", "process_refund", order_id="O1", amount_cents=2000)], stop_reason="tool_use", model=MODEL),
            message([text("Refund issued after verification.")], model=MODEL)]


def main():
    print("system prompt in both runs:", SYSTEM)
    for label, gated in (("prompt only", False), ("code gate  ", True)):
        client, transport = scripted_client(*replies(gated))
        backend = Backend()
        results = run(client, backend, gated)
        before = backend.log.index("process_refund") < backend.log.index("verify_identity") if "verify_identity" in backend.log else "process_refund" in backend.log
        print(f"{label}: backend calls = {backend.log}; refund before verification: {before}")
        if gated:
            print(f"{label}: first result sent back to the model: {results[0][1]} (is_error={results[0][2]})")
        assert all(r["system"] == SYSTEM for r in transport.requests)


if __name__ == "__main__":
    main()
```
```text
system prompt in both runs: You are a support agent. Verify the customer's identity before any refund.
prompt only: backend calls = ['process_refund']; refund before verification: True
code gate  : backend calls = ['verify_identity', 'lookup_order', 'process_refund']; refund before verification: False
code gate  : first result sent back to the model: BLOCKED identity_required: Verify the customer's identity before this action. (is_error=True)
```
```typescript
// The same scripted model, which skips identity verification, run against a loop that trusts the prompt and a loop that enforces the prerequisite in code.
//
// The replies are illustrative, hand-written bodies in the shape of the Messages API (claude-sonnet-5-5), not captures. The system prompt asks for
// verification first in both runs: what differs is whether the code that runs the tools checks it.
import Anthropic from "@anthropic-ai/sdk";
import { message, scriptedFetch, text } from "../../../harness/ts/scriptedFetch.ts";
import { logger } from "./logger.ts";
const log = logger("identity_gate");

export const MODEL = "claude-sonnet-5-5";
export const SYSTEM = "You are a support agent. Verify the customer's identity before any refund.";
const def = (name: string, description: string, properties: Record<string, unknown>): Anthropic.Tool =>
  ({ name, description, input_schema: { type: "object", properties, required: Object.keys(properties) } });
const TOOLS: Anthropic.Tool[] = [
  def("verify_identity", "Check the customer's identity code. Use it before any order or refund action.", { code: { type: "string" } }),
  def("lookup_order", "Look up one order of the verified customer.", { order_id: { type: "string" } }),
  def("process_refund", "Refund an amount in cents on a looked-up order.", { order_id: { type: "string" }, amount_cents: { type: "integer" } }),
];

export class Backend {
  log: string[] = [];
  verified = false;

  call(name: string, args: any): string {
    this.log.push(name);
    if (name === "verify_identity") {
      this.verified = args.code === "1234";
      return this.verified ? "verified=yes" : "verified=no";
    }
    return ({ lookup_order: "order_id=O1; total_cents=5000", process_refund: "refund_id=R1" } as Record<string, string>)[name];
  }
}

/** The prerequisite, in code: nothing but verification runs before the customer is verified. */
export function gate(backend: Backend, name: string, args: any): [string, boolean] {
  if (name !== "verify_identity" && !backend.verified) return ["BLOCKED identity_required: Verify the customer's identity before this action.", true];
  return [backend.call(name, args), false];
}

export async function run(client: Anthropic, backend: Backend, gated: boolean) {
  const messages: Anthropic.MessageParam[] = [{ role: "user", content: "Please refund 20.00 on order O1. My code is 1234." }];
  const results: Array<[string, string, boolean]> = [];
  for (;;) {
    const reply = await client.messages.create({ model: MODEL, max_tokens: 500, system: SYSTEM, tools: TOOLS, messages });
    messages.push({ role: "assistant", content: reply.content });
    if (reply.stop_reason !== "tool_use") return results;
    const out: Anthropic.ToolResultBlockParam[] = [];
    for (const block of reply.content) {
      if (block.type === "tool_use") {
        const [content, isError] = gated ? gate(backend, block.name, block.input) : [backend.call(block.name, block.input), false] as [string, boolean];
        results.push([block.name, content, isError]);
        out.push({ type: "tool_result", tool_use_id: block.id, content, ...(isError ? { is_error: true } : {}) });
      }
    }
    messages.push({ role: "user", content: out });
  }
}

const usage = { input_tokens: 1, output_tokens: 1 };
const call = (id: string, name: string, input: Record<string, unknown>) => ({ type: "tool_use", id, name, input });
export function replies(gated: boolean) {
  const first = { body: message([text("Refunding now."), call("toolu_01", "process_refund", { order_id: "O1", amount_cents: 2000 })], "tool_use", usage, MODEL) };
  if (!gated) return [first, { body: message([text("Refund issued.")], "end_turn", usage, MODEL) }];
  return [first,
    { body: message([call("toolu_02", "verify_identity", { code: "1234" })], "tool_use", usage, MODEL) },
    { body: message([call("toolu_03", "lookup_order", { order_id: "O1" })], "tool_use", usage, MODEL) },
    { body: message([call("toolu_04", "process_refund", { order_id: "O1", amount_cents: 2000 })], "tool_use", usage, MODEL) },
    { body: message([text("Refund issued after verification.")], "end_turn", usage, MODEL) }];
}

export function clientFor(script: Array<Record<string, unknown>>) {
  const fake = scriptedFetch(script as any);
  return { fake, client: new Anthropic({ apiKey: "placeholder", maxRetries: 0, fetch: fake.fetch }) };
}

const py = (b: boolean) => (b ? "True" : "False");
const pyList = (items: string[]) => `[${items.map((s) => `'${s}'`).join(", ")}]`;

async function main() {
  console.log("system prompt in both runs:", SYSTEM);
  for (const [label, gated] of [["prompt only", false], ["code gate  ", true]] as const) {
    const { fake, client } = clientFor(replies(gated));
    const backend = new Backend();
    const results = await run(client, backend, gated);
    const at = backend.log.indexOf("verify_identity");
    const before = at >= 0 ? backend.log.indexOf("process_refund") < at : backend.log.includes("process_refund");
    console.log(`${label}: backend calls = ${pyList(backend.log)}; refund before verification: ${py(before)}`);
    if (gated) console.log(`${label}: first result sent back to the model: ${results[0][1]} (is_error=${py(results[0][2])})`);
    assert(fake.seen.every((r) => r.body.system === SYSTEM));
  }
}

function assert(condition: boolean) {
  if (!condition) throw new Error("the system prompt differed between requests");
}

if (import.meta.main) await main();
```
```text
system prompt in both runs: You are a support agent. Verify the customer's identity before any refund.
prompt only: backend calls = ['process_refund']; refund before verification: True
code gate  : backend calls = ['verify_identity', 'lookup_order', 'process_refund']; refund before verification: False
code gate  : first result sent back to the model: BLOCKED identity_required: Verify the customer's identity before this action. (is_error=True)
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
import com.anthropic.models.messages.ToolResultBlockParam;
import harness.Scripted;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The same scripted model, which skips identity verification, run against a loop that trusts the prompt and a loop that enforces the prerequisite in code.
 *
 * <p>The replies are illustrative, hand-written bodies in the shape of the Messages API (claude-sonnet-5-5), not captures. The system prompt asks for
 * verification first in both runs: what differs is whether the code that runs the tools checks it.
 */
public final class IdentityGate {
    private static final System.Logger LOG = System.getLogger(IdentityGate.class.getName());
    static final String MODEL = "claude-sonnet-5-5";
    static final String SYSTEM = "You are a support agent. Verify the customer's identity before any refund.";

    static Tool tool(String name, String description, Map<String, Object> properties) {
        return Tool.builder().name(name).description(description)
            .inputSchema(Tool.InputSchema.builder().properties(JsonValue.from(properties)).required(new ArrayList<>(properties.keySet())).build()).build();
    }

    static final List<Tool> TOOLS = List.of(
        tool("verify_identity", "Check the customer's identity code. Use it before any order or refund action.", map("code", map("type", "string"))),
        tool("lookup_order", "Look up one order of the verified customer.", map("order_id", map("type", "string"))),
        tool("process_refund", "Refund an amount in cents on a looked-up order.", map("order_id", map("type", "string"), "amount_cents", map("type", "integer"))));

    static final class Backend {
        final List<String> log = new ArrayList<>();
        boolean verified = false;

        String call(String name, Map<String, Object> args) {
            log.add(name);
            return switch (name) {
                case "verify_identity" -> {
                    verified = "1234".equals(args.get("code"));
                    yield verified ? "verified=yes" : "verified=no";
                }
                case "lookup_order" -> "order_id=O1; total_cents=5000";
                case "process_refund" -> "refund_id=R1";
                default -> throw new IllegalArgumentException(name);
            };
        }
    }

    /** What one tool call returned and whether it is an error. */
    record Result(String name, String content, boolean isError) {}

    /** The prerequisite, in code: nothing but verification runs before the customer is verified. */
    static Result gate(Backend backend, String name, Map<String, Object> args) {
        if (!name.equals("verify_identity") && !backend.verified) {
            return new Result(name, "BLOCKED identity_required: Verify the customer's identity before this action.", true);
        }
        return new Result(name, backend.call(name, args), false);
    }

    @SuppressWarnings("unchecked")
    static List<Result> run(AnthropicClient client, Backend backend, boolean gated) {
        List<MessageParam> messages = new ArrayList<>();
        messages.add(MessageParam.builder().role(MessageParam.Role.USER).content("Please refund 20.00 on order O1. My code is 1234.").build());
        List<Result> results = new ArrayList<>();
        while (true) {
            MessageCreateParams.Builder request = MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(500).system(SYSTEM).messages(messages);
            TOOLS.forEach(request::addTool);
            Message reply = client.messages().create(request.build());
            messages.add(reply.toParam());
            if (!reply.stopReason().get().asString().equals("tool_use")) return results;
            List<ContentBlockParam> out = new ArrayList<>();
            for (ContentBlock block : reply.content()) {
                if (!block.isToolUse()) continue;
                var use = block.asToolUse();
                Map<String, Object> args = ObjectMappers.jsonMapper().convertValue(use._input(), Map.class);
                Result r = gated ? gate(backend, use.name(), args) : new Result(use.name(), backend.call(use.name(), args), false);
                results.add(r);
                ToolResultBlockParam.Builder b = ToolResultBlockParam.builder().toolUseId(use.id()).content(r.content());
                if (r.isError()) b.isError(true);
                out.add(ContentBlockParam.ofToolResult(b.build()));
            }
            messages.add(MessageParam.builder().role(MessageParam.Role.USER).contentOfBlockParams(out).build());
        }
    }

    static Object[] replies(boolean gated) {
        Map<String, Object> first = message(List.of(text("Refunding now."), toolUse("toolu_01", "process_refund", map("order_id", "O1", "amount_cents", 2000))), "tool_use");
        if (!gated) return new Object[] {first, message(List.of(text("Refund issued.")))};
        return new Object[] {first,
            message(List.of(toolUse("toolu_02", "verify_identity", map("code", "1234"))), "tool_use"),
            message(List.of(toolUse("toolu_03", "lookup_order", map("order_id", "O1"))), "tool_use"),
            message(List.of(toolUse("toolu_04", "process_refund", map("order_id", "O1", "amount_cents", 2000))), "tool_use"),
            message(List.of(text("Refund issued after verification.")))};
    }

    public static void main(String[] args) {
        System.out.println("system prompt in both runs: " + SYSTEM);
        for (Object[] mode : new Object[][] {{"prompt only", false}, {"code gate  ", true}}) {
            String label = (String) mode[0];
            boolean gated = (Boolean) mode[1];
            Scripted.Rig rig = Scripted.client(replies(gated));
            Backend backend = new Backend();
            List<Result> results = run(rig.client(), backend, gated);
            boolean before = backend.log.contains("verify_identity")
                ? backend.log.indexOf("process_refund") < backend.log.indexOf("verify_identity") : backend.log.contains("process_refund");
            System.out.println(label + ": backend calls = " + py(backend.log) + "; refund before verification: " + py(before));
            if (gated) System.out.println(label + ": first result sent back to the model: " + results.get(0).content() + " (is_error=" + py(results.get(0).isError()) + ")");
            for (var r : rig.http().requests) if (!r.get("system").asText().equals(SYSTEM)) throw new AssertionError("system prompt changed");
        }
    }
}
```
```text
system prompt in both runs: You are a support agent. Verify the customer's identity before any refund.
prompt only: backend calls = ['process_refund']; refund before verification: True
code gate  : backend calls = ['verify_identity', 'lookup_order', 'process_refund']; refund before verification: False
code gate  : first result sent back to the model: BLOCKED identity_required: Verify the customer's identity before this action. (is_error=True)
```
```kotlin
import com.anthropic.client.AnthropicClient
import com.anthropic.core.JsonValue
import com.anthropic.core.jsonMapper
import com.anthropic.models.messages.ContentBlockParam
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

private val log = System.getLogger("identity_gate")

/**
 * The same scripted model, which skips identity verification, run against a loop that trusts the prompt and a loop that enforces the prerequisite in code.
 *
 * The replies are illustrative, hand-written bodies in the shape of the Messages API (claude-sonnet-5-5), not captures. The system prompt asks for
 * verification first in both runs: what differs is whether the code that runs the tools checks it.
 */
const val MODEL = "claude-sonnet-5-5"
const val SYSTEM = "You are a support agent. Verify the customer's identity before any refund."

fun tool(name: String, description: String, properties: Map<String, Any>): Tool = Tool.builder().name(name).description(description)
    .inputSchema(Tool.InputSchema.builder().properties(JsonValue.from(properties)).required(properties.keys.toList()).build()).build()

val TOOLS = listOf(
    tool("verify_identity", "Check the customer's identity code. Use it before any order or refund action.", map("code", map("type", "string"))),
    tool("lookup_order", "Look up one order of the verified customer.", map("order_id", map("type", "string"))),
    tool("process_refund", "Refund an amount in cents on a looked-up order.", map("order_id", map("type", "string"), "amount_cents", map("type", "integer"))),
)

class Backend {
    val log = mutableListOf<String>()
    var verified = false

    fun call(name: String, args: Map<*, *>): String {
        log += name
        return when (name) {
            "verify_identity" -> {
                verified = args["code"] == "1234"
                if (verified) "verified=yes" else "verified=no"
            }
            "lookup_order" -> "order_id=O1; total_cents=5000"
            "process_refund" -> "refund_id=R1"
            else -> throw IllegalArgumentException(name)
        }
    }
}

/** What one tool call returned and whether it is an error. */
data class Result(val name: String, val content: String, val isError: Boolean)

/** The prerequisite, in code: nothing but verification runs before the customer is verified. */
fun gate(backend: Backend, name: String, args: Map<*, *>): Result {
    if (name != "verify_identity" && !backend.verified) return Result(name, "BLOCKED identity_required: Verify the customer's identity before this action.", true)
    return Result(name, backend.call(name, args), false)
}

fun run(client: AnthropicClient, backend: Backend, gated: Boolean): List<Result> {
    val messages = mutableListOf(MessageParam.builder().role(MessageParam.Role.USER).content("Please refund 20.00 on order O1. My code is 1234.").build())
    val results = mutableListOf<Result>()
    while (true) {
        val request = MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(500).system(SYSTEM).messages(messages.toList()).apply { TOOLS.forEach { addTool(it) } }
        val reply = client.messages().create(request.build())
        messages += reply.toParam()
        if (reply.stopReason().get().asString() != "tool_use") return results
        val out = reply.content().filter { it.isToolUse() }.map { block ->
            val use = block.asToolUse()
            val args = jsonMapper().convertValue(use._input(), Map::class.java)
            val r = if (gated) gate(backend, use.name(), args) else Result(use.name(), backend.call(use.name(), args), false)
            results += r
            val b = ToolResultBlockParam.builder().toolUseId(use.id()).content(r.content)
            if (r.isError) b.isError(true)
            ContentBlockParam.ofToolResult(b.build())
        }
        messages += MessageParam.builder().role(MessageParam.Role.USER).contentOfBlockParams(out).build()
    }
}

fun replies(gated: Boolean): Array<Any> {
    val first = message(listOf(text("Refunding now."), toolUse("toolu_01", "process_refund", map("order_id", "O1", "amount_cents", 2000))), "tool_use")
    if (!gated) return arrayOf(first, message(listOf(text("Refund issued."))))
    return arrayOf(
        first,
        message(listOf(toolUse("toolu_02", "verify_identity", map("code", "1234"))), "tool_use"),
        message(listOf(toolUse("toolu_03", "lookup_order", map("order_id", "O1"))), "tool_use"),
        message(listOf(toolUse("toolu_04", "process_refund", map("order_id", "O1", "amount_cents", 2000))), "tool_use"),
        message(listOf(text("Refund issued after verification."))),
    )
}

fun main() {
    println("system prompt in both runs: $SYSTEM")
    for ((label, gated) in listOf("prompt only" to false, "code gate  " to true)) {
        val rig = Scripted.client(*replies(gated))
        val backend = Backend()
        val results = run(rig.client(), backend, gated)
        val before = if ("verify_identity" in backend.log) backend.log.indexOf("process_refund") < backend.log.indexOf("verify_identity") else "process_refund" in backend.log
        println("$label: backend calls = ${py(backend.log)}; refund before verification: ${py(before)}")
        if (gated) println("$label: first result sent back to the model: ${results[0].content} (is_error=${py(results[0].isError)})")
        check(rig.http().requests.all { it["system"].asText() == SYSTEM })
    }
}
```
```text
system prompt in both runs: You are a support agent. Verify the customer's identity before any refund.
prompt only: backend calls = ['process_refund']; refund before verification: True
code gate  : backend calls = ['verify_identity', 'lookup_order', 'process_refund']; refund before verification: False
code gate  : first result sent back to the model: BLOCKED identity_required: Verify the customer's identity before this action. (is_error=True)
```
<!-- /example -->

The first loop ran the refund as its only backend call: the money moved before anyone was verified, and the prompt, which both runs carried, did nothing. In the second run the refund never reached the backend. The first result the model got back was the refusal, flagged as an error and naming the missing step, and the model's next three calls were the verification, the lookup and the refund, in the right order. All four languages print the same lines.

## Traps

These are the wrong answers that the exam's options for this task statement offer, each with the reason it is rejected.

1. **"Add ALWAYS verify identity first to the system prompt."** It is tempting because it is the cheapest change. The exam rejects it: the sentence lowers the rate of the skip and does not make it zero, and the step cannot be undone. The rule belongs in the code that runs the tools.
2. **"Trust the model's report that the customer was verified."** It is tempting because the model says so in plain words. The exam rejects it: "I have verified the customer" is text. The gate keeps the fact itself, set only by a successful verification call, and clears it on a failure.
3. **"Refuse the call with a bare error."** It is tempting because the call is stopped either way. The exam rejects it: a bare error or an empty result sends the model guessing. Return a flagged result with the missing step in it.
4. **"Force the verification tool with tool_choice, and the order is guaranteed."** It is tempting because it sounds like enforcement. The exam keys programmatic gating for ordering (task 1.4), and a forced choice applies only to the request that sets it, so later calls are free. On current models it is also refused with a 400 error.

## Quiz

1. A gate in a support agent's dispatcher refuses a refund because no identity has been verified. What should the dispatcher send back to the model?
   - **a**: A flagged failure result that names the missing step
   - **b**: Nothing, because an unanswered call simply ends the turn
   - **c**: A generic error that says only that the call failed
   - **d**: The refund's result after running the verification itself

2. On a model that supports forced tool use, a team forces the verification tool through tool_choice on the first request, believing the order of steps is now guaranteed. Which gap remains?
   - **a**: Later tools can stay blocked until verification returns a result
   - **b**: Text written before the call changes the order in which the tools run
   - **c**: Later turns are unconstrained, so a refund can still follow a failed check
   - **d**: The forced choice makes the system prompt uncached, so the cost rises


<details>
<summary>Answer key</summary>

1. **a**. Every call needs an answer, and an error that names the missing step lets the model do it and try again. *b* is ruled out because the API requires a result: "Tool result blocks must immediately follow their corresponding tool use blocks in the message history." *c* is ruled out because the documentation's advice is to "include what went wrong and what Claude should try next". *d* is ruled out because the model must obtain the code from the customer: "the gate refuses and says what is missing".
2. **c**. A forced choice applies to the request that sets it, and nothing carries it to later calls. *b* is ruled out because the model writes no text before a forced call: "the API prefills the assistant message to force a tool to be used". *a* is ruled out because keeping later tools blocked until verification returns is what a gate does, not what a forced choice does: "An order between steps is a property of the program that runs them." *d* is ruled out because the system prompt stays cached: "tool definitions and system prompts remain cached".

</details>
