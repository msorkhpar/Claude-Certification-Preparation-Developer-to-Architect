// Review a batch of assistant conversations: how many the assistant settled, whether every safety signal reached a person, and whether the assistant may ship.
// Read statement.md for the fields of a conversation, of the policy and of the report, then replace the body of review().
export type Conversation = { id: string; segment: string; turns: number; resolved: boolean; handoff: string; needed_person: boolean; repeated: boolean; risk: boolean };
export type Policy = { max_turns: number; max_repeat: number; min_resolved: number; min_n: number };
export type Segment = { segment: string; n: number; resolved: number; percent: number; weak: boolean };
export type Report = {
  n: number; resolved: number; resolved_pct: number; safety_missed: number; overlong: number; repeat_pct: number; repeat_ok: boolean; over_escalated: number;
  under_escalated: number; segments: Segment[]; verdict: string; reason: string;
};

export function review(conversations: Conversation[], policy: Policy): Report {
  return { n: 0, resolved: 0, resolved_pct: 0, safety_missed: 0, overlong: 0, repeat_pct: 0, repeat_ok: false, over_escalated: 0, under_escalated: 0, segments: [{ segment: "", n: 0, resolved: 0, percent: 0, weak: false }], verdict: "", reason: "" };
}
