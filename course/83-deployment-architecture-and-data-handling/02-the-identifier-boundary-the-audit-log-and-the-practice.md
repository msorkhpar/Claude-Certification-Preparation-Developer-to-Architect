# The identifier boundary, the audit log and the practice

**Level:** Architect Professional · **Module 83:** Deployment architecture and data handling · **Page 2 of 2**
**Exams:** P1, P5

**After this page you can** keep identifiers out of the request by tokenising them before the call and restoring them after it, say why an instruction to the model is not a control, design an audit log that proves what happened without becoming a second store of the sensitive data, set a retention window with a floor and a ceiling and a legal hold, and read the practice's configuration check.

Checked on 2026-10-04 against the Claude API documentation page "API and data retention" (HIPAA readiness, PHI handling), the Anthropic Usage Policy, and the Claude Certified Architect, Professional exam guide v1.0 (July 2026), domains 1 and 5. The example ran in the course's container in Python, TypeScript, Java and Kotlin with the same output and calls no model. It is a teaching model of a boundary, not a compliance control: its patterns find e-mail addresses and member numbers, they do not find names, and the output shows that gap on purpose. The person in it is a placeholder.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* the Professional guide expects sensitive data to be protected by the architecture and the audit trail to be defensible. *What the current documentation says (checked 2026-10-04):* HIPAA readiness "applies a broader set of privacy and security safeguards than ZDR" and covers message content, attached files and their names; the documentation also says not to put protected health information in a JSON schema definition. *How to read both:* the exam keys the control that sits in the architecture, a layer that removes identifiers before the call and a log that keeps no content, and rejects the control that depends on the model: an instruction to ignore the data acts after the data has already crossed the boundary.

## Why it matters

A claims assistant reads claim notes that contain names, e-mail addresses and policy numbers. A compliance reviewer asks what stops that data from reaching the model provider. "The system prompt tells the model to disregard personal data" is the answer that fails: the instruction is read by the model that has already received the data, and nothing proves it was followed. The reviewer's second question is about the log: it must show what was sent and when, and it must not become a second copy of the very data the design was protecting. A good design answers both with structure.

## The idea

### Keep the identifier out of the request

The robust control is a layer that runs before the call. It finds identifiers and replaces each with a token that means nothing to the model (`<EMAIL_1>`), keeps the map from token to value in a vault that never leaves the caller, sends the tokenised text, and swaps the tokens back in the reply. Three properties make it work.

1. **The same value gets the same token.** An address that appears twice becomes the same token twice, so the model can still reason about "the same person wrote twice".
2. **The vault is local.** The map exists only inside the caller. The provider sees tokens and never the values.
3. **Restoration happens after the reply.** The user sees the real values because the caller put them back, and the model never held them.

The example prints this in order: the text sent has `<EMAIL_1>` twice and `<MEMBER_1>` once, the reply uses the tokens, and the restored text has the real address and member number. Notice what this does and does not do. The model can still do its job (confirm the contact before reopening a claim). It cannot leak what it never had. And the control is auditable: a reviewer can test the layer with sample text, which cannot be done for an instruction.

### The gap, shown on purpose

The last line of the example reads `gap: the name survives tokenising: True`. The patterns find shapes that have a fixed form: an address, a member number. A person's name has no fixed shape, so a pattern layer leaves it in. A real deployment needs a stronger detector for names, or a design in which the free text does not reach the model at all, and tests that plant identifiers of every kind and check that none reaches the request. A layer that has not been tested against the identifiers it claims to remove is a hope with code around it.

### The audit log: proof without a copy

An audit log answers "what happened" for a regulator, so it must record who, when, which request and what size. It must not store the prompt when the prompt would hold sensitive data, because then the log is itself a store of health or personal data, with all the obligations of one, and it is usually kept longer and guarded less. The example's entry holds the request identifier, the number of characters, how many identifiers were tokenised and the flag `prompt_stored=False`, and the output checks that no raw value appears in it. When an investigation needs the content, it is rebuilt from the system of record under access control, not read from the log.

### Retention has a floor and a ceiling

