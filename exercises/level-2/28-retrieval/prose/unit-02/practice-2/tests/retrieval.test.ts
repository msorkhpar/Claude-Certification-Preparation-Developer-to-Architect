import { test } from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { pathToFileURL } from "node:url";
import { resolve } from "node:path";

// The solution is the file beside this test.
const dir = resolve(import.meta.dirname);
const { bm25Rank, buildChunks, chunk, embed, evaluate, fuse, recallAtK, rerank, retrieve } = await import(pathToFileURL(resolve(dir, "retrieval.ts")).href);

const FIXTURE = JSON.parse(readFileSync(new URL("./fixture.json", import.meta.url), "utf8"));

const c = (id: string, text: string) => ({ id, doc: id.split("#")[0], text });

/** "RangeError" when fn throws it, "crash" for another error, null when it returns. */
function failureOf(fn: () => unknown): string | null {
  try {
    fn();
  } catch (err) {
    return err instanceof RangeError ? "RangeError" : "crash";
  }
  return null;
}
const close = (a: unknown, b: number) => typeof a === "number" && Math.abs(a - b) < 1e-9;

test("m1 hybrid search finds what each single index misses", () => {
  const chunks = buildChunks(FIXTURE.corpus, FIXTURE.chunk_size, FIXTURE.overlap) ?? [];
  const lexical = evaluate(chunks, FIXTURE.queries, "bm25", 3);
  const semantic = evaluate(chunks, FIXTURE.queries, "embedding", 3);
  const hybrid = evaluate(chunks, FIXTURE.queries, "hybrid", 3);
  assert.deepEqual([close(lexical, 0.5), close(semantic, 0.75), close(hybrid, 1.0)], [true, true, true], JSON.stringify([lexical, semantic, hybrid]));
});

test("e1 chunks overlap and end at the last word", () => {
  const words = Array.from({ length: 10 }, (_, i) => `w${i + 1}`).join(" ");
  assert.deepEqual(chunk(words, 4, 1), ["w1 w2 w3 w4", "w4 w5 w6 w7", "w7 w8 w9 w10"]);
  assert.deepEqual(chunk(words, 4, 0), ["w1 w2 w3 w4", "w5 w6 w7 w8", "w9 w10"]);
  assert.deepEqual(chunk("a b c d e f g", 4, 1), ["a b c d", "d e f g"]);
  assert.deepEqual(chunk("a b c", 5, 2), ["a b c"]);
  assert.deepEqual(chunk("a b c d", 4, 1), ["a b c d"]);
  assert.deepEqual([chunk("", 3, 1), chunk("   ", 3, 1)], [[], []]);
  assert.deepEqual([failureOf(() => chunk("a b", 2, 2)), failureOf(() => chunk("a b", 0, 0)), failureOf(() => chunk("a b", 2, -1))], ["RangeError", "RangeError", "RangeError"]);
  const made = buildChunks([{ id: "doc", text: "a b c d e" }, { id: "other", text: "x y" }], 3, 1) ?? [];
  assert.deepEqual(made, [{ id: "doc#0", doc: "doc", text: "a b c" }, { id: "doc#1", doc: "doc", text: "c d e" }, { id: "other#0", doc: "other", text: "x y" }]);
});

test("e2 a rare word outweighs common ones and short chunks win", () => {
  const chunks = [c("x#0", "alpha beta"), c("d1#0", "alpha beta delta"), c("d2#0", "alpha beta epsilon"), c("d3#0", "alpha beta zeta"), c("d4#0", "alpha beta eta"), c("y#0", "gamma theta")];
  assert.deepEqual(bm25Rank(chunks, "alpha beta gamma"), ["y#0", "x#0", "d1#0", "d2#0", "d3#0", "d4#0"]);
  const shorter = [c("long#0", "needle alpha beta gamma delta"), c("short#0", "needle alpha"), c("none#0", "other words")];
  assert.deepEqual(bm25Rank(shorter, "needle"), ["short#0", "long#0"]);
  assert.deepEqual(bm25Rank(shorter, "absent terms"), []);
  assert.deepEqual(bm25Rank(shorter, "Needle NEEDLE"), ["short#0", "long#0"]);
  assert.equal((bm25Rank(shorter, "needle", { "none#0": "needle needle needle" }) ?? [])[0], "none#0");
});

