/** Rollout kit: the retirement calendar, the settings a new model refuses, a gate on a regression suite and a staged roll-out. See ../../statement.md. */
export type Case = { id: string; segment: string; mustPass: boolean; oldOk: boolean; newOk: boolean; oldCost: number; newCost: number; newMs: number };
export type Request = { model: string; temperature: number | null; topP: number | null; topK: number | null; thinking: string; toolChoice: string; strict: boolean; prefill: boolean };

export const STAGES = [1, 5, 25, 100];
export const TARGET = "claude-sonnet-5-5";

export function daysUntil(today: string, when: string): number {
  const [y1, m1, d1] = today.split("-").map(Number);
  const [y2, m2, d2] = when.split("-").map(Number);
  return Math.round((Date.UTC(y2, m2 - 1, d2) - Date.UTC(y1, m1 - 1, d1)) / 86400000);
}

export function percentile(values: number[], p: number): number {
  const ordered = [...values].sort((a, b) => a - b);
  return ordered.length ? ordered[Math.floor((p * ordered.length + 99) / 100) - 1] : 0;
}

export function retirementStatus(models: [string, string, boolean][], today: string): string[] {
  const rows = models.map(([name, when, tentative]) => ({ days: daysUntil(today, when), name, tentative }));
  rows.sort((a, b) => a.days - b.days || (a.name < b.name ? -1 : a.name > b.name ? 1 : Number(a.tentative) - Number(b.tentative)));
  return rows.map((r) => {
    const level = r.days < 0 ? "retired" : r.days <= 14 ? "urgent" : r.days <= 60 ? "migrate now" : "watch";
    return `${r.name}: ${r.days} days, ${level}${r.tentative ? " (tentative)" : ""}`;
  });
}

export function migrateRequest(request: Request, target = TARGET): [Request, string[]] {
  const changes: string[] = [];
  if (request.model !== target) changes.push(`model set to ${target}`);
  if (request.temperature !== null) changes.push("removed temperature");
  if (request.topP !== null) changes.push("removed top_p");
  if (request.topK !== null) changes.push("removed top_k");
  let thinking = request.thinking;
  if (thinking === "budget") {
    thinking = "adaptive";
    changes.push("thinking budget replaced by adaptive thinking; sweep the effort");
  } else if (thinking === "disabled") {
    thinking = "between_tools";
    changes.push("thinking disabled replaced by between_tools");
  }
  let toolChoice = request.toolChoice;
  let strict = request.strict;
  if (toolChoice === "any" || toolChoice === "tool") {
    toolChoice = "auto";
    strict = true;
    changes.push("forced tool choice replaced by auto with strict tools");
  }
  if (request.prefill) changes.push("assistant prefill removed; state the format in the instructions");
  return [{ model: target, temperature: null, topP: null, topK: null, thinking, toolChoice, strict, prefill: false }, changes];
}

export function gate(cases: Case[], protectedSegments: string[], maxCostUp: number, maxP95: number): { decision: string; reasons: string[] } {
  const reasons: string[] = [];
  const failed = cases.filter((c) => c.mustPass && !c.newOk).map((c) => c.id).sort();
  if (failed.length) reasons.push("must-pass failed: " + failed.join(", "));
  const lost = cases.filter((c) => c.oldOk && !c.newOk);
  const gained = cases.filter((c) => c.newOk && !c.oldOk);
  const hit = [...new Set(lost.filter((c) => protectedSegments.includes(c.segment)).map((c) => c.segment))].sort();
  if (hit.length) reasons.push("protected segment lost answers: " + hit.join(", "));
  if (lost.length > gained.length) reasons.push(`net loss: lost ${lost.length}, gained ${gained.length}`);
  const oldTotal = cases.reduce((a, c) => a + c.oldCost, 0);
  const newTotal = cases.reduce((a, c) => a + c.newCost, 0);
  const up = oldTotal > 0 && newTotal > oldTotal ? Math.floor(((newTotal - oldTotal) * 100) / oldTotal) : 0;
  if (up > maxCostUp) reasons.push(`cost up ${up}% over the ${maxCostUp}% limit`);
  const p95 = percentile(cases.map((c) => c.newMs), 95);
  if (p95 > maxP95) reasons.push(`p95 latency ${p95} ms over the ${maxP95} ms limit`);
  return { decision: reasons.length ? "no-go" : "go", reasons };
}

export function rolloutStep(stage: number, requests: number, errors: number, minRequests: number, maxErrorsPer1000: number): string {
  if (requests < minRequests) return `hold at ${stage}`;
  if (Math.floor((errors * 1000) / requests) > maxErrorsPer1000) return "rollback to 0";
  if (stage === STAGES[STAGES.length - 1]) return "complete";
  return `advance to ${STAGES[STAGES.indexOf(stage) + 1]}`;
}