How long to keep the log is a decision with two limits. A **floor**: some rules require records for a minimum period (an audit window), so deleting sooner is a breach. A **ceiling**: other rules, and the principle of data minimisation, say data must not be kept longer than the purpose needs, so keeping indefinitely creates obligations and risk without a reason. A design states both, and the configuration check treats a retention equal to a limit as acceptable and one beyond it as a finding. Purging follows the same precision: an entry is removed only when it is **more than** the ceiling old, and an entry under a **legal hold** is never removed, whatever its age. A hold ends only when counsel releases it, and it keeps the entry itself and not a copy made at purge time. "Store everything forever, since storage is cheap" fails twice: it ignores the ceiling and it leaves the largest possible store of data to protect.

### The practice: a data policy for a deployment

The practice is in [`exercises/83-deployment-architecture-and-data-handling`](../../exercises/83-deployment-architecture-and-data-handling/unit-01/practice-1/statement.md). You write three functions: `check_deployment` compares a configuration (platform, model, region, retention arrangement, agreement, tenancy, identifier handling, audit settings) with a customer's requirements and returns the findings that apply; `retention_actions` decides which audit entries to purge by age and hold; `pick_deployment` routes a user to a deployment that keeps their data in their region, or to none. It is graded in Python, TypeScript, Java and Kotlin, and the statement lists nine cases, each saying what you should see when it works.

### The example

