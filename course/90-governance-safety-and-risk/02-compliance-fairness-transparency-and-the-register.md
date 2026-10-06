# Compliance, fairness, transparency and the register

**Level:** Architect Professional · **Module 90:** Governance, safety and risk · **Page 2 of 2**
**Exams:** P5, P6

**After this page you can** turn a compliance regime into design requirements without claiming a certification, set a retention window with a floor, a ceiling and a legal hold, make erasure a matter of removing the map from tokens to people, measure fairness as a gap between segments, tell users that AI helped, and write the risk register that ties every risk to a control and an owner.

Checked on 2026-10-04 against the Claude documentation pages "Zero data retention" and "Legal and compliance" for Claude Code and the platform page on content moderation, the Claude Certified Architect, Professional exam guide (version 1.0, domain 5), and by running the example and the practice offline in the course container. Nothing here called a model, and the records, tokens and counts are invented. This page deepens module 10 (safety, privacy and policy) and module 83 (where the data goes, and the identifier boundary) to a design that must be defended to an auditor. The controls and the failure modes are the first page.

> **Exam guide and current product.** *What the guide states:* domain 5 asks the candidate to "Ensure compliance with regulations (e.g., GDPR, HIPAA, FedRAMP)" and to "Address ethical AI considerations (bias, fairness, transparency)". *What the current product's documentation says (pages read 2026-10-04):* the documentation describes the data arrangements a deployment can choose (retention periods, zero data retention for eligible products, the organisations that act as data processor on each platform) and points to a trust centre for the compliance material; it does not state that a design is compliant with any regulation, and it gives no fairness metric or threshold. Compliance, fairness and transparency are therefore design judgements, taught here as the exam's strategies. This page makes no claim that any regime is met, and the day counts, thresholds and counts of the example and the practice are this course's design values: take the legal reading from your own counsel and the vendor's trust centre.

## Why it matters

A regulator asks an insurer to show what its claims assistant did on a given day, and a customer asks that their data be deleted. The first team kept every prompt and reply for ten years, so it can show everything and has a second copy of the customers' data to defend. The second team kept nothing, so it can show nothing. A third team kept a record of identifiers, sizes and outcomes for a year, with the map from tokens to people in one place that it can delete: it proves what happened, and the deletion is one operation. The exam asks which design a given requirement calls for, and how a fairness or transparency duty becomes a part of the system.

## The idea

### A regime is a list of requirements, not a feature

The guide names three regimes: GDPR, HIPAA and FedRAMP. The architect's use of them is the same for all. Read the regime as requirements on the design (where data may sit, who may process it, how long it is kept, what a person may demand, what must be proven), map each requirement to a part of the system, and name the arrangements that have to exist outside the code, such as an agreement with the processor. Three limits keep this honest.

- **Compliance belongs to the deployment and its agreements.** A model does not make a system compliant, and a statement that "zero data retention is enabled" is only as good as who it applies to (module 83: the arrangement belongs to the party that runs the platform).
- **The course states design values, not legal ones.** A floor of 90 days, an erasure in 30 days and a ceiling of 365 days are numbers that fit an example. The numbers for a real system come from the requirement and from counsel.
- **A claim about a regime needs a source.** If a design review says "this is GDPR compliant", the question is which requirement, which part of the system meets it and what evidence shows that. The documentation points to a trust centre for compliance material, and that is where a certification claim is checked.

### Retention: a floor, a ceiling and a legal hold

The audit log serves two opposite duties. It must be kept long enough to prove what happened, and no longer than needed, because every extra month is exposure. The example writes this as a window: a **floor** (the shortest period kept, so that an incident found late can still be examined), a **ceiling** (the longest period kept without a reason) and the days actually kept, which lie between them, the ceiling itself allowed. A **legal hold** is the one thing that outranks the ceiling: when a matter is open the records in scope are kept past it, and they are released when the hold ends. What the log holds matters as much as how long: request id, action, consequence, outcome and the size of the text, and never the text itself (module 83). Content that was never stored cannot be breached, subpoenaed by accident or kept past a deletion request.

### Erasure by removing the map

