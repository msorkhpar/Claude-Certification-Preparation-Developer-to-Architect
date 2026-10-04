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
