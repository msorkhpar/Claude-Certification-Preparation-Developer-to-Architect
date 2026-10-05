# Sizing a workload: limits, tiers and the bill

**Level:** Architect Professional · **Module 84:** Cost and capacity engineering · **Page 1 of 2**
**Exams:** P2, P4

**After this page you can** turn a workload into the three rate limits it needs (requests, input tokens and output tokens per minute) with headroom, say which tokens count toward the input limit and why caching changes the answer, choose the smallest tier that covers the need, price the month in whole cents from the token mix, and tell a rate-limit error from an overload error and from a spend-cap error.

Checked on 2026-10-04 against the Claude API documentation pages "Rate limits" and "Pricing", the Prompt caching page, and the Claude Certified Architect, Professional exam guide v1.0 (July 2026), domain 2. The example ran in the course's container in Python, TypeScript, Java and Kotlin with the same output and calls no model. Its limits and prices are the documented figures for Claude Sonnet 5.5 on the checking date, kept in whole cents so that every language prints the same numbers. Limits, tiers and prices change and differ per model: re-read the pages before you plan, and treat every figure on this page as a worked input and not as a promise.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* domain 2 includes cost and capacity as design inputs, so the exam asks which limit a workload will hit first and which lever reduces cost without losing quality. *What the current documentation says (checked 2026-10-04):* limits are measured in requests per minute (RPM), input tokens per minute (ITPM) and output tokens per minute (OTPM) per model class, and "for most Claude models, only uncached input tokens count toward your ITPM rate limits", which means input tokens and cache writes count and cache reads do not. A reached limit returns a 429 error; an overloaded service returns a 529. The SDKs retry twice by default. *How to read both:* the exam keys the design that sizes by the limit that binds, caches the stable prefix to relieve it, and handles each error by its meaning. The multipliers and the tier thresholds are the documentation's, not the exam's.

## Why it matters

A team launches a support assistant, and on the first Monday it receives 429 errors for an hour. The model did not fail and the code did not change. The team sized the service on requests per minute, and the limit they hit was input tokens per minute, because each request carries a long policy. A short calculation done before launch would have found it: requests per minute times the tokens that count, with headroom, compared with the tier. The same calculation shows the fix: cache the policy, and most of those tokens stop counting. Capacity and cost are the same arithmetic seen from two sides.

## The idea

### Three limits, and the one that binds

Every workload has a rate, in requests per minute, and a token mix per request. From them, three numbers follow, each multiplied by a headroom for peaks and growth (the example uses 30 percent) and rounded up.

| Limit | What it is | How to compute it |
|---|---|---|
| **RPM** | Requests per minute | The peak rate, plus headroom |
| **ITPM** | Input tokens per minute that count | Rate times (uncached input plus cache-write tokens), plus headroom |
| **OTPM** | Output tokens per minute | Rate times output tokens, plus headroom |

Compare each number with the tier's limits; the workload fits a tier only if all three fit, and the limit that fails first is the **binding** one. The example's tiers for Claude Sonnet 5.5 are Start (1,000 RPM, 2 million ITPM, 400,000 OTPM), Build (5,000, 5 million, 1 million) and Scale (10,000, 10 million, 2 million), and a workload above Scale needs a custom arrangement.

### Caching moves the binding limit

Cache reads do not count toward ITPM. A prompt of 7,700 input tokens counts in full when it is sent plain. The same prompt with 6,000 tokens read from the cache, 200 written and 1,500 uncached counts 1,700, not 7,700. At 800 requests per minute, the example gives:

