# Prices, lifetimes, the lookback and what breaks a cache

**Level:** Developer · **Module 20:** Prompt caching · **Page 2 of 2**
**Exams:** DV2

**After this page you can** work out whether caching saves money for a given traffic pattern, choose between the five-minute and
one-hour lifetime, keep a growing conversation inside the lookback window, list the changes that invalidate a cache, and write
the planner that orders a request and places its breakpoints.

Checked against the Claude API documentation (Prompt caching, Pricing) on 2026-10-02, and by running the practice offline in
the course container in Python, TypeScript, Java and Kotlin.

## Why it matters

Caching is a trade. A write costs more than ordinary input, a read costs much less, and an entry lasts only a short while. Whether
it pays depends on how often the prefix is reused and how long the gaps are. The exam asks for the arithmetic, for the lifetime
that fits a traffic pattern, and for the change that quietly empties a cache.

## The idea

### The price of a write and a read

The documentation states the multipliers that sit behind its price table:

> 5-minute cache write tokens are 1.25 times the base input tokens price

Source: Prompt caching, Pricing.

The two other multipliers are: "1-hour cache write tokens are 2 times the base input tokens price" and "Cache read tokens are 0.1 times
the base input tokens price". Reads on two models are cheaper still: 0.05 times on Claude Opus 5.5 and 0.025 times on Claude Fable
5.1. The multipliers stack with other modifiers such as the Batch API discount. For Claude Sonnet 5.5, whose base input price is $2
per million tokens, the table reads $2.50 for a five-minute write, $4 for a one-hour write and $0.20 for a read.

From the multipliers you can compute the break-even, in units of the base price for one prefix. With the five-minute lifetime, n
requests that share the prefix cost 1.25 + 0.1 (n - 1) against n without caching, so two requests already cost 1.35 against 2. With the
one-hour lifetime they cost 2 + 0.1 (n - 1): two requests cost 2.1 against 2, and three cost 2.2 against 3. A one-hour entry needs a
third request to pay for itself, and a five-minute entry needs only a second. That arithmetic is the course's own, derived from the
multipliers above. It ignores the output price, which caching does not change: "Prompt caching has no effect on output token
generation."

### How long an entry lives

"By default, the cache has a 5-minute lifetime. The cache is refreshed for no additional cost each time the cached content is used." So
a prompt used more often than every five minutes keeps its entry alive for free and never needs the dearer lifetime. Two details
matter in practice. The clock runs from the start of the request: "The lifetime is measured from the start of the request that writes or
reads the cache entry, not from the end of its response." A four-minute stream leaves about a minute for the next request. And a
cache entry "only becomes available after the first response begins", so parallel requests that must share a cache follow the page's
advice: "If you need cache hits for parallel requests, wait for the first response before sending subsequent requests."

The one-hour lifetime is requested with `{"type": "ephemeral", "ttl": "1h"}`. The page recommends it when prompts are used "less
frequently than 5 minutes, but more frequently than every hour", for example a chat where the user may not reply within five minutes.
It also notes that cache hits "are not deducted against your rate limit". "You can use both 1-hour and 5-minute cache controls in the same request", with one rule:
"Cache entries with longer TTL must appear before shorter TTLs". The usage object then splits the writes in
`cache_creation.ephemeral_5m_input_tokens` and `cache_creation.ephemeral_1h_input_tokens`, which sum to `cache_creation_input_tokens`.

### The lookback window

On each request the system checks the hash at the breakpoint, and if nothing matches it walks backward one block at a time looking
for an entry an earlier request wrote. It checks at most 20 positions, the breakpoint included. A growing conversation that adds
more than 20 blocks between two requests walks past its last write and misses. The remedy is a second breakpoint near the old
position: "You can define up to 4 cache breakpoints". On the Claude API a run of consecutive `tool_use` blocks counts as one position, and so
does a run of consecutive `tool_result` blocks.

### What empties a cache

"Changes at each level invalidate that level and all subsequent levels" of the order tools, system, messages. The rows to know:

| Change | Tools | System | Messages |
|---|---|---|---|
| Tool definitions | ✘ | ✘ | ✘ |
| Web search or citations toggle | ✓ | ✘ | ✘ |
| Speed setting | ✓ | ✘ | ✘ |
| `tool_choice` | ✓ | ✓ | ✘ |
| Images added or removed | ✓ | ✓ | ✘ |
| Thinking parameters, effort setting | model-specific | model-specific | ✘ |

