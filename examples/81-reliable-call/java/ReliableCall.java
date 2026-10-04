import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;

/**
 * A retried refund that must not pay twice, and a circuit breaker around a failing agent.
 *
 * <p>Anthropic's multi-agent research write-up (read on 2026-10-04) says it combines "the adaptability of AI agents built on Claude with
 * deterministic safeguards like retry logic and regular checkpoints". This file shows two such safeguards on scripted failures. A retry is
 * safe only when the tool it repeats is idempotent: the refund tool records its idempotency key together with its effect, so a second
 * attempt with the same key returns the first result. A breaker stops calling an agent that keeps failing and lets one probe through after
 * a cooldown. Nothing is called over a network: the failures are scripted and the clock is a number.
 */
public final class ReliableCall {
    /** A failure worth retrying: a timeout, a rate limit, a lost response. */
    static final class Transient extends RuntimeException {
        Transient(String message) {
            super(message);
        }
    }

    /** The refunds actually paid, and the idempotency keys already used. */
    static final class Ledger {
        final List<String> paid = new ArrayList<>();
        final Map<String, String> keys = new HashMap<>();
    }

    record Attempt<T>(T result, int calls) {}

    /** Pays once per key. The response may be lost after the money has moved, which is the dangerous case. */
    static String refund(Ledger ledger, String key, String order, int amount, boolean loseResponse) {
        if (key != null && ledger.keys.containsKey(key)) return ledger.keys.get(key);
        ledger.paid.add(order + ":" + amount);
        String receipt = "refund-" + ledger.paid.size();
        if (key != null) ledger.keys.put(key, receipt);
        if (loseResponse) throw new Transient("response lost");
        return receipt;
    }

    /** Calls attempt(n) up to {@code tries} times while it throws Transient; returns the result and the calls made. */
    static <T> Attempt<T> retry(IntFunction<T> attempt, int tries) {
        for (int n = 1; ; n++) {
            try {
                return new Attempt<>(attempt.apply(n), n);
            } catch (Transient e) {
                if (n >= tries) throw e;
            }
        }
    }

    /** Closed until {@code threshold} failures in a row, then open for {@code cooldown} seconds, then one probe (half open). */
    static final class Breaker {
        private final int threshold;
        private final int cooldown;
        private int failures = 0;
        private Integer openedAt = null;

        Breaker(int threshold, int cooldown) {
            this.threshold = threshold;
            this.cooldown = cooldown;
        }

        String state(int now) {
            if (openedAt == null) return "closed";
            return now - openedAt >= cooldown ? "half-open" : "open";
        }

        boolean allow(int now) {
            return !state(now).equals("open");
        }

        void record(boolean ok, int now) {
            if (ok) {
                failures = 0;
                openedAt = null;
            } else {
                failures += 1;
                if (failures >= threshold || state(now).equals("half-open")) openedAt = now;
            }
        }
    }

    public static void main(String[] args) {
        Ledger naive = new Ledger();
        retry(n -> refund(naive, null, "order-7", 40, n == 1), 2);
        System.out.println("retry without a key: " + naive.paid.size() + " refunds paid for one order");
        Ledger keyed = new Ledger();
        Attempt<String> done = retry(n -> refund(keyed, "order-7:refund", "order-7", 40, n == 1), 2);
        System.out.println("retry with a key: " + keyed.paid.size() + " refund paid for one order, " + done.calls() + " calls, receipt " + done.result());
        Breaker breaker = new Breaker(3, 30);
        int reached = 0;
        int[] times = {0, 1, 2, 3, 40, 41};
        boolean[] healthy = {false, false, false, true, true, true};
        for (int i = 0; i < times.length; i++) {
            int now = times[i];
            if (!breaker.allow(now)) {
                System.out.println("t=" + now + ": breaker " + breaker.state(now) + ", call refused without reaching the agent");
                continue;
            }
            reached++;
            breaker.record(healthy[i], now);
            System.out.println("t=" + now + ": call " + (healthy[i] ? "succeeded" : "failed") + ", breaker " + breaker.state(now));
        }
        System.out.println("the agent was reached " + reached + " times in 6 attempts");
    }
}
