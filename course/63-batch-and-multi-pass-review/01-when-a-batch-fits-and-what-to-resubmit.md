# When a batch fits, how to meet an SLA with it, and what to resubmit

**Level:** Architect · **Module 63:** Batch and multi-pass review · **Page 1 of 2**
**Exams:** A4.5; S5, S6

**After this page you can** decide per workload between the synchronous API and a batch, compute how often to submit batches so that every item meets a delivery deadline, tell what a batch request may and may not contain, pair results with requests by `custom_id`, resubmit only the items that failed (changing what made them fail), and test a prompt on a sample before spending the batch discount on a large volume.

Checked on 2026-10-04 against the Claude API documentation page "Batch processing" and the Claude Code documentation page on subagents, and against the exam guide's task statement 4.5. Nothing here called a model: the example is the arithmetic and the bookkeeping of a batch, written as plain code (`examples/63-batch-and-review`). This page deepens module 21 (Message Batches), which covers limits, price, polling and the result types, and does not repeat them; it adds the decisions an architect makes around them.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* the Message Batches API gives 50 percent cost savings with "up to 24-hour processing window, no guaranteed latency SLA"; it fits non-blocking, latency-tolerant work (overnight reports, weekly audits, nightly test generation) and does not fit blocking work such as a pre-merge check; it "does not support multi-turn tool calling within a single request (cannot execute tools mid-request and return results)"; `custom_id` correlates requests with responses. *What the product does now (documentation read 2026-10-04):* a batch entry may carry tools, system messages, multi-turn message lists and thinking, "each request in the batch is processed independently", most batches finish in under an hour, results are available when every request has finished or after 24 hours, whichever comes first, and a batch that is not done in 24 hours expires. So a request can include tools and the model can answer with a tool call, but nobody is there to run the tool and continue, which is the guide's point: a batch cannot run an agent loop. On the exam the answers are the guide's: a blocking workflow uses the synchronous API, a tool loop needs the synchronous API, and the batch is for bulk work that can wait.

## Why it matters

A team has two Claude jobs. One reviews every pull request and a developer is waiting for its verdict; the other audits every service for outdated patterns once a week. Someone notices that the batch API is half the price and proposes it for both. The pull request check then takes anywhere from a few minutes to a day, with no promise, and blocks merging in the meantime. The architect's job is to put each workload on the interface whose latency it can bear, and to know the arithmetic that makes a promise like "within 30 hours" true. Scenarios S5 (Claude Code in CI) and S6 (structured data extraction) both test this.

## The idea

### Blocking or latency-tolerant

The deciding question is who or what waits for the answer.

| Workload | Waits for the answer | Interface |
|---|---|---|
| A check that gates a merge or a deploy | A developer and a pipeline | Synchronous |
| An interactive assistant | A person | Synchronous |
| A step inside an agent loop that calls tools | The loop itself | Synchronous |
| An overnight report, a weekly audit, nightly test generation | Nobody, until the morning | Batch |
| Back-filling labels or extractions for an archive | Nobody | Batch |

A single product can use both: route per item and not per product. Standard documents go to the batch queue, and a document a customer is waiting for goes to the synchronous API. The half price is a reason to move work that can wait. It is not a reason to move work that cannot, and no discount makes a blocking check acceptable if it can take a day.

### What a batch request may and may not contain

A batch entry is `{custom_id, params}`, where `params` are the parameters of a Messages request. Tools, system prompts, earlier turns and extended thinking are allowed. Three parameters are refused with a validation error: `stream: true` (the results come back as one file, not a stream), `speed` (fast mode tunes synchronous latency) and a `max_tokens` of 0. The `custom_id` is 1 to 64 characters of letters, digits, hyphens and underscores, and it is the only link between a request and its result, because the results "can be returned in any order". The example's `batch_entry` and `match_results` enforce both.

The tool point deserves care, because the guide's sentence and the product differ in wording but not in consequence. A request that offers tools may come back with a tool call as its answer. In a synchronous loop your code would run the tool and send the result in the next request; in a batch there is no next request in the same job, since every entry is independent. A workflow that needs several rounds with tools therefore runs synchronously, or is cut so that each round is its own entry in successive batches, with the cost in latency that implies.

