// An eval harness. See ../../statement.md.
export type Case = { id: string; input: string; tags?: string[]; check: Record<string, unknown> };
export type Judge = ((prompt: string) => string) | null | undefined;
export type Report = {
  total: number;
  passed: number;
  pass_rate: number;
  results: { id: string; passed: boolean; reason: string; flaky: boolean }[];
  by_tag: Record<string, { passed: number; total: number }>;
  flaky: string[];
};

/** Grade one output against the case's check. Returns { passed, reason } (and score for a judge check). */
export function grade(_c: Case, _output: string, _judge?: Judge): { passed: boolean; reason: string; score?: number | null } {
  return { passed: true, reason: "ok" };
}

/** Run every case `repeats` times through the model and grade it. A case passes only if every run passes. */
export function runEval(_cases: Case[], _model: (input: string) => string, _judge?: Judge, _repeats = 1): Report {
  return { total: 0, passed: 0, pass_rate: 0, results: [], by_tag: {}, flaky: [] };
}

/** Compare a report with success criteria: min_pass_rate, tags { tag: minimum rate }, max_flaky. */
export function meets(_report: Report, _criteria: { min_pass_rate?: number; tags?: Record<string, number>; max_flaky?: number }) {
  return { met: true, failures: [] as string[] };
}

/** What changed between two reports: regressions, fixed, added and removed case ids, the pass-rate change. */
export function compare(_baseline: Pick<Report, "pass_rate" | "results">, _current: Pick<Report, "pass_rate" | "results">) {
  return { regressions: [] as string[], fixed: [] as string[], added: [] as string[], removed: [] as string[], pass_rate_delta: 0, ok: true };
}
