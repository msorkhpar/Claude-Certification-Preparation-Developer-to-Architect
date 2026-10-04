import { test } from "node:test";
import assert from "node:assert/strict";
import { Desk, LIMIT, STALL, run, type Call } from "./supportDesk.ts";

const BEN: Call = ["get_customer", { query: "ben@example.com" }];
const lookup = (id: string): Call => ["lookup_order", { order_id: id }];

test("nothing runs before the customer is identified and the backend sees no call", () => {
  const desk = new Desk();
  const result = desk.call("lookup_order", { order_id: "O1" });
  assert.equal(result.code, "identity_required");
  assert.ok(result.message!.includes("get_customer"));
  assert.deepEqual(desk.backend, []);
});

test("two matching customers are a question and never a guess", () => {
  const [desk, outcome] = run([["get_customer", { query: "Ana Silva" }]]);
  assert.equal(desk.customer, null);
  assert.equal(outcome, "asked");
  assert.deepEqual(desk.refused, ["ambiguous_match"]);
});

test("an order of another customer is refused without naming its owner", () => {
  const desk = new Desk();
  desk.call("get_customer", { query: "ana@example.com" });
  const result = desk.call("lookup_order", { order_id: "O1" });
  assert.equal(result.code, "order_not_owned");
  assert.ok(!result.message!.includes("C3"));
  assert.deepEqual(desk.checked, []);
});

test("a refund at the limit runs and one cent above needs a person", () => {
  const desk = new Desk();
  desk.call(...BEN);
  desk.flaky.clear();
  desk.call(...lookup("O2"));
  assert.ok(desk.call("process_refund", { order_id: "O2", amount_cents: LIMIT }).ok);
  assert.equal(desk.call("process_refund", { order_id: "O2", amount_cents: LIMIT + 1 }).code, "needs_human");
  assert.deepEqual(desk.refunds, ["O2:10000"]);
});

test("a transient fault is retried once and a permanent one is not", () => {
  const [desk, outcome] = run([BEN, lookup("O2")]);
  assert.equal(desk.retries, 1);
  assert.deepEqual(desk.checked, ["O2"]);
  assert.equal(outcome, "resolved");
  assert.equal(run([BEN, lookup("O9")])[0].retries, 0);
});

test("the same call three times in a row escalates and different calls do not", () => {
  const [desk, outcome] = run([BEN, ...Array.from({ length: STALL }, () => lookup("O9"))]);
  assert.equal(outcome, "escalated");
  assert.equal(desk.escalation!.trigger, "stalled");
  assert.equal(run([BEN, lookup("O9"), lookup("O8"), lookup("O9")])[1], "resolved");
});

test("a person can always be reached and the record comes from the desks state", () => {
  const [first, outcome] = run([["escalate_to_human", { trigger: "customer_request", reason: "wants a person" }]]);
  assert.equal(outcome, "escalated");
  assert.equal(first.escalation!.verified, false);
  const [desk] = run([BEN, lookup("O2"), ["process_refund", { order_id: "O2", amount_cents: 25000 }], ["escalate_to_human", { trigger: "needs_human", reason: "x" }]]);
  const e = desk.escalation!;
  assert.deepEqual([e.customer, e.orders, e.refunds, e.refused], ["C3", ["O2"], [], ["needs_human"]]);
});
