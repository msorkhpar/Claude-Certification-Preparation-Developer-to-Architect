# Fast mode, thinking with tools, and thinking's effect on caching

**Level:** Developer · **Module 19:** Thinking, effort and speed · **Page 2 of 2**
**Exams:** DV2

**After this page you can** decide when fast mode is worth its price, keep thinking consistent through a tool loop, predict
which thinking and speed changes break a prompt cache, and write the request builder that applies all the per-model rules.

Checked against the Claude API documentation (Fast mode, Thinking, Prompt caching, Pricing, Migrating to Claude Opus 5.5) on
2026-10-02, and by running the practice offline in the course container in Python, TypeScript, Java and Kotlin.

## Why it matters

Speed, depth and price pull against each other, and each control sits in a different place: the model, the effort level, the
thinking mode, the speed flag. Changing one in the middle of a conversation can quietly discard a cache that was saving money.
The exam asks which control fixes which problem, and what a change costs you downstream.

## The idea

### Fast mode: faster output, higher price

The Fast mode page describes it in one sentence:

> Fast mode delivers up to 2.5x higher output tokens per second from Claude Opus 5.5, Claude Opus 5, and Claude Opus 4.8 at premium pricing.

Source: Fast mode.

It is the same model on a faster inference configuration: "There is no change to intelligence or capabilities." You opt in with
`speed: "fast"` and the beta header `fast-mode-2026-02-01`. Five limits decide whether it is an option for you:

- **It speeds output, not the first token.** "Speed benefits are focused on output tokens per second (OTPS), not time to first
  token (TTFT)." A feature that waits for the first word gains little.
- **It costs more.** On Opus 5.5 fast mode is $8 input and $40 output per million tokens, twice the standard $4 and $20.
  The pricing applies "across the full context window", and caching and data residency multipliers apply on top.
- **Only some models have it.** Opus 5.5, Opus 5 and Opus 4.8 do. Opus 4.7 rejects the flag with an error, and Opus 4.6 runs the
  request at standard speed and bills it at standard rates.
- **It is API-only.** The Pricing page says it "is available on the Claude API (first-party) only; it is not available on Claude
  Platform on AWS or partner-operated cloud platforms". Bedrock and Google Cloud do not have it (module 22).
- **It is not for batches.** "Fast mode is not available with the Batch API." A batch is not waiting on anyone's screen, so there is
  nothing for it to speed up.

It is a research preview, so access may need a request to the account team. Use it for interactive, output-heavy work where a
person watches the text arrive, and measure whether the seconds saved are worth twice the price.

### Thinking with tools

Thinking works with tool use, and two rules govern it. First, pass the thinking blocks back with the tool results, complete and
unmodified, empty ones included. Second, treat the whole loop as one turn: "A tool-use loop is one assistant turn." The page
continues:

> The entire turn runs in a single thinking mode: you can't toggle thinking in the middle of a turn, including during the tool-use loop.

Source: Thinking.

If a request changes the thinking mode in mid-turn, the API does not error: it "silently disables thinking for that request".
The page's advice is "Toggle between turns, not within them." In practice, choose the thinking and effort setting when a user
message arrives and keep it until the model's final answer.

Forced tool use interacts with thinking too. Manual extended thinking supports only `tool_choice` of `auto` or `none`. Adaptive
thinking supports forced tool use, except on Claude Opus 5.5, Claude Sonnet 5.5, Claude Fable 5.1 and Claude Mythos 5.1 (the
last shares the specifications of Fable 5.1 and is offered by invitation only). On those models, use `auto` and get a structured result another way.

### What breaks the cache

The prompt caching page lists the changes that invalidate a cached prefix, where "✘ indicates that the cache is invalidated, while
✓ indicates that the cache remains valid". The rows that matter here:

| Change | Tools | System | Messages |
|---|---|---|---|
| Speed setting (fast or standard) | ✓ | ✘ | ✘ |
| Tool choice | ✓ | ✓ | ✘ |
| Thinking parameters | model-specific | model-specific | ✘ |
| Effort setting | model-specific | model-specific | ✘ |

Changing the thinking mode or the effort always discards the cached messages, because the resolved configuration "is rendered into
the prompt". The one exception on the prompt caching page is per-message effort: on models that support it, an effort change carried in a `role: "system"` message inside `messages` leaves the cached prefix intact, which a change to the top-level setting does not. Setting a parameter explicitly to its default is the same as omitting it, so it does not break the cache. Cached
thinking blocks also cost something: in a tool loop they are cached with the tool results, and when read from the cache they "count
as input tokens in your usage metrics". The consequence is a design rule: pick the model, the thinking mode, the effort and the
speed once per conversation, and put everything that varies after the cached prefix.