A person asks to be forgotten. The data in the system is the audit record, which holds tokens, and the vault, which maps tokens to people. Deleting every audit entry would destroy the proof; keeping both leaves the person identifiable. The design removes the **map**: the vault entries for that person go, the audit entries stay, and the tokens in them can no longer be linked to anyone. The example shows it: a vault of three mappings holds two for one person; erasing that person removes two, the audit entries stay and one other token remains linkable. The deadline (30 days in the example, and exactly 30 passes) is part of the design and is tested like any threshold, with a case at 30 and a plant at 31.

### Fairness is a gap you measure

Bias, in the guide's wording, is a property of outcomes. The design cannot prevent unfairness by saying so; it measures it. The monitor layer produces a **parity report**: the same metric (the accuracy of module 88, the share of answers sent to a person, the share refused) computed per group, with the gap between the best and the worst group shown and a threshold that triggers a review. Two cautions follow from earlier modules. The groups must be defined where they can be measured (a segment you cannot label cannot be reported), and a headline average hides a failing group exactly as it hides a failing segment. The report does not fix anything on its own, which is why the register's residual column says "monitored and not prevented" and gives the report an owner.

### Transparency: tell people, and show the basis

Two duties travel together. Tell every person who receives output that AI helped produce it, and make that part of the deployment and not a setting someone may switch off; the Level 1 module on policy covers disclosure for decisions about people. And let a reader see the basis of an answer: the quote from the source that the grounding check used (page 1), the document version, and the way to reach a person. A trace that holds ids and sizes can show which document supported an answer without holding the answer.

### The register

The risk register ties it together, and its test is mechanical. Each row names a risk, its failure mode (hallucination, prompt injection, privacy leak, unfair outcome), a control that is defined in the design, a named owner and the residual risk in words. A row with no owner has nobody to call; a row with a control that does not exist is a wish; a register of two rows covers two of four modes. The register also carries the disclosure sentence, so a reviewer finds the transparency duty in the same place.

### The example

The example is the router from the first page, and its second half is this page: the **audit record** and the **erasure**. The record for the supported refund holds the request id, the action, the consequence `high`, the outcome `human`, the number of characters and `content_stored=False`. The erasure runs on a vault of three mappings, two of them for one person. It ran offline in every language.

