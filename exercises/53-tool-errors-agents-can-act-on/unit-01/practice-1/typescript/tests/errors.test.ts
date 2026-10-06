import { test } from "node:test";
import assert from "node:assert/strict";
import { resolve } from "node:path";
import { pathToFileURL } from "node:url";

// SOLUTION_DIR selects starter, reference or a planted wrong solution.
const dir = resolve(process.env.SOLUTION_DIR ?? "starter");
const { ToolError, makeError, nextAction, runTool, toToolResult } = await import(pathToFileURL(resolve(dir, "errors.ts")).href);

const ARGS = { order: "A-7", amount: 40 };

/** A scripted tool: each call takes the next script entry (an Error is thrown, anything else is returned); every call is kept. */
function makeTool(...script: any[]) {
  const calls: any[] = [];
  const tool = (args: any) => {
    calls.push({ ...args });
    const step = script.length > 1 ? script.shift() : script[0];
    if (step instanceof Error) throw step;
    return step;
  };
  return { tool, calls };
}

function run(tool: any, policy: any = { max_retries: 2, base_delay_ms: 100 }, args: any = { ...ARGS }) {
  const waits: number[] = [];
  const result = runTool(tool, args, policy, (ms: number) => waits.push(ms));
  assert.ok(result !== null && result !== undefined, "runTool returned nothing");
  return { result, waits };
}

test("m1 a failed call becomes a structured error with a category a retry flag and an error flag", () => {
  const error = makeError("transient", "The billing service timed out after 5 s.");
  assert.deepEqual(error, { is_error: true, category: "transient", retryable: true, message: "The billing service timed out after 5 s.", attempts: 1 });
  assert.deepEqual(toToolResult("toolu_1", error), { type: "tool_result", tool_use_id: "toolu_1", content: "transient error (retryable: yes): The billing service timed out after 5 s.", is_error: true });
  const business = makeError("business", "Refunds above 500 need a person.", "A colleague will contact you about this refund.");
  assert.ok(business.retryable === false && business.explanation === "A colleague will contact you about this refund.");
  assert.equal(toToolResult("toolu_2", business).content, "business error (retryable: no): Refunds above 500 need a person. Tell the customer: A colleague will contact you about this refund.");
  for (const kind of ["validation", "permission", "outcome_unknown", "internal"]) assert.equal(makeError(kind, "Specific text.").retryable, false);
  assert.deepEqual(toToolResult("toolu_3", { ok: true, content: "refund R-1 created" }), { type: "tool_result", tool_use_id: "toolu_3", content: "refund R-1 created", is_error: false });
});

test("e1 a generic message or an unknown category is refused", () => {
  let refused = 0;
  for (const message of ["Operation failed", "", "   ", "Failed.", "Error", "Something went wrong"]) {
    try { makeError("transient", message); } catch { refused++; }
  }
  assert.equal(refused, 6);
  refused = 0;
  for (const kind of ["oops", "timeout", "", "Transient"]) {
    try { makeError(kind, "A specific message that says what to change."); } catch { refused++; }
  }
  assert.equal(refused, 4);
});

test("e2 only transient failures are retried with growing waits and the other kinds return at once", () => {
  let t = makeTool(new ToolError("transient", "Billing is unavailable."), new ToolError("transient", "Billing is unavailable."), "refund R-1 created");
  let r = run(t.tool);
  assert.ok(r.result.ok === true && r.result.content === "refund R-1 created" && r.result.attempts === 3);
  assert.deepEqual(r.waits, [100, 200]);
  assert.equal(t.calls.length, 3);
  for (const [kind, message, explanation] of [["validation", "amount must be a positive whole number", null], ["permission", "this key may not issue refunds", null], ["business", "Refunds above 500 need a person.", "A colleague will contact you."]] as const) {
    t = makeTool(new ToolError(kind, message, null, explanation), "never reached");
    r = run(t.tool);
    assert.ok(r.result.is_error === true && r.result.category === kind && r.result.retryable === false);
    assert.equal(r.result.message, message);
    assert.equal(r.result.explanation ?? null, explanation);
    assert.ok(r.result.attempts === 1 && r.waits.length === 0 && t.calls.length === 1);
    assert.deepEqual(r.result.attempted, ARGS);
  }
});

