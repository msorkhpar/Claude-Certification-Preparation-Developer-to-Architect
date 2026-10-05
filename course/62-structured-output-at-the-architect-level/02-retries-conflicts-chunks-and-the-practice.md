# Retries, conflicts, chunks, honest accuracy and the practice

**Level:** Architect · **Module 62:** Structured output at the architect level · **Page 2 of 2**
**Exams:** A4.4; S6

**After this page you can** retry a failed extraction with the document, the failed record and the specific errors, tell the errors a retry can fix from the ones it cannot, route absent values and flagged conflicts to review, merge the results of the chunks of a long document, report accuracy that does not hide the failures, choose `tool_choice` for the request, and write the module's practice.

Checked on 2026-10-03 against the exam guide's task statement 4.4, the Claude API documentation pages "Define tools" (`tool_choice`) and "Prompt caching" (what invalidates the cache) and the Agent SDK page on structured outputs (result subtypes). Nothing here called a model: the practice is graded by Python, TypeScript, Java and Kotlin test suites, offline, on scripted model replies (`examples/62-extraction-checks` holds the checks the page describes). This page deepens module 25 and module 54 (where forcing a tool was first treated) and does not repeat them. The schema's limits and the checks are the first page.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* for validation and retry (4.4) it prescribes retry with error feedback, appending the specific validation errors to the prompt, and notes that retries are ineffective when the information is simply absent from the source; it names self-correction checks, such as extracting a `calculated_total` beside the `stated_total` and a `conflict_detected` flag for inconsistent source data, and a `detected_pattern` field in findings so that false positives can be analysed. *What the current product does (documentation checked 2026-10-03):* a result can end with a subtype `success` and no structured output, or with `error_max_structured_output_retries` ("No valid output remained after multiple attempts"); both are failures to be handled by the caller. On a model that supports forced tool use, "changes to the `tool_choice` parameter will invalidate cached message blocks. Tool definitions and system prompts remain cached, but message content must be reprocessed." On the models that reject forcing, `auto` with strict tool use is the documented replacement. On the exam, a retry question has the answer retry with the errors, unless the value is absent from the source; a measurement question has the answer count every document.

## Why it matters

The pipeline from the first page now has nullable fields, provenance and checks. A document fails one of them. What now? The first reflex is to ask again, the same way. That works for one kind of failure and is wasted on another. A model asked again for a purchase order number that is not in the document will, at best, say again that it is not there; at worst it invents one. And when the team finally measures the pipeline, the report counts only the documents that passed validation, which leaves out exactly the documents that did not. Scenario S6 tests both: when does a retry help, and what is the accuracy.

## The idea

### Retry with the evidence

A retry that repeats the request gets the same answer with the same probability. A retry that carries what went wrong gives the model something to correct. The request on the second attempt holds three things: the original document, the failed record, and the specific errors, each with its field and a message such as "`calculated_total` 100.0 is not the sum of the line items, 120.5". The model can then fix the field that failed and keep the rest. The number of retries is bounded (two in the practice), and a document that is still invalid after the last one is marked failed: a loop without a limit is a cost the pipeline did not choose.

### What a retry can and cannot fix

| Error | Retry helps | Why |
|---|---|---|
| Syntax (wrong format, enum value outside the list) | Yes | The information is in the document; the form was wrong |
| Semantic (the items do not sum) | Yes | A second look at the document can correct the arithmetic or the field |
| Ungrounded (a quote not in the document) | Yes | The model can find the real passage or return null |
| Absent (a required value the document does not contain) | No | No second look will find what is not there |

The last row is the exam's favourite. A required value that the model reports as absent is not retried: the document goes to review, since asking again either repeats the answer or pushes the model to invent a value, which is the failure that nullable fields were introduced to prevent. The example's pipeline separates the two kinds of error, and its retry feedback lists only the first three.

### Conflicts are information

Source documents are sometimes inconsistent: the invoice states a total that is not the sum of its lines. Ask for both numbers, `stated_total` as printed and `calculated_total` as computed, and a boolean `conflict_detected`. A record that carries a difference and says so is a correct record of a bad document: it is not an error to retry, because a second look will find the same difference. It goes to a person. A record that carries a difference and says `conflict_detected: false` is a semantic error, and is retried.

### A long document in chunks

A document too long for one request is cut into chunks and each is extracted, and the records are merged. The merge is a function, and its rules are decisions: keep the first value that is neither null nor unclear, with the quote it came with; concatenate the line items; recompute the total in code, not by asking a model to add; and when a later chunk gives a different vendor or total, do not pick silently: record the field in a `conflicts` list and set `conflict_detected`, so that the document goes to review. A merge that always takes the last value or the longest is a guess presented as data.

