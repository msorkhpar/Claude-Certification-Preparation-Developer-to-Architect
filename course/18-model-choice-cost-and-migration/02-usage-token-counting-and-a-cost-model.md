# Reading usage, counting tokens before sending, and modelling cost

**Level:** Developer · **Module 18:** Model choice, cost and migration · **Page 2 of 3**
**Exams:** DV2

**After this page you can** read every field of a response's `usage` object, count the tokens of a request before sending it,
price a request from its usage with the cache and batch multipliers, and write a router that is graded on a cost table.

Checked against the Claude API documentation (Pricing, Token counting, Prompt caching, Batch processing) on 2026-10-02, and by
running the example and the practice offline in the course container (`anthropic` 1.11.0, `@anthropic-ai/sdk` 0.131.0). The
replies are illustrative, hand-written responses in the API's shapes, not captures.

## Why it matters

A bill is a sum over requests, and every request reports what it used. A team that never reads `usage` finds its cost on the
invoice; a team that reads it can attribute cost to a feature, a customer or a prompt change on the day it happens. The exam
asks you to compute a cost from a usage object, to say which multipliers apply, and to say what the token counting endpoint is
for.

## The idea

### The usage object

Every Messages response carries `usage`. The fields that cost money are these:

| Field | Meaning | Billed at |
|---|---|---|
| `input_tokens` | Input tokens that were not read from or written to a cache: with caching, only those after the last breakpoint | base input price |
| `cache_creation_input_tokens` | Tokens written to the cache; `cache_creation` splits them into `ephemeral_5m_input_tokens` and `ephemeral_1h_input_tokens` | 1.25 times input (5 minutes) or 2 times input (1 hour) |
| `cache_read_input_tokens` | Tokens read from the cache | 0.1 times input; 0.05 on Opus 5.5 and 0.025 on Fable 5.1 |
| `output_tokens` | Tokens generated, thinking included | output price |

The prompt caching page gives the identity that ties them together: the total input is the sum of the three input fields.
Streaming splits the report: the input side arrives in `message_start` and the final output count in `message_delta`.

### Counting tokens before you send

The token counting endpoint takes the same structured input as a Messages request (system prompt, tools, images, documents) and
returns `input_tokens`. Four facts decide how to use it:

- It is an estimate. The page says "The token count is an estimate", and the real count may differ by a small amount.
- It is free. The page says: "Token counting is free to use but subject to requests per minute rate limits based on your usage tier."
- It has its own limits. "Token counting and message creation have separate and independent rate limits."
- It rejects some inputs the Messages API accepts, such as server tools like web search.

Use it to decide before paying: route a request by its size, refuse an input over a budget, or fit a prompt to a target length.
It is not a substitute for reading `usage` afterwards, which is exact and carries the cache split.

### The cost model

Per request, with prices per million tokens:

```text
cost = input_tokens x P_in
     + cache_5m_tokens x P_in x 1.25 + cache_1h_tokens x P_in x 2
     + cache_read_tokens x P_in x R
     + output_tokens x P_out
```

where `R` is 0.1 on most models, 0.05 on Claude Opus 5.5 and 0.025 on Claude Fable 5.1. The Pricing page states how the
modifiers combine: "These multipliers stack with other pricing modifiers such as the Batch API discount and data
residency." The batch discount is 50 percent on input and output, so the whole sum halves; data residency with
`inference_geo: "us"` adds 1.1 times on Claude 4.6 and later models. Fast mode, with its higher rates, is not available in a batch.

Because a dollar per million tokens equals a micro-dollar per token, the arithmetic is simple: 120 input tokens on Sonnet 5.5 at
$2 cost 240 micro-dollars. The practice works in micro-dollars so that no cost is rounded away.

### The example

The example counts a request's tokens, sends it, and prices the reply from its usage. Its prices are the pricing page's
of 2026-10-02.