<!-- example: m90-control-chain tabs: python,typescript,java,kotlin -->
```python
"""Governing a model call: a control that fails closed where the cost of an error is high, an independent check against the source that a confident answer must pass, and an audit record that holds no content.

The requests, answers and thresholds are invented; the confidence threshold of 95 is a value to tune to your own error costs. Nothing here calls a model.
"""
import logging

log = logging.getLogger(__name__)
from collections import namedtuple

Action = namedtuple("Action", "name consequence")
Answer = namedtuple("Answer", "text confidence quote")
AUTO_CONFIDENCE = 95


def route(action, answer, source, screen_up, confidence_min=AUTO_CONFIDENCE):
    """Decide what happens to an answer. A down screen holds a high-consequence action, an unsupported answer is held whatever its confidence, and only a confident, supported, low-consequence answer goes out unreviewed."""
    log.debug("route input: %r", action)
    if not screen_up and action.consequence == "high":
        return "hold: screen down"
    flag = "" if screen_up else " (unscreened)"
    if answer.quote not in source:
        return "hold: unsupported" + flag
    if action.consequence == "high":
        return "human" + flag
    return ("auto" if answer.confidence >= confidence_min else "review") + flag


def audit_record(request_id, action, outcome, text):
    """Proof of what happened without a copy of the data: who, what, how big and the outcome, and never the text."""
    return {"request": request_id, "action": action.name, "consequence": action.consequence, "outcome": outcome, "chars": len(text), "content_stored": False}


def erase(vault, subject):
    """Erasure removes the map from a token to a person, so the audit entries that carry only tokens can no longer be linked to anyone."""
    kept = {token: person for token, person in vault.items() if person != subject}
    return kept, len(vault) - len(kept)


def main():
    source = "Water damage is covered up to 5,000 per claim. Flood damage is excluded."
    reply = Action("draft_reply", "low")
    refund = Action("issue_refund", "high")
    good = Answer("Water damage is covered up to 5,000.", 99, "Water damage is covered up to 5,000 per claim.")
    edge = Answer("Water damage is covered up to 5,000.", 95, "Water damage is covered up to 5,000 per claim.")
    unsure = Answer("Water damage is covered up to 5,000.", 94, "Water damage is covered up to 5,000 per claim.")
    wrong = Answer("Water damage is covered up to 8,000.", 99, "Water damage is covered up to 8,000 per claim.")
    cases = [
        ("screen up, refund, supported", refund, good, True),
        ("screen up, reply, confidence 99", reply, good, True),
        ("screen up, reply, confidence 95", reply, edge, True),
        ("screen up, reply, confidence 94", reply, unsure, True),
        ("screen up, reply, confident but unsupported", reply, wrong, True),
        ("screen down, refund", refund, good, False),
        ("screen down, reply", reply, good, False),
    ]
    for label, action, answer, up in cases:
        print(f"{label}: {route(action, answer, source, up)}")
    record = audit_record("r-1001", refund, "human", good.text)
    print("audit record:", ", ".join(f"{k}={v}" for k, v in record.items()))
    vault = {"<EMAIL_1>": "person-a", "<EMAIL_2>": "person-b", "<MEMBER_1>": "person-a"}
    kept, removed = erase(vault, "person-a")
    print(f"erasure removed {removed} of {len(vault)} mappings; the audit entries stay, with {len(kept)} token still linkable")


if __name__ == "__main__":
    main()
```
```text
screen up, refund, supported: human
screen up, reply, confidence 99: auto
screen up, reply, confidence 95: auto
screen up, reply, confidence 94: review
screen up, reply, confident but unsupported: hold: unsupported
screen down, refund: hold: screen down
screen down, reply: auto (unscreened)
audit record: request=r-1001, action=issue_refund, consequence=high, outcome=human, chars=36, content_stored=False
erasure removed 2 of 3 mappings; the audit entries stay, with 1 token still linkable
```
```typescript
import { logger } from "./logger.ts";
const log = logger("control_chain");
/**
 * Governing a model call: a control that fails closed where the cost of an error is high, an independent check against the source that a confident answer must pass, and an audit record that holds no content.
 *
 * The requests, answers and thresholds are invented; the confidence threshold of 95 is a value to tune to your own error costs. Nothing here calls a model.
 */
export type Action = { name: string; consequence: string };
export type Answer = { text: string; confidence: number; quote: string };

export const AUTO_CONFIDENCE = 95;

/** Decide what happens to an answer. A down screen holds a high-consequence action, an unsupported answer is held whatever its confidence, and only a confident, supported, low-consequence answer goes out unreviewed. */
export function route(action: Action, answer: Answer, source: string, screenUp: boolean, confidenceMin = AUTO_CONFIDENCE): string {
  log.debug("route input", action);
  if (!screenUp && action.consequence === "high") return "hold: screen down";
  const flag = screenUp ? "" : " (unscreened)";
  if (!source.includes(answer.quote)) return "hold: unsupported" + flag;
  if (action.consequence === "high") return "human" + flag;
  return (answer.confidence >= confidenceMin ? "auto" : "review") + flag;
}

/** Proof of what happened without a copy of the data: who, what, how big and the outcome, and never the text. */
export function auditRecord(requestId: string, action: Action, outcome: string, text: string): Record<string, string | number | boolean> {
  return { request: requestId, action: action.name, consequence: action.consequence, outcome, chars: text.length, content_stored: false };
}

/** Erasure removes the map from a token to a person, so the audit entries that carry only tokens can no longer be linked to anyone. */
export function erase(vault: Record<string, string>, subject: string): [Record<string, string>, number] {
  const kept: Record<string, string> = {};
  for (const [token, person] of Object.entries(vault)) if (person !== subject) kept[token] = person;
  return [kept, Object.keys(vault).length - Object.keys(kept).length];
}

function main(): void {
  const source = "Water damage is covered up to 5,000 per claim. Flood damage is excluded.";
  const reply: Action = { name: "draft_reply", consequence: "low" };
  const refund: Action = { name: "issue_refund", consequence: "high" };
  const supported = "Water damage is covered up to 5,000 per claim.";
  const good: Answer = { text: "Water damage is covered up to 5,000.", confidence: 99, quote: supported };
  const edge: Answer = { text: "Water damage is covered up to 5,000.", confidence: 95, quote: supported };
  const unsure: Answer = { text: "Water damage is covered up to 5,000.", confidence: 94, quote: supported };
  const wrong: Answer = { text: "Water damage is covered up to 8,000.", confidence: 99, quote: "Water damage is covered up to 8,000 per claim." };
  const cases: [string, Action, Answer, boolean][] = [
    ["screen up, refund, supported", refund, good, true],
    ["screen up, reply, confidence 99", reply, good, true],
    ["screen up, reply, confidence 95", reply, edge, true],
    ["screen up, reply, confidence 94", reply, unsure, true],
    ["screen up, reply, confident but unsupported", reply, wrong, true],
    ["screen down, refund", refund, good, false],
    ["screen down, reply", reply, good, false],
  ];
  for (const [label, action, answer, up] of cases) console.log(`${label}: ${route(action, answer, source, up)}`);
  const record = auditRecord("r-1001", refund, "human", good.text);
  console.log("audit record:", Object.entries(record).map(([k, v]) => `${k}=${v === true ? "True" : v === false ? "False" : v}`).join(", "));
  const vault = { "<EMAIL_1>": "person-a", "<EMAIL_2>": "person-b", "<MEMBER_1>": "person-a" };
  const [kept, removed] = erase(vault, "person-a");
  console.log(`erasure removed ${removed} of ${Object.keys(vault).length} mappings; the audit entries stay, with ${Object.keys(kept).length} token still linkable`);
}

if (import.meta.main) main();
```
```text
screen up, refund, supported: human
screen up, reply, confidence 99: auto
screen up, reply, confidence 95: auto
screen up, reply, confidence 94: review
screen up, reply, confident but unsupported: hold: unsupported
screen down, refund: hold: screen down
screen down, reply: auto (unscreened)
audit record: request=r-1001, action=issue_refund, consequence=high, outcome=human, chars=36, content_stored=False
erasure removed 2 of 3 mappings; the audit entries stay, with 1 token still linkable
```
```java
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Governing a model call: a control that fails closed where the cost of an error is high, an independent check against the source that a confident answer must pass, and an audit record that holds no content.
 *
 * The requests, answers and thresholds are invented; the confidence threshold of 95 is a value to tune to your own error costs. Nothing here calls a model.
 */
public class ControlChain {
    private static final System.Logger LOG = System.getLogger(ControlChain.class.getName());
    record Action(String name, String consequence) {}

    record Answer(String text, int confidence, String quote) {}

    record Case(String label, Action action, Answer answer, boolean screenUp) {}

    record Erased(Map<String, String> kept, int removed) {}

    static final int AUTO_CONFIDENCE = 95;

    /** Decide what happens to an answer. A down screen holds a high-consequence action, an unsupported answer is held whatever its confidence, and only a confident, supported, low-consequence answer goes out unreviewed. */
    static String route(Action action, Answer answer, String source, boolean screenUp, int confidenceMin) {
        LOG.log(System.Logger.Level.DEBUG, "route input: {0}", action);
        if (!screenUp && action.consequence().equals("high")) return "hold: screen down";
        String flag = screenUp ? "" : " (unscreened)";
        if (!source.contains(answer.quote())) return "hold: unsupported" + flag;
        if (action.consequence().equals("high")) return "human" + flag;
        return (answer.confidence() >= confidenceMin ? "auto" : "review") + flag;
    }

    static String route(Action action, Answer answer, String source, boolean screenUp) {
        return route(action, answer, source, screenUp, AUTO_CONFIDENCE);
    }

    /** Proof of what happened without a copy of the data: who, what, how big and the outcome, and never the text. */
    static Map<String, Object> auditRecord(String requestId, Action action, String outcome, String text) {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("request", requestId);
        record.put("action", action.name());
        record.put("consequence", action.consequence());
        record.put("outcome", outcome);
        record.put("chars", text.length());
        record.put("content_stored", false);
        return record;
    }

    /** Erasure removes the map from a token to a person, so the audit entries that carry only tokens can no longer be linked to anyone. */
    static Erased erase(Map<String, String> vault, String subject) {
        Map<String, String> kept = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : vault.entrySet()) if (!e.getValue().equals(subject)) kept.put(e.getKey(), e.getValue());
        return new Erased(kept, vault.size() - kept.size());
    }

    static String flag(Object value) {
        return value instanceof Boolean b ? (b ? "True" : "False") : String.valueOf(value);
    }

    public static void main(String[] args) {
        String source = "Water damage is covered up to 5,000 per claim. Flood damage is excluded.";
        Action reply = new Action("draft_reply", "low");
        Action refund = new Action("issue_refund", "high");
        String supported = "Water damage is covered up to 5,000 per claim.";
        Answer good = new Answer("Water damage is covered up to 5,000.", 99, supported);
        Answer edge = new Answer("Water damage is covered up to 5,000.", 95, supported);
        Answer unsure = new Answer("Water damage is covered up to 5,000.", 94, supported);
        Answer wrong = new Answer("Water damage is covered up to 8,000.", 99, "Water damage is covered up to 8,000 per claim.");
        List<Case> cases = List.of(
            new Case("screen up, refund, supported", refund, good, true),
            new Case("screen up, reply, confidence 99", reply, good, true),
            new Case("screen up, reply, confidence 95", reply, edge, true),
            new Case("screen up, reply, confidence 94", reply, unsure, true),
            new Case("screen up, reply, confident but unsupported", reply, wrong, true),
            new Case("screen down, refund", refund, good, false),
            new Case("screen down, reply", reply, good, false));
        for (Case c : cases) System.out.println(c.label() + ": " + route(c.action(), c.answer(), source, c.screenUp()));
        Map<String, Object> record = auditRecord("r-1001", refund, "human", good.text());
        System.out.println("audit record: " + record.entrySet().stream().map(e -> e.getKey() + "=" + flag(e.getValue())).collect(Collectors.joining(", ")));
        Map<String, String> vault = new LinkedHashMap<>();
        vault.put("<EMAIL_1>", "person-a");
        vault.put("<EMAIL_2>", "person-b");
        vault.put("<MEMBER_1>", "person-a");
        Erased erased = erase(vault, "person-a");
        System.out.println("erasure removed " + erased.removed() + " of " + vault.size() + " mappings; the audit entries stay, with " + erased.kept().size() + " token still linkable");
    }
}
```
```text
screen up, refund, supported: human
screen up, reply, confidence 99: auto
screen up, reply, confidence 95: auto
screen up, reply, confidence 94: review
screen up, reply, confident but unsupported: hold: unsupported
screen down, refund: hold: screen down
screen down, reply: auto (unscreened)
audit record: request=r-1001, action=issue_refund, consequence=high, outcome=human, chars=36, content_stored=False
erasure removed 2 of 3 mappings; the audit entries stay, with 1 token still linkable
```
```kotlin
/**
 * Governing a model call: a control that fails closed where the cost of an error is high, an independent check against the source that a confident answer must pass, and an audit record that holds no content.
 *
 * The requests, answers and thresholds are invented; the confidence threshold of 95 is a value to tune to your own error costs. Nothing here calls a model.
 */

private val log = System.getLogger("control_chain")

data class Action(val name: String, val consequence: String)

data class Answer(val text: String, val confidence: Int, val quote: String)

data class Case(val label: String, val action: Action, val answer: Answer, val screenUp: Boolean)

data class Erased(val kept: Map<String, String>, val removed: Int)

const val AUTO_CONFIDENCE = 95

/** Decide what happens to an answer. A down screen holds a high-consequence action, an unsupported answer is held whatever its confidence, and only a confident, supported, low-consequence answer goes out unreviewed. */
fun route(action: Action, answer: Answer, source: String, screenUp: Boolean, confidenceMin: Int = AUTO_CONFIDENCE): String {
    log.log(System.Logger.Level.DEBUG, "route input: {0}", action)
    if (!screenUp && action.consequence == "high") return "hold: screen down"
    val flag = if (screenUp) "" else " (unscreened)"
    if (answer.quote !in source) return "hold: unsupported$flag"
    if (action.consequence == "high") return "human$flag"
    return (if (answer.confidence >= confidenceMin) "auto" else "review") + flag
}

/** Proof of what happened without a copy of the data: who, what, how big and the outcome, and never the text. */
fun auditRecord(requestId: String, action: Action, outcome: String, text: String): Map<String, Any> =
    linkedMapOf("request" to requestId, "action" to action.name, "consequence" to action.consequence, "outcome" to outcome, "chars" to text.length, "content_stored" to false)

/** Erasure removes the map from a token to a person, so the audit entries that carry only tokens can no longer be linked to anyone. */
fun erase(vault: Map<String, String>, subject: String): Erased {
    val kept = vault.filterValues { it != subject }
    return Erased(kept, vault.size - kept.size)
}

fun flag(value: Any): String = if (value is Boolean) (if (value) "True" else "False") else value.toString()

fun main() {
    val source = "Water damage is covered up to 5,000 per claim. Flood damage is excluded."
    val reply = Action("draft_reply", "low")
    val refund = Action("issue_refund", "high")
    val supported = "Water damage is covered up to 5,000 per claim."
    val good = Answer("Water damage is covered up to 5,000.", 99, supported)
    val edge = Answer("Water damage is covered up to 5,000.", 95, supported)
    val unsure = Answer("Water damage is covered up to 5,000.", 94, supported)
    val wrong = Answer("Water damage is covered up to 8,000.", 99, "Water damage is covered up to 8,000 per claim.")
    val cases = listOf(
        Case("screen up, refund, supported", refund, good, true),
        Case("screen up, reply, confidence 99", reply, good, true),
        Case("screen up, reply, confidence 95", reply, edge, true),
        Case("screen up, reply, confidence 94", reply, unsure, true),
        Case("screen up, reply, confident but unsupported", reply, wrong, true),
        Case("screen down, refund", refund, good, false),
        Case("screen down, reply", reply, good, false),
    )
    for (c in cases) println("${c.label}: ${route(c.action, c.answer, source, c.screenUp)}")
    val record = auditRecord("r-1001", refund, "human", good.text)
    println("audit record: " + record.entries.joinToString(", ") { "${it.key}=${flag(it.value)}" })
    val vault = linkedMapOf("<EMAIL_1>" to "person-a", "<EMAIL_2>" to "person-b", "<MEMBER_1>" to "person-a")
    val erased = erase(vault, "person-a")
    println("erasure removed ${erased.removed} of ${vault.size} mappings; the audit entries stay, with ${erased.kept.size} token still linkable")
}
```
```text
screen up, refund, supported: human
screen up, reply, confidence 99: auto
screen up, reply, confidence 95: auto
screen up, reply, confidence 94: review
screen up, reply, confident but unsupported: hold: unsupported
screen down, refund: hold: screen down
screen down, reply: auto (unscreened)
audit record: request=r-1001, action=issue_refund, consequence=high, outcome=human, chars=36, content_stored=False
erasure removed 2 of 3 mappings; the audit entries stay, with 1 token still linkable
```
<!-- /example -->

