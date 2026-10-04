import { test } from "node:test";
import assert from "node:assert/strict";
import { DOC, accuracy, check, extract, requestChoice, scriptedValue } from "./extractionChecks.ts";

const WRONG = { items: [100.0, 20.5], total: 130.0, evidence: "Total due: 130.00 EUR" };
const RIGHT = { items: [100.0, 20.5, 9.5], total: 130.0, evidence: "Total due: 130.00 EUR" };

test("a required field gets filled and a nullable one stays null", () => {
  assert.equal(scriptedValue("no order here", false), "PO-0000");
  assert.equal(scriptedValue("no order here", true), null);
  assert.equal(scriptedValue("Order PO 4471 shipped", false), "4471");
});

test("the checks find a wrong sum and a quotation that is not in the document", () => {
  assert.deepEqual(check(RIGHT, DOC), []);
  assert.deepEqual(check(WRONG, DOC), ["total: the items add up to 120.5, not 130"]);
  assert.deepEqual(check({ ...RIGHT, evidence: "Total due: 130.00 USD" }, DOC), ["evidence: this quotation is not in the document"]);
});

test("a retry carries the document the failed answer and the problems", () => {
  const result = extract(DOC, [WRONG, RIGHT]);
  assert.ok(result.status === "valid" && result.attempts === 2 && result.feedback.length === 1);
  const text = result.feedback[0];
  assert.ok(text.includes(DOC) && text.includes('"total":130') && text.includes("- total: the items add up to 120.5, not 130"));
  assert.equal(extract(DOC, [WRONG, WRONG], 1).status, "failed");
  assert.equal(extract(DOC, [WRONG, RIGHT], 0).status, "failed");
});

test("accuracy on validated records alone hides the failures", () => {
  const outcomes: Array<[string, boolean]> = [...Array(5).fill(["valid", true]), ["valid", false], ...Array(4).fill(["failed", false])];
  assert.deepEqual(accuracy(outcomes), { validated_only: 0.83, all_documents: 0.5 });
  assert.deepEqual(accuracy([]), { validated_only: 0, all_documents: 0 });
});

test("forced choice is used where accepted and auto with a reply check elsewhere", () => {
  assert.deepEqual(requestChoice("claude-haiku-4-5", ["a", "b"]), { tool_choice: "any", check_reply: false });
  assert.deepEqual(requestChoice("claude-haiku-4-5", ["a"]), { tool_choice: "tool:a", check_reply: false });
  assert.deepEqual(requestChoice("claude-opus-5-5", ["a", "b"]), { tool_choice: "auto", check_reply: true });
});
