# Graph-based, model-driven and typed frameworks

**Level:** Developer · **Module 37:** Agent frameworks compared · **Page 1 of 2**
**Exams:** DV3

**After this page you can** describe what a graph-based, a model-driven and a typed agent framework each gives you, say what a checkpoint is and why a graph needs one, and name the control each style trades away.

Checked on 2026-10-03 against the vendors' own pages: the Anthropic engineering post "Building effective agents" (published 2024-12-19), the LangGraph overview and persistence pages, the Strands Agents blog post of 2025-09-12 and the Pydantic AI home page. The frameworks change quickly, so the page teaches the families and quotes each vendor only for its own definition. The example runs offline with a scripted model, and it contains the course's own miniatures of the three styles, not any framework's code. The tabs are Python, TypeScript, Java and Kotlin.

## Why it matters

Teams reach for a framework before they know what they need from one. The exam asks you to recognise the three families by what they put under your control, and to give the reason a framework is, or is not, the right choice for a task. You do not need to know any API by heart.

## The idea

### Where the control sits

Every agent is a loop that asks a model what to do, runs the tool it picks and feeds back the result. The families differ in who owns the shape of that loop.

| Family | Who decides the next step | What the framework gives you |
|---|---|---|
| Graph-based | You, in advance: nodes and edges | State, checkpoints, resume, human approval |
| Model-driven | The model, at each turn | A loop, tool handling and stop conditions |
| Typed | The model, inside a schema | Validated output, typed dependencies |

The families are not exclusive. A graph can contain a model-driven node, and a typed agent can run inside a graph. They are the answer to a design question: how much of the route do I want to write down?

### Graph-based: you draw the route

LangGraph describes itself as a "low-level orchestration framework and runtime for building, managing, and deploying long-running, stateful agents." Its stated strength is "the ability to mix deterministic steps with LLM-driven agentic steps in a single graph". A graph has nodes, which do work, and edges, which choose the next node from the state. The model fills in a node. It does not pick the route.

The cost of drawing the route is what buys the main feature. When every step is a node with explicit state, the state can be saved after each node. LangGraph names this layer persistence: "Checkpointers persist a thread's graph state as checkpoints", used for "conversation continuity, human-in-the-loop workflows, time travel, and fault tolerance." A checkpoint is a copy of the state at a point in the graph. After a crash, or while a person decides, the run resumes from the last checkpoint and repeats no earlier node, so the model calls already paid for are not made again.

Use the style when the process is known and must be auditable: an approval chain, an order flow, a pipeline in which some steps must always run. Its limit is the other side of the same choice: a case you did not draw has no path.

### Model-driven: the model draws the route

The Strands Agents SDK takes the opposite position. Its authors write that "Traditional agent frameworks required developers to build elaborate orchestration logic, state machines, and predefined workflows to guide language models through tasks." Their answer is the model-driven approach: "we let modern large language models drive their own behavior, make intelligent decisions about tool usage, and adapt dynamically to whatever comes their way."

The framework shrinks to a loop, a set of tools and a stop rule. The route is whatever the model chooses, so the same code handles a case nobody planned for. The price is the usual one of an agent. "Agentic systems often trade latency and cost for better task performance", in the words of "Building effective agents", and a loop that never ends must be cut off: that post lists "stopping conditions (such as a maximum number of iterations) to maintain control." Claude Code's own loop, from module 35, is of this kind.

### Typed: the answer must fit a schema

Pydantic AI presents itself as "a typed, extensible agent loop with every model a string swap away." Its centre is the contract of the call. The agent is generic in the type of its dependencies and the type of its output, and a reply that does not validate against the output type is refused. In practice the failure goes back to the model as a retry message and the run continues, which turns a malformed reply into a recoverable event. The style fits services whose callers need a record of fixed shape: a refund amount in cents, a category from a list.

Typing and the other styles combine. A typed agent can be one node of a graph, and a model-driven loop can end with a typed result. Types guard the edges of the model's freedom. They do not choose the route.

