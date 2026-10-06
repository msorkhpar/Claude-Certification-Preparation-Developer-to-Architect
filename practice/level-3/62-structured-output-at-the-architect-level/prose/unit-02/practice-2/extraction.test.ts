import { test } from "node:test";
import assert from "node:assert/strict";
import { resolve } from "node:path";
import { pathToFileURL } from "node:url";

// The solution is the file beside this test.
const dir = resolve(import.meta.dirname);
const { accuracy, extractDocument, mergeChunks, requestChoice, validate } = await import(pathToFileURL(resolve(dir, "extraction.ts")).href);

const DOC = "Invoice from Acme Tools.\nItems: 100.00 + 20.50\nTotal due: 120.50 EUR\nThank you.";
const DOC_NO_VENDOR = "Items: 10.00\nTotal due: 10.00 USD";
const GOOD = { vendor: "Acme Tools", currency: "EUR", currency_detail: null, line_items: [100.0, 20.5], stated_total: 120.5, calculated_total: 120.5, conflict_detected: false,
  provenance: { vendor: "Invoice from Acme Tools", currency: "120.50 EUR", stated_total: "Total due: 120.50 EUR" } };
const record = (over: Record<string, unknown> = {}): any => ({ ...structuredClone(GOOD), ...over });

function scripted(...records: unknown[]) {
  const calls: Array<{ document: string; feedback: unknown }> = [];
  const callModel = (document: string, feedback: unknown) => {
    calls.push({ document, feedback: structuredClone(feedback) });
    return structuredClone(records[Math.min(calls.length - 1, records.length - 1)]);
  };
  return { callModel, calls };
}

function run(document: string, records: unknown[], options: { required?: string[]; maxRetries?: number } = {}) {
  const { callModel, calls } = scripted(...records);
  const result = extractDocument(document, callModel, options.required ?? [], options.maxRetries ?? 2);
  assert.ok(result && typeof result === "object", "extractDocument returned nothing");
  return { result, calls };
}

function kinds(errors: any): Array<[string, string]> {
  assert.ok(Array.isArray(errors), "validate returned nothing");
  return errors.map((e: any) => [e.kind, e.field]);
}

test("m1 a document with every value present and quoted comes back valid on the first attempt", () => {
  const { result, calls } = run(DOC, [GOOD]);
  assert.deepEqual(result, { status: "valid", record: GOOD, attempts: 1, errors: [] });
  assert.deepEqual(calls, [{ document: DOC, feedback: null }]);
});

test("e1 a value the document does not give is null and needs no quote while an invented value fails as ungrounded", () => {
  const noVendor = record({ vendor: null, currency: "USD", line_items: [10.0], stated_total: 10.0, calculated_total: 10.0, provenance: { currency: "10.00 USD", stated_total: "Total due: 10.00 USD" } });
  assert.deepEqual(kinds(validate(noVendor, DOC_NO_VENDOR)), []);
  const invented = record({ vendor: "Acme Tools", currency: "USD", line_items: [10.0], stated_total: 10.0, calculated_total: 10.0, provenance: { vendor: "Invoice from Acme Tools", currency: "10.00 USD", stated_total: "Total due: 10.00 USD" } });
  assert.deepEqual(kinds(validate(invented, DOC_NO_VENDOR)), [["ungrounded", "vendor"]]);
  const unquoted = record({ provenance: { vendor: "Invoice from Acme Tools", currency: "120.50 EUR" } });
  assert.deepEqual(kinds(validate(unquoted, DOC)), [["ungrounded", "stated_total"]]);
  assert.deepEqual(kinds(validate(record({ provenance: { vendor: "", currency: "120.50 EUR", stated_total: "Total due: 120.50 EUR" } }), DOC)), [["ungrounded", "vendor"]]);
});

