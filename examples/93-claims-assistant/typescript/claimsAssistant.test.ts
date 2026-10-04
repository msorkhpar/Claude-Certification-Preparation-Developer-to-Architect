import { test } from "node:test";
import assert from "node:assert/strict";
import { INDEX, STALE_INDEX, handle, release, retrieve, tokenise } from "./claimsAssistant.ts";

const WATER = "Water damage is covered up to 5,000 per claim.";
const request = (text: string, o: { allowed?: string[]; consequence?: string; quote?: string; confidence?: number } = {}) =>
  ({ id: "r", text, allowed: new Set(o.allowed ?? ["policy"]), consequence: o.consequence ?? "low", quote: o.quote ?? WATER, confidence: o.confidence ?? 97 });

test("identifiers become tokens and the same value gets the same token", () => {
  const [sent, vault] = tokenise("write to a@example.com or a@example.com or b@example.org");
  assert.equal(sent, "write to <EMAIL_1> or <EMAIL_1> or <EMAIL_2>");
  assert.equal(vault["<EMAIL_1>"], "a@example.com");
  assert.ok(!sent.includes("@"));
});

test("the readers rights come before the ranking", () => {
  assert.equal(retrieve("partner commission premiums", new Set(["contracts"]), INDEX)?.id, "contract-9");
  assert.equal(retrieve("partner commission premiums", new Set(["policy"]), INDEX), null);
});

test("a tie goes to the smaller id and no overlap is no evidence", () => {
  assert.equal(retrieve("water damage", new Set(["policy"]), STALE_INDEX)?.id, "policy-2-old");
  assert.equal(retrieve("water damage", new Set(["policy"]), INDEX)?.id, "policy-2");
  assert.equal(retrieve("zzzz yyyy", new Set(["policy"]), INDEX), null);
});

test("a stale or unsupported answer is held and confidence decides the rest", () => {
  const q = "How much does the policy cover for water damage?";
  assert.equal(handle(request(q), STALE_INDEX)[1].outcome, "hold: stale evidence (policy-2-old v2, current v3)");
  assert.equal(handle(request(q, { quote: "Water damage is covered up to 8,000 per claim." }), INDEX)[1].outcome, "hold: unsupported");
  assert.equal(handle(request(q, { confidence: 95 }), INDEX)[1].outcome, "auto");
  assert.equal(handle(request(q, { confidence: 94 }), INDEX)[1].outcome, "review");
  assert.equal(handle(request(q, { consequence: "high" }), INDEX)[1].outcome, "human");
});

test("the trace holds ids and sizes and no text", () => {
  const [sent, trace] = handle(request("Claims reported from jo@example.com, how many days?", { quote: "x" }), INDEX);
  assert.ok(!JSON.stringify(trace).includes("jo@example.com") && !sent.includes("@"));
  assert.deepEqual(Object.keys(trace).sort(), ["chars", "chunk", "outcome", "request"]);
});

test("a gate protects the costly segment even when gains cover the losses", () => {
  const cases = [{ id: "a", segment: "refund", oldOk: true, newOk: false }, { id: "b", segment: "status", oldOk: false, newOk: true }];
  assert.equal(release(cases, new Set(["refund"])), "no-go: protected segment lost answers: refund");
  assert.equal(release(cases, new Set()), "go: lost 1, gained 1");
  assert.equal(release([{ id: "a", segment: "x", oldOk: true, newOk: false }], new Set()), "no-go: net loss: lost 1, gained 0");
});
