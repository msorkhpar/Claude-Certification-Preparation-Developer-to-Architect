import { test } from "node:test";
import assert from "node:assert/strict";
import { resolve } from "node:path";
import { pathToFileURL } from "node:url";

// SOLUTION_DIR selects starter, reference or a planted wrong solution.
const dir = resolve(process.env.SOLUTION_DIR ?? "starter");
const { review } = await import(pathToFileURL(resolve(dir, "conversationReview.ts")).href);

const POLICY = { max_turns: 12, max_repeat: 10, min_resolved: 80, min_n: 3 };
type Fields = { segment?: string; turns?: number; resolved?: boolean; handoff?: string; needed?: boolean; repeated?: boolean; risk?: boolean };
const conv = (f: Fields = {}) => ({ id: "c", segment: f.segment ?? "billing", turns: f.turns ?? 5, resolved: f.resolved ?? true, handoff: f.handoff ?? "none",
  needed_person: f.needed ?? false, repeated: f.repeated ?? false, risk: f.risk ?? false });
const many = (n: number, f: Fields = {}) => Array.from({ length: n }, () => conv(f));
const seg = (segment: string, n: number, resolved: number, percent: number, weak: boolean) => ({ segment, n, resolved, percent, weak });
const handed = (f: Fields = {}) => ({ resolved: false, handoff: "requested", needed: true, ...f });

test("m1 a mixed batch gets every count the segments and a verdict", () => {
  const batch = [...many(3), conv(handed()), conv({ segment: "smalltalk" }), conv({ segment: "smalltalk", turns: 15 }),
    conv({ segment: "safety", resolved: false, handoff: "safety", needed: true, risk: true }), conv({ repeated: true })];
  assert.deepEqual(review(batch, POLICY), {
    n: 8, resolved: 6, resolved_pct: 75, safety_missed: 0, overlong: 1, repeat_pct: 13, repeat_ok: false, over_escalated: 0,
    under_escalated: 0, segments: [seg("billing", 5, 4, 80, false), seg("safety", 1, 0, 0, false), seg("smalltalk", 2, 2, 100, false)],
    verdict: "hold", reason: "repeats" });
});

test("e1 an empty batch has zero figures and is held for lack of data", () => {
  assert.deepEqual(review([], POLICY), {
    n: 0, resolved: 0, resolved_pct: 0, safety_missed: 0, overlong: 0, repeat_pct: 0, repeat_ok: true, over_escalated: 0,
    under_escalated: 0, segments: [], verdict: "hold", reason: "no_data" });
});

test("e2 a safety signal counts as missed unless it went to a person as a safety hand off", () => {
  const batch = [...many(5), conv({ segment: "safety", ...handed({ risk: true }) }), conv({ segment: "safety", resolved: false, risk: true }),
    conv({ segment: "safety", resolved: false, handoff: "safety", needed: true, risk: true })];
  const report = review(batch, POLICY);
  assert.deepEqual([report.safety_missed, report.verdict, report.reason], [2, "hold", "safety"]);
  assert.equal(review([...many(5), conv({ segment: "safety", resolved: false, handoff: "safety", needed: true, risk: true })], POLICY).safety_missed, 0);
});

test("e3 a conversation is overlong only above the turn limit and only when nobody took over", () => {
  assert.equal(review([conv({ turns: 12 })], POLICY).overlong, 0);
  assert.equal(review([conv({ turns: 13 })], POLICY).overlong, 1);
  assert.equal(review([conv({ turns: 30, ...handed({ handoff: "stalled" }) })], POLICY).overlong, 0);
});

test("e4 repeated questions are acceptable at exactly the limit and not above it", () => {
  const at = review([...many(9), ...many(1, { repeated: true })], POLICY);
  assert.deepEqual([at.repeat_pct, at.repeat_ok, at.verdict], [10, true, "ship"]);
  const over = review([...many(8), ...many(2, { repeated: true })], POLICY);
  assert.deepEqual([over.repeat_pct, over.repeat_ok, over.verdict, over.reason], [20, false, "hold", "repeats"]);
});

test("e5 a segment is weak only below the resolution floor and not at it", () => {
  const at = review([...many(8), ...many(2, handed())], POLICY);
  assert.deepEqual(at.segments, [seg("billing", 10, 8, 80, false)]);
  assert.equal(at.reason, "none");
  const below = review([...many(7), ...many(3, handed())], POLICY);
  assert.deepEqual(below.segments, [seg("billing", 10, 7, 70, true)]);
  assert.equal(below.reason, "weak_segment");
});

test("e6 a segment needs the minimum number of conversations before it can be called weak", () => {
  const two = review(many(2, handed({ segment: "refunds" })), POLICY);
  assert.deepEqual(two.segments, [seg("refunds", 2, 0, 0, false)]);
  const three = review(many(3, handed({ segment: "refunds" })), POLICY);
  assert.deepEqual(three.segments, [seg("refunds", 3, 0, 0, true)]);
  assert.equal(three.reason, "weak_segment");
});

test("e7 a hand off is over escalation only when no person was needed and no safety signal was present", () => {
  const batch = [conv({ handoff: "requested" }), conv(handed()), conv({ handoff: "safety", risk: true }), conv({ needed: true }), conv({ resolved: false, handoff: "stalled" })];
  const report = review(batch, POLICY);
  assert.deepEqual([report.over_escalated, report.under_escalated], [2, 1]);
});

test("e8 only conversations the assistant settled alone count as resolved", () => {
  const report = review([conv(), conv({ handoff: "requested", needed: true }), conv({ resolved: false })], POLICY);
  assert.deepEqual([report.resolved, report.resolved_pct], [1, 33]);
});

test("e9 percentages are whole numbers rounded half up", () => {
  const report = review([...many(1), ...many(7, handed())], POLICY);
  assert.deepEqual([report.resolved_pct, report.segments[0].percent], [13, 13]);
  assert.equal(review([...many(2), ...many(1, handed())], POLICY).resolved_pct, 67);
});
