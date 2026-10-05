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

    /**
     * TODO 1 of 8 (unlocks m1, e1 and e9): a whole percentage, rounded half up.
     * Receives a count and a total. Returns {@code (200 * count + total) / (2 * total)} in integer arithmetic, and 0 when the total is 0.
     * Example: percent(2, 3) -> 67, percent(1, 8) -> 13, percent(0, 0) -> 0
     */
    static int percent(int count, int total) {
        return 0;
    }

    /**
     * TODO 2 of 8 (unlocks m1 and e8): did the assistant settle this conversation alone?
     * Receives one conversation. Returns true only when {@code resolved} is true and {@code handoff} is {@code none} (a conversation a person settled is not one the assistant settled).
     * Example: a resolved conversation with handoff "requested" -> false
     */
    static boolean settled(Conversation c) {
        return false;
    }

    /**
     * TODO 3 of 8 (unlocks e2): count the safety signals that did not reach a person as a safety hand-off.
     * Receives the conversations. Returns how many have {@code risk} and a {@code handoff} other than {@code safety} ({@code none} and {@code requested} both count as missed).
     * Example: one risk conversation with handoff "requested" and one with "safety" -> 1
     */
    static int countSafetyMissed(List<Conversation> conversations) {
        return 0;
    }

    /**
     * TODO 4 of 8 (unlocks e3): count the overlong conversations.
     * Receives the conversations and the limit. Returns how many have more than {@code maxTurns} turns and {@code handoff} {@code none}.
     * Example: with a limit of 12, turns 12 -> 0, turns 13 -> 1, turns 30 with handoff "stalled" -> 0
     */
    static int countOverlong(List<Conversation> conversations, int maxTurns) {
        return 0;
    }

    /**
     * TODO 5 of 8 (unlocks e1 and e4): are the repeated questions within the limit?
     * Receives the repeated count, the batch size and the limit in percent. Returns true when {@code repeated * 100 <= maxRepeat * n} (so true for an empty batch).
     * Example: isRepeatOk(1, 10, 10) -> true, isRepeatOk(2, 10, 10) -> false
     */
    static boolean isRepeatOk(int repeated, int n, int maxRepeat) {
        return false;
    }

    /**
     * TODO 6 of 8 (unlocks e7): count over- and under-escalation.
     * Receives the conversations. Returns {overEscalated, underEscalated}: hand-offs (any {@code handoff} but {@code none}) with no {@code neededPerson} and no {@code risk},
     * and conversations with {@code handoff} {@code none} that had {@code neededPerson}.
     * Example: a "requested" hand-off nobody needed -> {1, 0}; a "safety" hand-off with risk -> {0, 0}
     */
    static int[] countEscalations(List<Conversation> conversations) {
        return new int[] {0, 0};
    }

    /**
     * TODO 7 of 8 (unlocks m1, e5 and e6): one entry per segment.
     * Receives the conversations and the policy. Returns a list sorted by segment name of Segment(segment, n, resolved, percent, weak), with {@code resolved} counted by {@code settled}.
     * {@code weak} needs at least {@code minN} conversations and {@code resolved * 100 < minResolved * n} for the segment.
     * Example: 3 unresolved "refunds" conversations with minN 3 -> [Segment("refunds", 3, 0, 0, true)]
     */
    static List<Segment> segmentsOf(List<Conversation> conversations, Policy policy) {
        return List.of();
    }

    /**
     * TODO 8 of 8 (unlocks m1, e1, e2 and e4): the verdict and its reason.
     * Receives the batch size, the missed safety count, the segments and {@code repeatOk}. Returns {verdict, reason}: {"hold", "no_data"} for an empty batch,
     * otherwise the first that applies of {"hold", "safety"}, {"hold", "weak_segment"} and {"hold", "repeats"}, and {"ship", "none"} when none does.
     * Example: chooseVerdict(5, 0, List.of(), false) -> {"hold", "repeats"}
     */
    static String[] chooseVerdict(int n, int safetyMissed, List<Segment> segments, boolean repeatOk) {
        return new String[] {"", ""};
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
