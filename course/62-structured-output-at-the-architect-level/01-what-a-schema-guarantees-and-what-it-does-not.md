# What a schema guarantees and what it does not

**Level:** Architect · **Module 62:** Structured output at the architect level · **Page 1 of 2**
**Exams:** A4.3; S6

**After this page you can** separate the errors a schema removes from the ones it cannot, design the fields of an extraction record so that the model is never pushed to invent a value, use `unclear` and `other` with a detail for values that do not fit an enum, and require a quote from the source as provenance for every extracted value.

Checked on 2026-10-03 against the Claude API documentation pages "Structured outputs", "Strict tool use" and "Define tools", the Agent SDK page on structured outputs and the exam guide's task statement 4.3. Nothing here called a model: the example is a pipeline's decisions, written as plain code over a scripted model. This page deepens module 25 (structured output and defensive parsing) and module 26 (tool use) and does not repeat them. Retries, conflicts, chunks and the measurement are the second page.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* for structured output (4.3) it says that tool use with a JSON schema is the most reliable way to get schema-compliant output and "eliminating JSON syntax errors", and that strict schemas "do not prevent semantic errors" such as line items that do not sum to the total or a value in the wrong field. It prescribes optional or nullable fields for information that may be absent, so that the model does not fabricate a value, `unclear` in enums for ambiguous cases, and "other" plus a detail field for extensible categories. It names `tool_choice` as `auto`, `any` or a forced tool. *What the current product does (documentation checked 2026-10-03):* constrained decoding guarantees "schema-compliant responses" with "Guaranteed field types and required fields"; the guarantee is about shape. The `tool_choice` modes are as the guide says, with a restriction: on Claude Opus 5.5, Sonnet 5.5, Fable 5.1 and Mythos 5.1, `any` and `tool` "return a 400 error", and the replacement is `auto` with strict tool use or structured outputs. On the exam, the guide's `tool_choice` answers are the keyed ones for a question about forcing; for a real pipeline on those models, use the replacement.

## Why it matters

A pipeline extracts invoices. Its schema is strict: every field is required, the currency is an enum, the totals are numbers. The JSON never fails to parse. Still, invoices with no purchase order number come back with `PO-0000`, an invoice whose line items add up to 120.50 comes back with a total of 130.00, and a vendor name is a plausible company that does not appear in the document. The schema did what it promised, and nothing it promised was this. Scenario S6 tests whether you know the boundary: syntax belongs to the schema, and meaning belongs to the checks you write.

## The idea

### Two kinds of error

| Kind | Example | Who removes it |
|---|---|---|
| Syntax | A missing key, `"2"` where a number belongs, a currency outside the enum | The schema: constrained decoding or strict tool use |
| Semantic | Items that do not add up to the total, a vendor in the currency field, a value that the source does not contain | Your code: checks that read the record against the source |

A strict schema makes the first kind rare and does nothing about the second. A pipeline that stops at schema validation reports a high success rate and passes wrong records on. The exam's question is almost always "which problem remains", and the answer is the second row.

### A field that may be absent is nullable

When a field is required and the document does not hold the value, the model has two choices: refuse, which a schema cannot express, or fill the field. It fills the field, and the value is plausible. The remedy is in the schema: a field that may be missing from the source is optional or nullable, so that `null` is a valid and honest answer. The example's `scripted_value` shows the pressure: a required string is filled with a default, and a nullable one stays null. A nullable field must come with a way to tell absent from missing-by-error, which is what the required list of the practice does: some fields must have a value for the record to be useful, and a null in one of them sends the document to review instead of passing it.

A length limit such as `minLength` is not the answer: constrained decoding does not enforce it (it is on the list of unsupported constraints), and it would not tell a real number from an invented one.

Two corollaries. Do not make every field nullable; nullability is for values that can really be absent, since a required field with a valid value is a stronger check. And do not give a default that looks like data (`PO-0000`, `unknown`): downstream code cannot tell it from a real value.

