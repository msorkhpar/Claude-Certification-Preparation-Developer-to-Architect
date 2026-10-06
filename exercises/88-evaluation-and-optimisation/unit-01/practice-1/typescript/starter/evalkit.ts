/** Evaluation kit: a report by segment, a latency percentile, an A/B verdict, a shadow-run gate, a diagnosis order and a model choice under limits. See ../../statement.md. */
import { logger } from "../logger.ts";
const log = logger("evalkit");

export type Line = [segment: string, cases: number, right: number, percent: number, cost: number];
export type Pair = [segment: string, oldOk: boolean, newOk: boolean];
export type Option = [name: string, accuracy: number, p95: number, cost: number];
export type Gate = { decision: string; lost: number; gained: number; blocked: string[] };

/** Whole percent, half up, integers only (written for you). */
export function pct(part: number, whole: number): number {
  return whole ? Math.floor((200 * part + whole) / (2 * whole)) : 0;
}

/**
 * TODO 1 of 7 (unlocks m1 and e1): the cost of the wrong answers of one segment.
 * Receives the number of wrong answers, the segment name and the costs by segment. Returns wrong times the segment's cost, 1 when the
 * segment has no entry. Example: errorCost(3, "refund", { refund: 20 }) -> 60, errorCost(2, "odd", { refund: 20 }) -> 2
 */
function errorCost(wrong: number, segment: string, costs: Record<string, number>): number {
  return 0;
}

/**
 * TODO 2 of 7 (unlocks m1 and e1): order the report lines.
 * Receives the lines [segment, cases, right, percent, cost]. Returns them with the highest cost first and equal costs by segment name.
 * Example: order([["a", 1, 1, 100, 0], ["b", 2, 1, 50, 5]]) -> [["b", 2, 1, 50, 5], ["a", 1, 1, 100, 0]]
 */
function order(table: Line[]): Line[] {
  return table;
}

export function segmentTable(results: [string, boolean][], costs: Record<string, number>): Line[] {
  log.debug("segmentTable input", results);
  const seen = new Map<string, [number, number]>();
  for (const [segment, correct] of results) {
    const [total, right] = seen.get(segment) ?? [0, 0];
    seen.set(segment, [total + 1, right + (correct ? 1 : 0)]);
  }
  return order([...seen.entries()].map(([s, [n, r]]): Line => [s, n, r, pct(r, n), errorCost(n - r, s, costs)]));
}

/**
 * TODO 3 of 7 (unlocks e2): the nearest-rank percentile.
 * Receives the values in any order and p from 1 to 100. Returns the value at rank ceil(p * n / 100) of the sorted values, counting from 1,
 * and 0 for no values. Example: percentile([4800, 800, 1000, 900], 95) -> 4800
 */
export function percentile(values: number[], p: number): number {
  return -1;
}

/**
 * TODO 4 of 7 (unlocks e3 and e4): the A/B verdict at 95 percent.
 * Receives the right answers and cases of the old version (x1 of n1) and the new one (x2 of n2). Returns "too few cases" when an arm has
 * fewer than minN cases, "no clear difference" when the pooled right answers are 0 or all, otherwise the integer test from the statement:
 * "new is better", "old is better" or "no clear difference". Example: abVerdict(410, 500, 438, 500) -> "new is better"
 */
export function abVerdict(x1: number, n1: number, x2: number, n2: number, minN = 200): string {
  return "";
}

/**
 * TODO 5 of 7 (unlocks e5): ship or hold.
 * Receives the protected segments that lost, the number lost and the number gained. Returns "hold" when a protected segment lost or more
 * were lost than gained, "ship" otherwise. Example: decision([], 2, 2) -> "ship", decision(["refund"], 1, 5) -> "hold"
 */
function decision(blocked: string[], lost: number, gained: number): string {
  return "";
}

export function shadowGate(pairs: Pair[], protectedSegments: string[]): Gate {
  const lost = pairs.filter(([, o, n]) => o && !n).map((p) => p[0]);
  const gained = pairs.filter(([, o, n]) => n && !o).length;
  const blocked = [...new Set(lost.filter((s) => protectedSegments.includes(s)))].sort();
  return { decision: decision(blocked, lost.length, gained), lost: lost.length, gained, blocked };
}

/**
 * TODO 6 of 7 (unlocks e6): where to look first for a wrong answer.
 * Receives four booleans. Returns "retrieval or data" when the evidence was not found, then "ungrounded answer" when it is not supported,
 * then "format instructions" when the format is wrong, then "prompt or task" when it fails on a stronger model, else "model mismatch".
 * Example: diagnose(true, true, false, true) -> "format instructions"
 */
export function diagnose(found: boolean, supported: boolean, formatOk: boolean, passesOnStronger: boolean): string {
  return "";
}

/**
 * TODO 7 of 7 (unlocks e7): the cheapest model that meets both limits.
 * Receives options [name, accuracy, p95, cost]. Returns the name of the cheapest one with accuracy >= minAccuracy and p95 <= maxP95,
 * equal costs by name, or "none". Example: chooseModel([["a", 90, 100, 2]], 95, 100) -> "none"
 */
export function chooseModel(options: Option[], minAccuracy: number, maxP95: number): string {
  return "";
}
