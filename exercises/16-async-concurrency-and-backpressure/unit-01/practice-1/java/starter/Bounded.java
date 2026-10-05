import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

/** Run work over an iterator with a bound on how much runs at once. See ../../statement.md. */
final class Bounded {
    private static final System.Logger LOG = System.getLogger(Bounded.class.getName());
    private Bounded() {}

    private static void checkLimit(int limit) {
        // TODO 1 of 6 (finish this to pass e4): refuse a limit below 1.
        // Receives the limit. Throws IllegalArgumentException("limit must be at least 1") when it is below 1; returns otherwise.
        // Example: checkLimit(0) throws, checkLimit(3) returns
    }

    /** An item and the index of its slot in the results. */
    private record Slot<T>(int index, T item) {}

    private static <T, R> Slot<T> nextSlot(Iterator<T> items, List<Outcome<R>> results) {
        // TODO 2 of 6 (finish this to pass e5 and m1): take the next item and reserve its slot. Called inside the lock.
        // Receives the iterator and the list `results`. Takes ONE item from the iterator, adds null to `results` as the slot of that item and
        // returns new Slot<>(index of the slot, item); returns null when the iterator is exhausted. Items are taken only here, one at a time.
        // Example: with results of size 1 and items yielding "b": returns Slot(1, "b") and results has size 2
        return null;
    }

    private static <R> Outcome<R> succeeded(R value) {
        // TODO 3 of 6 (finish this to pass m1, e1 and e3): the Outcome of an item whose work returned `value`.
        // Example: succeeded(4) -> Outcome(true, 4, null)
        return new Outcome<>(false, null, null);
    }

    private static <R> Outcome<R> failed(Throwable error) {
        // TODO 4 of 6 (finish this to pass e2): the Outcome of an item whose work threw `error`.
        // Example: failed(boom) -> Outcome(false, null, boom)
        return new Outcome<>(true, null, null);
    }

    private static <R> void place(List<Outcome<R>> results, int index, Outcome<R> outcome) {
        // TODO 5 of 6 (finish this to pass e1): store `outcome` in slot `index` of `results`, so the list stays in input order.
        // Example: results is [null, null], place(results, 1, o) makes it [null, o]
    }

    private static List<Thread> pool(int limit, Runnable body) {
        // TODO 6 of 6 (finish this to pass m1 and e5): start `limit` threads that each run `body`, and return them.
        // Receives the limit and the body. Returns the list of the `limit` started threads (they are joined by the caller).
        // Example: pool(3, body) -> three running threads
        return List.of();
    }

    static <T, R> List<Outcome<R>> mapBounded(Iterator<T> items, Function<T, R> work, int limit) {
        LOG.log(System.Logger.Level.DEBUG, "mapBounded input: limit={0}", limit);
        checkLimit(limit);
        List<Outcome<R>> results = new ArrayList<>(); // one slot per item, in input order, filled as items finish
        Object lock = new Object();
        List<Thread> workers = pool(limit, () -> {
            while (true) {
                Slot<T> slot;
                // The next item is taken only when this worker is free: a slow consumer holds the producer back.
                synchronized (lock) {
                    slot = nextSlot(items, results);
                    if (slot == null) return;
                }
                Outcome<R> outcome;
                try {
                    outcome = succeeded(work.apply(slot.item()));
                } catch (Throwable error) { // one failure must not stop the others
                    outcome = failed(error);
                }
                synchronized (lock) {
                    place(results, slot.index(), outcome);
                }
            }
        });
        for (Thread worker : workers) {
            try {
                worker.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
        return results;
    }
}
