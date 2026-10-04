// Audit a support agent's recorded sessions: the rates, the failure shapes and the first fix.
// Read statement.md for the fields of a session and of the report, then replace the body of audit().
export type Step = { tool: string; ok: boolean; right_tool?: string };
export type Session = { id: string; steps: Step[]; outcome: string; needs_human: boolean; refund_cents: number; limit_cents: number };
export type Report = {
  sessions: number; resolved: number; fcr: number; meets_target: boolean; over_escalated: number; under_escalated: number;
  skipped_prerequisite: number; wrong_tool: number; over_limit_refunds: number; diagnosis: string;
};

export function audit(sessions: Session[]): Report {
  return { sessions: 0, resolved: 0, fcr: 0, meets_target: false, over_escalated: 0, under_escalated: 0, skipped_prerequisite: 0, wrong_tool: 0, over_limit_refunds: 0, diagnosis: "" };
}
