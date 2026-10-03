/** Run work over an iterator with a bound on how much runs at once. See ../../statement.md. */
fun <T, R> mapBounded(items: Iterator<T>, work: (T) -> R, limit: Int): List<Outcome<R>> {
    // TODO: run work on at most `limit` items at a time, taking items lazily; one Outcome per item, in input order.
    return emptyList()
}