### The practice: the governance files

The practice is in [`exercises/90-governance-safety-and-risk`](../../exercises/90-governance-safety-and-risk/unit-01/practice-1/statement.md), the same one as the first page. The retention and erasure files and the risk register are the part of it that belongs here: a floor of at least 90 days, the days kept within the ceiling, no content stored, a legal hold that outranks the ceiling, erasure in at most 30 days, four register rows each with a defined control and a named owner, and the disclosure sentence.

## Traps

1. **"Keep every prompt and reply so that nothing can be disputed."** It is tempting because more records feel safer. The exam rejects it because a complete copy of the content is a second copy of the customers' data, to be protected, retained and deleted; keep proof of what happened, and keep content only where the requirement says so.
2. **"To honour a deletion request, delete the audit log."** It is tempting because the log is where the person's data is. The exam rejects it because the audit entries hold tokens and the proof is worth keeping; remove the vault map that links tokens to people, and the entries stay without a person attached.
3. **"The model is unbiased, so no fairness check is needed."** It is tempting because the model was tested in general and the average looks good. The exam rejects it because fairness is a property of outcomes by group, and an average hides a group that fails; measure the gap per segment and give the report an owner.

## Quiz

1. Scenario: Mira is asked to honour a deletion request from a customer whose requests appear in the audit entries of a claims assistant. Those entries hold tokens, and one table maps each token to a person. What does the design do?
   - **a**: Delete the entries of that customer from the log
   - **b**: Remove the vault rows that tie them to anything in the log
   - **c**: Move that customer's vault rows into a separate archive table
   - **d**: Mark that customer's entries as deleted and hide them from reports

