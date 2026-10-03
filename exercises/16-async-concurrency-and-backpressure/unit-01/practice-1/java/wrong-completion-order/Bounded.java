import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

/** Run work over an iterator with a bound on how much runs at once. See ../../statement.md. */
final class Bounded {
    private Bounded() {}

    static <T, R> List<Outcome<R>> mapBounded(Iterator<T> items, Function<T, R> work, int limit) {
        if (limit < 1) throw new IllegalArgumentException("limit must be at least 1");
        List<Outcome<R>> results = new ArrayList<>(); // one slot per item, in input order, filled as items finish
        Object lock = new Object();
        List<Thread> workers = new ArrayList<>();
        for (int i = 0; i < limit; i++) {
            Thread worker = new Thread(() -> {
                while (true) {
                    T item;
                    int index;
                    // The next item is taken only when this worker is free: a slow consumer holds the producer back.
                    synchronized (lock) {
                        if (!items.hasNext()) return;
                        item = items.next();
                        index = results.size();
                        results.add(null);
                    }
                    Outcome<R> outcome;
                    try {
                        outcome = new Outcome<>(true, work.apply(item), null);
                    } catch (Throwable error) { // one failure must not stop the others
                        outcome = new Outcome<>(false, null, error);
                    }
                    synchronized (lock) {
                        results.remove(null);
                        results.add(outcome);
                    }
                }
            });
            workers.add(worker);
            worker.start();
        }
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
