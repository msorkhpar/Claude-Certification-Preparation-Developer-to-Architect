# Hub and spoke: what the coordinator owns

**Level:** Architect · **Module 46:** Coordinator and subagents · **Page 1 of 2**
**Exams:** A1.2; S3

**After this page you can** explain hub and spoke and what a subagent's isolated context means for the code around it, list what the coordinator must do that no subagent will do for it (partition the scope, write complete briefs, pick subagents for the task at hand), judge when a task should not be delegated at all, and read a request to see whether a subagent was really isolated.

Checked on 2026-10-03 against Anthropic's engineering article on its multi-agent research system, its article on building effective agents, and the Claude Code documentation pages "Subagents in the SDK" and "Subagents in Claude Code". The example runs offline in Python (`anthropic` 1.11.0) and TypeScript (`@anthropic-ai/sdk` 0.131.0) against scripted replies in the shape of the Messages API, so it shows no live output. Module 34 introduced the orchestrator-workers pattern, module 35 the SDK's subagent option and module 39 the subagent file; this module is about the design of the team.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* a coordinator manages all communication between subagents, handles errors and routes information (hub and spoke); subagents work in isolated context and do not inherit the coordinator's conversation; the coordinator chooses which subagents to invoke from the complexity of the query, and an overly narrow split risks incomplete coverage of a broad topic. *What the current product does (documentation checked 2026-10-03):* a subagent's final message is the only thing that returns to its parent, which is the hub and spoke of the guide. The tool that starts a subagent is named `Agent` and was named `Task` in the guide's era and in older SDK versions (module 47). The product also has optional ways to break the pattern: a subagent that has the `SendMessage` tool is told the names of the other agents in the session, and agent teams, an experimental and disabled-by-default feature, coordinate sessions with shared tasks and messaging. The exam keys the hub and spoke; choose the option that routes through the coordinator, and treat peer messaging as a choice that gives up the observability and control that the guide asks for.

## Why it matters

Scenario S3 of the Architect exam is a multi-agent research system, and its questions are about the team, not the model. Two subagents come back with the same findings. A subagent cannot answer because the coordinator told it "look into the supply chain" and nothing else. One failed subagent takes the whole report down. A lead agent spawns a team for a question that one agent could have answered. Each of these is a decision in the coordinator, and a coordinator is mostly code and a prompt, so an architect is expected to be able to find the decision.

## The idea

### Hub and spoke, and where the context goes

In a hub-and-spoke design one agent, the coordinator or lead, owns the task, and "a lead agent coordinates the process while delegating to specialized subagents that operate in parallel." The research article describes the shape: the lead analyses the query, develops a strategy and starts subagents to look at different aspects at the same time; each subagent works with its own tools and returns what it found; the lead synthesizes the results. The documentation's term for the unit is "separate agent instances that your main agent can spawn to handle focused subtasks".

