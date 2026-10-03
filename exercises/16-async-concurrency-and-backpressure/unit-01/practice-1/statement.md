# Practice: bounded concurrency with backpressure

Write the worker pool that every batch job needs: run work over many items, never more than `limit` at a time,
without reading the whole input first. Pick your language folder (`python`, `typescript`, `java` or `kotlin`),
open `starter/` and edit the file there.

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

Run the tests with the command in the language folder's `run.sh` (Python, TypeScript) or its build file.
