import java.util.List;
import java.util.Map;

/** An architecture review against a rubric: the findings, the verdict and the cheapest design that is not rejected. See ../../statement.md. Designs and findings are JSON-like maps. */
final class ArchitectureReview {
    private ArchitectureReview() {}

    /** High findings come first. */
    static final Map<String, Integer> SEVERITY_ORDER = Map.of("high", 0, "medium", 1);
    static final List<String> AUTONOMOUS = List.of("agent", "multi-agent");

    static List<Map<String, Object>> review(Map<String, Object> design) {
        // TODO: the findings of the rubric, each {rule, severity}, ordered by severity and then by rule.
        return null;
    }

    static String verdict(List<Map<String, Object>> findings) {
        // TODO: "reject", "revise" or "approve" from the worst severity among the findings.
        return null;
    }

    static String cheapestAdequate(List<Map<String, Object>> designs) {
        // TODO: the name of the cheapest design that is not rejected, or null.
        return null;
    }
}