<!-- example: m37-three-agent-styles tabs: python,typescript,java,kotlin -->
```python
"""One small task, three ways to build it: a graph you draw, a loop the model drives, and a typed result you validate.

The task is a support ticket: decide whether it is about billing, look the invoice up when it is, and write a reply. The three
functions below are miniatures of what graph-based, model-driven and typed agent frameworks give you. They are the course's own
sketches, not any framework's code, and the model is a scripted function, so nothing here calls an API.
"""
import logging
import json

log = logging.getLogger(__name__)

TICKET = "I was charged twice for invoice 1042."
INVOICES = {"1042": "paid twice on 2026-09-30"}


def scripted(replies):
    """A stand-in model: it returns the next scripted reply and keeps the prompts it was shown."""
    queue, seen = list(replies), []

    def model(prompt):
        seen.append(prompt)
        return queue.pop(0)

    model.seen = seen
    return model


# 1. Graph style: the programmer fixes the nodes and the edges; the model only fills in a node. State is explicit and checkpointed.

def run_graph(model, ticket, resume_from=0, checkpoints=None):
    def classify(s):
        return {"topic": model(f"Classify as billing or other: {s['ticket']}").strip().lower()}

    def lookup(s):
        return {"invoice": INVOICES.get(s["ticket"].split("invoice ")[1].rstrip("."), "unknown")}

    def draft(s):
        return {"reply": model(f"Write a reply. Topic: {s['topic']}. Invoice: {s.get('invoice', 'none')}")}

    nodes = {"classify": classify, "lookup": lookup, "draft": draft}
    edges = {"classify": lambda s: "lookup" if s["topic"] == "billing" else "draft", "lookup": lambda s: "draft", "draft": lambda s: None}
    checkpoints = checkpoints if checkpoints is not None else [("classify", {"ticket": ticket})]
    node, state = checkpoints[resume_from]
    del checkpoints[resume_from + 1:]
    path = []
    while node:
        state = {**state, **nodes[node](state)}
        path.append(node)
        node = edges[node](state)
        if node:
            checkpoints.append((node, dict(state)))
    return {"path": path, "state": state, "checkpoints": checkpoints}


# 2. Model-driven style: the model sees the tools and decides the next step; the loop only executes and stops.

def run_agent(model, ticket, max_steps=5):
    tools = {"lookup_invoice": lambda arg: INVOICES.get(arg, "unknown")}
    trace, observation = [], ""
    for _ in range(max_steps):
        step = json.loads(model(f"Ticket: {ticket}\nTools: {sorted(tools)}\nLast result: {observation}\nReply JSON: a tool call or a final reply."))
        if "final" in step:
            return {"reply": step["final"], "trace": trace}
        observation = tools[step["tool"]](step["arg"])
        trace.append(f"{step['tool']}({step['arg']}) -> {observation}")
    return {"reply": None, "trace": trace, "stopped": "max_steps"}


# 3. Typed style: the answer must match a schema; a mismatch goes back to the model as feedback, once.

SCHEMA = {"topic": str, "refund_cents": int}


def validate(raw):
    try:
        data = json.loads(raw)
    except json.JSONDecodeError:
        return None, "the reply is not JSON"
    for field, kind in SCHEMA.items():
        if not isinstance(data.get(field), kind) or isinstance(data.get(field), bool):
            return None, f"field {field} must be {kind.__name__}"
    return data, None


def run_typed(model, ticket, retries=1):
    prompt, attempts = f"Return JSON with topic and refund_cents for: {ticket}", 0
    while True:
        attempts += 1
        data, error = validate(model(prompt))
        if data is not None:
            return {"data": data, "attempts": attempts}
        if attempts > retries:
            return {"data": None, "attempts": attempts, "error": error}
        prompt = f"{prompt}\nYour last reply was refused: {error}. Fix it."


def main():
    m = scripted(["billing", "We refunded the duplicate charge on invoice 1042."])
    graph = run_graph(m, TICKET)
    print("graph:", " -> ".join(graph["path"]), "| model calls:", len(m.seen), "| checkpoints:", len(graph["checkpoints"]))
    again = scripted(["We refunded the duplicate charge on invoice 1042."])
    resumed = run_graph(again, TICKET, resume_from=2, checkpoints=list(graph["checkpoints"]))
    print("graph resumed from checkpoint 2:", " -> ".join(resumed["path"]), "| model calls:", len(again.seen), "| same reply:", resumed["state"]["reply"] == graph["state"]["reply"])
    a = scripted(['{"tool": "lookup_invoice", "arg": "1042"}', '{"final": "Refunded the second payment."}'])
    agent = run_agent(a, TICKET)
    print("agent:", agent["trace"], "->", agent["reply"], "| model calls:", len(a.seen))
    loop = scripted(['{"tool": "lookup_invoice", "arg": "1"}'] * 3)
    print("agent that never finishes:", run_agent(loop, TICKET, max_steps=3)["stopped"], "after", len(loop.seen), "calls")
    t = scripted(['{"topic": "billing", "refund_cents": "4999"}', '{"topic": "billing", "refund_cents": 4999}'])
    typed = run_typed(t, TICKET)
    print("typed:", typed["data"], "| attempts:", typed["attempts"], "| second prompt ends:", t.seen[1].split("\n")[-1])
    bad = run_typed(scripted(["no json", "still no json"]), TICKET)
    print("typed, never valid:", bad["error"], "after", bad["attempts"], "attempts")


if __name__ == "__main__":
    main()
```
```text
graph: classify -> lookup -> draft | model calls: 2 | checkpoints: 3
graph resumed from checkpoint 2: draft | model calls: 1 | same reply: True
agent: ['lookup_invoice(1042) -> paid twice on 2026-09-30'] -> Refunded the second payment. | model calls: 2
agent that never finishes: max_steps after 3 calls
typed: {'topic': 'billing', 'refund_cents': 4999} | attempts: 2 | second prompt ends: Your last reply was refused: field refund_cents must be int. Fix it.
typed, never valid: the reply is not JSON after 2 attempts
```
```typescript
// One small task, three ways to build it: a graph you draw, a loop the model drives, and a typed result you validate.
//
// The task is a support ticket: decide whether it is about billing, look the invoice up when it is, and write a reply. The three
// functions below are miniatures of what graph-based, model-driven and typed agent frameworks give you. They are the course's own
// sketches, not any framework's code, and the model is a scripted function, so nothing here calls an API.
import { logger } from "./logger.ts";
const log = logger("three_styles");

export const TICKET = "I was charged twice for invoice 1042.";
const INVOICES: Record<string, string> = { "1042": "paid twice on 2026-09-30" };

export type Model = ((prompt: string) => string) & { seen: string[] };
type State = Record<string, any>;
type Checkpoint = [string, State];

/** A stand-in model: it returns the next scripted reply and keeps the prompts it was shown. */
export function scripted(replies: string[]): Model {
  const queue = [...replies];
  const seen: string[] = [];
  const model = ((prompt: string) => {
    seen.push(prompt);
    return queue.shift() as string;
  }) as Model;
  model.seen = seen;
  return model;
}

// 1. Graph style: the programmer fixes the nodes and the edges; the model only fills in a node. State is explicit and checkpointed.

export function runGraph(model: Model, ticket: string, resumeFrom = 0, checkpoints?: Checkpoint[]) {
  const nodes: Record<string, (s: State) => State> = {
    classify: (s) => ({ topic: model(`Classify as billing or other: ${s.ticket}`).trim().toLowerCase() }),
    lookup: (s) => ({ invoice: INVOICES[s.ticket.split("invoice ")[1].replace(/\.+$/, "")] ?? "unknown" }),
    draft: (s) => ({ reply: model(`Write a reply. Topic: ${s.topic}. Invoice: ${s.invoice ?? "none"}`) }),
  };
  const edges: Record<string, (s: State) => string | null> = {
    classify: (s) => (s.topic === "billing" ? "lookup" : "draft"),
    lookup: () => "draft",
    draft: () => null,
  };
  const saved: Checkpoint[] = checkpoints ?? [["classify", { ticket }]];
  let [node, state]: [string | null, State] = saved[resumeFrom];
  saved.length = resumeFrom + 1;
  const path: string[] = [];
  while (node) {
    state = { ...state, ...nodes[node](state) };
    path.push(node);
    node = edges[node](state);
    if (node) saved.push([node, { ...state }]);
  }
  return { path, state, checkpoints: saved };
}

// 2. Model-driven style: the model sees the tools and decides the next step; the loop only executes and stops.

export function runAgent(model: Model, ticket: string, maxSteps = 5): { reply: string | null; trace: string[]; stopped?: string } {
  const tools: Record<string, (arg: string) => string> = { lookup_invoice: (arg) => INVOICES[arg] ?? "unknown" };
  const trace: string[] = [];
  let observation = "";
  for (let i = 0; i < maxSteps; i++) {
    const step = JSON.parse(model(`Ticket: ${ticket}\nTools: ${JSON.stringify(Object.keys(tools).sort()).replace(/"/g, "'").replace(/,/g, ", ")}\nLast result: ${observation}\nReply JSON: a tool call or a final reply.`));
    if ("final" in step) return { reply: step.final, trace };
    observation = tools[step.tool](step.arg);
    trace.push(`${step.tool}(${step.arg}) -> ${observation}`);
  }
  return { reply: null, trace, stopped: "max_steps" };
}

