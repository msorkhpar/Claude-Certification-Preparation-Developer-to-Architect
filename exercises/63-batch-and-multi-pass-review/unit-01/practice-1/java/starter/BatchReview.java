import java.util.List;
import java.util.Map;

/** Batch and multi-pass review decisions: when a batch fits, what to resubmit, how a review is split into passes, and how the passes are combined. See ../../statement.md. */
final class BatchReview {
    private BatchReview() {}

    static final List<String> SEVERITIES = List.of("low", "medium", "high");

    record Result(String customId, String kind) {}

    record Step(String customId, String action) {}

    record Pass(String name, List<String> files) {}

    record Finding(String file, int line, String severity, String issue, int confidence) {}

    record Merged(String file, int line, String issue, String severity, int passes, int confidence, String route) {}

    static int submissionInterval(int slaHours) {
        return submissionInterval(slaHours, 24, 2);
    }

    static int submissionInterval(int slaHours, int windowHours, int handlingHours) {
        // TODO: the hours between submissions that still keep every item inside the SLA; refuse an SLA with no room.
        return 0;
    }

    static String chooseApi(boolean blocking, boolean needsToolLoop) {
        // TODO: "synchronous" or "batch" for a workload.
        return null;
    }

    static List<Step> resubmissionPlan(List<Result> results, Map<String, Integer> sizes, int limit) {
        // TODO: for the items that did not succeed, the action of each: resubmit, fix or chunk.
        return null;
    }

    static List<Pass> reviewPlan(List<String> files) {
        // TODO: the passes of a multi-file review.
        return null;
    }

    static List<Merged> mergePasses(List<List<Finding>> passes) {
        // TODO: combine the findings of independent passes into one list, each with its route.
        return null;
    }
}
