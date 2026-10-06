import { test } from "node:test";
import assert from "node:assert/strict";
import { resolve } from "node:path";
import { pathToFileURL } from "node:url";

// The solution is the file beside this test.
const dir = resolve(import.meta.dirname);
const { audit } = await import(pathToFileURL(resolve(dir, "runAudit.ts")).href);

const POLICY = { target: 90, min_n: 3, gap: 5 };
type Fields = { kind?: string; status?: string; correct?: boolean; invented?: boolean; wasted?: boolean; sum_ok?: boolean };
const run = (f: Fields = {}) => ({ id: "d", kind: f.kind ?? "typed", status: f.status ?? "valid", correct: f.correct ?? true, invented: f.invented ?? false,
  retried_absent: f.wasted ?? false, sum_ok: f.sum_ok ?? true });
const many = (n: number, f: Fields = {}) => Array.from({ length: n }, () => run(f));
const seg = (kind: string, n: number, correct: number, percent: number, automate: boolean) => ({ kind, n, correct, percent, automate });

test("m1 a mixed run gets every count both accuracies the segments and the first fix", () => {
  const runs = [...many(3, { kind: "typed" }), run({ kind: "scanned" }), run({ kind: "scanned", status: "needs_review", correct: false }),
    run({ kind: "handwritten", status: "failed", correct: false, invented: true }), run({ kind: "handwritten", status: "needs_review", correct: false, wasted: true })];
  assert.deepEqual(audit(runs, POLICY), {
    n: 7, valid: 4, needs_review: 2, failed: 1, accuracy_all: 57, accuracy_validated: 100, meets_target: false,
    segments: [seg("handwritten", 2, 0, 0, false), seg("scanned", 2, 1, 50, false), seg("typed", 3, 3, 100, true)],
    invented: 1, wasted_retries: 1, unchecked_totals: 0, overstated: true, first_fix: "make_fields_nullable" });
});

test("e1 an empty run has zero figures no segments and never meets the target", () => {
  assert.deepEqual(audit([], POLICY), {
    n: 0, valid: 0, needs_review: 0, failed: 0, accuracy_all: 0, accuracy_validated: 0, meets_target: false,
    segments: [], invented: 0, wasted_retries: 0, unchecked_totals: 0, overstated: false, first_fix: "none" });
});

test("e2 the run meets the target at exactly the target and not below it", () => {
  const at = [...many(9), ...many(1, { correct: false })];
  const below = [...many(8), ...many(2, { correct: false })];
  assert.equal(audit(at, POLICY).meets_target, true);
  assert.equal(audit(at, POLICY).accuracy_all, 90);
  assert.equal(audit(below, POLICY).meets_target, false);
  assert.equal(audit(below, POLICY).accuracy_all, 80);
});

test("e3 a kind needs at least the minimum number of documents to be automated", () => {
  const report = audit([...many(3, { kind: "typed" }), ...many(2, { kind: "scanned" })], POLICY);
  assert.deepEqual(report.segments, [seg("scanned", 2, 2, 100, false), seg("typed", 3, 3, 100, true)]);
});

test("e4 a kind is automated at exactly the target accuracy and not below it", () => {
  const report = audit([...many(9, { kind: "typed" }), ...many(1, { kind: "typed", correct: false }), ...many(17, { kind: "scanned" }), ...many(2, { kind: "scanned", correct: false })], POLICY);
  assert.deepEqual(report.segments, [seg("scanned", 19, 17, 89, false), seg("typed", 10, 9, 90, true)]);
});

test("e5 the figure is overstated only when the validated accuracy exceeds the all document accuracy by more than the gap", () => {
  const atGap = audit([...many(19), ...many(1, { status: "failed", correct: false })], POLICY);
  assert.deepEqual([atGap.accuracy_all, atGap.accuracy_validated, atGap.overstated, atGap.first_fix], [95, 100, false, "none"]);
  const over = audit([...many(16), ...many(1, { status: "failed", correct: false })], POLICY);
  assert.deepEqual([over.accuracy_all, over.accuracy_validated, over.overstated, over.first_fix], [94, 100, true, "measure_all_documents"]);
});

test("e6 each shape counts documents once and an unchecked total counts only documents accepted as valid", () => {
  const runs = [run({ sum_ok: false }), run({ status: "needs_review", correct: false, sum_ok: false }), run({ status: "failed", correct: false, sum_ok: false }),
    run({ invented: true, wasted: true })];
  const report = audit(runs, POLICY);
  assert.deepEqual([report.invented, report.wasted_retries, report.unchecked_totals], [1, 1, 1]);
});

test("e7 the first fix follows the order of what costs most", () => {
  assert.equal(audit([run({ invented: true, wasted: true, sum_ok: false })], POLICY).first_fix, "make_fields_nullable");
  assert.equal(audit([run({ wasted: true, sum_ok: false })], POLICY).first_fix, "stop_retrying_absent");
  assert.equal(audit([run({ sum_ok: false })], POLICY).first_fix, "add_semantic_checks");
  assert.equal(audit([...many(16), ...many(1, { status: "failed", correct: false })], POLICY).first_fix, "measure_all_documents");
  assert.equal(audit([...many(8), ...many(2, { correct: false })], POLICY).first_fix, "improve_weak_segments");
  assert.equal(audit(many(5), POLICY).first_fix, "none");
});

test("e8 percentages are whole numbers rounded half up", () => {
  const report = audit([...many(1), ...many(7, { correct: false })], POLICY);
  assert.deepEqual([report.accuracy_all, report.accuracy_validated, report.segments[0].percent], [13, 13, 13]);
  assert.equal(audit([...many(2), ...many(1, { correct: false })], POLICY).accuracy_all, 67);
  assert.equal(audit([...many(1), ...many(2, { correct: false })], POLICY).accuracy_all, 33);
});
