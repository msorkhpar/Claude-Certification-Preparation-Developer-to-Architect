# Practice: order a request for cache hits

A prompt cache stores a **prefix** of the request: the tools first, then the system prompt, then the messages, up to a
breakpoint. Anything that changes inside the prefix, a timestamp for instance, starts the cache over. Write the function
that puts a request's blocks in the order that keeps the prefix stable and places the breakpoints the cache needs. Pick
your language folder (`python`, `typescript`, `java` or `kotlin`), open `starter/` and edit the file there. The rules come
from the Claude documentation on prompt caching, read on 2026-10-02; the lesson pages explain them.

## The given data

A **block** is a map with `id`, `section` (`tools`, `system` or `messages`), `tokens`, and optional `volatile` (true when
the content changes on every request, such as the current time), `breakpoint` (true when the author wants the cache to end
here) and `ttl` (`5m`, the default, or `1h`). The function returns a list, one entry per block, as maps
`{"id": ..., "cache": null | "5m" | "1h"}`; a `cache` value stands for a `cache_control` marker on that block.

## `plan_request(blocks, min_tokens=1024)` (TypeScript `planRequest`, Java `CachePlan.planRequest`, Kotlin `planRequest`)

1. **Order.** The sections come in the order `tools`, `system`, `messages`; blocks of one section keep the order they
   were given in. Then every volatile block moves to the very end, in its given order, so that nothing volatile sits inside
   the prefix. A volatile block in `tools` cannot move there: raise `PlanError`.
2. **Breakpoints.** A block with `breakpoint` gets its `ttl` as `cache` only if it is not volatile and the stable tokens
   from the start of the request up to and including that block add up to at least `min_tokens`. A block that does not
   reach the minimum gets `null` (the API would ignore the marker and return no error, so there is nothing to raise).
3. **At most four** breakpoints may remain after rule 2; more is a `PlanError` (the API answers 400).
4. **Lifetimes.** A `1h` breakpoint must come before every `5m` breakpoint; the other order is a `PlanError`.
5. Every input block appears once in the result, and the input list and its maps are left unchanged.

## The cases

| Id | What it checks |
|---|---|
| `m1` | Stable content comes first, the volatile date goes last, and the breakpoints sit on the stable blocks |
| `e1` | Sections follow the prefix order and keep their own order |
| `e2` | A breakpoint needs the stable prefix up to it to reach the minimum |
| `e3` | At most four breakpoints are sent, counting only those that reach the minimum |
| `e4` | A one-hour breakpoint may not follow a five-minute one |
| `e5` | A volatile block never carries a breakpoint, and tools cannot be volatile |
| `e6` | Every block comes out once and the input is not changed |

Run the tests with the command in the language folder's `run.sh` (Python, TypeScript) or its build file.