A ✘ is invalidated and a ✓ stays valid. The page words two of the rows this way: "Adding/removing images anywhere in the prompt affects message blocks" and "Enabling/disabling web search modifies the system prompt". The last row ties back to module 19: choose the thinking mode and effort once for a
conversation. Two other facts from the page belong beside the table. Caches are isolated per workspace on the Claude API, Claude
Platform on AWS and Microsoft Foundry, while Bedrock and Google Cloud isolate per organization only, so a team that splits traffic across workspaces
splits its cache too. And tool definitions serialized with unstable key order, as some languages do, break the match: keep the order stable.

### Pre-warming

To load a cache before the first real request, send the same prefix with `max_tokens` of 0. The API writes the cache at the
breakpoint and returns at once with an empty `content` array and "a fully populated `usage` block". The breakpoint must sit on the last
block the follow-up shares, and the thinking and effort settings must match the follow-up, or the entry is never hit. It is rejected
in a batch: pre-warming "targets time-to-first-token, which does not apply to batch processing".

## The practice: order a request for cache hits

You write `plan_request(blocks, min_tokens)`. Given blocks tagged `tools`, `system` or `messages`, with a token count and optional
`volatile`, `breakpoint` and `ttl` fields, it returns the blocks in prefix order, with the volatile ones last, and the `cache` value
each block should carry. It raises `PlanError` where the API would answer 400. The statement is in
`exercises/20-prompt-caching/unit-01/practice-1/statement.md`; each language folder has a `starter`, the tests and a build file, and
the starter fails every test.

| Id | What it checks |
|---|---|
| `m1` | Stable content comes first, the volatile date goes last, and the breakpoints sit on the stable blocks |
| `e1` | Sections follow the prefix order and keep their own order |
| `e2` | A breakpoint needs the stable prefix up to it to reach the minimum |
| `e3` | At most four breakpoints are sent, counting only those that reach the minimum |
| `e4` | A one-hour breakpoint may not follow a five-minute one |
| `e5` | A volatile block never carries a breakpoint, and tools cannot be volatile |
| `e6` | Every block comes out once and the input is not changed |

In the planner, a volatile block moves to the very end of the request, after the last breakpoint, so the marker never sits on content that changes. Case `e2` is the quiet one: a short prefix returns no error from the real API, so the planner returns `null` for it rather than
raising, and a test that expects an exception there fails.

## Traps

1. **Choosing the one-hour lifetime by default.** It writes at twice the base price and needs a third request to pay back. Traffic
   more often than every five minutes keeps the cheaper entry alive for free.
2. **Firing parallel requests at a cold cache.** An entry only exists after the first response begins, so ten parallel requests
   write ten times. Send one, wait for the response to begin, then fan out.
3. **Changing `tool_choice` or adding an image mid-conversation.** Either empties the messages cache. Keep both fixed for the
   life of the conversation, or accept the write.

## Quiz

1. A support bot reuses a 20,000-token prefix once every 40 minutes, around the clock. Which choice does the page's guidance
   favour?
   - **a**: The default TTL, since each reuse refreshes the entry free of charge and keeps it alive
   - **b**: The one-hour TTL, since the gap is past the default but within the hour
   - **c**: Neither TTL, since an entry survives through frequent use alone
   - **d**: Both TTLs together, since each request has to carry the pair of settings

2. A conversation adds 25 blocks between two requests, with one breakpoint on the final block. The second request misses the
   earlier write. What fixes it?
   - **a**: A label on the request, so the older entry is found by name
   - **b**: A longer TTL on the last marker, so the older entry survives until the next request
   - **c**: A larger `max_tokens`, so the search can reach further back through the blocks
   - **d**: A further marker near the older spot, so a fresh entry builds up there

3. A team adds an image to the fifth message of a long conversation. Which parts of the cache are invalidated?
   - **a**: Only the tool list, while the instructions and the dialogue stay intact
   - **b**: Only the dialogue blocks; the tool list and instructions stay intact
   - **c**: The instructions and the dialogue, while the tool list stays intact
   - **d**: Nothing, since pictures sit outside the cached text

<details>
<summary>Answer key</summary>

