/** What a long exploration keeps outside its context: a scratchpad of findings, a manifest of agent state, and the prompt that resumes an agent after a crash. See ../../statement.md. */

private val log = System.getLogger("recovery")

val STATUSES = listOf("done", "running", "failed")

data class Finding(val area: String, val fact: String, val location: String)

data class AgentEntry(val name: String, val stateFile: String, val status: String)

data class Manifest(val version: Int, val agents: List<AgentEntry>)

data class Action(val name: String, val action: String)

fun addFinding(findings: List<Finding>, area: String, fact: String, location: String): List<Finding> =
    if (findings.any { it.area == area && it.fact == fact }) findings.toList() else findings + Finding(area, fact, location)

fun renderScratchpad(findings: List<Finding>): String =
    findings.map { it.area }.distinct().joinToString("\n\n") { area -> "## $area\n" + findings.filter { it.area == area }.joinToString("\n") { "- ${it.fact} (${it.location})" } }

fun buildManifest(agents: List<AgentEntry>): Manifest {
    log.log(System.Logger.Level.DEBUG, "buildManifest input: {0}", agents)
    require(agents.map { it.name }.toSet().size == agents.size) { "duplicate agent name" }
    for (a in agents) require(a.status in STATUSES) { "unknown status ${a.status}" }
    return Manifest(1, agents.sortedBy { it.name })
}

fun resumePlan(manifest: Manifest, existingFiles: Set<String>): List<Action> = manifest.agents.map { a ->
    Action(a.name, when {
        a.stateFile !in existingFiles -> "restart"
        a.status == "done" -> "reuse"
        else -> "resume"
    })
}

fun resumePrompt(task: String, stateLines: List<String>): String =
    if (stateLines.isEmpty()) task else task + "\n\nState from the last run:\n" + stateLines.joinToString("\n") { "- $it" } + "\nContinue from the first unfinished step."

fun compactCommand(keep: List<String>): String = if (keep.isEmpty()) "/compact" else "/compact Focus on " + keep.joinToString(", ")