// 3. Typed style: the answer must match a schema; a mismatch goes back to the model as feedback, once.

const SCHEMA: Record<string, "string" | "integer"> = { topic: "string", refund_cents: "integer" };
const KIND_NAME = { string: "str", integer: "int" };

export function validate(raw: string): [Record<string, any> | null, string | null] {
  let data: any;
  try {
    data = JSON.parse(raw);
  } catch {
    return [null, "the reply is not JSON"];
  }
  for (const [field, kind] of Object.entries(SCHEMA)) {
    const ok = kind === "string" ? typeof data?.[field] === "string" : Number.isInteger(data?.[field]);
    if (!ok) return [null, `field ${field} must be ${KIND_NAME[kind]}`];
  }
  return [data, null];
}

export function runTyped(model: Model, ticket: string, retries = 1) {
  let prompt = `Return JSON with topic and refund_cents for: ${ticket}`;
  let attempts = 0;
  for (;;) {
    attempts++;
    const [data, error] = validate(model(prompt));
    if (data) return { data, attempts };
    if (attempts > retries) return { data: null, attempts, error };
    prompt = `${prompt}\nYour last reply was refused: ${error}. Fix it.`;
  }
}

function show(value: unknown): string {
  if (Array.isArray(value)) return `[${value.map(show).join(", ")}]`;
  if (value && typeof value === "object") return `{${Object.entries(value).map(([k, v]) => `'${k}': ${show(v)}`).join(", ")}}`;
  return typeof value === "string" ? `'${value}'` : String(value);
}

