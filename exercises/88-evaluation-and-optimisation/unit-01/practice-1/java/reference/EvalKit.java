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

    static int errorCost(int wrong, String segment, Map<String, Integer> costs) {
        return wrong * costs.getOrDefault(segment, 1);
    }

    static List<Line> order(List<Line> table) {
        table.sort(Comparator.comparingInt((Line l) -> -l.cost()).thenComparing(Line::segment));
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

    static int percentile(List<Integer> values, int p) {
        if (values.isEmpty()) return 0;
        List<Integer> ordered = new ArrayList<>(values);
        ordered.sort(null);
        return ordered.get((p * ordered.size() + 99) / 100 - 1);
    }

    static String abVerdict(int x1, int n1, int x2, int n2) {
        return abVerdict(x1, n1, x2, n2, 200);
    }

    static String abVerdict(int x1, int n1, int x2, int n2, int minN) {
        if (n1 < minN || n2 < minN) return "too few cases";
        long bigN = n1 + n2;
        long x = x1 + x2;
        if (x == 0 || x == bigN) return "no clear difference";
        long d = (long) x2 * n1 - (long) x1 * n2;
        if (d * d * bigN * 10000L < 38416L * n1 * n2 * x * (bigN - x)) return "no clear difference";
        return d > 0 ? "new is better" : "old is better";
    }

    static String decision(List<String> blocked, int lost, int gained) {
        return blocked.isEmpty() && lost <= gained ? "ship" : "hold";
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

    static String diagnose(boolean found, boolean supported, boolean formatOk, boolean passesOnStronger) {
        if (!found) return "retrieval or data";
        if (!supported) return "ungrounded answer";
        if (!formatOk) return "format instructions";
        if (!passesOnStronger) return "prompt or task";
        return "model mismatch";
    }

    static String chooseModel(List<Option> options, int minAccuracy, int maxP95) {
        return options.stream().filter(o -> o.accuracy() >= minAccuracy && o.p95() <= maxP95)
            .min(Comparator.comparingInt(Option::cost).thenComparing(Option::name)).map(Option::name).orElse("none");
    }
}
