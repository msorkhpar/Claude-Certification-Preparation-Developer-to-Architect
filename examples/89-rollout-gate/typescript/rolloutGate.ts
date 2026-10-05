import { logger } from "./logger.ts";
const log = logger("rollout_gate");
/**
 * Moving a system to a new model at scale: the calendar of retirements, the settings a new model refuses, a gate that a regression suite must pass, and a staged roll-out with a way back.
 *
 * The cases, costs, timings and counts are invented. The model names and dates are the ones the Claude documentation listed on 2026-10-04; the rules about settings are those of its migration guide for Claude Sonnet 5.5. Nothing here calls a model.
 */
export type Case = { id: string; segment: string; mustPass: boolean; oldOk: boolean; newOk: boolean; oldCost: number; newCost: number; newMs: number };
export type Request = { model: string; temperature: number | null; topP: number | null; topK: number | null; thinking: string; toolChoice: string; strict: boolean; prefill: boolean };

export const STAGES = [1, 5, 25, 100];
export const TARGET = "claude-sonnet-5-5";

export function daysUntil(today: string, when: string): number {
  const [y1, m1, d1] = today.split("-").map(Number);
  const [y2, m2, d2] = when.split("-").map(Number);
  return Math.round((Date.UTC(y2, m2 - 1, d2) - Date.UTC(y1, m1 - 1, d1)) / 86400000);
}

export function level(days: number): string {
  if (days < 0) return "retired";
  if (days <= 14) return "urgent";
  return days <= 60 ? "migrate now" : "watch";
}

/** One line per model, the nearest retirement first. A date marked tentative is a date that may move later. */
export function retirementStatus(models: [string, string, boolean][], today: string): string[] {
  const rows = models.map(([name, when, tentative]) => ({ days: daysUntil(today, when), name, tentative }));
  rows.sort((a, b) => a.days - b.days || (a.name < b.name ? -1 : a.name > b.name ? 1 : Number(a.tentative) - Number(b.tentative)));
  return rows.map((r) => `${r.name}: ${r.days} days, ${level(r.days)}${r.tentative ? " (tentative)" : ""}`);
}

/** The settings the target model refuses are removed or replaced, and each change is named. */
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

export function percentile(values: number[], p: number): number {
  const ordered = [...values].sort((a, b) => a - b);
  return ordered.length ? ordered[Math.floor((p * ordered.length + 99) / 100) - 1] : 0;
}

/** A go needs every check to pass; every check that fails adds a reason, in a fixed order. */
export function gate(cases: Case[], protectedSegments: Set<string>, maxCostUp: number, maxP95: number): { decision: string; reasons: string[] } {
  log.debug("gate input", cases);
  const reasons: string[] = [];
  const failed = cases.filter((c) => c.mustPass && !c.newOk).map((c) => c.id).sort();
  if (failed.length) reasons.push("must-pass failed: " + failed.join(", "));
  const lost = cases.filter((c) => c.oldOk && !c.newOk);
  const gained = cases.filter((c) => c.newOk && !c.oldOk);
  const hit = [...new Set(lost.filter((c) => protectedSegments.has(c.segment)).map((c) => c.segment))].sort();
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

/** Hold until the stage has enough requests, roll back to zero when errors pass the limit, otherwise go on. */
export function rolloutStep(stage: number, requests: number, errors: number, minRequests: number, maxErrorsPer1000: number): string {
  if (requests < minRequests) return `hold at ${stage}`;
  if (Math.floor((errors * 1000) / requests) > maxErrorsPer1000) return "rollback to 0";
  if (stage === STAGES[STAGES.length - 1]) return "complete";
  return `advance to ${STAGES[STAGES.indexOf(stage) + 1]}`;
}

export function suite(): Case[] {
  const rows: Case[] = [];
  [900, 950, 1000, 1100, 1200].forEach((ms, k) => rows.push({ id: `b${k + 1}`, segment: "billing", mustPass: true, oldOk: true, newOk: true, oldCost: 4, newCost: 5, newMs: ms }));
  [1500, 1600, 1700, 1800, 2100].forEach((ms, k) => rows.push({ id: `r${k + 1}`, segment: "refund", mustPass: k + 1 <= 2, oldOk: true, newOk: k + 1 !== 4, oldCost: 6, newCost: 8, newMs: ms }));
  for (let i = 1; i <= 10; i++) rows.push({ id: `f${i}`, segment: "faq", mustPass: false, oldOk: ![8, 9, 10].includes(i), newOk: i !== 10, oldCost: 2, newCost: 3, newMs: 500 + 20 * i + 80 * Math.floor(i / 2) });
  return rows;
}

function main(): void {
  const models: [string, string, boolean][] = [["claude-haiku-4-5-20251001", "2026-10-15", true], ["claude-sonnet-4-5-20250929", "2026-11-30", false], ["claude-opus-4-1-20250805", "2026-08-05", false]];
  console.log("retirement calendar on 2026-10-04:");
  for (const line of retirementStatus(models, "2026-10-04")) console.log("  " + line);
  const old: Request = { model: "claude-sonnet-4-5-20250929", temperature: 0.7, topP: 0.9, topK: null, thinking: "budget", toolChoice: "tool", strict: false, prefill: true };
  const [next, changes] = migrateRequest(old);
  console.log(`request for ${next.model}: ${changes.length} changes`);
  for (const change of changes) console.log("  " + change);
  const cases = suite();
  const first = gate(cases, new Set(["refund"]), 25, 2000);
  console.log(`gate on ${cases.length} cases: ${first.decision}`);
  for (const reason of first.reasons) console.log("  " + reason);
  const fixed = cases.map((c) => (c.id === "r4" ? { ...c, newOk: true } : c));
  const second = gate(fixed, new Set(["refund"]), 40, 2000);
  console.log(`gate after the refund fix, cost limit 40%: ${second.decision}, ${second.reasons.length} reasons`);
  for (const [stage, requests, errors] of [[1, 2000, 6], [5, 300, 0], [5, 10000, 20], [25, 50000, 400], [100, 50000, 10]]) {
    console.log(`roll-out at ${stage}% with ${requests} requests and ${errors} errors: ${rolloutStep(stage, requests, errors, 1000, 5)}`);
  }
}

if (import.meta.main) main();
