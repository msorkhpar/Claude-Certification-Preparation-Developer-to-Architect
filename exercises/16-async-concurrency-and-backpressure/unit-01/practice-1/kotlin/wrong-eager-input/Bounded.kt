/** Run work over an iterator with a bound on how much runs at once. See ../../statement.md. */
fun <T, R> mapBounded(source: Iterator<T>, work: (T) -> R, limit: Int): List<Outcome<R>> {
    require(limit >= 1) { "limit must be at least 1" }
    val items = source.asSequence().toList().iterator()
    val results = ArrayList<Outcome<R>?>() // one slot per item, in input order, filled as items finish
    val lock = Any()
    val workers = List(limit) {
        Thread {
            while (true) {
                val item: T
                val index: Int
                // The next item is taken only when this worker is free: a slow consumer holds the producer back.
                synchronized(lock) {
                    if (!items.hasNext()) return@Thread
                    item = items.next()
                    index = results.size
                    results.add(null)
                }
                val outcome = try {
                    Outcome(true, work(item), null)
                } catch (error: Throwable) { // one failure must not stop the others
                    Outcome<R>(false, null, error)
                }
                synchronized(lock) { results[index] = outcome }
            }
        }.also { it.start() }
    }
    workers.forEach { it.join() }
    return results.map { it!! }
}