The property that matters is isolation. Each subagent runs in its own conversation, which "starts fresh" (unless it is a fork of the parent's). "The only content you pass from parent to subagent is the Agent tool's prompt string", so the instruction is to "include any file paths, error messages, or decisions the subagent needs directly in that prompt." And in the other direction "only its final message returns to the parent": the intermediate tool calls and results stay inside the subagent. That is why a subagent keeps the coordinator's context small, and it is also why nothing is shared by accident. A subagent does not know the user's original question, the plan, the other subagents or what the coordinator learned a minute ago, unless the brief says so.

Two consequences follow, and they are the basis of the module.

1. **The brief is the whole interface.** If a fact is not in the brief, the subagent does not have it, and a vague brief produces vague work.
2. **The coordinator is the only place where the pieces meet.** Dividing the work, noticing gaps and producing one answer cannot be left to the subagents, because each sees only its own slice.

The coordinator itself is a loop (module 45) whose "tool" is the delegation. A model-driven coordinator decides by its own reading of the subagent descriptions: "Claude uses each subagent's description to decide when to delegate tasks." A coordinator written as ordinary code, as in the practice, makes the same decisions in functions you can test.

### What the coordinator owns

| Duty | What goes wrong without it |
|---|---|
| **Partition the scope** into parts that do not overlap | Subagents repeat each other, or leave a gap |
| **Write a complete brief** for each part | A subagent guesses at what is wanted and returns something unusable |
| **Choose subagents dynamically**, only those the question needs | A fixed team runs on every query and costs most on the simplest |
| **Review the findings** and send back only the gaps | Missing parts reach the user, or the whole team is sent out again |
| **Synthesize once** from what came back | Pieces are passed on as an answer, with no one responsible for the whole |
| **Say what is missing** when a subagent failed | A partial answer looks like a full one |

**Partitioning.** The research article gives the failure in one sentence: "one subagent explored the 2021 automotive chip crisis while 2 others duplicated work investigating current 2025 supply chains." The cause it names is the brief: "Without detailed task descriptions, agents duplicate work, leave gaps, or fail to find necessary information." A partition names the boundary of each part by facet, by period or by source, and says what each part must not cover. Overlap is avoided by exclusion written into the brief, not by hoping that the model will see what its siblings are doing, because it cannot.

**A complete brief.** The article's checklist is short: "Each subagent needs an objective, an output format, guidance on the tools and sources to use, and clear task boundaries." Add what the page above says: every fact the subagent needs, written into the brief itself. The output format matters for the next step: if every subagent reports in the same shape (findings first, then a source line) the coordinator can compare and merge them.

**Dynamic choice.** The research article ties effort to the question: "Simple fact-finding requires just 1 agent with 3-10 tool calls, direct comparisons might need 2-4 subagents with 10-15 calls each, and complex research might use more than 10 subagents with clearly divided responsibilities." A coordinator that starts the same team for every query overspends on easy questions and may underspend on hard ones. The scale is a rule you write into the coordinator's prompt or code. The Agent SDK also gives you hard limits for when a model-driven lead is too generous: nesting is limited to "up to three layers below the main conversation" by default (`CLAUDE_CODE_MAX_SUBAGENT_SPAWN_DEPTH`, set to `1` to stop nesting), at most 20 subagents run at once by default (`CLAUDE_CODE_MAX_CONCURRENT_SUBAGENTS`), and the budget cap counts the subagents' spending toward the total. Page 2 comes back to the spend.

### The split can be too narrow

The mirror image of a vague brief is a plan that divides a broad topic into too few, too obvious parts. Each subagent then does its part well, and the answer still leaves out what no part covered. The research article states the cause on the brief's side: "Without detailed task descriptions, agents duplicate work, leave gaps, or fail to find necessary information". A coordinator therefore does two things about coverage. It writes the split from the question's facets, not from the first parts that come to mind. And it checks the findings against the question afterwards and sends out what is missing, which page 2 builds.

### When the coordinator should not delegate at all

A team is not free. The article puts the price plainly: "agents typically use about 4× more tokens than chat interactions, and multi-agent systems use about 15× more tokens than chats", and so "multi-agent systems require tasks where the value of the task is high enough to pay for the increased performance." Two more conditions weigh against a team. The article says "some domains that require all agents to share the same context or involve many dependencies between agents are not a good fit", and the Claude Code documentation lists the cases for staying in the main conversation: the task "needs frequent back-and-forth or iterative refinement", "multiple phases share significant context, such as planning, implementation, and testing", it is "a quick, targeted change", or latency matters, because a subagent that is not a fork "starts fresh and may need time to gather context".

The positive list is as short: use subagents when "the task produces verbose output you don't need in your main context", when you want to enforce tool restrictions, and when "the work is self-contained and can return a summary." The decision is a question the coordinator can ask first. Are the parts independent? Is the output noisy? Does the answer justify several times the tokens? If not, answer directly. The practice checks that a planner who says "no team" gets no team.

### The example: one request per subagent

The example is hub and spoke on the Messages API, so that you can see the isolation as data. A coordinator makes one planning call that offers a tool named `plan` and asks for it in words (Claude Sonnet 5.5 rejects a forced `tool_choice` with a 400 error, as module 26 explained), and reads the subtasks from the tool input. Each subtask becomes a separate conversation: the role's system prompt and the brief, nothing else. The coordinator then makes one synthesis call with every report. The replies are scripted, so what the output shows is what each request contains.

<!-- example: m46-hub-and-spoke tabs: python,typescript,java,kotlin -->
```python
"""Hub and spoke on the Messages API: a coordinator plans by calling a plan tool, each subagent is its own conversation, the coordinator synthesizes.

The replies are illustrative, hand-written bodies in the shape of the Messages API (claude-sonnet-5-5), not captures. The point is what each
request contains: a subagent's request holds its brief and nothing else, and only the synthesis request holds the findings.
"""
import json

from harness import scripted_client
from harness.scripted import message, text, tool_use

MODEL = "claude-sonnet-5-5"
PLAN_TOOL = {"name": "plan", "description": "Record the plan: one subtask per independent part of the question, each with a scope and a self-contained brief.",
             "input_schema": {"type": "object", "properties": {"subtasks": {"type": "array", "items": {"type": "object", "properties": {"scope": {"type": "string"}, "brief": {"type": "string"}}, "required": ["scope", "brief"]}}}, "required": ["subtasks"]}}
SUBAGENT_SYSTEM = "You are a research subagent. Answer only the brief you are given and end with a one-line source note."

SUBTASKS = [
    {"scope": "chips", "brief": "Find what changed in 2024 chip supply. Return three bullet points and a source note. Do not cover cars or interest rates."},
    {"scope": "cars", "brief": "Find what changed in 2024 car output. Return three bullet points and a source note. Do not cover chips or interest rates."},
    {"scope": "rates", "brief": "Find what changed in 2024 interest rates. Return three bullet points and a source note. Do not cover chips or cars."},
]
REPORTS = ["CHIPS-REPORT: output of foundries rose, lead times fell. Source: industry survey.",
           "CARS-REPORT: plants ran fuller as chips arrived. Source: producer filings.",
           "RATES-REPORT: central banks held rates, loans stayed dear. Source: bank statements."]


def plan(client, question):
    # tool_choice stays auto: Claude Sonnet 5.5 returns a 400 error for a forced choice, so the request asks for the tool in words
    reply = client.messages.create(model=MODEL, max_tokens=800, tools=[PLAN_TOOL], messages=[{"role": "user", "content": f"{question}\nRecord your plan by calling the plan tool."}])
    return next(b.input["subtasks"] for b in reply.content if b.type == "tool_use")


def run_subagent(client, brief):
    """A fresh conversation: the system prompt of the role and the brief. Nothing of the coordinator's history is passed."""
    reply = client.messages.create(model=MODEL, max_tokens=800, system=SUBAGENT_SYSTEM, messages=[{"role": "user", "content": brief}])
    return "".join(b.text for b in reply.content if b.type == "text")


def synthesize(client, question, findings):
    listing = "\n".join(f"[{scope}] {report}" for scope, report in findings)
    reply = client.messages.create(model=MODEL, max_tokens=800, messages=[{"role": "user", "content": f"Question: {question}\nFindings:\n{listing}\nWrite one answer that uses every finding."}])
    return "".join(b.text for b in reply.content if b.type == "text")


def main():
    question = "How did the 2024 supply picture change for chips, cars and interest rates?"
    replies = [message([text("Three independent parts."), tool_use("toolu_01", "plan", subtasks=SUBTASKS)], stop_reason="tool_use", model=MODEL)]
    replies += [message([text(r)], model=MODEL) for r in REPORTS]
    replies += [message([text("Chip supply recovered first, car output followed, and rates stayed high.")], model=MODEL)]
    client, transport = scripted_client(*replies)
    subtasks = plan(client, question)
    print("plan:", len(subtasks), "subtasks ->", [s["scope"] for s in subtasks])
    findings = []
    for number, task in enumerate(subtasks, start=1):
        report = run_subagent(client, task["brief"])
        findings.append((task["scope"], report))
        request = transport.requests[number]
        body = json.dumps(request)
        others = [r.split(":")[0] for r in REPORTS if r != report and r.split(":")[0] in body]
        print(f"subagent {number} request: {len(request['messages'])} message, system prompt of the role: {request['system'] == SUBAGENT_SYSTEM}, "
              f"holds the coordinator's question: {question in body}, holds another report: {bool(others)}")
    answer = synthesize(client, question, findings)
    synthesis = json.dumps(transport.requests[4])
    print("synthesis request: reports included:", sum(r.split(":")[0] in synthesis for r in REPORTS), "of", len(REPORTS))
    print("model calls:", len(transport.requests), "| one plan, three subagents, one synthesis")
    print("answer:", answer)


if __name__ == "__main__":
    main()
```
```text
plan: 3 subtasks -> ['chips', 'cars', 'rates']
subagent 1 request: 1 message, system prompt of the role: True, holds the coordinator's question: False, holds another report: False
subagent 2 request: 1 message, system prompt of the role: True, holds the coordinator's question: False, holds another report: False
subagent 3 request: 1 message, system prompt of the role: True, holds the coordinator's question: False, holds another report: False
synthesis request: reports included: 3 of 3
model calls: 5 | one plan, three subagents, one synthesis
answer: Chip supply recovered first, car output followed, and rates stayed high.
```
```typescript
// Hub and spoke on the Messages API: a coordinator plans by calling a plan tool, each subagent is its own conversation, the coordinator synthesizes.
//
// The replies are illustrative, hand-written bodies in the shape of the Messages API (claude-sonnet-5-5), not captures. The point is what each
// request contains: a subagent's request holds its brief and nothing else, and only the synthesis request holds the findings.
import Anthropic from "@anthropic-ai/sdk";
import { message, scriptedFetch, text } from "../../../harness/ts/scriptedFetch.ts";

export const MODEL = "claude-sonnet-5-5";
const PLAN_TOOL: Anthropic.Tool = {
  name: "plan", description: "Record the plan: one subtask per independent part of the question, each with a scope and a self-contained brief.",
  input_schema: { type: "object", properties: { subtasks: { type: "array", items: { type: "object", properties: { scope: { type: "string" }, brief: { type: "string" } }, required: ["scope", "brief"] } } }, required: ["subtasks"] },
};
export const SUBAGENT_SYSTEM = "You are a research subagent. Answer only the brief you are given and end with a one-line source note.";

export const SUBTASKS = [
  { scope: "chips", brief: "Find what changed in 2024 chip supply. Return three bullet points and a source note. Do not cover cars or interest rates." },
  { scope: "cars", brief: "Find what changed in 2024 car output. Return three bullet points and a source note. Do not cover chips or interest rates." },
  { scope: "rates", brief: "Find what changed in 2024 interest rates. Return three bullet points and a source note. Do not cover chips or cars." },
];
export const REPORTS = ["CHIPS-REPORT: output of foundries rose, lead times fell. Source: industry survey.",
  "CARS-REPORT: plants ran fuller as chips arrived. Source: producer filings.",
  "RATES-REPORT: central banks held rates, loans stayed dear. Source: bank statements."];

const textOf = (reply: Anthropic.Message) => reply.content.map((b) => (b.type === "text" ? b.text : "")).join("");

export async function plan(client: Anthropic, question: string) {
  // tool_choice stays auto: Claude Sonnet 5.5 returns a 400 error for a forced choice, so the request asks for the tool in words
  const reply = await client.messages.create({ model: MODEL, max_tokens: 800, tools: [PLAN_TOOL], messages: [{ role: "user", content: `${question}\nRecord your plan by calling the plan tool.` }] });
  const block = reply.content.find((b): b is Anthropic.ToolUseBlock => b.type === "tool_use")!;
  return (block.input as { subtasks: Array<{ scope: string; brief: string }> }).subtasks;
}

/** A fresh conversation: the system prompt of the role and the brief. Nothing of the coordinator's history is passed. */
export async function runSubagent(client: Anthropic, brief: string) {
  return textOf(await client.messages.create({ model: MODEL, max_tokens: 800, system: SUBAGENT_SYSTEM, messages: [{ role: "user", content: brief }] }));
}

export async function synthesize(client: Anthropic, question: string, findings: Array<[string, string]>) {
  const listing = findings.map(([scope, report]) => `[${scope}] ${report}`).join("\n");
  return textOf(await client.messages.create({ model: MODEL, max_tokens: 800, messages: [{ role: "user", content: `Question: ${question}\nFindings:\n${listing}\nWrite one answer that uses every finding.` }] }));
}

const usage = { input_tokens: 1, output_tokens: 1 };
export const replies = () => [
  { body: message([text("Three independent parts."), { type: "tool_use", id: "toolu_01", name: "plan", input: { subtasks: SUBTASKS } }], "tool_use", usage, MODEL) },
  ...REPORTS.map((r) => ({ body: message([text(r)], "end_turn", usage, MODEL) })),
  { body: message([text("Chip supply recovered first, car output followed, and rates stayed high.")], "end_turn", usage, MODEL) },
];

export function clientFor(script: Array<Record<string, unknown>>) {
  const fake = scriptedFetch(script as any);
  return { fake, client: new Anthropic({ apiKey: "placeholder", maxRetries: 0, fetch: fake.fetch }) };
}

async function main() {
  const question = "How did the 2024 supply picture change for chips, cars and interest rates?";
  const { fake, client } = clientFor(replies());
  const subtasks = await plan(client, question);
  console.log("plan:", subtasks.length, "subtasks ->", `[${subtasks.map((s) => `'${s.scope}'`).join(", ")}]`);
  const findings: Array<[string, string]> = [];
  for (const [index, task] of subtasks.entries()) {
    const report = await runSubagent(client, task.brief);
    findings.push([task.scope, report]);
    const request = fake.seen[index + 1].body;
    const body = JSON.stringify(request);
    const others = REPORTS.filter((r) => r !== report && body.includes(r.split(":")[0]));
    const flag = (b: boolean) => (b ? "True" : "False");
    console.log(`subagent ${index + 1} request: ${request.messages.length} message, system prompt of the role: ${flag(request.system === SUBAGENT_SYSTEM)}, ` +
      `holds the coordinator's question: ${flag(body.includes(question))}, holds another report: ${flag(others.length > 0)}`);
  }
  const answer = await synthesize(client, question, findings);
  const synthesis = JSON.stringify(fake.seen[4].body);
  console.log("synthesis request: reports included:", REPORTS.filter((r) => synthesis.includes(r.split(":")[0])).length, "of", REPORTS.length);
  console.log("model calls:", fake.seen.length, "| one plan, three subagents, one synthesis");
  console.log("answer:", answer);
}

