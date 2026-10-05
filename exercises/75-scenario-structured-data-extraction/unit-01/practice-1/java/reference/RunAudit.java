import java.util.List;
import java.util.TreeSet;

/** Audit an extraction run: the accuracy on every document, the accuracy by kind of document, the failure shapes and the first fix. */
public final class RunAudit {
    private static final System.Logger LOG = System.getLogger(RunAudit.class.getName());
    public record Run(String id, String kind, String status, boolean correct, boolean invented, boolean retriedAbsent, boolean sumOk) {}

    public record Policy(int target, int minN, int gap) {}

    public record Segment(String kind, int n, int correct, int percent, boolean automate) {}

    public record Report(int n, int valid, int needsReview, int failed, int accuracyAll, int accuracyValidated, boolean meetsTarget, List<Segment> segments,
                         int invented, int wastedRetries, int uncheckedTotals, boolean overstated, String firstFix) {}

    /** A whole percentage, rounded half up; 0 when there is nothing to divide. */
    static int percent(int correct, int total) {
        return total > 0 ? (200 * correct + total) / (2 * total) : 0;
    }

    /** One entry per kind, sorted by kind. */
    static List<Segment> segmentsOf(List<Run> runs, Policy policy) {
        return new TreeSet<>(runs.stream().map(Run::kind).toList()).stream().map(kind -> {
            List<Run> group = runs.stream().filter(r -> r.kind().equals(kind)).toList();
            int ok = (int) group.stream().filter(Run::correct).count();
            return new Segment(kind, group.size(), ok, percent(ok, group.size()), group.size() >= policy.minN() && ok * 100 >= policy.target() * group.size());
        }).toList();
    }

    /** {invented, wastedRetries, uncheckedTotals} counted in documents. */
    static int[] failureShapes(List<Run> runs) {
        int invented = (int) runs.stream().filter(Run::invented).count();
        int wasted = (int) runs.stream().filter(Run::retriedAbsent).count();
        int unchecked = (int) runs.stream().filter(r -> r.status().equals("valid") && !r.sumOk()).count();
        return new int[] {invented, wasted, unchecked};
    }

    /** True when the run has a document and right * 100 >= target * n. */
    static boolean meets(int right, int n, int target) {
        return n > 0 && right * 100 >= target * n;
    }

    /** True when the validated accuracy exceeds the all-document accuracy by more than the gap. */
    static boolean isOverstated(int accuracyValidated, int accuracyAll, int gap) {
        return accuracyValidated - accuracyAll > gap;
    }

    /** The first fix that applies, in the order of the statement. */
    static String chooseFix(int n, int invented, int wasted, int unchecked, boolean overstated, boolean meetsTarget) {
        if (n == 0) return "none";
        if (invented > 0) return "make_fields_nullable";
        if (wasted > 0) return "stop_retrying_absent";
        if (unchecked > 0) return "add_semantic_checks";
        if (overstated) return "measure_all_documents";
        if (!meetsTarget) return "improve_weak_segments";
        return "none";
    }

    public static Report audit(List<Run> runs, Policy policy) {
        LOG.log(System.Logger.Level.DEBUG, "audit input: {0}", runs);
        int n = runs.size();
        int valid = (int) runs.stream().filter(r -> r.status().equals("valid")).count();
        int right = (int) runs.stream().filter(Run::correct).count();
        int rightValid = (int) runs.stream().filter(r -> r.status().equals("valid") && r.correct()).count();
        int accuracyAll = percent(right, n);
        int accuracyValidated = percent(rightValid, valid);
        int[] shapes = failureShapes(runs);
        boolean overstated = isOverstated(accuracyValidated, accuracyAll, policy.gap());
        boolean meetsTarget = meets(right, n, policy.target());
        return new Report(n, valid, (int) runs.stream().filter(r -> r.status().equals("needs_review")).count(), (int) runs.stream().filter(r -> r.status().equals("failed")).count(),
            accuracyAll, accuracyValidated, meetsTarget, segmentsOf(runs, policy), shapes[0], shapes[1], shapes[2], overstated, chooseFix(n, shapes[0], shapes[1], shapes[2], overstated, meetsTarget));
    }
}
