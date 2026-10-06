import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** An architecture review against a rubric: the findings, the verdict and the cheapest design that is not rejected. See ../../statement.md. Designs and findings are JSON-like maps. */
final class ArchitectureReview {
    private static final System.Logger LOG = System.getLogger(ArchitectureReview.class.getName());
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

    /** missing-stage:<stage> for each absent stage, then no-feedback when the feedback stage is absent; all high. */
    static List<Map<String, Object>> stageFindings(Map<String, Object> stages) {
        List<Map<String, Object>> found = new ArrayList<>();
        for (String stage : List.of("input", "processing", "output")) {
            if (empty(stages.get(stage))) found.add(finding("missing-stage:" + stage, "high"));
        }
        if (empty(stages.get("feedback"))) found.add(finding("no-feedback", "high"));
        return found;
    }

    /** team-without-independence (high) for more than one agent with a shared context or parts that are not independent. */
    static List<Map<String, Object>> teamFindings(Map<String, Object> design) {
        int agents = design.get("agents") == null ? 1 : (Integer) design.get("agents");
        boolean team = agents > 1 && (truthy(design.get("shared_context")) || !truthy(design.get("parallel_independent")));
        return team ? List.of(finding("team-without-independence", "high")) : List.of();
    }

    /** unapproved-write (high) when the design writes without approval and an audit is needed. */
    static List<Map<String, Object>> writeFindings(Map<String, Object> design) {
        return truthy(design.get("writes_without_approval")) && truthy(design.get("needs_audit")) ? List.of(finding("unapproved-write", "high")) : List.of();
    }

    /** autonomy-without-need (medium) when an agent or a team is used on a known path. */
    static List<Map<String, Object>> autonomyFindings(Map<String, Object> design) {
        return AUTONOMOUS.contains(design.get("pattern")) && truthy(design.get("path_known")) ? List.of(finding("autonomy-without-need", "medium")) : List.of();
    }

    /** unvalidated-output (medium) when the output stage is present and has no validate step. */
    static List<Map<String, Object>> outputFindings(Map<String, Object> stages) {
        return !empty(stages.get("output")) && !((List<?>) stages.get("output")).contains("validate") ? List.of(finding("unvalidated-output", "medium")) : List.of();
    }

    /** High first, then by rule name. */
    static List<Map<String, Object>> orderFindings(List<Map<String, Object>> findings) {
        List<Map<String, Object>> ordered = new ArrayList<>(findings);
        ordered.sort(Comparator.comparingInt((Map<String, Object> f) -> SEVERITY_ORDER.get((String) f.get("severity"))).thenComparing(f -> (String) f.get("rule")));
        return ordered;
    }

    /** reject for any high finding, revise for any medium finding, otherwise approve. */
    static String verdict(List<Map<String, Object>> findings) {
        Set<Object> severities = new java.util.HashSet<>();
        for (Map<String, Object> f : findings) severities.add(f.get("severity"));
        return severities.contains("high") ? "reject" : severities.contains("medium") ? "revise" : "approve";
    }

    static List<Map<String, Object>> review(Map<String, Object> design) {
        LOG.log(System.Logger.Level.DEBUG, "review input: {0}", design);
        Map<String, Object> stages = asMap(design.get("stages"));
        List<Map<String, Object>> found = new ArrayList<>(stageFindings(stages));
        found.addAll(teamFindings(design));
        found.addAll(writeFindings(design));
        found.addAll(autonomyFindings(design));
        found.addAll(outputFindings(stages));
        return orderFindings(found);
    }

    /** The name of the cheapest design whose verdict is not reject; ties go to the lower name; null when none qualifies. */
    static String cheapestAdequate(List<Map<String, Object>> designs) {
        List<Map<String, Object>> adequate = new ArrayList<>();
        for (Map<String, Object> d : designs) if (!verdict(review(d)).equals("reject")) adequate.add(d);
        if (adequate.isEmpty()) return null;
        adequate.sort(Comparator.comparingInt((Map<String, Object> d) -> (Integer) d.get("cost")).thenComparing(d -> (String) d.get("name")));
        return (String) adequate.get(0).get("name");
    }
}
