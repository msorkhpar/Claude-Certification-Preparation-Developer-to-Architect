import { test } from "node:test";
import assert from "node:assert/strict";
import { CASES, askForIdentifier, byCriteria, bySentiment, errors, escalationSection, pickMostRecent } from "./escalationRules.ts";

test("the criteria route every case as a careful person would and the sentiment rule does not", () => {
  assert.deepEqual(errors(byCriteria), []);
  assert.deepEqual(errors(bySentiment), [1, 2, 3, 4, 5]);
  assert.equal(CASES.length, 6);
});

test("the recency heuristic picks one account and the question picks none", () => {
  const matches = [{ id: "c1", last_order: 1 }, { id: "c2", last_order: 2 }];
  assert.equal(pickMostRecent(matches), "c2");
  assert.equal(askForIdentifier(matches, ["the email", "the postcode"]), "I found 2 accounts for that name. Please give me one of: the email, the postcode.");
});

test("the prompt section lists the criteria and the examples with their reasons", () => {
  assert.equal(escalationSection(["a request for a person"], [["Hi", "resolve", "simple"]]), 'Escalate to a person when:\n- a request for a person\n\nExamples:\nCustomer: "Hi" -> resolve (simple)');
});
