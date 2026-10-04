// Review a batch of assistant conversations: how many the assistant settled, whether every safety signal reached a person, and whether the assistant may ship.
// Read statement.md for the fields of a conversation, of the policy and of the report, then replace the body of review().
data class Conversation(val id: String, val segment: String, val turns: Int, val resolved: Boolean, val handoff: String, val neededPerson: Boolean, val repeated: Boolean, val risk: Boolean)

data class Policy(val maxTurns: Int, val maxRepeat: Int, val minResolved: Int, val minN: Int)

data class Segment(val segment: String, val n: Int, val resolved: Int, val percent: Int, val weak: Boolean)

data class Report(
    val n: Int, val resolved: Int, val resolvedPct: Int, val safetyMissed: Int, val overlong: Int, val repeatPct: Int, val repeatOk: Boolean, val overEscalated: Int,
    val underEscalated: Int, val segments: List<Segment>, val verdict: String, val reason: String,
)

fun review(conversations: List<Conversation>, policy: Policy): Report = Report(0, 0, 0, 0, 0, 0, false, 0, 0, listOf(), "", "")
