/** Trace triage: which traces to keep, the layer that failed, drift, alerts, redaction and one request's trail. See ../../statement.md. */

export type Span = { id: string; parent: string; kind: string; name: string; status: string; ms: number; note: string };
export type Cause = { layer: string; name: string; why: string; path: string[] };
export type Event = { request: string; ts: number; component: string; message: string };
export const CONTENT = new Set(["prompt", "response", "tool_input", "tool_output"]);

export function bucket(traceId: string): number {
  let h = 7;
  for (const c of traceId) h = (h * 31 + c.charCodeAt(0)) % 1000003;
  return h % 100;
}

export function keepTrace(traceId: string, spans: Span[], rate: number, feedback = false, slowMs = 5000): string {
  if (spans.some((s) => s.status === "error")) return "error";
  if (spans[0].ms > slowMs) return "slow";
  const tools = spans.filter((s) => s.kind === "tool").map((s) => s.name);
  if (tools.some((n) => tools.filter((m) => m === n).length >= 3)) return "retries";
  if (feedback) return "feedback";
  return bucket(traceId) < rate ? "sampled" : "dropped";
}

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

export function alertAt(series: number[], threshold: number, windows: number): number {
  let run = 0;
  for (let i = 0; i < series.length; i++) {
    run = series[i] > threshold ? run + 1 : 0;
    if (run >= windows) return i;
  }
  return -1;
}

export function redact(event: Record<string, string | number>, allowed: string[] = []): Record<string, string | number> {
  return Object.fromEntries(Object.entries(event).filter(([k]) => !CONTENT.has(k) || allowed.includes(k)));
}

export function requestTrail(events: Event[], request: string): string[] {
  return events.filter((e) => e.request === request).sort((a, b) => a.ts - b.ts).map((e) => `${e.component}: ${e.message}`);
}
