// Run async work over an iterable with a bound on how much runs at once. See ../../statement.md.
export type Outcome<R> = { ok: true; value: R } | { ok: false; error: unknown };

export async function mapBounded<T, R>(items: Iterable<T>, work: (item: T) => Promise<R>, limit: number): Promise<Outcome<R>[]> {
  // TODO: start work(item) for at most `limit` items at a time, taking items lazily; one Outcome per item, in input order.
  return [];
}
