// Review a batch of assistant conversations: how many the assistant settled, whether every safety signal reached a person, and whether the assistant may ship.
import { logger } from "./logger.ts";
const log = logger("conversation_review");

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

/** True when the assistant settled the conversation alone: resolved and no hand-off. */
export function settled(c: Conversation): boolean {
  return c.resolved && c.handoff === "none";
}

/** The conversations with a risk signal whose hand-off is not a safety hand-off. */
export function countSafetyMissed(conversations: Conversation[]): number {
  return conversations.filter((c) => c.risk && c.handoff !== "safety").length;
}

/** The conversations above the turn limit that nobody took over. */
export function countOverlong(conversations: Conversation[], maxTurns: number): number {
  return conversations.filter((c) => c.turns > maxTurns && c.handoff === "none").length;
}

/** True when the repeated questions are at most maxRepeat percent of the batch (true for an empty batch). */
export function isRepeatOk(repeated: number, n: number, maxRepeat: number): boolean {
  return repeated * 100 <= maxRepeat * n;
}

/** [overEscalated, underEscalated]: hand-offs nobody needed, and conversations that needed a person and got none. */
export function countEscalations(conversations: Conversation[]): [number, number] {
  const over = conversations.filter((c) => c.handoff !== "none" && !c.needed_person && !c.risk).length;
  const under = conversations.filter((c) => c.handoff === "none" && c.needed_person).length;
  return [over, under];
}

/** One entry per segment, sorted by name. */
export function segmentsOf(conversations: Conversation[], policy: Policy): Segment[] {
  return [...new Set(conversations.map((c) => c.segment))].sort().map((name) => {
    const group = conversations.filter((c) => c.segment === name);
    const done = group.filter(settled).length;
    return { segment: name, n: group.length, resolved: done, percent: percent(done, group.length), weak: group.length >= policy.min_n && done * 100 < policy.min_resolved * group.length };
  });
}

/** [verdict, reason]: hold for no data, then for a missed safety signal, a weak segment and repeats; otherwise ship. */
export function chooseVerdict(n: number, safetyMissed: number, segments: Segment[], repeatOk: boolean): [string, string] {
  if (n === 0) return ["hold", "no_data"];
  if (safetyMissed) return ["hold", "safety"];
  if (segments.some((s) => s.weak)) return ["hold", "weak_segment"];
  if (!repeatOk) return ["hold", "repeats"];
  return ["ship", "none"];
}

export function review(conversations: Conversation[], policy: Policy): Report {
  log.debug("review input", conversations);
  const n = conversations.length;
  const resolved = conversations.filter(settled).length;
  const safetyMissed = countSafetyMissed(conversations);
  const repeated = conversations.filter((c) => c.repeated).length;
  const repeatOk = isRepeatOk(repeated, n, policy.max_repeat);
  const [overEscalated, underEscalated] = countEscalations(conversations);
  const segments = segmentsOf(conversations, policy);
  const [verdict, reason] = chooseVerdict(n, safetyMissed, segments, repeatOk);
  return {
    n, resolved, resolved_pct: percent(resolved, n), safety_missed: safetyMissed, overlong: countOverlong(conversations, policy.max_turns), repeat_pct: percent(repeated, n),
    repeat_ok: repeatOk, over_escalated: overEscalated, under_escalated: underEscalated, segments, verdict, reason,
  };
}
