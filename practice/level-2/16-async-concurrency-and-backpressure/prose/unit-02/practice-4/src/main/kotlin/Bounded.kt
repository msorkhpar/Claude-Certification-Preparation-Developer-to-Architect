private val log = System.getLogger("bounded")

private fun checkLimit(limit: Int) {
    // TODO 1 of 6 (finish this to pass e4): refuse a limit below 1.
    // Receives the limit. Throws IllegalArgumentException("limit must be at least 1") (use require) when it is below 1; returns otherwise.
    // Example: checkLimit(0) throws, checkLimit(3) returns
}

/** An item and the index of its slot in the results. */
private class Slot<T>(val index: Int, val item: T)

private fun <T, R> nextSlot(items: Iterator<T>, results: MutableList<Outcome<R>?>): Slot<T>? {
    // TODO 2 of 6 (finish this to pass e5 and m1): take the next item and reserve its slot. Called inside the lock.
    // Receives the iterator and the list `results`. Takes ONE item from the iterator, adds null to `results` as the slot of that item and
    // returns Slot(index of the slot, item); returns null when the iterator is exhausted. Items are taken only here, one at a time.
    // Example: with results of size 1 and items yielding "b": returns Slot(1, "b") and results has size 2
    return null
}

private fun <R> succeeded(value: R): Outcome<R> {
    // TODO 3 of 6 (finish this to pass m1, e1 and e3): the Outcome of an item whose work returned `value`.
    // Example: succeeded(4) -> Outcome(true, 4, null)
    return Outcome(false, null, null)
}

private fun <R> failed(error: Throwable): Outcome<R> {
    // TODO 4 of 6 (finish this to pass e2): the Outcome of an item whose work threw `error`.
    // Example: failed(boom) -> Outcome(false, null, boom)
    return Outcome(true, null, null)
}

private fun <R> place(results: MutableList<Outcome<R>?>, index: Int, outcome: Outcome<R>) {
    // TODO 5 of 6 (finish this to pass e1): store `outcome` in slot `index` of `results`, so the list stays in input order.
    // Example: results is [null, null], place(results, 1, o) makes it [null, o]
}

private fun pool(limit: Int, body: () -> Unit): List<Thread> {
    // TODO 6 of 6 (finish this to pass m1 and e5): start `limit` threads that each run `body`, and return them.
    // Receives the limit and the body. Returns the list of the `limit` started threads (they are joined by the caller).
    // Example: pool(3, body) -> three running threads
    return emptyList()
}

/** Run work over an iterator with a bound on how much runs at once. See ../../statement.md. */
fun <T, R> mapBounded(items: Iterator<T>, work: (T) -> R, limit: Int): List<Outcome<R>> {
    log.log(System.Logger.Level.DEBUG, "mapBounded input: limit={0}", limit)
    checkLimit(limit)
    val results = ArrayList<Outcome<R>?>() // one slot per item, in input order, filled as items finish
    val lock = Any()
    val workers = pool(limit) {
        while (true) {
            // The next item is taken only when this worker is free: a slow consumer holds the producer back.
            val slot = synchronized(lock) { nextSlot(items, results) } ?: return@pool
            val outcome = try {
                succeeded(work(slot.item))
            } catch (error: Throwable) { // one failure must not stop the others
                failed<R>(error)
            }
            synchronized(lock) { place(results, slot.index, outcome) }
        }
    }
    workers.forEach { it.join() }
    return results.map { it!! }
}
