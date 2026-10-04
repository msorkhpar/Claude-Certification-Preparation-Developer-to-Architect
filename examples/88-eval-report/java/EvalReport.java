import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Evaluation decisions for a system that changes: a report by segment, a latency percentile, an A/B verdict, a shadow-run gate, a diagnosis order and a model choice under limits.
 *
 * The cases, counts and timings are invented for the example. The rules come from the Claude Certified Architect - Professional exam guide (domain 4) and the Claude documentation pages on defining success, developing tests, reducing hallucinations and reducing latency, read on 2026-10-04. Nothing here calls a model.
 */
public class EvalReport {
    record Row(String segment, boolean oldOk, boolean newOk) {}

    record Line(String segment, int cases, int right, int percent, int cost) {}

    record Option(String name, int accuracy, int p95, int cost) {}

    record Gate(String decision, int lost, int gained, List<String> blocked) {}

    static final Map<String, Integer> COSTS = Map.of("order status", 1, "refund", 20, "policy", 5, "complaint", 10);

    /** One row per case: right in both, right only with the current prompt, right only with the new one, then wrong in both. */
    static List<Row> buildCases() {
        List<Row> rows = new ArrayList<>();
        String[] segments = {"order status", "refund", "policy", "complaint"};
        int[] counts = {30, 8, 10, 4};
        int[] both = {30, 5, 8, 3};
        int[] oldOnly = {0, 0, 1, 1};
        int[] newOnly = {0, 2, 0, 0};
        for (int g = 0; g < segments.length; g++) {
            int rest = counts[g] - both[g] - oldOnly[g] - newOnly[g];
            for (int i = 0; i < both[g]; i++) rows.add(new Row(segments[g], true, true));
            for (int i = 0; i < oldOnly[g]; i++) rows.add(new Row(segments[g], true, false));
            for (int i = 0; i < newOnly[g]; i++) rows.add(new Row(segments[g], false, true));
            for (int i = 0; i < rest; i++) rows.add(new Row(segments[g], false, false));
        }
        return rows;
    }

    /** Whole percent, half up, with integers only so that every language agrees. */
    static int pct(int part, int whole) {
        return whole != 0 ? (200 * part + whole) / (2 * whole) : 0;
    }

