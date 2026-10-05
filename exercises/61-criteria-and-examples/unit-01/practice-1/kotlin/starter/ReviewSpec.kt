/** A review specification that cuts false positives: the prompt, the trust in each category and the next step when a request is incomplete. See ../../statement.md. Results are JSON-like maps. */

private val log = System.getLogger("review_spec")

val VAGUE = listOf("be conservative", "high-confidence", "high confidence", "only important", "only significant", "if you are sure", "when you are sure", "use your judgment")

private fun present(value: Any?): Boolean = value is String && value.isNotBlank()

private fun blank(value: Any?): Boolean = value == null || (value is String && value.isBlank())

// TODO 2 of 7 (finish this to pass e1): the vague check. Receives a criterion's report or skip text. Return the first
//   phrase of VAGUE that the lower-cased text contains, or nothing when it contains none. Example: "Be conservative here"
//   -> "be conservative".
private fun vague(text: String): String? = null

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
        // TODO 3 of 7 (finish this to pass e2): the severity check. For each of the levels high and low, refuse the
        //   criterion when its severity has no concrete example text for that level. Example: severity {high: "..."} with
        //   no low -> refused.
        ids += c["id"]
        out += "<criterion id=\"${c["id"]}\">\nReport: ${c["report"]}\nSkip: ${c["skip"]}\nSeverity high: ${severity["high"]}\nSeverity low: ${severity["low"]}\n</criterion>"
    }
    out += "</criteria>"
    // TODO 4 of 7 (finish this to pass e3): the examples check. Refuse the specification unless it has two to four
    //   examples and their verdicts are exactly report and skip (at least one of each, nothing else). Example: three
    //   report examples and no skip -> refused.
    out += "<examples>"
    for (e in examples) {
        require(present(e["reason"])) { "every example needs a reason" }
        require(e["verdict"] != "report" || e["category"] in ids) { "a report example names one of the criteria" }
        val tag = if (e["verdict"] == "report") "verdict=\"report\" category=\"${e["category"]}\"" else "verdict=\"skip\""
        out += "<example $tag>\n<code>${e["code"]}</code>\n<reason>${e["reason"]}</reason>\n</example>"
    }
    out += "</examples>"
    // TODO 1 of 7 (finish this to pass m1): the end of the prompt. After the criteria and examples blocks, add the diff
    //   in a <diff> block and return the whole prompt joined with newlines, so that the diff comes last. Example:
    //   criteria, examples, then <diff>, the diff, </diff>.
    return diff
}

fun categoryReport(findings: List<Map<String, Any?>>, minReviewed: Int = 5, minPrecision: Double = 0.5): Map<String, Any?>? {
    val categories = LinkedHashMap<String, Any?>()
    val disable = mutableListOf<String>()
    for ((name, items) in findings.groupBy { it["category"] as String }) {
        val accepted = items.count { it["verdict"] == "accepted" }
        val counts = items.filter { it["verdict"] == "dismissed" }.groupingBy { it["detected_pattern"] as String }.eachCount()
        // TODO 6 of 7 (finish this to pass e5): the top dismissed patterns. Receives the count of dismissals per
        //   detected pattern. Return up to three [pattern, count] pairs, ordered by count (highest first) and then by
        //   pattern name. Example: {a: 2, b: 3, c: 2, d: 1} -> [b 3], [a 2], [c 2].
        val top = counts.entries.map { listOf(it.key, it.value) }
        val precision = Math.round(accepted.toDouble() / items.size * 100) / 100.0
        // TODO 5 of 7 (finish this to pass e4): the disable flag of a category. Receives the number reviewed, the
        //   precision, min_reviewed and min_precision. A category is disabled when it has at least min_reviewed reviews
        //   and a precision below min_precision. Example: 5 reviews, precision 0.4, defaults -> disabled; 4 reviews ->
        //   not.
        val off = false
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
        // TODO 7 of 7 (finish this to pass e6, e7): the next step. `unresolved` holds the missing fields that have no
        //   default. When some are unresolved and the run is attended, return action ask with those fields in `ask`; when
        //   unattended, return action stop with an empty `ask`. Both carry the assumptions. Example: attended, unresolved
        //   [repo] -> ask [repo].
        else -> mapOf("action" to "proceed", "ask" to emptyList<String>(), "assumptions" to assumptions)
    }
}
