import { test } from "node:test";
import assert from "node:assert/strict";
import { FINDINGS, ledgerLines, plainSummary, sourcesNamed, status, type Row } from "./provenanceLoss.ts";

const rows = (...items: Array<[string, string, string]>): Row[] => items.map(([value, source, date]) => ["c", value, source, date]);

test("the status tells agreement from conflict and change", () => {
  assert.equal(status(rows(["1", "A", "2024-01-01"], ["1", "B", "2024-02-01"])), "agreed");
  assert.equal(status(rows(["1", "A", "2024-01-01"], ["2", "B", "2024-01-01"])), "conflict");
  assert.equal(status(rows(["1", "A", "2022-01-01"], ["2", "B", "2024-01-01"])), "changed");
});

test("the plain summary keeps one value and no sources", () => {
  const summary = plainSummary(FINDINGS);
  assert.ok(summary.includes("market growth 2024: 12%") && !summary.split("growth forecast")[0].includes("9%"));
  assert.equal(sourcesNamed(summary, FINDINGS), 0);
});

test("the ledger keeps both sides of a conflict with their sources", () => {
  const line = ledgerLines(FINDINGS).split("\n").find((l) => l.startsWith("market growth 2024"));
  assert.equal(line, "market growth 2024 [conflict]: 12% (Firm A report, 2024-05-01); 9% (Firm B survey, 2024-05-01)");
});

test("a change over time lists the older value first", () => {
  const line = ledgerLines(FINDINGS).split("\n").find((l) => l.startsWith("growth forecast"));
  assert.equal(line, "growth forecast [changed]: 7% (Firm C yearbook, 2022-04-01); 9% (Firm B survey, 2024-05-01)");
});

test("the ledger names every source", () => {
  assert.equal(sourcesNamed(ledgerLines(FINDINGS), FINDINGS), 5);
});
