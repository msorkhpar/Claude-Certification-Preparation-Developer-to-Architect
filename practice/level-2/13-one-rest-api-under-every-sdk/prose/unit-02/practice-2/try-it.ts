// Run executes this file. Change the calls to try your code; Submit runs the tests.
import { logTo } from "./logger.ts";
import { ApiError, sendMessages, textOf } from "./rawClient.ts";

// Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logTo("try-it");

// A stand-in transport, like the one the tests use: it records the request and answers with a fixed reply.
const sent: any[] = [];
const transport = (request: any) => {
  sent.push(request);
  const body = { content: [{ type: "text", text: "Paris." }], stop_reason: "end_turn", usage: { input_tokens: 9, output_tokens: 3 } };
  return { status: 200, headers: {}, body: JSON.stringify(body) };
};

const messages = [{ role: "user", content: "Capital of France?" }];
const message = sendMessages(transport, "sk-test-0123456789abcdef", "claude-sonnet-5-5", messages, 64, "Be brief.");

console.log("requests sent:", sent.length);
console.log("method and url:", sent[0]?.method, sent[0]?.url);
console.log("header names:", sent[0] ? Object.keys(sent[0].headers).sort() : null);
console.log("reply text:", message ? textOf(message) : null);

// An error reply becomes an ApiError.
try {
  sendMessages(() => ({ status: 429, headers: { "request-id": "req_1" }, body: JSON.stringify(
    { type: "error", error: { type: "rate_limit_error", message: "Rate limited" } }) }),
    "sk-test-0123456789abcdef", "claude-sonnet-5-5", messages, 64);
} catch (err) {
  if (err instanceof ApiError) console.log("api error:", err.status, err.errorType, err.requestId);
  else throw err;
}
