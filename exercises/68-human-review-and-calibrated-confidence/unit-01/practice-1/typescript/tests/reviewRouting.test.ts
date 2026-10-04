import { test } from "node:test";
import assert from "node:assert/strict";
import { resolve } from "node:path";
import { pathToFileURL } from "node:url";

// SOLUTION_DIR selects starter, reference or a planted wrong solution.
const dir = resolve(process.env.SOLUTION_DIR ?? "starter");
const solution = await import(pathToFileURL(resolve(dir, "reviewRouting.ts")).href);
const got = (fn: (...args: any[]) => any) => (...args: any[]) => {
  const value = fn(...args);
  assert.ok(value !== null && value !== undefined, `${fn.name} returned nothing`);
  return value;
};
const [accuracyBy, canAutomate, stratifiedSample, route, checkpoint] = [solution.accuracyBy, solution.canAutomate, solution.stratifiedSample, solution.route, solution.checkpoint].map(got);
const calibrateThreshold = solution.calibrateThreshold;

const recs = (doc_type: string, field: string, right: number, wrong: number) => [
  ...Array(right).fill({ doc_type, field, correct: true }),
  ...Array(wrong).fill({ doc_type, field, correct: false }),
];
const seg = (rows: any[], name: string) => rows.find((r) => r.segment === name);

test("m1 accuracy is reported per document type and field next to the overall figure", () => {
  const rows = accuracyBy([...recs("invoice", "total", 90, 0), ...recs("receipt", "date", 8, 2)]);
  assert.deepEqual(rows.map((r: any) => r.segment), ["overall", "invoice/total", "receipt/date"]);
  assert.deepEqual(rows[0], { segment: "overall", correct: 98, total: 100, percent: 98 });
  assert.deepEqual(rows[2], { segment: "receipt/date", correct: 8, total: 10, percent: 80 });
});

test("e1 a weak segment is hidden by a high overall figure and found by the breakdown", () => {
  const rows = accuracyBy([...recs("invoice", "total", 970, 10), ...recs("handwritten", "total", 8, 12)]);
  assert.ok(seg(rows, "overall").percent === 98 && seg(rows, "handwritten/total").percent === 40 && seg(rows, "invoice/total").percent === 99);
  assert.equal(accuracyBy([])[0].percent, 0);
});

test("e2 automation needs every segment to pass and enough samples in each", () => {
  const records = [...recs("invoice", "total", 97, 3), ...recs("receipt", "date", 50, 0), ...recs("handwritten", "total", 10, 0)];
  assert.deepEqual(canAutomate(records, 95, 30), { automate: false, failing: [], undersampled: ["handwritten/total"] });
  assert.deepEqual(canAutomate([...recs("invoice", "total", 97, 3), ...recs("receipt", "date", 45, 5)], 95, 30), { automate: false, failing: ["receipt/date"], undersampled: [] });
  assert.deepEqual(canAutomate([...recs("invoice", "total", 97, 3), ...recs("receipt", "date", 50, 0)], 95, 30), { automate: true, failing: [], undersampled: [] });
  assert.equal(canAutomate([], 95, 30).automate, false);
});

test("e3 the threshold is the lowest confidence whose accepted items meet the target precision", () => {
  const labeled: Array<[number, boolean]> = [[95, true], [90, true], [85, true], [80, false], [70, false], [60, false]];
  assert.equal(calibrateThreshold(labeled, 90), 85);
  assert.equal(calibrateThreshold(labeled, 70), 80);
  assert.equal(calibrateThreshold(labeled, 50), 60);
});

test("e4 no threshold exists when no confidence level meets the target", () => {
  assert.equal(calibrateThreshold([[95, false], [90, false]], 90), null);
  assert.equal(calibrateThreshold([], 90), null);
  assert.equal(calibrateThreshold([[95, true], [90, false]], 100), 95);
});

test("e5 the stratified sample takes the best ranked items of every stratum", () => {
  const items = [{ id: "a1", stratum: "invoice", rank: 5 }, { id: "a2", stratum: "invoice", rank: 1 }, { id: "a3", stratum: "invoice", rank: 3 },
    { id: "b1", stratum: "receipt", rank: 9 }, { id: "c1", stratum: "handwritten", rank: 2 }, { id: "c2", stratum: "handwritten", rank: 2 }];
  assert.deepEqual(stratifiedSample(items, 2), ["a2", "a3", "b1", "c1", "c2"]);
  assert.deepEqual(stratifiedSample(items, 1), ["a2", "b1", "c1"]);
  assert.deepEqual(stratifiedSample([], 3), []);
});

test("e6 low confidence and conflicts go to review with the weakest first", () => {
  const rows = [{ id: "x1", confidence: 90, conflict: false }, { id: "x2", confidence: 60, conflict: false }, { id: "x3", confidence: 99, conflict: true }, { id: "x4", confidence: 79, conflict: false }];
  assert.deepEqual(route(rows, 80, 10), { review: ["x3", "x2", "x4"], backlog: [], auto: ["x1"] });
});

test("e7 review capacity is respected and the rest wait in a backlog", () => {
  const rows = [...Array(5).keys()].map((i) => ({ id: `r${i}`, confidence: 50 + i, conflict: false })).concat([{ id: "ok", confidence: 99, conflict: false }]);
  assert.deepEqual(route(rows, 80, 2), { review: ["r0", "r1"], backlog: ["r2", "r3", "r4"], auto: ["ok"] });
  assert.deepEqual(route(rows, 80, 0).review, []);
});

test("e8 an irreversible action needs a person whatever the confidence", () => {
  assert.ok(checkpoint("delete_records", 1) === "human" && checkpoint("send_payment", 5) === "human");
  assert.ok(checkpoint("update_label", 50) === "auto" && checkpoint("update_label", 5000) === "human");
  assert.ok(checkpoint("update_label", 1000) === "auto" && checkpoint("update_label", 300, 200) === "human");
});
