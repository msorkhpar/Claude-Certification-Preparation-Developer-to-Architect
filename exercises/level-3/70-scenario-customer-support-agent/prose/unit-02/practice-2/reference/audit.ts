// Audit a support agent's recorded sessions: the rates, the failure shapes and the first fix.
import { logger } from "./logger.ts";
const log = logger("audit");

export type Step = { tool: string; ok: boolean; right_tool?: string };
export type Session = { id: string; steps: Step[]; outcome: string; needs_human: boolean; refund_cents: number; limit_cents: number };
export type Report = {
  sessions: number; resolved: number; fcr: number; meets_target: boolean; over_escalated: number; under_escalated: number;
  skipped_prerequisite: number; wrong_tool: number; over_limit_refunds: number; diagnosis: string;
};

const PROTECTED = ["lookup_order", "process_refund"];
const TARGET = 0.8;

/** True when an order or refund call came before the first successful get_customer call. */
export function skippedPrerequisite(steps: Step[]): boolean {
  let verified = false;
  for (const step of steps) {
    if (step.tool === "get_customer" && step.ok) verified = true;
    else if (PROTECTED.includes(step.tool) && !verified) return true;
  }
  return false;
}

/** True when at least one step has a known right tool that differs from the tool used. */
export function hasWrongTool(steps: Step[]): boolean {
  return steps.some((st) => st.right_tool && st.tool !== st.right_tool);
}

/** True when the session was resolved with a refund above its limit. */
export function isOverLimit(session: Session): boolean {
  return session.outcome === "resolved" && session.refund_cents > session.limit_cents;
}

/** The resolved share, rounded to three decimals; 0 for no sessions. */
export function firstContactRate(resolved: number, n: number): number {
  return n ? Math.round((resolved / n) * 1000) / 1000 : 0;
}

/** The first fix, by the order of the statement. */
export function diagnose(skipped: number, overLimit: number, wrong: number, over: number, under: number): string {
  if (skipped || overLimit) return "enforce_in_code";
  if (wrong && wrong >= over + under) return "rewrite_tool_descriptions";
  if (over + under) return "write_escalation_criteria";
  return "none";
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
