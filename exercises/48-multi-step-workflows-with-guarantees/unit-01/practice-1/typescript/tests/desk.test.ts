import { test } from "node:test";
import assert from "node:assert/strict";
import { pathToFileURL } from "node:url";
import { resolve } from "node:path";

// SOLUTION_DIR selects starter, reference or a planted wrong solution.
const dir = resolve(process.env.SOLUTION_DIR ?? "starter");
const { RefundDesk } = await import(pathToFileURL(resolve(dir, "desk.ts")).href);

const ORDERS: Record<string, any> = {
  O1: { order_id: "O1", customer_id: "C1", total_cents: 5000, refunded_cents: 0 },
  O2: { order_id: "O2", customer_id: "C2", total_cents: 3000, refunded_cents: 0 },
  O9: { order_id: "O9", customer_id: "C1", total_cents: 50000, refunded_cents: 0 },
};

/** A scripted backend that keeps a log of every call it receives. */
function makeBackend(breakRefund = false) {
  const log: string[] = [];
  let refunds = 0;
  const backend = {
    verify_identity: (a: any) => { log.push("verify_identity"); return a.code === "1234" ? { verified: "yes", customer_id: "C1" } : { verified: "no" }; },
    lookup_order: (a: any) => { log.push("lookup_order"); return { ...ORDERS[a.order_id] }; },
    process_refund: (a: any) => { log.push("process_refund"); if (breakRefund) throw new Error("payment service offline"); refunds += 1; return { refund_id: `R${refunds}`, amount_cents: a.amount_cents }; },
    escalate: () => { log.push("escalate"); return { ticket_id: "T1" }; },
  };
  return { backend, log };
}

function desk(options: { breakRefund?: boolean; limit?: number } = {}) {
  const { backend, log } = makeBackend(options.breakRefund);
  return { d: new RefundDesk(backend, options.limit ?? 10000), log };
}

function res(d: any, name: string, args: Record<string, unknown> = {}) {
  const out = d.call(name, args);
  assert.ok(out !== null && out !== undefined, "call returned nothing");
  return out;
}

function verified(options: { breakRefund?: boolean; limit?: number } = {}) {
  const made = desk(options);
  res(made.d, "verify_identity", { code: "1234" });
  return made;
}

test("m1 a verified customer can look up an order and be refunded within the limit", () => {
  const { d, log } = desk();
  assert.deepEqual(res(d, "verify_identity", { code: "1234" }), { content: "verified=yes; customer_id=C1", is_error: false, blocked: null });
  assert.equal(res(d, "lookup_order", { order_id: "O1" }).content, "order_id=O1; customer_id=C1; total_cents=5000; refunded_cents=0");
  assert.deepEqual(res(d, "process_refund", { order_id: "O1", amount_cents: 2000 }), { content: "refund_id=R1; amount_cents=2000", is_error: false, blocked: null });
  const state = d.state() ?? {};
  assert.deepEqual([state.customer, state.locked, state.failures, state.blocked], ["C1", false, 0, []]);
  assert.deepEqual(state.refunds, [{ order_id: "O1", amount_cents: 2000, refund_id: "R1" }]);
  assert.equal(state.orders.O1.refunded_cents, 2000);
  assert.deepEqual(log, ["verify_identity", "lookup_order", "process_refund"]);
});

test("e1 a refund before identity is verified is blocked in code and never reaches the backend", () => {
  const { d, log } = desk();
  assert.deepEqual(res(d, "process_refund", { order_id: "O1", amount_cents: 500 }), { content: "BLOCKED identity_required: Verify the customer's identity before this action.", is_error: true, blocked: "identity_required" });
  assert.deepEqual(log, []);
  assert.deepEqual((d.state() ?? {}).blocked, [{ tool: "process_refund", code: "identity_required" }]);
  res(d, "verify_identity", { code: "1234" });
  res(d, "lookup_order", { order_id: "O1" });
  assert.equal(res(d, "process_refund", { order_id: "O1", amount_cents: 500 }).blocked, null);
  assert.equal(log.at(-1), "process_refund");
});

test("e2 an order that belongs to someone else is neither shown nor refundable", () => {
  const { d, log } = desk();
  assert.equal(res(d, "lookup_order", { order_id: "O1" }).blocked, "identity_required");
  assert.deepEqual(log, []);
  res(d, "verify_identity", { code: "1234" });
  const other = res(d, "lookup_order", { order_id: "O2" });
  assert.deepEqual(other, { content: "BLOCKED order_not_owned: That order does not belong to the verified customer.", is_error: true, blocked: "order_not_owned" });
  assert.ok(!other.content.includes("C2") && (d.state()?.orders ?? { O2: 1 }).O2 === undefined);
  assert.equal(res(d, "process_refund", { order_id: "O2", amount_cents: 100 }).blocked, "order_not_checked");
  assert.ok(!log.includes("process_refund"));
});

