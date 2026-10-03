// An eval run, a success gate and a regression comparison, on a scripted classifier.
//
// The Claude documentation on success criteria and evaluations (read on 2026-10-03) says to design evals that mirror the real task, edge
// cases included, to automate the grading, and to judge several dimensions at once ("an F1 score of at least 0.85", "99.5% of outputs are
// non-toxic"). This file runs six sentiment cases through two scripted versions of a prompt, grades them by exact match, and shows that a
// better average can still hide a regression. The two models are lookup tables standing in for the application: no model is called.

type Case = { id: string; input: string; expect: string; tags: string[] };
type Report = { results: { id: string; passed: boolean; tags: string[] }[]; pass_rate: number; by_tag: Record<string, [number, number]> };

export const CASES: Case[] = [
  { id: "pos-1", input: "Love it, works great", expect: "positive", tags: ["core"] },
  { id: "neg-1", input: "Broke after two days", expect: "negative", tags: ["core"] },
  { id: "neu-1", input: "It arrived on Tuesday", expect: "neutral", tags: ["core"] },
  { id: "sarcasm-1", input: "Oh great, another crash", expect: "negative", tags: ["edge"] },
  { id: "mixed-1", input: "Fast shipping but the screen is dim", expect: "neutral", tags: ["edge"] },
  { id: "empty-1", input: "", expect: "neutral", tags: ["edge"] },
];

export const PROMPT_V1: Record<string, string> = {
  "Love it, works great": "positive", "Broke after two days": "negative", "It arrived on Tuesday": "neutral",
  "Oh great, another crash": "positive", "Fast shipping but the screen is dim": "positive", "": "Neutral",
};
export const PROMPT_V2: Record<string, string> = { ...PROMPT_V1, "Oh great, another crash": "negative", "Fast shipping but the screen is dim": "neutral", "": "positive" };

export const CRITERIA = { min_pass_rate: 0.8, tags: { edge: 0.75 } as Record<string, number> };

/** Code-graded: the answer must equal the expected label, ignoring case and surrounding white space. */
export function grade(c: { expect: string }, output: string): boolean {
  return output.trim().toLowerCase() === c.expect;
}

export function run(cases: Case[], model: Record<string, string>): Report {
  const results = cases.map((c) => ({ id: c.id, passed: grade(c, model[c.input]), tags: c.tags }));
  const by_tag: Record<string, [number, number]> = {};
  for (const r of results) {
    for (const tag of r.tags) {
      const row = (by_tag[tag] ??= [0, 0]);
      row[0] += r.passed ? 1 : 0;
      row[1] += 1;
    }
  }
  return { results, pass_rate: results.filter((r) => r.passed).length / results.length, by_tag };
}

/** Every dimension of the success criteria must hold, not only the average. */
export function gate(report: Report, criteria: typeof CRITERIA): string[] {
  const failures: string[] = [];
  if (report.pass_rate < criteria.min_pass_rate) failures.push("overall");
  for (const [tag, minimum] of Object.entries(criteria.tags)) {
    const [passed, total] = report.by_tag[tag];
    if (passed / total < minimum) failures.push(`tag:${tag}`);
  }
  return failures;
}

export function compare(baseline: Report, current: Report) {
  const before = new Map(baseline.results.map((r) => [r.id, r.passed]));
  return {
    regressions: current.results.filter((r) => before.get(r.id) && !r.passed).map((r) => r.id),
    fixed: current.results.filter((r) => !before.get(r.id) && r.passed).map((r) => r.id),
  };
}

const list = (items: string[]) => `[${items.map((i) => `'${i}'`).join(", ")}]`;

function main() {
  const v1 = run(CASES, PROMPT_V1);
  const v2 = run(CASES, PROMPT_V2);
  for (const [name, report] of [["prompt v1", v1], ["prompt v2", v2]] as const) {
    const edge = report.by_tag.edge;
    console.log(`${name}: pass rate ${report.pass_rate.toFixed(3)}, edge ${edge[0]}/${edge[1]}, gate failures ${list(gate(report, CRITERIA))}`);
  }
  const diff = compare(v1, v2);
  console.log("v2 against v1: fixed", list(diff.fixed), "regressions", list(diff.regressions));
  const safe = diff.regressions.length === 0 && gate(v2, CRITERIA).length === 0;
  console.log("average improved:", v2.pass_rate > v1.pass_rate ? "True" : "False", "- safe to ship:", safe ? "True" : "False");
}

if (import.meta.main) main();