function main() {
  const m = scripted(["billing", "We refunded the duplicate charge on invoice 1042."]);
  const graph = runGraph(m, TICKET);
  console.log("graph:", graph.path.join(" -> "), "| model calls:", m.seen.length, "| checkpoints:", graph.checkpoints.length);
  const again = scripted(["We refunded the duplicate charge on invoice 1042."]);
  const resumed = runGraph(again, TICKET, 2, [...graph.checkpoints]);
  console.log("graph resumed from checkpoint 2:", resumed.path.join(" -> "), "| model calls:", again.seen.length, "| same reply:", resumed.state.reply === graph.state.reply ? "True" : "False");
  const a = scripted(['{"tool": "lookup_invoice", "arg": "1042"}', '{"final": "Refunded the second payment."}']);
  const agent = runAgent(a, TICKET);
  console.log("agent:", show(agent.trace), "->", agent.reply, "| model calls:", a.seen.length);
  const loop = scripted(Array(3).fill('{"tool": "lookup_invoice", "arg": "1"}'));
  console.log("agent that never finishes:", runAgent(loop, TICKET, 3).stopped, "after", loop.seen.length, "calls");
  const t = scripted(['{"topic": "billing", "refund_cents": "4999"}', '{"topic": "billing", "refund_cents": 4999}']);
  const typed = runTyped(t, TICKET);
  console.log("typed:", show(typed.data), "| attempts:", typed.attempts, "| second prompt ends:", t.seen[1].split("\n").at(-1));
  const bad = runTyped(scripted(["no json", "still no json"]), TICKET);
  console.log("typed, never valid:", bad.error, "after", bad.attempts, "attempts");
}

if (import.meta.main) main();
```
```text
graph: classify -> lookup -> draft | model calls: 2 | checkpoints: 3
graph resumed from checkpoint 2: draft | model calls: 1 | same reply: True
agent: ['lookup_invoice(1042) -> paid twice on 2026-09-30'] -> Refunded the second payment. | model calls: 2
agent that never finishes: max_steps after 3 calls
typed: {'topic': 'billing', 'refund_cents': 4999} | attempts: 2 | second prompt ends: Your last reply was refused: field refund_cents must be int. Fix it.
typed, never valid: the reply is not JSON after 2 attempts
```
```java
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * One small task, three ways to build it: a graph you draw, a loop the model drives, and a typed result you validate.
 *
 * <p>The task is a support ticket: decide whether it is about billing, look the invoice up when it is, and write a reply. The three
 * functions below are miniatures of what graph-based, model-driven and typed agent frameworks give you. They are the course's own
 * sketches, not any framework's code, and the model is a scripted function, so nothing here calls an API.
 */
public final class ThreeStyles {
    private static final System.Logger LOG = System.getLogger(ThreeStyles.class.getName());
    static final String TICKET = "I was charged twice for invoice 1042.";
    static final Map<String, String> INVOICES = Map.of("1042", "paid twice on 2026-09-30");
    private static final ObjectMapper JSON = new ObjectMapper();

    /** A stand-in model: it returns the next scripted reply and keeps the prompts it was shown. */
    static final class Scripted implements Function<String, String> {
        final List<String> queue;
        final List<String> seen = new ArrayList<>();

        Scripted(String... replies) {
            queue = new ArrayList<>(List.of(replies));
        }

        @Override
        public String apply(String prompt) {
            seen.add(prompt);
            return queue.remove(0);
        }
    }

    // 1. Graph style: the programmer fixes the nodes and the edges; the model only fills in a node. State is explicit and checkpointed.

    record Checkpoint(String node, Map<String, String> state) {}

    record GraphRun(List<String> path, Map<String, String> state, List<Checkpoint> checkpoints) {}

