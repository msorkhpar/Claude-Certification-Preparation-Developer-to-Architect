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

export function keepTrace(traceId: string, spans: Span[], rate: number, feedback = false, slowMs = 5000): string | null {
  // TODO: error, slow, retries, feedback, sampled or dropped, in that order of priority.
  return null;
}

export function rootCause(spans: Span[]): Cause | null {
  // TODO: the layer, name, why and path of the deepest failing span, or of a stale or empty retrieval when nothing failed.
  return null;
}

export function drift(baseline: Record<string, number>, current: Record<string, number>, tolerance: number): string[] | null {
  // TODO: "<name> up|down <percent>%" for each metric that moved by more than the tolerance, sorted by name.
  return null;
}

export function alertAt(series: number[], threshold: number, windows: number): number | null {
  // TODO: the index that completes `windows` consecutive values over the threshold, or -1.
  return null;
}

export function redact(event: Record<string, string | number>, allowed: string[] = []): Record<string, string | number> | null {
  // TODO: drop the content fields unless they are allowed by name.
  return null;
}

export function requestTrail(events: Event[], request: string): string[] | null {
  // TODO: "<component>: <message>" for each event of the request, in time order.
  return null;
}
