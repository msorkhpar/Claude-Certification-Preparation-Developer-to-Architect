# What to keep, where it failed and what must not be logged

**Level:** Architect Professional · **Module 87:** Observability at scale · **Page 1 of 2**
**Exams:** P3, P4

**After this page you can** say what metrics, events and traces each answer, choose a rule for which traces a system keeps when it cannot keep all of them, find the layer that failed in a trace across several agents and tools, decide which fields a log record may hold, and tie the records of one request together.

Checked on 2026-10-04 against the Claude Code documentation page "Monitoring" (OpenTelemetry metrics, events and traces), Anthropic's article "How we built our multi-agent research system", the Claude API documentation page "Errors" (the request id), the Claude Certified Architect - Professional exam guide (version 1.0, domains 3 and 4), and by running the example offline in the course container. Nothing here called a model, and the traces, metrics and events in the example are invented. This page deepens module 43 (reading one failing trace), module 15 (logging rules for one call) and module 66 (errors across agents) to the scale of a production system. Drift, alerts and the practice are the second page.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* domain 3 asks the candidate to "Analyze observability challenges and select monitoring strategies at scale", and domain 4 to "Monitor system performance using logging and observability tools". The guide names no tool and no signal: the items are about strategy. *What the current product does (documentation read 2026-10-04):* Claude Code exports telemetry through OpenTelemetry, "metrics as time series data", "events via the logs/events protocol", and "optionally distributed traces", the last in beta and off by default. Content is redacted by default and each kind of content is switched on by its own variable. On the exam, answer with the strategy: which signal, which sampling rule, which fields. In a design, name the signal and the setting. The sampling rule, the drift check and the alert rule on these pages are the course's own design choices, taught as general practice for any system, and not documented features of Claude.

## Why it matters

A support platform runs twelve agents and handles two million conversations a day. Its dashboard is green: the error rate is 0.4 percent and latency is steady. Yet users write that the assistant "did not find obvious information", and nobody can say why. Storing every trace costs more than the team will pay, so someone proposes to keep a random one percent. The exam gives this situation and asks what to do. The design question under it is the whole topic of the module: what to measure, what to keep, how to find the layer that failed, and what must never be written down.

## The idea

### Three signals, three questions

- **Metrics** are numbers over time: error rate, tokens per answer, tool errors per thousand calls. They are cheap, they aggregate, and they drive dashboards and alerts. A metric says that something changed. It never says where or why.
- **Events and logs** are one record for each thing that happened: a tool call, a model request, a refusal. They answer what happened on one occasion.
- **Traces** link the steps of one request across every component: the prompt, the model requests and the tool calls it triggered. Claude Code's documentation says its spans "link each user prompt to the API requests and tool executions it triggers, so you can view a full request as a single trace". A trace answers where in the chain the request went wrong.

A monitoring strategy uses all three for what each is cheap at: metrics for the alarm, traces for the location, events for the detail.

### Why agents need traces

Agents make choices, and the choices vary. Anthropic's research-system article says "Agents make dynamic decisions and are non-deterministic between runs, even with identical prompts", and that users reported agents "not finding obvious information" while the team "couldn't see why". Its questions are the ones a trace answers: "Were the agents using bad search queries? Choosing poor sources? Hitting tool failures?" Its remedy: "Adding full production tracing let us diagnose why agents failed and fix issues systematically." A single error rate cannot separate those three causes, and in a system of agents the cause may be two hops from the symptom.

### What to keep when you cannot keep everything

Keeping every trace is rarely affordable, so the system must choose, and the choice decides what you will be able to explain later. Head-based sampling decides when the request starts, at random. Failures are rare, so a random share of one percent holds almost none of them, and five percent holds few more. Sampling by customer size covers heavy use and says nothing about which requests failed. Tail-based sampling decides when the trace is complete, so it can look at the outcome. The example keeps a trace for the first of these reasons, in this order:

1. **error**: any span failed.
2. **slow**: the root took longer than a limit.
3. **retries**: one tool was called three times or more, a sign of a loop that eventually succeeded.
4. **feedback**: a user flagged the answer as bad.
5. **sampled**: the rest, kept by a share of their ids.

The share is taken from a number computed from the trace id alone, so every service, in any language, keeps the same traces and drops the same ones. A trace that is half kept is worse than one that is dropped. In the example, a trace with an error and a slow root is labelled error: the label says what to look at first, and the order of the checks is the policy. The tradeoff is in the last rule: the sampled share is what lets you see what normal looks like, which a failure-only store cannot.

### Finding the layer that failed

In a trace of agents, errors travel upward. The orchestrator reports that the research step failed because the researcher reported that its fetch tool failed. The span that reports an error is often only passing on the error of the span below it, and the first span to turn red is not the origin. The example's `root_cause` takes the failing spans and finds the one that no other failing span has as its parent: the deepest failure. It returns that span's layer (agent, model, tool or retrieval), its name, and the path of names from the root. When nothing failed, the answer is not "nothing wrong". Confident wrong answers follow a stale or empty retrieval, and the example blames a retrieval span that reported `stale` or `no-hits`. The exam's third sample item says as much: confident wrong answers after a document refresh, with model and latency unchanged, point to retrieval, "for example a broken re-index or mismatched embeddings" (module 85 builds that side).