<!-- example: m83-deidentify tabs: python,typescript,java,kotlin -->
```python
"""Tokenise identifiers before a model call, restore them locally, and log an audit entry that holds no content.

The Claude documentation on API and data retention (read on 2026-10-04) says that HIPAA readiness "applies a broader set of privacy and
security safeguards" and that its protection covers message content, so the safest design keeps identifiers out of the message at all.
This file is a teaching model of that design, not a compliance control: patterns replace e-mail addresses and member numbers with tokens
that mean nothing to the model, the vault that maps tokens back stays in the caller, and the audit entry records sizes and counts, never
the prompt. The patterns do not find names, and the output shows that gap on purpose. No model is called.
"""
import logging
import re

log = logging.getLogger(__name__)

PATTERNS = [("EMAIL", re.compile(r"[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}")), ("MEMBER", re.compile(r"\bM-\d{6}\b"))]


def tokenise(text, vault):
    """Replaces every match by a token; the same value always gets the same token. `vault` maps value to token and stays local."""
    log.debug("tokenise input: %r", text)
    for label, pattern in PATTERNS:
        def swap(match):
            value = match.group(0)
            if value not in vault:
                vault[value] = f"<{label}_{sum(t.startswith(f'<{label}_') for t in vault.values()) + 1}>"
            return vault[value]
        text = pattern.sub(swap, text)
    return text


def restore(text, vault):
    for value, token in vault.items():
        text = text.replace(token, value)
    return text


def audit_entry(request_id, original, vault):
    """What the log keeps: the request id, the size of the prompt, how many distinct identifiers were tokenised. Never the prompt."""
    return {"request_id": request_id, "chars": len(original), "tokens_issued": len(vault), "prompt_stored": False}


def main():
    original = "Jane Doe (jane.doe@example.com, member M-204518) asks about claim 7781; jane.doe@example.com wrote twice."
    vault = {}
    sent = tokenise(original, vault)
    print("sent to the model:", sent)
    reply = "Please confirm <EMAIL_1> and <MEMBER_1> before we reopen the claim."
    print("model reply (illustrative):", reply)
    print("restored for the user:", restore(reply, vault))
    entry = audit_entry("req-001", original, vault)
    print("audit entry:", " ".join(f"{k}={v}" for k, v in entry.items()))
    print("raw values in the entry:", any(value in str(entry) for value in vault))
    print("gap: the name survives tokenising:", "Jane Doe" in sent)


if __name__ == "__main__":
    main()
```
```text
sent to the model: Jane Doe (<EMAIL_1>, member <MEMBER_1>) asks about claim 7781; <EMAIL_1> wrote twice.
model reply (illustrative): Please confirm <EMAIL_1> and <MEMBER_1> before we reopen the claim.
restored for the user: Please confirm jane.doe@example.com and M-204518 before we reopen the claim.
audit entry: request_id=req-001 chars=105 tokens_issued=2 prompt_stored=False
raw values in the entry: False
gap: the name survives tokenising: True
```
```typescript
// Tokenise identifiers before a model call, restore them locally, and log an audit entry that holds no content.
//
// The Claude documentation on API and data retention (read on 2026-10-04) says that HIPAA readiness "applies a broader set of privacy and
// security safeguards" and that its protection covers message content, so the safest design keeps identifiers out of the message at all.
// This file is a teaching model of that design, not a compliance control: patterns replace e-mail addresses and member numbers with tokens
// that mean nothing to the model, the vault that maps tokens back stays in the caller, and the audit entry records sizes and counts, never
// the prompt. The patterns do not find names, and the output shows that gap on purpose. No model is called.

import { logger } from "./logger.ts";
const log = logger("deidentify");

export const PATTERNS: Array<[string, RegExp]> = [["EMAIL", /[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}/g], ["MEMBER", /\bM-\d{6}\b/g]];

/** Replaces every match by a token; the same value always gets the same token. `vault` maps value to token and stays local. */
export function tokenise(text: string, vault: Map<string, string>): string {
  log.debug("tokenise input", text);
  for (const [label, pattern] of PATTERNS) {
    text = text.replace(pattern, (value) => {
      if (!vault.has(value)) vault.set(value, `<${label}_${[...vault.values()].filter((t) => t.startsWith(`<${label}_`)).length + 1}>`);
      return vault.get(value)!;
    });
  }
  return text;
}

export function restore(text: string, vault: Map<string, string>): string {
  for (const [value, token] of vault) text = text.replaceAll(token, value);
  return text;
}

/** What the log keeps: the request id, the size of the prompt, how many distinct identifiers were tokenised. Never the prompt. */
export function auditEntry(requestId: string, original: string, vault: Map<string, string>) {
  return { request_id: requestId, chars: original.length, tokens_issued: vault.size, prompt_stored: false };
}

function main() {
  const original = "Jane Doe (jane.doe@example.com, member M-204518) asks about claim 7781; jane.doe@example.com wrote twice.";
  const vault = new Map<string, string>();
  const sent = tokenise(original, vault);
  console.log("sent to the model:", sent);
  const reply = "Please confirm <EMAIL_1> and <MEMBER_1> before we reopen the claim.";
  console.log("model reply (illustrative):", reply);
  console.log("restored for the user:", restore(reply, vault));
  const entry = auditEntry("req-001", original, vault);
  console.log("audit entry:", `request_id=${entry.request_id} chars=${entry.chars} tokens_issued=${entry.tokens_issued} prompt_stored=${entry.prompt_stored ? "True" : "False"}`);
  console.log("raw values in the entry:", [...vault.keys()].some((value) => JSON.stringify(entry).includes(value)) ? "True" : "False");
  console.log("gap: the name survives tokenising:", sent.includes("Jane Doe") ? "True" : "False");
}

if (import.meta.main) main();
```
```text
sent to the model: Jane Doe (<EMAIL_1>, member <MEMBER_1>) asks about claim 7781; <EMAIL_1> wrote twice.
model reply (illustrative): Please confirm <EMAIL_1> and <MEMBER_1> before we reopen the claim.
restored for the user: Please confirm jane.doe@example.com and M-204518 before we reopen the claim.
audit entry: request_id=req-001 chars=105 tokens_issued=2 prompt_stored=False
raw values in the entry: False
gap: the name survives tokenising: True
```
```java
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tokenise identifiers before a model call, restore them locally, and log an audit entry that holds no content.
 *
 * <p>The Claude documentation on API and data retention (read on 2026-10-04) says that HIPAA readiness "applies a broader set of privacy and
 * security safeguards" and that its protection covers message content, so the safest design keeps identifiers out of the message at all.
 * This file is a teaching model of that design, not a compliance control: patterns replace e-mail addresses and member numbers with tokens
 * that mean nothing to the model, the vault that maps tokens back stays in the caller, and the audit entry records sizes and counts, never
 * the prompt. The patterns do not find names, and the output shows that gap on purpose. No model is called.
 */
public final class Deidentify {
    private static final System.Logger LOG = System.getLogger(Deidentify.class.getName());
    record Kind(String label, Pattern pattern) {}

    /** What the log keeps: the request id, the size of the prompt, how many distinct identifiers were tokenised. Never the prompt. */
    record Audit(String requestId, int chars, int tokensIssued, boolean promptStored) {}

    static final List<Kind> PATTERNS = List.of(
        new Kind("EMAIL", Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")),
        new Kind("MEMBER", Pattern.compile("\\bM-\\d{6}\\b")));

    /** Replaces every match by a token; the same value always gets the same token. {@code vault} maps value to token and stays local. */
    static String tokenise(String text, Map<String, String> vault) {
        LOG.log(System.Logger.Level.DEBUG, "tokenise input: {0}", text);
        String out = text;
        for (Kind kind : PATTERNS) {
            Matcher m = kind.pattern().matcher(out);
            StringBuilder next = new StringBuilder();
            while (m.find()) {
                String value = m.group();
                if (!vault.containsKey(value)) {
                    String prefix = "<" + kind.label() + "_";
                    long same = vault.values().stream().filter(t -> t.startsWith(prefix)).count();
                    vault.put(value, prefix + (same + 1) + ">");
                }
                m.appendReplacement(next, Matcher.quoteReplacement(vault.get(value)));
            }
            m.appendTail(next);
            out = next.toString();
        }
        return out;
    }

    static String restore(String text, Map<String, String> vault) {
        String out = text;
        for (Map.Entry<String, String> e : vault.entrySet()) out = out.replace(e.getValue(), e.getKey());
        return out;
    }

    static Audit auditEntry(String requestId, String original, Map<String, String> vault) {
        return new Audit(requestId, original.length(), vault.size(), false);
    }

    private static String py(boolean value) {
        return value ? "True" : "False";
    }

    public static void main(String[] args) {
        String original = "Jane Doe (jane.doe@example.com, member M-204518) asks about claim 7781; jane.doe@example.com wrote twice.";
        Map<String, String> vault = new LinkedHashMap<>();
        String sent = tokenise(original, vault);
        System.out.println("sent to the model: " + sent);
        String reply = "Please confirm <EMAIL_1> and <MEMBER_1> before we reopen the claim.";
        System.out.println("model reply (illustrative): " + reply);
        System.out.println("restored for the user: " + restore(reply, vault));
        Audit entry = auditEntry("req-001", original, vault);
        System.out.println("audit entry: request_id=" + entry.requestId() + " chars=" + entry.chars() + " tokens_issued=" + entry.tokensIssued() + " prompt_stored=" + py(entry.promptStored()));
        System.out.println("raw values in the entry: " + py(vault.keySet().stream().anyMatch(v -> entry.toString().contains(v))));
        System.out.println("gap: the name survives tokenising: " + py(sent.contains("Jane Doe")));
    }
}
```
```text
sent to the model: Jane Doe (<EMAIL_1>, member <MEMBER_1>) asks about claim 7781; <EMAIL_1> wrote twice.
model reply (illustrative): Please confirm <EMAIL_1> and <MEMBER_1> before we reopen the claim.
restored for the user: Please confirm jane.doe@example.com and M-204518 before we reopen the claim.
audit entry: request_id=req-001 chars=105 tokens_issued=2 prompt_stored=False
raw values in the entry: False
gap: the name survives tokenising: True
```
```kotlin
private val log = System.getLogger("deidentify")

/**
 * Tokenise identifiers before a model call, restore them locally, and log an audit entry that holds no content.
 *
 * The Claude documentation on API and data retention (read on 2026-10-04) says that HIPAA readiness "applies a broader set of privacy and
 * security safeguards" and that its protection covers message content, so the safest design keeps identifiers out of the message at all.
 * This file is a teaching model of that design, not a compliance control: patterns replace e-mail addresses and member numbers with tokens
 * that mean nothing to the model, the vault that maps tokens back stays in the caller, and the audit entry records sizes and counts, never
 * the prompt. The patterns do not find names, and the output shows that gap on purpose. No model is called.
 */
val PATTERNS = listOf("EMAIL" to Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}"), "MEMBER" to Regex("\\bM-\\d{6}\\b"))

/** What the log keeps: the request id, the size of the prompt, how many distinct identifiers were tokenised. Never the prompt. */
data class Audit(val requestId: String, val chars: Int, val tokensIssued: Int, val promptStored: Boolean)

/** Replaces every match by a token; the same value always gets the same token. `vault` maps value to token and stays local. */
fun tokenise(text: String, vault: MutableMap<String, String>): String {
    log.log(System.Logger.Level.DEBUG, "tokenise input: {0}", text)
    var out = text
    for ((label, pattern) in PATTERNS) {
        out = pattern.replace(out) { match ->
            val value = match.value
            if (value !in vault) vault[value] = "<${label}_${vault.values.count { it.startsWith("<${label}_") } + 1}>"
            vault.getValue(value)
        }
    }
    return out
}

fun restore(text: String, vault: Map<String, String>): String = vault.entries.fold(text) { acc, (value, token) -> acc.replace(token, value) }

fun auditEntry(requestId: String, original: String, vault: Map<String, String>) = Audit(requestId, original.length, vault.size, false)

private fun py(value: Boolean) = if (value) "True" else "False"

fun main() {
    val original = "Jane Doe (jane.doe@example.com, member M-204518) asks about claim 7781; jane.doe@example.com wrote twice."
    val vault = linkedMapOf<String, String>()
    val sent = tokenise(original, vault)
    println("sent to the model: $sent")
    val reply = "Please confirm <EMAIL_1> and <MEMBER_1> before we reopen the claim."
    println("model reply (illustrative): $reply")
    println("restored for the user: ${restore(reply, vault)}")
    val entry = auditEntry("req-001", original, vault)
    println("audit entry: request_id=${entry.requestId} chars=${entry.chars} tokens_issued=${entry.tokensIssued} prompt_stored=${py(entry.promptStored)}")
    println("raw values in the entry: ${py(vault.keys.any { entry.toString().contains(it) })}")
    println("gap: the name survives tokenising: ${py("Jane Doe" in sent)}")
}
```
```text
sent to the model: Jane Doe (<EMAIL_1>, member <MEMBER_1>) asks about claim 7781; <EMAIL_1> wrote twice.
model reply (illustrative): Please confirm <EMAIL_1> and <MEMBER_1> before we reopen the claim.
restored for the user: Please confirm jane.doe@example.com and M-204518 before we reopen the claim.
audit entry: request_id=req-001 chars=105 tokens_issued=2 prompt_stored=False
raw values in the entry: False
gap: the name survives tokenising: True
```
<!-- /example -->

