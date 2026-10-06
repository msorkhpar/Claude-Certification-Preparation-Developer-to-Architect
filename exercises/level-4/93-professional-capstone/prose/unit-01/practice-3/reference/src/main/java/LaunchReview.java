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

    private static void p1Rules(List<String> found, Set<String> f, Map<String, Integer> n) {
        add(found, !f.contains("feedback_loop"), "high", "P1", "missing-feedback");
        add(found, (f.contains("agent") || f.contains("team")) && f.contains("path_known"), "medium", "P1", "autonomy-without-need");
        add(found, f.contains("team") && n.getOrDefault("team_value_chats", 0) < 15, "medium", "P1", "team-below-price");
    }

    private static void p3Rules(List<String> found, Set<String> f, Map<String, Integer> n) {
        add(found, f.contains("filter_after_ranking"), "high", "P3", "filter-after-ranking");
        add(found, !f.contains("replace_on_change"), "high", "P3", "stale-index");
        add(found, n.getOrDefault("tool_tokens", 0) > 10000 && !f.contains("deferral"), "medium", "P3", "tool-bloat");
        add(found, f.contains("agent_rights_only"), "high", "P3", "agent-rights-only");
    }

    private static void p4Rules(List<String> found, Set<String> f, Map<String, Integer> n) {
        add(found, !f.contains("protected_segment"), "medium", "P4", "no-protected-segment");
        add(found, n.getOrDefault("eval_cases", 0) < 20, "low", "P4", "small-eval-set");
        add(found, !f.contains("rollback"), "high", "P4", "no-way-back");
        add(found, n.getOrDefault("rollout_stages", 0) < 3, "medium", "P4", "big-bang-rollout");
    }

    private static void p5Rules(List<String> found, Set<String> f, Map<String, Integer> n) {
        add(found, f.contains("pii_reaches_model"), "high", "P5", "identifiers-reach-model");
        add(found, f.contains("residency_unmet"), "high", "P5", "residency-unmet");
        add(found, f.contains("audit_keeps_content"), "medium", "P5", "audit-keeps-content");
        add(found, f.contains("irreversible_action") && !f.contains("human_step"), "high", "P5", "irreversible-without-person");
        add(found, n.getOrDefault("retain_days", 0) < n.getOrDefault("floor_days", 0) || n.getOrDefault("retain_days", 0) > n.getOrDefault("ceiling_days", 0), "medium", "P5", "retention-outside-window");
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

    private static List<String> order(List<String> found) {
        found.sort(Comparator.comparing((String s) -> SEVERITY.get(s.split(" ")[0])).thenComparing(s -> s.split(" ")[1]).thenComparing(s -> s.split(" ")[2]));
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

    /** reject for any high finding, revise for any medium one, otherwise approve. */
    static String verdict(List<String> findings) {
        if (findings.stream().anyMatch(x -> x.startsWith("high "))) return "reject";
        if (findings.stream().anyMatch(x -> x.startsWith("medium "))) return "revise";
        return "approve";
    }

    /** The number of findings in each domain, P1 to P7. */
    static List<Integer> scorecard(List<String> findings) {
        List<Integer> out = new ArrayList<>();
        for (String d : DOMAINS) out.add((int) findings.stream().filter(x -> x.split(" ")[1].equals(d)).count());
        return out;
    }

    /** The break-even accuracy in whole percent, rounded up on the cost side; 0 when an error costs nothing or no more than a check. */
    static int neededAccuracy(int errorCost, int reviewCost) {
        if (errorCost <= 0) return 0;
        int needed = (100 * reviewCost + errorCost - 1) / errorCost;
        return Math.max(0, 100 - needed);
    }
}