### The accuracy that hides the failures

A pipeline has validation, review and failure outcomes. A report of "accuracy on validated documents" divides by the documents that passed, which are the easy ones, and leaves out the documents that went to review or failed. With ten documents, five correct and valid, one valid and wrong, two in review and one failed, accuracy over the validated is 5 of 6, 0.83, and accuracy over all documents is 5 of 10, 0.5. The first number is true and answers a different question. The one to report is the share of all documents extracted correctly, with the validated-only figure beside it and the counts that explain the gap. The same bias appears in any metric whose denominator is filtered by the pipeline's own judgement.

### The tool choice of the request

For extraction, the request forces the model to answer through a tool whose input is the schema. The modes are `auto`, `any` (one of the tools) and a named tool. On a model that supports forcing, a pipeline with several document types uses `any`, and a pipeline with one type may name it. On Claude Opus 5.5, Sonnet 5.5, Fable 5.1 and Mythos 5.1 the API rejects `any` and `tool`; the replacement is `auto` with `strict` on the tool, plus a check in code that the reply made the call, since `auto` allows a plain-text answer. Two cost facts for any model: forcing a tool means the model gives no sentence before the call, and changing `tool_choice` between requests invalidates cached message blocks (tools and system prompt stay cached), so a pipeline that alternates modes on a long cached document pays to reprocess the document.

### The example

