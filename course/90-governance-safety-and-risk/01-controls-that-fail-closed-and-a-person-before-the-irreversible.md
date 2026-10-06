# Controls that fail closed, and a person before the irreversible

**Level:** Architect Professional · **Module 90:** Governance, safety and risk · **Page 1 of 2**
**Exams:** P5, P4

**After this page you can** place a control at each layer of a design (the input, the output, the action and the monitor), decide for each control what happens when it fails, put a person in front of every action that cannot be undone, name the failure modes of a model system and match each to a control that really exists, and tell a control from a line in a prompt.

Checked on 2026-10-04 against the Claude documentation pages "Mitigate jailbreaks and prompt injections", "Handle streaming refusals" and "Reduce prompt leak", the Claude Certified Architect, Professional exam guide (version 1.0, domain 5), and by running the example offline in the course container. Nothing here called a model, and the requests, answers, thresholds and counts are invented. This page deepens module 41 (security and safety) and module 68 (human review and calibrated confidence) to a design that must stay safe when a part of it is down. What a design keeps, for how long, and how it is explained to users is the second page.

> **Exam guide and current product.** *What the guide states:* domain 5 asks the candidate to "Implement guardrails and safety controls", to "Identify risks, limitations, and failure modes of LLM systems" and to "Apply human-in-the-loop validation strategies". *What the current product's documentation says (pages read 2026-10-04):* the jailbreak page lists harmlessness screens with a lightweight model, input validation, prompt engineering, and a response to repeat offenders, and for third-party content it advises to put untrusted content only in tool results, to JSON-encode it, to limit access with least privilege and to screen tool outputs; the refusals page says a streamed request can end with a `refusal` stop reason, after which the context must be reset or the request retried on another model. The documentation describes the building blocks. It does not say what a design does when a screen is down or how a person is placed before an action; that is design judgement, taught here as the exam's strategy, and the confidence threshold and the cases are this course's values to tune to your own error costs.

## Why it matters

A claims assistant drafts replies, answers policy questions and can issue refunds. Its input screen is a small service, and one afternoon it times out. The code that calls it reads a timeout as "no problem found" and lets the request through, because a failed check had to be treated as something. For three hours every request skips the screen, and one of them is a document that tells the assistant to refund an account. Nobody decided to turn the control off. The design decided, in the line that says what happens when the check fails. The exam asks where a design puts its controls, what each one does when it breaks, and where a person stands.

## The idea

### Four layers, and what each guards

A control is a part of the system that stops, changes or records something, independent of the model's own judgement. Place them in four layers.

- **Input.** What reaches the model. A harmlessness screen with a small model and a structured verdict; validation for known injection patterns; the rule that untrusted content travels only in tool results, JSON-encoded, with its source named.
- **Output.** What leaves. A grounding check that the answer's quote exists in the source; a format check; the handling of a refusal.
- **Action.** What the system does in the world. The tools a role may call (least privilege, module 86), and the approval a risky call needs.
- **Monitor.** What is watched afterwards: drift, a parity report across groups, alerts (module 87).

The layers answer different attackers and different mistakes. An input control cannot catch a confident wrong answer, and an output control cannot stop a tool call that already happened. A design with all its controls on one layer has a gap on the others, and "the model is told in the system prompt" is a weak instance of one layer: it is a request to the model and not a control that the model cannot argue with.

### What a control does when it fails

Every control has a failure mode of its own, and the design must say it. The two choices are to **hold** (stop the request or the action until the control works) and to **proceed, flagged** (let it go on and record that the check did not run). The rule of the example is by consequence: a control holds whenever the action it guards is high-consequence, and every control on the action layer holds. A control that guards a low-consequence reply may proceed with a flag. The example's router applies it to the screen: with the screen down, a high-consequence action holds, and a low-consequence reply goes on, flagged.

The reason is the cost of a wrong guess. A screen that is down and lets a request through is the opening an attacker waits for. A drift alert that is down and lets the answers go on flagged costs a delay in noticing, and the answers themselves are still checked by other controls. The example shows both: with the screen down, a refund is held (`hold: screen down`), and a low-consequence reply goes on with a mark (`auto (unscreened)`) so that the gap shows in the record. "Fail open to keep the service up" is a business decision for the second kind of control and never for the first.

### A person before the irreversible

A refund paid, an account closed and a record deleted cannot be undone by a better answer next time. For a **high-consequence action** the design puts a person in front of it, and the person is a control: it exists as a step in the system, it holds when it cannot be reached, and nothing the model says skips it. The example's rule for the route is short: a high-consequence action goes to `human` whatever the confidence; a low-consequence action goes out unreviewed only at a confidence of at least 95, and below it goes to `review`.

Two things make this real. First, **confidence is not evidence.** An answer whose quote is not in the source is held (`hold: unsupported`) even at confidence 99, because a model can be certain and wrong; module 69 shows the quote check. Second, **the threshold is a decision about error cost.** At 95 the example lets an answer through at exactly 95 and reviews one at 94. It is a design value, set so that the cost of reviewing the rest is below the cost of the errors in the part sent out (module 79 gives the arithmetic), and it is checked against a measured sample (module 88), not copied.

