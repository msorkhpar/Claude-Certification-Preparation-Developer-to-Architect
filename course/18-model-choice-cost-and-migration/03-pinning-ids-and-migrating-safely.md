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
   call sets `budget_tokens` under `thinking`. Which fix does the page back?
   - **a**: Remove that field from the body, since the new model refuses it
   - **b**: Raise `max_tokens`, since a budget needs more room under the new id
   - **c**: Add retries, since a 400 on a fresh deployment is often transient
   - **d**: Set thinking to type `disabled`, so that no budget is sent

2. A deprecated id is still called by a nightly job that nobody remembers. How can the team find it?
   - **a**: Pull the Console usage export, which ties that id to its calling key
   - **b**: Wait for the retirement email, which names each calling application
   - **c**: Watch the 400 rate, which climbs each time that id is called
   - **d**: Look the id up on the Model deprecations page, which lists its callers

3. A team calls one model on the Claude API and on Amazon Bedrock, and the model has a published retirement day. What
   happens to the Bedrock traffic?
   - **a**: Its date is the API date plus the 60-day notice period
   - **b**: It ends on that day too, since the id names the same model
   - **c**: It keeps a separate date, set by the partner that runs it
   - **d**: It turns into an alias of the replacement model on that day

<details>
<summary>Answer key</summary>

1. **a**. The page says an item that is "rejected" returns a 400 error, and a manual `budget_tokens` is on the Opus 5.5 list, so removing it ends the failures. *b* is ruled out because "Sending thinking of type disabled, or a manual budget_tokens, is rejected", whatever the room. *c* is ruled out because a rejected setting fails every time, and a rising 400 rate is "the signal of a rejected parameter". *d* is ruled out because on Opus 5.5 "Thinking is adaptive and always on", and a `disabled` type is rejected too.
2. **a**. The Usage page exports a CSV, and "usage broken down by API key and model" shows which key still calls which model. *b* is ruled out because the notice is about dates, "providing at least 60 days' notice before model retirement for publicly released models", and names no callers. *c* is ruled out because a deprecated model "is still functional but no longer recommended", so its calls succeed and raise no 400. *d* is ruled out because "The Model deprecations page defines four states" and gives each model's dates, which say nothing about who calls it.
3. **c**. The page says "Partner-operated platforms (Amazon Bedrock and Google Cloud) set their own retirement schedules". *b* is ruled out because the listed dates apply to "the Claude API, Claude Platform on AWS and Microsoft Foundry", and Bedrock is not among them. *a* is ruled out because the page's period is "at least 60 days' notice before model retirement", a warning ahead of a date and not time added after it. *d* is ruled out because the page names no aliasing on retirement: "Requests to retired models will fail."

</details>

## Module quiz

This quiz covers the three pages of the module.

1. A support router builds one request body, with `temperature` set to 0.2, and sends tickets under 150,000 tokens to Haiku
   4.5 and larger ones, up to 400,000, to Sonnet 5.5. Only the larger tickets fail, each with a 400. Which change does the
   module back?
   - **a**: Send the larger tickets to Haiku 4.5 too, since it takes the shared body
   - **b**: Strip `temperature` from the newer model's calls, since it rejects the value
   - **c**: Pin Sonnet 5.5 by a dated id, since its dateless id may have moved
   - **d**: Route the larger tickets to Opus 5.5, whose higher tier takes more settings

2. A nightly bulk job and a chat feature both use a mid-size model. Finance asks which figure to track per feature. What
   does the module support?
   - **a**: Tokens counted before sending, summed at the end of each month
   - **b**: Price per million tokens from the list, taken from the pricing page
   - **c**: Cost per request, priced from usage with the multipliers applied
   - **d**: Output tokens only, since they carry the highest unit price

3. A router moves a long agent conversation from Opus 5.5 to Sonnet 5.5 halfway through, and answer quality drops. What
   does Sonnet 5.5 work from for the earlier turns?
   - **a**: A shortened history, since the old model's cache cannot move across
   - **b**: The full turns and reasoning, read through an older snapshot of itself
   - **c**: The latest turn alone, since the counting endpoint trims long input
   - **d**: The previous messages, with none of the thinking blocks they carried

4. A plan to move 20 percent of traffic to a replacement model on Monday and the rest at the end of the week is proposed. What
   should the team add before Monday?
   - **a**: The vendor's published benchmark scores, compared across both models
   - **b**: An evaluation on its own cases of quality, latency and task cost
   - **c**: The old model's effort level, carried over so behaviour matches
   - **d**: A cost budget per feature, set from the old model's token counts

<details>
<summary>Answer key</summary>

1. **b**. The Sonnet 5.5 guide "returns the same list of 400-error settings", sampling parameters among them, while "Claude Haiku 4.5 predates these rules and still accepts sampling parameters", so only the body sent to Sonnet 5.5 must lose the value. *a* is ruled out because "a 300,000-token input rules Haiku 4.5 out whatever its price", and the larger tickets reach 400,000 tokens. *c* is ruled out because "Every Claude model ID is a pinned snapshot, including the dateless IDs used from the 4.6 generation on." *d* is ruled out because the Opus 5.5 guide sets the same rule: "Omit temperature, top_p, and top_k, or leave them at their defaults: any other value is rejected."
2. **c**. The page gives the cost per request as a sum over the usage fields and says "These multipliers stack with other pricing modifiers such as the Batch API discount and data residency." *b* is ruled out because the tokenizer difference makes "the price per token a poor proxy for the price per task". *a* is ruled out because "The token count is an estimate" of input only, and output and cache split appear only in usage. *d* is ruled out because a usage has "four input-side fields at three prices", so output alone is incomplete.
3. **d**. Thinking blocks are tied to the model that wrote them, and the page quotes "A router or fallback that moves a conversation from Claude Opus 5.5 to any other model runs those turns without them." *b* is ruled out because "Every Claude model ID is a pinned snapshot" and a dateless id is its own snapshot. *c* is ruled out because the endpoint only counts and rejects inputs such as "server tools like web search", and trims nothing. *a* is ruled out because a cache changes only the price of input, and "the total input is the sum of the three input fields" whether read from a cache or not.
4. **b**. The page's step is to evaluate "on your own cases" and to "Compare quality, latency and cost per task, not per token", and it warns that migrating on the retirement date "leaves no time to evaluate". *a* is ruled out because, for whether a model can do the task, "only your own evaluation answers" it, not a published suite. *c* is ruled out because the Effort page says to "Run an effort sweep on your own evals rather than carrying settings over from an earlier model". *d* is ruled out because "A new tokenizer changes token counts", so the page says "Re-measure instead of copying numbers".

</details>
