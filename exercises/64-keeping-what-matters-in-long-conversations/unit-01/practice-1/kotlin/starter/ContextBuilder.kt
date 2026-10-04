/** What a long conversation keeps: trimmed tool output, case facts that newer information replaces, a context that never mixes customers, and a window that keeps tool calls whole. See ../../statement.md. */

data class Fact(val value: String, val asOf: String, val superseded: List<String>)

data class FactEntry(val customer: String, val name: String, val value: String, val asOf: String)

data class Message(val role: String, val kind: String, val id: String, val text: String)

fun estimateTokens(text: String): Int = (text.length + 3) / 4

fun trimRecord(record: Map<String, String>, keep: List<String>): Map<String, String>? {
    // TODO: the fields of the record named in keep, in that order, with their exact values.
    return null
}

fun updateFacts(facts: Map<String, Fact>, name: String, value: String, asOf: String): Map<String, Fact>? {
    // TODO: a new facts map; the input is not changed.
    return null
}

fun buildContext(customer: String, facts: List<FactEntry>, summary: String, recent: List<Message>): String? {
    // TODO: the text of the context: case facts of this customer, then the summary, then the recent messages, each under a heading.
    return null
}

fun missingFromSummary(summary: String, facts: List<FactEntry>): List<String>? {
    // TODO: the names of the facts whose value the summary no longer holds.
    return null
}

fun window(messages: List<Message>, budget: Int): List<Message>? {
    // TODO: the newest messages that fit the token budget, keeping every tool call together with its result.
    return null
}
