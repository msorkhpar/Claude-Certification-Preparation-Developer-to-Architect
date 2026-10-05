/** An extraction pipeline that admits absence, checks what a schema cannot, retries with feedback and is measured on every document. See ../../statement.md. */

export const CURRENCIES = ["USD", "EUR", "GBP", "other", "unclear"];
const KEYS = ["vendor", "currency", "currency_detail", "line_items", "stated_total", "calculated_total", "conflict_detected", "provenance"];
const RETRYABLE = ["syntax", "semantic", "ungrounded"];
const NO_FORCING = new Set(["claude-opus-5-5", "claude-sonnet-5-5", "claude-fable-5-1", "claude-mythos-5-1"]); // models whose API rejects tool_choice any and tool, as read on 2026-10-03

const isNumber = (value: unknown): boolean => typeof value === "number" && Number.isFinite(value);
const round2 = (x: number): number => Math.round(x * 100) / 100;

function quoteFound(quote: unknown, document: string): boolean {
  // TODO 1 of 10 (finish this to pass e1): is a provenance quote real?
  // Receives the quote the model gave for a field and the document text. Returns true only when the quote is a non-empty string that
  // appears in the document, so an invented value cannot be supported by an invented quote.
  // Example: quoteFound("Acme Ltd", "Invoice from Acme Ltd") -> true, quoteFound("Zed Corp", "Invoice from Acme Ltd") -> false
  return true;
}

function currencyOk(currency: unknown): boolean {
  // TODO 2 of 10 (finish this to pass e6): is this a currency the schema allows?
  // Receives the value of `currency`. Returns true when it is one of CURRENCIES ("unclear" and "other" count, anything else does not).
  // Example: currencyOk("unclear") -> true, currencyOk("dollars") -> false
  return true;
}

function detailMissing(currency: unknown, detail: unknown): boolean {
  // TODO 3 of 10 (finish this to pass e6): is a required currency detail missing?
  // Receives `currency` and `currency_detail`. Returns true when the currency is "other" and the detail is not a non-blank string.
  // Example: detailMissing("other", "  ") -> true, detailMissing("other", "CHF") -> false, detailMissing("USD", null) -> false
  return false;
}

function checkSemantics(record: any, err: (kind: string, field: string, message: string) => void): void {
  // TODO 10 of 10 (finish this to pass e5): report what a schema cannot check about the numbers.
  // Receives a record whose types are already valid and err(kind, field, message), which records one error. Call err("semantic",
  // "calculated_total", ...) when calculated_total is not the sum of line_items (to half a cent), and err("semantic", "stated_total", ...)
  // when stated_total is not null, differs from calculated_total (by more than half a cent) and conflict_detected is false; a conflict the
  // model flagged is information, not an error. It returns nothing.
  // Example: line_items [10, 5], calculated_total 15, stated_total 20, conflict_detected false -> one semantic error on stated_total
}

export function validate(record: any, document: string, required: string[] = []): any[] {
  const errors: any[] = [];
  const err = (kind: string, field: string, message: string) => errors.push({ kind, field, message });
  if (typeof record !== "object" || record === null || Array.isArray(record)) return [{ kind: "syntax", field: "$", message: "the record is not an object" }];
  for (const key of KEYS) if (!(key in record)) err("syntax", key, "is missing");
  if (errors.length > 0) return errors;
  if (record.vendor !== null && typeof record.vendor !== "string") err("syntax", "vendor", "must be a string or null");
  if (!currencyOk(record.currency)) err("syntax", "currency", `'${record.currency}' is not one of ${JSON.stringify(CURRENCIES)}`);
  if (detailMissing(record.currency, record.currency_detail)) err("syntax", "currency_detail", "is required when the currency is other");
  const items = record.line_items;
  if (!(Array.isArray(items) && items.every(isNumber))) err("syntax", "line_items", "must be a list of numbers");
  if (record.stated_total !== null && !isNumber(record.stated_total)) err("syntax", "stated_total", "must be a number or null");
  if (!isNumber(record.calculated_total)) err("syntax", "calculated_total", "must be a number");
  if (typeof record.conflict_detected !== "boolean") err("syntax", "conflict_detected", "must be true or false");
  if (typeof record.provenance !== "object" || record.provenance === null || Array.isArray(record.provenance)) err("syntax", "provenance", "must be an object");
  if (errors.length > 0) return errors;
  checkSemantics(record, err);
  for (const field of ["vendor", "currency", "stated_total"]) {
    const value = record[field];
    if (value === null || value === "unclear") continue;
    const quote = record.provenance[field];
    if (!quoteFound(quote, document)) err("ungrounded", field, `${field} has no quote that appears in the document`);
  }
  for (const field of required) {
    if (record[field] === undefined || record[field] === null || record[field] === "unclear") err("absent", field, "the document gave no value");
  }
  return errors;
}

function retryableErrors(errors: any[]): any[] {
  // TODO 4 of 10 (finish this to pass e2, e3 and e4): keep the errors a second look can fix.
  // Receives a list of { kind, field, message } errors. Returns those whose kind is in RETRYABLE (syntax, semantic, ungrounded), in order;
  // an "absent" error is never retried. Example: [{ kind: "absent" }, { kind: "syntax" }] -> [{ kind: "syntax" }]
  return [];
}