    /** Per segment: cases, right answers, accuracy and the cost of the wrong ones, worst cost first. */
    static List<Line> segmentTable(List<Row> rows, String which) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Row row : rows) {
            boolean ok = which.equals("old") ? row.oldOk() : row.newOk();
            int[] counts = out.computeIfAbsent(row.segment(), k -> new int[2]);
            counts[0]++;
            if (ok) counts[1]++;
        }
        List<Line> table = new ArrayList<>();
        out.forEach((s, c) -> table.add(new Line(s, c[0], c[1], pct(c[1], c[0]), (c[0] - c[1]) * COSTS.getOrDefault(s, 1))));
        table.sort(Comparator.comparingInt((Line l) -> -l.cost()).thenComparing(Line::segment));
        return table;
    }

    /** Nearest rank: the value at rank ceil(p * n / 100) of the sorted list. */
    static int percentile(List<Integer> values, int p) {
        if (values.isEmpty()) return 0;
        List<Integer> ordered = new ArrayList<>(values);
        ordered.sort(null);
        return ordered.get((p * ordered.size() + 99) / 100 - 1);
    }

    /** Two-proportion test at 95 percent, done with integers: z squared = D*D*N / (n1*n2*X*(N-X)), compared with 1.96 squared. */
    static String abVerdict(int x1, int n1, int x2, int n2, int minN) {
        if (n1 < minN || n2 < minN) return "too few cases";
        long bigN = n1 + n2;
        long x = x1 + x2;
        if (x == 0 || x == bigN) return "no clear difference";
        long d = (long) x2 * n1 - (long) x1 * n2;
        if (d * d * bigN * 10000L < 38416L * n1 * n2 * x * (bigN - x)) return "no clear difference";
        return d > 0 ? "new is better" : "old is better";
    }

    static String abVerdict(int x1, int n1, int x2, int n2) {
        return abVerdict(x1, n1, x2, n2, 200);
    }

    /** Ship only when no protected segment lost a right answer and the new version lost fewer than it gained. */
    static Gate shadowGate(List<Row> rows, Set<String> protectedSegments) {
        int lost = 0;
        int gained = 0;
        TreeSet<String> blocked = new TreeSet<>();
        for (Row row : rows) {
            if (row.oldOk() && !row.newOk()) {
                lost++;
                if (protectedSegments.contains(row.segment())) blocked.add(row.segment());
            }
            if (row.newOk() && !row.oldOk()) gained++;
        }
        boolean ship = blocked.isEmpty() && lost <= gained;
        return new Gate(ship ? "ship" : "hold", lost, gained, new ArrayList<>(blocked));
    }

    /** Where to look first: the evidence, then the grounding, then the format, then the task, and the model last. */
    static String diagnose(boolean found, boolean supported, boolean formatOk, boolean passesOnStronger) {
        if (!found) return "retrieval or data";
        if (!supported) return "ungrounded answer";
        if (!formatOk) return "format instructions";
        if (!passesOnStronger) return "prompt or task";
        return "model mismatch";
    }

    /** The cheapest option that meets the accuracy floor and the latency limit, ties by name; none when nothing does. */
    static String chooseModel(List<Option> options, int minAccuracy, int maxP95) {
        return options.stream().filter(o -> o.accuracy() >= minAccuracy && o.p95() <= maxP95)
            .min(Comparator.comparingInt(Option::cost).thenComparing(Option::name)).map(Option::name).orElse("none");
    }

    public static void main(String[] args) {
        List<Row> rows = buildCases();
        for (String[] which : new String[][] {{"old", "current prompt"}, {"new", "new prompt"}}) {
            List<Line> table = segmentTable(rows, which[0]);
            int right = 0;
            int cost = 0;
            for (Line l : table) {
                right += l.right();
                cost += l.cost();
            }
            System.out.println(which[1] + ": " + right + "/" + rows.size() + " right, " + pct(right, rows.size()) + "% overall, error cost " + cost);
            for (Line l : table) System.out.println("  " + String.format("%-13s", l.segment()) + " " + l.right() + "/" + l.cases() + " " + l.percent() + "% cost " + l.cost());
        }
        List<Integer> latencies = List.of(800, 820, 850, 900, 950, 980, 1000, 1100, 1200, 1500, 2400, 4800);
        int total = 0;
        for (int v : latencies) total += v;
        System.out.println("latency ms: mean " + total / latencies.size() + ", p50 " + percentile(latencies, 50) + ", p95 " + percentile(latencies, 95));
        System.out.println("live test, 500 cases each, 410 right against 438: " + abVerdict(410, 500, 438, 500));
        System.out.println("live test, 500 cases each, 410 right against 425: " + abVerdict(410, 500, 425, 500));
        System.out.println("live test, 100 cases each, 82 right against 90: " + abVerdict(82, 100, 90, 100));
        Gate gate = shadowGate(rows, Set.of("refund", "complaint"));
        System.out.println("shadow run: " + gate.decision() + ", lost " + gate.lost() + ", gained " + gate.gained() + ", protected segments hit: " + (gate.blocked().isEmpty() ? "none" : String.join(", ", gate.blocked())));
        System.out.println("diagnose, no chunk had the answer: " + diagnose(false, false, true, true));
        System.out.println("diagnose, a claim no chunk supports: " + diagnose(true, false, true, true));
        System.out.println("diagnose, a reply in the wrong shape: " + diagnose(true, true, false, true));
        System.out.println("diagnose, fails on a stronger model too: " + diagnose(true, true, true, false));
        System.out.println("diagnose, passes only on a stronger model: " + diagnose(true, true, true, true));
        List<Option> options = List.of(new Option("small", 84, 900, 1), new Option("medium", 91, 1800, 3), new Option("large", 95, 4200, 9));
        System.out.println("model for 90% accuracy within 2000 ms: " + chooseModel(options, 90, 2000) + "; for 94% within 2000 ms: " + chooseModel(options, 94, 2000));
    }
}