if (import.meta.main) await main();
```
```text
plan: 3 subtasks -> ['chips', 'cars', 'rates']
subagent 1 request: 1 message, system prompt of the role: True, holds the coordinator's question: False, holds another report: False
subagent 2 request: 1 message, system prompt of the role: True, holds the coordinator's question: False, holds another report: False
subagent 3 request: 1 message, system prompt of the role: True, holds the coordinator's question: False, holds another report: False
synthesis request: reports included: 3 of 3
model calls: 5 | one plan, three subagents, one synthesis
answer: Chip supply recovered first, car output followed, and rates stayed high.
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
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.anthropic.models.messages.Tool;
import harness.Scripted;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Hub and spoke on the Messages API: a coordinator plans by calling a plan tool, each subagent is its own conversation, the coordinator synthesizes.
 *
 * <p>The replies are illustrative, hand-written bodies in the shape of the Messages API (claude-sonnet-5-5), not captures. The point is what each
 * request contains: a subagent's request holds its brief and nothing else, and only the synthesis request holds the findings.
 */
public final class Hub {
    static final String MODEL = "claude-sonnet-5-5";
    static final Tool PLAN_TOOL = Tool.builder().name("plan")
        .description("Record the plan: one subtask per independent part of the question, each with a scope and a self-contained brief.")
        .inputSchema(Tool.InputSchema.builder()
            .properties(JsonValue.from(map("subtasks", map("type", "array", "items", map("type", "object",
                "properties", map("scope", map("type", "string"), "brief", map("type", "string")), "required", List.of("scope", "brief"))))))
            .required(List.of("subtasks")).build())
        .build();
    static final String SUBAGENT_SYSTEM = "You are a research subagent. Answer only the brief you are given and end with a one-line source note.";

