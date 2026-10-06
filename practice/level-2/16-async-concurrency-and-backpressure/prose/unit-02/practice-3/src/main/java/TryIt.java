import java.util.stream.IntStream;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Run executes this file. Change the calls in main to try your code; Submit runs the tests. */
public class TryIt {
    public static void main(String[] args) {
        // Turn the logger up, so the LOG.log(DEBUG, ...) lines of your code show under the printed lines.
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s %5$s%n");
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        Logger root = Logger.getLogger("");
        root.setLevel(Level.ALL);
        root.addHandler(handler);

        AtomicInteger now = new AtomicInteger();
        AtomicInteger peak = new AtomicInteger();

        // Ten items, never more than three at a time; the work is a stand-in for one API call.
        List<Outcome<Integer>> outcomes = Bounded.mapBounded(IntStream.range(0, 10).iterator(), item -> {
            peak.accumulateAndGet(now.incrementAndGet(), Math::max);
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            now.decrementAndGet();
            return item * 2;
        }, 3);

        System.out.println("outcomes: " + outcomes.size());
        System.out.println("values: " + outcomes.stream().filter(Outcome::ok).map(Outcome::value).sorted().toList());
        System.out.println("all ok: " + outcomes.stream().allMatch(Outcome::ok));
        System.out.println("peak in flight: " + peak.get());
    }
}
