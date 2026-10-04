import java.util.List;

/**
 * Audit a support agent's recorded sessions: the rates, the failure shapes and the first fix.
 * Read statement.md for the fields of a session and of the report, then replace the body of audit().
 */
public final class Audit {
    /** One call the agent made: the tool, whether it succeeded and the tool it should have used (null when not known). */
    public record Step(String tool, boolean ok, String rightTool) {}

    public record Session(String id, List<Step> steps, String outcome, boolean needsHuman, int refundCents, int limitCents) {}

    public record Report(int sessions, int resolved, double fcr, boolean meetsTarget, int overEscalated, int underEscalated,
                         int skippedPrerequisite, int wrongTool, int overLimitRefunds, String diagnosis) {}

    public static Report audit(List<Session> sessions) {
        return new Report(0, 0, 0.0, false, 0, 0, 0, 0, 0, "");
    }
}
