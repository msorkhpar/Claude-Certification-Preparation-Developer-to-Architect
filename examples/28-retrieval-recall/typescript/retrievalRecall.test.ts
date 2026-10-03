import { test } from "node:test";
import assert from "node:assert/strict";
import { QUERIES, bm25Rank, buildAll, embed, firstRank, fuse, rankers, recall } from "./retrievalRecall.ts";

const meanRecall = (mode: string, k = 3) => {
  const chunks = buildAll();
  return QUERIES.reduce((sum, [q, rel]) => sum + recall(rankers(chunks, q)[mode], chunks, rel, k), 0) / QUERIES.length;
};

test("hybrid beats each single index at recall 3", () => {
  assert.deepEqual([meanRecall("bm25"), meanRecall("embedding"), meanRecall("hybrid")], [0.5, 0.75, 1.0]);
});

test("the embedding has 64 dimensions and length one", () => {
  const v = embed("deleting accounts");
  assert.ok(v.length === 64 && Math.abs(v.reduce((s, x) => s + x * x, 0) - 1) < 1e-9);
});

test("fusion puts a chunk found by both lists first", () => {
  assert.equal(fuse([["a", "b"], ["c", "b"]])[0], "b");
});

test("a query with no shared word has no lexical hit", () => {
  assert.deepEqual(bm25Rank(buildAll(), "zzzz qqqq"), []);
  assert.equal(firstRank(rankers(buildAll(), "deleting accounts").bm25, buildAll(), ["deletion"]), null);
});
