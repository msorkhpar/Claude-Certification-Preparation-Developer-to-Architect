# Pinning model ids and migrating safely

**Level:** Developer · **Module 18:** Model choice, cost and migration · **Page 3 of 3**
**Exams:** DV2

**After this page you can** pin a model id, read the lifecycle states and retirement dates, list the request parameters a newer
model rejects, and plan a migration that changes one thing at a time and can be rolled back.

Checked against the Claude API documentation (Models overview, Model deprecations, Migrating to Claude Opus 5.5, Migrating to
Claude Sonnet 5.5, Effort) on 2026-10-02. Retirement dates are those of that day and move; the course re-reads them at release.

## Why it matters

A model is a dependency that is retired on a schedule, and the model that replaces it is not a drop-in. Newer models reject
settings the older ones accepted, so an application that swaps only the id can start returning 400 errors on every request.
The exam asks what to pin, how much notice you get, what breaks first and how to move traffic without an outage.

## The idea

### Pin the id, and know what a pinned id is

An id fixes the model that answers. The Models overview states:

> Every Claude model ID is a pinned snapshot, including the dateless IDs used from the 4.6 generation on.

Source: Models overview.

So `claude-sonnet-5-5` is as fixed as `claude-haiku-4-5-20251001`: the model behind it does not change under you. For models
before the 4.6 generation the dateless alias is "a convenience pointer that resolves to the dated ID". Put the id in one
configuration value, not in every call site, and record it with each evaluation result, so that a quality change can be traced
to a model change. On a cloud platform the id changes shape (module 22), so keep the mapping in the same place.

### The lifecycle

