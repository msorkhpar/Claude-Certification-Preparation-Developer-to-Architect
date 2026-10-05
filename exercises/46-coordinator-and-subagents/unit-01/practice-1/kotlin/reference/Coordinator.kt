private val log = System.getLogger("coordinator")
/** A coordinator that delegates to isolated subagents, reviews what comes back and synthesizes. See ../../statement.md. */

typealias Planner = (String) -> Map<String, Any?>
typealias Spoke = (String) -> String?
typealias Reviewer = (String, List<Map<String, Any?>>) -> List<String>
typealias Synthesizer = (String, List<Map<String, Any?>>) -> String

private fun dropReason(brief: String, key: String, seen: Set<String>, keptCount: Int, maxAgents: Int): String? = when {
    brief.isBlank() -> "empty brief"
    key in seen -> "duplicate scope"
    keptCount >= maxAgents -> "over limit"
    else -> null
}

private fun reportProblem(report: String?): String? = if (report == null || report.isBlank()) "empty report" else null

private fun followUp(gap: String, question: String): String = "Follow up: $gap\nQuestion: $question"

private fun mayRefine(gaps: List<String>, rounds: Int, maxRounds: Int): Boolean = gaps.isNotEmpty() && rounds < maxRounds

private fun finalStatus(gaps: List<String>, failed: List<Map<String, Any?>>): String = if (gaps.isEmpty() && failed.isEmpty()) "complete" else "partial"

private fun wantsTeam(plan: Map<String, Any?>): Boolean = plan["delegate"] == true

private fun cleanGaps(gaps: List<String>?, maxAgents: Int): List<String> {
    val out = mutableListOf<String>()
    for (gap in gaps ?: emptyList()) {
        val text = gap.trim()
        if (text.isNotEmpty() && text !in out) out.add(text)
    }
    return out.take(maxAgents)
}

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