test("e3 a refund is checked against the order its amount and what is left", () => {
  const { d, log } = verified();
  assert.equal(res(d, "process_refund", { order_id: "O1", amount_cents: 100 }).blocked, "order_not_checked");
  res(d, "lookup_order", { order_id: "O1" });
  for (const bad of [0, -5, 12.5, "100", true, null]) assert.equal(res(d, "process_refund", { order_id: "O1", amount_cents: bad }).blocked, "bad_amount", String(bad));
  assert.equal(res(d, "process_refund", { order_id: "O1", amount_cents: 5001 }).blocked, "exceeds_order");
  assert.equal(res(d, "process_refund", { order_id: "O1", amount_cents: 3000 }).blocked, null);
  const out = res(d, "process_refund", { order_id: "O1", amount_cents: 2500 });
  assert.deepEqual([out.blocked, out.content], ["exceeds_order", "BLOCKED exceeds_order: The amount is more than what is left to refund on the order."]);
  assert.equal(res(d, "process_refund", { order_id: "O1", amount_cents: 2000 }).blocked, null);
  assert.equal(log.filter((c) => c === "process_refund").length, 2);
});

test("e4 a refund over the limit is not executed and becomes a structured hand off", () => {
  const { d, log } = verified({ limit: 10000 });
  res(d, "lookup_order", { order_id: "O9" });
  const out = res(d, "process_refund", { order_id: "O9", amount_cents: 25000 });
  assert.deepEqual([out.blocked, out.is_error, out.content], ["needs_human", true, "BLOCKED needs_human: Refunds over the limit need a person."]);
  assert.ok(!log.includes("process_refund"));
  assert.equal(res(d, "process_refund", { order_id: "O9", amount_cents: 10000 }).blocked, null);
  assert.equal(res(d, "process_refund", { order_id: "O9", amount_cents: 10001 }).blocked, "needs_human");
  assert.deepEqual(d.handoff("Customer asks for a 250.00 refund") ?? {}, {
    customer_id: "C1", identity_verified: true, reason: "Customer asks for a 250.00 refund", orders_checked: ["O9"],
    refunds_done: [{ order_id: "O9", amount_cents: 10000, refund_id: "R1" }],
    blocked: [{ tool: "process_refund", code: "needs_human" }, { tool: "process_refund", code: "needs_human" }], recommended_action: "review_refund" });
});

test("e5 a failed check does not unlock anything and three in a row lock the desk", () => {
  const { d } = desk();
  assert.equal(res(d, "verify_identity", { code: "0000" }).content, "verified=no");
  assert.equal(res(d, "lookup_order", { order_id: "O1" }).blocked, "identity_required");
  res(d, "verify_identity", { code: "1234" });
  res(d, "verify_identity", { code: "0000" });
  assert.equal((d.state() ?? {}).customer, null);
  assert.equal(res(d, "lookup_order", { order_id: "O1" }).blocked, "identity_required");
  res(d, "verify_identity", { code: "0000" });
  res(d, "verify_identity", { code: "0000" });
  const state = d.state() ?? {};
  assert.deepEqual([state.locked, state.failures], [true, 3]);
  assert.deepEqual(res(d, "verify_identity", { code: "1234" }), { content: "BLOCKED locked: Too many failed identity checks; escalate to a person.", is_error: true, blocked: "locked" });
  assert.equal(res(d, "escalate", { reason: "locked out" }).content, "ticket_id=T1");
  assert.equal((d.handoff("identity could not be verified") ?? {}).recommended_action, "verify_identity_manually");
});

test("e6 unknown tools and backend errors are reported and the hand off lists every block", () => {
  const { d } = verified({ breakRefund: true });
  res(d, "lookup_order", { order_id: "O1" });
  assert.deepEqual(res(d, "delete_everything"), { content: "BLOCKED unknown_tool: Unknown tool: delete_everything", is_error: true, blocked: "unknown_tool" });
  assert.deepEqual(res(d, "process_refund", { order_id: "O1", amount_cents: 500 }), { content: "payment service offline", is_error: true, blocked: null });
  const state = d.state() ?? {};
  assert.deepEqual(state.refunds, []);
  assert.equal(state.orders.O1.refunded_cents, 0);
  const handoff = d.handoff("the payment service is down") ?? {};
  assert.deepEqual(handoff.blocked, [{ tool: "delete_everything", code: "unknown_tool" }]);
  assert.deepEqual([handoff.refunds_done, handoff.recommended_action, handoff.orders_checked], [[], "review_case", ["O1"]]);
});
