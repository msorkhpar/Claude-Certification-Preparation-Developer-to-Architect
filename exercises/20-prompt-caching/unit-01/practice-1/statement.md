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

## What is already written, and what you write

The starter is a working planner with six gaps cut out of it. Everything that is plumbing is written and correct: the constants, `PlanError`, the walk over the
blocks with the running token total, the building of the plan entries and the collecting of the breakpoints. Each gap is a small function with its signature, a comment that
says what it receives and returns, one example, and the cases it unlocks. A gap returns a neutral value, so the starter runs and fails the cases on an assertion. Python
names them with a leading underscore; TypeScript, Java and Kotlin use the same names without it, in camel case (`checkTools`, `wantsBreakpoint`). Write them in this order:

1. `_check_tools` unlocks `e5`: a volatile tool definition is refused.
2. `_ordered` unlocks `m1`, `e1` and `e6`: the prefix order, the volatile blocks last, and a new list.
3. `_wants_breakpoint` unlocks `e2` and `e5`: the prefix must reach the minimum, and a volatile block never carries a breakpoint.
4. `_marker` unlocks `m1`: the lifetime a breakpoint carries, `5m` by default.
5. `_check_count` unlocks `e3`: at most four breakpoints.
6. `_check_lifetimes` unlocks `e4`: a `1h` breakpoint never follows a `5m` one.

About a dozen lines in all. To see what a gap receives, log its input with the `log` line at the top of the file: `plan_request` already logs its input at debug level, and a
run shows the logged lines under the failing case.

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
