# Practice: a cost model and a model router, graded on a cost table

Write two functions: one that prices a request from the `usage` object the API returns, and one that picks the
cheapest model that can take a task. Pick your language folder (`python`, `typescript`, `java` or `kotlin`), open
`starter/` and edit the file there. Everything is plain data: no network, no clock.

## The given data

A **model** is a map with `id`, `tier` (1 smallest to 4 largest; a course label, not an API field), `context` (tokens),
`max_output` (tokens), `input` and `output` (the price in dollars per million tokens, which is the same number as
micro-dollars per token), `cache_read_multiplier` (what a cache read costs as a multiple of `input`) and an optional
`deprecated` flag. The tests use the four course models with the prices read from the pricing page on 2026-10-02.

A **usage** is the object a response carries: `input_tokens`, `output_tokens`, `cache_read_input_tokens`,
`cache_creation_input_tokens` and, when present, `cache_creation` with `ephemeral_5m_input_tokens` and
`ephemeral_1h_input_tokens`. A missing field counts as zero.

A **task** is `usage` (the estimate to price), `min_tier` (default 1), `max_tokens` (the output cap the request will set,
default 0) and `batch` (default false). Java and Kotlin pass maps, Python dictionaries, TypeScript objects.

## `request_cost(model, usage, batch=False)` (TypeScript `requestCost`, Java `Router.requestCost`, Kotlin `requestCost`)

Return the cost in micro-dollars, rounded to 6 decimals:

1. Uncached input (`input_tokens`) costs `input` per token and output costs `output` per token.
2. A cache write of the 5-minute kind costs **1.25 times** `input`, and of the 1-hour kind **2 times** `input`. When
   `cache_creation` is present use its split; when only `cache_creation_input_tokens` is present, count it all as the
   5-minute kind.
3. A cache read costs `input` times the model's `cache_read_multiplier`.
4. The batch discount takes **half off every part** of the sum.

## `route(catalog, task)` (TypeScript `route`, Java `Router.route`, Kotlin `route`)

Return the `id` of the cheapest model that can take the task, or raise `NoModelError`:

1. Skip deprecated models and models below `min_tier`.
2. Skip a model whose `context` is smaller than the task's total input (`input_tokens` plus cache reads plus all cache
   writes), or whose `max_output` is smaller than `max_tokens`.
3. Price the usage on each remaining model with `request_cost` (with the task's `batch` flag) and take the lowest.
4. On equal cost take the lower tier, then the smaller id. The order of the catalog never matters.

## The cases

| Id | What it checks |
|---|---|
| `m1` | A plain request is priced from input and output, and the router returns the cheapest model at or above the tier |
| `e1` | Cache writes cost 1.25 and 2 times input, reads use the model's own multiplier, and an unsplit write is the 5-minute kind |
| `e2` | The batch discount halves every part of the cost, cache parts included |
| `e3` | The router picks by cost and tier, whatever the order of the catalog |
| `e4` | A model with too small a context window or output limit is skipped |
| `e5` | Deprecated models are skipped, and a choice with no model raises `NoModelError` |
| `e6` | A cost tie goes to the lower tier |

Run the tests with the command in the language folder's `run.sh` (Python, TypeScript) or its build file.
