import java.util.List;

/**
 * Audit an extraction run: the accuracy on every document, the accuracy by kind of document, the failure shapes and the first fix.
 * Read statement.md for the fields of a run, of the policy and of the report, then replace the body of audit().
 */
public final class RunAudit {
    public record Run(String id, String kind, String status, boolean correct, boolean invented, boolean retriedAbsent, boolean sumOk) {}

    public record Policy(int target, int minN, int gap) {}

    public record Segment(String kind, int n, int correct, int percent, boolean automate) {}

    public record Report(int n, int valid, int needsReview, int failed, int accuracyAll, int accuracyValidated, boolean meetsTarget, List<Segment> segments,
                         int invented, int wastedRetries, int uncheckedTotals, boolean overstated, String firstFix) {}

    public static Report audit(List<Run> runs, Policy policy) {
        return new Report(0, 0, 0, 0, 0, 0, false, List.of(), 0, 0, 0, false, "");
    }
}