    static GraphRun runGraph(Function<String, String> model, String ticket, int resumeFrom, List<Checkpoint> given) {
        Map<String, Function<Map<String, String>, Map<String, String>>> nodes = Map.of(
            "classify", s -> Map.of("topic", model.apply("Classify as billing or other: " + s.get("ticket")).strip().toLowerCase(java.util.Locale.ROOT)),
            "lookup", s -> Map.of("invoice", INVOICES.getOrDefault(s.get("ticket").split("invoice ")[1].replaceAll("\\.+$", ""), "unknown")),
            "draft", s -> Map.of("reply", model.apply("Write a reply. Topic: " + s.get("topic") + ". Invoice: " + s.getOrDefault("invoice", "none"))));
        Map<String, Function<Map<String, String>, String>> edges = Map.of(
            "classify", s -> s.get("topic").equals("billing") ? "lookup" : "draft",
            "lookup", s -> "draft",
            "draft", s -> null);
        List<Checkpoint> checkpoints = given != null ? given : new ArrayList<>(List.of(new Checkpoint("classify", Map.of("ticket", ticket))));
        String node = checkpoints.get(resumeFrom).node();
        Map<String, String> state = checkpoints.get(resumeFrom).state();
        while (checkpoints.size() > resumeFrom + 1) checkpoints.remove(checkpoints.size() - 1);
        List<String> path = new ArrayList<>();
        while (node != null) {
            Map<String, String> next = new LinkedHashMap<>(state);
            next.putAll(nodes.get(node).apply(state));
            state = next;
            path.add(node);
            node = edges.get(node).apply(state);
            if (node != null) checkpoints.add(new Checkpoint(node, new LinkedHashMap<>(state)));
        }
        return new GraphRun(path, state, checkpoints);
    }

    // 2. Model-driven style: the model sees the tools and decides the next step; the loop only executes and stops.

    record AgentRun(String reply, List<String> trace, String stopped) {}

    @SuppressWarnings("unchecked")
    static AgentRun runAgent(Function<String, String> model, String ticket, int maxSteps) throws Exception {
        Map<String, Function<String, String>> tools = Map.of("lookup_invoice", arg -> INVOICES.getOrDefault(arg, "unknown"));
        List<String> trace = new ArrayList<>();
        String observation = "";
        for (int i = 0; i < maxSteps; i++) {
            Map<String, Object> step = JSON.readValue(model.apply("Ticket: " + ticket + "\nTools: " + py(tools.keySet().stream().sorted().toList())
                + "\nLast result: " + observation + "\nReply JSON: a tool call or a final reply."), Map.class);
            if (step.containsKey("final")) return new AgentRun((String) step.get("final"), trace, null);
            observation = tools.get((String) step.get("tool")).apply((String) step.get("arg"));
            trace.add(step.get("tool") + "(" + step.get("arg") + ") -> " + observation);
        }
        return new AgentRun(null, trace, "max_steps");
    }

    // 3. Typed style: the answer must match a schema; a mismatch goes back to the model as feedback, once.

    /** The checked reply, or the reason it was refused. */
    record Checked(Map<String, Object> data, String error) {}

    record TypedRun(Map<String, Object> data, int attempts, String error) {}

