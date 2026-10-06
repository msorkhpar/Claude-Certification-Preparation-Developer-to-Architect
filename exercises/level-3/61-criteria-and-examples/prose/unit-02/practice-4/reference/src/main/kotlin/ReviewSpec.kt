/** A review specification that cuts false positives: the prompt, the trust in each category and the next step when a request is incomplete. See ../../statement.md. Results are JSON-like maps. */

private val log = System.getLogger("review_spec")

val VAGUE = listOf("be conservative", "high-confidence", "high confidence", "only important", "only significant", "if you are sure", "when you are sure", "use your judgment")

private fun present(value: Any?): Boolean = value is String && value.isNotBlank()

private fun blank(value: Any?): Boolean = value == null || (value is String && value.isBlank())

private fun vague(text: String): String? = VAGUE.firstOrNull { text.lowercase().contains(it) }

@Suppress("UNCHECKED_CAST")
private fun maps(value: Any?): List<Map<String, Any?>> = (value as List<Map<String, Any?>>?) ?: emptyList()

@Suppress("UNCHECKED_CAST")
private fun asMap(value: Any?): Map<String, Any?> = (value as Map<String, Any?>?) ?: emptyMap()

fun buildReviewPrompt(spec: Map<String, Any?>, diff: String): String? {
    log.log(System.Logger.Level.DEBUG, "buildReviewPrompt input: {0}", spec)
    val criteria = maps(spec["criteria"])
    val examples = maps(spec["examples"])
    require(criteria.isNotEmpty()) { "at least one criterion is required" }
    val ids = mutableListOf<Any?>()
    val out = mutableListOf("<criteria>")
    for (c in criteria) {
        for (key in listOf("report", "skip")) {
            require(present(c[key])) { "criterion ${c["id"]}: $key is required" }
            val phrase = vague(c[key] as String)
            require(phrase == null) { "criterion ${c["id"]}: $key is vague ('$phrase'): name the pattern instead" }
        }
        val severity = asMap(c["severity"])
        for (level in listOf("high", "low")) require(present(severity[level])) { "criterion ${c["id"]}: severity $level needs a concrete example" }
        ids += c["id"]
        out += "<criterion id=\"${c["id"]}\">\nReport: ${c["report"]}\nSkip: ${c["skip"]}\nSeverity high: ${severity["high"]}\nSeverity low: ${severity["low"]}\n</criterion>"
    }
    out += "</criteria>"
    require(examples.size in 2..4) { "use two to four examples" }
    require(examples.map { it["verdict"] }.toSet() == setOf("report", "skip")) { "the examples need at least one report and one skip, and no other verdict" }
    out += "<examples>"
    for (e in examples) {
        require(present(e["reason"])) { "every example needs a reason" }
        require(e["verdict"] != "report" || e["category"] in ids) { "a report example names one of the criteria" }
        val tag = if (e["verdict"] == "report") "verdict=\"report\" category=\"${e["category"]}\"" else "verdict=\"skip\""
        out += "<example $tag>\n<code>${e["code"]}</code>\n<reason>${e["reason"]}</reason>\n</example>"
    }
    out += "</examples>"
    out += "<diff>"
    out += diff
    out += "</diff>"
    return out.joinToString("\n")
}

fun categoryReport(findings: List<Map<String, Any?>>, minReviewed: Int = 5, minPrecision: Double = 0.5): Map<String, Any?>? {
    val categories = LinkedHashMap<String, Any?>()
    val disable = mutableListOf<String>()
    for ((name, items) in findings.groupBy { it["category"] as String }) {
        val accepted = items.count { it["verdict"] == "accepted" }
        val counts = items.filter { it["verdict"] == "dismissed" }.groupingBy { it["detected_pattern"] as String }.eachCount()
        val top = counts.entries.sortedWith(compareBy({ -it.value }, { it.key })).take(3).map { listOf(it.key, it.value) }
        val precision = Math.round(accepted.toDouble() / items.size * 100) / 100.0
        val off = items.size >= minReviewed && precision < minPrecision
        categories[name] = mapOf("reviewed" to items.size, "precision" to precision, "disable" to off, "top_dismissed" to top)
        if (off) disable += name
    }
    return mapOf("categories" to categories, "disable" to disable.sorted())
}

fun nextStep(request: Map<String, Any?>, required: List<String>, defaults: Map<String, String>, attended: Boolean): Map<String, Any?>? {
    val missing = required.filter { blank(request[it]) }
    val assumptions = LinkedHashMap<String, Any?>()
    missing.filter { it in defaults }.forEach { assumptions[it] = defaults.getValue(it) }
    val unresolved = missing.filter { it !in defaults }
    return when {
        unresolved.isNotEmpty() && attended -> mapOf("action" to "ask", "ask" to unresolved, "assumptions" to assumptions)
        unresolved.isNotEmpty() -> mapOf("action" to "stop", "ask" to emptyList<String>(), "assumptions" to assumptions)
        else -> mapOf("action" to "proceed", "ask" to emptyList<String>(), "assumptions" to assumptions)
    }
}