2. Scenario: Tomas must set how long a support assistant keeps its audit records. Tomas's design has a shortest period, a longest period and a number of days between them. A court order then opens a case that covers a set of those records. What happens to those records?
   - **a**: They are trimmed to the floor, since every extra month is exposure
   - **b**: They follow the same schedule and the case uses what is left
   - **c**: They are moved to a table with no limit on their retention
   - **d**: They are held past the ceiling until the matter is settled

<details>
<summary>Answer key</summary>

1. **b**. Removing the vault rows leaves the entries and unlinks them from the person. *a* is ruled out because "the audit entries hold tokens and the proof is worth keeping", so the customer's entries stay and only their link to the customer goes. *c* is ruled out because "the vault entries for that person go", and an archive table still maps those tokens to the customer. *d* is ruled out because "keeping both leaves the person identifiable", and hidden entries still sit beside the vault map.
2. **d**. A legal hold is the one thing that outranks the ceiling, and the records are released when it ends. *a* is ruled out because the hold keeps records longer, as "the records in scope are kept past it". *c* is ruled out because "they are released when the hold ends", and an unlimited table keeps them after the case. *b* is ruled out because "A legal hold is the one thing that outranks the ceiling", so the ordinary schedule does not apply.

</details>

## Module quiz

This quiz covers both pages of the module.

