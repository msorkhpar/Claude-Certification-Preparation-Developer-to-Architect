# Drift, alerts and the practice

**Level:** Architect Professional · **Module 87:** Observability at scale · **Page 2 of 2**
**Exams:** P3, P4

**After this page you can** choose metrics for each layer of a system of agents, detect drift against a baseline in either direction, design an alert that pages for a real change and not for noise, control the cost of telemetry, and write the module's practice.

Checked on 2026-10-04 against the Claude Code documentation page "Monitoring" (metrics, cardinality and content limits), the Claude documentation migration guide for Claude Sonnet 5.5, the Claude Certified Architect - Professional exam guide (version 1.0, domains 3 and 4), and by running the example and the practice offline in the course container. Nothing here called a model, and the figures in the example are invented. This page deepens module 42 (evaluation) and module 41 (monitoring for misuse) to the scale of a running system. What to keep, how to find the layer that failed and what must not be logged are the first page.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* domain 3 asks the candidate to "Analyze observability challenges and select monitoring strategies at scale", and domain 4 to "Define evaluation metrics (accuracy, latency, cost, safety, security)" and to "Monitor system performance using logging and observability tools". *What the current product does (documentation read 2026-10-04):* Claude Code's metrics are labelled time series, and its documentation says "Lower cardinality generally means better performance and lower storage costs but less granular data for analysis". Model migration guides tell teams to re-measure: for Claude Sonnet 5.5, "Recount tokens and re-baseline cost". On the exam, answer with the monitoring strategy. The drift tolerance and the alert window below are design values to tune to your own traffic, and the figures in the example are not recommendations.

## Why it matters

Two weeks after a change of model, the dashboard still shows a 0.4 percent error rate and steady latency. A finance review then finds that cost per answer is up by more than a third, and a support lead notices that the assistant answers from the policy less often. Nothing threw. The system got worse without failing, and the monitoring that looked only for failures never saw it. A second team has the opposite problem: its on-call engineer is paged every night by a one-minute spike in tool errors that is gone before anyone looks. Both teams have metrics. Neither has a monitoring strategy.

## The idea

### Metrics for each layer

A system of agents fails in layers, so the metrics are chosen by layer, and each is a number that could move for a known reason.

- **Retrieval:** the number of chunks returned per query, the share of queries with no hits and the share of chunks older than their source. A fall in hits, or a rise in stale chunks, means the index no longer matches the documents.
- **Model:** tokens per answer, the share of refusals, the stop reasons and the latency of the first content. Tokens per answer climbing means the context or the prompt grew, or the model changed.
- **Tools:** errors per thousand calls, by tool, and retries per request.
- **End to end:** the share of answers a user flagged, and cost per resolved request.

An error rate alone misses a system that is wrong without failing. A refusal arrives with a normal status and is invisible to it (module 43).

### Drift: a change that throws nothing

Drift is a move in a metric that no error announces. A model, a prompt, a corpus or the mix of users changes, and the numbers slide. The check is the plainest one: store a baseline for each metric, compare each current value, and flag every move beyond a tolerance, in either direction. The example computes the relative change in whole percent and flags a metric when it is over the tolerance. Reading a sample of conversations each week finds what a reader happens to see, and it does not scale to two million conversations. Both directions matter because a rise is as likely to be a fault as growth, and a fall is not always an improvement. A refusal rate that drops to zero can mean a guardrail stopped working; a fall in retrieval hits can mean a re-index broke. A metric whose baseline is zero needs a rule of its own: the example treats any move from zero as a move of 100 percent, and never divides by it.

The baseline is not permanent. A planned change moves the numbers on purpose, and the migration guide for Claude Sonnet 5.5 says so for cost: "The price per token is higher, and the same text produces more tokens. Recount tokens and re-baseline cost." The rule is to reset a baseline deliberately, at a change you decided on, and to treat a move you did not plan as a finding. Module 89 gates such a change before it ships.

### Alerts that mean something

An alert is a decision to wake a person, so it needs the same care as any other design decision. Three rules cover most of it.

