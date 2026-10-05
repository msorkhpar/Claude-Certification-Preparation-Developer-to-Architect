// An eval harness. See ../../statement.md.
import { logger } from "../logger.ts";
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
  return text.split(/\s+/).filter(Boolean).join(" ").toLowerCase();
}

export function judgePrompt(criterion: string, output: string): string {
  return `Rate this response on a scale of 1-5 for ${criterion}:\n<response>${output}</response>\n` +
    `1: Not at all ${criterion}\n5: Perfectly ${criterion}\nOutput only the number.`;
}

function fieldResult(obj: Record<string, unknown>, check: { field: string; equals: string | number | boolean }): Verdict {
  if (!(check.field in obj)) return { passed: false, reason: "missing field" };
  return obj[check.field] === check.equals ? { passed: true, reason: "ok" } : { passed: false, reason: "mismatch" };
}

function parseScore(reply: unknown): number | null {
  const text = typeof reply === "string" ? reply.trim() : "";
  return /^[1-5]$/.test(text) ? Number(text) : null;
}

function judged(score: number, threshold: number): Verdict {
  return score >= threshold ? { passed: true, reason: "ok", score } : { passed: false, reason: "below threshold", score };
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
  let output: string;
  try {
    output = model(c.input);
  } catch {
    return { passed: false, reason: "model error" };
  }
  return grade(c, output, judge);
}

function outcome(runs: Verdict[]): { passed: boolean; mixed: boolean; reason: string } {
  const passed = runs.every((r) => r.passed);
  const mixed = runs.some((r) => r.passed) && !passed;
  return { passed, mixed, reason: passed ? "ok" : runs.find((r) => !r.passed)!.reason };
}

function countTags(byTag: Report["by_tag"], tags: string[], passed: boolean): void {
  for (const tag of tags) {
    const row = (byTag[tag] ??= { passed: 0, total: 0 });
    row.total += 1;
    row.passed += passed ? 1 : 0;
  }
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
  return !row || row.total === 0 || row.passed / row.total < minimum;
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
  const regressions = [...now.keys()].filter((i) => before.get(i) === true && now.get(i) === false);
  const fixed = [...now.keys()].filter((i) => before.get(i) === false && now.get(i) === true);
  const added = [...now.keys()].filter((i) => !before.has(i));
  const removed = [...before.keys()].filter((i) => !now.has(i));
  return { regressions, fixed, added, removed };
}

/** What changed between two reports: regressions, fixes, added and removed cases, the pass-rate change. */
export function compare(baseline: Pick<Report, "pass_rate" | "results">, current: Pick<Report, "pass_rate" | "results">) {
  const before = new Map(baseline.results.map((r) => [r.id, r.passed]));
  const now = new Map(current.results.map((r) => [r.id, r.passed]));
  const { regressions, fixed, added, removed } = changes(before, now);
  return { regressions, fixed, added, removed, pass_rate_delta: current.pass_rate - baseline.pass_rate, ok: regressions.length === 0 && removed.length === 0 };
}