<!-- example: m62-extraction-checks tabs: python,typescript,java,kotlin -->
```python
"""What a schema does not give an extraction pipeline: a field the document may lack, checks of meaning, a retry that carries feedback, and an accuracy figure that does not hide the failures.

The rules are the exam guide's for tasks 4.3 and 4.4 and the Claude documentation read on 2026-10-03 (structured outputs, "Define tools"): a schema guarantees syntax and not meaning; a field that may be missing from the source is
nullable so the model is not pushed to invent a value; a retry helps with format and structure and cannot supply what the source does not hold; a request that forces a tool is rejected by the current models, which use
`auto` with strict tool use. The "model" below is a script of fixed replies: it shows the pipeline's decisions, not what a real model would answer.
"""
import logging
import json

log = logging.getLogger(__name__)

DOC = "Invoice from Acme Tools.\nItems: 100.00 + 20.50\nTotal due: 130.00 EUR"
NO_FORCING = {"claude-opus-5-5", "claude-sonnet-5-5", "claude-fable-5-1", "claude-mythos-5-1"}


def scripted_value(document, nullable):
    """What a model under pressure does with a purchase order number the document does not contain: a required string gets filled, a nullable one stays null."""
    marker = "PO "
    if marker in document:
        return document.split(marker)[1].split()[0]
    return None if nullable else "PO-0000"


def check(record, document):
    """Checks a schema cannot make: the items add up to the total, and the quoted evidence is in the document."""
    log.debug("check input: %r", record)
    problems = []
    if abs(sum(record["items"]) - record["total"]) > 0.005:
        problems.append(f"total: the items add up to {sum(record['items'])}, not {record['total']}")
    if record["evidence"] not in document:
        problems.append("evidence: this quotation is not in the document")
    return problems


def extract(document, replies, max_retries=1):
    """Ask, check, and ask again with the document, the failed answer and the problems; give up after max_retries."""
    messages = []
    for attempt, reply in enumerate(replies[: max_retries + 1], 1):
        problems = check(reply, document)
        if not problems:
            return {"status": "valid", "attempts": attempt, "feedback": messages}
        messages.append(f"Document:\n{document}\nYour answer:\n{json.dumps(reply)}\nProblems:\n" + "\n".join(f"- {p}" for p in problems))
    return {"status": "failed", "attempts": min(len(replies), max_retries + 1), "feedback": messages}


def accuracy(outcomes):
    """outcomes: (status, correct) per document. The figure on validated records alone leaves out every document that failed."""
    valid = [correct for status, correct in outcomes if status == "valid"]
    return {"validated_only": round(sum(valid) / len(valid), 2) if valid else 0.0, "all_documents": round(sum(valid) / len(outcomes), 2) if outcomes else 0.0}


def request_choice(model, tools):
    """The tool_choice of an extraction request: any when several schemas fit, the one tool otherwise, auto with strict tool use where forcing is rejected."""
    if model in NO_FORCING:
        return {"tool_choice": "auto", "check_reply": True}
    return {"tool_choice": "any" if len(tools) > 1 else f"tool:{tools[0]}", "check_reply": False}


def main():
    no_po = "Invoice from Acme Tools. Total due: 130.00 EUR"
    print("purchase order, document without one: required ->", scripted_value(no_po, False), "| nullable ->", scripted_value(no_po, True))
    wrong = {"items": [100.0, 20.5], "total": 130.0, "evidence": "Total due: 130.00 EUR"}
    right = {"items": [100.0, 20.5, 9.5], "total": 130.0, "evidence": "Total due: 130.00 EUR"}
    fabricated = {**right, "evidence": "Total due: 130.00 USD"}
    result = extract(DOC, [wrong, right])
    print("answer 1 wrong, answer 2 right:", result["status"], "after", result["attempts"], "attempts")
    print(result["feedback"][0])
    print("two answers that stay wrong:", extract(DOC, [wrong, fabricated])["status"])
    print("accuracy of 10 documents (6 valid, 5 of them right):", accuracy([("valid", True)] * 5 + [("valid", False)] + [("failed", False)] * 4))
    for model in ("claude-haiku-4-5", "claude-sonnet-5-5"):
        print(f"{model}, two extraction tools:", request_choice(model, ["extract_invoice", "extract_receipt"]))


if __name__ == "__main__":
    main()
```
```text
purchase order, document without one: required -> PO-0000 | nullable -> None
answer 1 wrong, answer 2 right: valid after 2 attempts
Document:
Invoice from Acme Tools.
Items: 100.00 + 20.50
Total due: 130.00 EUR
Your answer:
{"items": [100.0, 20.5], "total": 130.0, "evidence": "Total due: 130.00 EUR"}
Problems:
- total: the items add up to 120.5, not 130.0
two answers that stay wrong: failed
accuracy of 10 documents (6 valid, 5 of them right): {'validated_only': 0.83, 'all_documents': 0.5}
claude-haiku-4-5, two extraction tools: {'tool_choice': 'any', 'check_reply': False}
claude-sonnet-5-5, two extraction tools: {'tool_choice': 'auto', 'check_reply': True}
```
```typescript
import { logger } from "./logger.ts";
const log = logger("extraction_checks");
/**
 * What a schema does not give an extraction pipeline: a field the document may lack, checks of meaning, a retry that carries feedback, and an accuracy figure that does not hide the failures.
 *
 * The rules are the exam guide's for tasks 4.3 and 4.4 and the Claude documentation read on 2026-10-03 (structured outputs, "Define tools"): a schema guarantees syntax and not meaning; a field that may be missing from the source is
 * nullable so the model is not pushed to invent a value; a retry helps with format and structure and cannot supply what the source does not hold; a request that forces a tool is rejected by the current models, which use
 * `auto` with strict tool use. The "model" below is a script of fixed replies: it shows the pipeline's decisions, not what a real model would answer.
 */
export const DOC = "Invoice from Acme Tools.\nItems: 100.00 + 20.50\nTotal due: 130.00 EUR";
const NO_FORCING = new Set(["claude-opus-5-5", "claude-sonnet-5-5", "claude-fable-5-1", "claude-mythos-5-1"]);
export type Answer = { items: number[]; total: number; evidence: string };

/** What a model under pressure does with a purchase order number the document does not contain: a required string gets filled, a nullable one stays null. */
export function scriptedValue(document: string, nullable: boolean): string | null {
  const marker = "PO ";
  if (document.includes(marker)) return document.split(marker)[1].split(/\s+/)[0];
  return nullable ? null : "PO-0000";
}

/** Checks a schema cannot make: the items add up to the total, and the quoted evidence is in the document. */
export function check(record: Answer, document: string): string[] {
  log.debug("check input", record);
  const problems: string[] = [];
  const sum = record.items.reduce((a, b) => a + b, 0);
  if (Math.abs(sum - record.total) > 0.005) problems.push(`total: the items add up to ${sum}, not ${record.total}`);
  if (!document.includes(record.evidence)) problems.push("evidence: this quotation is not in the document");
  return problems;
}

/** Ask, check, and ask again with the document, the failed answer and the problems; give up after maxRetries. */
export function extract(document: string, replies: Answer[], maxRetries = 1): { status: string; attempts: number; feedback: string[] } {
  const feedback: string[] = [];
  const tried = replies.slice(0, maxRetries + 1);
  for (const [i, reply] of tried.entries()) {
    const problems = check(reply, document);
    if (problems.length === 0) return { status: "valid", attempts: i + 1, feedback };
    feedback.push(`Document:\n${document}\nYour answer:\n${JSON.stringify(reply)}\nProblems:\n` + problems.map((p) => `- ${p}`).join("\n"));
  }
  return { status: "failed", attempts: tried.length, feedback };
}

/** outcomes: [status, correct] per document. The figure on validated records alone leaves out every document that failed. */
export function accuracy(outcomes: Array<[string, boolean]>): { validated_only: number; all_documents: number } {
  const valid = outcomes.filter(([status]) => status === "valid").map(([, correct]) => (correct ? 1 : 0) as number);
  const right = valid.reduce((a, b) => a + b, 0);
  return { validated_only: valid.length ? Math.round((right / valid.length) * 100) / 100 : 0, all_documents: outcomes.length ? Math.round((right / outcomes.length) * 100) / 100 : 0 };
}

/** The tool_choice of an extraction request: any when several schemas fit, the one tool otherwise, auto with strict tool use where forcing is rejected. */
export function requestChoice(model: string, tools: string[]): { tool_choice: string; check_reply: boolean } {
  if (NO_FORCING.has(model)) return { tool_choice: "auto", check_reply: true };
  return { tool_choice: tools.length > 1 ? "any" : `tool:${tools[0]}`, check_reply: false };
}

function main() {
  const noPo = "Invoice from Acme Tools. Total due: 130.00 EUR";
  console.log("purchase order, document without one: required ->", scriptedValue(noPo, false), "| nullable ->", scriptedValue(noPo, true));
  const wrong: Answer = { items: [100.0, 20.5], total: 130.0, evidence: "Total due: 130.00 EUR" };
  const right: Answer = { items: [100.0, 20.5, 9.5], total: 130.0, evidence: "Total due: 130.00 EUR" };
  const fabricated: Answer = { ...right, evidence: "Total due: 130.00 USD" };
  const result = extract(DOC, [wrong, right]);
  console.log("answer 1 wrong, answer 2 right:", result.status, "after", result.attempts, "attempts");
  console.log(result.feedback[0]);
  console.log("two answers that stay wrong:", extract(DOC, [wrong, fabricated]).status);
  const outcomes: Array<[string, boolean]> = [...Array(5).fill(["valid", true]), ["valid", false], ...Array(4).fill(["failed", false])];
  console.log("accuracy of 10 documents (6 valid, 5 of them right):", JSON.stringify(accuracy(outcomes)));
  for (const model of ["claude-haiku-4-5", "claude-sonnet-5-5"]) console.log(`${model}, two extraction tools:`, JSON.stringify(requestChoice(model, ["extract_invoice", "extract_receipt"])));
}

if (import.meta.main) main();
```
```text
purchase order, document without one: required -> PO-0000 | nullable -> null
answer 1 wrong, answer 2 right: valid after 2 attempts
Document:
Invoice from Acme Tools.
Items: 100.00 + 20.50
Total due: 130.00 EUR
Your answer:
{"items":[100,20.5],"total":130,"evidence":"Total due: 130.00 EUR"}
Problems:
- total: the items add up to 120.5, not 130
two answers that stay wrong: failed
accuracy of 10 documents (6 valid, 5 of them right): {"validated_only":0.83,"all_documents":0.5}
claude-haiku-4-5, two extraction tools: {"tool_choice":"any","check_reply":false}
claude-sonnet-5-5, two extraction tools: {"tool_choice":"auto","check_reply":true}
```
```java
import static harness.Show.py;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * What a schema does not give an extraction pipeline: a field the document may lack, checks of meaning, a retry that carries feedback, and an accuracy figure that does not hide the failures.
 *
 * <p>The rules are the exam guide's for tasks 4.3 and 4.4 and the Claude documentation read on 2026-10-03 (structured outputs, "Define tools"): a schema guarantees syntax and not meaning; a field that may be missing from the source is
 * nullable so the model is not pushed to invent a value; a retry helps with format and structure and cannot supply what the source does not hold; a request that forces a tool is rejected by the current models, which use
 * `auto` with strict tool use. The "model" below is a script of fixed replies: it shows the pipeline's decisions, not what a real model would answer.
 */
public final class ExtractionChecks {
    private static final System.Logger LOG = System.getLogger(ExtractionChecks.class.getName());
    static final String DOC = "Invoice from Acme Tools.\nItems: 100.00 + 20.50\nTotal due: 130.00 EUR";
    static final Set<String> NO_FORCING = Set.of("claude-opus-5-5", "claude-sonnet-5-5", "claude-fable-5-1", "claude-mythos-5-1");

    /** What a reply holds: the item amounts, the total and the quotation that shows where the total comes from. */
    record Invoice(List<Double> items, double total, String evidence) {
        Invoice withEvidence(String other) {
            return new Invoice(items, total, other);
        }

        /** The reply as JSON text, in the spacing Python's json.dumps uses, so every language edition prints the same. */
        String toJson() {
            String list = items.stream().map(String::valueOf).collect(Collectors.joining(", ", "[", "]"));
            return "{\"items\": " + list + ", \"total\": " + total + ", \"evidence\": \"" + evidence.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"}";
        }
    }

    /** The end of an extraction: its status, the attempts made and the feedback messages sent. */
    record Extraction(String status, int attempts, List<String> feedback) {}

    /** One document's outcome: its status and whether the record was right. */
    record Outcome(String status, boolean correct) {}

    /** What a model under pressure does with a purchase order number the document does not contain: a required string gets filled, a nullable one stays null. */
    static String scriptedValue(String document, boolean nullable) {
        String marker = "PO ";
        if (document.contains(marker)) return document.split(Pattern.quote(marker), -1)[1].strip().split("\\s+")[0];
        return nullable ? null : "PO-0000";
    }

    /** Checks a schema cannot make: the items add up to the total, and the quoted evidence is in the document. */
    static List<String> check(Invoice record, String document) {
        LOG.log(System.Logger.Level.DEBUG, "check input: {0}", record);
        List<String> problems = new ArrayList<>();
        double sum = 0;
        for (double item : record.items()) sum += item;
        if (Math.abs(sum - record.total()) > 0.005) problems.add("total: the items add up to " + sum + ", not " + record.total());
        if (!document.contains(record.evidence())) problems.add("evidence: this quotation is not in the document");
        return problems;
    }

    /** Ask, check, and ask again with the document, the failed answer and the problems; give up after maxRetries. */
    static Extraction extract(String document, List<Invoice> replies, int maxRetries) {
        List<String> messages = new ArrayList<>();
        int attempt = 0;
        for (Invoice reply : replies.subList(0, Math.min(replies.size(), maxRetries + 1))) {
            attempt++;
            List<String> problems = check(reply, document);
            if (problems.isEmpty()) return new Extraction("valid", attempt, messages);
            messages.add("Document:\n" + document + "\nYour answer:\n" + reply.toJson() + "\nProblems:\n" + problems.stream().map(p -> "- " + p).collect(Collectors.joining("\n")));
        }
        return new Extraction("failed", Math.min(replies.size(), maxRetries + 1), messages);
    }

    static Extraction extract(String document, List<Invoice> replies) {
        return extract(document, replies, 1);
    }

    /** outcomes: (status, correct) per document. The figure on validated records alone leaves out every document that failed. */
    static Map<String, Double> accuracy(List<Outcome> outcomes) {
        List<Boolean> valid = outcomes.stream().filter(o -> o.status().equals("valid")).map(Outcome::correct).toList();
        long right = valid.stream().filter(c -> c).count();
        Map<String, Double> out = new LinkedHashMap<>();
        out.put("validated_only", valid.isEmpty() ? 0.0 : Math.round((double) right / valid.size() * 100) / 100.0);
        out.put("all_documents", outcomes.isEmpty() ? 0.0 : Math.round((double) right / outcomes.size() * 100) / 100.0);
        return out;
    }

    /** The tool_choice of an extraction request: any when several schemas fit, the one tool otherwise, auto with strict tool use where forcing is rejected. */
    static Map<String, Object> requestChoice(String model, List<String> tools) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (NO_FORCING.contains(model)) {
            out.put("tool_choice", "auto");
            out.put("check_reply", true);
        } else {
            out.put("tool_choice", tools.size() > 1 ? "any" : "tool:" + tools.get(0));
            out.put("check_reply", false);
        }
        return out;
    }

    static List<Outcome> repeat(String status, boolean correct, int times) {
        List<Outcome> out = new ArrayList<>();
        for (int i = 0; i < times; i++) out.add(new Outcome(status, correct));
        return out;
    }

    public static void main(String[] args) {
        String noPo = "Invoice from Acme Tools. Total due: 130.00 EUR";
        String required = scriptedValue(noPo, false), nullable = scriptedValue(noPo, true);
        System.out.println("purchase order, document without one: required -> " + required + " | nullable -> " + (nullable == null ? "None" : nullable));
        Invoice wrong = new Invoice(List.of(100.0, 20.5), 130.0, "Total due: 130.00 EUR");
        Invoice right = new Invoice(List.of(100.0, 20.5, 9.5), 130.0, "Total due: 130.00 EUR");
        Invoice fabricated = right.withEvidence("Total due: 130.00 USD");
        Extraction result = extract(DOC, List.of(wrong, right));
        System.out.println("answer 1 wrong, answer 2 right: " + result.status() + " after " + result.attempts() + " attempts");
        System.out.println(result.feedback().get(0));
        System.out.println("two answers that stay wrong: " + extract(DOC, List.of(wrong, fabricated)).status());
        List<Outcome> outcomes = new ArrayList<>(repeat("valid", true, 5));
        outcomes.addAll(repeat("valid", false, 1));
        outcomes.addAll(repeat("failed", false, 4));
        System.out.println("accuracy of 10 documents (6 valid, 5 of them right): " + py(accuracy(outcomes)));
        for (String model : List.of("claude-haiku-4-5", "claude-sonnet-5-5")) {
            System.out.println(model + ", two extraction tools: " + py(requestChoice(model, List.of("extract_invoice", "extract_receipt"))));
        }
    }
}
```
```text
purchase order, document without one: required -> PO-0000 | nullable -> None
answer 1 wrong, answer 2 right: valid after 2 attempts
Document:
Invoice from Acme Tools.
Items: 100.00 + 20.50
Total due: 130.00 EUR
Your answer:
{"items": [100.0, 20.5], "total": 130.0, "evidence": "Total due: 130.00 EUR"}
Problems:
- total: the items add up to 120.5, not 130.0
two answers that stay wrong: failed
accuracy of 10 documents (6 valid, 5 of them right): {'validated_only': 0.83, 'all_documents': 0.5}
claude-haiku-4-5, two extraction tools: {'tool_choice': 'any', 'check_reply': False}
claude-sonnet-5-5, two extraction tools: {'tool_choice': 'auto', 'check_reply': True}
```
```kotlin
import harness.Show.py

private val log = System.getLogger("extraction_checks")

/**
 * What a schema does not give an extraction pipeline: a field the document may lack, checks of meaning, a retry that carries feedback, and an accuracy figure that does not hide the failures.
 *
 * The rules are the exam guide's for tasks 4.3 and 4.4 and the Claude documentation read on 2026-10-03 (structured outputs, "Define tools"): a schema guarantees syntax and not meaning; a field that may be missing from the source is
 * nullable so the model is not pushed to invent a value; a retry helps with format and structure and cannot supply what the source does not hold; a request that forces a tool is rejected by the current models, which use
 * `auto` with strict tool use. The "model" below is a script of fixed replies: it shows the pipeline's decisions, not what a real model would answer.
 */
const val DOC = "Invoice from Acme Tools.\nItems: 100.00 + 20.50\nTotal due: 130.00 EUR"
val NO_FORCING = setOf("claude-opus-5-5", "claude-sonnet-5-5", "claude-fable-5-1", "claude-mythos-5-1")

/** What a reply holds: the item amounts, the total and the quotation that shows where the total comes from. */
data class Invoice(val items: List<Double>, val total: Double, val evidence: String) {
    /** The reply as JSON text, in the spacing Python's json.dumps uses, so every language edition prints the same. */
    fun toJson(): String = "{\"items\": ${items.joinToString(", ", "[", "]")}, \"total\": $total, \"evidence\": \"${evidence.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")}\"}"
}

/** The end of an extraction: its status, the attempts made and the feedback messages sent. */
data class Extraction(val status: String, val attempts: Int, val feedback: List<String>)

/** What a model under pressure does with a purchase order number the document does not contain: a required string gets filled, a nullable one stays null. */
fun scriptedValue(document: String, nullable: Boolean): String? {
    val marker = "PO "
    if (marker in document) return document.split(marker)[1].trim().split(Regex("\\s+"))[0]
    return if (nullable) null else "PO-0000"
}

/** Checks a schema cannot make: the items add up to the total, and the quoted evidence is in the document. */
fun check(record: Invoice, document: String): List<String> {
    log.log(System.Logger.Level.DEBUG, "check input: {0}", record)
    val problems = mutableListOf<String>()
    val sum = record.items.fold(0.0) { a, b -> a + b }
    if (Math.abs(sum - record.total) > 0.005) problems += "total: the items add up to $sum, not ${record.total}"
    if (record.evidence !in document) problems += "evidence: this quotation is not in the document"
    return problems
}

/** Ask, check, and ask again with the document, the failed answer and the problems; give up after maxRetries. */
fun extract(document: String, replies: List<Invoice>, maxRetries: Int = 1): Extraction {
    val messages = mutableListOf<String>()
    for ((index, reply) in replies.take(maxRetries + 1).withIndex()) {
        val problems = check(reply, document)
        if (problems.isEmpty()) return Extraction("valid", index + 1, messages)
        messages += "Document:\n$document\nYour answer:\n${reply.toJson()}\nProblems:\n" + problems.joinToString("\n") { "- $it" }
    }
    return Extraction("failed", minOf(replies.size, maxRetries + 1), messages)
}

/** outcomes: (status, correct) per document. The figure on validated records alone leaves out every document that failed. */
fun accuracy(outcomes: List<Pair<String, Boolean>>): Map<String, Double> {
    val valid = outcomes.filter { it.first == "valid" }.map { it.second }
    val right = valid.count { it }
    return linkedMapOf(
        "validated_only" to if (valid.isEmpty()) 0.0 else Math.round(right.toDouble() / valid.size * 100) / 100.0,
        "all_documents" to if (outcomes.isEmpty()) 0.0 else Math.round(right.toDouble() / outcomes.size * 100) / 100.0,
    )
}

/** The tool_choice of an extraction request: any when several schemas fit, the one tool otherwise, auto with strict tool use where forcing is rejected. */
fun requestChoice(model: String, tools: List<String>): Map<String, Any> =
    if (model in NO_FORCING) linkedMapOf("tool_choice" to "auto", "check_reply" to true)
    else linkedMapOf("tool_choice" to if (tools.size > 1) "any" else "tool:${tools[0]}", "check_reply" to false)

fun main() {
    val noPo = "Invoice from Acme Tools. Total due: 130.00 EUR"
    println("purchase order, document without one: required -> ${scriptedValue(noPo, false)} | nullable -> ${scriptedValue(noPo, true) ?: "None"}")
    val wrong = Invoice(listOf(100.0, 20.5), 130.0, "Total due: 130.00 EUR")
    val right = Invoice(listOf(100.0, 20.5, 9.5), 130.0, "Total due: 130.00 EUR")
    val fabricated = right.copy(evidence = "Total due: 130.00 USD")
    val result = extract(DOC, listOf(wrong, right))
    println("answer 1 wrong, answer 2 right: ${result.status} after ${result.attempts} attempts")
    println(result.feedback[0])
    println("two answers that stay wrong: ${extract(DOC, listOf(wrong, fabricated)).status}")
    val outcomes = List(5) { "valid" to true } + listOf("valid" to false) + List(4) { "failed" to false }
    println("accuracy of 10 documents (6 valid, 5 of them right): ${py(accuracy(outcomes))}")
    for (model in listOf("claude-haiku-4-5", "claude-sonnet-5-5")) println("$model, two extraction tools: ${py(requestChoice(model, listOf("extract_invoice", "extract_receipt")))}")
}
```
```text
purchase order, document without one: required -> PO-0000 | nullable -> None
answer 1 wrong, answer 2 right: valid after 2 attempts
Document:
Invoice from Acme Tools.
Items: 100.00 + 20.50
Total due: 130.00 EUR
Your answer:
{"items": [100.0, 20.5], "total": 130.0, "evidence": "Total due: 130.00 EUR"}
Problems:
- total: the items add up to 120.5, not 130.0
two answers that stay wrong: failed
accuracy of 10 documents (6 valid, 5 of them right): {'validated_only': 0.83, 'all_documents': 0.5}
claude-haiku-4-5, two extraction tools: {'tool_choice': 'any', 'check_reply': False}
claude-sonnet-5-5, two extraction tools: {'tool_choice': 'auto', 'check_reply': True}
```
<!-- /example -->