### Meeting an SLA with a window

A batch finishes within 24 hours (usually much sooner), but an item that arrives just after one batch was submitted waits for the next submission. The worst case for an item is therefore:

worst-case wait = submission interval + processing window + handling time

With a 24-hour window and 2 hours to read and deliver the results, a promise of 30 hours leaves 30 − 24 − 2 = 4 hours for the interval: submit a batch at least every 4 hours. The exam asks this as a calculation: "4-hour submission windows guarantee a 30-hour SLA with 24-hour batch processing". The three numbers that are easy to forget are the interval itself (a nightly batch gives a worst wait of 24 + 24 + handling, which is 50 hours), the handling time, and the fact that the window is a ceiling and not a promise of an hour. A promise that the window cannot support is refused at design time: an SLA of 26 hours leaves no positive interval.

### Failures: resubmit the failed items, and change what failed

A finished batch has a result per entry: succeeded, errored, canceled or expired. The sound reaction is by `custom_id`:

- Succeeded entries are kept. Resubmitting the whole batch pays twice for work that is done.
- An entry that expired, was canceled, or hit a server error is resubmitted unchanged in the next batch: nothing was wrong with it.
- An entry rejected as an invalid request is repaired first, since sending it again fails again. A common cause is a document that exceeds the context window: the repair is to cut it into chunks, each its own entry with its own `custom_id` (the original id plus a suffix), and to merge the chunk results afterwards, as module 62 showed for extraction.

The practice has this as `resubmission_plan`: the actions are `resubmit`, `fix` and `chunk`, and an entry over the size limit is chunked whatever its failure.

### Refine on a sample before the volume

A prompt that fails on one document in ten wastes a tenth of a large batch, and the failures are discovered a day later. Before submitting a large volume, run the prompt synchronously on a small, varied sample, read the failures, refine the prompt, and repeat until the first-pass success rate is high. The batch discount then applies to a prompt that has been proven. The guide's wording is that this "maximizes first-pass success rates and reduces iterative resubmission costs". The sample must include the awkward documents (the long ones, the ones with missing fields), since a sample of easy documents proves nothing.

### An independent instance is the product's building block

The second half of this module needs a way to run a review that does not share the generator's reasoning. In Claude Code a subagent starts with a fresh context window that does not include the conversation history, the skills already invoked or the files the main agent has read, and returns only a summary. That isolation is what makes a subagent an independent reviewer; the next page builds on it.

### The example

