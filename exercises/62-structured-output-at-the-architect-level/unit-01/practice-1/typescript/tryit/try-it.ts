// Run executes this file. Change the calls to try your code; Submit runs the tests.
import { logTo } from "./logger.ts";
import { extractDocument, validate } from "./extraction.ts";

// Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logTo("try-it");

const DOC = "Invoice from Acme Tools.\nItems: 100.00 + 20.50\nTotal due: 120.50 EUR\nThank you.";
const GOOD = {
  vendor: "Acme Tools", currency: "EUR", currency_detail: null, line_items: [100.0, 20.5], stated_total: 120.5,
  calculated_total: 120.5, conflict_detected: false,
  provenance: { vendor: "Invoice from Acme Tools", currency: "120.50 EUR", stated_total: "Total due: 120.50 EUR" },
};

// A stand-in for the model, like the tests use: it always answers with the same record.
const callModel = (_document: string, feedback: any) => {
  console.log("model called, feedback:", feedback);
  return { ...GOOD };
};

const result = extractDocument(DOC, callModel);
console.log("status:", result?.status);
console.log("attempts:", result?.attempts);

// validate() on its own: a record whose calculated total disagrees with its line items.
const bad = { ...GOOD, calculated_total: 99.0 };
console.log("errors:", JSON.stringify(validate(bad, DOC)));
