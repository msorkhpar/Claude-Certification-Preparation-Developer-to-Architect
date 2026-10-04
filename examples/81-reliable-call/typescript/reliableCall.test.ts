import { test } from "node:test";
import assert from "node:assert/strict";
import { Breaker, Ledger, Transient, refund, retry } from "./reliableCall.ts";

test("a retry without a key pays twice", () => {
  const ledger = new Ledger();
  retry((n) => refund(ledger, null, "o1", 10, n === 1), 2);
  assert.equal(ledger.paid.length, 2);
});

test("a retry with the same key pays once and returns the first receipt", () => {
  const ledger = new Ledger();
  const [receipt, calls] = retry((n) => refund(ledger, "k", "o1", 10, n === 1), 2);
  assert.deepEqual([receipt, calls, ledger.paid.length], ["refund-1", 2, 1]);
});

test("a new key is a new refund", () => {
  const ledger = new Ledger();
  refund(ledger, "a", "o1", 10, false);
  refund(ledger, "b", "o1", 10, false);
  assert.equal(ledger.paid.length, 2);
});

test("retry gives up after the last try", () => {
  assert.throws(() => retry(() => { throw new Transient("down"); }, 3), Transient);
});

test("the breaker opens after the threshold and probes after the cooldown", () => {
  const b = new Breaker(3, 30);
  for (const t of [0, 1, 2]) b.record(false, t);
  assert.ok(b.state(3) === "open" && !b.allow(3));
  assert.ok(b.state(32) === "half-open" && b.allow(32));
  b.record(false, 32);
  assert.equal(b.state(33), "open");
  b.record(true, 70);
  assert.equal(b.state(71), "closed");
});
