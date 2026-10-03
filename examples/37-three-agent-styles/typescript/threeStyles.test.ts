import { test } from "node:test";
import assert from "node:assert/strict";
import { TICKET, runAgent, runGraph, runTyped, scripted, validate } from "./threeStyles.ts";

test("the graph follows its edges and other topics skip the lookup", () => {
  const m = scripted(["Billing", "reply"]);
  const result = runGraph(m, TICKET);
  assert.deepEqual(result.path, ["classify", "lookup", "draft"]);
  assert.equal(result.state.invoice, "paid twice on 2026-09-30");
  assert.equal(m.seen.length, 2);
  const other = runGraph(scripted(["other", "hello"]), "How do I log in?");
  assert.deepEqual(other.path, ["classify", "draft"]);
  assert.ok(!("invoice" in other.state));
});

test("a graph resumes from a checkpoint without repeating earlier nodes", () => {
  const first = runGraph(scripted(["billing", "the reply"]), TICKET);
  const again = scripted(["the reply"]);
  const resumed = runGraph(again, TICKET, 2, [...first.checkpoints]);
  assert.deepEqual(resumed.path, ["draft"]);
  assert.equal(again.seen.length, 1);
  assert.deepEqual(resumed.state, first.state);
});

test("the agent loop runs the tool the model picked and stops at the step limit", () => {
  const m = scripted(['{"tool": "lookup_invoice", "arg": "1042"}', '{"final": "done"}']);
  assert.deepEqual(runAgent(m, TICKET), { reply: "done", trace: ["lookup_invoice(1042) -> paid twice on 2026-09-30"] });
  const endless = scripted(Array(4).fill('{"tool": "lookup_invoice", "arg": "x"}'));
  const result = runAgent(endless, TICKET, 4);
  assert.equal(result.reply, null);
  assert.equal(result.stopped, "max_steps");
  assert.equal(endless.seen.length, 4);
});

test("a typed reply is validated and a mismatch is fed back once", () => {
  assert.deepEqual(validate('{"topic": "x", "refund_cents": 5}'), [{ topic: "x", refund_cents: 5 }, null]);
  assert.equal(validate('{"topic": "x", "refund_cents": true}')[1], "field refund_cents must be int");
  assert.equal(validate("nope")[1], "the reply is not JSON");
  const m = scripted(['{"topic": "billing", "refund_cents": "49"}', '{"topic": "billing", "refund_cents": 49}']);
  assert.deepEqual(runTyped(m, TICKET), { data: { topic: "billing", refund_cents: 49 }, attempts: 2 });
  assert.match(m.seen[1], /field refund_cents must be int/);
  assert.equal(runTyped(scripted(["a", "b"]), TICKET).attempts, 2);
});