- **Alert on a change that lasts.** One window over the threshold is noise more often than not. The example fires when several consecutive windows are over, and a window at or under the threshold starts the count again. The price is delay: three windows of one minute add up to a three-minute lag before the page.
- **Do not hide the problem to quiet the pager.** Raising the threshold until the pages stop hides the real incident with the noise. A daily average smooths a one-hour outage into a normal day. A message nobody reads at night turns a page into a log line.
- **Say where to look first.** An alert that names its metric, its layer and the trace query that finds an example saves the first quarter hour of the incident.

### The cost of telemetry

Telemetry has its own bill. Metrics are cheap to keep and costly when a label has many values: "each custom key becomes a label on every metric series, so high-cardinality values increase storage cost in your metrics backend". A label such as the conversation id belongs in a trace or an event, which are meant for single occurrences, and not on a metric. Moving to a larger store pays for the series and leaves their count growing. Dropping the id from everything gives up the ability to look at one conversation, which the traces exist for. And a random sample of metric points gives a wrong rate: metrics are counts, and a missing count is a wrong count. Content is the other large cost, and Claude Code bounds it: the content limit defaults to 60 KB and you can "lower it to cut telemetry volume". Truncating a record cuts its volume and not its exposure: the first 200 characters of a prompt are still the customer's words. Restricting who may open the log store narrows the audience and leaves the copy. Encryption protects the store and does nothing about what the store holds, or about how long it is held.

### The example

The example is the one from the first page. Its second half is this page: the drift check against last week's numbers, with a tolerance of 25 percent, the alert rule with one window and with three in a row, and the log record with and without an allowed field. It ran offline in every language.

