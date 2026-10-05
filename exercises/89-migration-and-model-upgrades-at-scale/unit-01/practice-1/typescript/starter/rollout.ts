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

/**
 * TODO 1 of 9 (unlocks e7): one line of the retirement calendar.
 * Receives the days left, the model name and whether the date is tentative. Returns `<name>: <days> days, <level>` with ` (tentative)`
 * added when it is tentative; the level is `retired` below 0, `urgent` up to 14, `migrate now` up to 60, else `watch`.
 * Example: statusLine(14, "a", true) -> "a: 14 days, urgent (tentative)"
 */
function statusLine(days: number, name: string, tentative: boolean): string {
  return "";
}

export function retirementStatus(models: [string, string, boolean][], today: string): string[] {
  log.debug("retirementStatus input", models);
  const rows = models.map(([name, when, tentative]) => ({ days: daysUntil(today, when), name, tentative }));
  rows.sort((a, b) => a.days - b.days || (a.name < b.name ? -1 : a.name > b.name ? 1 : Number(a.tentative) - Number(b.tentative)));
  return rows.map((r) => statusLine(r.days, r.name, r.tentative));
}

/**
 * TODO 2 of 9 (unlocks e8): migrate the thinking setting.
 * Receives the thinking setting and the list of changes so far. `budget` becomes `adaptive` and `disabled` becomes `between_tools`, each
 * pushing its sentence from the statement onto `changes`; anything else is kept. Returns the new setting.
 * Example: migrateThinking("disabled", changes) -> "between_tools", and changes gains "thinking disabled replaced by between_tools"
 */
function migrateThinking(thinking: string, changes: string[]): string {
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

/**
 * TODO 3 of 9 (unlocks e1): the reasons for failed must-pass cases.
 * Receives the cases. Returns an array with `must-pass failed: <ids>` (sorted, joined by `, `) for the cases marked must pass that the new
 * model fails, or an empty array. Example: one failing must-pass case a1 -> ["must-pass failed: a1"]
 */
function mustPass(cases: Case[]): string[] {
  return [];
}

/**
 * TODO 4 of 9 (unlocks e2): the reason for a protected segment that lost answers.
 * Receives the cases and the protected segments. Returns an array with `protected segment lost answers: <segments>` (sorted, distinct)
 * for the protected segments with a case the old model got right and the new one did not, or an empty array.
 * Example: refund lost b3 -> ["protected segment lost answers: refund"]
 */
function protectedLost(cases: Case[], protectedSegments: string[]): string[] {
  return [];
}

/**
 * TODO 5 of 9 (unlocks e3): the reason for more losses than gains.
 * Receives the cases. Returns an array with `net loss: lost N, gained M` when the cases lost (old right, new wrong) outnumber the cases
 * gained (the opposite), or an empty array. Example: 2 lost and 1 gained -> ["net loss: lost 2, gained 1"]
 */
function netLoss(cases: Case[]): string[] {
  return [];
}

/**
 * TODO 6 of 9 (unlocks e4): the reason for a cost rise over the limit.
 * Receives the cases and the largest allowed rise in whole percent. The rise is the new total cost over the old one, rounded down, 0 when
 * the old total is 0 or the cost fell. Returns an array with `cost up X% over the Y% limit` when X is above Y, or an empty array.
 * Example: costs 40 -> 50 with a limit of 20 -> ["cost up 25% over the 20% limit"]; costs 40 -> 48 with a limit of 20 -> []
 */
function costRise(cases: Case[], maxCostUp: number): string[] {
  return [];
}

/**
 * TODO 7 of 9 (unlocks e5): the reason for a slow tail.
 * Receives the cases and the largest allowed 95th-percentile time in ms. Uses `percentile` over the `newMs` of the cases. Returns an array
 * with `p95 latency X ms over the Y ms limit` when X is above Y, or an empty array. Example: p95 3000 with a limit of 2000 -> one reason
 */
function latency(cases: Case[], maxP95: number): string[] {
  return [];
}

/**
 * TODO 8 of 9 (unlocks m1): the decision of the gate.
 * Receives the array of reasons. Returns "no-go" when there is any reason, "go" when there is none. Example: decision([]) -> "go"
 */
function decision(reasons: string[]): string {
  return "";
}

export function gate(cases: Case[], protectedSegments: string[], maxCostUp: number, maxP95: number): { decision: string; reasons: string[] } {
  const reasons = [...mustPass(cases), ...protectedLost(cases, protectedSegments), ...netLoss(cases), ...costRise(cases, maxCostUp), ...latency(cases, maxP95)];
  return { decision: decision(reasons), reasons };
}

/**
 * TODO 9 of 9 (unlocks e6): the step of a staged roll-out.
 * Receives the stage (1, 5, 25 or 100), the requests and errors seen, the fewest requests to judge and the most errors per 1000.
 * Returns "hold at <stage>" with too few requests, "rollback to 0" when errors per 1000 (rounded down) pass the limit, "complete" at
 * stage 100 when healthy, else "advance to <next stage>". Example: rolloutStep(1, 2000, 6, 1000, 5) -> "advance to 5"
 */
export function rolloutStep(stage: number, requests: number, errors: number, minRequests: number, maxErrorsPer1000: number): string {
  return "";
}
