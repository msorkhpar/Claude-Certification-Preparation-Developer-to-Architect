private val log = System.getLogger("case_facts")

/**
 * What a long support conversation should keep, and where it should sit.
 *
 * The exam guide (task 5.1) names four risks: a progressive summary turns numbers, dates and the customer's stated expectations into vague prose; models attend well to the start and the end of a long input and may miss the middle;
 * tool results pile up in the context out of proportion to their use (40 fields in an order lookup, five of them wanted); and the whole history must be sent on each request. The Claude documentation on long-context prompts
 * (read 2026-10-04) says to put long documents at the top and the question at the end, which can improve quality in tests by up to 30 percent, and to structure documents with tags. The functions below show the bookkeeping;
 * nothing here calls a model, and the numbers come from the sample data, not from a measurement.
 */
val TOOL_FIELDS = mapOf(
    "lookup_order" to listOf("order_id", "purchase_date", "items", "return_window", "refund_amount"),
    "lookup_customer" to listOf("customer_id", "tier"),
)

data class Fact(val name: String, val value: String, val day: Int)

data class Document(val title: String, val text: String)

fun tokens(text: String): Int = (text.length + 3) / 4

fun render(record: Map<String, String>): String = record.entries.joinToString(";") { "${it.key}=${it.value}" }

/** Keep what the next decision needs from a tool result, with its exact values. */
fun shrink(tool: String, result: Map<String, String>): Map<String, String> {
    val out = linkedMapOf<String, String>()
    for (k in TOOL_FIELDS.getValue(tool)) if (k in result) out[k] = result.getValue(k)
    return out
}

/** Facts the conversation must not lose, as a block that goes into every request outside the summarised history. */
fun caseFactsBlock(facts: List<Fact>): String = "## Case facts\n" + facts.joinToString("\n") { "${it.name}: ${it.value} (as of day ${it.day})" }

/** Key facts first, a short findings summary, the long documents under headers, the question last. */
fun assemble(block: String, findings: List<String>, documents: List<Document>, question: String): String =
    listOf(block, "## Key findings\n" + findings.joinToString("\n") { "- $it" }, "## Documents\n" + documents.joinToString("\n") { "### ${it.title}\n${it.text}" }, "## Question\n$question").joinToString("\n\n")

/** A value read days ago is re-read before it is acted on. */
fun stale(seenDay: Int, today: Int, maxAgeDays: Int): Boolean = today - seenDay > maxAgeDays

fun main() {
    val order = linkedMapOf("order_id" to "A-1042", "purchase_date" to "2026-09-02", "items" to "2 x kettle", "return_window" to "30 days", "refund_amount" to "$129.50")
    for (n in 1..35) order["internal_%02d".format(n)] = "backend-value-%02d".format(n)
    val small = shrink("lookup_order", order)
    println("lookup_order result: ${order.size} fields, ${render(order).length} characters, about ${tokens(render(order))} tokens")
    println("after shrinking: ${small.size} fields, ${render(small).length} characters, about ${tokens(render(small))} tokens")
    println("twenty lookups kept whole: ${20 * tokens(render(order))} tokens; shrunk: ${20 * tokens(render(small))} tokens")
    val block = caseFactsBlock(listOf(Fact("refund_amount", "$129.50", 118), Fact("return_deadline", "2026-09-30", 118)))
    println(block)
    val prompt = assemble(block, listOf("The order qualifies for a refund", "The deadline is the binding fact"), listOf(Document("Policy", "..."), Document("Order history", "...")), "What should the customer be told?")
    println("section order: " + prompt.lines().filter { it.startsWith("#") }.joinToString(" | "))
    for (seen in listOf(118, 124)) println("refund_amount seen on day $seen, today day 125, limit 3 days: " + if (stale(seen, 125, 3)) "read it again before acting" else "still fresh")
}