    record Subtask(String scope, String brief) {}

    static final List<Subtask> SUBTASKS = List.of(
        new Subtask("chips", "Find what changed in 2024 chip supply. Return three bullet points and a source note. Do not cover cars or interest rates."),
        new Subtask("cars", "Find what changed in 2024 car output. Return three bullet points and a source note. Do not cover chips or interest rates."),
        new Subtask("rates", "Find what changed in 2024 interest rates. Return three bullet points and a source note. Do not cover chips or cars."));
    static final List<String> REPORTS = List.of(
        "CHIPS-REPORT: output of foundries rose, lead times fell. Source: industry survey.",
        "CARS-REPORT: plants ran fuller as chips arrived. Source: producer filings.",
        "RATES-REPORT: central banks held rates, loans stayed dear. Source: bank statements.");

    static String textOf(Message reply) {
        StringBuilder out = new StringBuilder();
        for (ContentBlock b : reply.content()) if (b.isText()) out.append(b.asText().text());
        return out.toString();
    }

    @SuppressWarnings("unchecked")
    static List<Subtask> plan(AnthropicClient client, String question) {
        // tool_choice stays auto: Claude Sonnet 5.5 returns a 400 error for a forced choice, so the request asks for the tool in words
        Message reply = client.messages().create(MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(800).addTool(PLAN_TOOL)
            .addUserMessage(question + "\nRecord your plan by calling the plan tool.").build());
        for (ContentBlock b : reply.content()) {
            if (!b.isToolUse()) continue;
            Map<String, Object> input = ObjectMappers.jsonMapper().convertValue(b.asToolUse()._input(), Map.class);
            List<Subtask> out = new ArrayList<>();
            for (Map<String, String> s : (List<Map<String, String>>) input.get("subtasks")) out.add(new Subtask(s.get("scope"), s.get("brief")));
            return out;
        }
        throw new IllegalStateException("no plan call");
    }

