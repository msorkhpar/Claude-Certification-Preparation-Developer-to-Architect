import harness.Show.py

/**
 * Three checks that keep a review prompt precise: lint a criterion for vague wording, check a set of few-shot examples, and measure the precision of each finding category from the verdicts developers gave.
 *
 * The rules are the exam guide's for tasks 4.1 and 4.2 (explicit categorical criteria instead of "be conservative", two to four targeted examples that include an acceptable pattern, a category with a high false positive rate is switched
 * off while its prompt is improved) and the prompting guide's advice on examples (read on 2026-10-03: relevant, diverse and structured, three to five). No model is called.
 */
val VAGUE = listOf("be conservative", "high-confidence", "high confidence", "only important", "only significant", "if you are sure", "use your judgment")

/** A review criterion: what to report, what to skip, and a concrete example for each severity level. */
data class Criterion(val report: String?, val skip: String?, val severity: Map<String, String>?)

/** One few-shot example: whether the reviewer reports or skips the code shown, and why. */
data class Example(val verdict: String, val reason: String? = null)

/** What the verdicts say about one category. */
data class Row(val reviewed: Int, val accepted: Int, val precision: Double, val off: Boolean)

/** Rule ids a review criterion breaks: wording that names no pattern, a missing skip list, a severity level without a concrete example. */
fun lintCriterion(criterion: Criterion): List<String> {
    val found = mutableListOf<String>()
    for ((key, text) in listOf("report" to (criterion.report ?: ""), "skip" to (criterion.skip ?: ""))) {
        if (text.isBlank()) found += "no-$key" else if (VAGUE.any { it in text.lowercase() }) found += "vague-$key"
    }
    for (level in listOf("high", "low")) if ('`' !in (criterion.severity?.get(level) ?: "")) found += "no-$level-example"
    return found
}

/** Rule ids for a set of few-shot examples: how many, whether both a finding and an acceptable pattern are shown, whether each says why. */
fun lintExamples(examples: List<Example>): List<String> {
    val found = mutableListOf<String>()
    if (examples.size !in 2..4) found += "two-to-four"
    if (examples.map { it.verdict }.toSet() != setOf("report", "skip")) found += "both-verdicts"
    if (examples.any { it.reason.isNullOrEmpty() }) found += "reason-missing"
    return found
}

/** Per category: how many findings were reviewed, the share developers accepted, and whether to switch the category off while its prompt is improved. */
fun trust(verdicts: List<Pair<String, String>>, minReviewed: Int = 5, minPrecision: Double = 0.5): Map<String, Row> {
    val table = linkedMapOf<String, Row>()
    for ((category, verdict) in verdicts.groupBy({ it.first }, { it.second })) {
        val reviewed = verdict.size
        val accepted = verdict.count { it == "accepted" }
        val precision = Math.round(accepted.toDouble() / reviewed * 100) / 100.0
        table[category] = Row(reviewed, accepted, precision, reviewed >= minReviewed && precision < minPrecision)
    }
    return table
}

val VAGUE_CRITERION = Criterion("Be conservative and only flag important problems.", "", mapOf("high" to "Something serious.", "low" to "A small thing."))
val GOOD_CRITERION = Criterion(
    "A comment whose claimed behaviour contradicts the code.", "Minor style and patterns the codebase already uses.",
    mapOf("high" to "A null dereference such as `user.profile.name` when `user` may be None.", "low" to "A misleading name such as `total` for a count."),
)

fun repeat(category: String, verdict: String, times: Int): List<Pair<String, String>> = List(times) { category to verdict }

fun main() {
    println("vague criterion: ${py(lintCriterion(VAGUE_CRITERION))}")
    println("good criterion: ${py(lintCriterion(GOOD_CRITERION))}")
    val oneSide = listOf(
        Example("report", "The comment says sum, the code multiplies."), Example("report", ""), Example("report", "Unchecked None."), Example("report", "Off by one."), Example("report", "Wrong key."),
    )
    println("five reports, one without a reason: ${py(lintExamples(oneSide))}")
    println("a report and a skip: ${py(lintExamples(listOf(Example("report", "r"), Example("skip", "s"))))}")
    val verdicts = repeat("bug", "accepted", 9) + repeat("bug", "dismissed", 1) + repeat("style", "accepted", 2) + repeat("style", "dismissed", 6) + repeat("naming", "dismissed", 3)
    for ((category, row) in trust(verdicts)) println("$category: reviewed ${row.reviewed}, precision ${row.precision}, switch off: ${py(row.off)}")
}
