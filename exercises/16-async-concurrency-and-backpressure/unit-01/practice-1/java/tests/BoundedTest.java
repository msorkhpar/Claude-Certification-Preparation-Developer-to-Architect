import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class BoundedTest {
    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** A work function that records how many calls are in flight at once. */
    private static final class Probe implements Function<Integer, Integer> {
        final AtomicInteger active = new AtomicInteger();
        final AtomicInteger peak = new AtomicInteger();
        final AtomicInteger started = new AtomicInteger();

        public Integer apply(Integer item) {
            started.incrementAndGet();
            int now = active.incrementAndGet();
            peak.accumulateAndGet(now, Math::max);
            try {
                sleep(30);
                return item * 2;
            } finally {
                active.decrementAndGet();
            }
        }
    }

    private static Iterator<Integer> range(int n) {
        return IntStream.range(0, n).iterator();
    }

    private static List<Object> values(List<Outcome<Integer>> outcomes) {
        List<Object> out = new ArrayList<>();
        for (Outcome<Integer> o : outcomes) out.add(o.value());
        return out;
    }

    @Test
    void m1_resultsComeBackForEveryItemAndNeverMoreThanLimitRunAtOnce() {
        Probe probe = new Probe();
        List<Outcome<Integer>> outcomes = Bounded.mapBounded(range(10), probe, 3);
        assertEquals(IntStream.range(0, 10).mapToObj(n -> (Object) (n * 2)).toList(), values(outcomes));
        assertTrue(outcomes.stream().allMatch(Outcome::ok));
        assertEquals(3, probe.peak.get());
        assertEquals(10, probe.started.get());
    }

    @Test
    void e1_resultsKeepTheInputOrderEvenWhenLaterItemsFinishFirst() {
        Function<Integer, Integer> slowFirst = item -> {
            sleep(item == 0 ? 120 : 2);
            return item;
        };
        List<Outcome<Integer>> outcomes = Bounded.mapBounded(range(5), slowFirst, 5);
        assertEquals(List.of(0, 1, 2, 3, 4), values(outcomes));
    }

    @Test
    void e2_aFailingItemIsReportedAndTheOthersStillFinish() {
        Function<Integer, Integer> sometimes = item -> {
            if (item == 2) throw new IllegalStateException("boom");
            sleep(5);
            return item;
        };
        List<Outcome<Integer>> outcomes;
        try {
            outcomes = Bounded.mapBounded(range(5), sometimes, 2);
        } catch (RuntimeException e) {
            outcomes = fail("the run threw " + e);
        }
        assertEquals(List.of(true, true, false, true, true), outcomes.stream().map(Outcome::ok).toList());
        assertInstanceOf(IllegalStateException.class, outcomes.get(2).error());
        assertEquals("boom", outcomes.get(2).error().getMessage());
        assertEquals(List.of(0, 1, 3, 4), outcomes.stream().filter(Outcome::ok).map(Outcome::value).toList());
    }

    @Test
    void e3_aLimitAboveTheItemCountAndAnEmptyInputBothWork() {
        Probe probe = new Probe();
        List<Outcome<Integer>> outcomes = Bounded.mapBounded(List.of(1, 2, 3).iterator(), probe, 50);
        assertEquals(List.of(2, 4, 6), values(outcomes));
        assertEquals(3, probe.peak.get());
        assertEquals(0, Bounded.mapBounded(range(0), new Probe(), 4).size());
    }

    @Test
    void e4_aLimitBelowOneIsRefused() {
        for (int bad : new int[] {0, -1}) {
            assertThrows(IllegalArgumentException.class, () -> Bounded.mapBounded(List.of(1).iterator(), new Probe(), bad), "limit " + bad);
        }
    }

    @Test
    void e5_itemsArePulledLazilySoASlowConsumerHoldsTheProducerBack() throws Exception {
        AtomicInteger pulled = new AtomicInteger();
        Iterator<Integer> source = new Iterator<>() {
            int n = 0;

            public boolean hasNext() {
                return n < 20;
            }

            public Integer next() {
                pulled.incrementAndGet();
                return n++;
            }
        };
        CountDownLatch gate = new CountDownLatch(1);
        Function<Integer, Integer> blocked = item -> {
            try {
                gate.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return item;
        };
        CompletableFuture<List<Outcome<Integer>>> run = CompletableFuture.supplyAsync(() -> Bounded.mapBounded(source, blocked, 2));
        sleep(300);
        int held = pulled.get(); // every worker is blocked: only `limit` items may have been taken
        gate.countDown();
        List<Outcome<Integer>> outcomes = run.get(10, TimeUnit.SECONDS);
        assertEquals(2, held, held + " items were pulled while 2 workers were blocked");
        assertEquals(IntStream.range(0, 20).mapToObj(n -> (Object) n).toList(), values(outcomes));
    }
}
