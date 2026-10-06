# Where a failure is handled, and what travels up

**Level:** Architect · **Module 66:** Errors across agents · **Page 1 of 2**
**Exams:** A5.3; S3

**After this page you can** decide which layer of a multi-agent system retries a failure and which reports it, say what a subagent can send up (only its final message) and what that message must therefore contain, tell an access failure from a valid empty result, write the structured error context a coordinator can act on, and name the three anti-patterns that hide or amplify failures.

Checked on 2026-10-04 against the exam guide's task statement 5.3, the Claude Agent SDK documentation page on subagents (what a parent receives, partial output at the turn limit) and the Claude Code documentation page on subagents. Nothing here called a model or a search tool: the example reports one set of five invented sources under four strategies (`examples/66-error-context`). Module 53 covered what a tool's error reply must carry and module 46 covered how a coordinator treats a failed subagent in its synthesis. This page covers the layer in between: where the failure is handled and what the subagent's report has to say. It does not repeat them.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* structured error context (failure type, attempted query, partial results, alternative approaches) lets the coordinator make recovery decisions; an access failure (a timeout that needs a retry decision) differs from a valid empty result (a successful query with no matches); a generic status such as "search unavailable" hides the context the coordinator needs; and both silently suppressing errors (returning an empty result as a success) and terminating the whole workflow on a single failure are anti-patterns. Subagents implement local recovery for transient failures and propagate only what they cannot resolve, with what was attempted and the partial results. *What the product does now (documentation read 2026-10-04):* "intermediate tool calls and results stay inside the subagent; only its final message returns to the parent"; a subagent that stops at its `maxTurns` limit has its output returned "marked as partial" (Claude Code v2.1.246 or later) and can be resumed; and "an API error that ends the subagent early, such as a rate limit, is never delivered as its result". So the structured context is not a protocol field: it is what the subagent writes in its final message, and the coordinator's prompt has to ask for it. On the exam, a question about a failure that the coordinator cannot act on has the answer: return the structured context.

## Why it matters

A research coordinator sends four subagents to four sources. One source times out, one refuses access, one has nothing on the topic, and one answers. The report that comes back says "findings: 3 items; the rest unavailable". The coordinator cannot tell the timeout (try again) from the refusal (do not), cannot tell "nothing there" from "could not look", and has lost the two items the timed-out subagent had already found. It then does one of three things, all bad: reruns everything, claims full coverage, or stops. Scenario S3 asks for the design in which none of them is needed.

## The idea

### Handle a failure at the lowest layer that can fix it

A transient failure, a timeout or a brief outage, is the subagent's to retry: it holds the query, knows what it has already found, and can try again within its own context at no cost to the coordinator's. Passing every timeout up has three costs: the coordinator's context fills with failure reports, it makes the retry decision without the subagent's detail, and it re-delegates the whole task and repeats work that was done. So the subagent retries a bounded number of times (two attempts in the practice), and only a failure that survives goes up. Retrying a failure that cannot change is the opposite mistake: a permission error or an invalid query fails again the same way, and the subagent reports it at once.

### The subagent's report is its final message

Everything the subagent did in between (the tool calls, the pages read, the errors met) stays inside it. The parent receives one message. If that message says "Analysis finished", the coordinator knows nothing about the two files that could not be opened. So the structure has to be asked for in the subagent's instructions: end with a report that states what was covered, what was not, and why. A failure that happens to the subagent itself (an API error that ends it early) is not delivered as a result at all, so the coordinator also needs a way to notice a subagent that returned nothing, and it records that scope as failed (module 46).

### What a failure must carry

A report of a failure that a coordinator can act on has four parts:

| Part | Why the coordinator needs it |
|---|---|
| Failure type (`timeout`, `permission`, `invalid_query`, `unavailable`) | It decides whether a retry is worthwhile and who can fix it |
| The query attempted | A retry or an alternative can be built from it; a person can reproduce it |
| Partial results | Work already done is kept and used |
| Alternatives | Another source, a narrower query, a cached copy, a person to ask |

"Search unavailable" has none of them. The example's generic report loses the one item the timed-out source had already found and cannot say which sources to retry and which to give up on.

### An empty result is an answer

A search that runs and finds nothing has succeeded. "No filings on this topic" is a finding, and a coordinator that is told so can write it into the report. The two ways to get this wrong are mirror images. Treat the empty result as a failure, and the system retries a search that already worked and may mark a covered topic as a gap. Treat a failure as an empty result, and the report states that nothing exists where in truth nobody looked. The second is the more harmful, because it is invisible: it is the silent suppression the guide names. The distinction is a field, not a sentence: `status: empty` and `status: failed` are different values with different handling.

