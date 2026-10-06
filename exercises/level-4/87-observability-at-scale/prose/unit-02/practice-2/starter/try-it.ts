// Run executes this file. Change the calls to try your code; Submit runs the tests.
import { logTo } from "./logger.ts";
import { keepTrace, redact, requestTrail, rootCause } from "./triage.ts";

// Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logTo("try-it");

const span = (id: string, parent: string, kind: string, name: string, status: string, ms: number, note = "") => ({ id, parent, kind, name, status, ms, note });

// One trace of a request: the assistant calls a search, then the model answers slowly.
const spans = [span("s1", "", "agent", "assistant", "ok", 1900), span("s2", "s1", "retrieval", "search", "ok", 100), span("s3", "s1", "llm", "answer", "ok", 1700)];
console.log("kept as:", keepTrace("trace-1", spans, 0));
console.log("kept as (a failed span):", keepTrace("trace-1", [span("s1", "", "agent", "assistant", "error", 90, "timeout")], 0));

// Where the failure started, from the deepest failed span.
const failed = [span("s1", "", "agent", "assistant", "error", 900), span("s2", "s1", "tool", "lookup_order", "error", 800, "HTTP 500")];
console.log("root cause:", JSON.stringify(rootCause(failed)));

// A log event without content, and the trail of one request across components.
console.log("redacted:", redact({ trace: "t", tool_input: "secret", input_tokens: 5 }));
const events = [{ request: "r1", ts: 30, component: "tool", message: "lookup done" }, { request: "r2", ts: 10, component: "api", message: "other" },
  { request: "r1", ts: 10, component: "api", message: "received" }];
console.log("trail:", requestTrail(events, "r1"));
