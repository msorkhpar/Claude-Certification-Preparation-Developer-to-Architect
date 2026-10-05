// A multi-agent research run in miniature: a coordinator that checks its own decomposition, a search subagent whose failure comes back as structured context,
// one retry through an alternative, a synthesis agent with a scoped verification tool, and a report that says what it could not cover.
//
// The subagents are functions over made-up data: this example is about what the coordinator does with what comes back, not about what a model writes. The
// shapes (a result with a status, an error with a type, the query, partial results and alternatives) are this course's design, not an Anthropic interface.
import { logger } from "./logger.ts";
const log = logger("research_run");

export const REQUIRED = ["visual arts", "music", "writing", "film"];

type Finding = [claim: string, value: string, source: string, date: string];
export type Task = { scope: string; query: string };
export type SearchError = { type: string; query: string; partial: Finding[]; alternatives: string[]; tried: string[] };
export type Result = { status: "ok" | "error"; findings?: Finding[]; error?: SearchError; recovered_from?: string };
export type Done = Task & Result;

// what the web search subagent finds for a query
const SOURCES: Record<string, Finding[]> = {
  "AI in digital art": [["studios using generative tools", "60%", "survey-a", "2025-02-01"]],
  "AI in graphic design": [["designers using generative tools weekly", "48%", "survey-b", "2025-03-10"]],
  "AI in photography": [["agencies labelling generated images", "yes", "policy-c", "2024-11-20"]],
  "AI in music": [["labels licensing their catalogues for training", "3 of 5", "report-d", "2025-01-15"]],
  "AI in writing": [["publishers with an AI policy", "70%", "survey-e", "2025-04-02"]],
  "AI in film production": [["studios testing AI previsualisation", "4 of 6", "report-f", "2025-05-12"]],
};
const ALTERNATIVES: Record<string, string[]> = { "AI in film": ["AI in film production"] };
const PUBLISHED: Record<string, string> = { "survey-a": "2025-02-01", "report-d": "2025-01-15" }; // what the synthesis agent can check here

/** The search subagent. A failure is returned with its type, the query, the partial results and what to try instead. */
export function search(query: string, down: string[] = []): Result {
  if (down.includes(query) || !(query in SOURCES)) return { status: "error", error: { type: "timeout", query, partial: [], alternatives: ALTERNATIVES[query] ?? [], tried: [query] } };
  return { status: "ok", findings: SOURCES[query] };
}

export function coverage(plan: Task[]): [string[], string[]] {
  const covered = REQUIRED.filter((s) => plan.some((t) => t.scope === s));
  return [covered, REQUIRED.filter((s) => !covered.includes(s))];
}

/** The coordinator compares its plan with the scopes the question needs and adds a subtask for each scope the plan leaves out. */
export function replan(plan: Task[]): Task[] {
  return [...plan, ...coverage(plan)[1].map((s) => ({ scope: s, query: `AI in ${s}` }))];
}

/** One retry through an alternative that the error offered. A second failure stays a failure and keeps both queries. */
export function recover(result: Result, down: string[]): Result {
  const error = result.error;
  if (result.status === "ok" || !error || error.alternatives.length === 0) return result;
  const again = search(error.alternatives[0], down);
  if (again.status === "ok") return { ...again, recovered_from: error.query };
  return { status: "error", error: { ...error, tried: [...error.tried, error.alternatives[0]] } };
}

export function research(plan: Task[], down: string[] = []): Done[] {
  return plan.map((task) => ({ ...task, ...recover(search(task.query, down), down) }));
}

/** The synthesis agent's scoped tool: a date it can check here, and anything else goes back to the coordinator. */
export function verifyFact(kind: string, source: string, value: string): string {
  if (kind === "date") return PUBLISHED[source] === value ? "confirmed" : "mismatch";
  return "needs_search";
}

export function report(results: Done[]) {
  const covered = REQUIRED.filter((s) => results.some((r) => r.scope === s && r.status === "ok"));
  const notes = results.filter((r) => r.status === "error").map((r) => `${r.scope} not covered: timeout on ` + r.error!.tried.map((q) => `'${q}'`).join(" and on "));
  const findings = results.filter((r) => r.status === "ok").reduce((n, r) => n + r.findings!.length, 0);
  return { status: covered.length === REQUIRED.length ? "complete" : "partial", covered: covered.length, findings, errors: notes.length, notes: notes.length ? notes : ["nothing left uncovered"] };
}

function main() {
  const narrow: Task[] = ["AI in digital art", "AI in graphic design", "AI in photography"].map((query) => ({ scope: "visual arts", query }));
  let [covered, gaps] = coverage(narrow);
  console.log(`plan 1: ${narrow.length} subtasks, scopes covered ${covered.length} of ${REQUIRED.length}, gaps: ${gaps.join(", ")}`);
  const plan = replan(narrow);
  [covered, gaps] = coverage(plan);
  console.log(`plan 2: ${plan.length} subtasks, scopes covered ${covered.length} of ${REQUIRED.length}, gaps: ${gaps.join(", ") || "none"}`);
  const first = search("AI in film");
  const e = first.error!;
  console.log(`search '${e.query}': ${first.status} ${e.type}, ${e.partial.length} partial, alternative '${e.alternatives[0]}'`);
  const recovered = research(plan).find((r) => r.recovered_from)!;
  console.log(`recovered: '${recovered.recovered_from}' -> '${ALTERNATIVES[recovered.recovered_from!][0]}' scope ${recovered.scope}, ${recovered.findings!.length} finding`);
  const checks: [string, string, string][] = [["date", "survey-a", "2025-02-01"], ["date", "report-d", "2025-01-15"], ["statistic", "survey-a", "60%"]];
  const verdicts = checks.map((c) => verifyFact(...c));
  console.log(`verify_fact: ${verdicts.filter((v) => v === "confirmed").length} confirmed here, ${verdicts.filter((v) => v === "needs_search").length} sent back to the coordinator`);
  for (const [label, down] of [["all sources up", []], ["film search down for good", ["AI in film", "AI in film production"]]] as [string, string[]][]) {
    const r = report(research(plan, down));
    console.log(`report (${label}): status=${r.status}, covered=${r.covered}/${REQUIRED.length}, findings=${r.findings}, errors=${r.errors}`);
    console.log(`  note: ${r.notes.join("; ")}`);
  }
}

if (import.meta.main) main();
