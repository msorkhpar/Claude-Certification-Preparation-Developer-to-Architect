// An eval harness. See ../../statement.md.
import { logger } from "./logger.ts";
const log = logger("harness");
export type Check =
  | { type: "exact"; expected: string }
  | { type: "regex"; pattern: string }
  | { type: "json_field"; field: string; equals: string | number | boolean }
  | { type: "judge"; criterion: string; threshold?: number };
export type Case = { id: string; input: string; tags?: string[]; check: Check };
export type Verdict = { passed: boolean; reason: string; score?: number | null };
export type Judge = ((prompt: string) => string) | null | undefined;
export type Report = {
  total: number;
  passed: number;
  pass_rate: number;
  results: { id: string; passed: boolean; reason: string; flaky: boolean }[];
  by_tag: Record<string, { passed: number; total: number }>;
  flaky: string[];
};

function norm(text: string): string {
  // TODO 1 of 9 (unlocks e1): the form two texts are compared in for an exact check.
  // Receives a text and returns it trimmed, every run of white space made one space, and lower-cased. Nothing else changes.
  // Example: norm("  Paris \n FRANCE ") -> "paris france"
  return text;
}

export function judgePrompt(criterion: string, output: string): string {
  return `Rate this response on a scale of 1-5 for ${criterion}:\n<response>${output}</response>\n` +
    `1: Not at all ${criterion}\n5: Perfectly ${criterion}\nOutput only the number.`;
}

function fieldResult(obj: Record<string, unknown>, check: { field: string; equals: string | number | boolean }): Verdict {
  // TODO 2 of 9 (unlocks e2): grade a parsed JSON object against a json_field check.
  // Receives the parsed object and the check { field, equals }. Returns a verdict: reason "missing field" when the field is absent;
  // "ok" when the value equals `equals` with the same JSON type; otherwise "mismatch".
  // Example: { n: "3" } against equals 3 -> { passed: false, reason: "mismatch" }
  return { passed: false, reason: "mismatch" };
}

function parseScore(reply: unknown): number | null {
  // TODO 3 of 9 (unlocks e3): read the judge's reply as a score.
  // Receives the reply (any value). Returns the number when the trimmed reply is exactly one digit from 1 to 5, else null.
  // Example: parseScore(" 4\n") -> 4, parseScore("I give it a 4") -> null
  return null;
}

function judged(score: number, threshold: number): Verdict {
  // TODO 4 of 9 (unlocks e3): the verdict for a judge score.
  // Receives the score and the threshold. Returns { passed, reason, score }: "ok" at or above the threshold, else "below threshold".
  // Example: judged(4, 4) -> { passed: true, reason: "ok", score: 4 }
  return { passed: false, reason: "below threshold", score };
}

/** Grade one output against the case's check. */
export function grade(c: Case, output: string, judge?: Judge): Verdict {
  log.debug("grade input", output);
  const check = c.check;
  if (check.type === "exact") return norm(output) === norm(check.expected) ? { passed: true, reason: "ok" } : { passed: false, reason: "mismatch" };
  if (check.type === "regex") return new RegExp(check.pattern).test(output) ? { passed: true, reason: "ok" } : { passed: false, reason: "mismatch" };
  if (check.type === "json_field") {
    let data: unknown;
    try {
      data = JSON.parse(output);
    } catch {
      return { passed: false, reason: "not json" };
    }
    if (typeof data !== "object" || data === null || Array.isArray(data)) return { passed: false, reason: "not json" };
    const obj = data as Record<string, unknown>;
    return fieldResult(obj, check);
  }
  if (check.type === "judge") {
    if (!judge) return { passed: false, reason: "ungradable", score: null };
    let reply: unknown;
    try {
      reply = judge(judgePrompt(check.criterion, output));
    } catch {
      return { passed: false, reason: "ungradable", score: null };
    }
    const score = parseScore(reply);
    if (score === null) return { passed: false, reason: "ungradable", score: null };
    return judged(score, check.threshold ?? 4);
  }
  return { passed: false, reason: "ungradable" };
}

function runOnce(model: (input: string) => string, judge: Judge, c: Case): Verdict {
  // TODO 5 of 9 (unlocks e4): one run of one case.
  // Receives the model, the judge and the case. Returns the verdict of `grade` for the model's output; when the model throws, the
  // verdict is { passed: false, reason: "model error" } and nothing is thrown. Example: a model that throws -> { passed: false, reason: "model error" }
  return { passed: false, reason: "model error" };
}

