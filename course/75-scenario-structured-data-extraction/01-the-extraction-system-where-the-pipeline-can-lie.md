# The extraction system: where the pipeline can lie

**Level:** Architect · **Module 75:** Scenario: structured data extraction · **Page 1 of 2**
**Exams:** A4, A5; S6

**After this page you can** describe the exam's extraction system and what it owns that no model call can, read a symptom in a pipeline's figures as a failure shape with its first fix, and follow a run in which a record is retried, a missing value goes to a person and the accuracy is reported on every document.

Checked on 2026-10-04 against the Claude API documentation pages "Define tools" (the `tool_choice` options and the restriction on forced tool use), "Batch processing" and "Structured outputs", and the Architect exam guide (version 1.0, scenario 6). The example runs offline in Python, TypeScript, Java and Kotlin: the model is a table of made-up replies, so the example is about what the pipeline does with a record. This page is a capstone: it uses modules 61, 62, 63 and 68, which treat the schema, the retry, the batch and the measurement in full.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* for the extraction setting it names tool use with a JSON schema as the way to get structured output, `tool_choice` set to a forced tool so that a particular extraction runs first, and `any` when the document type is unknown. *What the current product does (documentation checked 2026-10-04):* "Not every model and setting supports forced tool use." On Claude Opus 5.5, Sonnet 5.5 and Fable 5.1, `any` and `tool` return a 400 error, and the documented replacement is `auto` with strict tool use or structured outputs. On the exam, the forced tool is the keyed answer to a question about guaranteeing which extraction runs; in a pipeline on those models, use the replacement and check the reply.

## Why it matters

Scenario S6 is the setting where a system can look healthy while it is wrong. Every reply parses, the dashboard is green, and the figure that the team reports is high. The failures that the exam asks about all sit outside the model call: in the schema that pushes the model to invent, in the retry that asks again for something the page does not hold, in the check that nobody wrote, and in a measurement that leaves out the documents that went wrong. The architect's habit is to ask what the pipeline claims and what it could be hiding.

## The idea

### The scenario in plain words

A system extracts information from unstructured documents, validates every record against a JSON schema, keeps accuracy high and hands the result to downstream systems. It must treat the odd document gracefully. The domains the exam draws on are the design of prompts and structured output, and the management of context and reliability.

### Read a symptom as a failure shape

| What the figures or the logs show | The failure shape | The first fix | Taught in |
|---|---|---|---|
| Every record parses, and some vendor names are not in their documents | A required field that pushes the model to invent | Make the field nullable and require a quote from the source | Module 62 |
| A retry loop asks ten times for a value that the page does not hold | A retry that cannot improve anything | Retry only the errors a second look can fix, and send an absent value to a person | Module 62 |
| Line items add up to one figure and the total says another, and the record passed | A semantic error that no schema can see | Check the record against itself in code | Module 62 |
| The dashboard says 99 percent and the reviewers say handwriting is hopeless | A figure measured on the survivors, or an average over easy documents | Measure every document, and break the figure down by kind | Modules 62 and 68 |
| High-confidence records are accepted untouched and nobody knows how many are wrong | An automated stream with no sample | Draw a stratified sample of what was accepted | Module 68 |
| A batch of a hundred is resubmitted whole because three entries failed | Paying again for what already succeeded | Resubmit by `custom_id`, and chunk what was too long | Module 63 |
| The model that extracted a record also confirms it | A check made in the generator's own context | A second, independent pass | Module 63 |

The documentation of the batch interface says what the table's sixth row relies on: "Batches expire if processing does not complete within 24 hours." Each request carries a unique `custom_id`, "Must be 1 to 64 characters", and the three results that did not succeed are `errored`, `canceled` and `expired`. A pipeline that resubmits by identifier pays only for what it must.

### What the pipeline owns

A model call returns a record. The pipeline owns everything that is about a document and a run, and four of those duties are where the exam's failures sit.

1. **The status of a document.** A document is `valid` only when every check passed and no conflict was flagged. It is `needs_review` when the only open matters are a value the page lacks or a conflict that the model flagged, and neither is retried. It is `failed` when an error survived the retry. The status is derived from the checks, and the model's own opinion plays no part.
2. **The retry.** One retry that carries the document, the failed record and the errors, and only for errors a second look can fix: an invented value, a total that disagrees, a value of the wrong type. A value that the document does not contain stays absent however often it is asked for.
3. **The measurement.** The accuracy of a run is the share of all documents whose delivered record is correct. A document that failed is a document the customer sent. The figure on the validated ones alone is a different figure and is always at least as high.
4. **The route.** Some documents need a person: the absent, the conflicting, the failed. Which kinds of document may go without one is decided by evidence for each kind, never by the average.