The order matters in the other direction too. A failed span is a fact, and a stale retrieval is a suspicion. A system that blames the retrieval whenever it can will send engineers to the index while a tool is broken.

### What must not be logged

Telemetry is a copy of your users' data in a place with a different audience. Anthropic's article describes its own practice: "we monitor agent decision patterns and interaction structures—all without monitoring the contents of individual conversations, to maintain user privacy." Claude Code's defaults say the same in settings: "Spans redact user prompt text, tool input details, and tool content by default", and each is opened by a separate variable, `OTEL_LOG_USER_PROMPTS`, `OTEL_LOG_TOOL_DETAILS` and `OTEL_LOG_TOOL_CONTENT`, all off unless set. The variable that emits raw request and response bodies "implies consent to everything" the other three would reveal, since the bodies "include the entire conversation history". And a repository cannot turn any of it on: Claude Code ignores the exporter variables in a repository's settings, "so a repository can't use them to turn telemetry on, choose where it goes, or capture content".

Take the same shape for your own records. Keep ids, counts, timings, names of models and tools, and statuses. Drop the content fields, and allow one by name only for a stated purpose and a limited time. The example's `redact` does this with a fixed list of content fields (prompt, response, tool input, tool output) and an allow list.

### Tying one request together

A request crosses an API gateway, an orchestrator, subagents, a model and tools, and each writes its own log. Without a shared identifier the logs cannot be joined. Time alone cannot join records: two requests at the same millisecond look the same. A central store collects the records and still does not say which belong together, and keeping records longer keeps the same unjoinable records longer. Two identifiers do it. Every API response carries a unique `request-id` header, and "The same identifier appears as the `request_id` field in error response bodies", which is what support asks for. And the W3C trace context ties spans together: when tracing is active Claude Code puts a `traceparent` header on its model requests and on outbound HTTP MCP requests, and subprocesses inherit a `TRACEPARENT` variable, so a script that reads it can parent its own spans under the same trace. Your own services do the same: take the id at the edge, pass it on, write it in every record. The example's `request_trail` is the reading side: given an id, the events of every component in time order.

### The example

The example runs four invented traces and one log record through the decisions above, with the numbers kept small enough to check by eye. It ran offline in every language, and the output is the same in all four.