function outcome(runs: Verdict[]): { passed: boolean; mixed: boolean; reason: string } {
  // TODO 6 of 9 (unlocks e7): combine the runs of one case.
  // Receives the verdicts of its runs. Returns { passed, mixed, reason }: passed only if every run passed; mixed (flaky) when some
  // passed and some failed; the reason is "ok", or the reason of the first failed run.
  // Example: [ok, mismatch] -> { passed: false, mixed: true, reason: "mismatch" }
  return { passed: false, mixed: false, reason: "mismatch" };
}

function countTags(byTag: Report["by_tag"], tags: string[], passed: boolean): void {
  // TODO 7 of 9 (unlocks e5): count one case under each of its tags.
  // Receives the object `byTag` (changed in place), the case's tags and whether it passed. Every tag's `total` goes up by one, and its
  // `passed` by one when the case passed. Example: tags ["a"], passed -> byTag.a is { passed: 1, total: 1 }
}

/** Run every case `repeats` times through the model and grade it. A case passes only if every run passes. */
export function runEval(cases: Case[], model: (input: string) => string, judge?: Judge, repeats = 1): Report {
  const results: Report["results"] = [];
  const byTag: Report["by_tag"] = {};
  const flaky: string[] = [];
  for (const c of cases) {
    const runs: Verdict[] = [];
    for (let i = 0; i < repeats; i++) runs.push(runOnce(model, judge, c));
    const { passed, mixed, reason } = outcome(runs);
    results.push({ id: c.id, passed, reason, flaky: mixed });
    if (mixed) flaky.push(c.id);
    countTags(byTag, c.tags ?? [], passed);
  }
  const total = results.length;
  const passedCount = results.filter((r) => r.passed).length;
  return { total, passed: passedCount, pass_rate: total ? passedCount / total : 0, results, by_tag: byTag, flaky };
}

function tagFailed(row: { passed: number; total: number } | undefined, minimum: number): boolean {
  // TODO 8 of 9 (unlocks e5): does one tag fail its minimum rate?
  // Receives the tag's row { passed, total } or undefined (no case carries the tag) and the minimum rate. Returns true for undefined,
  // an empty row, or a rate below the minimum; a rate equal to the minimum is fine. Example: ({ passed: 1, total: 2 }, 0.5) -> false
  return false;
}

/** Compare a report with success criteria: min_pass_rate, tags {tag: minimum rate}, max_flaky. */
export function meets(report: Report, criteria: { min_pass_rate?: number; tags?: Record<string, number>; max_flaky?: number }) {
  const failures: string[] = [];
  if (criteria.min_pass_rate !== undefined && report.pass_rate < criteria.min_pass_rate) failures.push("overall");
  for (const [tag, minimum] of Object.entries(criteria.tags ?? {})) {
    if (tagFailed(report.by_tag[tag], minimum)) failures.push(`tag:${tag}`);
  }
  if (criteria.max_flaky !== undefined && report.flaky.length > criteria.max_flaky) failures.push("flaky");
  return { met: failures.length === 0, failures };
}

function changes(before: Map<string, boolean>, now: Map<string, boolean>): { regressions: string[]; fixed: string[]; added: string[]; removed: string[] } {
  // TODO 9 of 9 (unlocks e6): what changed between two runs.
  // Receives two maps { case id -> passed }, the baseline and the current run. Returns { regressions, fixed, added, removed }, arrays
  // of ids: passed before and fails now; failed before and passes now; only in the current run; only in the baseline. The first three
  // follow the current order, removed the baseline's. Example: before { a: true }, now { a: false, b: true } -> regressions ["a"], added ["b"]
  return { regressions: [], fixed: [], added: [], removed: [] };
}

/** What changed between two reports: regressions, fixes, added and removed cases, the pass-rate change. */
export function compare(baseline: Pick<Report, "pass_rate" | "results">, current: Pick<Report, "pass_rate" | "results">) {
  const before = new Map(baseline.results.map((r) => [r.id, r.passed]));
  const now = new Map(current.results.map((r) => [r.id, r.passed]));
  const { regressions, fixed, added, removed } = changes(before, now);
  return { regressions, fixed, added, removed, pass_rate_delta: current.pass_rate - baseline.pass_rate, ok: regressions.length === 0 && removed.length === 0 };
}
