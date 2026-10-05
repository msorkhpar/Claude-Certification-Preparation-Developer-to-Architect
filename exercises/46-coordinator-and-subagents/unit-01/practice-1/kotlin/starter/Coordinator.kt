private val log = System.getLogger("coordinator")
/** A coordinator that delegates to isolated subagents, reviews what comes back and synthesizes. See ../../statement.md. */

typealias Planner = (String) -> Map<String, Any?>
typealias Spoke = (String) -> String?
typealias Reviewer = (String, List<Map<String, Any?>>) -> List<String>
typealias Synthesizer = (String, List<Map<String, Any?>>) -> String

/**
 * TODO 1 of 7 (unlocks e3): why a subtask is dropped, or null to keep it.
 * Receives the brief, the lower-cased scope key, the keys already kept, how many are kept and the limit. Returns "empty brief" for a blank
 * brief, else "duplicate scope" for a key already seen, else "over limit" when keptCount has reached maxAgents, else null.
 * Example: dropReason("  ", "a", emptySet(), 0, 3) returns "empty brief".
 */
private fun dropReason(brief: String, key: String, seen: Set<String>, keptCount: Int, maxAgents: Int): String? = null

/**
 * TODO 2 of 7 (unlocks e4): what is wrong with a subagent's report, or null.
 * Receives what the subagent returned (maybe null). Returns "empty report" when it is null or blank, else null.
 * Example: reportProblem("   ") returns "empty report".
 */
private fun reportProblem(report: String?): String? = null

/**
 * TODO 3 of 7 (unlocks e5): the brief for a follow-up on one gap.
 * Returns "Follow up: " + gap, a newline, then "Question: " + question. Example: followUp("2023", "q") returns "Follow up: 2023\nQuestion: q".
 */
private fun followUp(gap: String, question: String): String = gap

/**
 * TODO 4 of 7 (unlocks e5 and e6): is another refinement round allowed?
 * True while there are gaps and fewer than maxRounds rounds have run. Example: mayRefine(listOf("x"), 2, 2) returns false.
 */
private fun mayRefine(gaps: List<String>, rounds: Int, maxRounds: Int): Boolean = false

/**
 * TODO 5 of 7 (unlocks m1 and e6): the status of a run that has an answer.
 * "complete" when no gaps remain and nothing failed, else "partial". Example: finalStatus(listOf("x"), emptyList()) returns "partial".
 */
private fun finalStatus(gaps: List<String>, failed: List<Map<String, Any?>>): String = "complete"

/**
 * TODO 6 of 7 (unlocks e1): does the plan ask for subagents?
 * Receives the planner's map. True when its "delegate" value is true, else false. Example: wantsTeam(mapOf("delegate" to false)) returns false.
 */
private fun wantsTeam(plan: Map<String, Any?>): Boolean = true

/**
 * TODO 7 of 7 (unlocks e5 and e6): the reviewer's gaps, tidied.
 * Receives the reviewer's list (maybe null) and the limit. Returns the gaps trimmed, without empty or repeated ones, in order, at most
 * maxAgents of them. Example: cleanGaps(listOf(" a ", "", "a", "b"), 1) returns ["a"].
 */
private fun cleanGaps(gaps: List<String>?, maxAgents: Int): List<String> = gaps ?: emptyList()

private fun copy(findings: List<Map<String, Any?>>): List<Map<String, Any?>> = findings.map { LinkedHashMap(it) }

@Suppress("UNCHECKED_CAST")
fun coordinate(planner: Planner, subagent: Spoke, reviewer: Reviewer, synthesizer: Synthesizer, question: String, maxAgents: Int = 4, maxRounds: Int = 2): Map<String, Any?>? {
    log.log(System.Logger.Level.DEBUG, "coordinate input: {0}", question)
    val plan = planner(question)
    if (!wantsTeam(plan)) { // a question the coordinator can answer itself is not worth a team
        return linkedMapOf("status" to "direct", "answer" to plan["answer"], "findings" to emptyList<Any?>(), "failed" to emptyList<Any?>(), "dropped" to emptyList<Any?>(),
            "gaps" to emptyList<Any?>(), "rounds" to 0, "subagent_calls" to 0)
    }
    val tasks = mutableListOf<Pair<String, String>>()
    val dropped = mutableListOf<Map<String, Any?>>()
    val seen = mutableSetOf<String>()
    for (task in (plan["subtasks"] as List<Map<String, Any?>>?) ?: emptyList()) {
        val scope = (task["scope"] ?: "").toString()
        val brief = (task["brief"] ?: "").toString()
        val key = scope.trim().lowercase()
        val reason = dropReason(brief, key, seen, tasks.size, maxAgents)
        if (reason != null) dropped.add(linkedMapOf("scope" to scope, "reason" to reason))
        else { seen.add(key); tasks.add(scope to brief) }
    }
    val findings = mutableListOf<Map<String, Any?>>()
    val failed = mutableListOf<Map<String, Any?>>()
    var calls = 0
    fun run(scope: String, brief: String) {
        calls++
        val report = try {
            subagent(brief) // the brief is everything the subagent knows
        } catch (error: RuntimeException) { // one subagent failing must not stop the others
            failed.add(linkedMapOf("scope" to scope, "error" to error.message.toString()))
            return
        }
        val problem = reportProblem(report)
        if (problem != null) failed.add(linkedMapOf("scope" to scope, "error" to problem))
        else findings.add(linkedMapOf("scope" to scope, "text" to report))
    }
    for ((scope, brief) in tasks) run(scope, brief)
    if (findings.isEmpty()) {
        return linkedMapOf("status" to "failed", "answer" to null, "findings" to emptyList<Any?>(), "failed" to failed, "dropped" to dropped, "gaps" to emptyList<Any?>(), "rounds" to 0, "subagent_calls" to calls)
    }
    var rounds = 0
    var gaps = cleanGaps(reviewer(question, copy(findings)), maxAgents)
    while (mayRefine(gaps, rounds, maxRounds)) {
        rounds++
        for (gap in gaps) run(gap, followUp(gap, question))
        gaps = cleanGaps(reviewer(question, copy(findings)), maxAgents)
    }
    val answer = synthesizer(question, copy(findings))
    val status = finalStatus(gaps, failed)
    return linkedMapOf("status" to status, "answer" to answer, "findings" to findings, "failed" to failed, "dropped" to dropped, "gaps" to gaps, "rounds" to rounds, "subagent_calls" to calls)
}
