import { test } from "node:test";
import assert from "node:assert/strict";
import { DOCS, REPLIES, extract, validate } from "./extractionRun.ts";

test("a record that is fine is valid on the first attempt", () => {
  const result = extract("d1", DOCS.d1[1]);
  assert.deepEqual([result.status, result.attempts, result.retried], ["valid", 1, []]);
});

test("a semantic error and an invented vendor are retried once and then fixed", () => {
  assert.deepEqual(validate(REPLIES.d2[0], DOCS.d2[1]), [["semantic", "total"]]);
  assert.deepEqual(validate(REPLIES.d3[0], DOCS.d3[1]), [["ungrounded", "vendor"]]);
  for (const doc of ["d2", "d3"]) {
    const result = extract(doc, DOCS[doc][1]);
    assert.deepEqual([result.status, result.attempts], ["valid", 2]);
  }
});

test("an absent value is never retried and goes to review", () => {
  const result = extract("d4", DOCS.d4[1]);
  assert.deepEqual([result.status, result.attempts, result.errors], ["needs_review", 1, [["absent", "total"]]]);
});

test("a flagged conflict is information and goes to review without a retry", () => {
  const result = extract("d5", DOCS.d5[1]);
  assert.deepEqual(validate(result.record, DOCS.d5[1]), []);
  assert.deepEqual([result.status, result.attempts], ["needs_review", 1]);
});

test("an error that survives the retry fails the document after two attempts", () => {
  const result = extract("d6", DOCS.d6[1]);
  assert.deepEqual([result.status, result.attempts, result.retried], ["failed", 2, ["ungrounded"]]);
});
