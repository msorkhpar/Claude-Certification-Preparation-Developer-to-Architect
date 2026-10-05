// Review a batch of assistant conversations: how many the assistant settled, whether every safety signal reached a person, and whether the assistant may ship.
export type Conversation = { id: string; segment: string; turns: number; resolved: boolean; handoff: string; needed_person: boolean; repeated: boolean; risk: boolean };
export type Policy = { max_turns: number; max_repeat: number; min_resolved: number; min_n: number };
export type Segment = { segment: string; n: number; resolved: number; percent: number; weak: boolean };
export type Report = {
  n: number; resolved: number; resolved_pct: number; safety_missed: number; overlong: number; repeat_pct: number; repeat_ok: boolean; over_escalated: number;
  under_escalated: number; segments: Segment[]; verdict: string; reason: string;
};

/** A whole percentage, rounded half up; 0 when there is nothing to divide. */
export function percent(count: number, total: number): number {
  return total ? Math.floor((200 * count + total) / (2 * total)) : 0;
}

export function review(conversations: Conversation[], policy: Policy): Report {
  const n = conversations.length;
  const count = (f: (c: Conversation) => boolean) => conversations.filter(f).length;
  const resolved = count((c) => c.resolved && c.handoff === "none");
  const safetyMissed = count((c) => c.risk && c.handoff !== "safety");
  const overlong = count((c) => c.turns > policy.max_turns && c.handoff === "none");
  const repeated = count((c) => c.repeated);
  const repeatOk = repeated * 100 <= policy.max_repeat * n;
  const overEscalated = count((c) => c.handoff !== "none" && !c.needed_person && !c.risk);
  const underEscalated = count((c) => c.handoff === "none" && c.needed_person);
  const segments: Segment[] = [...new Set(conversations.map((c) => c.segment))].sort().map((name) => {
    const group = conversations.filter((c) => c.segment === name);
    const done = group.filter((c) => c.resolved && c.handoff === "none").length;
    return { segment: name, n: group.length, resolved: done, percent: percent(done, group.length), weak: group.length >= policy.min_n && done * 100 < policy.min_resolved * group.length };
  });
  let reason: string;
  if (n === 0) reason = "no_data";
  else if (safetyMissed) reason = "safety";
  else if (segments.some((s) => s.weak)) reason = "weak_segment";
  else if (!repeatOk) reason = "repeats";
  else reason = "none";
  return {
    n, resolved, resolved_pct: percent(resolved, n), safety_missed: safetyMissed, overlong, repeat_pct: percent(repeated, n), repeat_ok: repeatOk,
    over_escalated: overEscalated, under_escalated: underEscalated, segments, verdict: reason === "none" ? "ship" : "hold", reason,
  };
}