    /** A fresh conversation: the system prompt of the role and the brief. Nothing of the coordinator's history is passed. */
    static String runSubagent(AnthropicClient client, String brief) {
        return textOf(client.messages().create(MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(800).system(SUBAGENT_SYSTEM).addUserMessage(brief).build()));
    }

    static String synthesize(AnthropicClient client, String question, List<String[]> findings) {
        StringBuilder listing = new StringBuilder();
        for (String[] f : findings) listing.append(listing.length() == 0 ? "" : "\n").append("[").append(f[0]).append("] ").append(f[1]);
        return textOf(client.messages().create(MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(800)
            .addUserMessage("Question: " + question + "\nFindings:\n" + listing + "\nWrite one answer that uses every finding.").build()));
    }

    static String tag(String report) {
        return report.split(":")[0];
    }

    public static void main(String[] args) {
        String question = "How did the 2024 supply picture change for chips, cars and interest rates?";
        List<Object> replies = new ArrayList<>();
        replies.add(message(List.of(text("Three independent parts."), toolUse("toolu_01", "plan", map("subtasks", SUBTASKS.stream().map(s -> map("scope", s.scope(), "brief", s.brief())).toList()))), "tool_use"));
        for (String r : REPORTS) replies.add(message(List.of(text(r))));
        replies.add(message(List.of(text("Chip supply recovered first, car output followed, and rates stayed high."))));
        Scripted.Rig rig = Scripted.client(replies.toArray());
        List<Subtask> subtasks = plan(rig.client(), question);
        System.out.println("plan: " + subtasks.size() + " subtasks -> " + py(subtasks.stream().map(Subtask::scope).toList()));
        List<String[]> findings = new ArrayList<>();
        int number = 0;
        for (Subtask task : subtasks) {
            number++;
            String report = runSubagent(rig.client(), task.brief());
            findings.add(new String[] {task.scope(), report});
            var request = rig.http().requests.get(number);
            String body = request.toString();
            final String current = report;
            boolean others = REPORTS.stream().anyMatch(r -> !r.equals(current) && body.contains(tag(r)));
            System.out.println("subagent " + number + " request: " + request.get("messages").size() + " message, system prompt of the role: "
                + py(request.get("system").asText().equals(SUBAGENT_SYSTEM)) + ", holds the coordinator's question: " + py(body.contains(question))
                + ", holds another report: " + py(others));
        }
        String answer = synthesize(rig.client(), question, findings);
        String synthesis = rig.http().requests.get(4).toString();
        System.out.println("synthesis request: reports included: " + REPORTS.stream().filter(r -> synthesis.contains(tag(r))).count() + " of " + REPORTS.size());
        System.out.println("model calls: " + rig.http().requests.size() + " | one plan, three subagents, one synthesis");
        System.out.println("answer: " + answer);
    }
}
```
```text
plan: 3 subtasks -> ['chips', 'cars', 'rates']
subagent 1 request: 1 message, system prompt of the role: True, holds the coordinator's question: False, holds another report: False
subagent 2 request: 1 message, system prompt of the role: True, holds the coordinator's question: False, holds another report: False
subagent 3 request: 1 message, system prompt of the role: True, holds the coordinator's question: False, holds another report: False
synthesis request: reports included: 3 of 3
model calls: 5 | one plan, three subagents, one synthesis
answer: Chip supply recovered first, car output followed, and rates stayed high.
```
```kotlin
import com.anthropic.client.AnthropicClient
import com.anthropic.core.JsonValue
import com.anthropic.core.jsonMapper
import com.anthropic.models.messages.Message
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.Model
import com.anthropic.models.messages.Tool
import harness.Scripted
import harness.Scripted.map
import harness.Scripted.message
import harness.Scripted.text
import harness.Scripted.toolUse
import harness.Show.py

