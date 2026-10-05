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

    /**
     * TODO 1 of 8 (unlocks e1): the findings about absent stages.
     * Receives the design's {@code stages} map. Returns a list of findings (use {@code finding(rule, severity)}): {@code missing-stage:input}, {@code missing-stage:processing}
     * and {@code missing-stage:output} for each of those stages that is absent or empty, then {@code no-feedback} when {@code feedback} is absent or empty; every one with severity {@code high}.
     * Example: {input: [parse], processing: [act], output: [send]} -> [{rule: no-feedback, severity: high}]
     */
    static List<Map<String, Object>> stageFindings(Map<String, Object> stages) {
        return new ArrayList<>();
    }

    /**
     * TODO 2 of 8 (unlocks e3): the finding about a team.
     * Receives the design. Returns [{@code team-without-independence}, severity {@code high}] when there is more than one agent ({@code agents}, default 1) and either
     * {@code shared_context} is true or {@code parallel_independent} is not true; otherwise an empty list.
     * Example: {agents: 3, shared_context: true, parallel_independent: true} -> one finding; {agents: 1, shared_context: true} -> []
     */
    static List<Map<String, Object>> teamFindings(Map<String, Object> design) {
        return List.of();
    }

    /**
     * TODO 3 of 8 (unlocks e4): the finding about an unapproved write.
     * Receives the design. Returns [{@code unapproved-write}, severity {@code high}] when {@code writes_without_approval} and {@code needs_audit} are both true; otherwise an empty list.
     * Example: {writes_without_approval: true, needs_audit: false} -> []
     */
    static List<Map<String, Object>> writeFindings(Map<String, Object> design) {
        return List.of();
    }

    /**
     * TODO 4 of 8 (unlocks e2): the finding about autonomy.
     * Receives the design. Returns [{@code autonomy-without-need}, severity {@code medium}] when {@code pattern} is in {@code AUTONOMOUS} and {@code path_known} is true; otherwise an empty list.
     * Example: {pattern: agent, path_known: false} -> []
     */
    static List<Map<String, Object>> autonomyFindings(Map<String, Object> design) {
        return List.of();
    }

    /**
     * TODO 5 of 8 (unlocks e5): the finding about unvalidated output.
     * Receives the design's {@code stages} map. Returns [{@code unvalidated-output}, severity {@code medium}] when the {@code output} stage is present and does not contain {@code validate};
     * otherwise an empty list (an absent output stage is already reported as a missing stage).
     * Example: {output: [send]} -> one finding; {output: [validate, send]} -> []
     */
    static List<Map<String, Object>> outputFindings(Map<String, Object> stages) {
        return List.of();
    }

    /**
     * TODO 6 of 8 (unlocks e6): order the findings.
     * Receives a list of findings. Returns them ordered by severity ({@code high} before {@code medium}, see {@code SEVERITY_ORDER}) and then by rule name.
     * Example: [medium autonomy-without-need, high no-feedback] -> the high one first
     */
    static List<Map<String, Object>> orderFindings(List<Map<String, Object>> findings) {
        return findings;
    }

    /**
     * TODO 7 of 8 (unlocks m1 and e6): the verdict.
     * Receives a list of findings. Returns {@code reject} when any is {@code high}, otherwise {@code revise} when any is {@code medium}, otherwise {@code approve}.
     * Example: verdict(List.of()) -> "approve"
     */
    static String verdict(List<Map<String, Object>> findings) {
        return "";
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

    /**
     * TODO 8 of 8 (unlocks e7): the cheapest design that is not rejected.
     * Receives a list of designs, each with {@code name} and {@code cost}. Returns the name of the one with the lowest {@code cost} among those whose {@code verdict(review(d))} is not
     * {@code reject}; equal costs go to the lower name; {@code null} when none qualifies or the list is empty.
     * Example: costs 2 (name "zeta") and 2 (name "alpha"), both sound -> "alpha"
     */
    static String cheapestAdequate(List<Map<String, Object>> designs) {
        return null;
    }
}
