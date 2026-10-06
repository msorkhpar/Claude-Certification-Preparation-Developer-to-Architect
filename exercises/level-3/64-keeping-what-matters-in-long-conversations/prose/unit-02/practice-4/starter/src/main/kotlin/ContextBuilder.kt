/** What a long conversation keeps: trimmed tool output, case facts that newer information replaces, a context that never mixes customers, and a window that keeps tool calls whole. See ../../statement.md. */

private val log = System.getLogger("context_builder")

data class Fact(val value: String, val asOf: String, val superseded: List<String>)

data class FactEntry(val customer: String, val name: String, val value: String, val asOf: String)

data class Message(val role: String, val kind: String, val id: String, val text: String)

fun estimateTokens(text: String): Int = (text.length + 3) / 4

fun trimRecord(record: Map<String, String>, keep: List<String>): Map<String, String> {
    // TODO 1 of 6 (finish this to pass m1, e1): the trim. Receives a tool's record and the list of fields to keep.
    //   Return a new record with only those fields, in the order of the list, with their exact values, skipping a field
    //   that the record does not have. Example: record {id, status, notes}, keep [status, id] -> {status, id}.
    return LinkedHashMap(record)
}

fun updateFacts(facts: Map<String, Fact>, name: String, value: String, asOf: String): Map<String, Fact> {
    val next = LinkedHashMap(facts)
    val current = next[name]
    if (current == null) {
        next[name] = Fact(value, asOf, emptyList())
    } else {
        // TODO 2 of 6 (finish this to pass e2, e3): the update of a known fact. When the new date is the same as or later
        //   than the stored one, replace the value and the date and append "oldvalue@olddate" to the history; when it is
        //   earlier, keep the current value and append "newvalue@newdate" to the history. Example: stored 5@2026-01-02, new
        //   7@2026-01-05 -> value 7, history [5@2026-01-02].
        next[name] = Fact(value, asOf, current.superseded)
    }
    return next
}

fun buildContext(customer: String, facts: List<FactEntry>, summary: String, recent: List<Message>): String {
    log.log(System.Logger.Level.DEBUG, "buildContext input: {0}", customer)
    val parts = mutableListOf<String>()
    // TODO 3 of 6 (finish this to pass e4): the facts of one customer. Receives the customer and all the fact entries.
    //   Keep only the entries whose customer is that customer. Example: facts of C1 and C2, customer C1 -> the C1 entries
    //   only.
    val mine = facts
    // TODO 4 of 6 (finish this to pass e5): the case facts section. When there are facts for the customer, add first a
    //   section titled "## Case facts" with one line per fact, "name: value (as of date)". Example: one fact -> "## Case
    //   facts\norder: A-7 (as of 2026-01-02)", before the summary.
    parts += "## Summary so far\n$summary"
    parts += "## Recent messages\n" + recent.joinToString("\n") { "${it.role}: ${it.text}" }
    return parts.joinToString("\n\n")
}

// TODO 5 of 6 (finish this to pass e6): the check of a summary. Receives the summary and the fact entries. Return the
//   names of the facts whose exact value does not appear in the summary text. Example: fact order = A-7, summary "customer
//   wants a refund" -> [order].
fun missingFromSummary(summary: String, facts: List<FactEntry>): List<String> = emptyList()

fun window(messages: List<Message>, budget: Int): List<Message> {
    val units = mutableListOf<List<Message>>()
    var i = 0
    while (i < messages.size) {
        val m = messages[i]
        // TODO 6 of 6 (finish this to pass e7): the units of the window. Walk the messages in order: a tool_use message
        //   followed by the tool_result with the same id forms one unit of two, which is never split; any other message is
        //   a unit of one. Example: [user, tool_use t1, tool_result t1] -> two units.
        units += listOf(m)
        i += 1
    }
    val kept = ArrayDeque<List<Message>>()
    var used = 0
    for (unit in units.asReversed()) {
        val cost = unit.sumOf { estimateTokens(it.text) }
        if (used + cost > budget) break
        kept.addFirst(unit)
        used += cost
    }
    return kept.flatten()
}