<!-- example: m18-cost-from-usage tabs: python,typescript,java,kotlin -->
```python
"""Count tokens before sending, then price the reply from its usage object.

Both calls go through a scripted transport, so nothing leaves the container. The replies are illustrative,
hand-written responses in the API's shapes (claude-sonnet-5-5), not captures. Prices are dollars per million
tokens, read from the Claude pricing page on 2026-10-02.
"""
import logging
from harness import scripted_client
from harness.scripted import message, text

log = logging.getLogger(__name__)

MODEL = "claude-sonnet-5-5"
PRICES = {  # input, output, cache-read multiplier
    "claude-haiku-4-5-20251001": (1.0, 5.0, 0.1),
    "claude-sonnet-5-5": (2.0, 10.0, 0.1),
    "claude-opus-5-5": (4.0, 20.0, 0.05),
    "claude-fable-5-1": (10.0, 50.0, 0.025),
}
PARAMS = dict(model=MODEL, max_tokens=300, system="You answer from the policy document.",
              messages=[{"role": "user", "content": "Summarise the refund policy in two sentences."}])
USAGE = {"input_tokens": 120, "output_tokens": 340, "cache_read_input_tokens": 0, "cache_creation_input_tokens": 4000,
         "cache_creation": {"ephemeral_5m_input_tokens": 4000, "ephemeral_1h_input_tokens": 0}}


def cost(model, usage, batch=False):
    """Dollars for one request: cache writes cost 1.25x input (5 minutes) or 2x (1 hour), reads a model-specific fraction."""
    price_in, price_out, read = PRICES[model]
    per_token = (usage.input_tokens * price_in
                 + usage.cache_creation.ephemeral_5m_input_tokens * price_in * 1.25
                 + usage.cache_creation.ephemeral_1h_input_tokens * price_in * 2.0
                 + usage.cache_read_input_tokens * price_in * read
                 + usage.output_tokens * price_out)
    return per_token / 1_000_000 * (0.5 if batch else 1.0)


def main():
    client, transport = scripted_client({"input_tokens": 4821}, message([text("Refunds take 14 days. Opened items are excluded.")], usage=USAGE))
    counted = client.messages.count_tokens(model=MODEL, system=PARAMS["system"], messages=PARAMS["messages"])
    print(f"count_tokens -> {counted.input_tokens} input tokens (POST {transport.urls[0].split('.com')[1]})")
    price_in = PRICES[MODEL][0]
    print(f"estimate before sending, input only: ${counted.input_tokens * price_in / 1_000_000:.6f} on {MODEL}")
    reply = client.messages.create(**PARAMS)
    u = reply.usage
    print(f"usage: input {u.input_tokens}, cache write {u.cache_creation_input_tokens}, cache read {u.cache_read_input_tokens}, output {u.output_tokens}")
    print(f"cost of this request: ${cost(MODEL, u):.6f}  (batch: ${cost(MODEL, u, batch=True):.6f})")
    print("the same usage on each model:")
    for model in PRICES:
        print(f"  {model:28} ${cost(model, u):.6f}")


if __name__ == "__main__":
    main()
```
```text
count_tokens -> 4821 input tokens (POST /v1/messages/count_tokens)
estimate before sending, input only: $0.009642 on claude-sonnet-5-5
usage: input 120, cache write 4000, cache read 0, output 340
cost of this request: $0.013640  (batch: $0.006820)
the same usage on each model:
  claude-haiku-4-5-20251001    $0.006820
  claude-sonnet-5-5            $0.013640
  claude-opus-5-5              $0.027280
  claude-fable-5-1             $0.068200
```
```typescript
// Count tokens before sending, then price the reply from its usage object.
// Both calls go through a scripted fetch, so nothing leaves the container. The replies are illustrative,
// hand-written responses in the API's shapes (claude-sonnet-5-5), not captures. Prices are dollars per million
// tokens, read from the Claude pricing page on 2026-10-02.
import Anthropic from "@anthropic-ai/sdk";
import { message, scriptedFetch, text } from "../../../harness/ts/scriptedFetch.ts";
import { logger } from "./logger.ts";
const log = logger("cost");

export const MODEL = "claude-sonnet-5-5";
export const PRICES: Record<string, [number, number, number]> = {
  // input, output, cache-read multiplier
  "claude-haiku-4-5-20251001": [1.0, 5.0, 0.1],
  "claude-sonnet-5-5": [2.0, 10.0, 0.1],
  "claude-opus-5-5": [4.0, 20.0, 0.05],
  "claude-fable-5-1": [10.0, 50.0, 0.025],
};
export const PARAMS = {
  model: MODEL,
  max_tokens: 300,
  system: "You answer from the policy document.",
  messages: [{ role: "user" as const, content: "Summarise the refund policy in two sentences." }],
};
export const USAGE = {
  input_tokens: 120, output_tokens: 340, cache_read_input_tokens: 0, cache_creation_input_tokens: 4000,
  cache_creation: { ephemeral_5m_input_tokens: 4000, ephemeral_1h_input_tokens: 0 },
};

/** Dollars for one request: cache writes cost 1.25x input (5 minutes) or 2x (1 hour), reads a model-specific fraction. */
export function cost(model: string, usage: any, batch = false): number {
  const [priceIn, priceOut, read] = PRICES[model];
  const micro =
    usage.input_tokens * priceIn +
    usage.cache_creation.ephemeral_5m_input_tokens * priceIn * 1.25 +
    usage.cache_creation.ephemeral_1h_input_tokens * priceIn * 2.0 +
    usage.cache_read_input_tokens * priceIn * read +
    usage.output_tokens * priceOut;
  return (micro / 1_000_000) * (batch ? 0.5 : 1.0);
}

export function clientFor(...bodies: unknown[]) {
  const fake = scriptedFetch(bodies.map((body) => ({ body })));
  return { fake, client: new Anthropic({ apiKey: "placeholder", maxRetries: 0, fetch: fake.fetch }) };
}

async function main() {
  const { fake, client } = clientFor({ input_tokens: 4821 }, message([text("Refunds take 14 days. Opened items are excluded.")], "end_turn", USAGE as any));
  const counted = await client.messages.countTokens({ model: MODEL, system: PARAMS.system, messages: PARAMS.messages });
  console.log(`count_tokens -> ${counted.input_tokens} input tokens (POST ${new URL(fake.seen[0].url).pathname})`);
  console.log(`estimate before sending, input only: $${((counted.input_tokens * PRICES[MODEL][0]) / 1_000_000).toFixed(6)} on ${MODEL}`);
  const reply = await client.messages.create(PARAMS);
  const u: any = reply.usage;
  console.log(`usage: input ${u.input_tokens}, cache write ${u.cache_creation_input_tokens}, cache read ${u.cache_read_input_tokens}, output ${u.output_tokens}`);
  console.log(`cost of this request: $${cost(MODEL, u).toFixed(6)}  (batch: $${cost(MODEL, u, true).toFixed(6)})`);
  console.log("the same usage on each model:");
  for (const model of Object.keys(PRICES)) console.log(`  ${model.padEnd(28)} $${cost(model, u).toFixed(6)}`);
}

if (import.meta.main) await main();
```
```text
count_tokens -> 4821 input tokens (POST /v1/messages/count_tokens)
estimate before sending, input only: $0.009642 on claude-sonnet-5-5
usage: input 120, cache write 4000, cache read 0, output 340
cost of this request: $0.013640  (batch: $0.006820)
the same usage on each model:
  claude-haiku-4-5-20251001    $0.006820
  claude-sonnet-5-5            $0.013640
  claude-opus-5-5              $0.027280
  claude-fable-5-1             $0.068200
```
```java
import static harness.Scripted.map;
import static harness.Scripted.message;
import static harness.Scripted.text;

import com.anthropic.models.messages.MessageCountTokensParams;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.anthropic.models.messages.Usage;
import harness.Scripted;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Count tokens before sending, then price the reply from its usage object.
 *
 * <p>Both calls go through a scripted transport, so nothing leaves the container. The replies are illustrative,
 * hand-written responses in the API's shapes (claude-sonnet-5-5), not captures. Prices are dollars per million
 * tokens, read from the Claude pricing page on 2026-10-02.
 */
public final class Cost {
    private static final System.Logger LOG = System.getLogger(Cost.class.getName());
    static final String MODEL = "claude-sonnet-5-5";
    static final String SYSTEM = "You answer from the policy document.";
    static final String QUESTION = "Summarise the refund policy in two sentences.";

    /** input, output, cache-read multiplier */
    static final Map<String, double[]> PRICES = new LinkedHashMap<>();

    static {
        PRICES.put("claude-haiku-4-5-20251001", new double[] {1.0, 5.0, 0.1});
        PRICES.put("claude-sonnet-5-5", new double[] {2.0, 10.0, 0.1});
        PRICES.put("claude-opus-5-5", new double[] {4.0, 20.0, 0.05});
        PRICES.put("claude-fable-5-1", new double[] {10.0, 50.0, 0.025});
    }

    static final Map<String, Object> USAGE = map("input_tokens", 120, "output_tokens", 340, "cache_read_input_tokens", 0, "cache_creation_input_tokens", 4000,
        "cache_creation", map("ephemeral_5m_input_tokens", 4000, "ephemeral_1h_input_tokens", 0));

    static MessageCreateParams params() {
        return MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(300).system(SYSTEM).addUserMessage(QUESTION).build();
    }

    static double cost(String model, Usage usage) {
        return cost(model, usage, false);
    }

    /** Dollars for one request: cache writes cost 1.25x input (5 minutes) or 2x (1 hour), reads a model-specific fraction. */
    static double cost(String model, Usage usage, boolean batch) {
        double[] price = PRICES.get(model);
        double priceIn = price[0], priceOut = price[1], read = price[2];
        double perToken = usage.inputTokens() * priceIn
            + usage.cacheCreation().get().ephemeral5mInputTokens() * priceIn * 1.25
            + usage.cacheCreation().get().ephemeral1hInputTokens() * priceIn * 2.0
            + usage.cacheReadInputTokens().get() * priceIn * read
            + usage.outputTokens() * priceOut;
        return perToken / 1_000_000 * (batch ? 0.5 : 1.0);
    }

    private static String dollars(double v) {
        return String.format(Locale.ROOT, "$%.6f", v);
    }

    public static void main(String[] args) {
        Scripted.Rig rig = Scripted.client(map("input_tokens", 4821), message(List.of(text("Refunds take 14 days. Opened items are excluded.")), "end_turn", MODEL, USAGE, null));
        long counted = rig.client().messages().countTokens(MessageCountTokensParams.builder().model(MODEL).system(SYSTEM).addUserMessage(QUESTION).build()).inputTokens();
        System.out.println("count_tokens -> " + counted + " input tokens (POST " + rig.http().urls.get(0).split("\\.com")[1] + ")");
        double priceIn = PRICES.get(MODEL)[0];
        System.out.println("estimate before sending, input only: " + dollars(counted * priceIn / 1_000_000) + " on " + MODEL);
        Usage u = rig.client().messages().create(params()).usage();
        System.out.println("usage: input " + u.inputTokens() + ", cache write " + u.cacheCreationInputTokens().get() + ", cache read " + u.cacheReadInputTokens().get() + ", output " + u.outputTokens());
        System.out.println("cost of this request: " + dollars(cost(MODEL, u)) + "  (batch: " + dollars(cost(MODEL, u, true)) + ")");
        System.out.println("the same usage on each model:");
        for (String model : PRICES.keySet()) System.out.println(String.format("  %-28s ", model) + dollars(cost(model, u)));
    }
}
```
```text
count_tokens -> 4821 input tokens (POST /v1/messages/count_tokens)
estimate before sending, input only: $0.009642 on claude-sonnet-5-5
usage: input 120, cache write 4000, cache read 0, output 340
cost of this request: $0.013640  (batch: $0.006820)
the same usage on each model:
  claude-haiku-4-5-20251001    $0.006820
  claude-sonnet-5-5            $0.013640
  claude-opus-5-5              $0.027280
  claude-fable-5-1             $0.068200
```
```kotlin
import com.anthropic.models.messages.MessageCountTokensParams
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.Model
import com.anthropic.models.messages.Usage
import harness.Scripted
import harness.Scripted.map
import harness.Scripted.message
import harness.Scripted.text

private val log = System.getLogger("cost")

/**
 * Count tokens before sending, then price the reply from its usage object.
 *
 * Both calls go through a scripted transport, so nothing leaves the container. The replies are illustrative,
 * hand-written responses in the API's shapes (claude-sonnet-5-5), not captures. Prices are dollars per million
 * tokens, read from the Claude pricing page on 2026-10-02.
 */
const val MODEL = "claude-sonnet-5-5"
const val SYSTEM = "You answer from the policy document."
const val QUESTION = "Summarise the refund policy in two sentences."

/** input, output, cache-read multiplier */
val PRICES = linkedMapOf(
    "claude-haiku-4-5-20251001" to Triple(1.0, 5.0, 0.1),
    "claude-sonnet-5-5" to Triple(2.0, 10.0, 0.1),
    "claude-opus-5-5" to Triple(4.0, 20.0, 0.05),
    "claude-fable-5-1" to Triple(10.0, 50.0, 0.025),
)

val USAGE = map(
    "input_tokens", 120, "output_tokens", 340, "cache_read_input_tokens", 0, "cache_creation_input_tokens", 4000,
    "cache_creation", map("ephemeral_5m_input_tokens", 4000, "ephemeral_1h_input_tokens", 0),
)

fun params(): MessageCreateParams = MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(300).system(SYSTEM).addUserMessage(QUESTION).build()

/** Dollars for one request: cache writes cost 1.25x input (5 minutes) or 2x (1 hour), reads a model-specific fraction. */
fun cost(model: String, usage: Usage, batch: Boolean = false): Double {
    val (priceIn, priceOut, read) = PRICES.getValue(model)
    val perToken = (usage.inputTokens() * priceIn
        + usage.cacheCreation().get().ephemeral5mInputTokens() * priceIn * 1.25
        + usage.cacheCreation().get().ephemeral1hInputTokens() * priceIn * 2.0
        + usage.cacheReadInputTokens().get() * priceIn * read
        + usage.outputTokens() * priceOut)
    return perToken / 1_000_000 * (if (batch) 0.5 else 1.0)
}

private fun dollars(v: Double) = "$" + "%.6f".format(v)

fun main() {
    val rig = Scripted.client(map("input_tokens", 4821), message(listOf(text("Refunds take 14 days. Opened items are excluded.")), "end_turn", MODEL, USAGE, null))
    val counted = rig.client().messages().countTokens(MessageCountTokensParams.builder().model(MODEL).system(SYSTEM).addUserMessage(QUESTION).build()).inputTokens()
    println("count_tokens -> $counted input tokens (POST ${rig.http().urls[0].split(".com")[1]})")
    println("estimate before sending, input only: ${dollars(counted * PRICES.getValue(MODEL).first / 1_000_000)} on $MODEL")
    val u = rig.client().messages().create(params()).usage()
    println("usage: input ${u.inputTokens()}, cache write ${u.cacheCreationInputTokens().get()}, cache read ${u.cacheReadInputTokens().get()}, output ${u.outputTokens()}")
    println("cost of this request: ${dollars(cost(MODEL, u))}  (batch: ${dollars(cost(MODEL, u, batch = true))})")
    println("the same usage on each model:")
    for (model in PRICES.keys) println("  ${model.padEnd(28)} ${dollars(cost(model, u))}")
}
```
```text
count_tokens -> 4821 input tokens (POST /v1/messages/count_tokens)
estimate before sending, input only: $0.009642 on claude-sonnet-5-5
usage: input 120, cache write 4000, cache read 0, output 340
cost of this request: $0.013640  (batch: $0.006820)
the same usage on each model:
  claude-haiku-4-5-20251001    $0.006820
  claude-sonnet-5-5            $0.013640
  claude-opus-5-5              $0.027280
  claude-fable-5-1             $0.068200
```
<!-- /example -->