test("e2 a retry carries the original document the failed record and only the errors a second look can fix", () => {
  const bad = record({ vendor: "Acme Corp", provenance: { vendor: "Invoice from Acme Corp", currency: "120.50 EUR", stated_total: "Total due: 120.50 EUR" } });
  let { result, calls } = run(DOC, [bad, GOOD], { required: ["vendor", "currency"] });
  assert.ok(result.status === "valid" && result.attempts === 2 && calls.length === 2);
  assert.deepEqual(calls[0], { document: DOC, feedback: null });
  assert.equal(calls[1].document, DOC);
  assert.deepEqual((calls[1].feedback as any).previous, bad);
  assert.deepEqual((calls[1].feedback as any).errors.map((e: any) => [e.kind, e.field]), [["ungrounded", "vendor"]]);
  assert.ok((calls[1].feedback as any).errors.every((e: any) => e.message));
  const mixed = record({ vendor: "Acme Corp", currency: "unclear", provenance: { vendor: "Invoice from Acme Corp", stated_total: "Total due: 120.50 EUR" } });
  ({ result, calls } = run(DOC, [mixed, GOOD], { required: ["currency"] }));
  assert.deepEqual((calls[1].feedback as any).errors.map((e: any) => [e.kind, e.field]), [["ungrounded", "vendor"]], "the absent currency is not something a second look can fix");
});

test("e3 a required value that the model reports as absent is not retried and goes to review", () => {
  const nothing = record({ stated_total: null, provenance: { vendor: "Invoice from Acme Tools", currency: "120.50 EUR" } });
  let { result, calls } = run(DOC, [nothing, GOOD], { required: ["stated_total"] });
  assert.ok(result.status === "needs_review" && result.attempts === 1 && calls.length === 1);
  assert.deepEqual(kinds(result.errors), [["absent", "stated_total"]]);
  ({ result, calls } = run(DOC, [nothing]));
  assert.equal(result.status, "valid", "a value that is not required may stay null");
});

test("e4 retries stop after the limit and the document is marked failed", () => {
  const bad = record({ currency: "dollars" });
  let { result, calls } = run(DOC, [bad]);
  assert.ok(result.status === "failed" && result.attempts === 3 && calls.length === 3);
  assert.deepEqual(kinds(result.errors), [["syntax", "currency"]]);
  ({ result } = run(DOC, [bad], { maxRetries: 0 }));
  assert.ok(result.status === "failed" && result.attempts === 1);
  ({ result } = run(DOC, [bad, GOOD], { maxRetries: 1 }));
  assert.ok(result.status === "valid" && result.attempts === 2);
  assert.equal(run(DOC, [{ vendor: "x" }]).result.status, "failed");
});

test("e5 a total that differs from the line items is a semantic error and a flagged conflict goes to review without a retry", () => {
  const wrongSum = record({ calculated_total: 100.0, stated_total: 100.0, provenance: { vendor: "Invoice from Acme Tools", currency: "120.50 EUR", stated_total: "Total due: 120.50 EUR" } });
  assert.deepEqual(kinds(validate(wrongSum, DOC)), [["semantic", "calculated_total"]]);
  const unflagged = record({ stated_total: 130.0 });
  assert.deepEqual(kinds(validate(unflagged, DOC)), [["semantic", "stated_total"]]);
  const flagged = record({ stated_total: 130.0, conflict_detected: true });
  assert.deepEqual(kinds(validate(flagged, DOC)), [], "a conflict the model flagged is information, not an error");
  const { result, calls } = run(DOC, [flagged]);
  assert.ok(result.status === "needs_review" && result.attempts === 1 && result.errors.length === 0 && calls.length === 1);
});

test("e6 currency takes unclear and other with a detail and rejects anything else as a syntax error", () => {
  const noCurrency = record({ currency: "unclear", provenance: { vendor: "Invoice from Acme Tools", stated_total: "Total due: 120.50 EUR" } });
  assert.deepEqual(kinds(validate(noCurrency, DOC)), []);
  const chf = "Invoice from Acme Tools.\nTotal due: 120.50 CHF";
  const other = record({ currency: "other", currency_detail: "CHF", provenance: { vendor: "Invoice from Acme Tools", currency: "120.50 CHF", stated_total: "Total due: 120.50 CHF" } });
  assert.deepEqual(kinds(validate(other, chf)), []);
  assert.deepEqual(kinds(validate({ ...other, currency_detail: null }, chf)), [["syntax", "currency_detail"]]);
  assert.deepEqual(kinds(validate({ ...other, currency_detail: "  " }, chf)), [["syntax", "currency_detail"]]);
  assert.deepEqual(kinds(validate(record({ currency: "dollars" }), DOC)), [["syntax", "currency"]]);
  assert.deepEqual(kinds(validate(record({ line_items: "100" }), DOC)), [["syntax", "line_items"]]);
  const missing = record();
  delete missing.provenance;
  assert.deepEqual(kinds(validate(missing, DOC)), [["syntax", "provenance"]]);
});

