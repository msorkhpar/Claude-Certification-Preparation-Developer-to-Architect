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

/**
 * TODO 1 of 8 (unlocks m1, e1 and e9): a whole percentage, rounded half up.
 * Receives a count and a total. Returns `Math.floor((200 * count + total) / (2 * total))`, and 0 when the total is 0.
 * Example: percent(2, 3) -> 67, percent(1, 8) -> 13, percent(0, 0) -> 0
 */
export function percent(count: number, total: number): number {
  return 0;
}

/**
 * TODO 2 of 8 (unlocks m1 and e8): did the assistant settle this conversation alone?
 * Receives one conversation. Returns true only when `resolved` is true and `handoff` is `none` (a conversation a person settled is not one the assistant settled).
 * Example: settled({ resolved: true, handoff: "requested", ... }) -> false
 */
export function settled(c: Conversation): boolean {
  return false;
}

/**
 * TODO 3 of 8 (unlocks e2): count the safety signals that did not reach a person as a safety hand-off.
 * Receives the conversations. Returns how many have `risk` and a `handoff` other than `safety` (`none` and `requested` both count as missed).
 * Example: one risk conversation with handoff "requested" and one with "safety" -> 1
 */
export function countSafetyMissed(conversations: Conversation[]): number {
  return 0;
}

/**
 * TODO 4 of 8 (unlocks e3): count the overlong conversations.
 * Receives the conversations and the limit. Returns how many have more than `maxTurns` turns and `handoff` `none`.
 * Example: with a limit of 12, turns 12 -> 0, turns 13 -> 1, turns 30 with handoff "stalled" -> 0
 */
export function countOverlong(conversations: Conversation[], maxTurns: number): number {
  return 0;
}

/**
 * TODO 5 of 8 (unlocks e1 and e4): are the repeated questions within the limit?
 * Receives the repeated count, the batch size and the limit in percent. Returns true when `repeated * 100 <= maxRepeat * n` (so true for an empty batch).
 * Example: isRepeatOk(1, 10, 10) -> true, isRepeatOk(2, 10, 10) -> false
 */
export function isRepeatOk(repeated: number, n: number, maxRepeat: number): boolean {
  return false;
}

/**
 * TODO 6 of 8 (unlocks e7): count over- and under-escalation.
 * Receives the conversations. Returns [overEscalated, underEscalated]: hand-offs (any `handoff` but `none`) with no `needed_person` and no `risk`,
 * and conversations with `handoff` `none` that had `needed_person`.
 * Example: a "requested" hand-off nobody needed -> [1, 0]; a "safety" hand-off with risk -> [0, 0]
 */
export function countEscalations(conversations: Conversation[]): [number, number] {
  return [0, 0];
}

/**
 * TODO 7 of 8 (unlocks m1, e5 and e6): one entry per segment.
 * Receives the conversations and the policy. Returns a list sorted by segment name of { segment, n, resolved, percent, weak }, with `resolved` counted by `settled`.
 * `weak` needs at least `min_n` conversations and `resolved * 100 < min_resolved * n` for the segment.
 * Example: 3 unresolved "refunds" conversations with min_n 3 -> [{ segment: "refunds", n: 3, resolved: 0, percent: 0, weak: true }]
 */
export function segmentsOf(conversations: Conversation[], policy: Policy): Segment[] {
  return [{ segment: "", n: 0, resolved: 0, percent: 0, weak: false }];
}

/**
 * TODO 8 of 8 (unlocks m1, e1, e2 and e4): the verdict and its reason.
 * Receives the batch size, the missed safety count, the segments and `repeatOk`. Returns [verdict, reason]: ["hold", "no_data"] for an empty batch,
 * otherwise the first that applies of ["hold", "safety"], ["hold", "weak_segment"] and ["hold", "repeats"], and ["ship", "none"] when none does.
 * Example: chooseVerdict(5, 0, [], false) -> ["hold", "repeats"]
 */
export function chooseVerdict(n: number, safetyMissed: number, segments: Segment[], repeatOk: boolean): [string, string] {
  return ["", ""];
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