test("e3 fusion rewards agreement and breaks ties by id", () => {
  assert.deepEqual(fuse([["x", "y", "z"], ["z", "x", "w"]]), ["x", "z", "y", "w"]);
  assert.deepEqual(fuse([["p", "q"], ["r", "q"]]), ["q", "p", "r"]);
  assert.deepEqual(fuse([["b"], ["a"]]), ["a", "b"]);
  assert.deepEqual(fuse([["x", "x", "y"]]), ["x", "y"]);
  assert.deepEqual(fuse([["a", "b"], ["b", "c"]], 1), ["b", "a", "c"]);
  assert.deepEqual(fuse([]), []);
});

test("e4 reranking orders the pool by the scorer", () => {
  const texts = { a: "x", b: "y", c: "z", d: "w" };
  const table: Record<string, number> = { x: 0.2, y: 0.9, z: 0.9, w: 1.0 };
  const scorer = (_q: string, t: string) => table[t];
  assert.deepEqual(rerank("q", ["a", "b", "c"], texts, scorer, 2), ["b", "c"]);
  assert.deepEqual(rerank("q", ["a", "b", "c"], texts, scorer), ["b", "c", "a"]);
  const chunks = [c("c1#0", "alpha"), c("c2#0", "alpha one"), c("c3#0", "alpha one two"), c("c4#0", "alpha one two three")];
  const preferLast: Record<string, number> = { "alpha one two three": 1.0, "alpha one two": 0.5 };
  const byTable = (_q: string, t: string) => preferLast[t] ?? 0;
  assert.deepEqual(bm25Rank(chunks, "alpha"), ["c1#0", "c2#0", "c3#0", "c4#0"]);
  assert.deepEqual(retrieve(chunks, "alpha", "bm25", 1, { pool: 2, scorer: byTable }), ["c1#0"]);
  assert.deepEqual(retrieve(chunks, "alpha", "bm25", 1, { pool: 4, scorer: byTable }), ["c4#0"]);
  assert.deepEqual(retrieve(chunks, "alpha", "bm25", 2), ["c1#0", "c2#0"]);
});

test("e5 recall counts documents not chunks", () => {
  const docOf = { "a#0": "A", "a#1": "A", "b#0": "B", "c#0": "C" };
  const ids = ["a#0", "a#1", "b#0", "c#0"];
  assert.ok(close(recallAtK(ids, docOf, ["A", "B"], 2), 0.5));
  assert.ok(close(recallAtK(ids, docOf, ["A", "B"], 3), 1.0));
  assert.ok(close(recallAtK(ids, docOf, ["A", "Z"], 4), 0.5));
  assert.ok(close(recallAtK(ids, docOf, ["B"], 0), 0.0));
  assert.ok(close(recallAtK(ids, docOf, ["A"], 10), 1.0));
  assert.equal(failureOf(() => recallAtK(ids, docOf, [], 3)), "RangeError");
});

test("e6 a context sentence makes a bare chunk findable", () => {
  const chunks = [c("a#0", "The limit is 30 days after delivery."), c("b#0", "The limit is 5 users per workspace."), c("c#0", "Our mascot is a friendly otter.")];
  const contexts = { "a#0": "Returns policy: the return window for physical orders." };
  assert.deepEqual(retrieve(chunks, "return window", "bm25", 2), []);
  assert.deepEqual(retrieve(chunks, "return window", "bm25", 2, { contexts }), ["a#0"]);
  const queries = [{ query: "return window", relevant: ["a"] }];
  assert.ok(close(evaluate(chunks, queries, "bm25", 1), 0.0));
  assert.ok(close(evaluate(chunks, queries, "bm25", 1, { contexts }), 1.0));
  assert.equal(chunks[0].text, "The limit is 30 days after delivery.");
  assert.equal(embed("x").length, 64);
});