/**
 * Hub and spoke on the Messages API: a coordinator plans by calling a plan tool, each subagent is its own conversation, the coordinator synthesizes.
 *
 * The replies are illustrative, hand-written bodies in the shape of the Messages API (claude-sonnet-5-5), not captures. The point is what each
 * request contains: a subagent's request holds its brief and nothing else, and only the synthesis request holds the findings.
 */
const val MODEL = "claude-sonnet-5-5"
val PLAN_TOOL: Tool = Tool.builder().name("plan")
    .description("Record the plan: one subtask per independent part of the question, each with a scope and a self-contained brief.")
    .inputSchema(
        Tool.InputSchema.builder()
            .properties(
                JsonValue.from(
                    map("subtasks", map("type", "array", "items", map("type", "object", "properties", map("scope", map("type", "string"), "brief", map("type", "string")), "required", listOf("scope", "brief")))),
                ),
            )
            .required(listOf("subtasks")).build(),
    ).build()
const val SUBAGENT_SYSTEM = "You are a research subagent. Answer only the brief you are given and end with a one-line source note."

data class Subtask(val scope: String, val brief: String)

val SUBTASKS = listOf(
    Subtask("chips", "Find what changed in 2024 chip supply. Return three bullet points and a source note. Do not cover cars or interest rates."),
    Subtask("cars", "Find what changed in 2024 car output. Return three bullet points and a source note. Do not cover chips or interest rates."),
    Subtask("rates", "Find what changed in 2024 interest rates. Return three bullet points and a source note. Do not cover chips or cars."),
)
val REPORTS = listOf(
    "CHIPS-REPORT: output of foundries rose, lead times fell. Source: industry survey.",
    "CARS-REPORT: plants ran fuller as chips arrived. Source: producer filings.",
    "RATES-REPORT: central banks held rates, loans stayed dear. Source: bank statements.",
)