### The practice: an extraction pipeline

The practice is in [`exercises/62-structured-output-at-the-architect-level`](../../exercises/62-structured-output-at-the-architect-level/unit-01/practice-1/statement.md). You write the validation of a record (syntax, semantic, ungrounded and absent errors), the extraction loop with feedback retries and a status, the merge of the chunks, the accuracy over all documents, and the request's tool choice. It is graded in Python, TypeScript, Java and Kotlin; the statement lists ten cases, each saying what you should see when it works.

## Traps

1. **"Retry the extraction until the field is filled."** It is tempting because the field is required. The exam rejects it for a value that is absent from the source: a retry cannot supply it and may fabricate it. The document goes to review.
2. **"On failure, send the same request again."** It is tempting because it is simple. The exam rejects it: the retry that works carries the document, the failed record and the specific errors.
3. **"Report accuracy on the documents that passed validation."** It is tempting because those documents have a clean verdict. The exam rejects it: the denominator leaves out the failures, so the figure overstates the pipeline. Count every document.
4. **"When two chunks disagree about the vendor, take the later one."** It is tempting because it keeps the merge simple. The exam rejects it: a disagreement is a conflict to record and send to review, and not a choice to make silently.

## Quiz

1. An extraction fails a check because the printed total differs from the sum of the lines, and the record has `conflict_detected` set to true. What should the pipeline do?
   - **a**: Retry with the error message appended to the prompt
   - **b**: Replace the printed total with the computed one
   - **c**: Route it to a person with the discrepancy noted
   - **d**: Mark the document failed once the retry limit is reached

