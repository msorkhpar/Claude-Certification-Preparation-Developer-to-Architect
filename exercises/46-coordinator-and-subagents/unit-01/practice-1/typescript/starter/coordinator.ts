// A coordinator that delegates to isolated subagents, reviews what comes back and synthesizes. See ../../statement.md.
import { logger } from "../logger.ts";
const log = logger("coordinator");
type Finding = { scope: string; text: string };

// TODO 1 of 8 (unlocks e3): why a subtask is dropped, or null to keep it.
// Receives the brief, the lower-cased scope key, the keys already kept, how many are kept and the limit. Returns "empty brief" for a blank
// brief, else "duplicate scope" for a key already seen, else "over limit" when keptCount has reached maxAgents, else null.
// Example: dropReason("  ", "a", new Set(), 0, 3) -> "empty brief"
function dropReason(brief: string, key: string, seen: Set<string>, keptCount: number, maxAgents: number): string | null {
  return null;
}

// TODO 2 of 8 (unlocks e4): what is wrong with a subagent's report, or null.
// Receives whatever the subagent returned. Returns "empty report" when it is not a string or only blank, else null.
// Example: reportProblem("   ") -> "empty report"
function reportProblem(report: unknown): string | null {
  return null;
}

// TODO 3 of 8 (unlocks e5): the brief for a follow-up on one gap.
// Returns "Follow up: <gap>" then a newline then "Question: <question>". Example: followUp("2023", "q") -> "Follow up: 2023\nQuestion: q"
function followUp(gap: string, question: string): string {
  return gap;
}

// TODO 4 of 8 (unlocks e5 and e6): is another refinement round allowed?
// True while there are gaps and fewer than maxRounds rounds have run. Example: mayRefine(["x"], 2, 2) -> false
function mayRefine(gaps: string[], rounds: number, maxRounds: number): boolean {
  return false;
}

// TODO 5 of 8 (unlocks m1 and e6): the status of a run that has an answer.
// "complete" when no gaps remain and nothing failed, else "partial". Example: finalStatus(["x"], []) -> "partial"
function finalStatus(gaps: string[], failed: unknown[]): string {
  return "complete";
}

// TODO 6 of 8 (unlocks e1): does the plan ask for subagents?
// Receives the planner's object. True when its delegate value is truthy, else false. Example: wantsTeam({ delegate: false }) -> false
function wantsTeam(plan: any): boolean {
  return true;
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

// TODO 7 of 8 (unlocks e5 and e6): the reviewer's gaps, tidied.
// Receives the reviewer's list (or anything else) and the limit. Returns the gaps trimmed, without empty or repeated ones, in order, at most
// maxAgents of them. Example: cleanGaps([" a ", "", "a", "b"], 1) -> ["a"]
function cleanGaps(gaps: unknown, maxAgents: number): string[] {
  return Array.isArray(gaps) ? gaps.map(String) : [];
}

// TODO 8 of 8 (unlocks m1 and e2): the text a subagent is handed.
// Receives the subtask's brief. Returns exactly what the subagent is told: that brief and nothing else (no scope list, no earlier findings).
// Example: briefOf("cars: find 2024 car output news") -> "cars: find 2024 car output news"
function briefOf(brief: string): string {
  return "";
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
