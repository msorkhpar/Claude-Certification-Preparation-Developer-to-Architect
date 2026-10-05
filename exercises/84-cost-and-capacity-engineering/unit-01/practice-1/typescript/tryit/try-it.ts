// Run executes this file. Change the calls to try your code; Submit runs the tests.
import { logTo } from "./logger.ts";
import { admit, delivery, route } from "./gatewayBudget.ts";

// Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logTo("try-it");

const policy = { allowed: ["haiku", "sonnet"], routes: { classify: "haiku", draft: "sonnet", review: "opus" }, default: "sonnet",
  cheaper: { opus: "sonnet", sonnet: "haiku" } };

// The gateway first checks the team's budget, then routes the request: near the limit it picks a cheaper model.
for (const spend of [0, 850, 990]) {
  const status = admit(spend, 1000, 100);
  console.log(`spend ${spend}/1000 -> ${status}: a review request goes to`, route({ task: "review" }, policy, status as string));
}

// A delivery check: does a 95th-percentile latency of 40 s leave 20 percent of margin under a 60 s timeout?
console.log("delivery:", delivery(40, 60, 20));