function status(errors: any[], record: any): string {
  // TODO 5 of 10 (finish this to pass m1, e3 and e5): the status of a finished extraction.
  // Receives the errors left after the last attempt and the last record. Returns "needs_review" when the only errors are "absent" ones, or
  // when there are no errors and the model flagged a conflict (record.conflict_detected); "failed" for any other error; "valid" otherwise.
  // Example: no errors and conflict_detected false -> "valid"; one absent error -> "needs_review"; one syntax error -> "failed"
  return "failed";
}

export function extractDocument(document: string, callModel: (document: string, feedback: any) => any, required: string[] = [], maxRetries = 2): any {
  let feedback: any = null;
  let attempts = 0;
  let record: any;
  let errors: any[];
  while (true) {
    attempts += 1;
    record = callModel(document, feedback);
    errors = validate(record, document, required);
    if (errors.length === 0) break;
    const retryable = retryableErrors(errors);
    if (retryable.length === 0) break;
    if (attempts > maxRetries) break;
    feedback = { previous: record, errors: retryable };
  }
  return { status: status(errors, record), record, attempts, errors };
}

function isUnset(value: unknown): boolean {
  // TODO 6 of 10 (finish this to pass e7): has a merged field no real value yet?
  // Receives a value. Returns true for null and for "unclear", so the first real value from a later chunk is kept.
  // Example: isUnset(null) -> true, isUnset("unclear") -> true, isUnset("Acme Ltd") -> false
  return false;
}

function isConflict(current: unknown, value: unknown, field: string, conflicts: string[]): boolean {
  // TODO 7 of 10 (finish this to pass e7): do two chunks disagree about a field, not yet recorded?
  // Receives the value kept so far, a later chunk's value, the field name and the fields already in `conflicts`. Returns true when the
  // values differ and the field is not yet in that list, so a conflict is recorded once.
  // Example: isConflict("A", "B", "vendor", []) -> true
  return false;
}

export function mergeChunks(records: any[]): any {
  const merged: any = { vendor: null, currency: "unclear", currency_detail: null, line_items: [], stated_total: null, calculated_total: 0, conflict_detected: false, provenance: {}, conflicts: [] };
  for (const record of records) {
    for (const field of ["vendor", "currency", "stated_total"]) {
      const value = record[field];
      if (value === null || value === undefined || value === "unclear") continue;
      const current = merged[field];
      if (isUnset(current)) {
        merged[field] = value;
        merged.provenance[field] = record.provenance[field] ?? null;
        if (field === "currency") merged.currency_detail = record.currency_detail ?? null;
      } else if (isConflict(current, value, field, merged.conflicts)) {
        merged.conflicts.push(field);
      }
    }
    merged.line_items.push(...(record.line_items ?? []));
    if (record.conflict_detected) merged.conflict_detected = true;
  }
  merged.calculated_total = round2(merged.line_items.reduce((a: number, b: number) => a + b, 0));
  if (merged.conflicts.length > 0) merged.conflict_detected = true;
  return merged;
}

function report(correct: number, valid: number, total: number): any {
  // TODO 8 of 10 (finish this to pass e8): the accuracy report, on every document and on the validated ones.
  // Receives the number of correct documents, the number of valid documents and the number of labelled documents. Returns an object with
  // all_documents (correct over total), validated_only (correct over valid), validated (= valid) and total; the two rates are rounded with
  // round2 and are 0 when the denominator is zero. Example: report(3, 6, 6) -> all_documents 0.5, validated_only 0.5
  return { all_documents: 0, validated_only: 0, validated: valid, total };
}

export function accuracy(results: Record<string, any>, labels: Record<string, any>): any {
  let valid = 0;
  let correct = 0;
  for (const [id, label] of Object.entries(labels)) {
    const result = results[id];
    const isValid = result !== undefined && result.status === "valid";
    if (isValid) valid += 1;
    if (isValid && result.record.vendor === label.vendor && result.record.stated_total === label.stated_total) correct += 1;
  }
  const total = Object.keys(labels).length;
  return report(correct, valid, total);
}

function forcedChoice(tools: string[], forced: string | null): any {
  // TODO 9 of 10 (finish this to pass e9): the tool_choice for a model that accepts a forced choice.
  // Receives the list of tool names and the name to force, or null. Returns { tool_choice, strict: true, verify_reply: false }: the forced
  // tool when `forced` is given ({ type: "tool", name: forced }), { type: "any" } when there are several tools, otherwise the one tool by name.
  // Example: (["a", "b"], null) -> { tool_choice: { type: "any" }, strict: true, verify_reply: false }
  return { tool_choice: { type: "auto" }, strict: true, verify_reply: false };
}

export function requestChoice(model: string, tools: string[], forced: string | null = null): any {
  if (NO_FORCING.has(model)) return { tool_choice: { type: "auto" }, strict: true, verify_reply: true };
  return forcedChoice(tools, forced);
}