### The example

The example runs six documents through a scripted model. Two are typed, two are scanned and two are handwritten. The first typed invoice comes back right. The second comes back with a total that does not match its lines, is retried with the error attached, and is right the second time. The first scanned invoice has no vendor, and the model invents one that the document never names; the retry returns null for the vendor, which is honest, and the document is valid. The second scanned invoice has no total anywhere, so there is nothing to retry and it goes to a person. The first handwritten form carries a total that disagrees with its lines, and the model flags the conflict, so it goes to a person without a retry. The last form keeps an invented vendor on the retry and fails after two attempts. The run ends with two figures and a line for each kind.

<!-- example: m75-extraction-run tabs: python,typescript,java,kotlin -->
```python
"""An extraction run in miniature: a scripted model reads six documents, every record is validated for what a schema cannot check, only the errors a second look can fix
are retried, what the document does not hold goes to a person, and the accuracy is reported on every document and not only on the validated ones.

The model is a table of made-up replies: this example is about what the pipeline does with a record, not about what a model writes. Amounts are in cents. The shapes
(a record, an error with a kind and a field, the status of a document) are this course's design, not an Anthropic interface.
"""
import logging

log = logging.getLogger(__name__)

DOCS = {
    "d1": ("typed", "Vendor: Acme Ltd. Lines: 10.00 20.00. Total: 30.00"),
    "d2": ("typed", "Vendor: Borealis Co. Lines: 40.00 5.00. Total: 45.00"),
    "d3": ("scanned", "Lines: 8.00 2.00. Total: 10.00"),
    "d4": ("scanned", "Vendor: Corvid Inc. Lines: 12.00 8.00."),
    "d5": ("handwritten", "Vendor: Dunmore. Lines: 6.00 6.00. Total: 12.50"),
    "d6": ("handwritten", "Lines: 3.00. Total: 3.00"),
}
LABELS = {"d1": ("Acme Ltd", 3000), "d2": ("Borealis Co", 4500), "d3": (None, 1000), "d4": ("Corvid Inc", 2000), "d5": ("Dunmore", 1200), "d6": (None, 300)}


def rec(vendor, lines, total, conflict=False):
    return {"vendor": vendor, "lines": lines, "total": total, "conflict": conflict}


# what the scripted model returns on the first and on the second attempt (a missing second reply repeats the first)
REPLIES = {
    "d1": [rec("Acme Ltd", [1000, 2000], 3000)],
    "d2": [rec("Borealis Co", [4000, 500], 5400), rec("Borealis Co", [4000, 500], 4500)],
    "d3": [rec("Globex", [800, 200], 1000), rec(None, [800, 200], 1000)],
    "d4": [rec("Corvid Inc", [1200, 800], None)],
    "d5": [rec("Dunmore", [600, 600], 1250, conflict=True)],
    "d6": [rec("Hollis", [300], 300)],
}


def validate(record, text):
    """What a schema cannot check: a vendor the document never names, totals that disagree, a required total that is missing."""
    errors = []
    if record["vendor"] is not None and record["vendor"] not in text:
        errors.append(("ungrounded", "vendor"))
    if record["total"] is None:
        errors.append(("absent", "total"))
    elif record["total"] != sum(record["lines"]) and not record["conflict"]:
        errors.append(("semantic", "total"))
    return errors


def extract(doc_id, text):
    """One attempt, then one retry that carries the errors, and only when a second look can fix one. An absent value is never retried."""
    log.debug("extract input: %r", text)
    retried, replies = [], REPLIES[doc_id]
    for attempt in (1, 2):
        record = replies[min(attempt, len(replies)) - 1]
        errors = validate(record, text)
        fixable = [e for e in errors if e[0] != "absent"]
        if not fixable:
            return {"id": doc_id, "attempts": attempt, "record": record, "errors": errors, "retried": retried,
                    "status": "needs_review" if errors or record["conflict"] else "valid"}
        retried = sorted({kind for kind, _ in fixable})
    return {"id": doc_id, "attempts": 2, "record": record, "errors": errors, "retried": retried, "status": "failed"}


def percent(correct, total):
    return (200 * correct + total) // (2 * total) if total else 0


def main():
    results = []
    for doc_id, (kind, text) in DOCS.items():
        r = {**extract(doc_id, text), "kind": kind}
        label = LABELS[doc_id]
        r["correct"] = r["status"] == "valid" and (r["record"]["vendor"], r["record"]["total"]) == label
        results.append(r)
        note = f" (retried: {', '.join(r['retried'])})" if r["status"] == "valid" and r["retried"] else ""
        if r["status"] == "needs_review":
            note = " (conflict flagged)" if r["record"]["conflict"] else f" ({r['errors'][0][0]}: {r['errors'][0][1]}, not retried)"
        if r["status"] == "failed":
            note = f" ({', '.join(r['retried'])})"
        print(f"{doc_id} {kind}: {r['status']} after {r['attempts']} attempt{'s' if r['attempts'] > 1 else ''}{note}")
    valid = [r for r in results if r["status"] == "valid"]
    right_valid, right = sum(r["correct"] for r in valid), sum(r["correct"] for r in results)
    print(f"accuracy: validated only {right_valid} of {len(valid)} ({percent(right_valid, len(valid))}%), all documents {right} of {len(results)} ({percent(right, len(results))}%)")
    kinds = list(dict.fromkeys(r["kind"] for r in results))
    parts = [f"{k} {sum(r['correct'] for r in results if r['kind'] == k)}/{sum(1 for r in results if r['kind'] == k)}" for k in kinds]
    print("by kind: " + ", ".join(parts))
    ready = [k for k in kinds if sum(1 for r in results if r["kind"] == k) >= 2 and all(r["correct"] for r in results if r["kind"] == k)]
    print("automate: " + (", ".join(ready) or "none"))


if __name__ == "__main__":
    main()
```
```text
d1 typed: valid after 1 attempt
d2 typed: valid after 2 attempts (retried: semantic)
d3 scanned: valid after 2 attempts (retried: ungrounded)
d4 scanned: needs_review after 1 attempt (absent: total, not retried)
d5 handwritten: needs_review after 1 attempt (conflict flagged)
d6 handwritten: failed after 2 attempts (ungrounded)
accuracy: validated only 3 of 3 (100%), all documents 3 of 6 (50%)
by kind: typed 2/2, scanned 1/2, handwritten 0/2
automate: typed
```
```typescript
// An extraction run in miniature: a scripted model reads six documents, every record is validated for what a schema cannot check, only the errors a second look can fix
// are retried, what the document does not hold goes to a person, and the accuracy is reported on every document and not only on the validated ones.
//
// The model is a table of made-up replies: this example is about what the pipeline does with a record, not about what a model writes. Amounts are in cents. The shapes
// (a record, an error with a kind and a field, the status of a document) are this course's design, not an Anthropic interface.
import { logger } from "./logger.ts";
const log = logger("extraction_run");
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
  log.debug("extract input", text);
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
```
```text
d1 typed: valid after 1 attempt
d2 typed: valid after 2 attempts (retried: semantic)
d3 scanned: valid after 2 attempts (retried: ungrounded)
d4 scanned: needs_review after 1 attempt (absent: total, not retried)
d5 handwritten: needs_review after 1 attempt (conflict flagged)
d6 handwritten: failed after 2 attempts (ungrounded)
accuracy: validated only 3 of 3 (100%), all documents 3 of 6 (50%)
by kind: typed 2/2, scanned 1/2, handwritten 0/2
automate: typed
```
```java
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * An extraction run in miniature: a scripted model reads six documents, every record is validated for what a schema cannot check, only the errors a second look can fix
 * are retried, what the document does not hold goes to a person, and the accuracy is reported on every document and not only on the validated ones.
 *
 * <p>The model is a table of made-up replies: this example is about what the pipeline does with a record, not about what a model writes. Amounts are in cents. The shapes
 * (a record, an error with a kind and a field, the status of a document) are this course's design, not an Anthropic interface.
 */
public final class ExtractionRun {
    private static final System.Logger LOG = System.getLogger(ExtractionRun.class.getName());
    record Doc(String kind, String text) {}

    record Rec(String vendor, List<Integer> lines, Integer total, boolean conflict) {}

    record Err(String kind, String field) {}

    record Extracted(String id, int attempts, Rec record, List<Err> errors, List<String> retried, String status) {}

    static final Map<String, Doc> DOCS = new LinkedHashMap<>();
    static final Map<String, Rec> LABELS = new LinkedHashMap<>();
    static final Map<String, List<Rec>> REPLIES = new LinkedHashMap<>();

    static Rec rec(String vendor, List<Integer> lines, Integer total) {
        return new Rec(vendor, lines, total, false);
    }

    static {
        DOCS.put("d1", new Doc("typed", "Vendor: Acme Ltd. Lines: 10.00 20.00. Total: 30.00"));
        DOCS.put("d2", new Doc("typed", "Vendor: Borealis Co. Lines: 40.00 5.00. Total: 45.00"));
        DOCS.put("d3", new Doc("scanned", "Lines: 8.00 2.00. Total: 10.00"));
        DOCS.put("d4", new Doc("scanned", "Vendor: Corvid Inc. Lines: 12.00 8.00."));
        DOCS.put("d5", new Doc("handwritten", "Vendor: Dunmore. Lines: 6.00 6.00. Total: 12.50"));
        DOCS.put("d6", new Doc("handwritten", "Lines: 3.00. Total: 3.00"));
        LABELS.put("d1", rec("Acme Ltd", List.of(), 3000));
        LABELS.put("d2", rec("Borealis Co", List.of(), 4500));
        LABELS.put("d3", rec(null, List.of(), 1000));
        LABELS.put("d4", rec("Corvid Inc", List.of(), 2000));
        LABELS.put("d5", rec("Dunmore", List.of(), 1200));
        LABELS.put("d6", rec(null, List.of(), 300));
        // what the scripted model returns on the first and on the second attempt (a missing second reply repeats the first)
        REPLIES.put("d1", List.of(rec("Acme Ltd", List.of(1000, 2000), 3000)));
        REPLIES.put("d2", List.of(rec("Borealis Co", List.of(4000, 500), 5400), rec("Borealis Co", List.of(4000, 500), 4500)));
        REPLIES.put("d3", List.of(rec("Globex", List.of(800, 200), 1000), rec(null, List.of(800, 200), 1000)));
        REPLIES.put("d4", List.of(rec("Corvid Inc", List.of(1200, 800), null)));
        REPLIES.put("d5", List.of(new Rec("Dunmore", List.of(600, 600), 1250, true)));
        REPLIES.put("d6", List.of(rec("Hollis", List.of(300), 300)));
    }

    /** What a schema cannot check: a vendor the document never names, totals that disagree, a required total that is missing. */
    static List<Err> validate(Rec record, String text) {
        List<Err> errors = new ArrayList<>();
        if (record.vendor() != null && !text.contains(record.vendor())) errors.add(new Err("ungrounded", "vendor"));
        if (record.total() == null) errors.add(new Err("absent", "total"));
        else if (record.total() != record.lines().stream().mapToInt(Integer::intValue).sum() && !record.conflict()) errors.add(new Err("semantic", "total"));
        return errors;
    }

    /** One attempt, then one retry that carries the errors, and only when a second look can fix one. An absent value is never retried. */
    static Extracted extract(String docId, String text) {
        LOG.log(System.Logger.Level.DEBUG, "extract input: {0}", text);
        List<String> retried = List.of();
        List<Rec> replies = REPLIES.get(docId);
        Rec record = replies.get(0);
        List<Err> errors = List.of();
        for (int attempt = 1; attempt <= 2; attempt++) {
            record = replies.get(Math.min(attempt, replies.size()) - 1);
            errors = validate(record, text);
            List<Err> fixable = errors.stream().filter(e -> !e.kind().equals("absent")).toList();
            if (fixable.isEmpty()) return new Extracted(docId, attempt, record, errors, retried, !errors.isEmpty() || record.conflict() ? "needs_review" : "valid");
            retried = new TreeSet<>(fixable.stream().map(Err::kind).toList()).stream().toList();
        }
        return new Extracted(docId, 2, record, errors, retried, "failed");
    }

    static int percent(int correct, int total) {
        return total > 0 ? (200 * correct + total) / (2 * total) : 0;
    }

    public static void main(String[] args) {
        List<Extracted> results = new ArrayList<>();
        List<String> kinds = new ArrayList<>();
        List<Boolean> correct = new ArrayList<>();
        for (var entry : DOCS.entrySet()) {
            Extracted r = extract(entry.getKey(), entry.getValue().text());
            Rec label = LABELS.get(entry.getKey());
            results.add(r);
            kinds.add(entry.getValue().kind());
            correct.add(r.status().equals("valid") && java.util.Objects.equals(r.record().vendor(), label.vendor()) && r.record().total().equals(label.total()));
            String note = r.status().equals("valid") && !r.retried().isEmpty() ? " (retried: " + String.join(", ", r.retried()) + ")" : "";
            if (r.status().equals("needs_review")) note = r.record().conflict() ? " (conflict flagged)" : " (" + r.errors().get(0).kind() + ": " + r.errors().get(0).field() + ", not retried)";
            if (r.status().equals("failed")) note = " (" + String.join(", ", r.retried()) + ")";
            System.out.println(r.id() + " " + entry.getValue().kind() + ": " + r.status() + " after " + r.attempts() + " attempt" + (r.attempts() > 1 ? "s" : "") + note);
        }
        int valid = 0, rightValid = 0, right = 0;
        for (int i = 0; i < results.size(); i++) {
            if (results.get(i).status().equals("valid")) {
                valid++;
                if (correct.get(i)) rightValid++;
            }
            if (correct.get(i)) right++;
        }
        System.out.println("accuracy: validated only " + rightValid + " of " + valid + " (" + percent(rightValid, valid) + "%), all documents " + right + " of " + results.size() + " (" + percent(right, results.size()) + "%)");
        List<String> order = new ArrayList<>(new LinkedHashSet<>(kinds));
        List<String> parts = new ArrayList<>();
        List<String> ready = new ArrayList<>();
        for (String k : order) {
            int n = 0, ok = 0;
            for (int i = 0; i < kinds.size(); i++) {
                if (kinds.get(i).equals(k)) {
                    n++;
                    if (correct.get(i)) ok++;
                }
            }
            parts.add(k + " " + ok + "/" + n);
            if (n >= 2 && ok == n) ready.add(k);
        }
        System.out.println("by kind: " + String.join(", ", parts));
        System.out.println("automate: " + (ready.isEmpty() ? "none" : ready.stream().collect(Collectors.joining(", "))));
    }
}
```
```text
d1 typed: valid after 1 attempt
d2 typed: valid after 2 attempts (retried: semantic)
d3 scanned: valid after 2 attempts (retried: ungrounded)
d4 scanned: needs_review after 1 attempt (absent: total, not retried)
d5 handwritten: needs_review after 1 attempt (conflict flagged)
d6 handwritten: failed after 2 attempts (ungrounded)
accuracy: validated only 3 of 3 (100%), all documents 3 of 6 (50%)
by kind: typed 2/2, scanned 1/2, handwritten 0/2
automate: typed
```
```kotlin
private val log = System.getLogger("extraction_run")

/**
 * An extraction run in miniature: a scripted model reads six documents, every record is validated for what a schema cannot check, only the errors a second look can fix
 * are retried, what the document does not hold goes to a person, and the accuracy is reported on every document and not only on the validated ones.
 *
 * The model is a table of made-up replies: this example is about what the pipeline does with a record, not about what a model writes. Amounts are in cents. The shapes
 * (a record, an error with a kind and a field, the status of a document) are this course's design, not an Anthropic interface.
 */
data class Doc(val kind: String, val text: String)

data class Rec(val vendor: String?, val lines: List<Int>, val total: Int?, val conflict: Boolean = false)

data class Err(val kind: String, val field: String)

data class Extracted(val id: String, val attempts: Int, val record: Rec, val errors: List<Err>, val retried: List<String>, val status: String)

val DOCS = linkedMapOf(
    "d1" to Doc("typed", "Vendor: Acme Ltd. Lines: 10.00 20.00. Total: 30.00"),
    "d2" to Doc("typed", "Vendor: Borealis Co. Lines: 40.00 5.00. Total: 45.00"),
    "d3" to Doc("scanned", "Lines: 8.00 2.00. Total: 10.00"),
    "d4" to Doc("scanned", "Vendor: Corvid Inc. Lines: 12.00 8.00."),
    "d5" to Doc("handwritten", "Vendor: Dunmore. Lines: 6.00 6.00. Total: 12.50"),
    "d6" to Doc("handwritten", "Lines: 3.00. Total: 3.00"),
)
val LABELS = mapOf("d1" to ("Acme Ltd" to 3000), "d2" to ("Borealis Co" to 4500), "d3" to (null to 1000), "d4" to ("Corvid Inc" to 2000), "d5" to ("Dunmore" to 1200), "d6" to (null to 300))

// what the scripted model returns on the first and on the second attempt (a missing second reply repeats the first)
val REPLIES = mapOf(
    "d1" to listOf(Rec("Acme Ltd", listOf(1000, 2000), 3000)),
    "d2" to listOf(Rec("Borealis Co", listOf(4000, 500), 5400), Rec("Borealis Co", listOf(4000, 500), 4500)),
    "d3" to listOf(Rec("Globex", listOf(800, 200), 1000), Rec(null, listOf(800, 200), 1000)),
    "d4" to listOf(Rec("Corvid Inc", listOf(1200, 800), null)),
    "d5" to listOf(Rec("Dunmore", listOf(600, 600), 1250, conflict = true)),
    "d6" to listOf(Rec("Hollis", listOf(300), 300)),
)

/** What a schema cannot check: a vendor the document never names, totals that disagree, a required total that is missing. */
fun validate(record: Rec, text: String): List<Err> {
    val errors = mutableListOf<Err>()
    if (record.vendor != null && record.vendor !in text) errors += Err("ungrounded", "vendor")
    if (record.total == null) errors += Err("absent", "total")
    else if (record.total != record.lines.sum() && !record.conflict) errors += Err("semantic", "total")
    return errors
}

/** One attempt, then one retry that carries the errors, and only when a second look can fix one. An absent value is never retried. */
fun extract(docId: String, text: String): Extracted {
    log.log(System.Logger.Level.DEBUG, "extract input: {0}", text)
    var retried = listOf<String>()
    val replies = REPLIES.getValue(docId)
    var record = replies[0]
    var errors = listOf<Err>()
    for (attempt in 1..2) {
        record = replies[minOf(attempt, replies.size) - 1]
        errors = validate(record, text)
        val fixable = errors.filter { it.kind != "absent" }
        if (fixable.isEmpty()) return Extracted(docId, attempt, record, errors, retried, if (errors.isNotEmpty() || record.conflict) "needs_review" else "valid")
        retried = fixable.map { it.kind }.toSortedSet().toList()
    }
    return Extracted(docId, 2, record, errors, retried, "failed")
}

fun percent(correct: Int, total: Int): Int = if (total > 0) (200 * correct + total) / (2 * total) else 0

fun main() {
    val results = DOCS.map { (docId, doc) ->
        val r = extract(docId, doc.text)
        val label = LABELS.getValue(docId)
        Triple(r, doc.kind, r.status == "valid" && r.record.vendor == label.first && r.record.total == label.second)
    }
    for ((r, kind, _) in results) {
        var note = if (r.status == "valid" && r.retried.isNotEmpty()) " (retried: ${r.retried.joinToString(", ")})" else ""
        if (r.status == "needs_review") note = if (r.record.conflict) " (conflict flagged)" else " (${r.errors[0].kind}: ${r.errors[0].field}, not retried)"
        if (r.status == "failed") note = " (${r.retried.joinToString(", ")})"
        println("${r.id} $kind: ${r.status} after ${r.attempts} attempt${if (r.attempts > 1) "s" else ""}$note")
    }
    val valid = results.filter { it.first.status == "valid" }
    val rightValid = valid.count { it.third }
    val right = results.count { it.third }
    println("accuracy: validated only $rightValid of ${valid.size} (${percent(rightValid, valid.size)}%), all documents $right of ${results.size} (${percent(right, results.size)}%)")
    val kinds = results.map { it.second }.distinct()
    println("by kind: " + kinds.joinToString(", ") { k -> "$k ${results.count { it.second == k && it.third }}/${results.count { it.second == k }}" })
    val ready = kinds.filter { k -> results.count { it.second == k } >= 2 && results.filter { it.second == k }.all { it.third } }
    println("automate: " + ready.joinToString(", ").ifEmpty { "none" })
}
```
```text
d1 typed: valid after 1 attempt
d2 typed: valid after 2 attempts (retried: semantic)
d3 scanned: valid after 2 attempts (retried: ungrounded)
d4 scanned: needs_review after 1 attempt (absent: total, not retried)
d5 handwritten: needs_review after 1 attempt (conflict flagged)
d6 handwritten: failed after 2 attempts (ungrounded)
accuracy: validated only 3 of 3 (100%), all documents 3 of 6 (50%)
by kind: typed 2/2, scanned 1/2, handwritten 0/2
automate: typed
```
<!-- /example -->

