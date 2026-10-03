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
  Platform on AWS or partner-operated cloud platforms". Bedrock and Vertex do not have it (module 22).
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
thinking supports forced tool use, except on Claude Opus 5.5, Claude Sonnet 5.5, Claude Fable 5.1 and Claude Mythos 5.1. On
those models, use `auto` and get a structured result another way.

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
the prompt". Setting a parameter explicitly to its default is the same as omitting it, so it does not break the cache. Cached
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

1. A chat feature on Opus 5.5 streams long answers to people watching the text appear, and the team wants it to arrive sooner.
   Which option does the page support?
   - **a**: Fast mode, at the standard price and on every cloud platform
   - **b**: Fast mode, at twice the standard price and on the first-party API only
   - **c**: A lower effort level, with a reduced price and shorter replies
   - **d**: A batch, at half the price and with the replies delivered sooner

2. A tool loop runs four calls for one user question. The team sets reasoning on for the first call and off for the last
   three to save tokens. What happens?
   - **a**: The API treats each call as its own turn and accepts the new setting
   - **b**: The API returns a 400 for the first changed request and stops the loop
   - **c**: The API keeps the first setting for the whole turn and ignores the rest
   - **d**: The API silently disables thinking for each altered step and raises no error

3. A team lowers the effort for later turns of a long conversation that relies on a cached document. What follows?
   - **a**: The stored tools and system are invalidated, but the messages stay valid
   - **b**: The stored messages are invalidated, so the text is written again
   - **c**: The cache stays valid, because effort is applied after the prefix
   - **d**: The cache is invalidated only when the effort is raised above the default

<details>
<summary>Answer key</summary>

1. **b**. The page says fast mode "is available on the Claude API (first-party) only" and prices it at twice the standard rate. *a* is ruled out because "it is not available on Claude Platform on AWS or partner-operated cloud platforms". *c* is ruled out because the table gives "fast mode (Opus only)" to streaming, and gives lowering the effort to "Too slow to finish". *d* is ruled out because "Fast mode is not available with the Batch API."
2. **d**. The page says the API does not error: it "silently disables thinking for that request". *b* is ruled out by that same sentence, since the stated behavior is "silently disables thinking for that request" and not a 400. *c* is ruled out because the changed request is the one that loses thinking: "silently disables thinking for that request". *a* is ruled out because "A tool-use loop is one assistant turn."
3. **b**. The page says changing the thinking mode or the effort "always discards the cached messages". *a* is ruled out because the messages are the part that "always discards the cached messages" names, while tools and system are model-specific. *c* is ruled out because the resolved configuration "is rendered into the prompt", so it is not applied after the prefix. *d* is ruled out because the messages are discarded on any change: "always discards the cached messages".

</details>

## Module quiz

This quiz covers both pages of the module.

1. An agent on Sonnet 5.5 runs at `xhigh` effort, and a developer adds `between_tools` to stop up-front thinking. The request
   fails. Which statement matches the module?
   - **a**: The option is valid on Sonnet 5.5 at any effort level
   - **b**: Each option is valid alone, yet the combination is refused by the API
   - **c**: The option exists only on Opus models, so any use fails
   - **d**: The option works only if a manual budget is also sent

2. A helpdesk bot prints replies live for agents and also runs overnight bulk jobs. Management wants the speed option on all of
   it. What does the module say?
   - **a**: It suits only the watched output, as the batch route excludes it
   - **b**: It suits neither, since it only lowers time to first token
   - **c**: It suits neither, because only the cloud platforms offer it
   - **d**: It suits both, at the standard price for the bulk jobs

3. A shared helper serves three Claude models and must never emit a parameter that one of them rejects. What design does the
   practice use?
   - **a**: It derives each field from the chosen tier and refuses bad pairs
   - **b**: It sends one body to every tier and retries whenever the API objects
   - **c**: It picks the lowest common field and drops the rest silently
   - **d**: It reads each field from the reply to an earlier call

4. A cost-conscious team wants cheaper answers on Opus 5.5 without losing the cache of a long conversation. Which plan fits?
   - **a**: Switch to fast mode for the cached turns and standard for the rest
   - **b**: Lower the effort on every second turn and raise it again afterward
   - **c**: Choose the effort level once, before the first turn, and keep it
   - **d**: Disable thinking after the first answer to cut the output

<details>
<summary>Answer key</summary>

1. **b**. The page says "Each setting is valid alone, and the pair returns a 400". *a* is ruled out because the mode "needs high effort or below". *c* is ruled out because the table says "skips thinking up front is Sonnet only". *d* is ruled out because the only budget rule is "A manual budget is at least 1,024", which has no link to this mode.
2. **a**. The page says "Fast mode is not available with the Batch API." and the speed gain is for output people watch. *b* is ruled out because "Speed benefits are focused on output tokens per second (OTPS), not time to first token (TTFT)." *c* is ruled out because fast mode "is available on the Claude API (first-party) only". *d* is ruled out because fast mode carries a premium and "Fast mode is not available with the Batch API."
3. **a**. The practice "turns what an application wants into request parameters for one model" and raises an error for the invalid pairs. *b* is ruled out because the same sentence says it builds "request parameters for one model", not one shared body. *c* is ruled out because the page warns the API "silently disables thinking for that request", which a silent drop would hide. *d* is ruled out because "The rules are the tables of this module", not a reply.
4. **c**. The page says to choose once per conversation and put what varies after the cached prefix. *b* is ruled out because "always discards the cached messages". *a* is ruled out because the table row "Speed setting (fast or standard)" shows system and messages invalidated. *d* is ruled out because "Changing the thinking mode or the effort" is on the invalidation list.

</details>