### Failure modes, and a control that exists

The guide asks you to identify the failure modes of language-model systems. Four recur in every design review, and each gets a control, an owner and a stated residual.

| Failure mode | What it looks like | A control of the right layer |
|---|---|---|
| Hallucination | a fluent answer the source does not support | a grounding check on the output (hold when unsupported) |
| Prompt injection | instructions inside a document, an email or a tool result | untrusted content only in tool results, JSON-encoded; screen tool output; least privilege |
| Privacy leak | personal data in a prompt, a log or an answer | identifiers swapped for tokens before the call (module 83); no content in the audit log |
| Unfair outcome | different results for different groups | a parity report by segment on the monitor layer |

A register that names a control which is not defined anywhere is a wish. The check is mechanical: every control in the register is a control in the design, with a layer and a failure mode. Two limits belong in the register too, because the documentation is candid about them. A screen with a small model reduces risk and does not remove it, and the page on prompt leaks says that "no method is foolproof" and advises trying monitoring techniques first, since leak-proofing the prompt can degrade the task. So the residual column says what is left: low because an unsupported answer is held, medium because patterns do not find names.

### Refusals are a result to handle

A streamed request can end with `stop_reason: "refusal"`. The design treats it as an outcome with its own branch: the context is reset (the turn that caused it removed or rephrased) before the conversation continues, or the request is retried on a different model; the `stop_details` fields can be null, so the user-facing message comes from the application and not from an assumption that a category is present. Counting refusals per user also feeds the response to repeat offenders that the jailbreak page describes: tell the user the action violates the usage policy, and throttle or end the access.

### The example

The example is a router for the claims assistant, and its first half is this page. It takes an action with a consequence, an answer with a confidence and a quote, the source text and the state of the screen, and it returns what happens: `human`, `auto`, `review`, or a hold with its reason. It runs seven cases: a supported refund goes to a person; a reply goes out automatically at confidences 99 and 95 and to review at 94; a confident but unsupported reply is held; and with the screen down a refund is held and a reply goes on marked unscreened. The second page covers its audit record and the erasure. It ran offline in every language.

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

The practice is in [`exercises/90-governance-safety-and-risk`](../../exercises/90-governance-safety-and-risk/unit-01/practice-1/statement.md). The draft governance files of a claims assistant are wrong in several places: a screen that fails open, a refund with no person in front of it, an automatic threshold of 80, an audit log that keeps content, and a register with two rows and an invented control. You correct the files, and the tests read them. It is graded in Python, TypeScript, Java and Kotlin; the statement lists seven cases, each saying what you should see.

## Traps

1. **"If the screen is down, let the request through so customers are not blocked."** It is tempting because availability is measured and a blocked request is visible. The exam rejects it because a failed input or action control that proceeds on a high-consequence request is the gap an attacker waits for; hold, and say so in the response. Only a control that guards a low-consequence reply may proceed with a flag.
2. **"The system prompt says to ask a person before refunds, so a person is in the loop."** It is tempting because the instruction is written and the model usually follows it. The exam rejects it because a line in a prompt is a request, and a control is a step the model cannot skip; build the approval into the action layer as a step that holds.
3. **"A confidence of 99 means the answer can go out without a check."** It is tempting because the score is high and the threshold is 95. The exam rejects it because confidence is not evidence: an answer whose quote is not in the source is held at any confidence, and a high-consequence action goes to a person at any confidence.

## Quiz

1. Scenario: Dana runs the refund flow of a claims assistant. The service that screens incoming requests stops answering, and the code must decide what to do with a request that asks for a refund. Which behaviour fits the design rules on this page?
   - **a**: Hold it and tell the caller that the check is unavailable
   - **b**: Let the request pass and flag it for a person to look at tomorrow
   - **c**: Send it to the model alone, which is trained to refuse such requests
   - **d**: Let the request pass because the later approval step will catch it

2. Scenario: Ravi reviews a design in which a model must "always ask a person before closing an account", a rule written in the system prompt. What is the main weakness of that design?
   - **a**: The sentence is too short for the model to follow it reliably
   - **b**: The rule should sit in the user turn so that the model reads it last
   - **c**: Nothing forces the step to happen, because an instruction only asks
   - **d**: It sends every closure to a person rather than only the unsure ones

<details>
<summary>Answer key</summary>

1. **a**. A refund is a high-consequence action and the screen is an input control, so the design holds. *b* is ruled out because "A screen that is down and lets a request through is the opening an attacker waits for". *c* is ruled out because a control works "independent of the model's own judgement", and the model's training is not the missing screen. *d* is ruled out because "a control holds whenever the action it guards is high-consequence", so the screen holds whatever steps come after it.
2. **c**. A line in a prompt is a request and a control is a step the model cannot skip. *a* is ruled out because the page grants that "the model usually follows it", so reliability of following is not the weakness. *b* is ruled out because the sentence stays "a request to the model and not a control" wherever it sits. *d* is ruled out because "a high-consequence action goes to `human` whatever the confidence", so a person on every closure is the intended design.

</details>
