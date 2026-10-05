import java.util.List;
import java.util.TreeSet;

/** Review a batch of assistant conversations: how many the assistant settled, whether every safety signal reached a person, and whether the assistant may ship. */
public final class ConversationReview {
    private static final System.Logger LOG = System.getLogger(ConversationReview.class.getName());

    public record Conversation(String id, String segment, int turns, boolean resolved, String handoff, boolean neededPerson, boolean repeated, boolean risk) {}

    public record Policy(int maxTurns, int maxRepeat, int minResolved, int minN) {}

    public record Segment(String segment, int n, int resolved, int percent, boolean weak) {}

    public record Report(int n, int resolved, int resolvedPct, int safetyMissed, int overlong, int repeatPct, boolean repeatOk, int overEscalated, int underEscalated,
                         List<Segment> segments, String verdict, String reason) {}

    /** A whole percentage, rounded half up; 0 when there is nothing to divide. */
    static int percent(int count, int total) {
        return total > 0 ? (200 * count + total) / (2 * total) : 0;
    }

    /** True when the assistant settled the conversation alone: resolved and no hand-off. */
    static boolean settled(Conversation c) {
        return c.resolved() && c.handoff().equals("none");
    }

    /** The conversations with a risk signal whose hand-off is not a safety hand-off. */
    static int countSafetyMissed(List<Conversation> conversations) {
        return (int) conversations.stream().filter(c -> c.risk() && !c.handoff().equals("safety")).count();
    }

    /** The conversations above the turn limit that nobody took over. */
    static int countOverlong(List<Conversation> conversations, int maxTurns) {
        return (int) conversations.stream().filter(c -> c.turns() > maxTurns && c.handoff().equals("none")).count();
    }

    /** True when the repeated questions are at most maxRepeat percent of the batch (true for an empty batch). */
    static boolean isRepeatOk(int repeated, int n, int maxRepeat) {
        return repeated * 100 <= maxRepeat * n;
    }

    /** {overEscalated, underEscalated}: hand-offs nobody needed, and conversations that needed a person and got none. */
    static int[] countEscalations(List<Conversation> conversations) {
        int over = (int) conversations.stream().filter(c -> !c.handoff().equals("none") && !c.neededPerson() && !c.risk()).count();
        int under = (int) conversations.stream().filter(c -> c.handoff().equals("none") && c.neededPerson()).count();
        return new int[] {over, under};
    }

    /** One entry per segment, sorted by name. */
    static List<Segment> segmentsOf(List<Conversation> conversations, Policy policy) {
        return new TreeSet<>(conversations.stream().map(Conversation::segment).toList()).stream().map(name -> {
            List<Conversation> group = conversations.stream().filter(c -> c.segment().equals(name)).toList();
            int done = (int) group.stream().filter(ConversationReview::settled).count();
            return new Segment(name, group.size(), done, percent(done, group.size()), group.size() >= policy.minN() && done * 100 < policy.minResolved() * group.size());
        }).toList();
    }

    /** {verdict, reason}: hold for no data, then for a missed safety signal, a weak segment and repeats; otherwise ship. */
    static String[] chooseVerdict(int n, int safetyMissed, List<Segment> segments, boolean repeatOk) {
        if (n == 0) return new String[] {"hold", "no_data"};
        if (safetyMissed > 0) return new String[] {"hold", "safety"};
        if (segments.stream().anyMatch(Segment::weak)) return new String[] {"hold", "weak_segment"};
        if (!repeatOk) return new String[] {"hold", "repeats"};
        return new String[] {"ship", "none"};
    }

    public static Report review(List<Conversation> conversations, Policy policy) {
        LOG.log(System.Logger.Level.DEBUG, "review input: {0}", conversations);
        int n = conversations.size();
        int resolved = (int) conversations.stream().filter(ConversationReview::settled).count();
        int safetyMissed = countSafetyMissed(conversations);
        int repeated = (int) conversations.stream().filter(Conversation::repeated).count();
        boolean repeatOk = isRepeatOk(repeated, n, policy.maxRepeat());
        int[] escalations = countEscalations(conversations);
        List<Segment> segments = segmentsOf(conversations, policy);
        String[] verdict = chooseVerdict(n, safetyMissed, segments, repeatOk);
        return new Report(n, resolved, percent(resolved, n), safetyMissed, countOverlong(conversations, policy.maxTurns()), percent(repeated, n), repeatOk, escalations[0], escalations[1],
            segments, verdict[0], verdict[1]);
    }
}
