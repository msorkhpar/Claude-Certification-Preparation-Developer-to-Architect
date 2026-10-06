import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Launch review: the findings of a design against the seven domains, the verdict, the scorecard and the accuracy a design needs. See ../../statement.md. */
final class LaunchReview {
    private static final System.Logger LOG = System.getLogger(LaunchReview.class.getName());
    private LaunchReview() {}

    private static final Map<String, Integer> SEVERITY = Map.of("high", 0, "medium", 1, "low", 2);
    private static final List<String> DOMAINS = List.of("P1", "P2", "P3", "P4", "P5", "P6", "P7");

    private static void add(List<String> found, boolean condition, String severity, String domain, String rule) {
        if (condition) found.add(severity + " " + domain + " " + rule);
    }

    /**
     * TODO 1 of 8 (unlocks e1, e4 and e7): the rules of domain P1.
     * Receives the flags and the numbers and reports each rule it finds through `add`. The rules: missing-feedback, autonomy-without-need and team-below-price from the statement's table. Example: missing-feedback: the flag feedback_loop is absent -> one finding `high P1 missing-feedback`.
     */
    private static void p1Rules(List<String> found, Set<String> f, Map<String, Integer> n) {
    }

    /**
     * TODO 2 of 8 (unlocks e4 and e8): the rules of domain P3.
     * Receives the flags and the numbers and reports each rule it finds through `add`. The rules: filter-after-ranking, stale-index, tool-bloat and agent-rights-only. Example: stale-index: the flag replace_on_change is absent -> one finding `high P3 stale-index`.
     */
    private static void p3Rules(List<String> found, Set<String> f, Map<String, Integer> n) {
    }

    /**
     * TODO 3 of 8 (unlocks e1, e3 and e4): the rules of domain P4.
     * Receives the flags and the numbers and reports each rule it finds through `add`. The rules: no-protected-segment, small-eval-set, no-way-back and big-bang-rollout. Example: no-way-back: the flag rollback is absent -> one finding `high P4 no-way-back`.
     */
    private static void p4Rules(List<String> found, Set<String> f, Map<String, Integer> n) {
    }

    /**
     * TODO 4 of 8 (unlocks e4, e7 and e8): the rules of domain P5.
     * Receives the flags and the numbers and reports each rule it finds through `add`. The rules: identifiers-reach-model, residency-unmet, audit-keeps-content, irreversible-without-person and retention-outside-window. Example: irreversible-without-person: irreversible_action present and human_step absent -> `high P5 irreversible-without-person`.
     */
    private static void p5Rules(List<String> found, Set<String> f, Map<String, Integer> n) {
    }

    private static void p2Rules(List<String> found, Set<String> f, Map<String, Integer> n) {
        add(found, f.contains("volatile_prefix"), "medium", "P2", "volatile-prefix");
        add(found, !f.contains("model_measured"), "low", "P2", "model-not-measured");
    }

    private static void p6Rules(List<String> found, Set<String> f, Map<String, Integer> n) {
        add(found, !f.contains("owner"), "medium", "P6", "no-accountable-owner");
        add(found, n.getOrDefault("latency_ms", 0) <= 0 || n.getOrDefault("availability_tenths", 0) <= 0, "medium", "P6", "sla-without-numbers");
        add(found, !f.contains("accuracy_stated"), "low", "P6", "accuracy-unstated");
    }

    private static void p7Rules(List<String> found, Set<String> f, Map<String, Integer> n) {
        add(found, n.getOrDefault("team_size", 0) > 10 && !f.contains("managed_settings"), "medium", "P7", "unmanaged-team-settings");
    }

    /**
     * TODO 5 of 8 (unlocks e3): order the findings.
     * Receives the findings. Returns them ordered by severity (high, medium, low), then by domain, then by rule id in alphabetical order. Example: low P2 a, high P6 z, high P1 y -> high P1 y, high P6 z, low P2 a
     */
    private static List<String> order(List<String> found) {
        return found;
    }

    /** The findings, each "<severity> <domain> <rule>", high first, then by domain, then by rule. */
    static List<String> launchReview(Set<String> f, Map<String, Integer> n) {
        LOG.log(System.Logger.Level.DEBUG, "launchReview input: {0}", f);
        List<String> found = new ArrayList<>();
        p1Rules(found, f, n);
        p2Rules(found, f, n);
        p3Rules(found, f, n);
        p4Rules(found, f, n);
        p5Rules(found, f, n);
        p6Rules(found, f, n);
        p7Rules(found, f, n);
        return order(found);
    }

    /**
     * TODO 6 of 8 (unlocks e1 and e2): the verdict.
     * Receives the findings. Returns `reject` when any is high, `revise` when none is high and any is medium, `approve` otherwise. Example: [low P2 a] -> approve
     */
    static String verdict(List<String> findings) {
        return "";
    }

    /**
     * TODO 7 of 8 (unlocks e5): the scorecard.
     * Receives the findings. Returns the seven counts of the findings of P1 to P7 in that order. Example: [high P1 a, low P7 c] -> [1, 0, 0, 0, 0, 0, 1]
     */
    static List<Integer> scorecard(List<String> findings) {
        return List.of();
    }

    /**
     * TODO 8 of 8 (unlocks m1 and e6): the accuracy a design needs.
     * Receives the cost of an error and the cost of a check. Returns 100 minus the check cost as a percent of the error cost, the percent rounded up, never below 0, and 0 when an error costs nothing or less. Example: neededAccuracy(250, 5) -> 98
     */
    static int neededAccuracy(int errorCost, int reviewCost) {
        return -1;
    }
}
