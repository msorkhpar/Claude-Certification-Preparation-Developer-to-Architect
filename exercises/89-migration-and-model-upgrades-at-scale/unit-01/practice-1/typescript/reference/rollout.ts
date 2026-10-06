/** Rollout kit: the retirement calendar, the settings a new model refuses, a gate on a regression suite and a staged roll-out. See ../../statement.md. */
import { logger } from "../logger.ts";
const log = logger("rollout");

export type Case = { id: string; segment: string; mustPass: boolean; oldOk: boolean; newOk: boolean; oldCost: number; newCost: number; newMs: number };
export type Request = { model: string; temperature: number | null; topP: number | null; topK: number | null; thinking: string; toolChoice: string; strict: boolean; prefill: boolean };

export const STAGES = [1, 5, 25, 100];
export const TARGET = "claude-sonnet-5-5";

/** Whole days from one ISO date to another, negative when it has passed (written for you). */
export function daysUntil(today: string, when: string): number {
  const [y1, m1, d1] = today.split("-").map(Number);
  const [y2, m2, d2] = when.split("-").map(Number);
  return Math.round((Date.UTC(y2, m2 - 1, d2) - Date.UTC(y1, m1 - 1, d1)) / 86400000);
}

/** Nearest-rank percentile, 0 for no values (written for you). */
export function percentile(values: number[], p: number): number {
  const ordered = [...values].sort((a, b) => a - b);
  return ordered.length ? ordered[Math.floor((p * ordered.length + 99) / 100) - 1] : 0;
}

function statusLine(days: number, name: string, tentative: boolean): string {
  const level = days < 0 ? "retired" : days <= 14 ? "urgent" : days <= 60 ? "migrate now" : "watch";
  return `${name}: ${days} days, ${level}${tentative ? " (tentative)" : ""}`;
}

export function retirementStatus(models: [string, string, boolean][], today: string): string[] {
  log.debug("retirementStatus input", models);
  const rows = models.map(([name, when, tentative]) => ({ days: daysUntil(today, when), name, tentative }));
  rows.sort((a, b) => a.days - b.days || (a.name < b.name ? -1 : a.name > b.name ? 1 : Number(a.tentative) - Number(b.tentative)));
  return rows.map((r) => statusLine(r.days, r.name, r.tentative));
}

function migrateThinking(thinking: string, changes: string[]): string {
  if (thinking === "budget") {
    changes.push("thinking budget replaced by adaptive thinking; sweep the effort");
    return "adaptive";
  }
  if (thinking === "disabled") {
    changes.push("thinking disabled replaced by between_tools");
    return "between_tools";
  }
  return thinking;
}

export function migrateRequest(request: Request, target = TARGET): [Request, string[]] {
  const changes: string[] = [];
  if (request.model !== target) changes.push(`model set to ${target}`);
  if (request.temperature !== null) changes.push("removed temperature");
  if (request.topP !== null) changes.push("removed top_p");
  if (request.topK !== null) changes.push("removed top_k");
  const thinking = migrateThinking(request.thinking, changes);
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

function mustPass(cases: Case[]): string[] {
  const failed = cases.filter((c) => c.mustPass && !c.newOk).map((c) => c.id).sort();
  return failed.length ? ["must-pass failed: " + failed.join(", ")] : [];
}

function protectedLost(cases: Case[], protectedSegments: string[]): string[] {
  const hit = [...new Set(cases.filter((c) => c.oldOk && !c.newOk && protectedSegments.includes(c.segment)).map((c) => c.segment))].sort();
  return hit.length ? ["protected segment lost answers: " + hit.join(", ")] : [];
}

function netLoss(cases: Case[]): string[] {
  const lost = cases.filter((c) => c.oldOk && !c.newOk).length;
  const gained = cases.filter((c) => c.newOk && !c.oldOk).length;
  return lost > gained ? [`net loss: lost ${lost}, gained ${gained}`] : [];
}

function costRise(cases: Case[], maxCostUp: number): string[] {
  const oldTotal = cases.reduce((a, c) => a + c.oldCost, 0);
  const newTotal = cases.reduce((a, c) => a + c.newCost, 0);
  const up = oldTotal > 0 && newTotal > oldTotal ? Math.floor(((newTotal - oldTotal) * 100) / oldTotal) : 0;
  return up > maxCostUp ? [`cost up ${up}% over the ${maxCostUp}% limit`] : [];
}

function latency(cases: Case[], maxP95: number): string[] {
  const p95 = percentile(cases.map((c) => c.newMs), 95);
  return p95 > maxP95 ? [`p95 latency ${p95} ms over the ${maxP95} ms limit`] : [];
}

function decision(reasons: string[]): string {
  return reasons.length ? "no-go" : "go";
}

export function gate(cases: Case[], protectedSegments: string[], maxCostUp: number, maxP95: number): { decision: string; reasons: string[] } {
  const reasons = [...mustPass(cases), ...protectedLost(cases, protectedSegments), ...netLoss(cases), ...costRise(cases, maxCostUp), ...latency(cases, maxP95)];
  return { decision: decision(reasons), reasons };
}

export function rolloutStep(stage: number, requests: number, errors: number, minRequests: number, maxErrorsPer1000: number): string {
  if (requests < minRequests) return `hold at ${stage}`;
  if (Math.floor((errors * 1000) / requests) > maxErrorsPer1000) return "rollback to 0";
  if (stage === STAGES[STAGES.length - 1]) return "complete";
  return `advance to ${STAGES[STAGES.indexOf(stage) + 1]}`;
}
