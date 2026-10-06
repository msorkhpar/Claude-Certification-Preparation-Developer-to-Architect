// Run executes this file. Change the calls to try your code; Submit runs the tests.
import { logTo } from "./logger.ts";
import { CallFailed, callWithRetry } from "./retry.ts";

// Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logTo("try-it");

const failure = (status: number, kind = "api_error") =>
  ({ status, headers: {}, body: { type: "error", error: { type: kind, message: "x" } } });

// A scripted send(), like the one the tests use: overloaded twice, then a good reply.
const replies = [failure(529, "overloaded_error"), failure(503), { status: 200, headers: {}, body: { type: "message" } }];
let calls = 0;
const send = () => {
  calls++;
  return replies.shift()!;
};

// The sleep is injected, so nothing really waits: it just records the delays asked for.
const waits: number[] = [];
try {
  const result = callWithRetry(send, (seconds) => waits.push(seconds));
  console.log("final status:", result?.status);
} catch (err) {
  if (err instanceof CallFailed) console.log("gave up:", err.message);
  else throw err;
}
console.log("calls made:", calls);
console.log("waits requested:", waits);