### Three anti-patterns, one example

The example runs five invented sources through four reports:

- the generic status loses the partial result and the cause;
- silent suppression reports the timed-out and refused sources as searches that found nothing;
- aborting on the first failure throws away every source after the failing one;
- structured context keeps what was found, says what was empty, and names the way forward for each gap.

### The example

<!-- example: m66-error-context tabs: python,typescript,java,kotlin -->
```python
"""What a coordinator is told when one of five sources fails, under four ways of reporting it.

The exam guide (task 5.3) calls structured error context (failure type, the query attempted, partial results, alternatives) what lets a coordinator recover intelligently. It names two anti-patterns: a generic status
such as "search unavailable", which hides the context, and silent suppression, which reports an empty result as a success; terminating the whole workflow on one failure is the third. The five sources below and
their outcomes are invented for the illustration; nothing here calls a model or a search tool.
"""
import logging

log = logging.getLogger(__name__)

# source -> (kind, items); kind is ok, timeout or permission
OUTCOMES = {"news": ("ok", ["n1", "n2"]), "papers": ("timeout", ["p1"]), "patents": ("ok", []), "filings": ("permission", []), "blogs": ("ok", ["b1"])}
TRY = {"timeout": "retry later", "permission": "request access"}


def found(outcomes):
    return [item for kind, items in outcomes.values() if kind == "ok" for item in items]


def generic(outcomes):
    down = [source for source, (kind, _) in outcomes.items() if kind != "ok"]
    return f"found {', '.join(found(outcomes))}; sources unavailable: {', '.join(down)}"


def suppress(outcomes):
    nothing = [source for source, (kind, items) in outcomes.items() if kind != "ok" or not items]
    return f"found {', '.join(found(outcomes))}; nothing found in: {', '.join(nothing)}"


def terminate(outcomes):
    kept = []
    for source, (kind, items) in outcomes.items():
        if kind != "ok":
            return f"aborted at {source}; found {', '.join(kept)}"
        kept += items
    return f"found {', '.join(kept)}"


def structured(outcomes):
    good = [s for s, (kind, items) in outcomes.items() if kind == "ok" and items]
    partial = [f"{s} ({kind}, kept {', '.join(items)})" for s, (kind, items) in outcomes.items() if kind != "ok" and items]
    empty = [s for s, (kind, items) in outcomes.items() if kind == "ok" and not items]
    gaps = [f"{s} ({kind}, try: {TRY[kind]})" for s, (kind, items) in outcomes.items() if kind != "ok" and not items]
    parts = [("well supported", good), ("partial", partial), ("no findings", empty), ("gaps", gaps)]
    return "; ".join(f"{name}: {', '.join(items)}" for name, items in parts if items)


def main():
    for name, report in (("generic status", generic), ("silent empty", suppress), ("abort on failure", terminate), ("structured context", structured)):
        print(f"{name}: {report(OUTCOMES)}")


if __name__ == "__main__":
    main()
```
```text
generic status: found n1, n2, b1; sources unavailable: papers, filings
silent empty: found n1, n2, b1; nothing found in: papers, patents, filings
abort on failure: aborted at papers; found n1, n2
structured context: well supported: news, blogs; partial: papers (timeout, kept p1); no findings: patents; gaps: filings (permission, try: request access)
```
```typescript
import { logger } from "./logger.ts";
const log = logger("error_context");
/**
 * What a coordinator is told when one of five sources fails, under four ways of reporting it.
 *
 * The exam guide (task 5.3) calls structured error context (failure type, the query attempted, partial results, alternatives) what lets a coordinator recover intelligently. It names two anti-patterns: a generic status
 * such as "search unavailable", which hides the context, and silent suppression, which reports an empty result as a success; terminating the whole workflow on one failure is the third. The five sources below and
 * their outcomes are invented for the illustration; nothing here calls a model or a search tool.
 */
// source -> [kind, items]; kind is ok, timeout or permission
export type Outcomes = Record<string, [string, string[]]>;
export const OUTCOMES: Outcomes = { news: ["ok", ["n1", "n2"]], papers: ["timeout", ["p1"]], patents: ["ok", []], filings: ["permission", []], blogs: ["ok", ["b1"]] };
const TRY: Record<string, string> = { timeout: "retry later", permission: "request access" };

const found = (outcomes: Outcomes): string[] => Object.values(outcomes).filter(([kind]) => kind === "ok").flatMap(([, items]) => items);

export function generic(outcomes: Outcomes): string {
  const down = Object.entries(outcomes).filter(([, [kind]]) => kind !== "ok").map(([source]) => source);
  return `found ${found(outcomes).join(", ")}; sources unavailable: ${down.join(", ")}`;
}

export function suppress(outcomes: Outcomes): string {
  const nothing = Object.entries(outcomes).filter(([, [kind, items]]) => kind !== "ok" || items.length === 0).map(([source]) => source);
  return `found ${found(outcomes).join(", ")}; nothing found in: ${nothing.join(", ")}`;
}

export function terminate(outcomes: Outcomes): string {
  const kept: string[] = [];
  for (const [source, [kind, items]] of Object.entries(outcomes)) {
    if (kind !== "ok") return `aborted at ${source}; found ${kept.join(", ")}`;
    kept.push(...items);
  }
  return `found ${kept.join(", ")}`;
}

export function structured(outcomes: Outcomes): string {
  const entries = Object.entries(outcomes);
  const good = entries.filter(([, [kind, items]]) => kind === "ok" && items.length > 0).map(([s]) => s);
  const partial = entries.filter(([, [kind, items]]) => kind !== "ok" && items.length > 0).map(([s, [kind, items]]) => `${s} (${kind}, kept ${items.join(", ")})`);
  const empty = entries.filter(([, [kind, items]]) => kind === "ok" && items.length === 0).map(([s]) => s);
  const gaps = entries.filter(([, [kind, items]]) => kind !== "ok" && items.length === 0).map(([s, [kind]]) => `${s} (${kind}, try: ${TRY[kind]})`);
  const parts: Array<[string, string[]]> = [["well supported", good], ["partial", partial], ["no findings", empty], ["gaps", gaps]];
  return parts.filter(([, items]) => items.length > 0).map(([name, items]) => `${name}: ${items.join(", ")}`).join("; ");
}

function main() {
  for (const [name, report] of [["generic status", generic], ["silent empty", suppress], ["abort on failure", terminate], ["structured context", structured]] as const) console.log(`${name}: ${report(OUTCOMES)}`);
}

if (import.meta.main) main();
```
```text
generic status: found n1, n2, b1; sources unavailable: papers, filings
silent empty: found n1, n2, b1; nothing found in: papers, patents, filings
abort on failure: aborted at papers; found n1, n2
structured context: well supported: news, blogs; partial: papers (timeout, kept p1); no findings: patents; gaps: filings (permission, try: request access)
```
```java
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What a coordinator is told when one of five sources fails, under four ways of reporting it.
 *
 * <p>The exam guide (task 5.3) calls structured error context (failure type, the query attempted, partial results, alternatives) what lets a coordinator recover intelligently. It names two anti-patterns: a generic status
 * such as "search unavailable", which hides the context, and silent suppression, which reports an empty result as a success; terminating the whole workflow on one failure is the third. The five sources below and
 * their outcomes are invented for the illustration; nothing here calls a model or a search tool.
 */
public final class ErrorContext {
    private static final System.Logger LOG = System.getLogger(ErrorContext.class.getName());
    /** kind is ok, timeout or permission */
    record Outcome(String kind, List<String> items) {}

    static final Map<String, Outcome> OUTCOMES = new LinkedHashMap<>();
    static final Map<String, String> TRY = Map.of("timeout", "retry later", "permission", "request access");

    static {
        OUTCOMES.put("news", new Outcome("ok", List.of("n1", "n2")));
        OUTCOMES.put("papers", new Outcome("timeout", List.of("p1")));
        OUTCOMES.put("patents", new Outcome("ok", List.of()));
        OUTCOMES.put("filings", new Outcome("permission", List.of()));
        OUTCOMES.put("blogs", new Outcome("ok", List.of("b1")));
    }

    private static boolean ok(Outcome o) {
        return o.kind().equals("ok");
    }

    private static List<String> found(Map<String, Outcome> outcomes) {
        List<String> out = new ArrayList<>();
        for (Outcome o : outcomes.values()) if (ok(o)) out.addAll(o.items());
        return out;
    }

    static String generic(Map<String, Outcome> outcomes) {
        List<String> down = new ArrayList<>();
        for (Map.Entry<String, Outcome> e : outcomes.entrySet()) if (!ok(e.getValue())) down.add(e.getKey());
        return "found " + String.join(", ", found(outcomes)) + "; sources unavailable: " + String.join(", ", down);
    }

    static String suppress(Map<String, Outcome> outcomes) {
        List<String> nothing = new ArrayList<>();
        for (Map.Entry<String, Outcome> e : outcomes.entrySet()) if (!ok(e.getValue()) || e.getValue().items().isEmpty()) nothing.add(e.getKey());
        return "found " + String.join(", ", found(outcomes)) + "; nothing found in: " + String.join(", ", nothing);
    }

    static String terminate(Map<String, Outcome> outcomes) {
        List<String> kept = new ArrayList<>();
        for (Map.Entry<String, Outcome> e : outcomes.entrySet()) {
            if (!ok(e.getValue())) return "aborted at " + e.getKey() + "; found " + String.join(", ", kept);
            kept.addAll(e.getValue().items());
        }
        return "found " + String.join(", ", kept);
    }

    static String structured(Map<String, Outcome> outcomes) {
        List<String> good = new ArrayList<>();
        List<String> partial = new ArrayList<>();
        List<String> empty = new ArrayList<>();
        List<String> gaps = new ArrayList<>();
        for (Map.Entry<String, Outcome> e : outcomes.entrySet()) {
            Outcome o = e.getValue();
            if (ok(o) && !o.items().isEmpty()) good.add(e.getKey());
            else if (!ok(o) && !o.items().isEmpty()) partial.add(e.getKey() + " (" + o.kind() + ", kept " + String.join(", ", o.items()) + ")");
            else if (ok(o)) empty.add(e.getKey());
            else gaps.add(e.getKey() + " (" + o.kind() + ", try: " + TRY.get(o.kind()) + ")");
        }
        Map<String, List<String>> parts = new LinkedHashMap<>();
        parts.put("well supported", good);
        parts.put("partial", partial);
        parts.put("no findings", empty);
        parts.put("gaps", gaps);
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, List<String>> p : parts.entrySet()) if (!p.getValue().isEmpty()) lines.add(p.getKey() + ": " + String.join(", ", p.getValue()));
        return String.join("; ", lines);
    }

    public static void main(String[] args) {
        System.out.println("generic status: " + generic(OUTCOMES));
        System.out.println("silent empty: " + suppress(OUTCOMES));
        System.out.println("abort on failure: " + terminate(OUTCOMES));
        System.out.println("structured context: " + structured(OUTCOMES));
    }
}
```
```text
generic status: found n1, n2, b1; sources unavailable: papers, filings
silent empty: found n1, n2, b1; nothing found in: papers, patents, filings
abort on failure: aborted at papers; found n1, n2
structured context: well supported: news, blogs; partial: papers (timeout, kept p1); no findings: patents; gaps: filings (permission, try: request access)
```
```kotlin
private val log = System.getLogger("error_context")

/**
 * What a coordinator is told when one of five sources fails, under four ways of reporting it.
 *
 * The exam guide (task 5.3) calls structured error context (failure type, the query attempted, partial results, alternatives) what lets a coordinator recover intelligently. It names two anti-patterns: a generic status
 * such as "search unavailable", which hides the context, and silent suppression, which reports an empty result as a success; terminating the whole workflow on one failure is the third. The five sources below and
 * their outcomes are invented for the illustration; nothing here calls a model or a search tool.
 */
/** kind is ok, timeout or permission */
data class Outcome(val kind: String, val items: List<String>)

val OUTCOMES = linkedMapOf("news" to Outcome("ok", listOf("n1", "n2")), "papers" to Outcome("timeout", listOf("p1")), "patents" to Outcome("ok", emptyList()), "filings" to Outcome("permission", emptyList()), "blogs" to Outcome("ok", listOf("b1")))
private val TRY = mapOf("timeout" to "retry later", "permission" to "request access")

private fun found(outcomes: Map<String, Outcome>): List<String> = outcomes.values.filter { it.kind == "ok" }.flatMap { it.items }

fun generic(outcomes: Map<String, Outcome>): String = "found ${found(outcomes).joinToString(", ")}; sources unavailable: " + outcomes.filter { it.value.kind != "ok" }.keys.joinToString(", ")

fun suppress(outcomes: Map<String, Outcome>): String = "found ${found(outcomes).joinToString(", ")}; nothing found in: " + outcomes.filter { it.value.kind != "ok" || it.value.items.isEmpty() }.keys.joinToString(", ")

fun terminate(outcomes: Map<String, Outcome>): String {
    val kept = mutableListOf<String>()
    for ((source, o) in outcomes) {
        if (o.kind != "ok") return "aborted at $source; found ${kept.joinToString(", ")}"
        kept += o.items
    }
    return "found ${kept.joinToString(", ")}"
}

fun structured(outcomes: Map<String, Outcome>): String {
    val good = outcomes.filter { it.value.kind == "ok" && it.value.items.isNotEmpty() }.keys.toList()
    val partial = outcomes.filter { it.value.kind != "ok" && it.value.items.isNotEmpty() }.map { "${it.key} (${it.value.kind}, kept ${it.value.items.joinToString(", ")})" }
    val empty = outcomes.filter { it.value.kind == "ok" && it.value.items.isEmpty() }.keys.toList()
    val gaps = outcomes.filter { it.value.kind != "ok" && it.value.items.isEmpty() }.map { "${it.key} (${it.value.kind}, try: ${TRY[it.value.kind]})" }
    return listOf("well supported" to good, "partial" to partial, "no findings" to empty, "gaps" to gaps).filter { it.second.isNotEmpty() }.joinToString("; ") { "${it.first}: ${it.second.joinToString(", ")}" }
}

fun main() {
    println("generic status: ${generic(OUTCOMES)}")
    println("silent empty: ${suppress(OUTCOMES)}")
    println("abort on failure: ${terminate(OUTCOMES)}")
    println("structured context: ${structured(OUTCOMES)}")
}
```
```text
generic status: found n1, n2, b1; sources unavailable: papers, filings
silent empty: found n1, n2, b1; nothing found in: papers, patents, filings
abort on failure: aborted at papers; found n1, n2
structured context: well supported: news, blogs; partial: papers (timeout, kept p1); no findings: patents; gaps: filings (permission, try: request access)
```
<!-- /example -->