## Traps

These are the wrong answers the exam's options for this domain offer, each with the reason it is rejected.

1. **"Add a line to the system prompt telling the model to disregard any health information."** It is tempting because it is one sentence. The exam rejects it: the data has already crossed the boundary, nothing proves the instruction was followed, and a compliance control belongs in the architecture where it can be tested.
2. **"Log the full prompt and response so every decision can be audited."** It is tempting because more detail looks like better evidence. The exam rejects it: the log becomes a second store of sensitive data with its own obligations, so it records identifiers, sizes and outcomes and rebuilds content from the system of record when needed.
3. **"Keep the audit logs indefinitely, since storage is cheap and deleting is risky."** It is tempting because deletion feels irreversible. The exam rejects it: retention needs a floor from the audit requirement and a ceiling from the purpose, and unlimited retention creates obligations that no one asked for.

## Quiz

1. Scenario: Fennel Health's drafting assistant receives claim notes with patient names, contact details and policy numbers. Which design best keeps these away from the model provider?
   - **a**: A system prompt that instructs the model to ignore any personal details it comes across
   - **b**: A review step that checks the model's reply for personal details after it has been returned
   - **c**: A larger model that is better at noticing which parts of a note are sensitive ones
   - **d**: A step in the caller that swaps each identifier for a token and puts the values back afterwards