The point is the pair of figures. Three documents are valid and all three are right, so the pipeline could report 100 percent. Six documents were sent and three are right, so the honest figure is 50 percent. The by-kind line says where the other half went: nothing was lost in the typed documents, one of the scanned ones went to a person, and neither handwritten form was delivered. Only typed documents have earned automation. All four languages print the same lines.

## Traps

These are the wrong answers that the exam's options for this scenario offer, each with the reason it is rejected.

1. **"Raise the retry limit to recover the documents that still fail."** It is tempting because failures are visible and retries are cheap. The exam rejects it: retries fix errors a second look can fix, and a value that the document does not contain stays absent however many times it is requested.
2. **"Report the accuracy of the records that passed validation."** It is tempting because it is the figure the pipeline can compute without labels for the rest. The exam rejects it: the failures leave the denominator, so the figure flatters the system and hides the kinds of document that fail.
3. **"Accept anything the model scores 95 or more."** It is tempting because the score comes for free. The exam rejects it: a self-reported score is poorly calibrated, and a stream accepted on it needs a sample that someone checks.

## Quiz

1. In the example run, a scanned invoice names its vendor but holds no total anywhere in its text, and the model returns the total as null. What does the pipeline do with that document?
   - **a**: Retry it once with the error attached, since a second look may still find a figure there
   - **b**: Send it to a person after one attempt, as no retry supplies what the page lacks
   - **c**: Compute the figure from the line items instead, since the lines imply what the total should be, and mark the record as valid
   - **d**: Fail it at once and discard the vendor that the scan did manage to read

