import harness.Show.py

/**
 * Four decisions about tools in a research and refund system: who gets which tool, what tool_choice a turn can use, whether a reply made the call it had to, and whether a refund may run.
 *
 * No model is called. The catalog, the models and the limits are illustrative; the models that reject a forced choice are the ones the "Define tools" page lists, read on 2026-10-03.
 */
val CATALOG = linkedMapOf(
    "web_search" to listOf("web"), "fetch_page" to listOf("web"), "verify_fact" to listOf("web", "synthesis"), "load_document" to listOf("documents"),
    "extract_data_points" to listOf("documents"), "summarize_content" to listOf("synthesis"), "write_report" to listOf("reports"), "send_report" to listOf("reports"),
)
val IRREVERSIBLE = setOf("send_report")
val ROLES = linkedMapOf("searcher" to "web", "analyst" to "documents", "synthesizer" to "synthesis", "reporter" to "reports")
val NO_FORCING = setOf("claude-opus-5-5", "claude-sonnet-5-5", "claude-fable-5-1", "claude-mythos-5-1")

/** The settings of one request: the tool_choice, the tools offered, and whether the reply must be checked for the call. */
data class Turn(val toolChoice: String, val tools: List<String>, val checkReply: Boolean)

/** One block of a reply: its type and the text or tool name it holds. */
data class Block(val type: String, val value: String)

fun toolsFor(role: String): List<String> = CATALOG.filter { (name, tags) -> ROLES.getValue(role) in tags && name !in IRREVERSIBLE }.keys.toList()

/** The request settings for a turn whose first call must be `forced`. */
fun turnFor(model: String, forced: String, tools: List<String>): Turn =
    if (model in NO_FORCING) Turn("auto", listOf(forced), true) else Turn("tool:$forced", tools, false)

/** What switching from one request to another costs in prompt caching. */
fun cacheCost(before: Turn, after: Turn): String = when {
    before.tools != after.tools -> "everything (the tool definitions changed)"
    before.toolChoice != after.toolChoice -> "the cached messages (tool_choice changed)"
    else -> "nothing"
}

fun madeTheCall(reply: List<Block>, forced: String): Boolean = reply.filter { it.type == "tool_use" }.let { it.isNotEmpty() && it[0].value == forced }

fun allowed(tool: String, amount: Int, approved: Boolean, cap: Int = 200): String = when {
    tool !in setOf("refund", "lookup") -> "refused: unknown tool"
    tool == "lookup" -> "run"
    amount > cap -> "refused: above the limit of $cap, send to a person"
    approved -> "run"
    else -> "wait: a person must approve"
}

fun main() {
    println("catalog: ${CATALOG.size} tools")
    for (role in ROLES.keys) println("  $role: ${toolsFor(role).joinToString(", ")}")
    println("a synthesizer that may also check one fact has verify_fact, and nothing else from the web")
    println("first call must be extract_metadata:")
    val both = listOf("extract_metadata", "enrich")
    for (model in listOf("claude-opus-5", "claude-sonnet-5-5")) {
        val turn = turnFor(model, "extract_metadata", both)
        println("  $model: tool_choice=${turn.toolChoice}, tools=${py(turn.tools)}, check the reply=${py(turn.checkReply)}")
        println("    cost against a turn with auto and both tools: ${cacheCost(Turn("auto", both, false), turn)}")
    }
    println("a reply to the fallback turn:")
    for ((label, reply) in listOf(
        "text only" to listOf(Block("text", "I will look at the metadata.")),
        "the right call" to listOf(Block("tool_use", "extract_metadata")),
        "another tool" to listOf(Block("tool_use", "enrich")),
    )) {
        println("  $label: ${if (madeTheCall(reply, "extract_metadata")) "accept" else "re-ask once, then escalate"}")
    }
    println("refund decisions:")
    for ((tool, amount, approved) in listOf(Triple("lookup", 0, false), Triple("refund", 150, false), Triple("refund", 150, true), Triple("refund", 400, true), Triple("delete_account", 0, true))) {
        println("  $tool $amount, approved=${if (approved) "yes" else "no"}: ${allowed(tool, amount, approved)}")
    }
}
