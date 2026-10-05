// Audit an extraction run: the accuracy on every document, the accuracy by kind of document, the failure shapes and the first fix.
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

export function audit(runs: Run[], policy: Policy): Report {
  const n = runs.length;
  const { target, min_n, gap } = policy;
  const count = (f: (r: Run) => boolean) => runs.filter(f).length;
  const valid = count((r) => r.status === "valid");
  const right = count((r) => r.correct);
  const rightValid = count((r) => r.status === "valid" && r.correct);
  const accuracyAll = percent(right, n);
  const accuracyValidated = percent(rightValid, valid);
  const segments: Segment[] = [...new Set(runs.map((r) => r.kind))].sort().map((kind) => {
    const group = runs.filter((r) => r.kind === kind);
    const ok = group.filter((r) => r.correct).length;
    return { kind, n: group.length, correct: ok, percent: percent(ok, group.length), automate: group.length >= min_n && ok * 100 >= target * group.length };
  });
  const invented = count((r) => r.invented);
  const wasted = count((r) => r.retried_absent);
  const unchecked = count((r) => r.status === "valid" && !r.sum_ok);
  const overstated = accuracyValidated - accuracyAll > gap;
  const meetsTarget = n > 0 && right * 100 >= target * n;
  let firstFix: string;
  if (n === 0) firstFix = "none";
  else if (invented) firstFix = "make_fields_nullable";
  else if (wasted) firstFix = "stop_retrying_absent";
  else if (unchecked) firstFix = "add_semantic_checks";
  else if (overstated) firstFix = "measure_all_documents";
  else if (!meetsTarget) firstFix = "improve_weak_segments";
  else firstFix = "none";
  return {
    n, valid, needs_review: count((r) => r.status === "needs_review"), failed: count((r) => r.status === "failed"), accuracy_all: accuracyAll,
    accuracy_validated: accuracyValidated, meets_target: meetsTarget, segments, invented, wasted_retries: wasted, unchecked_totals: unchecked,
    overstated, first_fix: firstFix,
  };
}
