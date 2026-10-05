// Audit an extraction run: the accuracy on every document, the accuracy by kind of document, the failure shapes and the first fix.
import { logger } from "../logger.ts";
const log = logger("run_audit");
export type Run = { id: string; kind: string; status: string; correct: boolean; invented: boolean; retried_absent: boolean; sum_ok: boolean };
export type Policy = { target: number; min_n: number; gap: number };
export type Segment = { kind: string; n: number; correct: number; percent: number; automate: boolean };
export type Report = {
  n: number; valid: number; needs_review: number; failed: number; accuracy_all: number; accuracy_validated: number; meets_target: boolean;
  segments: Segment[]; invented: number; wasted_retries: number; unchecked_totals: number; overstated: boolean; first_fix: string;
};

/** A whole percentage, rounded half up; 0 when there is nothing to divide. */
export function percent(correct: number, total: number): number {
  return total ? Math.floor((200 * correct + total) / (2 * total)) : 0;
}

/** One entry per kind, sorted by kind. */
export function segmentsOf(runs: Run[], policy: Policy): Segment[] {
  return [...new Set(runs.map((r) => r.kind))].sort().map((kind) => {
    const group = runs.filter((r) => r.kind === kind);
    const ok = group.filter((r) => r.correct).length;
    return { kind, n: group.length, correct: ok, percent: percent(ok, group.length), automate: group.length >= policy.min_n && ok * 100 >= policy.target * group.length };
  });
}

/** [invented, wastedRetries, uncheckedTotals] counted in documents. */
export function failureShapes(runs: Run[]): [number, number, number] {
  const count = (f: (r: Run) => boolean) => runs.filter(f).length;
  return [count((r) => r.invented), count((r) => r.retried_absent), count((r) => r.status === "valid" && !r.sum_ok)];
}

/** True when the run has a document and right * 100 >= target * n. */
export function meets(right: number, n: number, target: number): boolean {
  return n > 0 && right * 100 >= target * n;
}

/** True when the validated accuracy exceeds the all-document accuracy by more than the gap. */
export function isOverstated(accuracyValidated: number, accuracyAll: number, gap: number): boolean {
  return accuracyValidated - accuracyAll > gap;
}

/** The first fix that applies, in the order of the statement. */
export function chooseFix(n: number, invented: number, wasted: number, unchecked: number, overstated: boolean, meetsTarget: boolean): string {
  if (n === 0) return "none";
  if (invented) return "make_fields_nullable";
  if (wasted) return "stop_retrying_absent";
  if (unchecked) return "add_semantic_checks";
  if (overstated) return "measure_all_documents";
  if (!meetsTarget) return "improve_weak_segments";
  return "none";
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
