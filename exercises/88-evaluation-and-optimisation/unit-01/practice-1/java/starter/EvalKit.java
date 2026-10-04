import java.util.List;
import java.util.Map;
import java.util.Set;

/** Evaluation kit: a report by segment, a latency percentile, an A/B verdict, a shadow-run gate, a diagnosis order and a model choice under limits. See ../../statement.md. */
final class EvalKit {
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

    static List<Line> segmentTable(List<Result> results, Map<String, Integer> costs) {
        // TODO: one line per segment, the highest error cost first, ties by segment name.
        return null;
    }

    static int percentile(List<Integer> values, int p) {
        // TODO: the nearest-rank percentile of the values, 0 for no values.
        return -1;
    }

    static String abVerdict(int x1, int n1, int x2, int n2) {
        return abVerdict(x1, n1, x2, n2, 200);
    }

    static String abVerdict(int x1, int n1, int x2, int n2, int minN) {
        // TODO: too few cases, no clear difference, new is better or old is better, at 95 percent.
        return null;
    }

    static Gate shadowGate(List<Paired> pairs, Set<String> protectedSegments) {
        // TODO: the decision, the losses, the gains and the protected segments that lost, for a shadow run of the new version against the old.
        return null;
    }

    static String diagnose(boolean found, boolean supported, boolean formatOk, boolean passesOnStronger) {
        // TODO: retrieval or data, ungrounded answer, format instructions, prompt or task, or model mismatch, in that order.
        return null;
    }

    static String chooseModel(List<Option> options, int minAccuracy, int maxP95) {
        // TODO: the cheapest option that meets the accuracy floor and the latency limit, or none.
        return null;
    }
}
