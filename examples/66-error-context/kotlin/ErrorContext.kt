/**
 * What a coordinator is told when one of five sources fails, under four ways of reporting it.
 *
 * The exam guide (task 5.3) calls structured error context (failure type, the query attempted, partial results, alternatives) what lets a coordinator recover intelligently. It names two anti-patterns: a generic status
 * such as "search unavailable", which hides the context, and silent suppression, which reports an empty result as a success; terminating the whole workflow on one failure is the third. The five sources below and
 * their outcomes are invented for the illustration; nothing here calls a model or a search tool.
 */
/** kind is ok, timeout or permission */
data class Outcome(val kind: String, val items: List<String>)

val OUTCOMES = linkedMapOf("news" to Outcome("ok", listOf("n1", "n2")), "papers" to Outcome("timeout", listOf("p1")), "patents" to Outcome("ok", emptyList()), "filings" to Outcome("permission", emptyList()), "blogs" to Outcome("ok", listOf("b1")))
private val TRY = mapOf("timeout" to "retry later", "permission" to "request access")

private fun found(outcomes: Map<String, Outcome>): List<String> = outcomes.values.filter { it.kind == "ok" }.flatMap { it.items }

fun generic(outcomes: Map<String, Outcome>): String = "found ${found(outcomes).joinToString(", ")}; sources unavailable: " + outcomes.filter { it.value.kind != "ok" }.keys.joinToString(", ")

fun suppress(outcomes: Map<String, Outcome>): String = "found ${found(outcomes).joinToString(", ")}; nothing found in: " + outcomes.filter { it.value.kind != "ok" || it.value.items.isEmpty() }.keys.joinToString(", ")

fun terminate(outcomes: Map<String, Outcome>): String {
    val kept = mutableListOf<String>()
    for ((source, o) in outcomes) {
        if (o.kind != "ok") return "aborted at $source; found ${kept.joinToString(", ")}"
        kept += o.items
    }
    return "found ${kept.joinToString(", ")}"
}

fun structured(outcomes: Map<String, Outcome>): String {
    val good = outcomes.filter { it.value.kind == "ok" && it.value.items.isNotEmpty() }.keys.toList()
    val partial = outcomes.filter { it.value.kind != "ok" && it.value.items.isNotEmpty() }.map { "${it.key} (${it.value.kind}, kept ${it.value.items.joinToString(", ")})" }
    val empty = outcomes.filter { it.value.kind == "ok" && it.value.items.isEmpty() }.keys.toList()
    val gaps = outcomes.filter { it.value.kind != "ok" && it.value.items.isEmpty() }.map { "${it.key} (${it.value.kind}, try: ${TRY[it.value.kind]})" }
    return listOf("well supported" to good, "partial" to partial, "no findings" to empty, "gaps" to gaps).filter { it.second.isNotEmpty() }.joinToString("; ") { "${it.first}: ${it.second.joinToString(", ")}" }
}

fun main() {
    println("generic status: ${generic(OUTCOMES)}")
    println("silent empty: ${suppress(OUTCOMES)}")
    println("abort on failure: ${terminate(OUTCOMES)}")
    println("structured context: ${structured(OUTCOMES)}")
}