The Model deprecations page defines four states: **Active** (fully supported), **Legacy** (no longer receives updates and may be
deprecated), **Deprecated** ("The model is still functional but no longer recommended") and **Retired** ("The model is no longer
available for use. Requests to retired models will fail."). On 2026-10-02 the page listed Haiku 4.5 as Active with retirement
"not sooner than" October 15, 2026, and `claude-sonnet-4-5-20250929` as Deprecated, with retirement on November 30, 2026 and
`claude-sonnet-5-5` as the recommended replacement.

Three rules follow from the page:

- **Notice is at least 60 days.** The page says Anthropic is "providing at least 60 days' notice before model retirement for
  publicly released models". A retirement date is therefore a deadline you are told about, not a surprise.
- **Dates are per operator.** The dates apply to the Claude API, Claude Platform on AWS and Microsoft Foundry. "Partner-operated
  platforms (Amazon Bedrock and Google Cloud) set their own retirement schedules", so a model can live longer on a cloud platform.
- **You can find your own usage of a deprecated model.** In the Console, the Usage page exports a CSV, and "usage broken down by
  API key and model" shows which key still calls which model.

### What a newer model rejects

The largest migration risk is a request that the new model refuses. The Opus 5.5 migration guide lists what every request must
satisfy, and says an item that is "rejected" returns a 400 error. In summary:

- Thinking is adaptive and always on. Sending `thinking` of type `disabled`, or a manual `budget_tokens`, is rejected (module 19).
- Forcing a tool call with `tool_choice` of `any` or a named tool is rejected; use `auto` or `none`.
- The guide's rule for sampling is: "Omit temperature, top_p, and top_k, or leave them at their defaults: any other value is
  rejected."
- Prefill is rejected: "Don't end messages with a prefilled assistant turn: it is rejected."

The Sonnet 5.5 guide returns the same list of 400-error settings (thinking budgets, sampling parameters, assistant prefill, forced tool
choice). Claude Haiku 4.5 predates these rules and still accepts sampling parameters, which is why a router that sends one request
body to several models can work on one and fail on another. The practice of module 19 is a validator for exactly this.

Two quieter changes matter as much. A new tokenizer changes token counts (page 1), so budgets and `max_tokens` need re-checking.
And effort levels are recalibrated per model: the Effort page tells you to "Run an effort sweep on your own evals rather than
carrying settings over from an earlier model".

### A migration in steps

1. **Inventory.** Export usage by key and model and find every call site, including scripts and background jobs.
2. **Read the migration guide for the target** and remove what it says is rejected. Make the request builder per model, not
   one body for all.
3. **Evaluate on your own cases.** The deprecations page advises "thorough testing of your applications with the new models well
   before the retirement date". Compare quality, latency and cost per task, not per token, and run an effort sweep if the model
   thinks.
4. **Shift traffic gradually.** Send a small share to the new model, compare, raise the share. Keep the old id configurable so
   rollback is a configuration change. Watch the 400 rate first: it is the signal of a rejected parameter.
5. **Retire the old path** only after the old id has no traffic and its retirement date is not close.

## Traps

1. **Swapping the id only.** The new model may reject a setting the old one took, and every request then fails. Change the
   request builder together with the id.
2. **Treating the retirement date as the plan.** Migrating on the retirement date leaves no time to evaluate. The notice period
   exists so that testing happens before the deadline.
3. **Carrying effort and token budgets over.** Levels are recalibrated per model and the tokenizer changed. Re-measure instead
   of copying numbers.

## Quiz

1. A team changes one configuration value from an older id to Claude Opus 5.5 and every call starts failing with 400. Their
   call sets a manual thinking budget. Which fix does the page back?
   - **a**: Drop the settings that the new target rejects, and build per target
   - **b**: Raise `max_tokens`, since a budget needs more room under the new id
   - **c**: Add retries, since a 400 on a fresh deployment is often transient
   - **d**: Set thinking to type `disabled`, so that no budget is sent

2. A deprecated id is still called by a nightly job that nobody remembers. How can the team find it?
   - **a**: Export usage per API key and model, which names the key behind it
   - **b**: Wait for the retirement email, which names each calling application
   - **c**: Watch the 400 rate, which climbs each time that id is called
   - **d**: Check the alias list, which names every application that resolves it

3. A team calls one model on the Claude API and on Amazon Bedrock, and the model has a published retirement day. What
   happens to the Bedrock traffic?
   - **a**: Its date is the API date plus the 60-day notice period
   - **b**: It ends on that day too, since the id names the same model
   - **c**: It keeps a separate date, set by the partner that runs it
   - **d**: It turns into an alias of the replacement model on that day

<details>
<summary>Answer key</summary>

1. **a**. The page says an item that is "rejected" returns a 400 error and that the guide lists what to remove: a manual `budget_tokens` is rejected on Opus 5.5. *b* is ruled out because "Sending thinking of type disabled, or a manual budget_tokens, is rejected", whatever the room. *c* is ruled out because a rejected setting fails every time, and a rising 400 rate is "the signal of a rejected parameter". *d* is ruled out because on Opus 5.5 "Thinking is adaptive and always on", and a `disabled` type is rejected too.
2. **a**. The Usage page exports a CSV, and "usage broken down by API key and model" shows which key still calls which model. *b* is ruled out because the notice is about dates, "providing at least 60 days' notice before model retirement for publicly released models", and names no callers. *c* is ruled out because a deprecated model "is still functional but no longer recommended", so its calls succeed and raise no 400. *d* is ruled out because an alias is "a convenience pointer that resolves to the dated ID" and holds no list of callers.
3. **c**. The page says "Partner-operated platforms (Amazon Bedrock and Google Cloud) set their own retirement schedules". *b* is ruled out because the listed dates apply to "the Claude API, Claude Platform on AWS and Microsoft Foundry", and Bedrock is not among them. *a* is ruled out because the page's period is "at least 60 days' notice before model retirement", a warning ahead of a date and not time added after it. *d* is ruled out because the page names no aliasing on retirement: "Requests to retired models will fail."

</details>

## Module quiz

This quiz covers the three pages of the module.

1. A support product sends one request body, with `temperature` set to 0.2, to Haiku 4.5 and to Sonnet 5.5. Haiku answers
   and Sonnet returns a 400. What explains the difference?
   - **a**: The newer model has been retired, so every call to it fails
   - **b**: The newer model refuses sampling values the older one accepts
   - **c**: The newer id is an alias that resolves to an older snapshot
   - **d**: The body overflows the newer model's window but fits the older

2. A nightly bulk job and a chat feature both use a mid-size model. Finance asks which figure to track per feature. What
   does the module support?
   - **a**: Tokens counted before sending, summed at the end of each month
   - **b**: Price per million tokens from the list, taken from the pricing page
   - **c**: Cost per request, priced from usage with the multipliers applied
   - **d**: Output tokens only, since they carry the highest unit price

3. A router moves a long conversation to a different target halfway through, and answer quality drops. Which
   cause does the module name?
   - **a**: The old model's cache stays behind, so the new one loses the history
   - **b**: The new model has a different alias that resolves to an old snapshot
   - **c**: The token counting endpoint rejected the longer conversation
   - **d**: The new model sees the past turns but not the thinking behind them

4. A plan to move 20 percent of traffic to a replacement model on Monday and the rest at the end of the week is proposed. What
   should the team add before Monday?
   - **a**: A second evaluation on the vendor's published benchmark suite
   - **b**: An evaluation on its own cases, run ahead of the shift
   - **c**: The old model's effort level, carried over so behaviour matches
   - **d**: A larger `max_tokens` on every request, to leave room for thinking

<details>
<summary>Answer key</summary>

1. **b**. The Sonnet 5.5 guide "returns the same list of 400-error settings", sampling parameters among them, while "Claude Haiku 4.5 predates these rules and still accepts sampling parameters". *a* is ruled out because Sonnet 5.5 is listed "as the recommended replacement" for a deprecated model, not as a retired one. *c* is ruled out because "Every Claude model ID is a pinned snapshot, including the dateless IDs used from the 4.6 generation on." *d* is ruled out because Haiku 4.5 has the smaller window, as "a 300,000-token input rules Haiku 4.5 out whatever its price", so a body that fits Haiku also fits Sonnet 5.5.
2. **c**. The page gives the cost per request as a sum over the usage fields and says "These multipliers stack with other pricing modifiers such as the Batch API discount and data residency." *b* is ruled out because the tokenizer difference makes "the price per token a poor proxy for the price per task". *a* is ruled out because "The token count is an estimate" of input only, and output and cache split appear only in usage. *d* is ruled out because a usage has "four input-side fields at three prices", so output alone is incomplete.
3. **d**. The page quotes "A router or fallback that moves a conversation from Claude Opus 5.5 to any other model runs those turns without them." *b* is ruled out because "Every Claude model ID is a pinned snapshot" and a dateless id is its own snapshot. *c* is ruled out because the endpoint rejects inputs such as "server tools like web search", not long conversations. *a* is ruled out because a cache changes only the price of input, and "the total input is the sum of the three input fields" whether read from a cache or not.
4. **b**. The page says to evaluate "on your own cases" and warns that migrating on the retirement date "leaves no time to evaluate". *a* is ruled out because the page's step is an evaluation "on your own cases", not a published suite. *c* is ruled out because the Effort page says to "Run an effort sweep on your own evals rather than carrying settings over from an earlier model". *d* is ruled out because the page says "Re-measure instead of copying numbers" and gives no blanket increase.

</details>
