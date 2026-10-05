// Run async work over an iterable with a bound on how much runs at once. See ../../statement.md.
import { logger } from "../logger.ts";
const log = logger("bounded");

export type Outcome<R> = { ok: true; value: R } | { ok: false; error: unknown };

/** Refuse a limit that is not a whole number of at least 1. */
function checkLimit(limit: number): void {
  if (!Number.isInteger(limit) || limit < 1) throw new Error("limit must be at least 1");
}

/** Take the next item from `source` and reserve its slot at the end of `results`. */
function nextSlot<T, R>(source: Iterator<T>, results: Outcome<R>[]): { index: number; item: T } | null {
  const next = source.next();
  if (next.done) return null;
  results.push(undefined as unknown as Outcome<R>);
  return { index: results.length - 1, item: next.value };
}

/** The Outcome of an item whose work returned `value`. */
function succeeded<R>(value: R): Outcome<R> {
  return { ok: true, value };
}

/** The Outcome of an item whose work threw `error`. */
function failed<R>(error: unknown): Outcome<R> {
  return { ok: false, error };
}

/** Store `outcome` in the slot of its item, so the list stays in input order. */
function place<R>(results: Outcome<R>[], index: number, outcome: Outcome<R>): void {
  results[index] = outcome;
}

/** The `limit` worker promises that run side by side. */
function pool(limit: number, worker: () => Promise<void>): Promise<void>[] {
  return Array.from({ length: limit }, worker);
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
