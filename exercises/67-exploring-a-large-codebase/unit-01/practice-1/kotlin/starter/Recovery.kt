/** What a long exploration keeps outside its context: a scratchpad of findings, a manifest of agent state, and the prompt that resumes an agent after a crash. See ../../statement.md. */

val STATUSES = listOf("done", "running", "failed")

data class Finding(val area: String, val fact: String, val location: String)

data class AgentEntry(val name: String, val stateFile: String, val status: String)

data class Manifest(val version: Int, val agents: List<AgentEntry>)

data class Action(val name: String, val action: String)

fun addFinding(findings: List<Finding>, area: String, fact: String, location: String): List<Finding>? {
    // TODO: a new list with the finding added unless the same fact is already recorded for the area.
    return null
}

fun renderScratchpad(findings: List<Finding>): String? {
    // TODO: Markdown text: one "## <area>" heading per area, in first-seen order, with one line per finding.
    return null
}

fun buildManifest(agents: List<AgentEntry>): Manifest? {
    // TODO: version 1 and the agents sorted by name; an error for a duplicate name or an unknown status.
    return null
}

fun resumePlan(manifest: Manifest, existingFiles: Set<String>): List<Action>? {
    // TODO: an action for each agent of the manifest: reuse, resume or restart.
    return null
}

fun resumePrompt(task: String, stateLines: List<String>): String? {
    // TODO: the prompt that continues a task from the state the last run exported.
    return null
}

fun compactCommand(keep: List<String>): String? {
    // TODO: the /compact command, telling it what to keep.
    return null
}
