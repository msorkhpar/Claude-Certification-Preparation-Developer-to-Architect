private val log = System.getLogger("conversation_review")

data class Conversation(val id: String, val segment: String, val turns: Int, val resolved: Boolean, val handoff: String, val neededPerson: Boolean, val repeated: Boolean, val risk: Boolean)

data class Policy(val maxTurns: Int, val maxRepeat: Int, val minResolved: Int, val minN: Int)

data class Segment(val segment: String, val n: Int, val resolved: Int, val percent: Int, val weak: Boolean)

data class Report(
    val n: Int, val resolved: Int, val resolvedPct: Int, val safetyMissed: Int, val overlong: Int, val repeatPct: Int, val repeatOk: Boolean, val overEscalated: Int,
    val underEscalated: Int, val segments: List<Segment>, val verdict: String, val reason: String,
)

/** A whole percentage, rounded half up; 0 when there is nothing to divide. */
fun percent(count: Int, total: Int): Int = if (total > 0) (200 * count + total) / (2 * total) else 0

/** True when the assistant settled the conversation alone: resolved and no hand-off. */
fun settled(c: Conversation): Boolean = c.resolved && c.handoff == "none"

/** The conversations with a risk signal whose hand-off is not a safety hand-off. */
fun countSafetyMissed(conversations: List<Conversation>): Int = conversations.count { it.risk && it.handoff != "safety" }

/** The conversations above the turn limit that nobody took over. */
fun countOverlong(conversations: List<Conversation>, maxTurns: Int): Int = conversations.count { it.turns > maxTurns && it.handoff == "none" }

/** True when the repeated questions are at most maxRepeat percent of the batch (true for an empty batch). */
fun isRepeatOk(repeated: Int, n: Int, maxRepeat: Int): Boolean = repeated * 100 <= maxRepeat * n

/** Pair(overEscalated, underEscalated): hand-offs nobody needed, and conversations that needed a person and got none. */
fun countEscalations(conversations: List<Conversation>): Pair<Int, Int> =
    Pair(conversations.count { it.handoff != "none" && !it.neededPerson && !it.risk }, conversations.count { it.handoff == "none" && it.neededPerson })

/** One entry per segment, sorted by name. */
fun segmentsOf(conversations: List<Conversation>, policy: Policy): List<Segment> = conversations.map { it.segment }.toSortedSet().map { name ->
    val group = conversations.filter { it.segment == name }
    val done = group.count { settled(it) }
    Segment(name, group.size, done, percent(done, group.size), group.size >= policy.minN && done * 100 < policy.minResolved * group.size)
}

/** Pair(verdict, reason): hold for no data, then for a missed safety signal, a weak segment and repeats; otherwise ship. */
fun chooseVerdict(n: Int, safetyMissed: Int, segments: List<Segment>, repeatOk: Boolean): Pair<String, String> = when {
    n == 0 -> Pair("hold", "no_data")
    safetyMissed > 0 -> Pair("hold", "safety")
    segments.any { it.weak } -> Pair("hold", "weak_segment")
    !repeatOk -> Pair("hold", "repeats")
    else -> Pair("ship", "none")
}

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