2. A pipeline reports 98 percent accuracy over the documents that passed its checks, while 30 percent of all documents did not pass. What is wrong with the figure?
   - **a**: It leaves out the failures, so it flatters the system
   - **b**: It is too low, because a passed document is always correct
   - **c**: It counts each document twice, once per validation rule
   - **d**: It is right, since only passed documents have a verdict

<details>
<summary>Answer key</summary>

1. **c**. A flagged conflict is information about a bad source, and a second look finds the same difference. *a* is ruled out because "a second look will find the same difference", so the retry cannot change the outcome. *b* is ruled out because "A record that carries a difference and says so is a correct record of a bad document", and replacing the printed value hides what it says. *d* is ruled out because this record is not invalid, since "a document that is still invalid after the last one is marked failed" applies to errors that are retried.
2. **a**. The denominator is filtered by the pipeline's own verdict, which drops the documents most likely to be wrong. *b* is ruled out because the report "divides by the documents that passed", so it overstates and does not understate. *c* is ruled out because it "leaves out the documents that went to review or failed", and nothing is counted twice. *d* is ruled out because "The first number is true and answers a different question".

</details>

## Module quiz

This quiz covers both pages of the module.

1. Scenario S6, structured data extraction. A 300-page contract is cut into chunks, and the per-chunk results are merged. Two chunks give different vendor names. Which merge rule fits?
   - **a**: Take the later value, since it was read with more context
   - **b**: Keep the first value, note the clash and send it to a person
   - **c**: Take the longer name, since it carries more detail
   - **d**: Drop the field from the merged result to avoid a wrong value