<!-- example: m87-trace-triage tabs: python,typescript,java,kotlin -->
```python
"""Observability decisions for a system of agents and tools: which traces to keep, how to find the layer that failed, when a change in a metric is drift, when to alert and what a log record may hold.

The traces, metrics and events are invented for the example. The rules come from the Claude Certified Architect - Professional exam guide (domains 3 and 4), Anthropic's article on its multi-agent research system
and the Claude Code monitoring documentation, read on 2026-10-04. Nothing here calls a model.
"""
from collections import namedtuple

Span = namedtuple("Span", "id parent kind name status ms note")
CONTENT = {"prompt", "response", "tool_input", "tool_output"}
TRACES = {
    "t-refund": [Span("s1", "", "agent", "orchestrator", "error", 9200, ""), Span("s2", "s1", "agent", "order-researcher", "error", 8700, ""), Span("s3", "s2", "llm", "plan", "ok", 900, ""),
                 Span("s4", "s2", "tool", "web_fetch", "error", 5000, ""), Span("s5", "s1", "llm", "summarise", "ok", 400, "")],
    "t-policy": [Span("s1", "", "agent", "assistant", "ok", 2100, ""), Span("s2", "s1", "retrieval", "policy_search", "ok", 120, "stale"), Span("s3", "s1", "llm", "answer", "ok", 1800, "")],
    "t-empty": [Span("s1", "", "agent", "assistant", "ok", 1500, ""), Span("s2", "s1", "retrieval", "policy_search", "ok", 90, "no-hits"), Span("s3", "s1", "llm", "answer", "ok", 1300, "")],
    "t-plain": [Span("s1", "", "agent", "assistant", "ok", 1900, ""), Span("s2", "s1", "retrieval", "policy_search", "ok", 110, ""), Span("s3", "s1", "llm", "answer", "ok", 1700, "")],
}


def bucket(trace_id):
    """A number from 0 to 99 that depends on the trace id alone, the same in every agent and every language."""
    h = 7
    for c in trace_id:
        h = (h * 31 + ord(c)) % 1000003
    return h % 100


def keep_reason(trace_id, spans, rate, feedback=False, slow_ms=5000):
    """Tail-based sampling: a trace with an error, a slow root or a bad-answer flag is always kept, and the rest are kept by their id at `rate` percent."""
    if any(s.status == "error" for s in spans):
        return "error"
    if spans[0].ms > slow_ms:
        return "slow"
    if feedback:
        return "feedback"
    return "sampled" if bucket(trace_id) < rate else "dropped"


def root_cause(spans):
    """The deepest failing span is the origin, not the span that reported the error; with no failure, a retrieval that returned stale or no chunks is blamed."""
    by_id = {s.id: s for s in spans}
    failed = [s for s in spans if s.status == "error"]
    if failed:
        parents = {s.parent for s in failed}
        origin = next(s for s in failed if s.id not in parents)
        path, cursor = [], origin
        while cursor is not None:
            path.append(cursor.name)
            cursor = by_id.get(cursor.parent)
        return {"layer": origin.kind, "name": origin.name, "why": "failed", "path": path[::-1]}
    for s in spans:
        if s.kind == "retrieval" and s.note in ("stale", "no-hits"):
            return {"layer": "retrieval", "name": s.name, "why": s.note, "path": [spans[0].name, s.name]}
    return {"layer": "none", "name": "", "why": "no span failed", "path": []}


def drift(baseline, current, tolerance):
    """Metrics whose relative change since the baseline is over `tolerance` percent, in either direction."""
    out = []
    for name in sorted(baseline):
        base, now = baseline[name], current[name]
        pct = abs(now - base) * 100 // base if base else (100 if now else 0)
        if pct > tolerance:
            out.append(f"{name} {'up' if now > base else 'down'} {pct}%")
    return out


def alert_at(series, threshold, windows):
    """The index of the window that completes `windows` consecutive values over the threshold, or -1."""
    run = 0
    for i, value in enumerate(series):
        run = run + 1 if value > threshold else 0
        if run >= windows:
            return i
    return -1


def redact(event, allowed=()):
    """A log record keeps ids, counts and timings and drops the content fields unless they are allowed by name."""
    return {k: v for k, v in event.items() if k not in CONTENT or k in allowed}


def main():
    healthy = [f"trace-{i}" for i in range(100)]
    kept = sum(1 for t in healthy if keep_reason(t, TRACES["t-plain"], 10) == "sampled")
    print(f"100 healthy traces at a 10 percent rate: {kept} kept by id, the same {kept} in every agent: {kept == sum(1 for t in healthy if bucket(t) < 10)}")
    for name, spans in TRACES.items():
        cause = root_cause(spans)
        print(f"{name}: kept as {keep_reason(name, spans, 0)}; cause: " + " ".join(x for x in (cause["layer"], cause["name"], cause["why"]) if x))
    print("path of t-refund: " + " > ".join(root_cause(TRACES["t-refund"])["path"]))
    print("a bad-answer flag keeps t-plain at rate 0:", keep_reason("t-plain", TRACES["t-plain"], 0, feedback=True))
    drifted = drift({"retrieval_hits": 5, "refusals_per_1000": 4, "tokens_per_answer": 900, "tool_errors_per_1000": 12}, {"retrieval_hits": 3, "refusals_per_1000": 4, "tokens_per_answer": 1260, "tool_errors_per_1000": 13}, 25)
    print("drift against last week, tolerance 25%: " + ", ".join(drifted))
    series = [1, 2, 9, 2, 8, 9, 10, 3]
    print(f"error rate per window {series}, threshold 5: one window over fires at {alert_at(series, 5, 1)}, three in a row fire at {alert_at(series, 5, 3)}")
    event = {"trace": "t-1", "model": "claude-sonnet-5-5", "input_tokens": 1200, "output_tokens": 300, "tool": "lookup_order", "status": "ok", "prompt": "(text)", "tool_input": "(text)"}
    print("log record keeps: " + ", ".join(sorted(redact(event))) + "; with tool_input allowed by name: " + ", ".join(sorted(set(redact(event, ('tool_input',))) - set(redact(event)))))


if __name__ == "__main__":
    main()
```
```text
100 healthy traces at a 10 percent rate: 13 kept by id, the same 13 in every agent: True
t-refund: kept as error; cause: tool web_fetch failed
t-policy: kept as dropped; cause: retrieval policy_search stale
t-empty: kept as dropped; cause: retrieval policy_search no-hits
t-plain: kept as dropped; cause: none no span failed
path of t-refund: orchestrator > order-researcher > web_fetch
a bad-answer flag keeps t-plain at rate 0: feedback
drift against last week, tolerance 25%: retrieval_hits down 40%, tokens_per_answer up 40%
error rate per window [1, 2, 9, 2, 8, 9, 10, 3], threshold 5: one window over fires at 2, three in a row fire at 6
log record keeps: input_tokens, model, output_tokens, status, tool, trace; with tool_input allowed by name: tool_input
```
```typescript
/**
 * Observability decisions for a system of agents and tools: which traces to keep, how to find the layer that failed, when a change in a metric is drift, when to alert and what a log record may hold.
 *
 * The traces, metrics and events are invented for the example. The rules come from the Claude Certified Architect - Professional exam guide (domains 3 and 4), Anthropic's article on its multi-agent research system
 * and the Claude Code monitoring documentation, read on 2026-10-04. Nothing here calls a model.
 */
export type Span = { id: string; parent: string; kind: string; name: string; status: string; ms: number; note: string };
export type Cause = { layer: string; name: string; why: string; path: string[] };

const span = (id: string, parent: string, kind: string, name: string, status: string, ms: number, note: string): Span => ({ id, parent, kind, name, status, ms, note });
export const CONTENT = new Set(["prompt", "response", "tool_input", "tool_output"]);
export const TRACES: Record<string, Span[]> = {
  "t-refund": [span("s1", "", "agent", "orchestrator", "error", 9200, ""), span("s2", "s1", "agent", "order-researcher", "error", 8700, ""), span("s3", "s2", "llm", "plan", "ok", 900, ""),
    span("s4", "s2", "tool", "web_fetch", "error", 5000, ""), span("s5", "s1", "llm", "summarise", "ok", 400, "")],
  "t-policy": [span("s1", "", "agent", "assistant", "ok", 2100, ""), span("s2", "s1", "retrieval", "policy_search", "ok", 120, "stale"), span("s3", "s1", "llm", "answer", "ok", 1800, "")],
  "t-empty": [span("s1", "", "agent", "assistant", "ok", 1500, ""), span("s2", "s1", "retrieval", "policy_search", "ok", 90, "no-hits"), span("s3", "s1", "llm", "answer", "ok", 1300, "")],
  "t-plain": [span("s1", "", "agent", "assistant", "ok", 1900, ""), span("s2", "s1", "retrieval", "policy_search", "ok", 110, ""), span("s3", "s1", "llm", "answer", "ok", 1700, "")],
};

/** A number from 0 to 99 that depends on the trace id alone, the same in every agent and every language. */
export function bucket(traceId: string): number {
  let h = 7;
  for (const c of traceId) h = (h * 31 + c.charCodeAt(0)) % 1000003;
  return h % 100;
}

/** Tail-based sampling: a trace with an error, a slow root or a bad-answer flag is always kept, and the rest are kept by their id at `rate` percent. */
export function keepReason(traceId: string, spans: Span[], rate: number, feedback = false, slowMs = 5000): string {
  if (spans.some((s) => s.status === "error")) return "error";
  if (spans[0].ms > slowMs) return "slow";
  if (feedback) return "feedback";
  return bucket(traceId) < rate ? "sampled" : "dropped";
}

/** The deepest failing span is the origin, not the span that reported the error; with no failure, a retrieval that returned stale or no chunks is blamed. */
export function rootCause(spans: Span[]): Cause {
  const byId = new Map(spans.map((s) => [s.id, s]));
  const failed = spans.filter((s) => s.status === "error");
  if (failed.length > 0) {
    const parents = new Set(failed.map((s) => s.parent));
    const origin = failed.find((s) => !parents.has(s.id))!;
    const path: string[] = [];
    let cursor: Span | undefined = origin;
    while (cursor !== undefined) {
      path.push(cursor.name);
      cursor = byId.get(cursor.parent);
    }
    return { layer: origin.kind, name: origin.name, why: "failed", path: path.reverse() };
  }
  for (const s of spans) if (s.kind === "retrieval" && (s.note === "stale" || s.note === "no-hits")) return { layer: "retrieval", name: s.name, why: s.note, path: [spans[0].name, s.name] };
  return { layer: "none", name: "", why: "no span failed", path: [] };
}

/** Metrics whose relative change since the baseline is over `tolerance` percent, in either direction. */
export function drift(baseline: Record<string, number>, current: Record<string, number>, tolerance: number): string[] {
  const out: string[] = [];
  for (const name of Object.keys(baseline).sort()) {
    const base = baseline[name];
    const now = current[name];
    const pct = base !== 0 ? Math.floor((Math.abs(now - base) * 100) / base) : now !== 0 ? 100 : 0;
    if (pct > tolerance) out.push(`${name} ${now > base ? "up" : "down"} ${pct}%`);
  }
  return out;
}

/** The index of the window that completes `windows` consecutive values over the threshold, or -1. */
export function alertAt(series: number[], threshold: number, windows: number): number {
  let run = 0;
  for (let i = 0; i < series.length; i++) {
    run = series[i] > threshold ? run + 1 : 0;
    if (run >= windows) return i;
  }
  return -1;
}

/** A log record keeps ids, counts and timings and drops the content fields unless they are allowed by name. */
export function redact(event: Record<string, string | number>, allowed: string[] = []): Record<string, string | number> {
  return Object.fromEntries(Object.entries(event).filter(([k]) => !CONTENT.has(k) || allowed.includes(k)));
}

function main(): void {
  const healthy = Array.from({ length: 100 }, (_, i) => `trace-${i}`);
  const kept = healthy.filter((t) => keepReason(t, TRACES["t-plain"], 10) === "sampled").length;
  const same = kept === healthy.filter((t) => bucket(t) < 10).length;
  console.log(`100 healthy traces at a 10 percent rate: ${kept} kept by id, the same ${kept} in every agent: ${same ? "True" : "False"}`);
  for (const [name, spans] of Object.entries(TRACES)) {
    const cause = rootCause(spans);
    console.log(`${name}: kept as ${keepReason(name, spans, 0)}; cause: ` + [cause.layer, cause.name, cause.why].filter((x) => x !== "").join(" "));
  }
  console.log("path of t-refund: " + rootCause(TRACES["t-refund"]).path.join(" > "));
  console.log("a bad-answer flag keeps t-plain at rate 0:", keepReason("t-plain", TRACES["t-plain"], 0, true));
  const drifted = drift({ retrieval_hits: 5, refusals_per_1000: 4, tokens_per_answer: 900, tool_errors_per_1000: 12 }, { retrieval_hits: 3, refusals_per_1000: 4, tokens_per_answer: 1260, tool_errors_per_1000: 13 }, 25);
  console.log("drift against last week, tolerance 25%: " + drifted.join(", "));
  const series = [1, 2, 9, 2, 8, 9, 10, 3];
  console.log(`error rate per window [${series.join(", ")}], threshold 5: one window over fires at ${alertAt(series, 5, 1)}, three in a row fire at ${alertAt(series, 5, 3)}`);
  const event = { trace: "t-1", model: "claude-sonnet-5-5", input_tokens: 1200, output_tokens: 300, tool: "lookup_order", status: "ok", prompt: "(text)", tool_input: "(text)" };
  const plain = Object.keys(redact(event)).sort();
  const extra = Object.keys(redact(event, ["tool_input"])).filter((k) => !plain.includes(k)).sort();
  console.log("log record keeps: " + plain.join(", ") + "; with tool_input allowed by name: " + extra.join(", "));
}

if (import.meta.main) main();
```
```text
100 healthy traces at a 10 percent rate: 13 kept by id, the same 13 in every agent: True
t-refund: kept as error; cause: tool web_fetch failed
t-policy: kept as dropped; cause: retrieval policy_search stale
t-empty: kept as dropped; cause: retrieval policy_search no-hits
t-plain: kept as dropped; cause: none no span failed
path of t-refund: orchestrator > order-researcher > web_fetch
a bad-answer flag keeps t-plain at rate 0: feedback
drift against last week, tolerance 25%: retrieval_hits down 40%, tokens_per_answer up 40%
error rate per window [1, 2, 9, 2, 8, 9, 10, 3], threshold 5: one window over fires at 2, three in a row fire at 6
log record keeps: input_tokens, model, output_tokens, status, tool, trace; with tool_input allowed by name: tool_input
```
```java
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Observability decisions for a system of agents and tools: which traces to keep, how to find the layer that failed, when a change in a metric is drift, when to alert and what a log record may hold.
 *
 * The traces, metrics and events are invented for the example. The rules come from the Claude Certified Architect - Professional exam guide (domains 3 and 4), Anthropic's article on its multi-agent research system
 * and the Claude Code monitoring documentation, read on 2026-10-04. Nothing here calls a model.
 */
public class TraceTriage {
    record Span(String id, String parent, String kind, String name, String status, int ms, String note) {}

    record Cause(String layer, String name, String why, List<String> path) {}

    static final Set<String> CONTENT = Set.of("prompt", "response", "tool_input", "tool_output");
    static final Map<String, List<Span>> TRACES = new LinkedHashMap<>();

    static {
        TRACES.put("t-refund", List.of(new Span("s1", "", "agent", "orchestrator", "error", 9200, ""), new Span("s2", "s1", "agent", "order-researcher", "error", 8700, ""),
                new Span("s3", "s2", "llm", "plan", "ok", 900, ""), new Span("s4", "s2", "tool", "web_fetch", "error", 5000, ""), new Span("s5", "s1", "llm", "summarise", "ok", 400, "")));
        TRACES.put("t-policy", List.of(new Span("s1", "", "agent", "assistant", "ok", 2100, ""), new Span("s2", "s1", "retrieval", "policy_search", "ok", 120, "stale"), new Span("s3", "s1", "llm", "answer", "ok", 1800, "")));
        TRACES.put("t-empty", List.of(new Span("s1", "", "agent", "assistant", "ok", 1500, ""), new Span("s2", "s1", "retrieval", "policy_search", "ok", 90, "no-hits"), new Span("s3", "s1", "llm", "answer", "ok", 1300, "")));
        TRACES.put("t-plain", List.of(new Span("s1", "", "agent", "assistant", "ok", 1900, ""), new Span("s2", "s1", "retrieval", "policy_search", "ok", 110, ""), new Span("s3", "s1", "llm", "answer", "ok", 1700, "")));
    }

    /** A number from 0 to 99 that depends on the trace id alone, the same in every agent and every language. */
    static int bucket(String traceId) {
        int h = 7;
        for (char c : traceId.toCharArray()) h = (h * 31 + c) % 1000003;
        return h % 100;
    }

    /** Tail-based sampling: a trace with an error, a slow root or a bad-answer flag is always kept, and the rest are kept by their id at `rate` percent. */
    static String keepReason(String traceId, List<Span> spans, int rate, boolean feedback, int slowMs) {
        for (Span s : spans) if (s.status().equals("error")) return "error";
        if (spans.get(0).ms() > slowMs) return "slow";
        if (feedback) return "feedback";
        return bucket(traceId) < rate ? "sampled" : "dropped";
    }

    static String keepReason(String traceId, List<Span> spans, int rate) {
        return keepReason(traceId, spans, rate, false, 5000);
    }

    /** The deepest failing span is the origin, not the span that reported the error; with no failure, a retrieval that returned stale or no chunks is blamed. */
    static Cause rootCause(List<Span> spans) {
        Map<String, Span> byId = new HashMap<>();
        for (Span s : spans) byId.put(s.id(), s);
        List<Span> failed = new ArrayList<>();
        for (Span s : spans) if (s.status().equals("error")) failed.add(s);
        if (!failed.isEmpty()) {
            Set<String> parents = new HashSet<>();
            for (Span s : failed) parents.add(s.parent());
            Span origin = null;
            for (Span s : failed) if (!parents.contains(s.id())) { origin = s; break; }
            List<String> path = new ArrayList<>();
            for (Span cursor = origin; cursor != null; cursor = byId.get(cursor.parent())) path.add(cursor.name());
            Collections.reverse(path);
            return new Cause(origin.kind(), origin.name(), "failed", path);
        }
        for (Span s : spans) {
            if (s.kind().equals("retrieval") && (s.note().equals("stale") || s.note().equals("no-hits"))) return new Cause("retrieval", s.name(), s.note(), List.of(spans.get(0).name(), s.name()));
        }
        return new Cause("none", "", "no span failed", List.of());
    }

    /** Metrics whose relative change since the baseline is over `tolerance` percent, in either direction. */
    static List<String> drift(Map<String, Integer> baseline, Map<String, Integer> current, int tolerance) {
        List<String> out = new ArrayList<>();
        for (String name : new TreeSet<>(baseline.keySet())) {
            int base = baseline.get(name);
            int now = current.get(name);
            int pct = base != 0 ? Math.abs(now - base) * 100 / base : (now != 0 ? 100 : 0);
            if (pct > tolerance) out.add(name + " " + (now > base ? "up" : "down") + " " + pct + "%");
        }
        return out;
    }

    /** The index of the window that completes `windows` consecutive values over the threshold, or -1. */
    static int alertAt(List<Integer> series, int threshold, int windows) {
        int run = 0;
        for (int i = 0; i < series.size(); i++) {
            run = series.get(i) > threshold ? run + 1 : 0;
            if (run >= windows) return i;
        }
        return -1;
    }

    /** A log record keeps ids, counts and timings and drops the content fields unless they are allowed by name. */
    static Map<String, Object> redact(Map<String, Object> event, Set<String> allowed) {
        Map<String, Object> out = new LinkedHashMap<>();
        event.forEach((k, v) -> {
            if (!CONTENT.contains(k) || allowed.contains(k)) out.put(k, v);
        });
        return out;
    }

    static Map<String, Integer> metrics(int hits, int refusals, int tokens, int toolErrors) {
        Map<String, Integer> m = new TreeMap<>();
        m.put("retrieval_hits", hits);
        m.put("refusals_per_1000", refusals);
        m.put("tokens_per_answer", tokens);
        m.put("tool_errors_per_1000", toolErrors);
        return m;
    }

    public static void main(String[] args) {
        int kept = 0;
        int byBucket = 0;
        for (int i = 0; i < 100; i++) {
            String id = "trace-" + i;
            if (keepReason(id, TRACES.get("t-plain"), 10).equals("sampled")) kept++;
            if (bucket(id) < 10) byBucket++;
        }
        System.out.println("100 healthy traces at a 10 percent rate: " + kept + " kept by id, the same " + kept + " in every agent: " + (kept == byBucket ? "True" : "False"));
        TRACES.forEach((name, spans) -> {
            Cause cause = rootCause(spans);
            List<String> parts = new ArrayList<>();
            for (String x : List.of(cause.layer(), cause.name(), cause.why())) if (!x.isEmpty()) parts.add(x);
            System.out.println(name + ": kept as " + keepReason(name, spans, 0) + "; cause: " + String.join(" ", parts));
        });
        System.out.println("path of t-refund: " + String.join(" > ", rootCause(TRACES.get("t-refund")).path()));
        System.out.println("a bad-answer flag keeps t-plain at rate 0: " + keepReason("t-plain", TRACES.get("t-plain"), 0, true, 5000));
        System.out.println("drift against last week, tolerance 25%: " + String.join(", ", drift(metrics(5, 4, 900, 12), metrics(3, 4, 1260, 13), 25)));
        List<Integer> series = List.of(1, 2, 9, 2, 8, 9, 10, 3);
        System.out.println("error rate per window " + series + ", threshold 5: one window over fires at " + alertAt(series, 5, 1) + ", three in a row fire at " + alertAt(series, 5, 3));
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("trace", "t-1");
        event.put("model", "claude-sonnet-5-5");
        event.put("input_tokens", 1200);
        event.put("output_tokens", 300);
        event.put("tool", "lookup_order");
        event.put("status", "ok");
        event.put("prompt", "(text)");
        event.put("tool_input", "(text)");
        TreeSet<String> plain = new TreeSet<>(redact(event, Set.of()).keySet());
        TreeSet<String> extra = new TreeSet<>(redact(event, Set.of("tool_input")).keySet());
        extra.removeAll(plain);
        System.out.println("log record keeps: " + String.join(", ", plain) + "; with tool_input allowed by name: " + String.join(", ", extra));
    }
}
```
```text
100 healthy traces at a 10 percent rate: 13 kept by id, the same 13 in every agent: True
t-refund: kept as error; cause: tool web_fetch failed
t-policy: kept as dropped; cause: retrieval policy_search stale
t-empty: kept as dropped; cause: retrieval policy_search no-hits
t-plain: kept as dropped; cause: none no span failed
path of t-refund: orchestrator > order-researcher > web_fetch
a bad-answer flag keeps t-plain at rate 0: feedback
drift against last week, tolerance 25%: retrieval_hits down 40%, tokens_per_answer up 40%
error rate per window [1, 2, 9, 2, 8, 9, 10, 3], threshold 5: one window over fires at 2, three in a row fire at 6
log record keeps: input_tokens, model, output_tokens, status, tool, trace; with tool_input allowed by name: tool_input
```
```kotlin
/**
 * Observability decisions for a system of agents and tools: which traces to keep, how to find the layer that failed, when a change in a metric is drift, when to alert and what a log record may hold.
 *
 * The traces, metrics and events are invented for the example. The rules come from the Claude Certified Architect - Professional exam guide (domains 3 and 4), Anthropic's article on its multi-agent research system
 * and the Claude Code monitoring documentation, read on 2026-10-04. Nothing here calls a model.
 */
data class Span(val id: String, val parent: String, val kind: String, val name: String, val status: String, val ms: Int, val note: String)

data class Cause(val layer: String, val name: String, val why: String, val path: List<String>)

val CONTENT = setOf("prompt", "response", "tool_input", "tool_output")
val TRACES: Map<String, List<Span>> = linkedMapOf(
    "t-refund" to listOf(Span("s1", "", "agent", "orchestrator", "error", 9200, ""), Span("s2", "s1", "agent", "order-researcher", "error", 8700, ""), Span("s3", "s2", "llm", "plan", "ok", 900, ""),
        Span("s4", "s2", "tool", "web_fetch", "error", 5000, ""), Span("s5", "s1", "llm", "summarise", "ok", 400, "")),
    "t-policy" to listOf(Span("s1", "", "agent", "assistant", "ok", 2100, ""), Span("s2", "s1", "retrieval", "policy_search", "ok", 120, "stale"), Span("s3", "s1", "llm", "answer", "ok", 1800, "")),
    "t-empty" to listOf(Span("s1", "", "agent", "assistant", "ok", 1500, ""), Span("s2", "s1", "retrieval", "policy_search", "ok", 90, "no-hits"), Span("s3", "s1", "llm", "answer", "ok", 1300, "")),
    "t-plain" to listOf(Span("s1", "", "agent", "assistant", "ok", 1900, ""), Span("s2", "s1", "retrieval", "policy_search", "ok", 110, ""), Span("s3", "s1", "llm", "answer", "ok", 1700, "")),
)

/** A number from 0 to 99 that depends on the trace id alone, the same in every agent and every language. */
fun bucket(traceId: String): Int {
    var h = 7
    for (c in traceId) h = (h * 31 + c.code) % 1000003
    return h % 100
}

/** Tail-based sampling: a trace with an error, a slow root or a bad-answer flag is always kept, and the rest are kept by their id at `rate` percent. */
fun keepReason(traceId: String, spans: List<Span>, rate: Int, feedback: Boolean = false, slowMs: Int = 5000): String {
    if (spans.any { it.status == "error" }) return "error"
    if (spans[0].ms > slowMs) return "slow"
    if (feedback) return "feedback"
    return if (bucket(traceId) < rate) "sampled" else "dropped"
}

/** The deepest failing span is the origin, not the span that reported the error; with no failure, a retrieval that returned stale or no chunks is blamed. */
fun rootCause(spans: List<Span>): Cause {
    val byId = spans.associateBy { it.id }
    val failed = spans.filter { it.status == "error" }
    if (failed.isNotEmpty()) {
        val parents = failed.map { it.parent }.toSet()
        val origin = failed.first { it.id !in parents }
        val path = mutableListOf<String>()
        var cursor: Span? = origin
        while (cursor != null) {
            path.add(cursor.name)
            cursor = byId[cursor.parent]
        }
        return Cause(origin.kind, origin.name, "failed", path.reversed())
    }
    for (s in spans) if (s.kind == "retrieval" && (s.note == "stale" || s.note == "no-hits")) return Cause("retrieval", s.name, s.note, listOf(spans[0].name, s.name))
    return Cause("none", "", "no span failed", listOf())
}

/** Metrics whose relative change since the baseline is over `tolerance` percent, in either direction. */
fun drift(baseline: Map<String, Int>, current: Map<String, Int>, tolerance: Int): List<String> {
    val out = mutableListOf<String>()
    for (name in baseline.keys.sorted()) {
        val base = baseline.getValue(name)
        val now = current.getValue(name)
        val pct = if (base != 0) Math.abs(now - base) * 100 / base else if (now != 0) 100 else 0
        if (pct > tolerance) out.add("$name ${if (now > base) "up" else "down"} $pct%")
    }
    return out
}

/** The index of the window that completes `windows` consecutive values over the threshold, or -1. */
fun alertAt(series: List<Int>, threshold: Int, windows: Int): Int {
    var run = 0
    for ((i, value) in series.withIndex()) {
        run = if (value > threshold) run + 1 else 0
        if (run >= windows) return i
    }
    return -1
}

/** A log record keeps ids, counts and timings and drops the content fields unless they are allowed by name. */
fun redact(event: Map<String, Any>, allowed: Set<String> = setOf()): Map<String, Any> = event.filter { it.key !in CONTENT || it.key in allowed }

fun main() {
    val healthy = (0 until 100).map { "trace-$it" }
    val kept = healthy.count { keepReason(it, TRACES.getValue("t-plain"), 10) == "sampled" }
    val same = kept == healthy.count { bucket(it) < 10 }
    println("100 healthy traces at a 10 percent rate: $kept kept by id, the same $kept in every agent: ${if (same) "True" else "False"}")
    for ((name, spans) in TRACES) {
        val cause = rootCause(spans)
        println("$name: kept as ${keepReason(name, spans, 0)}; cause: " + listOf(cause.layer, cause.name, cause.why).filter { it.isNotEmpty() }.joinToString(" "))
    }
    println("path of t-refund: " + rootCause(TRACES.getValue("t-refund")).path.joinToString(" > "))
    println("a bad-answer flag keeps t-plain at rate 0: " + keepReason("t-plain", TRACES.getValue("t-plain"), 0, true))
    val drifted = drift(
        mapOf("retrieval_hits" to 5, "refusals_per_1000" to 4, "tokens_per_answer" to 900, "tool_errors_per_1000" to 12),
        mapOf("retrieval_hits" to 3, "refusals_per_1000" to 4, "tokens_per_answer" to 1260, "tool_errors_per_1000" to 13), 25)
    println("drift against last week, tolerance 25%: " + drifted.joinToString(", "))
    val series = listOf(1, 2, 9, 2, 8, 9, 10, 3)
    println("error rate per window $series, threshold 5: one window over fires at ${alertAt(series, 5, 1)}, three in a row fire at ${alertAt(series, 5, 3)}")
    val event = linkedMapOf<String, Any>("trace" to "t-1", "model" to "claude-sonnet-5-5", "input_tokens" to 1200, "output_tokens" to 300, "tool" to "lookup_order", "status" to "ok", "prompt" to "(text)", "tool_input" to "(text)")
    val plain = redact(event).keys.sorted()
    val extra = redact(event, setOf("tool_input")).keys.filter { it !in plain }.sorted()
    println("log record keeps: " + plain.joinToString(", ") + "; with tool_input allowed by name: " + extra.joinToString(", "))
}
```
```text
100 healthy traces at a 10 percent rate: 13 kept by id, the same 13 in every agent: True
t-refund: kept as error; cause: tool web_fetch failed
t-policy: kept as dropped; cause: retrieval policy_search stale
t-empty: kept as dropped; cause: retrieval policy_search no-hits
t-plain: kept as dropped; cause: none no span failed
path of t-refund: orchestrator > order-researcher > web_fetch
a bad-answer flag keeps t-plain at rate 0: feedback
drift against last week, tolerance 25%: retrieval_hits down 40%, tokens_per_answer up 40%
error rate per window [1, 2, 9, 2, 8, 9, 10, 3], threshold 5: one window over fires at 2, three in a row fire at 6
log record keeps: input_tokens, model, output_tokens, status, tool, trace; with tool_input allowed by name: tool_input
```
<!-- /example -->

