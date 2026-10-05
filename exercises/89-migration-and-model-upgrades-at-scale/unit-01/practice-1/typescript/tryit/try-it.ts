// Run executes this file. Change the calls to try your code; Submit runs the tests.
import { logTo } from "./logger.ts";
import { migrateRequest, retirementStatus, rolloutStep } from "./rollout.ts";

// Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logTo("try-it");

// The retirement calendar: days left per model, nearest first, with the level of urgency.
const models: Array<[string, string, boolean]> = [["claude-old-a", "2026-10-18", true], ["claude-old-b", "2026-11-30", false], ["claude-old-c", "2027-01-01", false]];
for (const line of retirementStatus(models, "2026-10-04")) console.log("calendar:", line);

// A request written for an older model, and what the migration changes in it.
const old = { model: "claude-sonnet-4-5-20250929", temperature: 0.7, topP: 0.9, topK: 40, thinking: "disabled", toolChoice: "any", strict: false, prefill: true };
const [migrated, changes] = migrateRequest(old);
console.log("migrated model:", migrated.model);
for (const change of changes) console.log("change:", change);

// A staged rollout: one decision per stage from the traffic and the errors seen so far.
console.log("stage 1:", rolloutStep(1, 2000, 6, 1000, 5));
console.log("stage 25:", rolloutStep(25, 50000, 400, 1000, 5));
