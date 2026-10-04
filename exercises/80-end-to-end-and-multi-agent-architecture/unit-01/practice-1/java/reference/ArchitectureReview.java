import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** An architecture review against a rubric: the findings, the verdict and the cheapest design that is not rejected. See ../../statement.md. Designs and findings are JSON-like maps. */
final class ArchitectureReview {
    private ArchitectureReview() {}

    static final Map<String, Integer> SEVERITY_ORDER = Map.of("high", 0, "medium", 1);
    static final List<String> AUTONOMOUS = List.of("agent", "multi-agent");

    private static Map<String, Object> finding(String rule, String severity) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("rule", rule);
        m.put("severity", severity);
        return m;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return value == null ? Map.of() : (Map<String, Object>) value;
    }

    private static boolean empty(Object value) {
        return value == null || (value instanceof Collection<?> c && c.isEmpty());
    }

    private static boolean truthy(Object value) {
        return Boolean.TRUE.equals(value);
    }

    static List<Map<String, Object>> review(Map<String, Object> design) {
        Map<String, Object> stages = asMap(design.get("stages"));
        List<Map<String, Object>> findings = new ArrayList<>();
        for (String stage : List.of("input", "processing", "output")) {
            if (empty(stages.get(stage))) findings.add(finding("missing-stage:" + stage, "high"));
        }
        if (empty(stages.get("feedback"))) findings.add(finding("no-feedback", "high"));
        int agents = design.get("agents") == null ? 1 : (Integer) design.get("agents");
        if (agents > 1 && (truthy(design.get("shared_context")) || !truthy(design.get("parallel_independent")))) findings.add(finding("team-without-independence", "high"));
        if (truthy(design.get("writes_without_approval")) && truthy(design.get("needs_audit"))) findings.add(finding("unapproved-write", "high"));
        if (AUTONOMOUS.contains(design.get("pattern")) && truthy(design.get("path_known"))) findings.add(finding("autonomy-without-need", "medium"));
        if (!empty(stages.get("output")) && !((List<?>) stages.get("output")).contains("validate")) findings.add(finding("unvalidated-output", "medium"));
        findings.sort(Comparator.comparingInt((Map<String, Object> f) -> SEVERITY_ORDER.get((String) f.get("severity"))).thenComparing(f -> (String) f.get("rule")));
        return findings;
    }

    static String verdict(List<Map<String, Object>> findings) {
        Set<Object> severities = new java.util.HashSet<>();
        for (Map<String, Object> f : findings) severities.add(f.get("severity"));
        return severities.contains("high") ? "reject" : severities.contains("medium") ? "revise" : "approve";
    }

    static String cheapestAdequate(List<Map<String, Object>> designs) {
        List<Map<String, Object>> adequate = new ArrayList<>();
        for (Map<String, Object> d : designs) if (!verdict(review(d)).equals("reject")) adequate.add(d);
        if (adequate.isEmpty()) return null;
        adequate.sort(Comparator.comparingInt((Map<String, Object> d) -> (Integer) d.get("cost")).thenComparing(d -> (String) d.get("name")));
        return (String) adequate.get(0).get("name");
    }
}
