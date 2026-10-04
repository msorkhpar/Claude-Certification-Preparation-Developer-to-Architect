import java.util.List;

/** Audit a support agent's recorded sessions: the rates, the failure shapes and the first fix. */
public final class Audit {
    /** One call the agent made: the tool, whether it succeeded and the tool it should have used (null when not known). */
    public record Step(String tool, boolean ok, String rightTool) {}

    public record Session(String id, List<Step> steps, String outcome, boolean needsHuman, int refundCents, int limitCents) {}

    public record Report(int sessions, int resolved, double fcr, boolean meetsTarget, int overEscalated, int underEscalated,
                         int skippedPrerequisite, int wrongTool, int overLimitRefunds, String diagnosis) {}

    static final List<String> PROTECTED = List.of("lookup_order", "process_refund");
    static final double TARGET = 0.8;

    /** True when an order or refund call came before the first successful get_customer call. */
    static boolean skippedPrerequisite(List<Step> steps) {
        boolean verified = false;
        for (Step step : steps) {
            if (step.tool().equals("get_customer") && step.ok()) verified = true;
            else if (PROTECTED.contains(step.tool()) && !verified) return true;
        }
        return false;
    }

    public static Report audit(List<Session> sessions) {
        int n = sessions.size();
        int resolved = (int) sessions.stream().filter(s -> s.outcome().equals("resolved")).count();
        int over = (int) sessions.stream().filter(s -> s.outcome().equals("escalated") && !s.needsHuman()).count();
        int under = (int) sessions.stream().filter(s -> s.outcome().equals("resolved") && s.needsHuman()).count();
        int skipped = (int) sessions.stream().filter(s -> skippedPrerequisite(s.steps())).count();
        int wrong = (int) sessions.stream().filter(s -> s.steps().stream().anyMatch(st -> st.rightTool() != null && !st.tool().equals(st.rightTool()))).count();
        int overLimit = (int) sessions.stream().filter(s -> s.outcome().equals("resolved") && s.refundCents() > s.limitCents()).count();
        double fcr = n > 0 ? Math.round((double) resolved / n * 1000) / 1000.0 : 0.0;
        String diagnosis;
        if (skipped > 0 || overLimit > 0) diagnosis = "enforce_in_code";
        else if (wrong > 0 && wrong >= over + under) diagnosis = "rewrite_tool_descriptions";
        else if (over + under > 0) diagnosis = "write_escalation_criteria";
        else diagnosis = "none";
        return new Report(n, resolved, fcr, fcr >= TARGET, over, under, skipped, wrong, overLimit, diagnosis);
    }
}