2. Scenario S6, structured data extraction. A team extracts invoice fields. Each failed extraction gets two more attempts. One failure is a currency written outside the allowed list, and another is a purchase order number that the document does not contain but downstream needs. Which handling is right?
   - **a**: Send the first back with its error, and route the second to review untried
   - **b**: Send both back with their errors until each field has a value
   - **c**: Send neither back and mark both documents failed at once
   - **d**: Route the first to review and send the second back with a stricter, firmer prompt

3. Scenario S6, structured data extraction. A team extracts invoice fields with a strict schema through a forced tool, and caches a long instruction block and a 200-page document. For some requests it switches `tool_choice` from `any` to `auto` and back, and costs run above plan. What explains it?
   - **a**: Each change also discards the stored tool definitions and the system prompt text
   - **b**: Moving between modes turns off structured output for the next request
   - **c**: Each change discards stored conversation content, which is processed again
   - **d**: Requests in automatic mode are billed at a higher rate than the others

<details>
<summary>Answer key</summary>

1. **b**. A disagreement between chunks is a conflict to record and review, and not a choice to make silently. *a* is ruled out because "A merge that always takes the last value or the longest is a guess presented as data". *c* is ruled out because a later chunk with a different vendor is to be handled with "do not pick silently", and choosing the longer name is the same kind of pick. *d* is ruled out because the rule is to record the clash "so that the document goes to review", and dropping the field hides it.
2. **a**. The currency error is fixable by a second look and the missing number is absent from the source. *b* is ruled out because for an absent value "No second look will find what is not there". *c* is ruled out because a format error is what feedback is for, since "The information is in the document; the form was wrong". *d* is ruled out because it inverts the two, while "A required value that the model reports as absent is not retried".
3. **c**. Changing the mode invalidates cached message blocks, so the content must be reprocessed. *a* is ruled out because "Tool definitions and system prompts remain cached". *b* is ruled out because the documented effect of a change is on the cache: "changing tool_choice between requests invalidates cached message blocks". *d* is ruled out because the cost described is reprocessing, since a pipeline that alternates modes "pays to reprocess the document".

</details>

Adapted from the Claude Certified Architect - Foundations exam preparation guide by Daron Yondem (CC BY 4.0, https://creativecommons.org/licenses/by/4.0/), changed for this course.
