// Run executes this file. Change the calls to try your code; Submit runs the tests.
import { logTo } from "./logger.ts";
import { review } from "./conversationReview.ts";

// Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logTo("try-it");

const policy = { max_turns: 12, max_repeat: 10, min_resolved: 80, min_n: 3 };
const conv = (segment = "billing", turns = 5, resolved = true, handoff = "none", needed = false, repeated = false, risk = false) =>
  ({ id: "c", segment, turns, resolved, handoff, needed_person: needed, repeated, risk });

// A batch of conversations: billing mostly resolved, a safety case handed off, one overlong smalltalk.
const batch = [conv(), conv(), conv(), conv("billing", 5, false, "requested", true), conv("smalltalk"),
  conv("smalltalk", 15), conv("safety", 5, false, "safety", true, false, true), conv("billing", 5, true, "none", false, true)];
const report = review(batch, policy);
console.log("conversations:", report.n, "| resolved:", report.resolved, `(${report.resolved_pct}%)`);
console.log("safety missed:", report.safety_missed, "| overlong:", report.overlong, "| repeat %:", report.repeat_pct);
console.log("segments:", JSON.stringify(report.segments));
console.log("verdict:", report.verdict, "-", report.reason);