Read the last table of the output. The same usage costs $0.006820 on Haiku 4.5, twice that on Sonnet 5.5, four times on Opus
5.5 and ten times on Fable 5.1: the costs follow the input prices (1, 2, 4 and 10), because every part of this usage is priced
from the input price and output is five times input on every row. The ratios break only where a model has its own cache read
multiplier. A prompt that is mostly cache reads costs the same on Sonnet 5.5 and Opus 5.5, although Opus lists twice the price.
That is why a router must price the real usage and not compare list prices.

## The practice: a router and a cost model

You write `request_cost(model, usage, batch)` and `route(catalog, task)`. The first prices a usage object with the rules above,
in micro-dollars. The second returns the id of the cheapest model that can take a task: not deprecated, at or above the task's
minimum tier, with a context window that holds the task's input and an output limit that holds its `max_tokens`. A cost tie
goes to the lower tier, and the order of the catalog never matters. The statement is in
`exercises/18-model-choice-cost-and-migration/unit-01/practice-1/statement.md`; each language folder has a `starter`, the tests
and a build file. The starter fails every test.

| Id | What it checks |
|---|---|
| `m1` | A plain request is priced from input and output, and the router returns the cheapest model at or above the tier |
| `e1` | Cache writes cost 1.25 and 2 times input, reads use the model's own multiplier, and an unsplit write is the 5-minute kind |
| `e2` | The batch discount halves every part of the cost, cache parts included |
| `e3` | The router picks by cost and tier, whatever the order of the catalog |
| `e4` | A model with too small a context window or output limit is skipped |
| `e5` | Deprecated models are skipped, and a choice with no model raises `NoModelError` |
| `e6` | A cost tie goes to the lower tier |

