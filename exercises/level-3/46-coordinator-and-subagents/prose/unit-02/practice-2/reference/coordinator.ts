// A coordinator that delegates to isolated subagents, reviews what comes back and synthesizes. See ../../statement.md.
import { logger } from "./logger.ts";
const log = logger("coordinator");
type Finding = { scope: string; text: string };

function dropReason(brief: string, key: string, seen: Set<string>, keptCount: number, maxAgents: number): string | null {
  if (brief.trim() === "") return "empty brief";
  if (seen.has(key)) return "duplicate scope";
  if (keptCount >= maxAgents) return "over limit";
  return null;
}

function reportProblem(report: unknown): string | null {
  return typeof report !== "string" || report.trim() === "" ? "empty report" : null;
}

function followUp(gap: string, question: string): string {
  return `Follow up: ${gap}\nQuestion: ${question}`;
}

function mayRefine(gaps: string[], rounds: number, maxRounds: number): boolean {
  return gaps.length > 0 && rounds < maxRounds;
}

function finalStatus(gaps: string[], failed: unknown[]): string {
  return gaps.length === 0 && failed.length === 0 ? "complete" : "partial";
}

function wantsTeam(plan: any): boolean {
  return Boolean(plan.delegate);
}

function clean(subtasks: Array<{ scope?: unknown; brief?: unknown }>, maxAgents: number) {
  const kept: Array<{ scope: string; brief: string }> = [];
  const dropped: Array<{ scope: string; reason: string }> = [];
  const seen = new Set<string>();
  for (const task of subtasks) {
    const scope = String(task.scope ?? "");
    const brief = String(task.brief ?? "");
    const key = scope.trim().toLowerCase();
    const reason = dropReason(brief, key, seen, kept.length, maxAgents);
    if (reason) dropped.push({ scope, reason });
    else {
      seen.add(key);
      kept.push({ scope, brief });
    }
  }
  return { kept, dropped };
}

function cleanGaps(gaps: unknown, maxAgents: number): string[] {
  const out: string[] = [];
  for (const gap of Array.isArray(gaps) ? gaps : []) {
    const text = String(gap).trim();
    if (text !== "" && !out.includes(text)) out.push(text);
  }
  return out.slice(0, maxAgents);
}

/** What a subagent is told: its own brief and nothing the others found. */
function briefOf(brief: string): string {
  return brief;
}

export function coordinate(
  planner: (question: string) => any,
  subagent: (brief: string) => string,
  reviewer: (question: string, findings: Finding[]) => string[],
  synthesizer: (question: string, findings: Finding[]) => string,
  question: string,
  maxAgents = 4,
  maxRounds = 2,
): any {
  log.debug("coordinate input", question);
  const plan = planner(question);
  if (!wantsTeam(plan)) { // a question the coordinator can answer itself is not worth a team
    return { status: "direct", answer: plan.answer ?? null, findings: [], failed: [], dropped: [], gaps: [], rounds: 0, subagent_calls: 0 };
  }
  const { kept, dropped } = clean(plan.subtasks ?? [], maxAgents);
  const findings: Finding[] = [];
  const failed: Array<{ scope: string; error: string }> = [];
  let calls = 0;

  const run = (scope: string, brief: string) => {
    calls += 1;
    let report: unknown;
    try {
      report = subagent(brief); // the brief is everything the subagent knows
    } catch (error) { // one subagent failing must not stop the others
      failed.push({ scope, error: error instanceof Error ? error.message : String(error) });
      return;
    }
    const problem = reportProblem(report);
    if (problem) failed.push({ scope, error: problem });
    else findings.push({ scope, text: report });
  };

  for (const task of kept) run(task.scope, briefOf(task.brief));
  if (findings.length === 0) return { status: "failed", answer: null, findings: [], failed, dropped, gaps: [], rounds: 0, subagent_calls: calls };
  let rounds = 0;
  let gaps = cleanGaps(reviewer(question, findings.map((f) => ({ ...f }))), maxAgents);
  while (mayRefine(gaps, rounds, maxRounds)) {
    rounds += 1;
    for (const gap of gaps) run(gap, followUp(gap, question));
    gaps = cleanGaps(reviewer(question, findings.map((f) => ({ ...f }))), maxAgents);
  }
  const answer = synthesizer(question, findings.map((f) => ({ ...f })));
  const status = finalStatus(gaps, failed);
  return { status, answer, findings, failed, dropped, gaps, rounds, subagent_calls: calls };
}
