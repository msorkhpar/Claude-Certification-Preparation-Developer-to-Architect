/** Trace triage: which traces to keep, the layer that failed, drift, alerts, redaction and one request's trail. See ../../statement.md. */
import { logger } from "./logger.ts";
const log = logger("triage");

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
  log.debug("keepTrace input", spans);
  // TODO 1 of 7 (unlocks m1 and e1): why a trace is kept.
  // Receives the trace id, its spans (`status`, `ms`, `kind`, `name`; the first span is the root), the sampling rate (0 to 100) and the flags. Returns the first reason
  // that applies, in this order: "error" when any span has the status "error"; "slow" when the root's `ms` is over `slowMs` (equal is not slow); "retries" when
  // one tool (a span of kind "tool") was called three times or more by name; "feedback" when `feedback` is set; otherwise "sampled" when `bucket(traceId)` is
  // below `rate` and "dropped" when it is not.
  // Example: one tool span called "fetch" three times, nothing else wrong -> "retries"
  return null;
}

function deepest(failed: Span[]): Span {
  // TODO 2 of 7 (unlocks e2): the span that is the origin of a failure.
  // Receives the spans that failed (each has `id` and `parent`). Returns the first one that is not the parent of another failed span, so the error that a
  // parent merely reported is passed over for the span below it.
  // Example: s1 (root) <- s2 <- s3, all failed -> s3; two failed children of one parent -> the first of them
  return failed[0];
}

function blamedRetrieval(spans: Span[]): Span | undefined {
  // TODO 3 of 7 (unlocks e3): the retrieval span to blame when nothing failed.
  // Receives the spans. Returns the first span of kind "retrieval" whose `note` is "stale" or "no-hits", or undefined when there is none.
  // Example: a retrieval span with the note "stale" -> that span
  return undefined;
}

export function rootCause(spans: Span[]): Cause {
  const byId = new Map(spans.map((s) => [s.id, s]));
  const failed = spans.filter((s) => s.status === "error");
  if (failed.length > 0) {
    const origin = deepest(failed);
    const path: string[] = [];
    let cursor: Span | undefined = origin;
    while (cursor !== undefined) {
      path.push(cursor.name);
      cursor = byId.get(cursor.parent);
    }
    return { layer: origin.kind, name: origin.name, why: "failed", path: path.reverse() };
  }
  const s = blamedRetrieval(spans);
  if (s !== undefined) return { layer: "retrieval", name: s.name, why: s.note, path: [spans[0].name, s.name] };
  return { layer: "none", name: "", why: "no span failed", path: [] };
}

export function drift(baseline: Record<string, number>, current: Record<string, number>, tolerance: number): string[] {
  // TODO 4 of 7 (unlocks e4): the metrics that moved.
  // Receives the baseline and the current values (metric name to a whole number) and a tolerance in percent. Goes through the baseline names in alphabetical order.
  // The change is `Math.floor(Math.abs(now - base) * 100 / base)`; a baseline of 0 counts as 100 when the value is not 0 and 0 when it is. Returns one string
  // "<name> up <pct>%" or "<name> down <pct>%" for each metric whose change is over the tolerance (equal is fine).
  // Example: baseline 10, now 14, tolerance 30 -> ["a up 40%"]
  return [];
}

export function alertAt(series: number[], threshold: number, windows: number): number {
  // TODO 5 of 7 (unlocks e5): when an alert fires.
  // Receives the series of window values, the threshold and how many windows in a row must be over it (strictly over). Returns the index of the window at which the
  // count of consecutive windows over the threshold first reaches `windows`, or -1 when it never does. A window at or under the threshold starts the count again.
  // Example: [1, 2, 9, 2, 8, 9, 10, 3], threshold 5, windows 3 -> 6
  return -1;
}

export function redact(event: Record<string, string | number>, allowed: string[] = []): Record<string, string | number> {
  // TODO 6 of 7 (unlocks e6): what a log record keeps.
  // Receives a record (an object) and the names that may stay. Returns a new object without the fields in CONTENT (`prompt`, `response`, `tool_input`, `tool_output`) unless the
  // field's name is in `allowed`; every other field stays.
  // Example: { trace: "t", prompt: "x" } -> { trace: "t" }
  return { ...event };
}

export function requestTrail(events: Event[], request: string): string[] {
  // TODO 7 of 7 (unlocks e7): one request's story.
  // Receives the events of every component (`request`, `ts`, `component`, `message`) and a request id. Returns "<component>: <message>" for each event of that request, in
  // order of `ts` (events with the same `ts` stay in the order they came); an unknown request gives an empty array.
  // Example: events at ts 30 (tool), 10 (api), 20 (agent) -> ["api: ...", "agent: ...", "tool: ..."]
  return [];
}