    static Checked validate(String raw) {
        Map<String, Object> data;
        try {
            data = JSON.readValue(raw, new com.fasterxml.jackson.core.type.TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (Exception e) {
            return new Checked(null, "the reply is not JSON");
        }
        if (!(data.get("topic") instanceof String)) return new Checked(null, "field topic must be str");
        if (!(data.get("refund_cents") instanceof Integer)) return new Checked(null, "field refund_cents must be int");
        return new Checked(data, null);
    }

    static TypedRun runTyped(Function<String, String> model, String ticket, int retries) {
        String prompt = "Return JSON with topic and refund_cents for: " + ticket;
        int attempts = 0;
        while (true) {
            attempts++;
            Checked checked = validate(model.apply(prompt));
            if (checked.data() != null) return new TypedRun(checked.data(), attempts, null);
            if (attempts > retries) return new TypedRun(null, attempts, checked.error());
            prompt = prompt + "\nYour last reply was refused: " + checked.error() + ". Fix it.";
        }
    }

    /** Python's repr of strings, lists and maps, so every language of the course prints the same text. */
    static String py(Object v) {
        if (v == null) return "None";
        if (v instanceof String s) {
            String q = s.contains("'") && !s.contains("\"") ? "\"" : "'";
            return q + s.replace("\\", "\\\\").replace("\n", "\\n").replace(q, "\\" + q) + q;
        }
        if (v instanceof Map<?, ?> m) return m.entrySet().stream().map(e -> py(e.getKey()) + ": " + py(e.getValue())).collect(Collectors.joining(", ", "{", "}"));
        if (v instanceof List<?> l) return l.stream().map(ThreeStyles::py).collect(Collectors.joining(", ", "[", "]"));
        return String.valueOf(v);
    }

    public static void main(String[] args) throws Exception {
        Scripted m = new Scripted("billing", "We refunded the duplicate charge on invoice 1042.");
        GraphRun graph = runGraph(m, TICKET, 0, null);
        System.out.println("graph: " + String.join(" -> ", graph.path()) + " | model calls: " + m.seen.size() + " | checkpoints: " + graph.checkpoints().size());
        Scripted again = new Scripted("We refunded the duplicate charge on invoice 1042.");
        GraphRun resumed = runGraph(again, TICKET, 2, new ArrayList<>(graph.checkpoints()));
        System.out.println("graph resumed from checkpoint 2: " + String.join(" -> ", resumed.path()) + " | model calls: " + again.seen.size()
            + " | same reply: " + (resumed.state().get("reply").equals(graph.state().get("reply")) ? "True" : "False"));
        Scripted a = new Scripted("{\"tool\": \"lookup_invoice\", \"arg\": \"1042\"}", "{\"final\": \"Refunded the second payment.\"}");
        AgentRun agent = runAgent(a, TICKET, 5);
        System.out.println("agent: " + py(agent.trace()) + " -> " + agent.reply() + " | model calls: " + a.seen.size());
        Scripted loop = new Scripted("{\"tool\": \"lookup_invoice\", \"arg\": \"1\"}", "{\"tool\": \"lookup_invoice\", \"arg\": \"1\"}", "{\"tool\": \"lookup_invoice\", \"arg\": \"1\"}");
        System.out.println("agent that never finishes: " + runAgent(loop, TICKET, 3).stopped() + " after " + loop.seen.size() + " calls");
        Scripted t = new Scripted("{\"topic\": \"billing\", \"refund_cents\": \"4999\"}", "{\"topic\": \"billing\", \"refund_cents\": 4999}");
        TypedRun typed = runTyped(t, TICKET, 1);
        String[] lines = t.seen.get(1).split("\n");
        System.out.println("typed: " + py(typed.data()) + " | attempts: " + typed.attempts() + " | second prompt ends: " + lines[lines.length - 1]);
        TypedRun bad = runTyped(new Scripted("no json", "still no json"), TICKET, 1);
        System.out.println("typed, never valid: " + bad.error() + " after " + bad.attempts() + " attempts");
    }
}
```
```text
graph: classify -> lookup -> draft | model calls: 2 | checkpoints: 3
graph resumed from checkpoint 2: draft | model calls: 1 | same reply: True
agent: ['lookup_invoice(1042) -> paid twice on 2026-09-30'] -> Refunded the second payment. | model calls: 2
agent that never finishes: max_steps after 3 calls
typed: {'topic': 'billing', 'refund_cents': 4999} | attempts: 2 | second prompt ends: Your last reply was refused: field refund_cents must be int. Fix it.
typed, never valid: the reply is not JSON after 2 attempts
```
```kotlin
import com.fasterxml.jackson.databind.ObjectMapper

private val log = System.getLogger("three_styles")

/**
 * One small task, three ways to build it: a graph you draw, a loop the model drives, and a typed result you validate.
 *
 * The task is a support ticket: decide whether it is about billing, look the invoice up when it is, and write a reply. The three
 * functions below are miniatures of what graph-based, model-driven and typed agent frameworks give you. They are the course's own
 * sketches, not any framework's code, and the model is a scripted function, so nothing here calls an API.
 */
const val TICKET = "I was charged twice for invoice 1042."
val INVOICES = mapOf("1042" to "paid twice on 2026-09-30")
private val JSON = ObjectMapper()

/** A stand-in model: it returns the next scripted reply and keeps the prompts it was shown. */
class Scripted(vararg replies: String) : (String) -> String {
    private val queue = replies.toMutableList()
    val seen = mutableListOf<String>()

    override fun invoke(prompt: String): String {
        seen += prompt
        return queue.removeAt(0)
    }
}

// 1. Graph style: the programmer fixes the nodes and the edges; the model only fills in a node. State is explicit and checkpointed.

typealias State = Map<String, String>

data class Checkpoint(val node: String, val state: State)

data class GraphRun(val path: List<String>, val state: State, val checkpoints: MutableList<Checkpoint>)

fun runGraph(model: (String) -> String, ticket: String, resumeFrom: Int = 0, given: MutableList<Checkpoint>? = null): GraphRun {
    val nodes = mapOf<String, (State) -> State>(
        "classify" to { s -> mapOf("topic" to model("Classify as billing or other: ${s["ticket"]}").trim().lowercase()) },
        "lookup" to { s -> mapOf("invoice" to (INVOICES[s.getValue("ticket").split("invoice ")[1].trimEnd('.')] ?: "unknown")) },
        "draft" to { s -> mapOf("reply" to model("Write a reply. Topic: ${s["topic"]}. Invoice: ${s["invoice"] ?: "none"}")) },
    )
    val edges = mapOf<String, (State) -> String?>(
        "classify" to { s -> if (s["topic"] == "billing") "lookup" else "draft" },
        "lookup" to { "draft" },
        "draft" to { null },
    )
    val checkpoints = given ?: mutableListOf(Checkpoint("classify", mapOf("ticket" to ticket)))
    var node: String? = checkpoints[resumeFrom].node
    var state = checkpoints[resumeFrom].state
    while (checkpoints.size > resumeFrom + 1) checkpoints.removeLast()
    val path = mutableListOf<String>()
    while (node != null) {
        state = state + nodes.getValue(node)(state)
        path += node
        node = edges.getValue(node)(state)
        if (node != null) checkpoints += Checkpoint(node, state.toMap())
    }
    return GraphRun(path, state, checkpoints)
}

// 2. Model-driven style: the model sees the tools and decides the next step; the loop only executes and stops.

data class AgentRun(val reply: String?, val trace: List<String>, val stopped: String? = null)

fun runAgent(model: (String) -> String, ticket: String, maxSteps: Int = 5): AgentRun {
    val tools = mapOf<String, (String) -> String>("lookup_invoice" to { arg -> INVOICES[arg] ?: "unknown" })
    val trace = mutableListOf<String>()
    var observation = ""
    repeat(maxSteps) {
        @Suppress("UNCHECKED_CAST")
        val step = JSON.readValue(model("Ticket: $ticket\nTools: ${py(tools.keys.sorted())}\nLast result: $observation\nReply JSON: a tool call or a final reply."), Map::class.java) as Map<String, Any?>
        if ("final" in step) return AgentRun(step["final"] as String, trace)
        observation = tools.getValue(step["tool"] as String)(step["arg"] as String)
        trace += "${step["tool"]}(${step["arg"]}) -> $observation"
    }
    return AgentRun(null, trace, "max_steps")
}

// 3. Typed style: the answer must match a schema; a mismatch goes back to the model as feedback, once.

/** The checked reply, or the reason it was refused. */
data class Checked(val data: Map<String, Any?>?, val error: String?)

data class TypedRun(val data: Map<String, Any?>?, val attempts: Int, val error: String? = null)

fun validate(raw: String): Checked {
    @Suppress("UNCHECKED_CAST")
    val data = runCatching { JSON.readValue(raw, LinkedHashMap::class.java) as Map<String, Any?> }.getOrNull() ?: return Checked(null, "the reply is not JSON")
    if (data["topic"] !is String) return Checked(null, "field topic must be str")
    if (data["refund_cents"] !is Int) return Checked(null, "field refund_cents must be int")
    return Checked(data, null)
}

fun runTyped(model: (String) -> String, ticket: String, retries: Int = 1): TypedRun {
    var prompt = "Return JSON with topic and refund_cents for: $ticket"
    var attempts = 0
    while (true) {
        attempts++
        val (data, error) = validate(model(prompt))
        if (data != null) return TypedRun(data, attempts)
        if (attempts > retries) return TypedRun(null, attempts, error)
        prompt = "$prompt\nYour last reply was refused: $error. Fix it."
    }
}

/** Python's repr of strings, lists and maps, so every language of the course prints the same text. */
fun py(v: Any?): String = when (v) {
    null -> "None"
    is String -> (if ("'" in v && "\"" !in v) "\"" else "'").let { q -> q + v.replace("\\", "\\\\").replace("\n", "\\n").replace(q, "\\" + q) + q }
    is Map<*, *> -> v.entries.joinToString(", ", "{", "}") { "${py(it.key)}: ${py(it.value)}" }
    is List<*> -> v.joinToString(", ", "[", "]") { py(it) }
    else -> v.toString()
}

fun main() {
    val m = Scripted("billing", "We refunded the duplicate charge on invoice 1042.")
    val graph = runGraph(m, TICKET)
    println("graph: ${graph.path.joinToString(" -> ")} | model calls: ${m.seen.size} | checkpoints: ${graph.checkpoints.size}")
    val again = Scripted("We refunded the duplicate charge on invoice 1042.")
    val resumed = runGraph(again, TICKET, resumeFrom = 2, given = graph.checkpoints.toMutableList())
    println("graph resumed from checkpoint 2: ${resumed.path.joinToString(" -> ")} | model calls: ${again.seen.size} | same reply: ${if (resumed.state["reply"] == graph.state["reply"]) "True" else "False"}")
    val a = Scripted("""{"tool": "lookup_invoice", "arg": "1042"}""", """{"final": "Refunded the second payment."}""")
    val agent = runAgent(a, TICKET)
    println("agent: ${py(agent.trace)} -> ${agent.reply} | model calls: ${a.seen.size}")
    val loop = Scripted(*Array(3) { """{"tool": "lookup_invoice", "arg": "1"}""" })
    println("agent that never finishes: ${runAgent(loop, TICKET, maxSteps = 3).stopped} after ${loop.seen.size} calls")
    val t = Scripted("""{"topic": "billing", "refund_cents": "4999"}""", """{"topic": "billing", "refund_cents": 4999}""")
    val typed = runTyped(t, TICKET)
    println("typed: ${py(typed.data)} | attempts: ${typed.attempts} | second prompt ends: ${t.seen[1].lines().last()}")
    val bad = runTyped(Scripted("no json", "still no json"), TICKET)
    println("typed, never valid: ${bad.error} after ${bad.attempts} attempts")
}
```
```text
graph: classify -> lookup -> draft | model calls: 2 | checkpoints: 3
graph resumed from checkpoint 2: draft | model calls: 1 | same reply: True
agent: ['lookup_invoice(1042) -> paid twice on 2026-09-30'] -> Refunded the second payment. | model calls: 2
agent that never finishes: max_steps after 3 calls
typed: {'topic': 'billing', 'refund_cents': 4999} | attempts: 2 | second prompt ends: Your last reply was refused: field refund_cents must be int. Fix it.
typed, never valid: the reply is not JSON after 2 attempts
```
<!-- /example -->

The example solves one support ticket three ways with a scripted model. The graph calls the model for two of its three nodes and saves three checkpoints, and a run resumed from checkpoint 2 makes one model call and writes the same reply. The agent chooses its own tool, and a run that never finishes is cut off after three calls by `max_steps`. The typed run sends the validation error back as a retry and succeeds on the second attempt, and a model that never returns JSON fails after two attempts with an error that names the cause.

## Traps

1. **Choosing the family by the brand.** The question is who should own the route, not which library is popular. A fixed approval chain wants a graph, and an open-ended research task wants a loop.
2. **Expecting a checkpoint in a loop with no state of its own.** A model-driven loop keeps its history, but resuming it means replaying a conversation, while a graph resumes from a node.
3. **Calling a typed result a correct result.** Validation proves the shape. A refund of 4999 cents passes the schema whether or not it is right.

## Quiz

1. A bank's loan flow must always check identity, then credit, then ask a person to approve, and an interrupted application must not repeat the credit check. Which style fits?
   - **a**: A single prompt, because the order is stated in the text of the call
   - **b**: A model-driven loop, because the model picks each next step on its own
   - **c**: A typed agent, because the loan is a record of fixed shape and size
   - **d**: A graph, because each node saves state and resumes after a stop

2. A team's agent receives a request type nobody planned for. Which style handles it with the least extra code?
   - **a**: A model-driven loop, since the model chooses its own steps
   - **b**: A graph with a node written for every request type that may arrive
   - **c**: A graph, since a missing edge falls back to the last node of the flow
   - **d**: A fixed script, since the replies follow a template that was written

3. A model-driven loop calls a tool again and again and never produces a final reply. What does the engineering post recommend?
   - **a**: A second model that repeats the same call for confirmation
   - **b**: A larger context window, so that the loop can finish its work
   - **c**: A stopping condition such as a maximum number of iterations
   - **d**: A typed schema, which ends the loop when the data is valid

<details>
<summary>Answer key</summary>

1. **d**. The page says "Checkpointers persist a thread's graph state as checkpoints", and a resumed run "repeats no earlier node", so the credit check is not paid for twice. *b* is ruled out because the table gives the next step to "The model, at each turn", and the loan order must always be fixed. *c* is ruled out because a typed agent has "Validated output" but "Types guard the edges of the model's freedom. They do not choose the route." *a* is ruled out because a prompt keeps no state, and the page gives the graph, not the prompt, the job of resuming: "the run resumes from the last checkpoint".
2. **a**. The page says the model-driven route is "whatever the model chooses, so the same code handles a case nobody planned for." *c* is ruled out because "a case you did not draw has no path", and there is no fallback edge. *b* is ruled out because the graph is for a process that is "known and must be auditable", and a node for every type is the elaborate logic the model-driven authors reject: "elaborate orchestration logic, state machines, and predefined workflows". *d* is ruled out because a script is a route drawn in advance, and "a case you did not draw has no path".
3. **c**. The post lists "stopping conditions (such as a maximum number of iterations) to maintain control." *b* is ruled out because "a loop that never ends must be cut off", and a larger window does not give the loop an end. *a* is ruled out because a repeat of the same call adds cost without a stop, and "Agentic systems often trade latency and cost for better task performance". *d* is ruled out because a schema checks shape, and "Validation proves the shape", not that a loop has finished.

</details>
