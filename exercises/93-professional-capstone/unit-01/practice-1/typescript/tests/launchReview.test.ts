import { test } from "node:test";
import assert from "node:assert/strict";
import { resolve } from "node:path";
import { pathToFileURL } from "node:url";

// SOLUTION_DIR selects starter, reference or a planted wrong solution.
const dir = resolve(process.env.SOLUTION_DIR ?? "starter");
const { launchReview, neededAccuracy, scorecard, verdict } = await import(pathToFileURL(resolve(dir, "launchReview.ts")).href);

const CLEAN_FLAGS = ["feedback_loop", "model_measured", "replace_on_change", "deferral", "protected_segment", "rollback", "human_step", "owner", "accuracy_stated", "managed_settings", "irreversible_action", "team"];
const CLEAN_NUMBERS: Record<string, number> = { team_value_chats: 15, tool_tokens: 10000, eval_cases: 20, rollout_stages: 3, retain_days: 365, floor_days: 90, ceiling_days: 365, team_size: 10, latency_ms: 2000, availability_tenths: 995 };
const ALL_BAD_FLAGS = ["agent", "path_known", "volatile_prefix", "filter_after_ranking", "agent_rights_only", "pii_reaches_model", "residency_unmet", "audit_keeps_content", "irreversible_action", "team"];
const ALL_BAD_NUMBERS: Record<string, number> = { team_value_chats: 14, tool_tokens: 10001, eval_cases: 19, rollout_stages: 2, retain_days: 366, floor_days: 90, ceiling_days: 365, team_size: 11, latency_ms: 0, availability_tenths: 0 };

/** The clean design with some flags added or dropped and some numbers changed. */
function review(add: string[] = [], drop: string[] = [], numbers: Record<string, number> = {}): string[] {
  const flags = new Set([...CLEAN_FLAGS, ...add].filter((x) => !drop.includes(x)));
  const result = launchReview(flags, { ...CLEAN_NUMBERS, ...numbers });
  assert.ok(Array.isArray(result), "launchReview returned nothing");
  return result;
}

test("m1 a design that sits exactly at every threshold has no findings and is approved", () => {
  const findings = review();
  assert.deepEqual(findings, []);
  assert.equal(verdict(findings), "approve");
  assert.deepEqual(scorecard(findings), [0, 0, 0, 0, 0, 0, 0]);
  assert.equal(neededAccuracy(250, 5), 98);
});

test("e1 a missing feedback loop a filter after ranking or no way back is a high finding and the design is rejected", () => {
  const findings = review(["filter_after_ranking"], ["feedback_loop"]);
  assert.deepEqual(findings, ["high P1 missing-feedback", "high P3 filter-after-ranking"]);
  assert.equal(verdict(findings), "reject");
  assert.deepEqual(review([], ["rollback"]), ["high P4 no-way-back"]);
});

test("e2 medium findings revise the design and low findings alone approve it", () => {
  const medium = review([], ["owner"]);
  assert.deepEqual(medium, ["medium P6 no-accountable-owner"]);
  assert.equal(verdict(medium), "revise");
  const low = review([], ["model_measured"]);
  assert.deepEqual(low, ["low P2 model-not-measured"]);
  assert.equal(verdict(low), "approve");
  assert.equal(verdict(review([], ["owner", "feedback_loop"])), "reject");
});

test("e3 findings are ordered by severity then domain then rule id", () => {
  const findings = review(["volatile_prefix"], ["protected_segment", "accuracy_stated", "owner", "feedback_loop"], { rollout_stages: 2 });
  assert.deepEqual(findings, [
    "high P1 missing-feedback",
    "medium P2 volatile-prefix",
    "medium P4 big-bang-rollout",
    "medium P4 no-protected-segment",
    "medium P6 no-accountable-owner",
    "low P6 accuracy-unstated",
  ]);
});

