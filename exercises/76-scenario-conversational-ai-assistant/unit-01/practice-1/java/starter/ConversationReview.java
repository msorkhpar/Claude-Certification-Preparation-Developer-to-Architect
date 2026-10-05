import java.util.List;

/**
 * Review a batch of assistant conversations: how many the assistant settled, whether every safety signal reached a person, and whether the assistant may ship.
 * Read statement.md for the fields of a conversation, of the policy and of the report, then replace the body of review().
 */
public final class ConversationReview {
    public record Conversation(String id, String segment, int turns, boolean resolved, String handoff, boolean neededPerson, boolean repeated, boolean risk) {}

    public record Policy(int maxTurns, int maxRepeat, int minResolved, int minN) {}

    public record Segment(String segment, int n, int resolved, int percent, boolean weak) {}

    public record Report(int n, int resolved, int resolvedPct, int safetyMissed, int overlong, int repeatPct, boolean repeatOk, int overEscalated, int underEscalated,
                         List<Segment> segments, String verdict, String reason) {}

    public static Report review(List<Conversation> conversations, Policy policy) {
        return new Report(0, 0, 0, 0, 0, 0, false, 0, 0, List.of(), "", "");
    }
}
