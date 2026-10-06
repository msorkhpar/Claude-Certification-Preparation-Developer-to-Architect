import { test } from "node:test";
import assert from "node:assert/strict";
import { resolve } from "node:path";
import { pathToFileURL } from "node:url";

// The solution is the file beside this test.
const dir = resolve(import.meta.dirname);
const { chooseRetrieval, chunkSections, docVersion, recallAtK, reindex, search, stale } = await import(pathToFileURL(resolve(dir, "pipeline.ts")).href);

function cs(docId: string, text: string, maxWords?: number): any[] {
  const result = maxWords === undefined ? chunkSections(docId, text) : chunkSections(docId, text, maxWords);
  assert.ok(Array.isArray(result), "no chunks returned");
  return result;
}

const PLAN = "# Annual plan\n## Cancellation\nYou can cancel within 14 days for a full refund.";
const MONTHLY = "# Monthly plan\n## Cancellation\nYou can cancel at any time.";
const ERRORS = "# Error codes\n## E-7310\nThe warehouse could not reserve stock.\n## E-4021\nThe payment gateway rejected the card.";
const LONG = "# Guide\n## Setup\nInstall the agent. Configure the proxy. Restart the service. Check the logs for errors.";

const texts = (chunks: any) => {
  assert.ok(Array.isArray(chunks), "no chunks returned");
  return chunks.map((c: any) => [c.id, c.text]);
};
const ids = (chunks: any) => chunks.map((c: any) => c.id);

test("m1 sections become chunks that carry their title and section and the version of their document", () => {
  const chunks = cs("annual", PLAN);
  assert.deepEqual(texts(chunks), [["annual/Cancellation", "Annual plan > Cancellation. You can cancel within 14 days for a full refund."]]);
  assert.equal(chunks[0].doc, "annual");
  assert.equal(chunks[0].version, docVersion(PLAN));
  assert.deepEqual(ids(cs("errors", ERRORS)), ["errors/E-7310", "errors/E-4021"]);
});

test("e1 a long section splits at sentence ends under the word limit and every part keeps the prefix", () => {
  assert.deepEqual(texts(cs("guide", LONG, 8)), [
    ["guide/Setup#1", "Guide > Setup. Install the agent. Configure the proxy."],
    ["guide/Setup#2", "Guide > Setup. Restart the service. Check the logs for errors."],
  ]);
  assert.equal(cs("guide", LONG, 3).length, 4, "a sentence longer than the limit stays whole");
});

test("e2 a code outweighs a word and a word in no chunk matches nothing", () => {
  const chunks = [...cs("errors", ERRORS), ...cs("annual", PLAN)];
  assert.deepEqual(search(chunks, "what does E-7310 mean"), ["errors/E-7310"]);
  assert.deepEqual(search(chunks, "cancel E-4021 card"), ["errors/E-4021", "annual/Cancellation"]);
  assert.deepEqual(search(chunks, "card gateway E-7310"), ["errors/E-7310", "errors/E-4021"], "a code outweighs two plain words");
  assert.deepEqual(search(chunks, "when will I be reimbursed"), []);
  assert.deepEqual(search(chunks, "cancel", 1), ["annual/Cancellation"]);
});

test("e3 a search for a reader never returns a chunk of a document that reader may not see", () => {
  const chunks = [...cs("monthly", MONTHLY), ...cs("annual", PLAN)];
  assert.deepEqual(search(chunks, "cancel annual plan", 1), ["annual/Cancellation"]);
  assert.deepEqual(search(chunks, "cancel annual plan", 1, new Set(["monthly"])), ["monthly/Cancellation"]);
  assert.deepEqual(search(chunks, "cancel annual plan", 3, new Set(["monthly"])), ["monthly/Cancellation"]);
  assert.deepEqual(search(chunks, "cancel", 3, new Set()), []);
});

test("e4 the mechanism follows the corpus size then the data shape then the query pattern", () => {
  assert.equal(chooseRetrieval(50000, "text", "identifier"), "cached prompt");
  assert.equal(chooseRetrieval(199999, "table", "multi-hop"), "cached prompt");
  assert.equal(chooseRetrieval(200000, "text", "paraphrase"), "embedding index");
  assert.equal(chooseRetrieval(2000000, "table", "paraphrase"), "structured query");
  assert.equal(chooseRetrieval(2000000, "table", "multi-hop"), "structured query");
  assert.equal(chooseRetrieval(2000000, "text", "multi-hop"), "agentic search");
  assert.equal(chooseRetrieval(2000000, "text", "identifier"), "keyword index");
  assert.equal(chooseRetrieval(2000000, "text", "mixed"), "hybrid index");
});

test("e5 a reindex keeps unchanged documents replaces changed ones adds new ones and drops removed ones", () => {
  const old = [...cs("annual", PLAN), ...cs("monthly", MONTHLY), ...cs("errors", ERRORS)];
  const docs = { annual: PLAN.replace("14 days", "30 days"), errors: ERRORS, guide: LONG };
  const result = reindex(old, docs);
  assert.ok(Array.isArray(result), "reindex returned nothing");
  const [fresh, report] = result;
  assert.deepEqual(report, { added: ["guide"], replaced: ["annual"], removed: ["monthly"], kept: ["errors"] });
  assert.deepEqual(ids(fresh), ["annual/Cancellation", "errors/E-7310", "errors/E-4021", "guide/Setup"]);
  assert.deepEqual(fresh.filter((c: any) => c.doc === "annual").map((c: any) => c.text), ["Annual plan > Cancellation. You can cancel within 30 days for a full refund."]);
  assert.ok(fresh.every((c: any) => c.doc !== "monthly"));
});

test("e6 stale lists the chunks whose document changed or vanished", () => {
  const old = [...cs("annual", PLAN), ...cs("monthly", MONTHLY), ...cs("errors", ERRORS)];
  const docs = { annual: PLAN.replace("14 days", "30 days"), errors: ERRORS };
  assert.deepEqual(stale(old, docs), ["annual/Cancellation", "monthly/Cancellation"]);
  const result = reindex(old, docs);
  assert.ok(Array.isArray(result), "reindex returned nothing");
  assert.deepEqual(stale(result[0], docs), []);
  assert.deepEqual(stale(old, { annual: PLAN, monthly: MONTHLY, errors: ERRORS }), []);
});

test("e7 recall counts every labelled question and a question with no results is a miss", () => {
  const relevant = { q1: "a", q2: "b", q3: "c" };
  const results = { q1: ["a", "x"], q2: ["x", "y", "b"] };
  assert.equal(recallAtK(results, relevant, 3), 0.67);
  assert.equal(recallAtK(results, relevant, 1), 0.33);
  assert.equal(recallAtK({}, relevant, 3), 0);
  assert.equal(recallAtK(results, {}, 3), 0);
});
