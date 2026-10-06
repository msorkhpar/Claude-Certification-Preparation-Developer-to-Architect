import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Evaluation kit: a report by segment, a latency percentile, an A/B verdict, a shadow-run gate, a diagnosis order and a model choice under limits. See ../../statement.md. */
final class EvalKit {
    private static final System.Logger LOG = System.getLogger(EvalKit.class.getName());
    private EvalKit() {}

    /** One graded case: its segment and whether the answer was right. */
    record Result(String segment, boolean correct) {}

    /** One line of the report: the segment, its cases, the right answers, the whole percent and the cost of the wrong ones. */
    record Line(String segment, int cases, int right, int percent, int cost) {}

    /** One case run on the old and the new version. */
    record Paired(String segment, boolean oldOk, boolean newOk) {}

    /** The shadow gate's decision, the right answers lost and gained, and the protected segments that lost. */
    record Gate(String decision, int lost, int gained, List<String> blocked) {}

    /** A model option: its name, accuracy in percent, 95th-percentile latency in ms and cost. */
    record Option(String name, int accuracy, int p95, int cost) {}

    /** Whole percent, half up, integers only (written for you). */
    static int pct(int part, int whole) {
        return whole != 0 ? (200 * part + whole) / (2 * whole) : 0;
    }

    /**
     * TODO 1 of 7 (unlocks m1 and e1): the cost of the wrong answers of one segment.
     * Receives the number of wrong answers, the segment name and the costs by segment. Returns wrong times the segment's cost, 1 when the
     * segment has no entry. Example: errorCost(3, "refund", Map.of("refund", 20)) -> 60, errorCost(2, "odd", Map.of("refund", 20)) -> 2
     */
    static int errorCost(int wrong, String segment, Map<String, Integer> costs) {
        return 0;
    }

    /**
     * TODO 2 of 7 (unlocks m1 and e1): order the report lines (the list is mutable).
     * Receives the lines. Returns them with the highest cost first and equal costs by segment name.
     * Example: a line with cost 5 comes before a line with cost 0, and two lines with cost 1 are ordered "a" before "b".
     */
    static List<Line> order(List<Line> table) {
        return table;
    }

    static List<Line> segmentTable(List<Result> results, Map<String, Integer> costs) {
        LOG.log(System.Logger.Level.DEBUG, "segmentTable input: {0}", results);
        Map<String, int[]> seen = new LinkedHashMap<>();
        for (Result r : results) {
            int[] counts = seen.computeIfAbsent(r.segment(), k -> new int[2]);
            counts[0]++;
            if (r.correct()) counts[1]++;
        }
        List<Line> table = new ArrayList<>();
        seen.forEach((s, c) -> table.add(new Line(s, c[0], c[1], pct(c[1], c[0]), errorCost(c[0] - c[1], s, costs))));
        return order(table);
    }

    /**
     * TODO 3 of 7 (unlocks e2): the nearest-rank percentile.
     * Receives the values in any order and p from 1 to 100. Returns the value at rank ceil(p * n / 100) of the sorted values, counting
     * from 1, and 0 for no values. Example: percentile(List.of(4800, 800, 1000, 900), 95) -> 4800
     */
    static int percentile(List<Integer> values, int p) {
        return -1;
    }

    static String abVerdict(int x1, int n1, int x2, int n2) {
        return abVerdict(x1, n1, x2, n2, 200);
    }

    /**
     * TODO 4 of 7 (unlocks e3 and e4): the A/B verdict at 95 percent.
     * Receives the right answers and cases of the old version (x1 of n1) and the new one (x2 of n2). Returns "too few cases" when an arm
     * has fewer than minN cases, "no clear difference" when the pooled right answers are 0 or all, otherwise the integer test from the
     * statement: "new is better", "old is better" or "no clear difference". Example: abVerdict(410, 500, 438, 500, 200) -> "new is better"
     */
    static String abVerdict(int x1, int n1, int x2, int n2, int minN) {
        return "";
    }

    /**
     * TODO 5 of 7 (unlocks e5): ship or hold.
     * Receives the protected segments that lost, the number lost and the number gained. Returns "hold" when a protected segment lost or
     * more were lost than gained, "ship" otherwise. Example: decision(List.of(), 2, 2) -> "ship", decision(List.of("refund"), 1, 5) -> "hold"
     */
    static String decision(List<String> blocked, int lost, int gained) {
        return "";
    }

    static Gate shadowGate(List<Paired> pairs, Set<String> protectedSegments) {
        int lost = 0;
        int gained = 0;
        TreeSet<String> blocked = new TreeSet<>();
        for (Paired p : pairs) {
            if (p.oldOk() && !p.newOk()) {
                lost++;
                if (protectedSegments.contains(p.segment())) blocked.add(p.segment());
            }
            if (p.newOk() && !p.oldOk()) gained++;
        }
        return new Gate(decision(new ArrayList<>(blocked), lost, gained), lost, gained, new ArrayList<>(blocked));
    }

    /**
     * TODO 6 of 7 (unlocks e6): where to look first for a wrong answer.
     * Receives four booleans. Returns "retrieval or data" when the evidence was not found, then "ungrounded answer" when it is not
     * supported, then "format instructions" when the format is wrong, then "prompt or task" when it fails on a stronger model, else
     * "model mismatch". Example: diagnose(true, true, false, true) -> "format instructions"
     */
    static String diagnose(boolean found, boolean supported, boolean formatOk, boolean passesOnStronger) {
        return "";
    }

    /**
     * TODO 7 of 7 (unlocks e7): the cheapest model that meets both limits.
     * Receives options (name, accuracy, p95, cost). Returns the name of the cheapest one with accuracy >= minAccuracy and p95 <= maxP95,
     * equal costs by name, or "none". Example: chooseModel(List.of(new Option("a", 90, 100, 2)), 95, 100) -> "none"
     */
    static String chooseModel(List<Option> options, int minAccuracy, int maxP95) {
        return "";
    }
}
