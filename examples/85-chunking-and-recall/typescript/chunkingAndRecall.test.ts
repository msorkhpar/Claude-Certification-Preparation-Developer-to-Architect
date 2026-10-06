import { test } from "node:test";
import assert from "node:assert/strict";
import { DOCS, chooseRetrieval, chunkFixed, chunkSections, fuse, holds, index, layer, lexical, reindexAdditive, reindexReplace, stale } from "./chunkingAndRecall.ts";

const RULE = "Items marked final sale cannot be returned, except when they arrive damaged.";

test("a fixed cut splits the rule from its exception and a heading cut keeps them together", () => {
  const fixed = chunkFixed("refunds", DOCS.refunds, 12);
  assert.equal(fixed.length, 4);
  assert.equal(holds(fixed, fixed.map((c) => c[0]), RULE), false);
  const sections = chunkSections("refunds", DOCS.refunds, false);
  assert.deepEqual(sections.map((c) => c[0]), ["refunds/Eligibility", "refunds/Exceptions", "refunds/Process"]);
  assert.equal(holds(sections, ["refunds/Exceptions"], RULE), true);
});

test("a chunk that carries its title and section can be told from its twin", () => {
  assert.deepEqual(lexical(index(DOCS, false), "cancel the annual plan", 1), ["monthly/Cancellation"]);
  assert.deepEqual(lexical(index(DOCS, true), "cancel the annual plan", 1), ["annual/Cancellation"]);
  assert.ok(chunkSections("annual", DOCS.annual, true)[0][1].startsWith("Annual plan > Cancellation. You can cancel"));
});

test("a code outweighs a word and the fusion rewards agreement", () => {
  const chunks = index(DOCS, true);
  assert.deepEqual(lexical(chunks, "what does E-7310 mean"), ["errors/E-7310"]);
  assert.deepEqual(lexical(chunks, "when will I be reimbursed"), []);
  assert.deepEqual(fuse([["a", "b"], ["b", "c"]]), ["b", "a", "c"]);
  assert.deepEqual(fuse([[], ["x"]]), ["x"]);
});

test("adding chunks without removing the old ones leaves a stale answer in the index", () => {
  const chunks = index(DOCS, true);
  const edited = DOCS.refunds.replace("within 30 days", "within 60 days");
  const live = { ...DOCS, refunds: edited };
  assert.deepEqual(stale(chunks, live), ["refunds/Eligibility"]);
  assert.deepEqual(stale(reindexAdditive(chunks, "refunds", edited), live), ["refunds/Eligibility"]);
  const replaced = reindexReplace(chunks, "refunds", edited);
  assert.deepEqual(stale(replaced, live), []);
  assert.equal(replaced.length, chunks.length);
});

test("the mechanism follows the size of the corpus then the shape of the data then the query", () => {
  const rows: Array<[number, string, string]> = [[199999, "table", "multi-hop"], [200000, "table", "identifier"], [5000000, "text", "multi-hop"], [5000000, "text", "identifier"], [5000000, "text", "paraphrase"], [5000000, "text", "mixed"]];
  assert.deepEqual(rows.map(([n, s, p]) => chooseRetrieval(n, s, p)), ["cached prompt", "structured query", "agentic search", "keyword index", "embedding index", "hybrid index"]);
});

test("retrieval and generation are judged apart", () => {
  const cases: Array<[boolean, boolean]> = [[true, true], [true, false], [false, false], [false, true]];
  assert.deepEqual(cases.map(([r, c]) => layer(r, c)), ["ok", "generation", "retrieval", "unsupported"]);
});
