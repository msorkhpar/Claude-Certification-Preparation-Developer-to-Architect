import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

/** Run work over an iterator with a bound on how much runs at once. See ../../statement.md. */
final class Bounded {
    private static final System.Logger LOG = System.getLogger(Bounded.class.getName());
    private Bounded() {}

    /** Refuse a limit below 1. */
    private static void checkLimit(int limit) {
        if (limit < 1) throw new IllegalArgumentException("limit must be at least 1");
    }

    /** An item and the index of its slot in the results. */
    private record Slot<T>(int index, T item) {}

    /** Take the next item from `items` and reserve its slot at the end of `results`; null when the iterator is exhausted. Call it inside the lock. */
    private static <T, R> Slot<T> nextSlot(Iterator<T> items, List<Outcome<R>> results) {
        if (!items.hasNext()) return null;
        T item = items.next();
        results.add(null);
        return new Slot<>(results.size() - 1, item);
    }

    /** The Outcome of an item whose work returned `value`. */
    private static <R> Outcome<R> succeeded(R value) {
        return new Outcome<>(true, value, null);
    }

    /** The Outcome of an item whose work threw `error`. */
    private static <R> Outcome<R> failed(Throwable error) {
        return new Outcome<>(false, null, error);
    }

    /** Store `outcome` in the slot of its item, so the list stays in input order. */
    private static <R> void place(List<Outcome<R>> results, int index, Outcome<R> outcome) {
        results.set(index, outcome);
    }

    /** Start `limit` threads that each run `body`, and return them. */
    private static List<Thread> pool(int limit, Runnable body) {
        List<Thread> workers = new ArrayList<>();
        for (int i = 0; i < limit; i++) {
            Thread worker = new Thread(body);
            workers.add(worker);
            worker.start();
        }
        return workers;
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
