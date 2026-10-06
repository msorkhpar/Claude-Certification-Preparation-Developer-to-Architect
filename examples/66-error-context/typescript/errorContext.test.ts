import { test } from "node:test";
import assert from "node:assert/strict";
import { OUTCOMES, generic, structured, suppress, terminate } from "./errorContext.ts";

test("the generic status loses the partial result and the cause", () => {
  assert.equal(generic(OUTCOMES), "found n1, n2, b1; sources unavailable: papers, filings");
});

test("silent suppression reports a failed source as a search that found nothing", () => {
  assert.equal(suppress(OUTCOMES), "found n1, n2, b1; nothing found in: papers, patents, filings");
});

test("aborting on the first failure loses every later source", () => {
  assert.equal(terminate(OUTCOMES), "aborted at papers; found n1, n2");
  assert.equal(terminate({ a: ["ok", ["x"]] }), "found x");
});

test("structured context keeps the partial result the empty answer and the way forward", () => {
  assert.equal(structured(OUTCOMES), "well supported: news, blogs; partial: papers (timeout, kept p1); no findings: patents; gaps: filings (permission, try: request access)");
});