fun textOf(reply: Message): String = reply.content().filter { it.isText() }.joinToString("") { it.asText().text() }

fun plan(client: AnthropicClient, question: String): List<Subtask> {
    // tool_choice stays auto: Claude Sonnet 5.5 returns a 400 error for a forced choice, so the request asks for the tool in words
    val reply = client.messages().create(
        MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(800).addTool(PLAN_TOOL).addUserMessage("$question\nRecord your plan by calling the plan tool.").build(),
    )
    val use = reply.content().first { it.isToolUse() }.asToolUse()
    val input = jsonMapper().convertValue(use._input(), Map::class.java)
    return (input["subtasks"] as List<*>).map { s -> (s as Map<*, *>).let { Subtask(it["scope"] as String, it["brief"] as String) } }
}

/** A fresh conversation: the system prompt of the role and the brief. Nothing of the coordinator's history is passed. */
fun runSubagent(client: AnthropicClient, brief: String): String =
    textOf(client.messages().create(MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(800).system(SUBAGENT_SYSTEM).addUserMessage(brief).build()))

fun synthesize(client: AnthropicClient, question: String, findings: List<Pair<String, String>>): String {
    val listing = findings.joinToString("\n") { (scope, report) -> "[$scope] $report" }
    return textOf(
        client.messages().create(
            MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(800)
                .addUserMessage("Question: $question\nFindings:\n$listing\nWrite one answer that uses every finding.").build(),
        ),
    )
}

fun tag(report: String): String = report.substringBefore(":")