1. Scenario: Ines reviews the risk register of a claims assistant. It has four rows, one for each of the four risks, each with an owner, and the prompt-injection row names a source filter that no part of the system implements. What should the review conclude about that row?
   - **a**: It passes, because the row names an owner who answers for it
   - **b**: It passes, because its residual column rates the risk as medium
   - **c**: It fails until that check is built as a control on a layer
   - **d**: It fails until the system prompt tells the model to filter input

2. Scenario: A review finds that a model system's average accuracy is 94 percent, and that one language group, a small part of the traffic, scores 70 percent. The team says the system passes. How should the reviewer answer that claim?
   - **a**: Accept it, since a language group is not a segment that is reported
   - **b**: Reject it, since the measure to judge is the gap between segments
   - **c**: Reject it until the overall accuracy rises well above 94 percent
   - **d**: Accept it, since the launch test covered the traffic broadly

3. Scenario: An auditor asks a team to show that a refund paid 200 days ago was put in front of a person. The team's audit entry for it holds the request id, the action, the consequence high, the outcome human and a character count. The team keeps entries for 365 days, above a floor of 90. What should the team tell the auditor?
   - **a**: That it falls short, since it leaves out the model's confidence score
   - **b**: That it has expired, since it is older than the 90-day floor
   - **c**: That it cannot be shared, since it would expose the customer
   - **d**: That it proves the step, since the routing result is logged

