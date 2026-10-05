/** What a long exploration keeps outside its context: a scratchpad of findings, a manifest of agent state, and the prompt that resumes an agent after a crash. See ../../statement.md. */

private val log = System.getLogger("recovery")

val STATUSES = listOf("done", "running", "failed")

data class Finding(val area: String, val fact: String, val location: String)

data class AgentEntry(val name: String, val stateFile: String, val status: String)

data class Manifest(val version: Int, val agents: List<AgentEntry>)

data class Action(val name: String, val action: String)

// TODO 1 of 6 (finish this to pass m1): the scratchpad's duplicate rule. Receives the findings so far and a new one
//   (area, fact, location). When a finding with the same area and the same fact already exists, return the findings
//   unchanged; otherwise return them with the new one added last. Example: adding (api, "uses JWT") twice -> one entry.
fun addFinding(findings: List<Finding>, area: String, fact: String, location: String): List<Finding> =
    findings + Finding(area, fact, location)

// TODO 2 of 6 (finish this to pass e1): the lines of one area. Receives all the findings and one area. Return one line
//   "- fact (location)" for each finding of that area only, in order. Example: findings of api and db, area api -> the api
//   lines only.
fun renderScratchpad(findings: List<Finding>): String =
    findings.map { it.area }.distinct().joinToString("\n\n") { area -> "## $area\n" + findings.joinToString("\n") { "- ${it.fact} (${it.location})" } }

fun buildManifest(agents: List<AgentEntry>): Manifest {
    log.log(System.Logger.Level.DEBUG, "buildManifest input: {0}", agents)
    // TODO 3 of 6 (finish this to pass e2): the checks of the manifest. Refuse with an error when two agents have the
    //   same name, and when an agent's status is not one of STATUSES. Example: two agents named a -> refused; status
    //   "paused" -> refused.
    return Manifest(1, agents.sortedBy { it.name })
}

fun resumePlan(manifest: Manifest, existingFiles: Set<String>): List<Action> = manifest.agents.map { a ->
    Action(a.name, when {
        // TODO 4 of 6 (finish this to pass e3, e4, e5): the action for one agent. When its state file does not exist,
        //   restart; otherwise when its status is done, reuse it; otherwise resume it from the state file. Example: done
        //   with its file -> reuse; failed with its file -> resume; any status without its file -> restart.
        else -> "skip"
    })
}

// TODO 5 of 6 (finish this to pass e6): the resume prompt. Receives the task and the state lines. With no state lines,
//   return the task alone; otherwise the task, a blank line, "State from the last run:", one "- line" per state line, and
//   "Continue from the first unfinished step.". Example: [] -> the task.
fun resumePrompt(task: String, stateLines: List<String>): String =
    task

// TODO 6 of 6 (finish this to pass e7): the compact command. Receives the things to keep. Return /compact when there are
//   none, otherwise "/compact Focus on " followed by them joined with ", ". Example: [auth flow, schema] -> /compact Focus
//   on auth flow, schema.
fun compactCommand(keep: List<String>): String = "/compact"
