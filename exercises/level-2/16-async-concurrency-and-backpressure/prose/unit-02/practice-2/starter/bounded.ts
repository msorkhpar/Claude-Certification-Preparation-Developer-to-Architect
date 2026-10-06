// Run async work over an iterable with a bound on how much runs at once. See ../../statement.md.
import { logger } from "./logger.ts";
const log = logger("bounded");

export type Outcome<R> = { ok: true; value: R } | { ok: false; error: unknown };

function checkLimit(limit: number): void {
  // TODO 1 of 6 (finish this to pass e4): refuse a limit that is not a whole number of at least 1.
  // Receives the limit. Throws Error("limit must be at least 1") for 0, a negative number or a non-integer; returns nothing otherwise.
  // Example: checkLimit(0) throws, checkLimit(3) returns
}

function nextSlot<T, R>(source: Iterator<T>, results: Outcome<R>[]): { index: number; item: T } | null {
  // TODO 2 of 6 (finish this to pass e5 and m1): take the next item and reserve its slot.
  // Receives the iterator `source` and the array `results`. Takes ONE item from the iterator, pushes a placeholder onto `results` as the slot of
  // that item and returns { index of the slot, item }; returns null when the iterator is done. Items are taken only here, one at a time.
  // Example: with results of length 1 and source yielding "b": returns { index: 1, item: "b" } and results has length 2
  return null;
}

function succeeded<R>(value: R): Outcome<R> {
  // TODO 3 of 6 (finish this to pass m1, e1 and e3): the Outcome of an item whose work returned `value`.
  // Example: succeeded(4) -> { ok: true, value: 4 }
  return { ok: false, error: undefined };
}

function failed<R>(error: unknown): Outcome<R> {
  // TODO 4 of 6 (finish this to pass e2): the Outcome of an item whose work threw `error`.
  // Example: failed(new Error("boom")) -> { ok: false, error: Error("boom") }
  return { ok: true, value: undefined as unknown as R };
}

function place<R>(results: Outcome<R>[], index: number, outcome: Outcome<R>): void {
  // TODO 5 of 6 (finish this to pass e1): store `outcome` in slot `index` of `results`, so the array stays in input order.
  // Example: results is [x, y], place(results, 1, o) makes it [x, o]
}

function pool(limit: number, worker: () => Promise<void>): Promise<void>[] {
  // TODO 6 of 6 (finish this to pass m1 and e5): the `limit` worker promises that run side by side.
  // Receives the limit and the worker function. Returns an array of `limit` promises, each from a fresh call of worker().
  // Example: pool(3, worker) -> [worker(), worker(), worker()]
  return [];
}

export async function mapBounded<T, R>(items: Iterable<T>, work: (item: T) => Promise<R>, limit: number): Promise<Outcome<R>[]> {
  log.debug("mapBounded input", items, limit);
  checkLimit(limit);
  const source = items[Symbol.iterator]();
  const results: Outcome<R>[] = []; // one slot per item, in input order, filled as items finish

  async function worker(): Promise<void> {
    for (;;) {
      // The next item is taken only when this worker is free: a slow consumer holds the producer back.
      const slot = nextSlot(source, results);
      if (slot === null) return;
      let outcome: Outcome<R>;
      try {
        outcome = succeeded(await work(slot.item));
      } catch (error) {
        outcome = failed(error); // one failure must not stop the others
      }
      place(results, slot.index, outcome);
    }
  }

  await Promise.all(pool(limit, worker));
  return results;
}
