/**
 * A multi-agent research run in miniature: a coordinator that checks its own decomposition, a search subagent whose failure comes back as structured context,
 * one retry through an alternative, a synthesis agent with a scoped verification tool, and a report that says what it could not cover.
 *
 * The subagents are functions over made-up data: this example is about what the coordinator does with what comes back, not about what a model writes. The
 * shapes (a result with a status, an error with a type, the query, partial results and alternatives) are this course's design, not an Anthropic interface.
 */
val REQUIRED = listOf("visual arts", "music", "writing", "film")

data class Finding(val claim: String, val value: String, val source: String, val date: String)

data class Task(val scope: String, val query: String)

data class SearchError(val type: String, val query: String, val partial: List<Finding>, val alternatives: List<String>, val tried: List<String>)

/** A result: status "ok" with findings, or "error" with its context; recoveredFrom is set when an alternative query succeeded. */
data class Result(val status: String, val findings: List<Finding> = listOf(), val error: SearchError? = null, val recoveredFrom: String? = null)

data class Done(val task: Task, val result: Result)

data class Report(val status: String, val covered: Int, val findings: Int, val errors: Int, val notes: List<String>)

/** What the web search subagent finds for a query. */
val SOURCES = mapOf(
    "AI in digital art" to listOf(Finding("studios using generative tools", "60%", "survey-a", "2025-02-01")),
    "AI in graphic design" to listOf(Finding("designers using generative tools weekly", "48%", "survey-b", "2025-03-10")),
    "AI in photography" to listOf(Finding("agencies labelling generated images", "yes", "policy-c", "2024-11-20")),
    "AI in music" to listOf(Finding("labels licensing their catalogues for training", "3 of 5", "report-d", "2025-01-15")),
    "AI in writing" to listOf(Finding("publishers with an AI policy", "70%", "survey-e", "2025-04-02")),
    "AI in film production" to listOf(Finding("studios testing AI previsualisation", "4 of 6", "report-f", "2025-05-12")),
)
val ALTERNATIVES = mapOf("AI in film" to listOf("AI in film production"))
val PUBLISHED = mapOf("survey-a" to "2025-02-01", "report-d" to "2025-01-15") // what the synthesis agent can check here

/** The search subagent. A failure is returned with its type, the query, the partial results and what to try instead. */
fun search(query: String, down: List<String> = listOf()): Result {
    if (query in down || query !in SOURCES) return Result("error", error = SearchError("timeout", query, listOf(), ALTERNATIVES[query] ?: listOf(), listOf(query)))
    return Result("ok", SOURCES.getValue(query))
}

/** The covered scopes and the gaps, in the order of the required scopes. */
fun coverage(plan: List<Task>): Pair<List<String>, List<String>> {
    val covered = REQUIRED.filter { s -> plan.any { it.scope == s } }
    return covered to REQUIRED.filter { it !in covered }
}

/** The coordinator compares its plan with the scopes the question needs and adds a subtask for each scope the plan leaves out. */
fun replan(plan: List<Task>): List<Task> = plan + coverage(plan).second.map { Task(it, "AI in $it") }

/** One retry through an alternative that the error offered. A second failure stays a failure and keeps both queries. */
fun recover(result: Result, down: List<String>): Result {
    val error = result.error
    if (result.status == "ok" || error == null || error.alternatives.isEmpty()) return result
    val alternative = error.alternatives[0]
    val again = search(alternative, down)
    if (again.status == "ok") return Result("ok", again.findings, null, error.query)
    return Result("error", error = error.copy(tried = error.tried + alternative))
}

fun research(plan: List<Task>, down: List<String> = listOf()): List<Done> = plan.map { Done(it, recover(search(it.query, down), down)) }

/** The synthesis agent's scoped tool: a date it can check here, and anything else goes back to the coordinator. */
fun verifyFact(kind: String, source: String, value: String): String =
    if (kind == "date") (if (PUBLISHED[source] == value) "confirmed" else "mismatch") else "needs_search"

fun report(results: List<Done>): Report {
    val covered = REQUIRED.filter { s -> results.any { it.task.scope == s && it.result.status == "ok" } }
    val notes = results.filter { it.result.status == "error" }.map { r -> "${r.task.scope} not covered: timeout on " + r.result.error!!.tried.joinToString(" and on ") { "'$it'" } }
    val findings = results.filter { it.result.status == "ok" }.sumOf { it.result.findings.size }
    return Report(if (covered.size == REQUIRED.size) "complete" else "partial", covered.size, findings, notes.size, notes.ifEmpty { listOf("nothing left uncovered") })
}

fun main() {
    val narrow = listOf("AI in digital art", "AI in graphic design", "AI in photography").map { Task("visual arts", it) }
    var (covered, gaps) = coverage(narrow)
    println("plan 1: ${narrow.size} subtasks, scopes covered ${covered.size} of ${REQUIRED.size}, gaps: ${gaps.joinToString(", ")}")
    val plan = replan(narrow)
    coverage(plan).let { covered = it.first; gaps = it.second }
    println("plan 2: ${plan.size} subtasks, scopes covered ${covered.size} of ${REQUIRED.size}, gaps: ${gaps.joinToString(", ").ifEmpty { "none" }}")
    val first = search("AI in film")
    val e = first.error!!
    println("search '${e.query}': ${first.status} ${e.type}, ${e.partial.size} partial, alternative '${e.alternatives[0]}'")
    val recovered = research(plan).first { it.result.recoveredFrom != null }
    println("recovered: '${recovered.result.recoveredFrom}' -> '${recovered.task.query}' scope ${recovered.task.scope}, ${recovered.result.findings.size} finding")
    val verdicts = listOf(verifyFact("date", "survey-a", "2025-02-01"), verifyFact("date", "report-d", "2025-01-15"), verifyFact("statistic", "survey-a", "60%"))
    println("verify_fact: ${verdicts.count { it == "confirmed" }} confirmed here, ${verdicts.count { it == "needs_search" }} sent back to the coordinator")
    for ((label, down) in listOf("all sources up" to listOf<String>(), "film search down for good" to listOf("AI in film", "AI in film production"))) {
        val r = report(research(plan, down))
        println("report ($label): status=${r.status}, covered=${r.covered}/${REQUIRED.size}, findings=${r.findings}, errors=${r.errors}")
        println("  note: ${r.notes.joinToString("; ")}")
    }
}
