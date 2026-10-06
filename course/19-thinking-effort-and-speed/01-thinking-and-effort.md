# Adaptive thinking, extended thinking and effort

**Level:** Developer · **Module 19:** Thinking, effort and speed · **Page 1 of 2**
**Exams:** DV2

**After this page you can** say how adaptive and extended thinking differ, set `thinking` and `output_config.effort` correctly
for each course model, read what the thinking cost from `usage`, and bound spend with `max_tokens` and effort.

Checked against the Claude API documentation (Thinking, Steering thinking, Effort, Extended thinking) on 2026-10-02, and by
running the example offline in the course container (`anthropic` 1.11.0, `@anthropic-ai/sdk` 0.131.0). The reply in the example
is an illustrative, hand-written response in the API's shape, not a capture.

## Why it matters

Thinking is the other large cost lever after the model tier. It adds output tokens, which are the dear ones, and it changes
latency. It is also where model migrations break: a request that worked on one model returns a 400 on the next because its
thinking setting no longer exists. The exam asks which setting a model takes, what effort does, and why the bill for a thinking
call is larger than the text you can see.

## The idea

### Two kinds of thinking

**Extended thinking** is manual: the request sets `thinking` to `{"type": "enabled", "budget_tokens": N}` and the model thinks up to
a budget. **Adaptive thinking** is the newer mode: the model decides for each request whether to think and how much, steered by
the effort setting. The Steering thinking page puts it plainly:

> Claude's thinking is adaptive: the model evaluates each request and decides for itself whether to think and how much.

Source: Steering thinking.

Which of the two a model has is a property of the model. This table is the part of the documentation's per-model table that
covers the four course models, read on 2026-10-02:

| Model | No `thinking` field | `adaptive` | `enabled` with budget | `between_tools` | `disabled` |
|---|---|---|---|---|---|
| Claude Fable 5.1 | adaptive thinking | adaptive thinking | 400 error | 400 error | 400 error |
| Claude Opus 5.5 | adaptive thinking | adaptive thinking | 400 error | 400 error | 400 error |
| Claude Sonnet 5.5 | adaptive thinking | adaptive thinking | 400 error | up-front thinking off at high effort or below | 400 error |
| Claude Haiku 4.5 | thinking off | 400 error | extended thinking | 400 error | thinking off |

Read the rows as three families. Fable 5.1 and Opus 5.5 always think adaptively and cannot be told otherwise. Sonnet 5.5 thinks
adaptively too, and has a lowest setting, `between_tools`, that turns off the up-front thinking and keeps the short updates
between tool calls; it works at `low`, `medium` and `high` effort and returns a 400 at `xhigh` and `max`. Haiku 4.5 is the old
kind: no thinking unless you ask, and then only the manual budget. A `budget_tokens` has two rules: at least 1,024, and less than
`max_tokens`.

On the models where thinking is always on, the thinking text is hidden by default. The documentation says `display` defaults to
`"omitted"` there, so the response carries a thinking block whose `thinking` field is empty and whose signature carries the
encrypted reasoning. Opt in to a summary with `thinking: {"type": "adaptive", "display": "summarized"}`. Pass thinking blocks back
unchanged in a tool loop, empty ones included.

### What thinking costs

Thinking tokens are output tokens: the page lists them as "billed as output tokens". It states the consequence in one sentence that is easy to forget:

> You are billed for the full thinking process, not the thinking content visible in the response.

Source: Steering thinking.

So the visible text understates the bill. Read the breakdown from `usage.output_tokens_details.thinking_tokens`: it is never more than `output_tokens`, and `output_tokens` is the total that is billed. Thinking also
counts against `max_tokens`, shown or not, so a `max_tokens` sized for a plain answer is often too small once the model starts
to think, and the call stops with `stop_reason: "max_tokens"`. The page offers two remedies and says which one to pick: raise
`max_tokens` if the truncated answers needed the reasoning, lower the effort if they were over-thought.

### Effort: the control that replaced the budget

Effort goes in `output_config`, not inside `thinking`:

```json
{ "model": "claude-opus-5-5", "max_tokens": 4096, "output_config": { "effort": "medium" }, "messages": [] }
```

There are five levels, `low`, `medium`, `high`, `xhigh` and `max`, and all five are supported on Fable 5.1, Opus 5.5 and Sonnet 5.5.
Haiku 4.5 does not support effort. The default is `high` on every model that supports effort except Opus 5.5, whose default is
`medium`. Passing the default is the same as passing nothing: setting effort to the model's default "produces exactly the same
behavior as omitting the effort parameter entirely".

