/**
 * An eval run, a success gate and a regression comparison, on a scripted classifier.
 *
 * The Claude documentation on success criteria and evaluations (read on 2026-10-03) says to design evals that mirror the real task, edge
 * cases included, to automate the grading, and to judge several dimensions at once ("an F1 score of at least 0.85", "99.5% of outputs are
 * non-toxic"). This file runs six sentiment cases through two scripted versions of a prompt, grades them by exact match, and shows that a
 * better average can still hide a regression. The two models are lookup tables standing in for the application: no model is called.
 */
data class Case(val id: String, val input: String, val expect: String, val tags: List<String>)

data class Result(val id: String, val passed: Boolean, val tags: List<String>)

/** The pass rate of a run, and per tag (passed, total). */
data class Report(val results: List<Result>, val passRate: Double, val byTag: Map<String, Pair<Int, Int>>)

data class Criteria(val minPassRate: Double, val tags: Map<String, Double>)

data class Diff(val regressions: List<String>, val fixed: List<String>)

val CASES = listOf(
    Case("pos-1", "Love it, works great", "positive", listOf("core")),
    Case("neg-1", "Broke after two days", "negative", listOf("core")),
    Case("neu-1", "It arrived on Tuesday", "neutral", listOf("core")),
    Case("sarcasm-1", "Oh great, another crash", "negative", listOf("edge")),
    Case("mixed-1", "Fast shipping but the screen is dim", "neutral", listOf("edge")),
    Case("empty-1", "", "neutral", listOf("edge")),
)

val PROMPT_V1 = mapOf(
    "Love it, works great" to "positive", "Broke after two days" to "negative", "It arrived on Tuesday" to "neutral",
    "Oh great, another crash" to "positive", "Fast shipping but the screen is dim" to "positive", "" to "Neutral",
)
val PROMPT_V2 = PROMPT_V1 + mapOf("Oh great, another crash" to "negative", "Fast shipping but the screen is dim" to "neutral", "" to "positive")

val CRITERIA = Criteria(0.8, mapOf("edge" to 0.75))

/** Code-graded: the answer must equal the expected label, ignoring case and surrounding white space. */
fun grade(case: Case, output: String) = output.trim().lowercase() == case.expect

fun run(cases: List<Case>, model: Map<String, String>): Report {
    val results = cases.map { Result(it.id, grade(it, model.getValue(it.input)), it.tags) }
    val byTag = linkedMapOf<String, Pair<Int, Int>>()
    for (r in results) for (tag in r.tags) {
        val (passed, total) = byTag[tag] ?: (0 to 0)
        byTag[tag] = (passed + if (r.passed) 1 else 0) to (total + 1)
    }
    return Report(results, results.count { it.passed } / results.size.toDouble(), byTag)
}

/** Every dimension of the success criteria must hold, not only the average. */
fun gate(report: Report, criteria: Criteria): List<String> {
    val failures = mutableListOf<String>()
    if (report.passRate < criteria.minPassRate) failures += "overall"
    for ((tag, minimum) in criteria.tags) {
        val (passed, total) = report.byTag.getValue(tag)
        if (passed / total.toDouble() < minimum) failures += "tag:$tag"
    }
    return failures
}

fun compare(baseline: Report, current: Report): Diff {
    val before = baseline.results.associate { it.id to it.passed }
    return Diff(
        regressions = current.results.filter { before.getValue(it.id) && !it.passed }.map { it.id },
        fixed = current.results.filter { !before.getValue(it.id) && it.passed }.map { it.id },
    )
}

private fun py(names: List<String>) = names.joinToString(", ", "[", "]") { "'$it'" }

private fun py(value: Boolean) = if (value) "True" else "False"

fun main() {
    val v1 = run(CASES, PROMPT_V1)
    val v2 = run(CASES, PROMPT_V2)
    for ((name, report) in listOf("prompt v1" to v1, "prompt v2" to v2)) {
        val (passed, total) = report.byTag.getValue("edge")
        println("$name: pass rate ${"%.3f".format(report.passRate)}, edge $passed/$total, gate failures ${py(gate(report, CRITERIA))}")
    }
    val diff = compare(v1, v2)
    println("v2 against v1: fixed ${py(diff.fixed)} regressions ${py(diff.regressions)}")
    println("average improved: ${py(v2.passRate > v1.passRate)} - safe to ship: ${py(diff.regressions.isEmpty() && gate(v2, CRITERIA).isEmpty())}")
}
