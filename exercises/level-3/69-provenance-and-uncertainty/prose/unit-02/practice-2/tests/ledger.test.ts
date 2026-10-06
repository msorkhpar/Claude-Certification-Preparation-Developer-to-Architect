import { test } from "node:test";
import assert from "node:assert/strict";
import { resolve } from "node:path";
import { pathToFileURL } from "node:url";

// The solution is the file beside this test.
const dir = resolve(import.meta.dirname);
const solution = await import(pathToFileURL(resolve(dir, "ledger.ts")).href);
const got = (fn: (...args: any[]) => any) => (...args: any[]) => {
  const value = fn(...args);
  assert.ok(value !== null && value !== undefined, `${fn.name} returned nothing`);
  return value;
};
const [checkFinding, merge, coverageNote, render] = [solution.checkFinding, solution.merge, solution.coverageNote, solution.render].map(got);

const f = (claim: string, value: string, source: string, date: string) => ({ claim, value, source, date });
const entry = (claim: string, status: string, ...values: Array<[string, Array<[string, string]>]>) => ({
  claim,
  status,
  values: values.map(([value, srcs]) => ({ value, sources: srcs.map(([source, date]) => ({ source, date })) })),
});

test("m1 findings are merged per claim with every source kept once", () => {
  const merged = merge([
    f("revenue 2023", "4.1B", "Annual report", "2024-02-01"),
    f("headcount", "910", "Press release", "2024-03-01"),
    f("revenue 2023", "4.1B", "Press release", "2024-02-03"),
    f("revenue 2023", "4.1B", "Annual report", "2024-02-01"),
  ]);
  assert.deepEqual(merged, [
    entry("revenue 2023", "agreed", ["4.1B", [["Annual report", "2024-02-01"], ["Press release", "2024-02-03"]]]),
    entry("headcount", "agreed", ["910", [["Press release", "2024-03-01"]]]),
  ]);
  assert.deepEqual(merge([]), []);
});

test("e1 a finding without a source or a date is refused", () => {
  assert.deepEqual(checkFinding(f("c", "v", "", " ")), ["source", "date"]);
  assert.deepEqual(checkFinding({ value: "v", source: "s", date: "2024-01-01" }), ["claim"]);
  assert.deepEqual(checkFinding(f("c", "v", "s", "2024-01-01")), []);
  assert.throws(() => merge([f("c", "v", "s", "2024-01-01"), f("c", "w", "s", "")]));
  assert.doesNotThrow(() => merge([f("c", "v", "s", "2024-01-01")]));
});

test("e2 two values from the same date are a conflict that keeps both", () => {
  const merged = merge([f("market size", "12%", "Firm A report", "2024-05-01"), f("market size", "9%", "Firm B survey", "2024-05-01")]);
  assert.deepEqual(merged, [entry("market size", "conflict", ["12%", [["Firm A report", "2024-05-01"]]], ["9%", [["Firm B survey", "2024-05-01"]]])]);
});

test("e3 two values from different dates are a change not a conflict", () => {
  const merged = merge([f("market size", "12%", "Firm A report", "2024-05-01"), f("market size", "9%", "Firm B survey", "2022-05-01")]);
  assert.deepEqual(merged, [entry("market size", "changed", ["9%", [["Firm B survey", "2022-05-01"]]], ["12%", [["Firm A report", "2024-05-01"]]])]);
});

test("e4 the coverage note separates what is well supported from what is not", () => {
  const merged = merge([
    f("a", "1", "S1", "2024-01-01"), f("a", "1", "S2", "2024-01-02"),
    f("b", "2", "S1", "2024-01-01"), f("b", "2", "S1", "2024-02-01"),
    f("c", "3", "S1", "2024-01-01"), f("c", "4", "S2", "2022-01-01"),
    f("d", "5", "S1", "2024-01-01"), f("d", "6", "S2", "2024-01-01"),
  ]);
  const note = coverageNote(["a", "b", "c", "d"], merged, {});
  assert.deepEqual([note.well_supported, note.single_source, note.changed, note.contested, note.gaps], [["a"], ["b"], ["c"], ["d"], []]);
});

test("e5 a planned claim with no finding is a gap with its reason", () => {
  const merged = merge([f("a", "1", "S1", "2024-01-01"), f("z", "9", "S1", "2024-01-01")]);
  const note = coverageNote(["q", "a", "r"], merged, { q: "the registry timed out" });
  assert.deepEqual(note.gaps, [{ claim: "q", reason: "the registry timed out" }, { claim: "r", reason: "no source found" }]);
  assert.deepEqual(note.single_source, ["a", "z"]);
});

test("e6 financial data is rendered as a table", () => {
  const e = entry("revenue 2023", "agreed", ["4.1B", [["Annual report", "2024-02-01"], ["Press release", "2024-02-03"]]]);
  assert.equal(render(e, "financial"), "| Source | Date | Value |\n|---|---|---|\n| Annual report | 2024-02-01 | 4.1B |\n| Press release | 2024-02-03 | 4.1B |");
});

test("e7 news is rendered as prose that says when sources disagree", () => {
  const agreed = entry("launch", "agreed", ["in May", [["Daily", "2024-05-02"]]]);
  const changed = entry("size", "changed", ["9%", [["B", "2022-05-01"]]], ["12%", [["A", "2024-05-01"]]]);
  const conflict = entry("size", "conflict", ["12%", [["A", "2024-05-01"]]], ["9%", [["B", "2024-05-01"]]]);
  assert.equal(render(agreed, "news"), "launch: in May (Daily, 2024-05-02).");
  assert.equal(render(changed, "news"), "size: 9% (B, 2022-05-01); 12% (A, 2024-05-01). The figures are from different dates.");
  assert.equal(render(conflict, "news"), "size: 12% (A, 2024-05-01); 9% (B, 2024-05-01). The sources disagree.");
});

test("e8 technical findings are a list and an unknown content type is refused", () => {
  const e = entry("rate limit", "agreed", ["100 per minute", [["API docs", "2024-06-01"], ["Changelog", "2024-06-05"]]]);
  assert.equal(render(e, "technical"), "rate limit:\n- 100 per minute (API docs, 2024-06-01)\n- 100 per minute (Changelog, 2024-06-05)");
  assert.notEqual(render(e, "technical"), render(e, "financial"));
  assert.throws(() => render(e, "poem"));
});
