// Run executes this file. Change the calls to try your code; Submit runs the tests.
import { logTo } from "./logger.ts";
import { buildContext, trimRecord, updateFacts, window } from "./contextBuilder.ts";

// Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logTo("try-it");

// A tool result is large; keep only the fields the next turn needs, with their exact values.
const order = { order_id: "A-1042", items: "2 x kettle", warehouse_bin: "R7-22", refund_amount: "$129.50" };
console.log("trimmed record:", trimRecord(order, ["refund_amount", "order_id"]));

// A newer value replaces the current one; the old one is kept as superseded.
let facts = updateFacts({}, "address", "12 Oak St", "2026-08-01");
facts = updateFacts(facts, "address", "9 Elm Rd", "2026-09-10");
console.log("facts:", JSON.stringify(facts));

// The context of the next request: case facts, then the summary, then the recent messages.
const caseFacts = [{ customer: "c1", name: "refund", value: "$129.50", as_of: "2026-09-02" }];
const recent = [{ role: "user", kind: "text", id: "m1", text: "Where is my refund?" }];
console.log(buildContext("c1", caseFacts, "The customer asked about a refund.", recent));

// A tool call and its result stay together when the window drops old messages.
const messages = [{ role: "user", kind: "text", id: "m1", text: "x".repeat(40) },
  { role: "assistant", kind: "tool_use", id: "t1", text: "lookup" },
  { role: "user", kind: "tool_result", id: "t1", text: "found" }];
console.log("window ids:", window(messages, 6).map((m: any) => m.id));