Case `e6` is built on a real tie: on a prompt that is all cache reads, Sonnet 5.5 at $2 and 0.1 costs the same as Opus 5.5 at
$4 and 0.05. A router that breaks ties by list order gives an answer that depends on how the catalog was written.

## Traps

1. **Pricing input as one number.** A usage object has four input-side fields at three prices. Multiplying
   `input_tokens` by the input price misses the cache writes and reads, and with caching on, `input_tokens` is only the tail.
2. **Applying one cache multiplier to every model.** Reads are 0.1 of input on most models and 0.05 and 0.025 on two of them.
   A cost model with a constant is wrong exactly where the large models are used.
3. **Treating the counted tokens as the bill.** The count is an estimate of input only. Output, thinking and the cache split
   appear only in `usage` after the call.

## Quiz

1. A response reports 120 `input_tokens`, 4,000 `cache_creation_input_tokens` and 340 `output_tokens`. A team adds them up as
   460 tokens in and out. What does the page say they missed?
   - **a**: The thinking inside the output is billed at the input price
   - **b**: The cache write has its own price above the base input price
   - **c**: The count in `input_tokens` already includes the written tokens
   - **d**: The 4,000 tokens are cache reads billed at a tenth of input

2. Before sending a long prompt, an application wants to know whether it fits a budget. Which tool does the page back, and with
   what caveat?
   - **a**: A fixed ratio of characters per token, allowing some drift by model
   - **b**: A trial request with a tiny `max_tokens`, whose usage is exact and free of charge
   - **c**: The `usage` of an earlier call on a similar prompt, read as exact
   - **d**: The token counting endpoint, allowing a small gap from the billed input