2. Scenario: Rook Mutual's auditors ask for proof of each model call. The team proposes logging every full prompt and reply for ten years. What should the architect say?
   - **a**: Log each call's id, caller, time and size, and pull content from the source records only when needed
   - **b**: Accept the proposal, since a complete record is the strongest evidence that a regulator can ask to see
   - **c**: Log full content, but encrypt the log, which removes the obligations that a data store has
   - **d**: Log nothing from the calls, since any stored record of a call becomes a risk in itself

<details>
<summary>Answer key</summary>

1. **d**. A layer before the call means the provider sees tokens and never the values, and the layer can be tested. *a* is ruled out because "the data has already crossed the boundary". *c* is ruled out because a larger model still receives the data, and "a compliance control belongs in the architecture where it can be tested". *b* is ruled out because a check on the reply, like an instruction, "acts after the data has already crossed the boundary".
2. **a**. The log proves what happened without becoming a second copy of the data. *b* is ruled out because "the log becomes a second store of sensitive data with its own obligations". *c* is ruled out because encryption protects a store and does not remove it: the log would still be "a store of health or personal data, with all the obligations of one". *d* is ruled out because an audit log must record "who, when, which request and what size".

</details>

## Module quiz

This quiz covers both pages of the module.

1. Scenario: Marlow Care's service holds data of patients in the United States and in the European Union. It runs a deployment in each region. A European patient's request arrives while the European deployment is down for maintenance. What should the router do?
   - **a**: Turn it away for now with an error, and let the caller try again once the local site is back up
   - **b**: Send it to the United States deployment, since being available matters more than where the data sits
   - **c**: Send it to the United States deployment, after tokenising the identifiers in the request
   - **d**: Send it to whichever deployment answers the fastest, and record the exception in the audit log

