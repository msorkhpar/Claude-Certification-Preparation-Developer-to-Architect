// Run async work over an iterable with a bound on how much runs at once. See ../../statement.md.
export type Outcome<R> = { ok: true; value: R } | { ok: false; error: unknown };

export async function mapBounded<T, R>(items: Iterable<T>, work: (item: T) => Promise<R>, limit: number): Promise<Outcome<R>[]> {
  if (!Number.isInteger(limit) || limit < 1) throw new Error("limit must be at least 1");
  const source = [...items][Symbol.iterator]();
  const results: Outcome<R>[] = []; // one slot per item, in input order, filled as items finish

  async function worker(): Promise<void> {
    for (;;) {
      // The next item is taken only when this worker is free: a slow consumer holds the producer back.
      const next = source.next();
      if (next.done) return;
      const index = results.length;
      results.push(undefined as unknown as Outcome<R>);
      let outcome: Outcome<R>;
      try {
        outcome = { ok: true, value: await work(next.value) };
      } catch (error) {
        outcome = { ok: false, error }; // one failure must not stop the others
      }
      results[index] = outcome;
    }
  }

  await Promise.all(Array.from({ length: limit }, worker));
  return results;
}