## Traps

1. **"Pass every failure up; the coordinator decides about retries."** It is tempting because the coordinator sees the whole picture. The exam rejects it: transient failures are handled locally, and only what the subagent cannot resolve goes up, with what was attempted and the partial results.
2. **"Return 'search unavailable' so the report stays short."** It is tempting because short reports save context. The exam rejects it: the coordinator cannot choose a recovery without the failure type, the query, the partial results and the alternatives.
3. **"If the search returns nothing, report the failure and retry."** It is tempting because empty feels like broken. The exam rejects it: a valid empty result is a successful query and is reported as a finding.
4. **"One source is refused: stop and tell the user the run failed."** It is tempting because it is simple and honest. The exam rejects it: terminating on a single failure throws away the work of the other subagents. Continue, and record the gap.

## Quiz

1. Scenario S3, multi-agent research system. A web-search subagent's service call times out now and then. Each time, the subagent reports a failure to the coordinator, which delegates the whole task again. Runs are slow, and the coordinator's context fills with repeated failure reports. What should change?
   - **a**: Let the coordinator make every retry itself, since only it sees the whole picture of the run
   - **b**: Let the worker retry the transient fault itself, passing up only what it cannot resolve
   - **c**: Raise the subagent's timeout far enough that a failure can no longer occur
   - **d**: Stop retrying anywhere, so that the user decides about each failure