2. Scenario: Alder Dental needs zero data retention on its Claude API deployment and has chosen a model that requires 30-day retention. What is the finding?
   - **a**: None, because the arrangement is organisation-wide and covers the models that are in use
   - **b**: A failed check, because that arrangement does not cover the one they have picked
   - **c**: A minor warning, since thirty days is short enough to be treated as nothing kept
   - **d**: None, provided the deployment's audit log is set to expire after thirty days

3. Scenario: Wren Logistics must keep audit entries for at least 90 days and at most 365. An entry is 400 days old, and counsel has placed it under a legal hold. What does the purge rule do?
   - **a**: Purges it, because the ceiling applies to every entry alike
   - **b**: Purges the entry and keeps a copy for a further ninety days
   - **c**: Retains it, because that order outranks the time limits
   - **d**: Retains it until the next audit closes, then purges it as usual

<details>
<summary>Answer key</summary>

1. **a**. Serving the user from another region breaks the residency requirement, so with no deployment that keeps the data in the region the answer is none. *b* is ruled out because "serving a European user from a United States deployment because it was available is the failure the router exists to prevent". *c* is ruled out because tokenising does not change where the request is processed, and "a user's data goes only to a deployment that keeps it in the user's region". *d* is ruled out because speed is not a criterion, and the router sends a user's data "only to a deployment that keeps it in that region".
2. **b**. A model that requires 30-day retention cannot meet a zero-retention requirement. *a* is ruled out because the arrangement "excludes models that need 30-day retention" so it does not cover every model. *c* is ruled out because the requirement is zero, not short, and "A model that requires 30-day retention cannot satisfy a zero-retention requirement". *d* is ruled out because the arrangement means "Anthropic does not store prompts or responses at rest", which the deployment's own audit log does not change, and "the model is part of the check and not only the platform".
3. **c**. A legal hold keeps an entry whatever its age. *a* is ruled out because an entry under a hold "is never removed, whatever its age". *b* is ruled out because the hold "keeps the entry itself and not a copy made at purge time", so nothing is removed first. *d* is ruled out because no audit date decides: "A hold ends only when counsel releases it".

</details>

Adapted from CLAUDE-CERTIFICATIONS by Amey Thakur (MIT License).
