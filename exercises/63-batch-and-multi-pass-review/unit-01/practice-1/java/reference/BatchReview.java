import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        int interval = slaHours - windowHours - handlingHours;
        if (interval <= 0) throw new IllegalArgumentException("the SLA leaves no room to wait for a batch to fill");
        return interval;
    }

    static String chooseApi(boolean blocking, boolean needsToolLoop) {
        return blocking || needsToolLoop ? "synchronous" : "batch";
    }

    static List<Step> resubmissionPlan(List<Result> results, Map<String, Integer> sizes, int limit) {
        List<Step> plan = new ArrayList<>();
        for (Result r : results) {
            if (r.kind().equals("succeeded")) continue;
            String action;
            if (sizes.getOrDefault(r.customId(), 0) > limit) action = "chunk";
            else if (r.kind().equals("invalid_request")) action = "fix";
            else action = "resubmit";
            plan.add(new Step(r.customId(), action));
        }
        return plan;
    }

    static List<Pass> reviewPlan(List<String> files) {
        List<Pass> passes = new ArrayList<>();
        for (String f : files) passes.add(new Pass("local:" + f, List.of(f)));
        if (files.size() > 1) passes.add(new Pass("integration", List.copyOf(files)));
        return passes;
    }

    static List<Merged> mergePasses(List<List<Finding>> passes) {
        Map<List<Object>, String> severity = new LinkedHashMap<>();
        Map<List<Object>, Integer> confidence = new LinkedHashMap<>();
        Map<List<Object>, Set<Integer>> seen = new LinkedHashMap<>();
        for (int number = 0; number < passes.size(); number++) {
            for (Finding f : passes.get(number)) {
                List<Object> key = List.of(f.file(), f.line(), f.issue());
                severity.merge(key, f.severity(), (old, now) -> SEVERITIES.indexOf(now) > SEVERITIES.indexOf(old) ? now : old);
                confidence.merge(key, f.confidence(), Math::min);
                seen.computeIfAbsent(key, k -> new LinkedHashSet<>()).add(number);
            }
        }
        List<Merged> out = new ArrayList<>();
        for (List<Object> key : severity.keySet()) {
            int count = seen.get(key).size();
            int conf = confidence.get(key);
            out.add(new Merged((String) key.get(0), (Integer) key.get(1), (String) key.get(2), severity.get(key), count, conf, count >= 2 && conf >= 80 ? "accept" : "verify"));
        }
        return out;
    }
}
