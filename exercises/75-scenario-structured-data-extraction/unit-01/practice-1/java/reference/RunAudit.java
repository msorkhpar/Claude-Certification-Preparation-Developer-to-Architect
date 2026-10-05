import java.util.List;
import java.util.TreeSet;

/** Audit an extraction run: the accuracy on every document, the accuracy by kind of document, the failure shapes and the first fix. */
public final class RunAudit {
    public record Run(String id, String kind, String status, boolean correct, boolean invented, boolean retriedAbsent, boolean sumOk) {}

    public record Policy(int target, int minN, int gap) {}

    public record Segment(String kind, int n, int correct, int percent, boolean automate) {}

    public record Report(int n, int valid, int needsReview, int failed, int accuracyAll, int accuracyValidated, boolean meetsTarget, List<Segment> segments,
                         int invented, int wastedRetries, int uncheckedTotals, boolean overstated, String firstFix) {}

    /** A whole percentage, rounded half up; 0 when there is nothing to divide. */
    static int percent(int correct, int total) {
        return total > 0 ? (200 * correct + total) / (2 * total) : 0;
    }

    public static Report audit(List<Run> runs, Policy policy) {
        int n = runs.size();
        int valid = (int) runs.stream().filter(r -> r.status().equals("valid")).count();
        int right = (int) runs.stream().filter(Run::correct).count();
        int rightValid = (int) runs.stream().filter(r -> r.status().equals("valid") && r.correct()).count();
        int accuracyAll = percent(right, n);
        int accuracyValidated = percent(rightValid, valid);
        List<Segment> segments = new TreeSet<>(runs.stream().map(Run::kind).toList()).stream().map(kind -> {
            List<Run> group = runs.stream().filter(r -> r.kind().equals(kind)).toList();
            int ok = (int) group.stream().filter(Run::correct).count();
            return new Segment(kind, group.size(), ok, percent(ok, group.size()), group.size() >= policy.minN() && ok * 100 >= policy.target() * group.size());
        }).toList();
        int invented = (int) runs.stream().filter(Run::invented).count();
        int wasted = (int) runs.stream().filter(Run::retriedAbsent).count();
        int unchecked = (int) runs.stream().filter(r -> r.status().equals("valid") && !r.sumOk()).count();
        boolean overstated = accuracyValidated - accuracyAll > policy.gap();
        boolean meetsTarget = n > 0 && right * 100 >= policy.target() * n;
        String firstFix;
        if (n == 0) firstFix = "none";
        else if (invented > 0) firstFix = "make_fields_nullable";
        else if (wasted > 0) firstFix = "stop_retrying_absent";
        else if (unchecked > 0) firstFix = "add_semantic_checks";
        else if (overstated) firstFix = "measure_all_documents";
        else if (!meetsTarget) firstFix = "improve_weak_segments";
        else firstFix = "none";
        return new Report(n, valid, (int) runs.stream().filter(r -> r.status().equals("needs_review")).count(), (int) runs.stream().filter(r -> r.status().equals("failed")).count(),
            accuracyAll, accuracyValidated, meetsTarget, segments, invented, wasted, unchecked, overstated, firstFix);
    }
}
