// Run executes this file. Change the calls to try your code; Submit runs the tests.
import { logTo } from "./logger.ts";
import { extract } from "./extractor.ts";

// Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logTo("try-it");

const DOC = "Invoice from Acme Tools. Total due: 120.50 EUR. Thank you for your business.";
const SCHEMA = {
  type: "object",
  required: ["vendor", "total", "currency", "evidence"],
  properties: {
    vendor: { type: "string" },
    total: { type: "number", minimum: 0 },
    currency: { type: "string", enum: ["USD", "EUR", "GBP"] },
    evidence: { type: "string" },
  },
  additionalProperties: false,
};
const GOOD = { vendor: "Acme Tools", total: 120.5, currency: "EUR", evidence: "Total due: 120.50 EUR" };

// A hand-written stand-in for the model, in the shape of a Messages API reply: it always answers with valid JSON.
const ask = (_messages: any[]) => ({ content: [{ type: "text", text: JSON.stringify(GOOD) }], stop_reason: "end_turn" });

const result: any = extract(ask, DOC, SCHEMA, 3, ["evidence"]) ?? {};

console.log("status:", result.status);
console.log("attempts:", result.attempts);
console.log("value:", JSON.stringify(result.value));
console.log("errors:", JSON.stringify(result.errors));
