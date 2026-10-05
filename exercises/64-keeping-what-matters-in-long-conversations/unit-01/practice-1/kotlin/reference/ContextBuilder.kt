/** What a long conversation keeps: trimmed tool output, case facts that newer information replaces, a context that never mixes customers, and a window that keeps tool calls whole. See ../../statement.md. */

private val log = System.getLogger("context_builder")

data class Fact(val value: String, val asOf: String, val superseded: List<String>)

data class FactEntry(val customer: String, val name: String, val value: String, val asOf: String)

data class Message(val role: String, val kind: String, val id: String, val text: String)

fun estimateTokens(text: String): Int = (text.length + 3) / 4

fun trimRecord(record: Map<String, String>, keep: List<String>): Map<String, String> {
    val out = linkedMapOf<String, String>()
    for (k in keep) if (k in record) out[k] = record.getValue(k)
    return out
}

fun updateFacts(facts: Map<String, Fact>, name: String, value: String, asOf: String): Map<String, Fact> {
    val next = LinkedHashMap(facts)
    val current = next[name]
    if (current == null) {
        next[name] = Fact(value, asOf, emptyList())
    } else if (asOf >= current.asOf) {
        next[name] = Fact(value, asOf, current.superseded + "${current.value}@${current.asOf}")
    } else {
        next[name] = Fact(current.value, current.asOf, current.superseded + "$value@$asOf")
    }
    return next
}

fun buildContext(customer: String, facts: List<FactEntry>, summary: String, recent: List<Message>): String {
    log.log(System.Logger.Level.DEBUG, "buildContext input: {0}", customer)
    val parts = mutableListOf<String>()
    val mine = facts.filter { it.customer == customer }
    if (mine.isNotEmpty()) parts += "## Case facts\n" + mine.joinToString("\n") { "${it.name}: ${it.value} (as of ${it.asOf})" }
    parts += "## Summary so far\n$summary"
    parts += "## Recent messages\n" + recent.joinToString("\n") { "${it.role}: ${it.text}" }
    return parts.joinToString("\n\n")
}

fun missingFromSummary(summary: String, facts: List<FactEntry>): List<String> = facts.filter { it.value !in summary }.map { it.name }

fun window(messages: List<Message>, budget: Int): List<Message> {
    val units = mutableListOf<List<Message>>()
    var i = 0
    while (i < messages.size) {
        val m = messages[i]
        if (m.kind == "tool_use" && i + 1 < messages.size && messages[i + 1].kind == "tool_result" && messages[i + 1].id == m.id) {
            units += listOf(m, messages[i + 1])
            i += 2
        } else {
            units += listOf(m)
            i += 1
        }
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
