import { test } from "node:test";
import assert from "node:assert/strict";
import { resolve } from "node:path";
import { pathToFileURL } from "node:url";

// SOLUTION_DIR selects starter, reference or a planted wrong solution.
const dir = resolve(process.env.SOLUTION_DIR ?? "starter");
const { synthesize } = await import(pathToFileURL(resolve(dir, "synthesis.ts")).href);

const f = (claim: string, value: string, source: string, date = "2025-01-01") => ({ claim, value, source, date });
const ok = (scope: string, ...findings: unknown[]) => ({ scope, status: "ok", findings, error: null });
const err = (scope: string, query: string, partial: unknown[] = [], alternatives: string[] = [], type = "timeout") =>
  ({ scope, status: "error", findings: [], error: { type, query, partial, alternatives } });
const src = (source: string, date = "2025-01-01") => ({ source, date });

test("m1 a full run with agreement returns claims with every source and a complete status", () => {
  const report = synthesize(["a", "b"], [ok("a", f("X", "1", "s1"), f("Y", "2", "s2")), ok("b", f("X", "1", "s3"))]);
  assert.deepEqual(report, { status: "complete", covered: ["a", "b"], gaps: [], partial: [], conflicts: [], errors: [], note: "all scopes covered",
    claims: [{ claim: "X", value: "1", sources: [src("s1"), src("s3")], partial: false }, { claim: "Y", value: "2", sources: [src("s2")], partial: false }] });
});

test("e1 no results leave every scope a gap and say not researched", () => {
  const report = synthesize(["a", "b"], []);
  assert.deepEqual([report.status, report.covered, report.gaps, report.claims], ["partial", [], ["a", "b"], []]);
  assert.equal(report.note, "not covered: a (not researched), b (not researched)");
});

test("e2 a scope the plan never covered and a scope whose search failed read differently in the note", () => {
  const report = synthesize(["a", "b", "c"], [ok("a", f("X", "1", "s1")), err("b", "query b", [], ["query b2"])]);
  assert.equal(report.status, "partial");
  assert.deepEqual(report.gaps, ["b", "c"]);
  assert.equal(report.note, "not covered: b (timeout on 'query b'), c (not researched)");
  assert.deepEqual(report.errors, [{ scope: "b", type: "timeout", query: "query b", alternatives: ["query b2"] }]);
});

test("e3 two values for one claim are a conflict that names both sources and no claim", () => {
  const report = synthesize(["a", "b"], [ok("a", f("X", "12", "s1", "2024-03-01")), ok("b", f("X", "14", "s2", "2025-01-15"), f("X", "14", "s2", "2025-01-15"))]);
  assert.deepEqual(report.claims, []);
  assert.deepEqual(report.conflicts, [{ claim: "X", values: [{ value: "12", source: "s1", date: "2024-03-01" }, { value: "14", source: "s2", date: "2025-01-15" }] }]);
  assert.equal(report.status, "complete");
});

test("e4 partial results are kept and flagged but do not cover the scope and a retry that worked clears the error", () => {
  const report = synthesize(["a", "b"], [ok("a", f("X", "1", "s1")), err("b", "qb", [f("Z", "9", "s9")], ["qb2"])]);
  assert.deepEqual(report.claims, [{ claim: "X", value: "1", sources: [src("s1")], partial: false }, { claim: "Z", value: "9", sources: [src("s9")], partial: true }]);
  assert.deepEqual([report.covered, report.gaps, report.partial], [["a"], ["b"], ["b"]]);
  const healed = synthesize(["a", "b"], [ok("a", f("X", "1", "s1")), err("b", "qb", [], ["qb2"]), ok("b", f("Z", "9", "s9"))]);
  assert.deepEqual(healed.errors, []);
  assert.equal(healed.status, "complete");
  assert.deepEqual(healed.covered, ["a", "b"]);
  const mixed = synthesize(["a"], [err("a", "qa", [f("X", "1", "s1")]), ok("a", f("X", "1", "s2"))]);
  assert.equal(mixed.claims[0].partial, false);
});

test("e5 a result with no findings does not cover its scope", () => {
  const report = synthesize(["a", "b"], [ok("a", f("X", "1", "s1")), ok("b")]);
  assert.deepEqual([report.covered, report.gaps, report.status], [["a"], ["b"], "partial"]);
  assert.equal(report.note, "not covered: b (no findings)");
});

test("e6 claims sources and scopes come out in a fixed order without duplicates", () => {
  const report = synthesize(["b", "a"], [ok("a", f("Y", "2", "s2"), f("X", "1", "s9")), ok("b", f("X", "1", "s1"), f("X", "1", "s9"))]);
  assert.deepEqual(report.covered, ["b", "a"]);
  assert.deepEqual(report.claims.map((c: any) => c.claim), ["X", "Y"]);
  assert.deepEqual(report.claims[0].sources, [src("s9"), src("s1")]);
});
