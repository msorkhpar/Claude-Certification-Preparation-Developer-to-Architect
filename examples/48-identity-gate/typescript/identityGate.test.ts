import { test } from "node:test";
import assert from "node:assert/strict";
import { Backend, clientFor, gate, replies, run } from "./identityGate.ts";

test("the gate lets only verification through before a customer is verified", () => {
  const backend = new Backend();
  assert.deepEqual(gate(backend, "process_refund", { order_id: "O1", amount_cents: 1 }), ["BLOCKED identity_required: Verify the customer's identity before this action.", true]);
  assert.equal(gate(backend, "lookup_order", { order_id: "O1" })[1], true);
  assert.deepEqual(backend.log, []);
  assert.deepEqual(gate(backend, "verify_identity", { code: "1234" }), ["verified=yes", false]);
  assert.deepEqual(gate(backend, "lookup_order", { order_id: "O1" }), ["order_id=O1; total_cents=5000", false]);
});

test("a wrong code unlocks nothing", () => {
  const backend = new Backend();
  gate(backend, "verify_identity", { code: "0000" });
  assert.equal(gate(backend, "process_refund", { order_id: "O1", amount_cents: 1 })[1], true);
  assert.deepEqual(backend.log, ["verify_identity"]);
});

test("the ungated loop refunds before any verification and the gated one does not", async () => {
  const plain = new Backend();
  await run(clientFor(replies(false)).client, plain, false);
  assert.deepEqual(plain.log, ["process_refund"]);
  const gated = new Backend();
  await run(clientFor(replies(true)).client, gated, true);
  assert.deepEqual(gated.log, ["verify_identity", "lookup_order", "process_refund"]);
});