### Which lever for which problem

| The problem | The lever | What it costs you |
|---|---|---|
| Too expensive, quality has headroom | lower the effort, or a smaller tier | quality on hard cases; a cache restart if changed mid-conversation |
| Too slow to finish | lower the effort | the same |
| Slow to stream, a person is watching | fast mode (Opus only) | twice the price, API only |
| Answers are cut off | raise `max_tokens`, or lower the effort | more spend, or less depth |
| Reasoning too shallow | raise the effort | more output tokens |

## The practice: thinking, effort and speed set correctly per model

You write `build_params(model, max_tokens, options)`: it turns what an application wants into request parameters for one model, and
raises `RejectedRequest` for what the API would answer with a 400. The rules are the tables of this module: thinking modes per
model, effort levels and where they go, sampling parameters, fast mode and the budget limits. The statement is in
`exercises/19-thinking-effort-and-speed/unit-01/practice-1/statement.md`; each language folder has a `starter`, the tests and a build
file. The starter fails every test.

| Id | What it checks |
|---|---|
| `m1` | Each course model gets the request it accepts |
| `e1` | Thinking modes a model does not have are refused |
| `e2` | The mode that skips thinking up front is Sonnet only and needs high effort or below |
| `e3` | Effort needs a supporting model and a real level |
| `e4` | Newer models reject sampling parameters and Haiku keeps them |
| `e5` | Fast mode is Opus only, carries its beta header and is never sent in a batch |
| `e6` | A manual budget is at least 1,024 and below `max_tokens`; `max_tokens` is at least 1 |
| `e7` | Effort lives in `output_config`, never inside `thinking`, and the options are left unchanged |

Case `e2` catches the most natural mistake in a shared request builder: copying `between_tools` to a request that also asks for `xhigh`
effort. Each setting is valid alone, and the pair returns a 400.

## Traps

1. **Using fast mode where nobody waits.** It buys output speed at twice the price, and does nothing for a batch, a background job
   or a request that is slow for another reason.
2. **Toggling thinking inside a tool loop.** The API will not tell you: it silently turns thinking off for that request. Check
   the response for thinking blocks if the setting matters.
3. **Varying effort per turn in a cached conversation.** The change restarts the cache for the messages, and the saving you
   chose effort for is paid back as cache writes.

## Quiz

1. A chat feature on Opus 5.5 streams long answers to people watching the text appear, and the team wants it to arrive sooner without a change of tier or effort.
   Which option fits?
   - **a**: Fast mode, at the standard price, since the model itself is unchanged
   - **b**: Fast mode, at twice the standard price, on the first-party API
   - **c**: Fast mode, at twice the price, for a shorter wait before the first word
   - **d**: Fast mode, at twice the price, through Bedrock or Google Cloud

2. A tool loop runs four calls for one user question. The team leaves reasoning off for the first call and turns it on for
   the last three to handle a harder step. What happens?
   - **a**: The API treats each call as its own turn and accepts the new setting
   - **b**: The API returns a 400 for the first changed request and stops the loop
   - **c**: The API thinks on the later calls but leaves the thinking text out
   - **d**: The API serves the later rounds without thinking or any error

3. A team lowers the top-level effort for later turns of a long conversation whose first user turn holds a cached
   document. What follows?
   - **a**: The stored tools and system are invalidated, but the messages stay valid
   - **b**: The stored messages are invalidated, so the text is written again
   - **c**: The cache stays valid, because effort is applied after the prefix
   - **d**: The cache is invalidated only when the effort is raised above the default

<details>
<summary>Answer key</summary>

