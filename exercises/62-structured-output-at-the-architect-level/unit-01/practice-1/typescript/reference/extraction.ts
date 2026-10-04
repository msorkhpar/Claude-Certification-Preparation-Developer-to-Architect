/** An extraction pipeline that admits absence, checks what a schema cannot, retries with feedback and is measured on every document. See ../../statement.md. */

export const CURRENCIES = ["USD", "EUR", "GBP", "other", "unclear"];
const KEYS = ["vendor", "currency", "currency_detail", "line_items", "stated_total", "calculated_total", "conflict_detected", "provenance"];
const RETRYABLE = ["syntax", "semantic", "ungrounded"];
const NO_FORCING = new Set(["claude-opus-5-5", "claude-sonnet-5-5", "claude-fable-5-1", "claude-mythos-5-1"]); // models whose API rejects tool_choice any and tool, as read on 2026-10-03

const isNumber = (value: unknown): boolean => typeof value === "number" && Number.isFinite(value);
const round2 = (x: number): number => Math.round(x * 100) / 100;

export function validate(record: any, document: string, required: string[] = []): any[] {
  const errors: any[] = [];
  const err = (kind: string, field: string, message: string) => errors.push({ kind, field, message });
  if (typeof record !== "object" || record === null || Array.isArray(record)) return [{ kind: "syntax", field: "$", message: "the record is not an object" }];
  for (const key of KEYS) if (!(key in record)) err("syntax", key, "is missing");
  if (errors.length > 0) return errors;
  if (record.vendor !== null && typeof record.vendor !== "string") err("syntax", "vendor", "must be a string or null");
  if (!CURRENCIES.includes(record.currency)) err("syntax", "currency", `'${record.currency}' is not one of ${JSON.stringify(CURRENCIES)}`);
  if (record.currency === "other" && !(typeof record.currency_detail === "string" && record.currency_detail.trim() !== "")) err("syntax", "currency_detail", "is required when the currency is other");
  const items = record.line_items;
  if (!(Array.isArray(items) && items.every(isNumber))) err("syntax", "line_items", "must be a list of numbers");
  if (record.stated_total !== null && !isNumber(record.stated_total)) err("syntax", "stated_total", "must be a number or null");
  if (!isNumber(record.calculated_total)) err("syntax", "calculated_total", "must be a number");
  if (typeof record.conflict_detected !== "boolean") err("syntax", "conflict_detected", "must be true or false");
  if (typeof record.provenance !== "object" || record.provenance === null || Array.isArray(record.provenance)) err("syntax", "provenance", "must be an object");
  if (errors.length > 0) return errors;
  const sum = (items as number[]).reduce((a, b) => a + b, 0);
  if (Math.abs(sum - record.calculated_total) > 0.005) err("semantic", "calculated_total", `${record.calculated_total} is not the sum of the line items, ${sum}`);
  const stated = record.stated_total;
  if (stated !== null && Math.abs(stated - record.calculated_total) > 0.005 && !record.conflict_detected) {
    err("semantic", "stated_total", `the stated total ${stated} differs from the calculated total ${record.calculated_total} but conflict_detected is false`);
  }
  for (const field of ["vendor", "currency", "stated_total"]) {
    const value = record[field];
    if (value === null || value === "unclear") continue;
    const quote = record.provenance[field];
    if (typeof quote !== "string" || quote === "" || !document.includes(quote)) err("ungrounded", field, `${field} has no quote that appears in the document`);
  }
  for (const field of required) {
    if (record[field] === undefined || record[field] === null || record[field] === "unclear") err("absent", field, "the document gave no value");
  }
  return errors;
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
    const retryable = errors.filter((e) => RETRYABLE.includes(e.kind));
    if (retryable.length === 0) break;
    if (attempts > maxRetries) break;
    feedback = { previous: record, errors: retryable };
  }
  let status: string;
  if (errors.length > 0) status = errors.every((e) => e.kind === "absent") ? "needs_review" : "failed";
  else if (record.conflict_detected) status = "needs_review";
  else status = "valid";
  return { status, record, attempts, errors };
}

export function mergeChunks(records: any[]): any {
  const merged: any = { vendor: null, currency: "unclear", currency_detail: null, line_items: [], stated_total: null, calculated_total: 0, conflict_detected: false, provenance: {}, conflicts: [] };
  for (const record of records) {
    for (const field of ["vendor", "currency", "stated_total"]) {
      const value = record[field];
      if (value === null || value === undefined || value === "unclear") continue;
      const current = merged[field];
      if (current === null || current === "unclear") {
        merged[field] = value;
        merged.provenance[field] = record.provenance[field] ?? null;
        if (field === "currency") merged.currency_detail = record.currency_detail ?? null;
      } else if (current !== value && !merged.conflicts.includes(field)) {
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
  return { all_documents: total ? round2(correct / total) : 0, validated_only: valid ? round2(correct / valid) : 0, validated: valid, total };
}

export function requestChoice(model: string, tools: string[], forced: string | null = null): any {
  if (NO_FORCING.has(model)) return { tool_choice: { type: "auto" }, strict: true, verify_reply: true };
  if (forced !== null) return { tool_choice: { type: "tool", name: forced }, strict: true, verify_reply: false };
  if (tools.length > 1) return { tool_choice: { type: "any" }, strict: true, verify_reply: false };
  return { tool_choice: { type: "tool", name: tools[0] }, strict: true, verify_reply: false };
}
