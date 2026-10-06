// Run executes this file. Change the calls to try your code; Submit runs the tests.
import { logTo } from "./logger.ts";
import { launchReview, neededAccuracy, scorecard, verdict } from "./launchReview.ts";

// Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logTo("try-it");

// A design that sits at every threshold of the review, as the tests' clean design does.
const cleanFlags = ["feedback_loop", "model_measured", "replace_on_change", "deferral", "protected_segment", "rollback", "human_step", "owner",
  "accuracy_stated", "managed_settings", "irreversible_action", "team"];
const numbers = { team_value_chats: 15, tool_tokens: 10000, eval_cases: 20, rollout_stages: 3, retain_days: 365, floor_days: 90,
  ceiling_days: 365, team_size: 10, latency_ms: 2000, availability_tenths: 995 };

const findings = launchReview(new Set(cleanFlags), numbers);
console.log("clean design:", findings, "->", verdict(findings));

// The same design with an agent that does not need to be one, and PII reaching the model.
const risky = launchReview(new Set([...cleanFlags, "agent", "path_known", "pii_reaches_model"]), numbers);
console.log("risky design:", risky, "->", verdict(risky));
console.log("findings per domain:", scorecard(risky));
console.log("accuracy needed when an error costs 250 and a review 5:", neededAccuracy(250, 5));