1. **b**. The page recommends the one-hour TTL for prompts used "less frequently than 5 minutes, but more frequently than every hour". *a* is ruled out because the entry is "refreshed for no additional cost each time the cached content is used", and a 40-minute gap lets the default entry lapse first. *c* is ruled out because the one-hour TTL exists for "less frequently than 5 minutes, but more frequently than every hour". *d* is ruled out because "You can use both 1-hour and 5-minute cache controls in the same request" is allowed, not required.
2. **d**. The page says a second breakpoint near the older position starts a second lookback window there, since "You can define up to 4 cache breakpoints". *b* is ruled out because the miss comes from the window: "It checks at most 20 positions", and a longer TTL does not widen it. *c* is ruled out because the search "walks backward one block at a time", and `max_tokens` governs the reply. *a* is ruled out because the search is "looking for an entry an earlier request wrote", by hash, not by a label.
3. **b**. The page says "Adding/removing images anywhere in the prompt affects message blocks", and the table shows ✓ for tools and system. *a* is ruled out because "Changes at each level invalidate that level and all subsequent levels", and tools come first. *c* is ruled out because that pattern belongs to "Enabling/disabling web search modifies the system prompt", not to images. *d* is ruled out because "Adding/removing images anywhere in the prompt affects message blocks".

</details>

## Module quiz

This quiz covers both pages of the module.

1. A service fires ten calls together, all sharing a 30,000-token prefix, against a cold cache. What does the module predict?
   - **a**: One writes and nine read, since all ten share the same hash and the same prefix
   - **b**: Each one writes, as the entry appears only once a first reply starts
   - **c**: All read, since the entry is created the moment a request arrives
   - **d**: One writes and nine fail, since an entry allows a single writer

2. A prefix is sent twice within five minutes on a model that reads at 0.1 times the base price. How does the spend on the pair
   compare with two ordinary sends?
   - **a**: Roughly two thirds as much
   - **b**: Roughly five eighths as much
   - **c**: A few percent more
   - **d**: A little over half as much

3. A request marks the system block with a five-minute breakpoint and the final message with a one-hour breakpoint. What does
   the module say?
   - **a**: It is refused, since the longer entry has to come first
   - **b**: It is accepted, with both entries billed at one common rate
   - **c**: It is accepted, with only the short entry written
   - **d**: It is refused, since one request carries a single TTL

4. A 600-token system prompt is marked on a model whose minimum is 512, and usage shows a write on every call and no reads. The
   prompt begins with the clock reading. What should the team do?
   - **a**: Pad the prompt so it clears the floor by a wider margin and stays well above it
   - **b**: Raise the TTL to an hour so the entry survives between uses and keeps being read
   - **c**: Shift that line to the end and set the boundary where unchanging text stops
   - **d**: Switch to a larger number of explicit markers so more segments are cached separately

<details>
<summary>Answer key</summary>

1. **b**. The page says a cache entry "only becomes available after the first response begins", so all ten write. *a* is ruled out because the entry "only becomes available after the first response begins", so the others have nothing to read. *c* is ruled out because the same sentence says it is available only "after the first response begins". *d* is ruled out because the advice is "wait for the first response before sending subsequent requests", not that extra requests fail.
2. **a**. A five-minute write at 1.25 plus a read at 0.1 gives 1.35 against 2, about two thirds. *b* is ruled out because it takes the write alone, and "Cache read tokens are 0.1 times the base input tokens price" applies to the second request. *c* is ruled out because that figure is the one-hour case, from "1-hour cache write tokens are 2 times the base input tokens price". *d* is ruled out because it drops the write premium: "5-minute cache write tokens are 1.25 times the base input tokens price".
3. **a**. The page says "Cache entries with longer TTL must appear before shorter TTLs". *b* is ruled out because the writes are billed differently: "1-hour cache write tokens are 2 times the base input tokens price". *c* is ruled out because "The usage object then splits the writes" by lifetime, so both can be written. *d* is ruled out because "You can use both 1-hour and 5-minute cache controls in the same request" when ordered correctly.
4. **c**. The planner puts "a volatile block moves to the very end of the request", after the last breakpoint. *b* is ruled out because "Changes at each level invalidate that level and all subsequent levels", whatever the TTL. *a* is ruled out because the prompt already clears 512 tokens and usage shows writes, and the cause is "a volatile block moves to the very end" in the plan. *d* is ruled out because "You can define up to 4 cache breakpoints", yet an earlier changing block still changes the hash.

</details>
