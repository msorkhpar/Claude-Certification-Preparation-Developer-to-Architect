import { test } from "node:test";
import assert from "node:assert/strict";
import { resolve } from "node:path";
import { pathToFileURL } from "node:url";

// SOLUTION_DIR selects starter, reference or a planted wrong solution.
const dir = resolve(process.env.SOLUTION_DIR ?? "starter");
const { alertAt, drift, keepTrace, redact, requestTrail, rootCause } = await import(pathToFileURL(resolve(dir, "triage.ts")).href);

const sp = (id: string, parent = "", kind = "agent", name = "n", status = "ok", ms = 100, note = "") => ({ id, parent, kind, name, status, ms, note });

function keep(traceId: string, spans: any[], rate: number, feedback = false, slowMs = 5000): string {
  const result = keepTrace(traceId, spans, rate, feedback, slowMs);
  assert.equal(typeof result, "string", "keepTrace returned nothing");
  return result;
}

function cause(spans: any[]): any {
  const result = rootCause(spans);
  assert.ok(result !== null && typeof result === "object", "rootCause returned nothing");
  return result;
}

function listed(result: any): any {
  assert.ok(Array.isArray(result), "a list was expected");
  return result;
}

const PLAIN = [sp("s1", "", "agent", "assistant", "ok", 1900), sp("s2", "s1", "retrieval", "search", "ok", 100), sp("s3", "s1", "llm", "answer", "ok", 1700)];

test("m1 every trace gets one reason and a healthy trace is kept by its id", () => {
  assert.equal(keep("a", [sp("s1", "", "agent", "n", "error")], 0), "error");
  assert.equal(keep("a", [sp("s1", "", "agent", "n", "ok", 6000)], 0), "slow");
  assert.equal(keep("a", PLAIN, 0, true), "feedback");
  assert.equal(keep("a", PLAIN, 0), "dropped");
  assert.equal(keep("a", PLAIN, 100), "sampled");
  const ids = Array.from({ length: 100 }, (_, i) => `trace-${i}`);
  const first = ids.map((t) => keep(t, PLAIN, 10));
  assert.deepEqual(first, ids.map((t) => keep(t, PLAIN, 10)));
  const n = first.filter((r) => r === "sampled").length;
  assert.ok(n > 0 && n < 100);
});

test("e1 an error outranks a slow root which outranks retries which outrank a flag", () => {
  const calls = [sp("s2", "s1", "tool", "fetch"), sp("s3", "s1", "tool", "fetch"), sp("s4", "s1", "tool", "fetch")];
  assert.equal(keep("a", [sp("s1", "", "agent", "n", "error", 9000)], 0), "error");
  assert.equal(keep("a", [sp("s1", "", "agent", "n", "ok", 9000), ...calls], 0, true), "slow");
  assert.equal(keep("a", [sp("s1"), ...calls], 0, true), "retries");
  assert.equal(keep("a", [sp("s1"), ...calls.slice(0, 2)], 0), "dropped");
  assert.equal(keep("a", [sp("s1", "", "agent", "n", "ok", 5000)], 0), "dropped");
  assert.equal(keep("a", [sp("s1", "", "agent", "n", "ok", 3000)], 0, false, 2000), "slow");
});

test("e2 the root cause is the deepest failing span and its path starts at the root", () => {
  const spans = [sp("s1", "", "agent", "orchestrator", "error"), sp("s2", "s1", "agent", "researcher", "error"), sp("s3", "s2", "tool", "fetch", "error"), sp("s4", "s1", "llm", "summarise")];
  const c = cause(spans);
  assert.deepEqual([c.layer, c.name, c.why], ["tool", "fetch", "failed"]);
  assert.deepEqual(c.path, ["orchestrator", "researcher", "fetch"]);
  const two = [sp("s1", "", "agent", "top"), sp("s2", "s1", "tool", "first", "error"), sp("s3", "s1", "tool", "second", "error")];
  assert.equal(cause(two).name, "first");
});

test("e3 a stale or empty retrieval is blamed only when no span failed", () => {
  const stale = [sp("s1", "", "agent", "assistant"), sp("s2", "s1", "retrieval", "search", "ok", 100, "stale"), sp("s3", "s1", "llm", "answer")];
  assert.deepEqual([cause(stale).layer, cause(stale).why], ["retrieval", "stale"]);
  assert.deepEqual(cause(stale).path, ["assistant", "search"]);
  const empty = [sp("s1", "", "agent", "assistant"), sp("s2", "s1", "retrieval", "search", "ok", 100, "no-hits")];
  assert.equal(cause(empty).why, "no-hits");
  const both = [stale[0], stale[1], sp("s3", "s1", "llm", "answer", "error")];
  assert.deepEqual([cause(both).layer, cause(both).why], ["llm", "failed"]);
  assert.deepEqual(cause(PLAIN), { layer: "none", name: "", why: "no span failed", path: [] });
});

test("e4 drift reports a move in either direction over the tolerance and never divides by zero", () => {
  assert.deepEqual(listed(drift({ a: 10, b: 10, c: 10 }, { a: 14, b: 6, c: 11 }, 25)), ["a up 40%", "b down 40%"]);
  assert.deepEqual(listed(drift({ a: 10 }, { a: 13 }, 30)), []);
  assert.deepEqual(listed(drift({ a: 10 }, { a: 14 }, 30)), ["a up 40%"]);
  assert.deepEqual(listed(drift({ z: 0, y: 0 }, { z: 5, y: 0 }, 25)), ["z up 100%"]);
  assert.deepEqual(listed(drift({ b: 5, a: 5 }, { b: 10, a: 10 }, 0)), ["a up 100%", "b up 100%"]);
});

test("e5 an alert needs consecutive windows over the threshold and a dip starts the count again", () => {
  const s = [1, 2, 9, 2, 8, 9, 10, 3];
  assert.equal(alertAt(s, 5, 1), 2);
  assert.equal(alertAt(s, 5, 3), 6);
  assert.equal(alertAt([9, 1, 9, 1, 9], 5, 2), -1);
  assert.equal(alertAt([5, 5, 5], 5, 1), -1);
  assert.equal(alertAt([], 5, 1), -1);
});

test("e6 a log record drops the content fields unless they are allowed by name", () => {
  const e = { trace: "t", prompt: "x", response: "y", tool_input: "z", tool_output: "w", input_tokens: 5 };
  const red = (allowed?: string[]): any => {
    const r = allowed === undefined ? redact(e) : redact(e, allowed);
    assert.ok(r !== null && typeof r === "object", "redact returned nothing");
    return r;
  };
  assert.deepEqual(red(), { trace: "t", input_tokens: 5 });
  assert.deepEqual(red(["tool_input"]), { trace: "t", tool_input: "z", input_tokens: 5 });
  assert.deepEqual(red(["input_tokens"]), { trace: "t", input_tokens: 5 });
});

test("e7 a requests trail joins the events of every component in time order", () => {
  const events = [
    { request: "r1", ts: 30, component: "tool", message: "lookup done" },
    { request: "r2", ts: 10, component: "api", message: "other" },
    { request: "r1", ts: 10, component: "api", message: "received" },
    { request: "r1", ts: 20, component: "agent", message: "plan" },
    { request: "r1", ts: 20, component: "llm", message: "called" },
  ];
  assert.deepEqual(listed(requestTrail(events, "r1")), ["api: received", "agent: plan", "llm: called", "tool: lookup done"]);
  assert.deepEqual(listed(requestTrail(events, "r9")), []);
});
