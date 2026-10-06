// Run executes this file. Change the calls to try your code; Submit runs the tests.
import { logTo } from "./logger.ts";
import { diagnose } from "./diagnose.ts";

// Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logTo("try-it");

// A trace is the list of what happened: the request, then what came back.
const REQUEST = { kind: "request", model: "claude-sonnet-5-5", max_tokens: 1024, tools: ["get_weather"], last_user_blocks: ["text"] };
const error = (status: number, error_type: string) => ({ kind: "error", status, error_type, message: "m" });

for (const [status, errorType] of [[401, "authentication_error"], [504, "timeout_error"], [529, "overloaded_error"]] as [number, string][]) {
  const d: any = diagnose([REQUEST, error(status, errorType)]) ?? {};
  console.log(`HTTP ${status}: type=${d.type} origin=${d.origin} recovery=${d.recovery}`);
}
