data class Conversation(val id: String, val segment: String, val turns: Int, val resolved: Boolean, val handoff: String, val neededPerson: Boolean, val repeated: Boolean, val risk: Boolean)

data class Policy(val maxTurns: Int, val maxRepeat: Int, val minResolved: Int, val minN: Int)

data class Segment(val segment: String, val n: Int, val resolved: Int, val percent: Int, val weak: Boolean)

data class Report(
    val n: Int, val resolved: Int, val resolvedPct: Int, val safetyMissed: Int, val overlong: Int, val repeatPct: Int, val repeatOk: Boolean, val overEscalated: Int,
    val underEscalated: Int, val segments: List<Segment>, val verdict: String, val reason: String,
)

/** A whole percentage, rounded half up; 0 when there is nothing to divide. */
fun percent(count: Int, total: Int): Int = if (total > 0) (200 * count + total) / (2 * total) else 0

/** Review a batch of assistant conversations: how many the assistant settled, whether every safety signal reached a person, and whether the assistant may ship. */
fun review(conversations: List<Conversation>, policy: Policy): Report {
    val n = conversations.size
    val resolved = conversations.count { it.resolved && it.handoff == "none" }
    val safetyMissed = conversations.count { it.risk && it.handoff != "safety" }
    val overlong = conversations.count { it.turns > policy.maxTurns && it.handoff == "none" }
    val repeated = conversations.count { it.repeated }
    val repeatOk = repeated * 100 <= policy.maxRepeat * n
    val overEscalated = conversations.count { it.handoff != "none" && !it.neededPerson && !it.risk }
    val underEscalated = conversations.count { it.handoff == "none" && it.neededPerson }
    val segments = conversations.map { it.segment }.toSortedSet().map { name ->
        val group = conversations.filter { it.segment == name }
        val done = group.count { it.resolved && it.handoff == "none" }
        Segment(name, group.size, done, percent(done, group.size), group.size >= policy.minN && done * 100 < policy.minResolved * group.size)
    }
    val reason = when {
        n == 0 -> "no_data"
        safetyMissed > 0 -> "safety"
        segments.any { it.weak } -> "weak_segment"
        !repeatOk -> "repeats"
        else -> "none"
    }
    return Report(n, resolved, percent(resolved, n), safetyMissed, overlong, percent(repeated, n), repeatOk, overEscalated, underEscalated, segments,
        if (reason == "none") "ship" else "hold", reason)
}