1. **b**. Long streamed answers gain from higher output speed, and fast mode on Opus 5.5 "is available on the Claude API (first-party) only", at twice the standard rate. *a* is ruled out because, though the model is the same, "On Opus 5.5 fast mode is $8 input and $40 output per million tokens", twice the standard rate. *c* is ruled out because "Speed benefits are focused on output tokens per second (OTPS), not time to first token (TTFT)." *d* is ruled out because "Bedrock and Google Cloud do not have it".
2. **d**. The four calls are one turn in one thinking mode, and a request that changes the mode in mid-turn loses its thinking: the API "silently disables thinking for that request". *b* is ruled out because "If a request changes the thinking mode in mid-turn, the API does not error". *c* is ruled out because the thinking is not hidden but switched off: "it silently turns thinking off for that request". *a* is ruled out because "A tool-use loop is one assistant turn."
3. **b**. A top-level effort change in mid-conversation "restarts the cache for the messages", so the saving "is paid back as cache writes". *a* is ruled out because the effort change "always discards the cached messages", while tools and system are model-specific. *c* is ruled out because the resolved configuration "is rendered into the prompt", so it is not applied after the prefix. *d* is ruled out because the rule covers any change, down as well as up: "Changing the thinking mode or the effort always discards" the cached messages.

</details>

## Module quiz

This quiz covers both pages of the module.

1. An agent on Sonnet 5.5 runs at `xhigh` effort, and a developer adds `between_tools` to stop up-front thinking. The request
   fails. What should the developer change?
   - **a**: Keep `xhigh` and send `disabled` to turn the up-front thinking off
   - **b**: Keep `between_tools` and drop to `high` or a lower level
   - **c**: Move the request to Opus 5.5, which takes the option at `xhigh`
   - **d**: Keep `xhigh` and add a manual budget beside `between_tools`

2. A helpdesk bot prints replies live for agents and also sends overnight jobs through the Batch API. Management wants the
   speed option on all of it. What should the team tell management?
   - **a**: It suits only the watched output, since the bulk path excludes it
   - **b**: It suits only the bulk path, where the output volume is the largest
   - **c**: It suits both, once the beta header is added to the bulk jobs
   - **d**: It suits neither, since the premium price outweighs the gain on any job

3. A shared helper serves three Claude models and must never emit a parameter that one of them rejects. What design does the
   practice use?
   - **a**: It derives each field from the chosen tier and refuses bad pairs
   - **b**: It sends one body to every tier and retries whenever the API objects
   - **c**: It picks the lowest common field and drops the rest silently
   - **d**: It passes each field from the caller through to the API unchanged

4. A feature on Opus 5.5 handles short lookup questions, yet some replies stop with `stop_reason: "max_tokens"`. The usage
   shows long thinking before replies that a sentence would settle. What should the team change?
   - **a**: Raise `max_tokens` so the thinking has room to finish before the answer
   - **b**: Send a manual `budget_tokens` that keeps the thinking below `max_tokens`
   - **c**: Lower the effort so the model reasons less before each brief answer
   - **d**: Drop `max_tokens` and let the effort level cap the length of each reply

<details>
<summary>Answer key</summary>

1. **b**. The mode is valid on Sonnet 5.5 only below the top two levels: "it works at low, medium and high effort and returns a 400 at xhigh and max". *a* is ruled out because "disabled and a manual budget return a 400 on the always-on models". *c* is ruled out because "Fable 5.1 and Opus 5.5 always think adaptively and cannot be told otherwise". *d* is ruled out because the manual budget belongs to the old kind of model: "Haiku 4.5 is the old kind: no thinking unless you ask, and then only the manual budget".
2. **a**. Live replies that agents watch are the case the page names, "Use it for interactive, output-heavy work where a person watches the text arrive", and the batch path cannot take the option. *b* is ruled out because "A batch is not waiting on anyone's screen, so there is nothing for it to speed up." *c* is ruled out because no header changes the batch rule: "Fast mode is not available with the Batch API." *d* is ruled out because the page weighs the price per use, telling the team to "measure whether the seconds saved are worth twice the price", not to rule it out for every job.
3. **a**. The practice "turns what an application wants into request parameters for one model" and raises an error for the invalid pairs. *b* is ruled out because the course's rule is "Build the thinking setting from the model, as the practice does", not one body and a retry. *c* is ruled out because the practice raises `RejectedRequest` "for what the API would answer with a 400" instead of dropping a field. *d* is ruled out because "The rules are the tables of this module", and a pass-through applies none of them.
4. **c**. The cut-off answers are simple ones that were over-thought, and the page's choice between the two remedies is to "lower the effort if they were over-thought". *b* is ruled out because "`disabled` and a manual budget return a 400 on the always-on models", Opus 5.5 among them. *a* is ruled out because the page keeps that remedy for answers that needed the depth: "raise `max_tokens` if the truncated answers needed the reasoning". *d* is ruled out because "Effort is a behavioral signal, not a strict token budget", and only `max_tokens` is a hard ceiling.

</details>