const chunk = (over: Record<string, unknown> = {}) => ({ vendor: null, currency: "unclear", currency_detail: null, line_items: [], stated_total: null, calculated_total: 0, conflict_detected: false, provenance: {}, ...over });

test("e7 chunk results merge by keeping the first value and recording a conflict when two chunks disagree", () => {
  const one = chunk({ vendor: "Acme Tools", line_items: [100.0], provenance: { vendor: "Invoice from Acme Tools" } });
  const two = chunk({ currency: "EUR", line_items: [20.5], stated_total: 120.5, provenance: { currency: "120.50 EUR", stated_total: "Total due: 120.50 EUR" } });
  const three = chunk({ vendor: "Acme Tool Ltd", stated_total: 120.5, provenance: { vendor: "Acme Tool Ltd", stated_total: "Total due: 120.50 EUR" } });
  const merged = mergeChunks([one, two, three]);
  assert.deepEqual(merged, { vendor: "Acme Tools", currency: "EUR", currency_detail: null, line_items: [100.0, 20.5], stated_total: 120.5, calculated_total: 120.5, conflict_detected: true,
    provenance: { vendor: "Invoice from Acme Tools", currency: "120.50 EUR", stated_total: "Total due: 120.50 EUR" }, conflicts: ["vendor"] });
  const clean = mergeChunks([one, two]);
  assert.ok(clean.conflicts.length === 0 && clean.conflict_detected === false && clean.vendor === "Acme Tools");
  assert.ok(mergeChunks([chunk(), chunk()]).vendor === null && mergeChunks([chunk({ conflict_detected: true })]).conflict_detected === true);
});

test("e8 accuracy counts every document and not only the validated ones", () => {
  const labels: Record<string, any> = {};
  for (let i = 0; i < 10; i++) labels[`d${i}`] = { vendor: `V${i}`, stated_total: i };
  const results: Record<string, any> = {};
  for (let i = 0; i < 5; i++) results[`d${i}`] = { status: "valid", record: { vendor: `V${i}`, stated_total: i } };
  results.d5 = { status: "valid", record: { vendor: "wrong", stated_total: 5 } };
  results.d6 = { status: "needs_review", record: { vendor: "V6", stated_total: 6 } };
  results.d7 = { status: "needs_review", record: { vendor: "V7", stated_total: 7 } };
  results.d8 = { status: "failed", record: { vendor: "V8", stated_total: 8 } };
  assert.deepEqual(accuracy(results, labels), { all_documents: 0.5, validated_only: 0.83, validated: 6, total: 10 });
  assert.deepEqual(accuracy({}, {}), { all_documents: 0, validated_only: 0, validated: 0, total: 0 });
});

test("e9 the request forces a tool where the model allows it and falls back to auto with a reply check where it does not", () => {
  const two = ["extract_invoice", "extract_receipt"];
  assert.deepEqual(requestChoice("claude-haiku-4-5", two), { tool_choice: { type: "any" }, strict: true, verify_reply: false });
  assert.deepEqual(requestChoice("claude-haiku-4-5", ["extract_invoice"]), { tool_choice: { type: "tool", name: "extract_invoice" }, strict: true, verify_reply: false });
  assert.deepEqual(requestChoice("claude-haiku-4-5", two, "extract_metadata"), { tool_choice: { type: "tool", name: "extract_metadata" }, strict: true, verify_reply: false });
  for (const model of ["claude-opus-5-5", "claude-sonnet-5-5", "claude-fable-5-1"]) {
    assert.deepEqual(requestChoice(model, two), { tool_choice: { type: "auto" }, strict: true, verify_reply: true });
    assert.deepEqual(requestChoice(model, two, "extract_metadata"), { tool_choice: { type: "auto" }, strict: true, verify_reply: true });
  }
});