3. In this module's practice, a request made only of cache reads costs the same on a model at $2 with a 0.1 read rate and on
   one at $4 with 0.05. What does the router return?
   - **a**: The one listed first, since order is the last signal left
   - **b**: Sonnet 5.5, since the tier settles it when the totals match
   - **c**: The one with the larger context window, kept as spare headroom
   - **d**: Opus 5.5, since quality is worth having at equal spend

<details>
<summary>Answer key</summary>

1. **b**. The page prices a 5-minute cache write at "1.25 times input", so 4,000 written tokens are not ordinary input. *a* is ruled out because output tokens are "Tokens generated, thinking included", all priced at the output price. *c* is ruled out because `input_tokens` is "Input tokens that were not read from or written to a cache". *d* is ruled out because `cache_creation_input_tokens` are "Tokens written to the cache", while a tenth of input is the price of "Tokens read from the cache".
2. **d**. The page says "The token count is an estimate" and "the real count may differ by a small amount", while "Token counting is free to use but subject to requests per minute rate limits based on your usage tier." *b* is ruled out because a trial request is a billed Messages call (the page calls counting "free to use"), while the page offers counting as a separate call with "separate and independent rate limits". *c* is ruled out because `usage` reports a call already made on another input, while the page counts the new prompt itself "to decide before paying". *a* is ruled out because the endpoint "takes the same structured input as a Messages request", tools, images and documents included, which a character ratio cannot see.
3. **b**. Case `e6` is "built on a real tie", the $2 model being Sonnet 5.5, the lower of the two tiers, and the practice sends a tie down a tier "and the order of the catalog never matters". *a* is ruled out because a router that breaks ties by list order gives "an answer that depends on how the catalog was written". *c* is ruled out because the window is a filter, a model needs only "a context window that holds the task's input", and the rule never ranks windows. *d* is ruled out because "A cost tie goes to the lower tier", and Opus 5.5 is the higher one.

</details>
