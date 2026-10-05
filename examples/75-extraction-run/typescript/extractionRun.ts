// An extraction run in miniature: a scripted model reads six documents, every record is validated for what a schema cannot check, only the errors a second look can fix
// are retried, what the document does not hold goes to a person, and the accuracy is reported on every document and not only on the validated ones.
//
// The model is a table of made-up replies: this example is about what the pipeline does with a record, not about what a model writes. Amounts are in cents. The shapes
// (a record, an error with a kind and a field, the status of a document) are this course's design, not an Anthropic interface.
export type Rec = { vendor: string | null; lines: number[]; total: number | null; conflict: boolean };
export type Err = [kind: string, field: string];
export type Extracted = { id: string; attempts: number; record: Rec; errors: Err[]; retried: string[]; status: string };

export const DOCS: Record<string, [kind: string, text: string]> = {
  d1: ["typed", "Vendor: Acme Ltd. Lines: 10.00 20.00. Total: 30.00"],
  d2: ["typed", "Vendor: Borealis Co. Lines: 40.00 5.00. Total: 45.00"],
  d3: ["scanned", "Lines: 8.00 2.00. Total: 10.00"],
  d4: ["scanned", "Vendor: Corvid Inc. Lines: 12.00 8.00."],
  d5: ["handwritten", "Vendor: Dunmore. Lines: 6.00 6.00. Total: 12.50"],
  d6: ["handwritten", "Lines: 3.00. Total: 3.00"],
};
const LABELS: Record<string, [string | null, number]> = { d1: ["Acme Ltd", 3000], d2: ["Borealis Co", 4500], d3: [null, 1000], d4: ["Corvid Inc", 2000], d5: ["Dunmore", 1200], d6: [null, 300] };

const rec = (vendor: string | null, lines: number[], total: number | null, conflict = false): Rec => ({ vendor, lines, total, conflict });

// what the scripted model returns on the first and on the second attempt (a missing second reply repeats the first)
export const REPLIES: Record<string, Rec[]> = {
  d1: [rec("Acme Ltd", [1000, 2000], 3000)],
  d2: [rec("Borealis Co", [4000, 500], 5400), rec("Borealis Co", [4000, 500], 4500)],
  d3: [rec("Globex", [800, 200], 1000), rec(null, [800, 200], 1000)],
  d4: [rec("Corvid Inc", [1200, 800], null)],
  d5: [rec("Dunmore", [600, 600], 1250, true)],
  d6: [rec("Hollis", [300], 300)],
};

/** What a schema cannot check: a vendor the document never names, totals that disagree, a required total that is missing. */
export function validate(record: Rec, text: string): Err[] {
  const errors: Err[] = [];
  if (record.vendor !== null && !text.includes(record.vendor)) errors.push(["ungrounded", "vendor"]);
  if (record.total === null) errors.push(["absent", "total"]);
  else if (record.total !== record.lines.reduce((a, b) => a + b, 0) && !record.conflict) errors.push(["semantic", "total"]);
  return errors;
}

/** One attempt, then one retry that carries the errors, and only when a second look can fix one. An absent value is never retried. */
export function extract(docId: string, text: string): Extracted {
  let retried: string[] = [];
  const replies = REPLIES[docId];
  let record = replies[0];
  let errors: Err[] = [];
  for (const attempt of [1, 2]) {
    record = replies[Math.min(attempt, replies.length) - 1];
    errors = validate(record, text);
    const fixable = errors.filter(([kind]) => kind !== "absent");
    if (fixable.length === 0) return { id: docId, attempts: attempt, record, errors, retried, status: errors.length > 0 || record.conflict ? "needs_review" : "valid" };
    retried = [...new Set(fixable.map(([kind]) => kind))].sort();
  }
  return { id: docId, attempts: 2, record, errors, retried, status: "failed" };
}

const percent = (correct: number, total: number) => (total ? Math.floor((200 * correct + total) / (2 * total)) : 0);

function main() {
  const results = Object.entries(DOCS).map(([docId, [kind, text]]) => {
    const r = extract(docId, text);
    const label = LABELS[docId];
    return { ...r, kind, correct: r.status === "valid" && r.record.vendor === label[0] && r.record.total === label[1] };
  });
  for (const r of results) {
    let note = r.status === "valid" && r.retried.length > 0 ? ` (retried: ${r.retried.join(", ")})` : "";
    if (r.status === "needs_review") note = r.record.conflict ? " (conflict flagged)" : ` (${r.errors[0][0]}: ${r.errors[0][1]}, not retried)`;
    if (r.status === "failed") note = ` (${r.retried.join(", ")})`;
    console.log(`${r.id} ${r.kind}: ${r.status} after ${r.attempts} attempt${r.attempts > 1 ? "s" : ""}${note}`);
  }
  const valid = results.filter((r) => r.status === "valid");
  const rightValid = valid.filter((r) => r.correct).length;
  const right = results.filter((r) => r.correct).length;
  console.log(`accuracy: validated only ${rightValid} of ${valid.length} (${percent(rightValid, valid.length)}%), all documents ${right} of ${results.length} (${percent(right, results.length)}%)`);
  const kinds = [...new Set(results.map((r) => r.kind))];
  console.log("by kind: " + kinds.map((k) => `${k} ${results.filter((r) => r.kind === k && r.correct).length}/${results.filter((r) => r.kind === k).length}`).join(", "));
  const ready = kinds.filter((k) => results.filter((r) => r.kind === k).length >= 2 && results.filter((r) => r.kind === k).every((r) => r.correct));
  console.log("automate: " + (ready.join(", ") || "none"));
}

if (import.meta.main) main();
