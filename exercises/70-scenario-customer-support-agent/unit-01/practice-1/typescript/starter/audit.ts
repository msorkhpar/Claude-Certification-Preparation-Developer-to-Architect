// Audit a support agent's recorded sessions: the rates, the failure shapes and the first fix.
import { logger } from "../logger.ts";
const log = logger("audit");

export type Step = { tool: string; ok: boolean; right_tool?: string };
export type Session = { id: string; steps: Step[]; outcome: string; needs_human: boolean; refund_cents: number; limit_cents: number };
export type Report = {
  sessions: number; resolved: number; fcr: number; meets_target: boolean; over_escalated: number; under_escalated: number;
  skipped_prerequisite: number; wrong_tool: number; over_limit_refunds: number; diagnosis: string;
};

const PROTECTED = ["lookup_order", "process_refund"];
const TARGET = 0.8;

/**
 * TODO 1 of 5 (unlocks e2): did an order or refund call come before the first successful get_customer call?
 * Receives a session's steps. Returns true or false; a failed get_customer does not identify anyone.
 * Example: [lookup_order, get_customer] -> true, [get_customer, lookup_order] -> false
 */
export function skippedPrerequisite(steps: Step[]): boolean {
  return false;
}

/**
 * TODO 2 of 5 (unlocks e6): does a step have a known right tool that differs from the tool used?
 * Receives a session's steps (a step may lack right_tool). Returns true for at least one such step, false otherwise.
 * Example: [{ tool: "get_customer", ok: true, right_tool: "lookup_order" }] -> true; a step with no right_tool never counts.
 */
export function hasWrongTool(steps: Step[]): boolean {
  return false;
}

/**
 * TODO 3 of 5 (unlocks e3): was a refund above the limit actually made?
 * Receives one session. Returns true only when its outcome is "resolved" and refund_cents is above limit_cents.
 * Example: resolved, refund 10001, limit 10000 -> true; escalated with the same refund -> false
 */
export function isOverLimit(session: Session): boolean {
  return false;
}

/**
 * TODO 4 of 5 (unlocks e1 and e5): the first contact rate.
 * Receives the resolved count and the session count. Returns resolved / n rounded to three decimals, 0 when n is 0.
 * Example: firstContactRate(2, 3) -> 0.667
 */
export function firstContactRate(resolved: number, n: number): number {
  return 0;
}

/**
 * TODO 5 of 5 (unlocks e4 and e1): the first fix, by the order in the statement.
 * Receives the counts of skipped prerequisites, over-limit refunds, wrong-tool sessions, over- and under-escalations.
 * Returns "enforce_in_code", "rewrite_tool_descriptions", "write_escalation_criteria" or "none".
 * Example: diagnose(0, 0, 2, 1, 0) -> "rewrite_tool_descriptions"
 */
export function diagnose(skipped: number, overLimit: number, wrong: number, over: number, under: number): string {
  return "";
}

export function audit(sessions: Session[]): Report {
  log.debug("audit input", sessions);
  const n = sessions.length;
  const count = (f: (s: Session) => boolean) => sessions.filter(f).length;
  const resolved = count((s) => s.outcome === "resolved");
  const over = count((s) => s.outcome === "escalated" && !s.needs_human);
  const under = count((s) => s.outcome === "resolved" && s.needs_human);
  const skipped = count((s) => skippedPrerequisite(s.steps));
  const wrong = count((s) => hasWrongTool(s.steps));
  const overLimit = count((s) => isOverLimit(s));
  const fcr = firstContactRate(resolved, n);
  return { sessions: n, resolved, fcr, meets_target: fcr >= TARGET, over_escalated: over, under_escalated: under, skipped_prerequisite: skipped, wrong_tool: wrong, over_limit_refunds: overLimit, diagnosis: diagnose(skipped, overLimit, wrong, over, under) };
}