<!-- example: m63-batch-and-review tabs: python,typescript,java,kotlin -->
```python
"""What a batch asks of its caller, and what an independent review is given.

Read on 2026-10-04 in the Claude API documentation ("Batch processing"): a batch is processed asynchronously, results are available when every request has finished or after 24 hours, whichever comes first,
a request is identified by its `custom_id` (1 to 64 letters, digits, hyphens and underscores), results can come back in any order, and `stream`, `speed` and a `max_tokens` of 0 are refused. The exam guide's wording for
task 4.5 (a batch has no latency guarantee and cannot run a tool mid-request) and 4.6 (an independent instance reviews better than the generator) is what the functions below make visible. Nothing here calls a model.
"""
import re

CUSTOM_ID = re.compile(r"^[a-zA-Z0-9_-]{1,64}$")


def worst_case_wait(interval_hours, window_hours=24, handling_hours=2):
    """An item that arrives just after a submission waits one interval for the next batch, then the processing window, then the handling."""
    return interval_hours + window_hours + handling_hours


def batch_entry(custom_id, params):
    """One entry of a batch request, refused for the same reasons the API refuses it."""
    if not CUSTOM_ID.match(custom_id):
        raise ValueError(f"custom_id '{custom_id}' must be 1 to 64 letters, digits, hyphens or underscores")
    if params.get("stream") is True:
        raise ValueError("stream is not supported in a batch")
    if "speed" in params:
        raise ValueError("speed is not supported in a batch")
    if params.get("max_tokens") == 0:
        raise ValueError("a max_tokens of 0 is not supported in a batch")
    return {"custom_id": custom_id, "params": params}


def match_results(requests, results):
    """Results come back in any order: pair them with the requests by custom_id, and report a result nobody asked for."""
    by_id = dict(results)
    asked = set(requests)
    return [(r, by_id.get(r, "missing")) for r in requests], [i for i, _ in results if i not in asked]


def review_request(code, reasoning, independent):
    """What the reviewing instance receives: an independent one gets the code alone, a self-review also gets the reasoning that produced it."""
    parts = ["Review this code for defects.", f"<code>{code}</code>"]
    if not independent:
        parts.append(f"<your_earlier_reasoning>{reasoning}</your_earlier_reasoning>")
    return "\n".join(parts)


def main():
    for interval in (4, 6):
        print(f"worst-case wait with a {interval} hour interval: {worst_case_wait(interval)} hours (SLA 30 hours)")
    for custom_id in ("invoice-0042", "invoice 0042"):
        try:
            batch_entry(custom_id, {"max_tokens": 1024})
            print(f"custom_id {custom_id}: accepted")
        except ValueError as e:
            print(f"custom_id {custom_id}: refused, {e}")
    for params in ({"stream": True}, {"speed": "fast"}, {"max_tokens": 0}):
        try:
            batch_entry("a1", params)
        except ValueError as e:
            print(f"refused: {e}")
    matched, orphans = match_results(["a1", "a2", "a3"], [("a2", "expired"), ("z9", "succeeded"), ("a1", "succeeded")])
    print("matched: " + ", ".join(f"{i}={kind}" for i, kind in matched) + "; unrequested: " + ", ".join(orphans))
    code, reasoning = "total = price * qty", "qty is always positive, so no check"
    for independent in (False, True):
        request = review_request(code, reasoning, independent)
        print(f"independent={'yes' if independent else 'no'}: carries the reasoning: {'yes' if reasoning in request else 'no'}")


if __name__ == "__main__":
    main()
```
```text
worst-case wait with a 4 hour interval: 30 hours (SLA 30 hours)
worst-case wait with a 6 hour interval: 32 hours (SLA 30 hours)
custom_id invoice-0042: accepted
custom_id invoice 0042: refused, custom_id 'invoice 0042' must be 1 to 64 letters, digits, hyphens or underscores
refused: stream is not supported in a batch
refused: speed is not supported in a batch
refused: a max_tokens of 0 is not supported in a batch
matched: a1=succeeded, a2=expired, a3=missing; unrequested: z9
independent=no: carries the reasoning: yes
independent=yes: carries the reasoning: no
```
```typescript
/**
 * What a batch asks of its caller, and what an independent review is given.
 *
 * Read on 2026-10-04 in the Claude API documentation ("Batch processing"): a batch is processed asynchronously, results are available when every request has finished or after 24 hours, whichever comes first,
 * a request is identified by its `custom_id` (1 to 64 letters, digits, hyphens and underscores), results can come back in any order, and `stream`, `speed` and a `max_tokens` of 0 are refused. The exam guide's wording for
 * task 4.5 (a batch has no latency guarantee and cannot run a tool mid-request) and 4.6 (an independent instance reviews better than the generator) is what the functions below make visible. Nothing here calls a model.
 */
const CUSTOM_ID = /^[a-zA-Z0-9_-]{1,64}$/;

/** An item that arrives just after a submission waits one interval for the next batch, then the processing window, then the handling. */
export function worstCaseWait(intervalHours: number, windowHours = 24, handlingHours = 2): number {
  return intervalHours + windowHours + handlingHours;
}

/** One entry of a batch request, refused for the same reasons the API refuses it. */
export function batchEntry(customId: string, params: Record<string, unknown>): { custom_id: string; params: Record<string, unknown> } {
  if (!CUSTOM_ID.test(customId)) throw new Error(`custom_id '${customId}' must be 1 to 64 letters, digits, hyphens or underscores`);
  if (params.stream === true) throw new Error("stream is not supported in a batch");
  if ("speed" in params) throw new Error("speed is not supported in a batch");
  if (params.max_tokens === 0) throw new Error("a max_tokens of 0 is not supported in a batch");
  return { custom_id: customId, params };
}

/** Results come back in any order: pair them with the requests by custom_id, and report a result nobody asked for. */
export function matchResults(requests: string[], results: Array<[string, string]>): [Array<[string, string]>, string[]] {
  const byId = new Map(results);
  const asked = new Set(requests);
  return [requests.map((r): [string, string] => [r, byId.get(r) ?? "missing"]), results.map(([i]) => i).filter((i) => !asked.has(i))];
}

/** What the reviewing instance receives: an independent one gets the code alone, a self-review also gets the reasoning that produced it. */
export function reviewRequest(code: string, reasoning: string, independent: boolean): string {
  const parts = ["Review this code for defects.", `<code>${code}</code>`];
  if (!independent) parts.push(`<your_earlier_reasoning>${reasoning}</your_earlier_reasoning>`);
  return parts.join("\n");
}

function main() {
  for (const interval of [4, 6]) console.log(`worst-case wait with a ${interval} hour interval: ${worstCaseWait(interval)} hours (SLA 30 hours)`);
  for (const customId of ["invoice-0042", "invoice 0042"]) {
    try {
      batchEntry(customId, { max_tokens: 1024 });
      console.log(`custom_id ${customId}: accepted`);
    } catch (e) {
      console.log(`custom_id ${customId}: refused, ${(e as Error).message}`);
    }
  }
  for (const params of [{ stream: true }, { speed: "fast" }, { max_tokens: 0 }]) {
    try {
      batchEntry("a1", params);
    } catch (e) {
      console.log(`refused: ${(e as Error).message}`);
    }
  }
  const [matched, orphans] = matchResults(["a1", "a2", "a3"], [["a2", "expired"], ["z9", "succeeded"], ["a1", "succeeded"]]);
  console.log("matched: " + matched.map(([i, kind]) => `${i}=${kind}`).join(", ") + "; unrequested: " + orphans.join(", "));
  const code = "total = price * qty";
  const reasoning = "qty is always positive, so no check";
  for (const independent of [false, true]) {
    const request = reviewRequest(code, reasoning, independent);
    console.log(`independent=${independent ? "yes" : "no"}: carries the reasoning: ${request.includes(reasoning) ? "yes" : "no"}`);
  }
}

if (import.meta.main) main();
```
```text
worst-case wait with a 4 hour interval: 30 hours (SLA 30 hours)
worst-case wait with a 6 hour interval: 32 hours (SLA 30 hours)
custom_id invoice-0042: accepted
custom_id invoice 0042: refused, custom_id 'invoice 0042' must be 1 to 64 letters, digits, hyphens or underscores
refused: stream is not supported in a batch
refused: speed is not supported in a batch
refused: a max_tokens of 0 is not supported in a batch
matched: a1=succeeded, a2=expired, a3=missing; unrequested: z9
independent=no: carries the reasoning: yes
independent=yes: carries the reasoning: no
```
```java
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * What a batch asks of its caller, and what an independent review is given.
 *
 * <p>Read on 2026-10-04 in the Claude API documentation ("Batch processing"): a batch is processed asynchronously, results are available when every request has finished or after 24 hours, whichever comes first,
 * a request is identified by its `custom_id` (1 to 64 letters, digits, hyphens and underscores), results can come back in any order, and `stream`, `speed` and a `max_tokens` of 0 are refused. The exam guide's wording for
 * task 4.5 (a batch has no latency guarantee and cannot run a tool mid-request) and 4.6 (an independent instance reviews better than the generator) is what the methods below make visible. Nothing here calls a model.
 */
public final class BatchAndReview {
    private static final Pattern CUSTOM_ID = Pattern.compile("^[a-zA-Z0-9_-]{1,64}$");

    record Matched(String customId, String kind) {}

    record Entry(String customId, Map<String, Object> params) {}

    record Pairing(List<Matched> matched, List<String> unrequested) {}

    /** An item that arrives just after a submission waits one interval for the next batch, then the processing window, then the handling. */
    static int worstCaseWait(int intervalHours, int windowHours, int handlingHours) {
        return intervalHours + windowHours + handlingHours;
    }

    /** One entry of a batch request, refused for the same reasons the API refuses it. */
    static Entry batchEntry(String customId, Map<String, Object> params) {
        if (!CUSTOM_ID.matcher(customId).matches()) throw new IllegalArgumentException("custom_id '" + customId + "' must be 1 to 64 letters, digits, hyphens or underscores");
        if (Boolean.TRUE.equals(params.get("stream"))) throw new IllegalArgumentException("stream is not supported in a batch");
        if (params.containsKey("speed")) throw new IllegalArgumentException("speed is not supported in a batch");
        if (Integer.valueOf(0).equals(params.get("max_tokens"))) throw new IllegalArgumentException("a max_tokens of 0 is not supported in a batch");
        return new Entry(customId, params);
    }

    /** Results come back in any order: pair them with the requests by custom_id, and report a result nobody asked for. */
    static Pairing matchResults(List<String> requests, List<Matched> results) {
        Map<String, String> byId = new HashMap<>();
        for (Matched r : results) byId.put(r.customId(), r.kind());
        Set<String> asked = new HashSet<>(requests);
        List<Matched> matched = new ArrayList<>();
        for (String r : requests) matched.add(new Matched(r, byId.getOrDefault(r, "missing")));
        List<String> unrequested = new ArrayList<>();
        for (Matched r : results) if (!asked.contains(r.customId())) unrequested.add(r.customId());
        return new Pairing(matched, unrequested);
    }

    /** What the reviewing instance receives: an independent one gets the code alone, a self-review also gets the reasoning that produced it. */
    static String reviewRequest(String code, String reasoning, boolean independent) {
        List<String> parts = new ArrayList<>(List.of("Review this code for defects.", "<code>" + code + "</code>"));
        if (!independent) parts.add("<your_earlier_reasoning>" + reasoning + "</your_earlier_reasoning>");
        return String.join("\n", parts);
    }

    public static void main(String[] args) {
        for (int interval : new int[] {4, 6}) System.out.println("worst-case wait with a " + interval + " hour interval: " + worstCaseWait(interval, 24, 2) + " hours (SLA 30 hours)");
        for (String customId : new String[] {"invoice-0042", "invoice 0042"}) {
            try {
                batchEntry(customId, Map.of("max_tokens", 1024));
                System.out.println("custom_id " + customId + ": accepted");
            } catch (IllegalArgumentException e) {
                System.out.println("custom_id " + customId + ": refused, " + e.getMessage());
            }
        }
        for (Map<String, Object> params : List.<Map<String, Object>>of(Map.of("stream", true), Map.of("speed", "fast"), Map.of("max_tokens", 0))) {
            try {
                batchEntry("a1", params);
            } catch (IllegalArgumentException e) {
                System.out.println("refused: " + e.getMessage());
            }
        }
        Pairing p = matchResults(List.of("a1", "a2", "a3"), List.of(new Matched("a2", "expired"), new Matched("z9", "succeeded"), new Matched("a1", "succeeded")));
        List<String> pairs = new ArrayList<>();
        for (Matched m : p.matched()) pairs.add(m.customId() + "=" + m.kind());
        System.out.println("matched: " + String.join(", ", pairs) + "; unrequested: " + String.join(", ", p.unrequested()));
        String code = "total = price * qty";
        String reasoning = "qty is always positive, so no check";
        for (boolean independent : new boolean[] {false, true}) {
            String request = reviewRequest(code, reasoning, independent);
            System.out.println("independent=" + (independent ? "yes" : "no") + ": carries the reasoning: " + (request.contains(reasoning) ? "yes" : "no"));
        }
    }
}
```
```text
worst-case wait with a 4 hour interval: 30 hours (SLA 30 hours)
worst-case wait with a 6 hour interval: 32 hours (SLA 30 hours)
custom_id invoice-0042: accepted
custom_id invoice 0042: refused, custom_id 'invoice 0042' must be 1 to 64 letters, digits, hyphens or underscores
refused: stream is not supported in a batch
refused: speed is not supported in a batch
refused: a max_tokens of 0 is not supported in a batch
matched: a1=succeeded, a2=expired, a3=missing; unrequested: z9
independent=no: carries the reasoning: yes
independent=yes: carries the reasoning: no
```
```kotlin
/**
 * What a batch asks of its caller, and what an independent review is given.
 *
 * Read on 2026-10-04 in the Claude API documentation ("Batch processing"): a batch is processed asynchronously, results are available when every request has finished or after 24 hours, whichever comes first,
 * a request is identified by its `custom_id` (1 to 64 letters, digits, hyphens and underscores), results can come back in any order, and `stream`, `speed` and a `max_tokens` of 0 are refused. The exam guide's wording for
 * task 4.5 (a batch has no latency guarantee and cannot run a tool mid-request) and 4.6 (an independent instance reviews better than the generator) is what the functions below make visible. Nothing here calls a model.
 */
private val CUSTOM_ID = Regex("^[a-zA-Z0-9_-]{1,64}$")

data class Matched(val customId: String, val kind: String)

data class Entry(val customId: String, val params: Map<String, Any>)

data class Pairing(val matched: List<Matched>, val unrequested: List<String>)

/** An item that arrives just after a submission waits one interval for the next batch, then the processing window, then the handling. */
fun worstCaseWait(intervalHours: Int, windowHours: Int = 24, handlingHours: Int = 2): Int = intervalHours + windowHours + handlingHours

/** One entry of a batch request, refused for the same reasons the API refuses it. */
fun batchEntry(customId: String, params: Map<String, Any>): Entry {
    require(CUSTOM_ID.matches(customId)) { "custom_id '$customId' must be 1 to 64 letters, digits, hyphens or underscores" }
    require(params["stream"] != true) { "stream is not supported in a batch" }
    require("speed" !in params) { "speed is not supported in a batch" }
    require(params["max_tokens"] != 0) { "a max_tokens of 0 is not supported in a batch" }
    return Entry(customId, params)
}

/** Results come back in any order: pair them with the requests by custom_id, and report a result nobody asked for. */
fun matchResults(requests: List<String>, results: List<Matched>): Pairing {
    val byId = results.associate { it.customId to it.kind }
    val asked = requests.toSet()
    return Pairing(requests.map { Matched(it, byId[it] ?: "missing") }, results.map { it.customId }.filter { it !in asked })
}

/** What the reviewing instance receives: an independent one gets the code alone, a self-review also gets the reasoning that produced it. */
fun reviewRequest(code: String, reasoning: String, independent: Boolean): String {
    val parts = mutableListOf("Review this code for defects.", "<code>$code</code>")
    if (!independent) parts += "<your_earlier_reasoning>$reasoning</your_earlier_reasoning>"
    return parts.joinToString("\n")
}

fun main() {
    for (interval in listOf(4, 6)) println("worst-case wait with a $interval hour interval: ${worstCaseWait(interval)} hours (SLA 30 hours)")
    for (customId in listOf("invoice-0042", "invoice 0042")) {
        try {
            batchEntry(customId, mapOf("max_tokens" to 1024))
            println("custom_id $customId: accepted")
        } catch (e: IllegalArgumentException) {
            println("custom_id $customId: refused, ${e.message}")
        }
    }
    for (params in listOf(mapOf<String, Any>("stream" to true), mapOf("speed" to "fast"), mapOf("max_tokens" to 0))) {
        try {
            batchEntry("a1", params)
        } catch (e: IllegalArgumentException) {
            println("refused: ${e.message}")
        }
    }
    val p = matchResults(listOf("a1", "a2", "a3"), listOf(Matched("a2", "expired"), Matched("z9", "succeeded"), Matched("a1", "succeeded")))
    println("matched: " + p.matched.joinToString(", ") { "${it.customId}=${it.kind}" } + "; unrequested: " + p.unrequested.joinToString(", "))
    val code = "total = price * qty"
    val reasoning = "qty is always positive, so no check"
    for (independent in listOf(false, true)) {
        val request = reviewRequest(code, reasoning, independent)
        println("independent=${if (independent) "yes" else "no"}: carries the reasoning: ${if (reasoning in request) "yes" else "no"}")
    }
}
```
```text
worst-case wait with a 4 hour interval: 30 hours (SLA 30 hours)
worst-case wait with a 6 hour interval: 32 hours (SLA 30 hours)
custom_id invoice-0042: accepted
custom_id invoice 0042: refused, custom_id 'invoice 0042' must be 1 to 64 letters, digits, hyphens or underscores
refused: stream is not supported in a batch
refused: speed is not supported in a batch
refused: a max_tokens of 0 is not supported in a batch
matched: a1=succeeded, a2=expired, a3=missing; unrequested: z9
independent=no: carries the reasoning: yes
independent=yes: carries the reasoning: no
```
<!-- /example -->

