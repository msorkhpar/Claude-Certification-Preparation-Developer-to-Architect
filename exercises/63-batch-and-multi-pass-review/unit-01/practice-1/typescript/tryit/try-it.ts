// Run executes this file. Change the calls to try your code; Submit runs the tests.
import { logTo } from "./logger.ts";
import { chooseApi, resubmissionPlan, reviewPlan, submissionInterval } from "./batchReview.ts";

// Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logTo("try-it");

// How often to submit a batch so a 30-hour SLA still leaves room for the 24-hour window and 2 hours of handling.
console.log("interval for a 30 h SLA:", submissionInterval(30));
console.log("API for a blocking check:", chooseApi(true));
console.log("API for a nightly report:", chooseApi(false));

// What to do with the results of a batch: (custom id, result kind) pairs and the size of each request.
const results: Array<[string, string]> = [["a1", "succeeded"], ["big", "expired"], ["bad", "invalid_request"], ["late", "errored"]];
console.log("resubmission plan:", JSON.stringify(resubmissionPlan(results, { big: 2000, bad: 10, late: 10 }, 1000)));
console.log("review passes:", JSON.stringify(reviewPlan(["a.py", "b.py", "c.py"])));
