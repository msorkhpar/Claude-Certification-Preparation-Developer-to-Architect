import java.util.List;
import java.util.TreeSet;

/** Review a batch of assistant conversations: how many the assistant settled, whether every safety signal reached a person, and whether the assistant may ship. */
public final class ConversationReview {
    public record Conversation(String id, String segment, int turns, boolean resolved, String handoff, boolean neededPerson, boolean repeated, boolean risk) {}

    public record Policy(int maxTurns, int maxRepeat, int minResolved, int minN) {}

    public record Segment(String segment, int n, int resolved, int percent, boolean weak) {}

    public record Report(int n, int resolved, int resolvedPct, int safetyMissed, int overlong, int repeatPct, boolean repeatOk, int overEscalated, int underEscalated,
                         List<Segment> segments, String verdict, String reason) {}

    /** A whole percentage, rounded half up; 0 when there is nothing to divide. */
    static int percent(int count, int total) {
        return total > 0 ? (200 * count + total) / (2 * total) : 0;
    }

    public static Report review(List<Conversation> conversations, Policy policy) {
        int n = conversations.size();
        int resolved = (int) conversations.stream().filter(c -> c.resolved() && c.handoff().equals("none")).count();
        int safetyMissed = (int) conversations.stream().filter(c -> c.risk() && !c.handoff().equals("safety")).count();
        int overlong = (int) conversations.stream().filter(c -> c.turns() > policy.maxTurns() && c.handoff().equals("none")).count();
        int repeated = (int) conversations.stream().filter(Conversation::repeated).count();
        boolean repeatOk = repeated * 100 <= policy.maxRepeat() * n;
        int overEscalated = (int) conversations.stream().filter(c -> !c.handoff().equals("none") && !c.neededPerson() && !c.risk()).count();
        int underEscalated = (int) conversations.stream().filter(c -> c.handoff().equals("none") && c.neededPerson()).count();
        List<Segment> segments = new TreeSet<>(conversations.stream().map(Conversation::segment).toList()).stream().map(name -> {
            List<Conversation> group = conversations.stream().filter(c -> c.segment().equals(name)).toList();
            int done = (int) group.stream().filter(c -> c.resolved() && c.handoff().equals("none")).count();
            return new Segment(name, group.size(), done, percent(done, group.size()), group.size() >= policy.minN() && done * 100 < policy.minResolved() * group.size());
        }).toList();
        String reason;
        if (n == 0) reason = "no_data";
        else if (safetyMissed > 0) reason = "safety";
        else if (segments.stream().anyMatch(Segment::weak)) reason = "weak_segment";
        else if (!repeatOk) reason = "repeats";
        else reason = "none";
        return new Report(n, resolved, percent(resolved, n), safetyMissed, overlong, percent(repeated, n), repeatOk, overEscalated, underEscalated, segments,
            reason.equals("none") ? "ship" : "hold", reason);
    }
}