<!-- example: m87-trace-triage tabs: python,typescript,java,kotlin -->
```python
"""Observability decisions for a system of agents and tools: which traces to keep, how to find the layer that failed, when a change in a metric is drift, when to alert and what a log record may hold.

The traces, metrics and events are invented for the example. The rules come from the Claude Certified Architect - Professional exam guide (domains 3 and 4), Anthropic's article on its multi-agent research system
and the Claude Code monitoring documentation, read on 2026-10-04. Nothing here calls a model.
"""
import logging
from collections import namedtuple

log = logging.getLogger(__name__)

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
    log.debug("root_cause input: %r", spans)
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
import { logger } from "./logger.ts";
const log = logger("trace_triage");

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
  log.debug("rootCause input", spans);
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
    private static final System.Logger LOG = System.getLogger(TraceTriage.class.getName());
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
        LOG.log(System.Logger.Level.DEBUG, "rootCause input: {0}", spans);
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
private val log = System.getLogger("trace_triage")

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
    log.log(System.Logger.Level.DEBUG, "rootCause input: {0}", spans)
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

### The practice: trace triage as code

The practice is in [`exercises/87-observability-at-scale`](../../exercises/87-observability-at-scale/unit-01/practice-1/statement.md). You write the choice of reason to keep a trace, with its order of priority and the retry rule, the search for the deepest failing span and its path, the drift check, the alert over consecutive windows, the redaction of a log record with an allow list, and the trail of one request across components. It is graded in Python, TypeScript, Java and Kotlin; the statement lists eight cases, each saying what you should see.

## Traps

1. **"Alert whenever the metric crosses the line once."** It is tempting because it never misses a spike. It fails because most spikes are over before anyone looks, and a pager that cries every night is soon ignored. Alert on a change that lasts, and accept the delay.
2. **"Watch the error rate; if it is flat, the system is healthy."** It is tempting because errors are the most visible failure. It fails because a model change, a stale index or a growing prompt makes a system worse without throwing. Compare each layer's metrics with a baseline, up and down.
3. **"Put the conversation id on every metric so that nothing is lost."** It is tempting because it makes any slice possible. It fails on cost: every value becomes a series. Keep metric labels few, and put single occurrences in traces and events.

## Quiz

1. After a change of model, the error rate and the latency look the same. Yet tokens per answer are up by 40 percent and cost per answer by more than a third, and nobody was told. Which practice would have caught it?
   - **a**: Page the on-call engineer when the error rate passes a fixed ceiling, which covers every failure
   - **b**: Compare each layer's figures with values stored earlier and flag a shift past a set margin
   - **c**: Read a sample of the conversations each week and note anything that looks odd to the reader
   - **d**: Track the daily totals of tokens and cost, and read a rise in them as more use of the product

2. A team pages its on-call engineer whenever a single minute of tool errors passes the threshold. The pages come nightly and are mostly noise. Which change fits?
   - **a**: Raise the threshold until the nightly pages stop coming
   - **b**: Average the error rate over a full day before comparing
   - **c**: Send the alert to a channel that is read in the morning
   - **d**: Fire only when several windows in a row cross the line

<details>
<summary>Answer key</summary>

1. **b**. A baseline for each metric, compared in both directions with a tolerance, flags a system that got worse without failing. *a* is ruled out because "An error rate alone misses a system that is wrong without failing". *c* is ruled out because "Reading a sample of conversations each week finds what a reader happens to see, and it does not scale to two million conversations". *d* is ruled out because "a rise is as likely to be a fault as growth".
2. **d**. An alert on a change that lasts, several windows in a row, removes the one-window spikes at the price of delay. *a* is ruled out because "Raising the threshold until the pages stop hides the real incident with the noise". *c* is ruled out because "A message nobody reads at night turns a page into a log line". *b* is ruled out because "A daily average smooths a one-hour outage into a normal day".

</details>

## Module quiz

This quiz covers both pages of the module.

1. A project commits a settings file in its repository that turns on prompt logging for telemetry, so that a reviewer can read developers' prompts. What does Claude Code do with it?
   - **a**: It captures the prompts of everyone who clones the repository, starting with the first session
   - **b**: It captures the prompts only in sessions where each developer has also enabled tracing
   - **c**: It ignores the setting, and nothing typed in any session is captured because of it
   - **d**: It applies the setting but redacts the prompts anyway, because redaction cannot be changed

2. A complaint must be followed through the gateway, the orchestrator, two subagents and a tool, each logging in its own format. The records cannot be joined into one story. What was missing?
   - **a**: A common timestamp format across the services, so that the records can be sorted into a single list
   - **b**: A request id that the entry point assigns and every later service writes into each of its log lines
   - **c**: A longer retention period for the logs, so that all of the records are still there when somebody needs them
   - **d**: One central log store that every service writes to, in place of the separate stores that each of them has now

3. After each team added the conversation id as a metric label, the metrics bill tripled, and the dashboards are used no differently. Which fix fits?
   - **a**: Move the metrics to a larger store and keep the labels
   - **b**: Remove the id from the whole system, traces and all
   - **c**: Store a random tenth of the metric points to cut the series
   - **d**: Move the value from the series to traces and events

<details>
<summary>Answer key</summary>

1. **c**. Claude Code ignores the exporter variables in a repository's settings, so the file captures nothing. *a* is ruled out because "a repository can't use them to turn telemetry on". *b* is ruled out because "a repository cannot turn any of it on", whether or not a developer enables tracing. *d* is ruled out because "each is opened by a separate variable", so redaction can be changed, just not from a repository.
2. **b**. A shared identifier, taken at the edge and written everywhere, joins the records. *a* is ruled out because "Time alone cannot join records: two requests at the same millisecond look the same". *c* is ruled out because "keeping records longer keeps the same unjoinable records longer". *d* is ruled out because "A central store collects the records and still does not say which belong together".
3. **d**. The id is high-cardinality, so it belongs on the traces and events, which are meant for single occurrences. *a* is ruled out because "Moving to a larger store pays for the series and leaves their count growing". *c* is ruled out because "a random sample of metric points gives a wrong rate: metrics are counts, and a missing count is a wrong count". *b* is ruled out because "Dropping the id from everything gives up the ability to look at one conversation, which the traces exist for".

</details>
