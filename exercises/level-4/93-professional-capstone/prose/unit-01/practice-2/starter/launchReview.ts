/** Launch review: the findings of a design against the seven domains, the verdict, the scorecard and the accuracy a design needs. See ../../statement.md. */
import { logger } from "./logger.ts";
const log = logger("launch_review");

const SEVERITY: Record<string, number> = { high: 0, medium: 1, low: 2 };
const DOMAINS = ["P1", "P2", "P3", "P4", "P5", "P6", "P7"];

type Add = (condition: boolean, severity: string, domain: string, rule: string) => void;
type Numbers = Record<string, number>;

/**
 * TODO 1 of 8 (unlocks e1, e4 and e7): the rules of domain P1.
 * Receives the flags and the numbers and reports each rule it finds through `add`. The rules: missing-feedback, autonomy-without-need and team-below-price from the statement's table. Example: missing-feedback: the flag feedback_loop is absent -> one finding `high P1 missing-feedback`.
 */
function p1Rules(f: Set<string>, n: Numbers, add: Add): void {
}

/**
 * TODO 2 of 8 (unlocks e4 and e8): the rules of domain P3.
 * Receives the flags and the numbers and reports each rule it finds through `add`. The rules: filter-after-ranking, stale-index, tool-bloat and agent-rights-only. Example: stale-index: the flag replace_on_change is absent -> one finding `high P3 stale-index`.
 */
function p3Rules(f: Set<string>, n: Numbers, add: Add): void {
}

/**
 * TODO 3 of 8 (unlocks e1, e3 and e4): the rules of domain P4.
 * Receives the flags and the numbers and reports each rule it finds through `add`. The rules: no-protected-segment, small-eval-set, no-way-back and big-bang-rollout. Example: no-way-back: the flag rollback is absent -> one finding `high P4 no-way-back`.
 */
function p4Rules(f: Set<string>, n: Numbers, add: Add): void {
}

/**
 * TODO 4 of 8 (unlocks e4, e7 and e8): the rules of domain P5.
 * Receives the flags and the numbers and reports each rule it finds through `add`. The rules: identifiers-reach-model, residency-unmet, audit-keeps-content, irreversible-without-person and retention-outside-window. Example: irreversible-without-person: irreversible_action present and human_step absent -> `high P5 irreversible-without-person`.
 */
function p5Rules(f: Set<string>, n: Numbers, add: Add): void {
}

function p2Rules(f: Set<string>, n: Numbers, add: Add): void {
  add(f.has("volatile_prefix"), "medium", "P2", "volatile-prefix");
  add(!f.has("model_measured"), "low", "P2", "model-not-measured");
}

function p6Rules(f: Set<string>, n: Numbers, add: Add): void {
  add(!f.has("owner"), "medium", "P6", "no-accountable-owner");
  add((n["latency_ms"] ?? 0) <= 0 || (n["availability_tenths"] ?? 0) <= 0, "medium", "P6", "sla-without-numbers");
  add(!f.has("accuracy_stated"), "low", "P6", "accuracy-unstated");
}

function p7Rules(f: Set<string>, n: Numbers, add: Add): void {
  add((n["team_size"] ?? 0) > 10 && !f.has("managed_settings"), "medium", "P7", "unmanaged-team-settings");
}

/**
 * TODO 5 of 8 (unlocks e3): order the findings.
 * Receives the findings. Returns them ordered by severity (high, medium, low), then by domain, then by rule id in alphabetical order. Example: low P2 a, high P6 z, high P1 y -> high P1 y, high P6 z, low P2 a
 */
function order(found: string[]): string[] {
  return found;
}

/** The findings, each "<severity> <domain> <rule>", high first, then by domain, then by rule. */
export function launchReview(flags: Set<string>, numbers: Record<string, number>): string[] {
  log.debug("launchReview input", [...flags].sort());
  const f = new Set(flags);
  const n = { ...numbers };
  const found: string[] = [];
  const add: Add = (condition, severity, domain, rule) => {
    if (condition) found.push(`${severity} ${domain} ${rule}`);
  };
  for (const rules of [p1Rules, p2Rules, p3Rules, p4Rules, p5Rules, p6Rules, p7Rules]) rules(f, n, add);
  return order(found);
}

/**
 * TODO 6 of 8 (unlocks e1 and e2): the verdict.
 * Receives the findings. Returns `reject` when any is high, `revise` when none is high and any is medium, `approve` otherwise. Example: [low P2 a] -> approve
 */
export function verdict(findings: string[]): string {
  return "";
}

/**
 * TODO 7 of 8 (unlocks e5): the scorecard.
 * Receives the findings. Returns the seven counts of the findings of P1 to P7 in that order. Example: [high P1 a, low P7 c] -> [1, 0, 0, 0, 0, 0, 1]
 */
export function scorecard(findings: string[]): number[] {
  return [];
}

/**
 * TODO 8 of 8 (unlocks m1 and e6): the accuracy a design needs.
 * Receives the cost of an error and the cost of a check. Returns 100 minus the check cost as a percent of the error cost, the percent rounded up, never below 0, and 0 when an error costs nothing or less. Example: neededAccuracy(250, 5) -> 98
 */
export function neededAccuracy(errorCost: number, reviewCost: number): number {
  return -1;
}
