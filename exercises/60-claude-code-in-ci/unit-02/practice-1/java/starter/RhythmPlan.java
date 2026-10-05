import java.util.Map;

/** Which way of running Claude Code unattended or on a rhythm a job calls for, and why. See ../../statement.md. */
final class RhythmPlan {
    private RhythmPlan() {}

    record Choice(String mechanism, String reason, int intervalMinutes) {}

    static Choice choose(Map<String, Object> job) {
        // TODO: the mechanism, the reason code and the interval in minutes for a job (a map whose missing keys take the defaults in the statement).
        return new Choice("", "", 0);
    }
}