## Traps

1. **"Keep a random one percent of traces; it is unbiased."** It is tempting because it is simple, cheap and statistically clean. It fails because the failures are what you need to explain, and a random share holds almost none of them. Tail-based sampling keeps what failed, what was slow and what a user flagged, and samples the rest.
2. **"Blame the span that reports the error."** It is tempting because it is the one the dashboard shows first. It fails because the error travelled up from a deeper span, and the cure belongs to the one that failed. Read the trace to the deepest failure.
3. **"Log the full prompt and tool output so that nothing is missing."** It is tempting because it makes every incident easy to read. It fails on privacy and on size: the log becomes a second copy of customer data, with a wider audience. Log ids, counts and timings, and open a content field by name.

## Quiz

1. A platform handles two million conversations a day and cannot afford to store every trace. Users report wrong answers that the dashboards do not show, and a random share of the traces rarely holds one. Which rule fits best?
   - **a**: Retain every request that errored, ran slowly or drew a complaint, and pick the others by a hash of the id
   - **b**: Raise the random share from one percent to five, and have an engineer review what that larger share holds at the end of each week
   - **c**: Retain only the requests of the largest customers, because their heavy usage accounts for the greatest part of all conversations
   - **d**: Drop the stored requests entirely and rely on the daily error count to show what went wrong

2. In one trace, the orchestrator, a researcher agent and a download tool all show an error. The orchestrator's message says the research step failed. Where does the failure originate?
   - **a**: In the orchestrator, because it is the first component the user's request reached
   - **b**: In the researcher, because it is the first agent below the orchestrator to show an error
   - **c**: In the deepest span that broke, the call at the bottom, since the two above only relayed what it reported
   - **d**: In the model, because every decision of every agent is finally produced by the model

<details>
<summary>Answer key</summary>

1. **a**. Tail-based sampling can look at the outcome, so it retains what failed, what was slow and what a user flagged, and takes a share of the rest by id. *b* is ruled out because "a random share of one percent holds almost none of them, and five percent holds few more". *c* is ruled out because "Sampling by customer size covers heavy use and says nothing about which requests failed". *d* is ruled out because a metric "says that something changed. It never says where or why".
2. **c**. The deepest failing span is the origin, and the spans above it only relay its error. *a* is ruled out because "the first span to turn red is not the origin". *b* is ruled out for the same reason: "The span that reports an error is often only passing on the error of the span below it". *d* is ruled out because the example reports the layer of the origin, and a failing tool is the tool layer, not the model: it "returns that span's layer (agent, model, tool or retrieval)".

</details>