test("e4 each numeric threshold passes exactly at its value and fails one step beyond", () => {
  assert.deepEqual(review([], [], { team_value_chats: 15 }), []);
  assert.deepEqual(review([], [], { team_value_chats: 14 }), ["medium P1 team-below-price"]);
  assert.deepEqual(review([], ["deferral"], { tool_tokens: 10000 }), []);
  assert.deepEqual(review([], ["deferral"], { tool_tokens: 10001 }), ["medium P3 tool-bloat"]);
  assert.deepEqual(review([], [], { tool_tokens: 10001 }), []);
  assert.deepEqual(review([], [], { eval_cases: 20 }), []);
  assert.deepEqual(review([], [], { eval_cases: 19 }), ["low P4 small-eval-set"]);
  assert.deepEqual(review([], [], { rollout_stages: 3 }), []);
  assert.deepEqual(review([], [], { rollout_stages: 2 }), ["medium P4 big-bang-rollout"]);
  assert.deepEqual(review([], [], { retain_days: 90 }), []);
  assert.deepEqual(review([], [], { retain_days: 89 }), ["medium P5 retention-outside-window"]);
  assert.deepEqual(review([], [], { retain_days: 365 }), []);
  assert.deepEqual(review([], [], { retain_days: 366 }), ["medium P5 retention-outside-window"]);
  assert.deepEqual(review([], ["managed_settings"], { team_size: 10 }), []);
  assert.deepEqual(review([], ["managed_settings"], { team_size: 11 }), ["medium P7 unmanaged-team-settings"]);
  assert.deepEqual(review([], [], { team_size: 11 }), []);
  assert.deepEqual(review([], [], { latency_ms: 0 }), ["medium P6 sla-without-numbers"]);
  assert.deepEqual(review([], [], { availability_tenths: 0 }), ["medium P6 sla-without-numbers"]);
  assert.deepEqual(review([], [], { latency_ms: 1, availability_tenths: 1 }), []);
});

test("e5 the scorecard counts the findings of each domain from P1 to P7", () => {
  assert.deepEqual(scorecard(["high P1 a", "medium P1 b", "low P7 c", "medium P3 d"]), [2, 0, 1, 0, 0, 0, 1]);
  assert.deepEqual(scorecard([]), [0, 0, 0, 0, 0, 0, 0]);
});

test("e6 the accuracy a design needs is the break even rounded up from the two costs", () => {
  assert.equal(neededAccuracy(250, 5), 98);
  assert.equal(neededAccuracy(60, 5), 91);
  assert.equal(neededAccuracy(3, 1), 66);
  assert.equal(neededAccuracy(5, 5), 0);
  assert.equal(neededAccuracy(5, 6), 0);
  assert.equal(neededAccuracy(0, 5), 0);
  assert.equal(neededAccuracy(-1, 5), 0);
});

test("e7 rules that depend on a second fact fire only when both hold", () => {
  assert.deepEqual(review(["path_known"]), ["medium P1 autonomy-without-need"]);
  assert.deepEqual(review(["path_known"], ["team"]), []);
  assert.deepEqual(review(["agent"]), []);
  assert.deepEqual(review(["agent", "path_known"], ["team"]), ["medium P1 autonomy-without-need"]);
  assert.deepEqual(review([], ["human_step"]), ["high P5 irreversible-without-person"]);
  assert.deepEqual(review([], ["human_step", "irreversible_action"]), []);
  assert.deepEqual(review([], ["team"], { team_value_chats: 3 }), []);
});

test("e8 retrieval and privacy flaws are high findings in domains P3 and P5", () => {
  assert.deepEqual(review(["filter_after_ranking", "agent_rights_only"], ["replace_on_change"]), ["high P3 agent-rights-only", "high P3 filter-after-ranking", "high P3 stale-index"]);
  assert.deepEqual(review(["pii_reaches_model", "residency_unmet", "audit_keeps_content"]), ["high P5 identifiers-reach-model", "high P5 residency-unmet", "medium P5 audit-keeps-content"]);
});

test("e9 a design with every flaw gets all 22 findings and a scorecard that adds up", () => {
  const findings = launchReview(new Set(ALL_BAD_FLAGS), ALL_BAD_NUMBERS);
  assert.equal(findings.length, 22);
  assert.equal(findings[0], "high P1 missing-feedback");
  assert.equal(findings[findings.length - 1], "low P6 accuracy-unstated");
  assert.equal(verdict(findings), "reject");
  assert.deepEqual(scorecard(findings), [3, 2, 4, 4, 5, 3, 1]);
});