| | Need | Tier |
|---|---|---|
| With caching | 1,040 RPM, 1,768,000 ITPM, 416,000 OTPM | Build (Start is out on RPM, 1,000, and on OTPM, 400,000) |
| Without caching | 1,040 RPM, 8,008,000 ITPM, 416,000 OTPM | Scale (ITPM exceeds Build's 5 million) |

So caching moved the workload down a tier. With it, input tokens (1,768,000 against Start's 2 million) are no longer the problem, and request rate and output decide the tier; without it, input tokens bind and Build is out. An architect who did not count cache reads out would buy Scale for a workload that fits Build. The prefix has to be cacheable for this to hold (module 82), which makes the prompt's order a capacity decision as well as a cost one.

### The bill is the same mix at different prices

Price the month from the same token mix, by category. The documented Sonnet 5.5 prices on the checking date are input 2, output 10, five-minute cache write 2.50 and cache read 0.20 dollars per million tokens, and the Batch API is billed at half price. The example keeps money in whole cents. For 2 million requests a month:

| Design | Monthly bill |
|---|---|
| With caching | $17,400.00 |
| Without caching | $38,800.00 |
| With caching, 30 percent of requests through the Batch API | $14,790.00 |

Two levers show here. **Caching** more than halves the bill, because the long stable prefix is read at a tenth of the input price. **Batching** takes a further 15 percent off by moving the share of work that can wait. Both are available only if the design allows them: a prefix that changes defeats the first, and a caller who needs the answer in seconds cannot use the second. The check on a design's savings is arithmetic on the mix, not a claim, and the usage fields the API returns for cache reads, cache writes and output are the figures to price from.

### Reading the errors

| Status | What it means | What the design does |
|---|---|---|
| **429, rate limit** | A limit was reached; the response says how long to wait | Wait for the time given and retry, with backoff; cut the token rate by caching, queueing or spreading the load |
| **429, spend cap** | The organisation's spend limit was reached; there is no wait time to honour | Do not retry: raise the limit or stop, and alert a person |
| **529, overloaded** | The service is overloaded, not your limit | Retry with backoff; consider a fallback or a queue |

The SDKs retry twice by default, which absorbs a brief limit and does nothing against a sustained one. A retry loop that ignores the wait time or retries a spend-cap error spends effort on a request that will fail. The reliability patterns of module 81 (the same key on every retry, a breaker) apply here too.

### The example

<!-- example: m84-capacity-model tabs: python,typescript,java,kotlin -->
```python
"""A capacity and cost model for one workload: the limits it needs, the tier that gives them, and the monthly bill.

The Claude documentation on rate limits (read on 2026-10-04) says that "for most Claude models, only uncached input tokens count toward
your ITPM rate limits": `input_tokens` and `cache_creation_input_tokens` count, `cache_read_input_tokens` do not. The limits of the Start,
Build and Scale tiers below are the documented figures for Claude Sonnet 5.5, and the prices are the documented Sonnet 5.5 prices on the
Claude API (input 2, output 10, 5-minute cache write 2.50, cache read 0.20 dollars per million tokens, batch at half price). Prices and
limits change; re-read them before you plan. Money is kept in whole cents so that every language prints the same figures.
"""
import logging

log = logging.getLogger(__name__)

TIERS = [("Start", 1000, 2_000_000, 400_000), ("Build", 5000, 5_000_000, 1_000_000), ("Scale", 10_000, 10_000_000, 2_000_000)]  # name, RPM, ITPM, OTPM
CENTS_PER_MTOK = {"input": 200, "cache_write": 250, "cache_read": 20, "output": 1000}

CACHED = {"rpm": 800, "input": 1500, "cache_write": 200, "cache_read": 6000, "output": 400}  # tokens per request, requests per minute
UNCACHED = {"rpm": 800, "input": 7700, "cache_write": 0, "cache_read": 0, "output": 400}  # the same prompts with no caching
REQUESTS_PER_MONTH = 2_000_000


def required_capacity(workload, headroom_percent):
    """RPM, ITPM and OTPM to ask for. Cache reads do not count toward ITPM; every figure is rounded up after the headroom."""
    def up(x):
        return -(-x * (100 + headroom_percent) // 100)
    return {"rpm": up(workload["rpm"]), "itpm": up(workload["rpm"] * (workload["input"] + workload["cache_write"])), "otpm": up(workload["rpm"] * workload["output"])}


def smallest_tier(need, tiers):
    for name, rpm, itpm, otpm in tiers:
        if need["rpm"] <= rpm and need["itpm"] <= itpm and need["otpm"] <= otpm:
            return name
    return "Custom"


def monthly_cents(workload, requests, batch_percent):
    """Cents per month. The share of requests sent through the Batch API is billed at half price in every category."""
    log.debug("monthly_cents input: %r", workload)
    per_request = sum(workload[k] * CENTS_PER_MTOK[k] for k in CENTS_PER_MTOK)  # cents times tokens, per million
    return requests * per_request * (200 - batch_percent) // (200 * 1_000_000)


def dollars(cents):
    return f"${cents // 100:,}.{cents % 100:02d}"


def main():
    for label, workload in (("with caching", CACHED), ("without caching", UNCACHED)):
        need = required_capacity(workload, 30)
        print(f"{label}: need {need['rpm']} rpm, {need['itpm']} itpm, {need['otpm']} otpm -> tier {smallest_tier(need, TIERS)}")
    print("monthly bill with caching:", dollars(monthly_cents(CACHED, REQUESTS_PER_MONTH, 0)))
    print("monthly bill without caching:", dollars(monthly_cents(UNCACHED, REQUESTS_PER_MONTH, 0)))
    print("monthly bill with caching and 30 percent batch:", dollars(monthly_cents(CACHED, REQUESTS_PER_MONTH, 30)))


if __name__ == "__main__":
    main()
```
```text
with caching: need 1040 rpm, 1768000 itpm, 416000 otpm -> tier Build
without caching: need 1040 rpm, 8008000 itpm, 416000 otpm -> tier Scale
monthly bill with caching: $17,400.00
monthly bill without caching: $38,800.00
monthly bill with caching and 30 percent batch: $14,790.00
```
```typescript
// A capacity and cost model for one workload: the limits it needs, the tier that gives them, and the monthly bill.
//
// The Claude documentation on rate limits (read on 2026-10-04) says that "for most Claude models, only uncached input tokens count toward
// your ITPM rate limits": `input_tokens` and `cache_creation_input_tokens` count, `cache_read_input_tokens` do not. The limits of the Start,
// Build and Scale tiers below are the documented figures for Claude Sonnet 5.5, and the prices are the documented Sonnet 5.5 prices on the
// Claude API (input 2, output 10, 5-minute cache write 2.50, cache read 0.20 dollars per million tokens, batch at half price). Prices and
// limits change; re-read them before you plan. Money is kept in whole cents so that every language prints the same figures.

import { logger } from "./logger.ts";
const log = logger("capacity_model");

export type Workload = { rpm: number; input: number; cache_write: number; cache_read: number; output: number }; // tokens per request, requests per minute
export type Need = { rpm: number; itpm: number; otpm: number };

export const TIERS: Array<[string, number, number, number]> = [["Start", 1000, 2_000_000, 400_000], ["Build", 5000, 5_000_000, 1_000_000], ["Scale", 10_000, 10_000_000, 2_000_000]]; // name, RPM, ITPM, OTPM
export const CENTS_PER_MTOK = { input: 200, cache_write: 250, cache_read: 20, output: 1000 };

export const CACHED: Workload = { rpm: 800, input: 1500, cache_write: 200, cache_read: 6000, output: 400 };
export const UNCACHED: Workload = { rpm: 800, input: 7700, cache_write: 0, cache_read: 0, output: 400 }; // the same prompts with no caching
export const REQUESTS_PER_MONTH = 2_000_000;

/** RPM, ITPM and OTPM to ask for. Cache reads do not count toward ITPM; every figure is rounded up after the headroom. */
export function requiredCapacity(w: Workload, headroomPercent: number): Need {
  const up = (x: number) => Math.ceil((x * (100 + headroomPercent)) / 100);
  return { rpm: up(w.rpm), itpm: up(w.rpm * (w.input + w.cache_write)), otpm: up(w.rpm * w.output) };
}

export function smallestTier(need: Need, tiers: Array<[string, number, number, number]>): string {
  for (const [name, rpm, itpm, otpm] of tiers) if (need.rpm <= rpm && need.itpm <= itpm && need.otpm <= otpm) return name;
  return "Custom";
}

/** Cents per month. The share of requests sent through the Batch API is billed at half price in every category. */
export function monthlyCents(w: Workload, requests: number, batchPercent: number): number {
  log.debug("monthlyCents input", w);
  const perRequest = w.input * CENTS_PER_MTOK.input + w.cache_write * CENTS_PER_MTOK.cache_write + w.cache_read * CENTS_PER_MTOK.cache_read + w.output * CENTS_PER_MTOK.output;
  return Math.floor((requests * perRequest * (200 - batchPercent)) / (200 * 1_000_000));
}

export function dollars(cents: number): string {
  return `$${Math.floor(cents / 100).toLocaleString("en-US")}.${String(cents % 100).padStart(2, "0")}`;
}

function main() {
  for (const [label, workload] of [["with caching", CACHED], ["without caching", UNCACHED]] as Array<[string, Workload]>) {
    const need = requiredCapacity(workload, 30);
    console.log(`${label}: need ${need.rpm} rpm, ${need.itpm} itpm, ${need.otpm} otpm -> tier ${smallestTier(need, TIERS)}`);
  }
  console.log("monthly bill with caching:", dollars(monthlyCents(CACHED, REQUESTS_PER_MONTH, 0)));
  console.log("monthly bill without caching:", dollars(monthlyCents(UNCACHED, REQUESTS_PER_MONTH, 0)));
  console.log("monthly bill with caching and 30 percent batch:", dollars(monthlyCents(CACHED, REQUESTS_PER_MONTH, 30)));
}

if (import.meta.main) main();
```
```text
with caching: need 1040 rpm, 1768000 itpm, 416000 otpm -> tier Build
without caching: need 1040 rpm, 8008000 itpm, 416000 otpm -> tier Scale
monthly bill with caching: $17,400.00
monthly bill without caching: $38,800.00
monthly bill with caching and 30 percent batch: $14,790.00
```
```java
import java.util.List;
import java.util.Locale;

/**
 * A capacity and cost model for one workload: the limits it needs, the tier that gives them, and the monthly bill.
 *
 * <p>The Claude documentation on rate limits (read on 2026-10-04) says that "for most Claude models, only uncached input tokens count toward
 * your ITPM rate limits": {@code input_tokens} and {@code cache_creation_input_tokens} count, {@code cache_read_input_tokens} do not. The limits of the
 * Start, Build and Scale tiers below are the documented figures for Claude Sonnet 5.5, and the prices are the documented Sonnet 5.5 prices on the
 * Claude API (input 2, output 10, 5-minute cache write 2.50, cache read 0.20 dollars per million tokens, batch at half price). Prices and
 * limits change; re-read them before you plan. Money is kept in whole cents so that every language prints the same figures.
 */
public final class CapacityModel {
    private static final System.Logger LOG = System.getLogger(CapacityModel.class.getName());
    /** Tokens per request and requests per minute. */
    record Workload(long rpm, long input, long cacheWrite, long cacheRead, long output) {}

    record Need(long rpm, long itpm, long otpm) {}

    record Tier(String name, long rpm, long itpm, long otpm) {}

    static final List<Tier> TIERS = List.of(new Tier("Start", 1000, 2_000_000, 400_000), new Tier("Build", 5000, 5_000_000, 1_000_000), new Tier("Scale", 10_000, 10_000_000, 2_000_000));

    static final Workload CACHED = new Workload(800, 1500, 200, 6000, 400);
    static final Workload UNCACHED = new Workload(800, 7700, 0, 0, 400); // the same prompts with no caching
    static final long REQUESTS_PER_MONTH = 2_000_000;

    private static long up(long x, long headroomPercent) {
        return (x * (100 + headroomPercent) + 99) / 100;
    }

    /** RPM, ITPM and OTPM to ask for. Cache reads do not count toward ITPM; every figure is rounded up after the headroom. */
    static Need requiredCapacity(Workload w, long headroomPercent) {
        return new Need(up(w.rpm(), headroomPercent), up(w.rpm() * (w.input() + w.cacheWrite()), headroomPercent), up(w.rpm() * w.output(), headroomPercent));
    }

    static String smallestTier(Need need, List<Tier> tiers) {
        for (Tier t : tiers) if (need.rpm() <= t.rpm() && need.itpm() <= t.itpm() && need.otpm() <= t.otpm()) return t.name();
        return "Custom";
    }

    /** Cents per month. The share of requests sent through the Batch API is billed at half price in every category. */
    static long monthlyCents(Workload w, long requests, long batchPercent) {
        LOG.log(System.Logger.Level.DEBUG, "monthlyCents input: {0}", w);
        long perRequest = w.input() * 200 + w.cacheWrite() * 250 + w.cacheRead() * 20 + w.output() * 1000; // cents times tokens, per million
        return requests * perRequest * (200 - batchPercent) / (200 * 1_000_000L);
    }

    static String dollars(long cents) {
        return String.format(Locale.ROOT, "$%,d.%02d", cents / 100, cents % 100);
    }

    public static void main(String[] args) {
        for (Object[] row : new Object[][] {{"with caching", CACHED}, {"without caching", UNCACHED}}) {
            Need need = requiredCapacity((Workload) row[1], 30);
            System.out.println(row[0] + ": need " + need.rpm() + " rpm, " + need.itpm() + " itpm, " + need.otpm() + " otpm -> tier " + smallestTier(need, TIERS));
        }
        System.out.println("monthly bill with caching: " + dollars(monthlyCents(CACHED, REQUESTS_PER_MONTH, 0)));
        System.out.println("monthly bill without caching: " + dollars(monthlyCents(UNCACHED, REQUESTS_PER_MONTH, 0)));
        System.out.println("monthly bill with caching and 30 percent batch: " + dollars(monthlyCents(CACHED, REQUESTS_PER_MONTH, 30)));
    }
}
```
```text
with caching: need 1040 rpm, 1768000 itpm, 416000 otpm -> tier Build
without caching: need 1040 rpm, 8008000 itpm, 416000 otpm -> tier Scale
monthly bill with caching: $17,400.00
monthly bill without caching: $38,800.00
monthly bill with caching and 30 percent batch: $14,790.00
```
```kotlin
private val log = System.getLogger("capacity_model")

/**
 * A capacity and cost model for one workload: the limits it needs, the tier that gives them, and the monthly bill.
 *
 * The Claude documentation on rate limits (read on 2026-10-04) says that "for most Claude models, only uncached input tokens count toward
 * your ITPM rate limits": `input_tokens` and `cache_creation_input_tokens` count, `cache_read_input_tokens` do not. The limits of the Start,
 * Build and Scale tiers below are the documented figures for Claude Sonnet 5.5, and the prices are the documented Sonnet 5.5 prices on the
 * Claude API (input 2, output 10, 5-minute cache write 2.50, cache read 0.20 dollars per million tokens, batch at half price). Prices and
 * limits change; re-read them before you plan. Money is kept in whole cents so that every language prints the same figures.
 */
/** Tokens per request and requests per minute. */
data class Workload(val rpm: Long, val input: Long, val cacheWrite: Long, val cacheRead: Long, val output: Long)

data class Need(val rpm: Long, val itpm: Long, val otpm: Long)

data class Tier(val name: String, val rpm: Long, val itpm: Long, val otpm: Long)

val TIERS = listOf(Tier("Start", 1000, 2_000_000, 400_000), Tier("Build", 5000, 5_000_000, 1_000_000), Tier("Scale", 10_000, 10_000_000, 2_000_000))

val CACHED = Workload(800, 1500, 200, 6000, 400)
val UNCACHED = Workload(800, 7700, 0, 0, 400) // the same prompts with no caching
const val REQUESTS_PER_MONTH = 2_000_000L

private fun up(x: Long, headroomPercent: Long) = (x * (100 + headroomPercent) + 99) / 100

/** RPM, ITPM and OTPM to ask for. Cache reads do not count toward ITPM; every figure is rounded up after the headroom. */
fun requiredCapacity(w: Workload, headroomPercent: Long) =
    Need(up(w.rpm, headroomPercent), up(w.rpm * (w.input + w.cacheWrite), headroomPercent), up(w.rpm * w.output, headroomPercent))

fun smallestTier(need: Need, tiers: List<Tier>): String =
    tiers.firstOrNull { need.rpm <= it.rpm && need.itpm <= it.itpm && need.otpm <= it.otpm }?.name ?: "Custom"

/** Cents per month. The share of requests sent through the Batch API is billed at half price in every category. */
fun monthlyCents(w: Workload, requests: Long, batchPercent: Long): Long {
    log.log(System.Logger.Level.DEBUG, "monthlyCents input: {0}", w)
    val perRequest = w.input * 200 + w.cacheWrite * 250 + w.cacheRead * 20 + w.output * 1000 // cents times tokens, per million
    return requests * perRequest * (200 - batchPercent) / (200 * 1_000_000L)
}

fun dollars(cents: Long): String = "$" + "%,d".format(java.util.Locale.ROOT, cents / 100) + "." + "%02d".format(cents % 100)

fun main() {
    for ((label, workload) in listOf("with caching" to CACHED, "without caching" to UNCACHED)) {
        val need = requiredCapacity(workload, 30)
        println("$label: need ${need.rpm} rpm, ${need.itpm} itpm, ${need.otpm} otpm -> tier ${smallestTier(need, TIERS)}")
    }
    println("monthly bill with caching: " + dollars(monthlyCents(CACHED, REQUESTS_PER_MONTH, 0)))
    println("monthly bill without caching: " + dollars(monthlyCents(UNCACHED, REQUESTS_PER_MONTH, 0)))
    println("monthly bill with caching and 30 percent batch: " + dollars(monthlyCents(CACHED, REQUESTS_PER_MONTH, 30)))
}
```
```text
with caching: need 1040 rpm, 1768000 itpm, 416000 otpm -> tier Build
without caching: need 1040 rpm, 8008000 itpm, 416000 otpm -> tier Scale
monthly bill with caching: $17,400.00
monthly bill without caching: $38,800.00
monthly bill with caching and 30 percent batch: $14,790.00
```
<!-- /example -->

## Traps

These are the wrong answers the exam's options for this domain offer, each with the reason it is rejected.

1. **"Size the service on requests per minute; tokens are the model's business."** It is tempting because the request rate is the number everyone knows. The exam rejects it: the limit that binds is often tokens, and a long prompt multiplies the token rate, so the design computes all three limits and compares each with the tier.
2. **"Count every input token, including cache reads, toward the input limit."** It is tempting because every token is processed. The exam rejects it: for most models only uncached input and cache writes count, so caching a stable prefix relieves the limit as well as the bill.
3. **"On a 429, retry immediately and as often as needed."** It is tempting because a retry usually works. The exam rejects it: a rate-limit error says how long to wait, and a spend-cap error has no wait time to honour and must not be retried.

## Quiz

1. Scenario: Wicker Foods' service peaks at 600 requests a minute, each with 4,000 uncached input tokens and 300 output tokens, with 30 percent headroom. Using the tiers on the page, which is the smallest that fits?
   - **a**: Start, since 780 requests a minute is below its request ceiling
   - **b**: Scale, since the headroom pushes the totals past the limits of the tier below
   - **c**: A custom arrangement, since four thousand tokens a request is more than any tier allows
   - **d**: Build, since only the intake volume exceeds Start's allowance while the rest fit

2. Scenario: Fjord Media's support assistant costs $38,800.00 a month without caching and $17,400.00 with it, on 2 million requests. About what share of the uncached bill does caching remove?
   - **a**: About 15 percent
   - **b**: About 30 percent
   - **c**: About 55 percent
   - **d**: About 85 percent

<details>
<summary>Answer key</summary>

1. **d**. The three figures are 780 RPM, 3,120,000 ITPM and 234,000 OTPM, and the input figure is over Start's 2 million and under Build's 5 million. *a* is ruled out because "the workload fits a tier only if all three fit". *b* is ruled out because the headroom is already in the figures: "each multiplied by a headroom for peaks and growth". *c* is ruled out because "a workload above Scale needs a custom arrangement", and this one is far below it.
2. **c**. The difference is $21,400.00, which is about 55 percent of $38,800.00. *a* is ruled out because 15 percent is the batching figure: "takes a further 15 percent off by moving the share of work that can wait". *b* is ruled out because 30 percent is the share sent through the Batch API: "30 percent of requests through the Batch API". *d* is ruled out because caching removes a little over half, "because the long stable prefix is read at a tenth of the input price" and the output and the first writes are still billed.

</details>
