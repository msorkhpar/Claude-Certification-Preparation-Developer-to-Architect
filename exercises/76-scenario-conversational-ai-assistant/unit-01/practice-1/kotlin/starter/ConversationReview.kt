private val log = System.getLogger("conversation_review")

data class Conversation(val id: String, val segment: String, val turns: Int, val resolved: Boolean, val handoff: String, val neededPerson: Boolean, val repeated: Boolean, val risk: Boolean)

data class Policy(val maxTurns: Int, val maxRepeat: Int, val minResolved: Int, val minN: Int)

data class Segment(val segment: String, val n: Int, val resolved: Int, val percent: Int, val weak: Boolean)

data class Report(
    val n: Int, val resolved: Int, val resolvedPct: Int, val safetyMissed: Int, val overlong: Int, val repeatPct: Int, val repeatOk: Boolean, val overEscalated: Int,
    val underEscalated: Int, val segments: List<Segment>, val verdict: String, val reason: String,
)

/**
 * TODO 1 of 8 (unlocks m1, e1 and e9): a whole percentage, rounded half up.
 * Receives a count and a total. Returns `(200 * count + total) / (2 * total)` in integer arithmetic, and 0 when the total is 0.
 * Example: percent(2, 3) -> 67, percent(1, 8) -> 13, percent(0, 0) -> 0
 */
fun percent(count: Int, total: Int): Int = 0

/**
 * TODO 2 of 8 (unlocks m1 and e8): did the assistant settle this conversation alone?
 * Receives one conversation. Returns true only when `resolved` is true and `handoff` is `none` (a conversation a person settled is not one the assistant settled).
 * Example: a resolved conversation with handoff "requested" -> false
 */
fun settled(c: Conversation): Boolean = false

/**
 * TODO 3 of 8 (unlocks e2): count the safety signals that did not reach a person as a safety hand-off.
 * Receives the conversations. Returns how many have `risk` and a `handoff` other than `safety` (`none` and `requested` both count as missed).
 * Example: one risk conversation with handoff "requested" and one with "safety" -> 1
 */
fun countSafetyMissed(conversations: List<Conversation>): Int = 0

/**
 * TODO 4 of 8 (unlocks e3): count the overlong conversations.
 * Receives the conversations and the limit. Returns how many have more than `maxTurns` turns and `handoff` `none`.
 * Example: with a limit of 12, turns 12 -> 0, turns 13 -> 1, turns 30 with handoff "stalled" -> 0
 */
fun countOverlong(conversations: List<Conversation>, maxTurns: Int): Int = 0

/**
 * TODO 5 of 8 (unlocks e1 and e4): are the repeated questions within the limit?
 * Receives the repeated count, the batch size and the limit in percent. Returns true when `repeated * 100 <= maxRepeat * n` (so true for an empty batch).
 * Example: isRepeatOk(1, 10, 10) -> true, isRepeatOk(2, 10, 10) -> false
 */
fun isRepeatOk(repeated: Int, n: Int, maxRepeat: Int): Boolean = false

/**
 * TODO 6 of 8 (unlocks e7): count over- and under-escalation.
 * Receives the conversations. Returns Pair(overEscalated, underEscalated): hand-offs (any `handoff` but `none`) with no `neededPerson` and no `risk`,
 * and conversations with `handoff` `none` that had `neededPerson`.
 * Example: a "requested" hand-off nobody needed -> Pair(1, 0); a "safety" hand-off with risk -> Pair(0, 0)
 */
fun countEscalations(conversations: List<Conversation>): Pair<Int, Int> = Pair(0, 0)

/**
 * TODO 7 of 8 (unlocks m1, e5 and e6): one entry per segment.
 * Receives the conversations and the policy. Returns a list sorted by segment name of Segment(segment, n, resolved, percent, weak), with `resolved` counted by `settled`.
 * `weak` needs at least `minN` conversations and `resolved * 100 < minResolved * n` for the segment.
 * Example: 3 unresolved "refunds" conversations with minN 3 -> [Segment("refunds", 3, 0, 0, true)]
 */
fun segmentsOf(conversations: List<Conversation>, policy: Policy): List<Segment> = emptyList()

/**
 * TODO 8 of 8 (unlocks m1, e1, e2 and e4): the verdict and its reason.
 * Receives the batch size, the missed safety count, the segments and `repeatOk`. Returns Pair(verdict, reason): Pair("hold", "no_data") for an empty batch,
 * otherwise the first that applies of Pair("hold", "safety"), Pair("hold", "weak_segment") and Pair("hold", "repeats"), and Pair("ship", "none") when none does.
 * Example: chooseVerdict(5, 0, emptyList(), false) -> Pair("hold", "repeats")
 */
fun chooseVerdict(n: Int, safetyMissed: Int, segments: List<Segment>, repeatOk: Boolean): Pair<String, String> = Pair("", "")

/** Review a batch of assistant conversations: how many the assistant settled, whether every safety signal reached a person, and whether the assistant may ship. */
fun review(conversations: List<Conversation>, policy: Policy): Report {
    log.log(System.Logger.Level.DEBUG, "review input: {0}", conversations)
    val n = conversations.size
    val resolved = conversations.count { settled(it) }
    val safetyMissed = countSafetyMissed(conversations)
    val repeated = conversations.count { it.repeated }
    val repeatOk = isRepeatOk(repeated, n, policy.maxRepeat)
    val (overEscalated, underEscalated) = countEscalations(conversations)
    val segments = segmentsOf(conversations, policy)
    val (verdict, reason) = chooseVerdict(n, safetyMissed, segments, repeatOk)
    return Report(n, resolved, percent(resolved, n), safetyMissed, countOverlong(conversations, policy.maxTurns), percent(repeated, n), repeatOk, overEscalated, underEscalated, segments,
        verdict, reason)
}
