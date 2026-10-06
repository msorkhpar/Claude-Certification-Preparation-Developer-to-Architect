# Practice: bounded concurrency with backpressure

Write the worker pool that every batch job needs: run work over many items, never more than `limit` at a time,
without reading the whole input first.

## The function

**`map_bounded(items, work, limit)`** (TypeScript `mapBounded`, Java `Bounded.mapBounded`, Kotlin `mapBounded`)

| Language | Shape |
|---|---|
| Python | `async def map_bounded(items, work, limit)`: `items` is any iterable, `work(item)` is a coroutine function |
| TypeScript | `async function mapBounded(items, work, limit)`: `items` is an `Iterable`, `work(item)` returns a promise |
| Java | `static List<Outcome<R>> mapBounded(Iterator<T> items, Function<T, R> work, int limit)`: blocking, one worker thread per slot |
| Kotlin | `fun mapBounded(items: Iterator<T>, work: (T) -> R, limit: Int): List<Outcome<R>>`: blocking, one worker thread per slot |

It returns one **`Outcome`** per item, **in input order**: `ok` with a `value`, or not `ok` with the `error` that
`work` raised. `Outcome` is given in the starter.

## What is already written, and what you write

The starter is the working pool with six small gaps cut out of it: the worker loop, the `Outcome` type, the lazy source and the final join are written and correct. Each gap
is one small function with its signature, a comment that says what it receives and returns with one example, and the cases it unlocks. A gap returns a neutral value, so
the starter runs and fails every case on an assertion. To see what a gap receives, log its input with the `log` line at the top of the file (`log.debug(...)`); a run shows
the lines under the failing case. The names below are Python's; TypeScript, Java and Kotlin use the camel-case forms (`checkLimit`, `nextSlot`, `succeeded`, `failed`, `place`, `pool`).

1. `_check_limit` unlocks `e4`: refuse a limit below 1.
2. `_next_slot` unlocks `e5` and `m1`: take one item and reserve its slot, or report that the input is exhausted.
3. `_succeeded` unlocks `m1`, `e1` and `e3`: the outcome of an item whose work returned.
4. `_failed` unlocks `e2`: the outcome of an item whose work raised.
5. `_place` unlocks `e1`: store an outcome in the slot of its item, so the list keeps the input order.
6. `_pool` unlocks `m1` and `e5`: the `limit` workers that run side by side.

About ten lines in all.

## Rules

1. **Bounded.** No more than `limit` calls of `work` are running at any moment, and when there are enough items,
   `limit` of them are.
2. **Ordered.** The outcomes are in the order of the input, even when later items finish first.
3. **Isolated.** A failing item is reported in its own outcome; the other items still run and finish.
4. **Edges.** A limit larger than the number of items works; an empty input gives an empty list; a limit below 1 is
   refused (`ValueError`, TypeScript `Error`, Java and Kotlin `IllegalArgumentException`).
5. **Backpressure.** Items are taken from the input **one at a time, only when a worker is free**. While every
   worker is busy, at most `limit` items have been taken: a slow consumer holds the producer back, so a million-line
   file never sits in memory.

## The cases

| Id | What it checks |
|---|---|
| `m1` | Every item is processed, and never more than `limit` run at once |
| `e1` | Results keep the input order even when later items finish first |
| `e2` | A failing item is reported and the others still finish |
| `e3` | A limit above the item count and an empty input both work |
| `e4` | A limit below one is refused |
| `e5` | Items are pulled lazily: while all workers are blocked, only `limit` items have been taken |