Three properties matter for design:

- **Effort is soft.** The page says "Effort is a behavioral signal, not a strict token budget." Only `max_tokens` is a hard ceiling: "`max_tokens` is a strict limit."
- **Effort scales everything.** It affects "all tokens in the response", which means text, tool calls and thinking. Lower effort also
  means fewer and terser tool calls. It works with or without thinking.
- **`adaptive` is not an effort level.** The page warns: "Don't pass adaptive as an effort value: adaptive is a thinking mode,
  not an effort level."

To lower cost or latency, the page says to "lower effort first", because it "scales the whole response down, thinking included".
How to choose a level: run an effort sweep on your own evaluation. The page says of Sonnet 5.5 that "its levels are recalibrated", so a
level does not give the thinking it gave on Sonnet 5. A starting point from the page: for Sonnet 5.5, `high` unless the workload is agentic or latency-sensitive; for Opus 5.5,
`medium` is the default and the primary control for how much the model reasons and what a request costs.

### The example

The example sends a request with adaptive thinking and `effort: high`, reads the reply's blocks, and splits the output tokens into
thinking and answer. A second, low-effort reply has no thinking block at all: the model chose not to think.

<!-- example: m19-thinking-and-effort tabs: python,typescript,java,kotlin -->
```python
"""Adaptive thinking steered by effort: the request, the reply's blocks and what the thinking cost.

The reply is an illustrative, hand-written response in the API's shape (claude-opus-5-5), not a capture. It carries
an omitted thinking block (the default display on this model: an empty `thinking` field and a signature) and the
`output_tokens_details.thinking_tokens` breakdown the thinking page documents.
"""
import logging
from harness import scripted_client
from harness.scripted import message, text

log = logging.getLogger(__name__)

MODEL = "claude-opus-5-5"
PRICE_OUT = 20.0  # dollars per million output tokens, pricing page 2026-10-02
THINKING_BLOCK = {"type": "thinking", "thinking": "", "signature": "illustrative-signature"}
USAGE = {"input_tokens": 410, "output_tokens": 1900, "output_tokens_details": {"thinking_tokens": 1650}}


def thinking_tokens(usage):
    """usage.output_tokens_details.thinking_tokens, read whether the SDK types the field or keeps it as an extra."""
    details = getattr(usage, "output_tokens_details", None) or (usage.model_extra or {}).get("output_tokens_details")
    return details["thinking_tokens"] if isinstance(details, dict) else details.thinking_tokens


def request(client, effort):
    return client.messages.create(model=MODEL, max_tokens=8000, thinking={"type": "adaptive"}, output_config={"effort": effort},
                                  messages=[{"role": "user", "content": "Which of these two schedules has no conflicts?"}])


def main():
    reply_body = message([THINKING_BLOCK, text("Schedule B has no conflicts.")], usage=USAGE)
    client, transport = scripted_client(reply_body, message([text("B.")], usage={"input_tokens": 410, "output_tokens": 12}))
    reply = request(client, "high")
    sent = transport.requests[0]
    print("thinking sent:", sent["thinking"], "| effort sent:", sent["output_config"])
    print("blocks:", [b.type for b in reply.content], "| thinking text shown:", repr(reply.content[0].thinking))
    thinking = thinking_tokens(reply.usage)
    print(f"output_tokens {reply.usage.output_tokens} = thinking {thinking} + answer {reply.usage.output_tokens - thinking}")
    print(f"output cost: ${reply.usage.output_tokens * PRICE_OUT / 1_000_000:.4f} (thinking is billed as output, shown or not)")
    quick = request(client, "low")
    print("a low-effort turn may skip thinking:", [b.type for b in quick.content], "| output_tokens", quick.usage.output_tokens)
    print("effort differs between the two requests:", transport.requests[0]["output_config"] != transport.requests[1]["output_config"])


if __name__ == "__main__":
    main()
```
```text
thinking sent: {'type': 'adaptive'} | effort sent: {'effort': 'high'}
blocks: ['thinking', 'text'] | thinking text shown: ''
output_tokens 1900 = thinking 1650 + answer 250
output cost: $0.0380 (thinking is billed as output, shown or not)
a low-effort turn may skip thinking: ['text'] | output_tokens 12
effort differs between the two requests: True
```
```typescript
// Adaptive thinking steered by effort: the request, the reply's blocks and what the thinking cost.
// The reply is an illustrative, hand-written response in the API's shape (claude-opus-5-5), not a capture. It carries
// an omitted thinking block (the default display on this model: an empty `thinking` field and a signature) and the
// `output_tokens_details.thinking_tokens` breakdown the thinking page documents.
import Anthropic from "@anthropic-ai/sdk";
import { message, scriptedFetch, text } from "../../../harness/ts/scriptedFetch.ts";
import { logger } from "./logger.ts";
const log = logger("thinking");

export const MODEL = "claude-opus-5-5";
const PRICE_OUT = 20.0; // dollars per million output tokens, pricing page 2026-10-02
export const THINKING_BLOCK = { type: "thinking", thinking: "", signature: "illustrative-signature" };
export const USAGE = { input_tokens: 410, output_tokens: 1900, output_tokens_details: { thinking_tokens: 1650 } };

export function clientFor(...bodies: unknown[]) {
  const fake = scriptedFetch(bodies.map((body) => ({ body })));
  return { fake, client: new Anthropic({ apiKey: "placeholder", maxRetries: 0, fetch: fake.fetch }) };
}

export function request(client: Anthropic, effort: string) {
  return client.messages.create({
    model: MODEL,
    max_tokens: 8000,
    thinking: { type: "adaptive" },
    output_config: { effort } as any,
    messages: [{ role: "user", content: "Which of these two schedules has no conflicts?" }],
  } as any);
}

async function main() {
  const { fake, client } = clientFor(message([THINKING_BLOCK, text("Schedule B has no conflicts.")], "end_turn", USAGE as any), message([text("B.")], "end_turn", { input_tokens: 410, output_tokens: 12 }));
  const reply: any = await request(client, "high");
  const sent = fake.seen[0].body;
  console.log("thinking sent:", JSON.stringify(sent.thinking), "| effort sent:", JSON.stringify(sent.output_config));
  console.log("blocks:", JSON.stringify(reply.content.map((b: any) => b.type)), "| thinking text shown:", JSON.stringify(reply.content[0].thinking));
  const thinking = reply.usage.output_tokens_details.thinking_tokens;
  console.log(`output_tokens ${reply.usage.output_tokens} = thinking ${thinking} + answer ${reply.usage.output_tokens - thinking}`);
  console.log(`output cost: $${((reply.usage.output_tokens * PRICE_OUT) / 1_000_000).toFixed(4)} (thinking is billed as output, shown or not)`);
  const quick: any = await request(client, "low");
  console.log("a low-effort turn may skip thinking:", JSON.stringify(quick.content.map((b: any) => b.type)), "| output_tokens", quick.usage.output_tokens);
}

if (import.meta.main) await main();
```
```text
thinking sent: {"type":"adaptive"} | effort sent: {"effort":"high"}
blocks: ["thinking","text"] | thinking text shown: ""
output_tokens 1900 = thinking 1650 + answer 250
output cost: $0.0380 (thinking is billed as output, shown or not)
a low-effort turn may skip thinking: ["text"] | output_tokens 12
```
```java
import static harness.Scripted.map;
import static harness.Scripted.message;
import static harness.Scripted.text;
import static harness.Show.py;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.ThinkingConfigAdaptive;
import com.anthropic.models.messages.Usage;
import harness.Scripted;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Adaptive thinking steered by effort: the request, the reply's blocks and what the thinking cost.
 *
 * <p>The reply is an illustrative, hand-written response in the API's shape (claude-opus-5-5), not a capture. It carries
 * an omitted thinking block (the default display on this model: an empty `thinking` field and a signature) and the
 * `output_tokens_details.thinking_tokens` breakdown the thinking page documents.
 * (`py` is the harness's formatter: it prints a value the way the Python edition does, so the output of the editions matches.)
 */
public final class Thinking {
    private static final System.Logger LOG = System.getLogger(Thinking.class.getName());
    static final String MODEL = "claude-opus-5-5";
    static final double PRICE_OUT = 20.0; // dollars per million output tokens, pricing page 2026-10-02
    static final Map<String, Object> THINKING_BLOCK = map("type", "thinking", "thinking", "", "signature", "illustrative-signature");
    static final Map<String, Object> USAGE = map("input_tokens", 410, "output_tokens", 1900, "output_tokens_details", map("thinking_tokens", 1650));

    /** usage.output_tokens_details.thinking_tokens */
    static long thinkingTokens(Usage usage) {
        return usage.outputTokensDetails().get().thinkingTokens();
    }

    static Message request(AnthropicClient client, String effort) {
        return client.messages().create(MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(8000)
            .thinking(ThinkingConfigAdaptive.builder().build()).outputConfig(OutputConfig.builder().effort(OutputConfig.Effort.of(effort)).build())
            .addUserMessage("Which of these two schedules has no conflicts?").build());
    }

    static String kinds(Message reply) {
        return py(reply.content().stream().map(b -> b.isThinking() ? "thinking" : b.isText() ? "text" : "other").toList());
    }

    public static void main(String[] args) {
        Map<String, Object> replyBody = message(List.of(THINKING_BLOCK, text("Schedule B has no conflicts.")), "end_turn", MODEL, USAGE, null);
        Scripted.Rig rig = Scripted.client(replyBody, message(List.of(text("B.")), "end_turn", MODEL, map("input_tokens", 410, "output_tokens", 12), null));
        Message reply = request(rig.client(), "high");
        var sent = rig.http().requests.get(0);
        System.out.println("thinking sent: " + py(sent.get("thinking")) + " | effort sent: " + py(sent.get("output_config")));
        ContentBlock first = reply.content().get(0);
        System.out.println("blocks: " + kinds(reply) + " | thinking text shown: " + py(first.asThinking().thinking()));
        long thinking = thinkingTokens(reply.usage());
        System.out.println("output_tokens " + reply.usage().outputTokens() + " = thinking " + thinking + " + answer " + (reply.usage().outputTokens() - thinking));
        System.out.println(String.format(Locale.ROOT, "output cost: $%.4f (thinking is billed as output, shown or not)", reply.usage().outputTokens() * PRICE_OUT / 1_000_000));
        Message quick = request(rig.client(), "low");
        System.out.println("a low-effort turn may skip thinking: " + kinds(quick) + " | output_tokens " + quick.usage().outputTokens());
        System.out.println("effort differs between the two requests: " + py(!rig.http().requests.get(0).get("output_config").equals(rig.http().requests.get(1).get("output_config"))));
    }
}
```
```text
thinking sent: {'type': 'adaptive'} | effort sent: {'effort': 'high'}
blocks: ['thinking', 'text'] | thinking text shown: ''
output_tokens 1900 = thinking 1650 + answer 250
output cost: $0.0380 (thinking is billed as output, shown or not)
a low-effort turn may skip thinking: ['text'] | output_tokens 12
effort differs between the two requests: True
```
```kotlin
import com.anthropic.client.AnthropicClient
import com.anthropic.models.messages.Message
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.Model
import com.anthropic.models.messages.OutputConfig
import com.anthropic.models.messages.ThinkingConfigAdaptive
import com.anthropic.models.messages.Usage
import harness.Scripted
import harness.Scripted.map
import harness.Scripted.message
import harness.Scripted.text
import harness.Show.py

private val log = System.getLogger("thinking")

/**
 * Adaptive thinking steered by effort: the request, the reply's blocks and what the thinking cost.
 *
 * The reply is an illustrative, hand-written response in the API's shape (claude-opus-5-5), not a capture. It carries
 * an omitted thinking block (the default display on this model: an empty `thinking` field and a signature) and the
 * `output_tokens_details.thinking_tokens` breakdown the thinking page documents.
 * (`py` is the harness's formatter: it prints a value the way the Python edition does, so the output of the editions matches.)
 */
const val MODEL = "claude-opus-5-5"
const val PRICE_OUT = 20.0 // dollars per million output tokens, pricing page 2026-10-02
val THINKING_BLOCK = map("type", "thinking", "thinking", "", "signature", "illustrative-signature")
val USAGE = map("input_tokens", 410, "output_tokens", 1900, "output_tokens_details", map("thinking_tokens", 1650))

/** usage.output_tokens_details.thinking_tokens */
fun thinkingTokens(usage: Usage): Long = usage.outputTokensDetails().get().thinkingTokens()

fun request(client: AnthropicClient, effort: String): Message =
    client.messages().create(
        MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(8000)
            .thinking(ThinkingConfigAdaptive.builder().build()).outputConfig(OutputConfig.builder().effort(OutputConfig.Effort.of(effort)).build())
            .addUserMessage("Which of these two schedules has no conflicts?").build(),
    )

fun kinds(reply: Message): String = py(reply.content().map { if (it.isThinking()) "thinking" else if (it.isText()) "text" else "other" })

fun main() {
    val replyBody = message(listOf(THINKING_BLOCK, text("Schedule B has no conflicts.")), "end_turn", MODEL, USAGE, null)
    val rig = Scripted.client(replyBody, message(listOf(text("B.")), "end_turn", MODEL, map("input_tokens", 410, "output_tokens", 12), null))
    val reply = request(rig.client(), "high")
    val sent = rig.http().requests[0]
    println("thinking sent: ${py(sent["thinking"])} | effort sent: ${py(sent["output_config"])}")
    println("blocks: ${kinds(reply)} | thinking text shown: ${py(reply.content()[0].asThinking().thinking())}")
    val thinking = thinkingTokens(reply.usage())
    println("output_tokens ${reply.usage().outputTokens()} = thinking $thinking + answer ${reply.usage().outputTokens() - thinking}")
    println("output cost: $${"%.4f".format(reply.usage().outputTokens() * PRICE_OUT / 1_000_000)} (thinking is billed as output, shown or not)")
    val quick = request(rig.client(), "low")
    println("a low-effort turn may skip thinking: ${kinds(quick)} | output_tokens ${quick.usage().outputTokens()}")
    println("effort differs between the two requests: ${py(rig.http().requests[0]["output_config"] != rig.http().requests[1]["output_config"])}")
}
```
```text
thinking sent: {'type': 'adaptive'} | effort sent: {'effort': 'high'}
blocks: ['thinking', 'text'] | thinking text shown: ''
output_tokens 1900 = thinking 1650 + answer 250
output cost: $0.0380 (thinking is billed as output, shown or not)
a low-effort turn may skip thinking: ['text'] | output_tokens 12
effort differs between the two requests: True
```
<!-- /example -->