test("e3 the retries are bounded and a wait the service asks for is honoured", () => {
  let t = makeTool(new ToolError("transient", "Billing is unavailable."));
  let r = run(t.tool);
  assert.equal(t.calls.length, 3);
  assert.deepEqual(r.waits, [100, 200]);
  assert.ok(r.result.is_error === true && r.result.category === "transient" && r.result.retryable === true && r.result.attempts === 3);
  assert.ok(r.result.message.includes("Gave up after 3 attempts"));
  assert.deepEqual(r.result.attempted, ARGS);
  t = makeTool(new ToolError("transient", "Rate limited.", 1500), new ToolError("transient", "Rate limited."), "ok");
  r = run(t.tool);
  assert.deepEqual(r.waits, [1500, 200]);
  assert.ok(r.result.ok === true && r.result.attempts === 3);
  t = makeTool(new ToolError("transient", "Billing is unavailable."));
  r = run(t.tool, { max_retries: 0, base_delay_ms: 100 });
  assert.ok(t.calls.length === 1 && r.waits.length === 0 && r.result.attempts === 1 && r.result.category === "transient");
});

test("e4 a valid empty result is a success and not an error", () => {
  for (const empty of [[], "", {}, null]) {
    const r = run(makeTool(empty).tool);
    assert.ok(r.result.ok === true && r.result.empty === true && r.result.attempts === 1 && r.waits.length === 0);
    assert.equal(nextAction(r.result), "accept_empty");
  }
  const block = toToolResult("toolu_1", { ok: true, content: "" });
  assert.ok(block.is_error === false);
  assert.equal(block.content, "");
  const full = run(makeTool("3 orders").tool);
  assert.ok(full.result.empty === false && nextAction(full.result) === "continue");
});

test("e5 a timeout on a write is an unknown outcome and is retried only when repeating it is safe", () => {
  let t = makeTool(new ToolError("timeout", "No answer from the refund service."), "refund R-1 created");
  let r = run(t.tool);
  assert.ok(t.calls.length === 1 && r.waits.length === 0);
  assert.ok(r.result.is_error === true && r.result.category === "outcome_unknown" && r.result.retryable === false);
  assert.ok(r.result.message.includes("No answer from the refund service.") && r.result.message.includes("check the current state"));
  assert.deepEqual(r.result.attempted, ARGS);
  t = makeTool(new ToolError("timeout", "No answer."), "3 orders");
  r = run(t.tool, { max_retries: 2, base_delay_ms: 100, read_only: true });
  assert.ok(r.result.ok === true && r.result.attempts === 2);
  assert.deepEqual(r.waits, [100]);
  t = makeTool(new ToolError("timeout", "No answer."), new ToolError("timeout", "No answer."), "refund R-1 created");
  const mine = { ...ARGS };
  r = run(t.tool, { max_retries: 2, base_delay_ms: 100, idempotency_key: "k-1" }, mine);
  assert.ok(r.result.ok === true && r.result.attempts === 3);
  assert.deepEqual(r.waits, [100, 200]);
  assert.deepEqual(t.calls, [{ ...ARGS, idempotency_key: "k-1" }, { ...ARGS, idempotency_key: "k-1" }, { ...ARGS, idempotency_key: "k-1" }]);
  assert.deepEqual(mine, ARGS, "the caller's arguments must not be changed");
});

test("e6 the next action follows the category", () => {
  const expected: Record<string, string> = { transient: "retry_later", validation: "repair_input", permission: "escalate", business: "explain", outcome_unknown: "verify_first", internal: "escalate" };
  for (const [kind, action] of Object.entries(expected)) assert.equal(nextAction(makeError(kind, "A specific message.")), action);
  assert.equal(nextAction({ ok: true, content: "x", empty: false }), "continue");
  assert.equal(nextAction({ ok: true, content: [], empty: true }), "accept_empty");
});

test("e7 an unexpected exception becomes an internal error and the run goes on", () => {
  for (const boom of [new Error("boom"), new TypeError("missing")]) {
    const t = makeTool(boom, "never reached");
    const r = run(t.tool);
    assert.ok(r.result.is_error === true && r.result.category === "internal" && r.result.retryable === false);
    assert.ok(r.result.attempts === 1 && r.waits.length === 0 && t.calls.length === 1);
  }
  const r = run(makeTool(new Error("boom")).tool);
  assert.ok(r.result.message.includes("boom"));
  assert.deepEqual(r.result.attempted, ARGS);
});