## Traps

1. **"Put the pre-merge check on the batch API; it is half the price."** It is tempting because the discount is real. The exam rejects it: a blocking check cannot wait for a window that has no latency guarantee. The discount buys latency tolerance and is spent only where there is some.
2. **"Submit one batch a night; the SLA is 30 hours and batches take 24."** It is tempting because 24 is less than 30. The exam rejects it: an item that just missed tonight's batch waits a full day for the next, then 24 hours of processing, so the worst case is about 50 hours. The interval is part of the arithmetic.
3. **"Some results failed, so resubmit the whole batch."** It is tempting because it is one call. The exam rejects it: the succeeded entries are paid for again. Resubmit the failed `custom_id`s only, and change the cause (chunk the oversized document) when the cause was in the request.
4. **"Run the whole archive in one batch and tune the prompt afterwards."** It is tempting because the batch is cheap per item. The exam rejects it: a day passes before the failures show, and the whole run is repeated. Refine on a sample first.

## Quiz

1. Scenario S5, Claude Code in CI. A pipeline runs two Claude jobs: a verdict that a pull request needs before it may merge, and a Sunday scan of every service for outdated patterns. A manager wants both moved to the discounted bulk queue. What should the architect do?
   - **a**: Move both, since half the price outweighs a wait of unknown length
   - **b**: Keep the first on the immediate API and send the second through a batch
   - **c**: Put the first through a batch and keep the second on the immediate API
   - **d**: Move both, but submit a batch every hour so that the wait stays short

