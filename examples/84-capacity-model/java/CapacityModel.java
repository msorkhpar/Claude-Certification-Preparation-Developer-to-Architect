import java.util.List;
import java.util.Locale;

/**
 * A capacity and cost model for one workload: the limits it needs, the tier that gives them, and the monthly bill.
 *
 * <p>The Claude documentation on rate limits (read on 2026-10-04) says that "for most Claude models, only uncached input tokens count toward
 * your ITPM rate limits": {@code input_tokens} and {@code cache_creation_input_tokens} count, {@code cache_read_input_tokens} do not. The limits of the
 * Start, Build and Scale tiers below are the documented figures for Claude Sonnet 5.5, and the prices are the documented Sonnet 5.5 prices on the
 * Claude API (input 2, output 10, 5-minute cache write 2.50, cache read 0.20 dollars per million tokens, batch at half price). Prices and
 * limits change; re-read them before you plan. Money is kept in whole cents so that every language prints the same figures.
 */
public final class CapacityModel {
    /** Tokens per request and requests per minute. */
    record Workload(long rpm, long input, long cacheWrite, long cacheRead, long output) {}

    record Need(long rpm, long itpm, long otpm) {}

    record Tier(String name, long rpm, long itpm, long otpm) {}

    static final List<Tier> TIERS = List.of(new Tier("Start", 1000, 2_000_000, 400_000), new Tier("Build", 5000, 5_000_000, 1_000_000), new Tier("Scale", 10_000, 10_000_000, 2_000_000));

    static final Workload CACHED = new Workload(800, 1500, 200, 6000, 400);
    static final Workload UNCACHED = new Workload(800, 7700, 0, 0, 400); // the same prompts with no caching
    static final long REQUESTS_PER_MONTH = 2_000_000;

    private static long up(long x, long headroomPercent) {
        return (x * (100 + headroomPercent) + 99) / 100;
    }

    /** RPM, ITPM and OTPM to ask for. Cache reads do not count toward ITPM; every figure is rounded up after the headroom. */
    static Need requiredCapacity(Workload w, long headroomPercent) {
        return new Need(up(w.rpm(), headroomPercent), up(w.rpm() * (w.input() + w.cacheWrite()), headroomPercent), up(w.rpm() * w.output(), headroomPercent));
    }

    static String smallestTier(Need need, List<Tier> tiers) {
        for (Tier t : tiers) if (need.rpm() <= t.rpm() && need.itpm() <= t.itpm() && need.otpm() <= t.otpm()) return t.name();
        return "Custom";
    }

    /** Cents per month. The share of requests sent through the Batch API is billed at half price in every category. */
    static long monthlyCents(Workload w, long requests, long batchPercent) {
        long perRequest = w.input() * 200 + w.cacheWrite() * 250 + w.cacheRead() * 20 + w.output() * 1000; // cents times tokens, per million
        return requests * perRequest * (200 - batchPercent) / (200 * 1_000_000L);
    }

    static String dollars(long cents) {
        return String.format(Locale.ROOT, "$%,d.%02d", cents / 100, cents % 100);
    }

    public static void main(String[] args) {
        for (Object[] row : new Object[][] {{"with caching", CACHED}, {"without caching", UNCACHED}}) {
            Need need = requiredCapacity((Workload) row[1], 30);
            System.out.println(row[0] + ": need " + need.rpm() + " rpm, " + need.itpm() + " itpm, " + need.otpm() + " otpm -> tier " + smallestTier(need, TIERS));
        }
        System.out.println("monthly bill with caching: " + dollars(monthlyCents(CACHED, REQUESTS_PER_MONTH, 0)));
        System.out.println("monthly bill without caching: " + dollars(monthlyCents(UNCACHED, REQUESTS_PER_MONTH, 0)));
        System.out.println("monthly bill with caching and 30 percent batch: " + dollars(monthlyCents(CACHED, REQUESTS_PER_MONTH, 30)));
    }
}
