// Run executes this file. Change the calls to try your code; Submit runs the tests.
import { logTo } from "./logger.ts";
import { BatchError, buildRequests, collect } from "./batches.ts";

// Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logTo("try-it");

const MODEL = "claude-haiku-4-5-20251001";

const item = (id: string) =>
  ({ id, params: { model: MODEL, max_tokens: 200, messages: [{ role: "user", content: `Classify ticket ${id}` }] } });

// One line of the .jsonl results file, like the ones the tests build.
const resultLine = (customId: string, text: string) => JSON.stringify({
  custom_id: customId,
  result: { type: "succeeded", message: { id: "msg_illustrative", type: "message", role: "assistant", model: MODEL,
    stop_reason: "end_turn", content: [{ type: "text", text }], usage: { input_tokens: 50, output_tokens: 7 } } },
});

try {
  const requests = buildRequests([item("t-1"), item("t-2"), item("t-3")]) ?? [];
  console.log("custom ids:", requests.map((r) => r.custom_id).join(", "));
  // The results come back in any order: they are matched by custom_id, not by position.
  const done: any = collect(requests, [resultLine("t-3", "billing"), resultLine("t-1", "refund"), resultLine("t-2", "shipping")]) ?? {};
  console.log("outcomes:", (done.outcomes ?? []).map((o: any) => `${o.custom_id}/${o.status}/${o.text}`).join(", "));
  console.log("to retry / fix / unknown:", JSON.stringify(done.retry), JSON.stringify(done.fix), JSON.stringify(done.unknown));
} catch (err) {
  if (err instanceof BatchError) console.log("batch error:", err.message);
  else throw err;
}
