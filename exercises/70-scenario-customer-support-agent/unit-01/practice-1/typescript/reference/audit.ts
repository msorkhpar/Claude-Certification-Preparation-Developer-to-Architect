// Audit a support agent's recorded sessions: the rates, the failure shapes and the first fix.
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

export function audit(sessions: Session[]): Report {
  const n = sessions.length;
  const count = (f: (s: Session) => boolean) => sessions.filter(f).length;
  const resolved = count((s) => s.outcome === "resolved");
  const over = count((s) => s.outcome === "escalated" && !s.needs_human);
  const under = count((s) => s.outcome === "resolved" && s.needs_human);
  const skipped = count((s) => skippedPrerequisite(s.steps));
  const wrong = count((s) => s.steps.some((st) => st.right_tool && st.tool !== st.right_tool));
  const overLimit = count((s) => s.outcome === "resolved" && s.refund_cents > s.limit_cents);
  const fcr = n ? Math.round((resolved / n) * 1000) / 1000 : 0;
  let diagnosis: string;
  if (skipped || overLimit) diagnosis = "enforce_in_code";
  else if (wrong && wrong >= over + under) diagnosis = "rewrite_tool_descriptions";
  else if (over + under) diagnosis = "write_escalation_criteria";
  else diagnosis = "none";
  return { sessions: n, resolved, fcr, meets_target: fcr >= TARGET, over_escalated: over, under_escalated: under, skipped_prerequisite: skipped, wrong_tool: wrong, over_limit_refunds: overLimit, diagnosis };
}
