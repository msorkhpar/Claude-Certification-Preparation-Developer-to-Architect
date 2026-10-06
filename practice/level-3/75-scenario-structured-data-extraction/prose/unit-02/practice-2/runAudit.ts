// Audit an extraction run: the accuracy on every document, the accuracy by kind of document, the failure shapes and the first fix.
import { logger } from "./logger.ts";
const log = logger("run_audit");
export type Run = { id: string; kind: string; status: string; correct: boolean; invented: boolean; retried_absent: boolean; sum_ok: boolean };
export type Policy = { target: number; min_n: number; gap: number };
export type Segment = { kind: string; n: number; correct: number; percent: number; automate: boolean };
export type Report = {
  n: number; valid: number; needs_review: number; failed: number; accuracy_all: number; accuracy_validated: number; meets_target: boolean;
  segments: Segment[]; invented: number; wasted_retries: number; unchecked_totals: number; overstated: boolean; first_fix: string;
};

/**
 * TODO 1 of 6 (unlocks m1, e1 and e8): a whole percentage, rounded half up.
 * Receives the correct count and the total. Returns `Math.floor((200 * correct + total) / (2 * total))`, and 0 when the total is 0.
 * Example: percent(2, 3) -> 67, percent(1, 3) -> 33, percent(0, 0) -> 0
 */
export function percent(correct: number, total: number): number {
  return 0;
}

/**
 * TODO 2 of 6 (unlocks m1, e3 and e4): one entry per kind of document.
 * Receives the runs and the policy. Returns a list sorted by kind of {kind, n, correct, percent, automate}; `automate` needs at
 * least `min_n` documents of the kind and `correct * 100 >= target * n` for it.
 * Example: 9 of 10 typed documents with min_n 5 and target 90 -> [{ kind: "typed", n: 10, correct: 9, percent: 90, automate: true }]
 */
export function segmentsOf(runs: Run[], policy: Policy): Segment[] {
  return [{ kind: "", n: 0, correct: 0, percent: 0, automate: false }];
}

/**
 * TODO 3 of 6 (unlocks m1 and e6): count the failure shapes in documents.
 * Receives the runs. Returns [invented, wastedRetries, uncheckedTotals]: documents flagged `invented`, documents flagged
 * `retried_absent`, and `valid` documents whose `sum_ok` is false (a document never accepted is not counted).
 * Example: one valid document with sum_ok false and one failed document with sum_ok false -> [0, 0, 1]
 */
export function failureShapes(runs: Run[]): [number, number, number] {
  return [0, 0, 0];
}

/**
 * TODO 4 of 6 (unlocks m1, e1 and e2): does the run meet the target?
 * Receives the correct count, the document count and the target in percent. Returns true when there is at least one document
 * and `right * 100 >= target * n`. Example: meets(9, 10, 90) -> true, meets(8, 10, 90) -> false, meets(0, 0, 90) -> false
 */
export function meets(right: number, n: number, target: number): boolean {
  return false;
}

/**
 * TODO 5 of 6 (unlocks m1 and e5): is the figure overstated?
 * Receives both accuracies and the gap in points. Returns true only when the validated accuracy exceeds the all-document
 * accuracy by more than the gap. Example: isOverstated(100, 95, 5) -> false, isOverstated(100, 94, 5) -> true
 */
export function isOverstated(accuracyValidated: number, accuracyAll: number, gap: number): boolean {
  return false;
}

/**
 * TODO 6 of 6 (unlocks m1, e1 and e7): the first fix that applies.
 * Receives the document count, the three shape counts, `overstated` and `meetsTarget`. Returns `none` for an empty run, else the
 * first that applies of make_fields_nullable (invented), stop_retrying_absent (wasted), add_semantic_checks (unchecked),
 * measure_all_documents (overstated), improve_weak_segments (target not met), and `none`.
 * Example: chooseFix(3, 0, 1, 1, false, true) -> "stop_retrying_absent"
 */
export function chooseFix(n: number, invented: number, wasted: number, unchecked: number, overstated: boolean, meetsTarget: boolean): string {
  return "";
}

export function audit(runs: Run[], policy: Policy): Report {
  log.debug("audit input", runs);
  const n = runs.length;
  const count = (f: (r: Run) => boolean) => runs.filter(f).length;
  const valid = count((r) => r.status === "valid");
  const right = count((r) => r.correct);
  const rightValid = count((r) => r.status === "valid" && r.correct);
  const accuracyAll = percent(right, n);
  const accuracyValidated = percent(rightValid, valid);
  const [invented, wasted, unchecked] = failureShapes(runs);
  const overstated = isOverstated(accuracyValidated, accuracyAll, policy.gap);
  const meetsTarget = meets(right, n, policy.target);
  return {
    n, valid, needs_review: count((r) => r.status === "needs_review"), failed: count((r) => r.status === "failed"), accuracy_all: accuracyAll,
    accuracy_validated: accuracyValidated, meets_target: meetsTarget, segments: segmentsOf(runs, policy), invented, wasted_retries: wasted, unchecked_totals: unchecked,
    overstated, first_fix: chooseFix(n, invented, wasted, unchecked, overstated, meetsTarget),
  };
}