fun main() {
    val question = "How did the 2024 supply picture change for chips, cars and interest rates?"
    val replies = mutableListOf<Any>(
        message(listOf(text("Three independent parts."), toolUse("toolu_01", "plan", map("subtasks", SUBTASKS.map { map("scope", it.scope, "brief", it.brief) }))), "tool_use"),
    )
    REPORTS.forEach { replies += message(listOf(text(it))) }
    replies += message(listOf(text("Chip supply recovered first, car output followed, and rates stayed high.")))
    val rig = Scripted.client(*replies.toTypedArray())
    val subtasks = plan(rig.client(), question)
    println("plan: ${subtasks.size} subtasks -> ${py(subtasks.map { it.scope })}")
    val findings = mutableListOf<Pair<String, String>>()
    subtasks.forEachIndexed { index, task ->
        val number = index + 1
        val report = runSubagent(rig.client(), task.brief)
        findings += task.scope to report
        val request = rig.http().requests[number]
        val body = request.toString()
        val others = REPORTS.any { it != report && tag(it) in body }
        println(
            "subagent $number request: ${request["messages"].size()} message, system prompt of the role: ${py(request["system"].asText() == SUBAGENT_SYSTEM)}, " +
                "holds the coordinator's question: ${py(question in body)}, holds another report: ${py(others)}",
        )
    }
    val answer = synthesize(rig.client(), question, findings)
    val synthesis = rig.http().requests[4].toString()
    println("synthesis request: reports included: ${REPORTS.count { tag(it) in synthesis }} of ${REPORTS.size}")
    println("model calls: ${rig.http().requests.size} | one plan, three subagents, one synthesis")
    println("answer: $answer")
}
```
```text
plan: 3 subtasks -> ['chips', 'cars', 'rates']
subagent 1 request: 1 message, system prompt of the role: True, holds the coordinator's question: False, holds another report: False
subagent 2 request: 1 message, system prompt of the role: True, holds the coordinator's question: False, holds another report: False
subagent 3 request: 1 message, system prompt of the role: True, holds the coordinator's question: False, holds another report: False
synthesis request: reports included: 3 of 3
model calls: 5 | one plan, three subagents, one synthesis
answer: Chip supply recovered first, car output followed, and rates stayed high.
```
<!-- /example -->

Read the three subagent lines. Each request has one message, the role's system prompt, no copy of the coordinator's question and none of the other reports: the brief was written to be read alone, and the code never passed anything else. The synthesis request is the only one that holds all three reports, which is the point where the pieces meet. Five model calls in all: one plan, three subagents, one synthesis. Both languages print the same lines.

## Traps

These are the wrong answers that the exam's options for this task statement offer, each with the reason it is rejected.

1. **"Send every subagent the same broad brief, to be thorough."** It is tempting because it is quick to write. The exam rejects it: three subagents given "Research the supply chain" return three overlapping answers. Give each its facet, its period or source, its output format and a line saying what it must not cover.
2. **"Share the coordinator's conversation so that nobody lacks context."** It is tempting because it looks safe. The exam rejects it: it removes the isolation that keeps the context small, and it hands the subagents what they will repeat. A brief carries what the subagent needs, not what the coordinator has.
3. **"Always run the full team, for thoroughness."** It is tempting because a fixed pipeline is simple. The exam rejects it: a multi-agent system uses about fifteen times the tokens of a chat (the article's figure), so a fixed team for every query is expensive, and a task with shared context or many dependencies does not split. The coordinator first asks whether the parts are independent.
4. **"Split the topic into the few parts that are obvious."** It is tempting because the split is fast. The exam rejects it: a narrow split leaves part of a broad topic uncovered. The coordinator checks coverage afterwards (page 2) and sends out the gaps.
5. **"Let the subagents pass findings to each other to save the coordinator's context."** It is tempting because it shortens the path. The exam rejects it: all communication runs through the coordinator, for observability, one way of handling errors and control over what flows where.

## Quiz

1. A coordinator spends ten turns locating the broken check and its failure text, then tells a subagent only to "fix the broken check". The subagent opens other modules and edits the wrong one. What should the coordinator have done?
   - **a**: Put the path, the exact message and its decisions so far into the prompt it hands over
   - **b**: Rely on the subagent to inherit the parent's earlier tool results by itself, unprompted
   - **c**: Name a stronger model for the subagent in its definition file
   - **d**: Add a line telling the subagent to ask the coordinator whenever it is unsure

2. A team plans a multi-agent design for a feature whose planning, coding and verifying steps all draw on the same evolving sources, with the user steering between steps. Which choice fits best?
   - **a**: Run the three steps as parallel subagents to cut the total waiting time
   - **b**: Give each step to its own subagent so that every context stays clean and small
   - **c**: Stay in one single conversation, since the phases lean on shared context
   - **d**: Add a coordinator that merges what the three step-agents produce at the end

<details>
<summary>Answer key</summary>

1. **a**. The subagent's context starts fresh, so the brief is the only channel for facts the coordinator already found. *b* is ruled out because nothing is inherited: "The only content you pass from parent to subagent is the Agent tool's prompt string". *c* is ruled out because the problem is missing information, and "a vague brief produces vague work." *d* is ruled out because the subagent cannot converse with its parent while it runs: "only its final message returns to the parent".
2. **c**. The steps depend on one shared context and on steering, which a team cannot give. *b* is ruled out because the documentation lists this case for the main conversation: "multiple phases share significant context, such as planning, implementation, and testing". *a* is ruled out because a delegate loses time: a subagent that is not a fork "starts fresh and may need time to gather context". *d* is ruled out because the dependencies are the problem: "some domains that require all agents to share the same context or involve many dependencies between agents are not a good fit".

</details>