2. Scenario S6, structured data extraction. A contract-intake service promises that every uploaded document is extracted within 30 hours. Extraction runs as batches that finish within 24 hours, and the results take 2 hours to validate and deliver. What is the longest gap, in hours, between batch submissions that still keeps the promise?
   - **a**: 28
   - **b**: 24
   - **c**: 6
   - **d**: 4

<details>
<summary>Answer key</summary>

1. **b**. A merge verdict is a blocking check that cannot wait for a window without a latency guarantee, while the weekly scan can wait. *a* is ruled out because "It is not a reason to move work that cannot" wait, whatever the discount. *c* is ruled out because "a blocking check cannot wait for a window that has no latency guarantee", and this option sends exactly that check there. *d* is ruled out because "the window is a ceiling and not a promise of an hour", so more frequent submissions do not give the verdict a deadline.
2. **d**. The worst case is the interval plus the window plus the handling, so 30 minus 24 minus 2 leaves 4. *a* is ruled out because "worst-case wait = submission interval + processing window + handling time" includes the 24-hour window, which 28 ignores. *b* is ruled out because "a nightly batch gives a worst wait of 24 + 24 + handling, which is 50 hours". *c* is ruled out because the arithmetic starts from "2 hours to read and deliver the results", which 6 leaves out.

</details>

Adapted from the Claude Certified Architect - Foundations exam preparation guide by Daron Yondem (CC BY 4.0, https://creativecommons.org/licenses/by/4.0/), changed for this course.