2. Scenario S3, multi-agent research system. A document-analysis subagent opens forty files, fails to open two, and ends with the sentence "Analysis finished." The coordinator's report then claims complete coverage. Which change fits best?
   - **a**: Have the coordinator read the subagent's tool calls afterwards to find the files that failed
   - **b**: Raise the coordinator's turn limit so that it can inspect more of the run
   - **c**: Instruct the worker to close with a structured account of what it read and missed
   - **d**: Run the subagent twice and compare its two final sentences against each other

<details>
<summary>Answer key</summary>

1. **b**. The subagent holds the query and the work done, so it retries what is transient and reports only what survives. *a* is ruled out because passing failures up has the cost that "the coordinator's context fills with failure reports" and the decision is made "without the subagent's detail". *c* is ruled out because a timeout is the example of a "transient failure", which is retried "a bounded number of times" and not designed away. *d* is ruled out because it would hand the user each transient failure, when "a transient failure, a timeout or a brief outage, is the subagent's to retry".
2. **c**. Only the final message reaches the parent, so the structure has to be requested there. *a* is ruled out because "intermediate tool calls and results stay inside the subagent; only its final message returns to the parent". *b* is ruled out because the coordinator has nothing more to inspect: "Everything the subagent did in between" stays inside it. *d* is ruled out because two runs of the same instruction return the same kind of sentence, and "the structure has to be asked for in the subagent's instructions".

</details>

Adapted from the Claude Certified Architect - Foundations exam preparation guide by Daron Yondem (CC BY 4.0, https://creativecommons.org/licenses/by/4.0/), changed for this course.