In the output, 1,650 of the 1,900 output tokens are thinking, and the thinking text shown is empty. The bill covers all 1,900. In
the TypeScript tab the same lines are formatted with JSON quoting; the numbers are identical.

## Traps

1. **Estimating cost from the visible answer.** The answer is a small part of the output on a thinking call. Price
   `output_tokens`, not the text length, and watch `thinking_tokens`.
2. **Changing effort between turns of one conversation.** The thinking page says the resolved effort "is rendered into the
   prompt", so a change starts the cache over for messages (module 20). Choose a level per conversation, or vary it with the
   message-level mechanism on the models that have it.
3. **Passing an old thinking setting to a new model.** `disabled` and a manual budget return a 400 on the always-on models, and
   `adaptive` returns a 400 on Haiku 4.5. Build the thinking setting from the model, as the practice does.

## Quiz

1. A team wants to cut the cost of a workload on Claude Opus 5.5 that currently thinks a lot. A developer proposes setting
   `thinking` to `disabled`. What does the page say?
   - **a**: The request returns a 400, so remove the output cap instead
   - **b**: The request succeeds, and thinking stops for that call alone
   - **c**: The request succeeds, and thinking falls to its lowest setting
   - **d**: The request returns a 400, so lower the effort level instead

2. A response has a short visible reply and an `output_tokens` of 1,900. The `thinking_tokens` field reads 1,650. What is
   billed as output?
   - **a**: All 1,900 tokens, since thinking is charged even when it is not shown
   - **b**: Only 250 tokens, since the thinking was hidden by the default display
   - **c**: Only 1,650 tokens, since the answer is covered by the input price
   - **d**: About 80 tokens, since billing follows the words that are returned

