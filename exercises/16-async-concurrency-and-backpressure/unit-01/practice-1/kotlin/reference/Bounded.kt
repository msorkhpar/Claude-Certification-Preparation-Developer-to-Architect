private val log = System.getLogger("bounded")

/** Refuse a limit below 1. */
private fun checkLimit(limit: Int) {
    require(limit >= 1) { "limit must be at least 1" }
}

/** An item and the index of its slot in the results. */
private class Slot<T>(val index: Int, val item: T)

/** Take the next item from `items` and reserve its slot at the end of `results`; null when the iterator is exhausted. Call it inside the lock. */
private fun <T, R> nextSlot(items: Iterator<T>, results: MutableList<Outcome<R>?>): Slot<T>? {
    if (!items.hasNext()) return null
    val item = items.next()
    results.add(null)
    return Slot(results.size - 1, item)
}

/** The Outcome of an item whose work returned `value`. */
private fun <R> succeeded(value: R): Outcome<R> = Outcome(true, value, null)

/** The Outcome of an item whose work threw `error`. */
private fun <R> failed(error: Throwable): Outcome<R> = Outcome(false, null, error)

/** Store `outcome` in the slot of its item, so the list stays in input order. */
private fun <R> place(results: MutableList<Outcome<R>?>, index: Int, outcome: Outcome<R>) {
    results[index] = outcome
}

/** Start `limit` threads that each run `body`, and return them. */
private fun pool(limit: Int, body: () -> Unit): List<Thread> = List(limit) { Thread { body() }.also { it.start() } }

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
