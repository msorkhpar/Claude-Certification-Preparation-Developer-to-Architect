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

    /**
     * TODO 1 of 6 (unlocks m1, e1 and e8): a whole percentage, rounded half up.
     * Receives the correct count and the total. Returns {@code (200 * correct + total) / (2 * total)} in integer arithmetic, and 0 when the total is 0.
     * Example: percent(2, 3) -> 67, percent(1, 3) -> 33, percent(0, 0) -> 0
     */
    static int percent(int correct, int total) {
        return 0;
    }

    /**
     * TODO 2 of 6 (unlocks m1, e3 and e4): one entry per kind of document.
     * Receives the runs and the policy. Returns a list sorted by kind of Segment(kind, n, correct, percent, automate); {@code automate} needs at
     * least {@code minN} documents of the kind and {@code correct * 100 >= target * n} for it.
     * Example: 9 of 10 typed documents with minN 5 and target 90 -> [Segment("typed", 10, 9, 90, true)]
     */
    static List<Segment> segmentsOf(List<Run> runs, Policy policy) {
        return List.of();
    }

    /**
     * TODO 3 of 6 (unlocks m1 and e6): count the failure shapes in documents.
     * Receives the runs. Returns {invented, wastedRetries, uncheckedTotals}: documents flagged {@code invented}, documents flagged
     * {@code retriedAbsent}, and {@code valid} documents whose {@code sumOk} is false (a document never accepted is not counted).
     * Example: one valid document with sumOk false and one failed document with sumOk false -> {0, 0, 1}
     */
    static int[] failureShapes(List<Run> runs) {
        return new int[] {0, 0, 0};
    }

    /**
     * TODO 4 of 6 (unlocks m1, e1 and e2): does the run meet the target?
     * Receives the correct count, the document count and the target in percent. Returns true when there is at least one document
     * and {@code right * 100 >= target * n}. Example: meets(9, 10, 90) -> true, meets(8, 10, 90) -> false, meets(0, 0, 90) -> false
     */
    static boolean meets(int right, int n, int target) {
        return false;
    }

    /**
     * TODO 5 of 6 (unlocks m1 and e5): is the figure overstated?
     * Receives both accuracies and the gap in points. Returns true only when the validated accuracy exceeds the all-document
     * accuracy by more than the gap. Example: isOverstated(100, 95, 5) -> false, isOverstated(100, 94, 5) -> true
     */
    static boolean isOverstated(int accuracyValidated, int accuracyAll, int gap) {
        return false;
    }

    /**
     * TODO 6 of 6 (unlocks m1, e1 and e7): the first fix that applies.
     * Receives the document count, the three shape counts, {@code overstated} and {@code meetsTarget}. Returns {@code none} for an empty run, else the
     * first that applies of make_fields_nullable (invented), stop_retrying_absent (wasted), add_semantic_checks (unchecked),
     * measure_all_documents (overstated), improve_weak_segments (target not met), and {@code none}.
     * Example: chooseFix(3, 0, 1, 1, false, true) -> "stop_retrying_absent"
     */
    static String chooseFix(int n, int invented, int wasted, int unchecked, boolean overstated, boolean meetsTarget) {
        return "";
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
