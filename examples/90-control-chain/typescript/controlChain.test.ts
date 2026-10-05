import { test } from "node:test";
import assert from "node:assert/strict";
import { auditRecord, erase, route } from "./controlChain.ts";

const SOURCE = "Water damage is covered up to 5,000 per claim.";
const ok = { text: "ok", confidence: 99, quote: SOURCE };
const reply = { name: "draft_reply", consequence: "low" };
const refund = { name: "issue_refund", consequence: "high" };

test("a down screen holds a high consequence action and flags a low one", () => {
  assert.equal(route(refund, ok, SOURCE, false), "hold: screen down");
  assert.equal(route(reply, ok, SOURCE, false), "auto (unscreened)");
  assert.equal(route(reply, ok, SOURCE, true), "auto");
});

test("a confident answer that the source does not support is held", () => {
  const wrong = { text: "ok", confidence: 100, quote: "Water damage is covered up to 8,000 per claim." };
  assert.equal(route(reply, wrong, SOURCE, true), "hold: unsupported");
  assert.equal(route(refund, wrong, SOURCE, true), "hold: unsupported");
});

test("confidence exactly at the threshold goes out and one below is reviewed", () => {
  assert.equal(route(reply, { ...ok, confidence: 95 }, SOURCE, true), "auto");
  assert.equal(route(reply, { ...ok, confidence: 94 }, SOURCE, true), "review");
});

test("a high consequence action always reaches a person", () => {
  assert.equal(route(refund, { ...ok, confidence: 100 }, SOURCE, true), "human");
});

test("the audit record holds no content and erasure unlinks only the person", () => {
  const record = auditRecord("r-1", refund, "human", "secret text");
  assert.equal(record.chars, 11);
  assert.equal(record.content_stored, false);
  assert.ok(!JSON.stringify(record).includes("secret text"));
  const [kept, removed] = erase({ "<A>": "p1", "<B>": "p2", "<C>": "p1" }, "p1");
  assert.deepEqual(kept, { "<B>": "p2" });
  assert.equal(removed, 2);
});
