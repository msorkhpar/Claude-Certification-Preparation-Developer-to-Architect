import java.util.List;

/** Audit a support agent's recorded sessions: the rates, the failure shapes and the first fix. */
public final class Audit {
    private static final System.Logger LOG = System.getLogger(Audit.class.getName());

    /** One call the agent made: the tool, whether it succeeded and the tool it should have used (null when not known). */
    public record Step(String tool, boolean ok, String rightTool) {}

    public record Session(String id, List<Step> steps, String outcome, boolean needsHuman, int refundCents, int limitCents) {}

    public record Report(int sessions, int resolved, double fcr, boolean meetsTarget, int overEscalated, int underEscalated,
                         int skippedPrerequisite, int wrongTool, int overLimitRefunds, String diagnosis) {}

    static final List<String> PROTECTED = List.of("lookup_order", "process_refund");
    static final double TARGET = 0.8;

    /**
     * TODO 1 of 5 (unlocks e2): did an order or refund call come before the first successful get_customer call?
     * Receives a session's steps. Returns true or false; a failed get_customer does not identify anyone.
     * Example: [lookup_order, get_customer] -> true, [get_customer, lookup_order] -> false
     */
    static boolean skippedPrerequisite(List<Step> steps) {
        return false;
    }

    /**
     * TODO 2 of 5 (unlocks e6): does a step have a known right tool that differs from the tool used?
     * Receives a session's steps (rightTool is null when not known). Returns true for at least one such step, false otherwise.
     * Example: [Step("get_customer", true, "lookup_order")] -> true; a step with a null rightTool never counts.
     */
    static boolean hasWrongTool(List<Step> steps) {
        return false;
    }

    /**
     * TODO 3 of 5 (unlocks e3): was a refund above the limit actually made?
     * Receives one session. Returns true only when its outcome is "resolved" and refundCents is above limitCents.
     * Example: resolved, refund 10001, limit 10000 -> true; escalated with the same refund -> false
     */
    static boolean isOverLimit(Session session) {
        return false;
    }

    /**
     * TODO 4 of 5 (unlocks e1 and e5): the first contact rate.
     * Receives the resolved count and the session count. Returns resolved / n rounded to three decimals, 0.0 when n is 0.
     * Example: firstContactRate(2, 3) -> 0.667
     */
    static double firstContactRate(int resolved, int n) {
        return 0.0;
    }

    /**
     * TODO 5 of 5 (unlocks e4 and e1): the first fix, by the order in the statement.
     * Receives the counts of skipped prerequisites, over-limit refunds, wrong-tool sessions, over- and under-escalations.
     * Returns "enforce_in_code", "rewrite_tool_descriptions", "write_escalation_criteria" or "none".
     * Example: diagnose(0, 0, 2, 1, 0) -> "rewrite_tool_descriptions"
     */
    static String diagnose(int skipped, int overLimit, int wrong, int over, int under) {
        return "";
    }

    public static Report audit(List<Session> sessions) {
        LOG.log(System.Logger.Level.DEBUG, "audit input: {0}", sessions);
        int n = sessions.size();
        int resolved = (int) sessions.stream().filter(s -> s.outcome().equals("resolved")).count();
        int over = (int) sessions.stream().filter(s -> s.outcome().equals("escalated") && !s.needsHuman()).count();
        int under = (int) sessions.stream().filter(s -> s.outcome().equals("resolved") && s.needsHuman()).count();
        int skipped = (int) sessions.stream().filter(s -> skippedPrerequisite(s.steps())).count();
        int wrong = (int) sessions.stream().filter(s -> hasWrongTool(s.steps())).count();
        int overLimit = (int) sessions.stream().filter(Audit::isOverLimit).count();
        double fcr = firstContactRate(resolved, n);
        return new Report(n, resolved, fcr, fcr >= TARGET, over, under, skipped, wrong, overLimit, diagnose(skipped, overLimit, wrong, over, under));
    }
}