<details>
<summary>Answer key</summary>

1. **c**. Every control in the register must be a real control in the design, with a layer and a failure mode, so a filter that exists only on paper fails the row. *b* is ruled out because a residual comes on top of a control, as each failure mode "gets a control, an owner and a stated residual". *a* is ruled out because an owner does not create the control, and "a row with a control that does not exist is a wish". *d* is ruled out because "a line in a prompt is a request, and a control is a step the model cannot skip".
2. **b**. Fairness is the same metric computed per group, with the gap between the best and the worst group shown, and a group at 70 beside an average of 94 is a gap the average does not show. *a* is ruled out because "a segment you cannot label cannot be reported", and this group was labelled, since it was scored at 70 percent. *c* is ruled out because "a headline average hides a failing group exactly as it hides a failing segment", so a higher average can still hide the group at 70. *d* is ruled out because "fairness is a property of outcomes by group", and a broad launch test is not a gap measure.
3. **d**. A high-consequence action is routed to a person, and the outcome field records that route for this refund, with no text needed to show it. *b* is ruled out because the floor is "the shortest period kept, so that an incident found late can still be examined", and an entry of 200 days is still inside a window of 365. *c* is ruled out because the entry holds "request id, action, consequence, outcome and the size of the text", none of which names the customer. *a* is ruled out because "a high-consequence action goes to `human` whatever the confidence", so a score adds nothing to the proof of the route.

</details>
