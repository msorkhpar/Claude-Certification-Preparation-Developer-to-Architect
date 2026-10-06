// Run executes this file. Change the calls to try your code; Submit runs the tests.
import { logTo } from "./logger.ts";
import { RejectedRequest, buildParams } from "./params.ts";

// Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logTo("try-it");

const SONNET = "claude-sonnet-5-5";
const HAIKU = "claude-haiku-4-5-20251001";

// What the application wants, turned into the request parameters one model accepts.
console.log("sonnet adaptive:", JSON.stringify(buildParams(SONNET, 4096, { thinking: { type: "adaptive" }, effort: "medium" })));
console.log("haiku budget:", JSON.stringify(buildParams(HAIKU, 4096, { thinking: { type: "enabled", budget_tokens: 2048 } })));

// A combination the API would answer with a 400 is refused, naming the parameter.
try {
  console.log("haiku with effort:", JSON.stringify(buildParams(HAIKU, 4096, { effort: "high" })));
} catch (err) {
  if (err instanceof RejectedRequest) console.log("refused:", err.param);
  else throw err;
}
