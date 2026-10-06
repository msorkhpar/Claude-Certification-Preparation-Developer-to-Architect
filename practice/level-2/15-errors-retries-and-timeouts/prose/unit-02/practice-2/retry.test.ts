import { test } from "node:test";
import assert from "node:assert/strict";
import { pathToFileURL } from "node:url";
import { resolve } from "node:path";

// The solution is the file beside this test.
const dir = resolve(import.meta.dirname);
const { CallFailed, TransportError, callWithRetry } = await import(pathToFileURL(resolve(dir, "retry.ts")).href);

const OK = { status: 200, headers: {}, body: { type: "message" } };

function status(code: number, headers: Record<string, string> = {}, kind?: string, details?: object) {
  const error: Record<string, unknown> = { type: kind ?? "api_error", message: "x" };
  if (details) error.details = details;
  return { status: code, headers, body: { type: "error", error } };
}

/** Runs the policy against scripted replies (an Error is thrown); returns what the tests judge. */
function run(replies: any[], options: Record<string, unknown> = {}) {
  const queue = [...replies];
  const slept: number[] = [];
  let calls = 0;
  const send = () => {
    calls++;
    const item = queue.shift();
    if (item instanceof Error) throw item;
    return item;
  };
  try {
    const result = callWithRetry(send, (s: number) => slept.push(s), options);
    return { calls: () => calls, slept, result, err: null as any };
  } catch (err) {
    return { calls: () => calls, slept, result: null as any, err: err as any };
  }
}

test("m1 overloaded twice then success returns the good reply after two waits", () => {
  const r = run([status(529, {}, "overloaded_error"), status(503), OK]);
  assert.ok(r.err === null && r.result === OK);
  assert.equal(r.calls(), 3);
  assert.deepEqual(r.slept, [0.5, 1.0]);
});

test("e1 client errors are not retried", () => {
  for (const [code, kind] of [[400, "invalid_request_error"], [401, "authentication_error"], [404, "not_found_error"], [413, "request_too_large"]] as const) {
    const r = run([status(code, { "request-id": "req_1" }, kind), OK]);
    assert.ok(r.err instanceof CallFailed, `${code}: ${r.err}`);
    assert.deepEqual([r.err.status, r.err.errorType, r.err.attempts, r.err.requestId], [code, kind, 1, "req_1"]);
    assert.ok(r.calls() === 1 && r.slept.length === 0);
  }
});

test("e2 retry after is a floor for the wait", () => {
  const r = run([status(429, { "retry-after": "3" }, "rate_limit_error"), status(429, { "retry-after": "1" }, "rate_limit_error"), status(529, { "retry-after": "1" }), OK], { maxAttempts: 5 });
  assert.equal(r.err, null);
  assert.deepEqual(r.slept, [3, 1, 2]); // waits: max(0.5, 3), max(1, 1), max(2, 1)
});

test("e3 delay doubles up to the cap and the jitter is applied last", () => {
  const six = Array.from({ length: 6 }, () => status(500));
  const r = run([...six, OK], { maxAttempts: 7, baseDelay: 1, cap: 5 });
  assert.ok(r.err === null);
  assert.deepEqual(r.slept, [1, 2, 4, 5, 5, 5]);
  const halved = run([status(500), status(500), OK], { jitter: (d: number) => d / 2 });
  assert.deepEqual(halved.slept, [0.25, 0.5]);
});

test("e4 a spend cap 429 is not retried", () => {
  const cap = status(429, { "request-id": "req_cap" }, "rate_limit_error", { error_code: "enforced_spend_limit_reached" });
  const r = run([cap, OK]);
  assert.ok(r.err instanceof CallFailed && r.err.status === 429 && r.err.attempts === 1 && r.err.requestId === "req_cap");
  assert.ok(r.calls() === 1 && r.slept.length === 0);
});

test("e5 connection errors are retried like server errors", () => {
  const r = run([new TransportError("reset"), new TransportError("timeout"), OK]);
  assert.ok(r.err === null && r.result === OK && r.calls() === 3);
  assert.deepEqual(r.slept, [0.5, 1.0]);
  const dead = run([new TransportError("a"), new TransportError("b")], { maxAttempts: 2 });
  assert.ok(dead.err instanceof CallFailed, String(dead.err));
  assert.deepEqual([dead.err.status, dead.err.errorType, dead.err.attempts, dead.err.requestId], [0, "connection_error", 2, null]);
});

test("e6 giving up reports the last reply and does not wait after the last attempt", () => {
  const r = run([status(500, { "request-id": "req_a" }), status(503, { "request-id": "req_b" }), status(529, { "request-id": "req_c" }, "overloaded_error"), OK], { maxAttempts: 3 });
  assert.ok(r.err instanceof CallFailed, String(r.err));
  assert.ok(r.calls() === 3 && r.slept.length === 2);
  assert.deepEqual([r.err.status, r.err.errorType, r.err.attempts, r.err.requestId], [529, "overloaded_error", 3, "req_c"]);
});
