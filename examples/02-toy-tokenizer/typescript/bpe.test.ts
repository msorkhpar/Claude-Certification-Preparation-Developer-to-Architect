import { test } from "node:test";
import assert from "node:assert/strict";
import { encode, train } from "./bpe.ts";

const CORPUS = "low low low lower lower lowest newest newest widest widest";

test("a frequent word becomes one token and a rare one splits", () => {
  const rules = train(CORPUS, 6);
  assert.deepEqual(encode("low", rules), ["low"]);
  assert.ok(encode("lowish", rules).length > 1);
});

test("an unseen word still encodes with known pieces", () => {
  assert.equal(encode("newer", train(CORPUS, 6)).join(""), "newer");
});

test("no merges means one token per character", () => {
  assert.deepEqual(encode("low", train(CORPUS, 0)), ["l", "o", "w"]);
});
