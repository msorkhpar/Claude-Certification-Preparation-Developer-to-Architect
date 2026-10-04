import { test } from "node:test";
import assert from "node:assert/strict";
import { assemble, caseFactsBlock, render, shrink, stale, tokens } from "./caseFacts.ts";

test("shrinking keeps the fields the tool is used for with exact values", () => {
  const result = { order_id: "A-1", refund_amount: "$129.50", secret_hash: "zz", items: "kettle", purchase_date: "d", return_window: "30 days" };
  assert.equal(render(shrink("lookup_order", result)), "order_id=A-1;purchase_date=d;items=kettle;return_window=30 days;refund_amount=$129.50");
  assert.deepEqual(shrink("lookup_customer", { tier: "gold", x: "1" }), { tier: "gold" });
});

test("tokens round up by four characters", () => {
  assert.deepEqual([tokens(""), tokens("abcd"), tokens("abcde")], [0, 1, 2]);
});

test("the facts block carries the value and the day it was read", () => {
  assert.equal(caseFactsBlock([["refund_amount", "$129.50", 118]]), "## Case facts\nrefund_amount: $129.50 (as of day 118)");
});

test("the prompt puts key facts first and the question last", () => {
  const prompt = assemble("## Case facts\nx: 1 (as of day 1)", ["f1"], [["Doc", "text"]], "Q?");
  assert.deepEqual(prompt.split("\n").filter((l) => l.startsWith("#")), ["## Case facts", "## Key findings", "## Documents", "### Doc", "## Question"]);
  assert.ok(prompt.endsWith("Q?"));
});

test("a value older than the limit is read again", () => {
  assert.deepEqual([stale(118, 125, 3), stale(124, 125, 3), stale(122, 125, 3)], [true, false, false]);
});
