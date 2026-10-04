/**
 * An approval gate for tools that an agent proposes, in miniature: the proposal is decided from the permissions it asks for, a reviewer answers the ones that need a person,
 * an approved tool runs and its result is checked against the schema it declared, and every step leaves a line in an audit log.
 *
 * The tools are made-up proposals with made-up results: nothing generated is executed here, because this example is about the decisions around a run, not about running code.
 * The shapes (a decision, a reviewer's answer, a result check, an audit line) are this course's design, not an Anthropic interface.
 */
data class Proposal(val name: String, val permissions: List<String>, val result: Map<String, Any>)

data class Decision(val decision: String, val why: List<String>)

val DENIED = listOf("network", "run_process")
val NEEDS_APPROVAL = listOf("write_files")
const val MAX_CHARS = 200

val PROPOSALS = listOf(
    Proposal("read_report", listOf("read_files"), mapOf("headline" to "Q3 up 4%", "rows" to 12)),
    Proposal("write_summary", listOf("read_files", "write_files"), mapOf("headline" to "Q3 up 4%")),
    Proposal("fetch_prices", listOf("network"), mapOf("headline" to "x", "rows" to 1)),
    Proposal("tidy_up", listOf("write_files"), mapOf("headline" to "x", "rows" to 1)),
)
val REVIEWER = mapOf("write_summary" to true, "tidy_up" to false) // the person's answers, by tool name

/** Refused when a denied permission is asked for, held for a person when a permission needs one, otherwise automatic. */
fun decide(permissions: List<String>): Decision {
    val denied = permissions.filter { it in DENIED }
    if (denied.isNotEmpty()) return Decision("refused", denied)
    val gated = permissions.filter { it in NEEDS_APPROVAL }
    if (gated.isNotEmpty()) return Decision("needs_approval", gated)
    return Decision("auto", listOf())
}

/** The result of a tool is data to check before the agent uses it: every declared field, of its declared type, and no more than the limit of characters of text. */
fun checkOutput(result: Map<String, Any>): List<String> {
    val problems = mutableListOf<String>()
    if ("headline" !in result) problems += "missing: headline"
    if ("rows" !in result) problems += "missing: rows"
    if ("headline" in result && result["headline"] !is String) problems += "type: headline"
    if ("rows" in result && result["rows"] !is Int) problems += "type: rows"
    if (result.values.sumOf { if (it is String) it.length else 0 } > MAX_CHARS) problems += "too large"
    return problems
}

fun main() {
    val log = linkedMapOf<String, String>()
    for (proposal in PROPOSALS) {
        val name = proposal.name
        val d = decide(proposal.permissions)
        if (d.decision == "refused") {
            println("$name: refused (${d.why.joinToString(", ")})")
            log[name] = "refused"
            continue
        }
        if (d.decision == "needs_approval") {
            val answer = REVIEWER.getValue(name)
            println("$name: needs_approval -> ${if (answer) "approved" else "declined"}")
            if (!answer) {
                log[name] = "declined"
                continue
            }
        } else {
            println("$name: auto")
        }
        val problems = checkOutput(proposal.result)
        log[name] = if (problems.isEmpty()) "ran" else "rejected"
        if (problems.isNotEmpty()) println("  result of $name rejected (${problems.joinToString("; ")})")
    }
    println("audit: ${PROPOSALS.size} proposals, ${log.values.count { it == "ran" }} ran, ${log.values.count { it == "rejected" }} rejected, ${log.values.count { it == "refused" }} refused, ${log.values.count { it == "declined" }} declined")
}
