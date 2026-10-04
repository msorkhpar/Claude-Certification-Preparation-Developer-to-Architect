// Audit an extraction run: the accuracy on every document, the accuracy by kind of document, the failure shapes and the first fix.
// Read statement.md for the fields of a run, of the policy and of the report, then replace the body of audit().
export type Run = { id: string; kind: string; status: string; correct: boolean; invented: boolean; retried_absent: boolean; sum_ok: boolean };
export type Policy = { target: number; min_n: number; gap: number };
export type Segment = { kind: string; n: number; correct: number; percent: number; automate: boolean };
export type Report = {
  n: number; valid: number; needs_review: number; failed: number; accuracy_all: number; accuracy_validated: number; meets_target: boolean;
  segments: Segment[]; invented: number; wasted_retries: number; unchecked_totals: number; overstated: boolean; first_fix: string;
};

export function audit(runs: Run[], policy: Policy): Report {
  return { n: 0, valid: 0, needs_review: 0, failed: 0, accuracy_all: 0, accuracy_validated: 0, meets_target: false, segments: [], invented: 0, wasted_retries: 0, unchecked_totals: 0, overstated: false, first_fix: "" };
}