3. A request on Claude Haiku 4.5 puts `low` under `output_config` to save money. What does the page back?
   - **a**: Haiku 4.5 takes the setting only together with a manual budget
   - **b**: Haiku 4.5 treats `low` as its default, so the setting changes nothing
   - **c**: That model has no such control, so the setting buys no saving
   - **d**: Haiku 4.5 applies the setting to tool calls and ignores thinking

<details>
<summary>Answer key</summary>

1. **d**. The model table shows "400 error" for `disabled` on Opus 5.5, and the page says to "lower effort first" to cut cost. *b* is ruled out because "Fable 5.1 and Opus 5.5 always think adaptively and cannot be told otherwise". *c* is ruled out because the lowest setting named is Sonnet 5.5's, "up-front thinking off at high effort or below", and Opus 5.5 has no such setting. *a* is ruled out because "`max_tokens` is a strict limit" on spend and does not change how much the model thinks.
2. **a**. The page says "You are billed for the full thinking process, not the thinking content visible in the response." *b* is ruled out because the thinking tokens are "billed as output tokens", whether the display shows them or not. *c* is ruled out because the page bills "the full thinking process, not the thinking content visible in the response", so the answer is not the only part charged. *d* is ruled out because the page says the visible text understates the bill: "So the visible text understates the bill."
3. **c**. The page says "Haiku 4.5 does not support effort", and the table of effort support lists the other three models. *b* is ruled out because the default row names only Opus 5.5 and the models that support effort: "The default is high on every model that supports effort except Opus 5.5". *a* is ruled out because the manual budget belongs to the thinking setting, and the page puts effort "in output_config, not inside thinking". *d* is ruled out because the page says effort "works with or without thinking" on models that support it.

</details>
