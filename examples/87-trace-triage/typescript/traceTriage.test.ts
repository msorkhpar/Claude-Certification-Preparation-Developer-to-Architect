import { test } from "node:test";
import assert from "node:assert/strict";
import type { Span } from "./traceTriage.ts";
import { TRACES, alertAt, drift, keepReason, redact, rootCause } from "./traceTriage.ts";

test("errors and slow traces are always kept", () => {
  assert.equal(keepReason("t-refund", TRACES["t-refund"], 0), "error");
  const slow: Span[] = [{ id: "s1", parent: "", kind: "agent", name: "a", status: "ok", ms: 6000, note: "" }];
  assert.equal(keepReason("x", slow, 0), "slow");
});

test("healthy traces are kept by id and not by chance", () => {
  const ids = Array.from({ length: 100 }, (_, i) => `trace-${i}`);
  const run = () => ids.map((t) => keepReason(t, TRACES["t-plain"], 10));
  assert.deepEqual(run(), run());
  const n = run().filter((r) => r === "sampled").length;
  assert.ok(n > 0 && n < 100);
  assert.equal(keepReason("trace-1", TRACES["t-plain"], 0), "dropped");
});

test("root cause is the deepest failing span", () => {
  const c = rootCause(TRACES["t-refund"]);
  assert.deepEqual([c.layer, c.name], ["tool", "web_fetch"]);
  assert.deepEqual(c.path, ["orchestrator", "order-researcher", "web_fetch"]);
});

test("a stale retrieval is blamed when nothing failed", () => {
  assert.equal(rootCause(TRACES["t-policy"]).why, "stale");
  assert.equal(rootCause(TRACES["t-empty"]).why, "no-hits");
  assert.equal(rootCause(TRACES["t-plain"]).layer, "none");
});

test("drift flags both directions over tolerance", () => {
  assert.deepEqual(drift({ a: 10, b: 10, c: 10 }, { a: 14, b: 6, c: 11 }, 25), ["a up 40%", "b down 40%"]);
});

test("an alert needs consecutive windows", () => {
  const s = [1, 2, 9, 2, 8, 9, 10, 3];
  assert.equal(alertAt(s, 5, 1), 2);
  assert.equal(alertAt(s, 5, 3), 6);
  assert.equal(alertAt([9, 1, 9, 1, 9], 5, 2), -1);
});

test("redact drops content unless allowed", () => {
  const e = { trace: "t", prompt: "x", tool_input: "y", input_tokens: 5 };
  assert.deepEqual(redact(e), { trace: "t", input_tokens: 5 });
  assert.deepEqual(redact(e, ["tool_input"]), { trace: "t", tool_input: "y", input_tokens: 5 });
});
