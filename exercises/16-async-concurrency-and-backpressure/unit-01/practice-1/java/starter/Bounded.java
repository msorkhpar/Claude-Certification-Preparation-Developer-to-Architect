import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

/** Run work over an iterator with a bound on how much runs at once. See ../../statement.md. */
final class Bounded {
    private Bounded() {}

    static <T, R> List<Outcome<R>> mapBounded(Iterator<T> items, Function<T, R> work, int limit) {
        // TODO: run work on at most `limit` items at a time, taking items lazily; one Outcome per item, in input order.
        return List.of();
    }
}
