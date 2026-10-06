import { test } from "node:test";
import assert from "node:assert/strict";
import { pathToFileURL } from "node:url";
import { resolve } from "node:path";

// The solution is the file beside this test.
const dir = resolve(import.meta.dirname);
const { BatchError, buildRequests, collect, splitBatches } = await import(pathToFileURL(resolve(dir, "batches.ts")).href);

const MODEL = "claude-haiku-4-5-20251001";

const item = (id: string, extra: Record<string, unknown> = {}) => ({
  id,
  params: { model: MODEL, max_tokens: 200, messages: [{ role: "user", content: `Classify ticket ${id}` }], ...extra },
});
const line = (customId: string, result: unknown) => JSON.stringify({ custom_id: customId, result });
const message = (text: string, inputTokens = 50, outputTokens = 7, extraUsage: Record<string, number> = {}) => ({
  type: "succeeded",
  message: { id: "msg_illustrative", type: "message", role: "assistant", model: MODEL, stop_reason: "end_turn", content: [{ type: "text", text }], usage: { input_tokens: inputTokens, output_tokens: outputTokens, ...extraUsage } },
});
const errored = (kind: string) => ({ type: "errored", error: { type: "error", error: { type: kind, message: "x" } } });

/** The BatchError field fn throws, "crash" for another error, null when it returns. */
function failureOf(fn: () => unknown): string | null {
  try {
    fn();
  } catch (err) {
    return err instanceof BatchError ? err.field : "crash";
  }
  return null;
}

test("m1 results are matched to requests by custom id not by position", () => {
  const requests = buildRequests([item("t-1"), item("t-2"), item("t-3")]) ?? [];
  assert.deepEqual(requests.map((r: any) => r.custom_id), ["t-1", "t-2", "t-3"]);
  assert.equal(requests[0].params.max_tokens, 200);
  const done = collect(requests, [line("t-3", message("billing")), line("t-1", message("refund")), line("t-2", message("shipping"))]) ?? {};
  assert.deepEqual((done.outcomes ?? []).map((o: any) => [o.custom_id, o.status, o.text]), [["t-1", "succeeded", "refund"], ["t-2", "succeeded", "shipping"], ["t-3", "succeeded", "billing"]]);
  assert.deepEqual([done.retry, done.fix, done.unknown], [[], [], []]);
});

test("e1 a custom id is 1 to 64 safe characters and unique", () => {
  for (const bad of ["", "has space", "dot.dot", "x".repeat(65), "naïve"]) assert.equal(failureOf(() => buildRequests([item(bad)])), "custom_id", JSON.stringify(bad));
  assert.equal(failureOf(() => buildRequests([item("a"), item("a")])), "custom_id");
  assert.equal(failureOf(() => buildRequests([item("x".repeat(64)), item("A_b-9")])), null);
});

test("e2 parameters a batch cannot take are refused", () => {
  assert.equal(failureOf(() => buildRequests([item("a", { stream: true })])), "params.stream");
  assert.equal(failureOf(() => buildRequests([item("a", { speed: "fast" })])), "params.speed");
  assert.equal(failureOf(() => buildRequests([{ id: "a", params: { model: MODEL, max_tokens: 0, messages: [] } }])), "params.max_tokens");
  assert.equal(failureOf(() => buildRequests([item("a", { stream: false })])), null);
});

test("e3 a big job is cut in order by request count and by size", () => {
  const requests = buildRequests(Array.from({ length: 7 }, (_, i) => item(`r${i}`))) ?? [];
  const byCount = splitBatches(requests, 3) ?? [];
  assert.deepEqual(byCount.map((b: any[]) => b.map((r) => r.custom_id)), [["r0", "r1", "r2"], ["r3", "r4", "r5"], ["r6"]]);
  const size = Buffer.byteLength(JSON.stringify(requests[0] ?? {}), "utf8");
  const bySize = splitBatches(requests, 100_000, size * 2 + 5) ?? [];
  assert.deepEqual(bySize.map((b: any[]) => b.length), [2, 2, 2, 1]);
  assert.deepEqual(bySize.flat().map((r: any) => r.custom_id), Array.from({ length: 7 }, (_, i) => `r${i}`));
  assert.deepEqual(splitBatches([]), []);
  assert.equal(failureOf(() => splitBatches(requests, 100_000, size - 1)), "size");
});

test("e4 invalid requests are fixed and the rest are retried", () => {
  const requests = buildRequests([item("ok"), item("bad"), item("busy"), item("late"), item("stopped")]) ?? [];
  const lines = [line("ok", message("fine")), line("bad", errored("invalid_request_error")), line("busy", errored("overloaded_error")), line("late", { type: "expired" }), line("stopped", { type: "canceled" })];
  const done = collect(requests, lines) ?? {};
  assert.deepEqual(done.fix, ["bad"]);
  assert.deepEqual(done.retry, ["busy", "late", "stopped"]);
  assert.deepEqual((done.outcomes ?? []).map((o: any) => o.status), ["succeeded", "errored", "errored", "expired", "canceled"]);
  assert.equal((done.outcomes ?? [])[1]?.error_type, "invalid_request_error");
});

test("e5 a request with no result is missing and a stranger is reported", () => {
  const requests = buildRequests([item("a"), item("b"), item("c")]) ?? [];
  const done = collect(requests, [line("c", message("three")), "", line("zzz", message("who")), line("a", message("one"))]) ?? {};
  assert.deepEqual((done.outcomes ?? []).map((o: any) => o.status), ["succeeded", "missing", "succeeded"]);
  assert.deepEqual(done.retry, ["b"]);
  assert.deepEqual(done.unknown, ["zzz"]);
  assert.equal((done.outcomes ?? []).length, 3);
});

test("e6 only requests that succeeded count toward usage", () => {
  const requests = buildRequests([item("a"), item("b"), item("c")]) ?? [];
  const lines = [line("a", message("x", 100, 10, { cache_read_input_tokens: 400 })), line("b", errored("api_error")), line("c", message("y", 30, 5, { cache_creation_input_tokens: 200 }))];
  assert.deepEqual((collect(requests, lines) ?? {}).usage, { input_tokens: 130, output_tokens: 15, cache_creation_input_tokens: 200, cache_read_input_tokens: 400 });
});