### Enums that admit ambiguity and growth

A closed enum forces a choice even when the document is silent or the value is new. Two additions keep it honest:

- `unclear` for the case in which the source does not settle the value, which the pipeline can route to review;
- `other` plus a detail field for a value the enum did not foresee: the currency `other` with `currency_detail: "CHF"`. A record with `other` and no detail is a syntax error, because the pair is the contract.

Both beat the alternative of widening the enum for every new value, which hides the question of whether the value matters.

### Provenance: the quote that proves it

A semantic check needs something to check against. For an extracted value, the most useful is the sentence it came from: a required `provenance` object that maps each field to a quote, and a check that the quote appears in the document. Three things follow. An invented value has no quote that appears in the source, so it fails the check. A reviewer sees where each value came from. And the quote costs little, because the model has the text at hand. A `null` or `unclear` value needs no quote, since there is nothing to ground.

The check is a plain substring test in code: it does not ask the model whether it is right. Provenance does not prove a value is correct, only that it is grounded; the sum check and the review of conflicts cover the rest.

### Checks that read the record

The semantic checks of the example are small: the line items add up to the calculated total, to half a cent; the stated total either equals the calculated one or the record says `conflict_detected: true`, so a difference that the model noticed is information to review and not an error to retry. These are the checks the guide names (items that do not sum to the stated total) written as functions. What to do when a check fails is the next page.

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

## Traps

1. **"A strict schema makes the extraction correct."** It is tempting because the output always parses. The exam rejects it: a schema "eliminating JSON syntax errors" and "do[es] not prevent semantic errors" such as items that do not sum to the total.
2. **"Make every field required so that nothing is missed."** It is tempting because a complete record looks better. The exam rejects it: for information that may be absent, a required field pushes the model to fabricate a value. Nullable fields let it say the document does not give one.
3. **"Add a default such as `unknown` for a missing value."** It is tempting because the field is never empty. It fails because the default looks like data to downstream code; `null`, with a required list for fields that must have a value, is explicit.
4. **"Add every new currency to the enum as it appears."** It is tempting because the enum stays closed. The exam prefers `other` with a detail field, and `unclear` for ambiguity, which keep the schema stable and make the new values visible.

## Quiz

1. An extraction schema marks the purchase order number as required, and invoices lacking one come back with believable numbers that appear nowhere in the documents. What fixes it?
   - **a**: Add a minimum length so that short numbers are refused
   - **b**: Allow a null for that field when the source gives none
   - **c**: Tell the model never to leave a required field empty
   - **d**: Set a default of `unknown` in the schema for that field

2. A strict schema is in force, and one record lists line items that add up to 120.50 beside a total of 130.00. Which statement holds?
   - **a**: The type check rejects it at validation
   - **b**: Strict mode rejects it once the tool sets `strict`
   - **c**: The required-field check rejects it as inconsistent
   - **d**: Only a check in your own code rejects it

<details>
<summary>Answer key</summary>

1. **b**. A field that may be missing from the source is nullable, so that the model is not pushed to fabricate a value. *a* is ruled out because a length limit "would not tell a real number from an invented one". *c* is ruled out because "a required field pushes the model to fabricate a value", and the instruction adds to the pressure. *d* is ruled out because a default that looks like data is a problem because "downstream code cannot tell it from a real value".
2. **d**. The guide says strict schemas do not prevent semantic errors, and a sum is one: shape is promised and meaning is not, so the check that catches it is "Your code: checks that read the record against the source". *a* is ruled out because "A strict schema makes the first kind rare and does nothing about the second", and both numbers pass a type check. *b* is ruled out because strict schemas "do not prevent semantic errors", and this record already matches its shape. *c* is ruled out because "the guarantee is about shape", which covers presence of fields and not agreement between them.

</details>

Adapted from the Claude Certified Architect - Foundations exam preparation guide by Daron Yondem (CC BY 4.0, https://creativecommons.org/licenses/by/4.0/), changed for this course.