2. The example run reports three valid documents, all correct, out of six that were sent. Which pair of figures does an honest report give?
   - **a**: A full hundred percent, with nothing else reported alongside it
   - **b**: Half of everything received and all of the accepted ones, with the gap stated
   - **c**: Half of everything received, with the accepted-only figure left out of the report
   - **d**: Three quarters, the mean of the two figures, reported alone

3. A pipeline on Claude Sonnet 5.5 must make sure that an extraction tool is used on every request, and it forces the tool through `tool_choice`. What happens, and what does the documentation recommend instead?
   - **a**: The call works, but cached message blocks are processed again from the start
   - **b**: The tool is used, and the text before the tool call is dropped
   - **c**: The call fails with a 400 error, and `auto` with strict schemas replaces it
   - **d**: The setting is ignored, and the model chooses whichever tool it likes best

<details>
<summary>Answer key</summary>

1. **b**. A missing value is routed, not retried. *a* is ruled out because the retry covers only what a second look can fix: "A value that the document does not contain stays absent however often it is asked for." *c* is ruled out because the status comes from the checks and not from a guess: "The status is derived from the checks, and the model's own opinion plays no part." *d* is ruled out because a missing value is not a failure of the document: "It is `needs_review` when the only open matters are a value the page lacks or a conflict that the model flagged, and neither is retried."
2. **b**. The two figures and the gap are the report. *a* is ruled out because the denominator is every document: "A document that failed is a document the customer sent." *c* is ruled out because the validated figure is a different figure and not an irrelevant one: "The figure on the validated ones alone is a different figure and is always at least as high." *d* is ruled out because the figures are not blended: "The accuracy of a run is the share of all documents whose delivered record is correct."
3. **c**. On the models in the documentation's table, forcing is refused and the replacement is named. *a* is ruled out because the request does not get as far as the cache: "Not every model and setting supports forced tool use." *b* is ruled out because the request does not run: "On Claude Opus 5.5, Sonnet 5.5 and Fable 5.1, `any` and `tool` return a 400 error". *d* is ruled out because a setting that the API cannot honour is rejected and not ignored: "`any` and `tool` return a 400 error, and the documented replacement is `auto` with strict tool use or structured outputs".

</details>
