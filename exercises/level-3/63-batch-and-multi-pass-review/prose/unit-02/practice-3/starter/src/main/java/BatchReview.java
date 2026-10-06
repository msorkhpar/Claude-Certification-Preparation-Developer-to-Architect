import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Batch and multi-pass review decisions: when a batch fits, what to resubmit, how a review is split into passes, and how the passes are combined. See ../../statement.md. */
final class BatchReview {
    private static final System.Logger LOG = System.getLogger(BatchReview.class.getName());
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
        LOG.log(System.Logger.Level.DEBUG, "submissionInterval input: {0}", slaHours);
        // TODO 1 of 8 (finish this to pass m1): the interval. Receives the SLA, the window and the handling time, all in
        //   hours. Return the SLA minus the window minus the handling time. Example: an SLA of 30 hours, defaults -> 4.
        int interval = slaHours;
        // TODO 2 of 8 (finish this to pass e1): the refusal. When the interval is zero or negative, refuse with an error
        //   that says the SLA leaves no room to wait for a batch to fill. Example: an SLA of 26 hours, defaults ->
        //   refused.
        return interval;
    }

    static String chooseApi(boolean blocking, boolean needsToolLoop) {
        // TODO 3 of 8 (finish this to pass e2): the API. Receives whether something is blocked on the result and whether
        //   the job needs a tool loop. Return synchronous when either is true, otherwise batch. Example: blocking false,
        //   tool loop true -> synchronous.
        return "batch";
    }

    static List<Step> resubmissionPlan(List<Result> results, Map<String, Integer> sizes, int limit) {
        List<Step> plan = new ArrayList<>();
        for (Result r : results) {
            // TODO 4 of 8 (finish this to pass e3): the items to resubmit. Skip a result whose kind is succeeded; every
            //   other result is planned by its custom id. Example: results [(a, succeeded), (b, errored)] -> a plan for b
            //   only.
            String action;
            // TODO 5 of 8 (finish this to pass e4): the action for one item. When the item's size is above the limit,
            //   the action is chunk; otherwise fix when its kind is invalid_request; otherwise resubmit. An entry exactly
            //   at the limit is not chunked. Example: size 101, limit 100 -> chunk.
            if (r.kind().equals("invalid_request")) action = "fix";
            else action = "resubmit";
            plan.add(new Step(r.customId(), action));
        }
        return plan;
    }

    static List<Pass> reviewPlan(List<String> files) {
        List<Pass> passes = new ArrayList<>();
        for (String f : files) passes.add(new Pass("local:" + f, List.of(f)));
        // TODO 6 of 8 (finish this to pass e5): the integration pass. After the local passes, when there is more than
        //   one file add one pass named integration over all the files. Example: two files -> local, local, integration;
        //   one file -> local only.
        return passes;
    }

    static List<Merged> mergePasses(List<List<Finding>> passes) {
        Map<List<Object>, String> severity = new LinkedHashMap<>();
        Map<List<Object>, Integer> confidence = new LinkedHashMap<>();
        Map<List<Object>, Set<Integer>> seen = new LinkedHashMap<>();
        for (int number = 0; number < passes.size(); number++) {
            for (Finding f : passes.get(number)) {
                List<Object> key = List.of(f.file(), f.line(), f.issue());
                // TODO 7 of 8 (finish this to pass e6): the merge of one finding seen again. When the same finding
                //   (file, line, issue) is reported by another pass, keep the higher of the two severities (low, medium,
                //   high) and the lower of the two confidences. Example: medium at 90 and high at 70 -> high at 70.
                severity.putIfAbsent(key, f.severity());
                confidence.putIfAbsent(key, f.confidence());
                seen.computeIfAbsent(key, k -> new LinkedHashSet<>()).add(number);
            }
        }
        List<Merged> out = new ArrayList<>();
        for (List<Object> key : severity.keySet()) {
            int count = seen.get(key).size();
            int conf = confidence.get(key);
            // TODO 8 of 8 (finish this to pass e7): the route of a merged finding. Receives the number of passes that
            //   reported it and its confidence. Return accept when at least 2 passes reported it and the confidence is at
            //   least 80, otherwise verify. Example: 2 passes, confidence 79 -> verify.
            out.add(new Merged((String) key.get(0), (Integer) key.get(1), (String) key.get(2), severity.get(key), count, conf, "verify"));
        }
        return out;
    }
}
