/**
 * Surviving a crash during a long exploration: each agent exports its state to a known place, and the coordinator reads a manifest on resume.
 *
 * The exam guide (task 5.4) describes crash recovery as agents that export structured state to a known location and a coordinator that loads a manifest on resume and injects the state into the prompts of the agents it
 * restarts. The Claude Code documentation (read 2026-10-04) says that subagents explore in a separate context and report back summaries, and that a context window which fills up degrades Claude's work. Below, a map
 * stands for the file system, three agents explore three modules, one crashes, and the coordinator recovers. The sizes of the transcripts are invented for the illustration; nothing here calls a model.
 */
const val MANIFEST = "state/manifest.txt"

data class Entry(val status: String, val path: String)

data class Finding(val fact: String, val where: String)

data class Step(val agent: String, val action: String)

fun tokens(chars: Int): Int = (chars + 3) / 4

fun tokens(text: String): Int = tokens(text.length)

fun readManifest(fs: Map<String, String>): LinkedHashMap<String, Entry> {
    val entries = linkedMapOf<String, Entry>()
    for (line in (fs[MANIFEST] ?: "").lines().filter { it.isNotEmpty() }) {
        val (name, status, path) = line.split("|")
        entries[name] = Entry(status, path)
    }
    return entries
}

private fun writeManifest(fs: MutableMap<String, String>, entries: Map<String, Entry>) {
    fs[MANIFEST] = entries.entries.joinToString("\n") { "${it.key}|${it.value.status}|${it.value.path}" }
}

/** The manifest is written when the agent starts, so that a crash leaves a trace. */
fun start(fs: MutableMap<String, String>, agent: String) {
    val entries = readManifest(fs)
    entries[agent] = Entry("running", "state/$agent.md")
    writeManifest(fs, entries)
}

/** The state file is written when the agent has something to keep, and the manifest then says done. */
fun finish(fs: MutableMap<String, String>, agent: String, findings: List<Finding>) {
    val entries = readManifest(fs)
    val path = entries.getValue(agent).path
    fs[path] = findings.joinToString("\n") { "- ${it.fact} (${it.where})" }
    entries[agent] = Entry("done", path)
    writeManifest(fs, entries)
}

fun recoveryPlan(fs: Map<String, String>, planned: List<String>): List<Step> {
    val entries = readManifest(fs)
    return planned.map { agent ->
        val e = entries[agent] ?: Entry("running", "state/$agent.md")
        Step(agent, if (e.path !in fs) "restart" else if (e.status == "done") "reuse" else "resume")
    }
}

/** What the coordinator puts into the next phase's prompt: the exported findings of every agent that need not run again. */
fun injectedState(fs: Map<String, String>, plan: List<Step>): String {
    val entries = readManifest(fs)
    return plan.filter { it.action == "reuse" || it.action == "resume" }.joinToString("\n") { "${it.agent}:\n${fs.getValue(entries.getValue(it.agent).path)}" }
}

fun main() {
    val fs = linkedMapOf<String, String>()
    val work = linkedMapOf(
        "auth" to listOf(Finding("sessions expire after 30 minutes", "auth/Session.java:18"), Finding("tokens are signed in TokenSigner", "auth/TokenSigner.java:12"), Finding("the login route is POST /login", "auth/Routes.java:7")),
        "billing" to listOf(Finding("amounts are integer cents", "billing/Money.java:5"), Finding("refunds go through RefundService", "billing/RefundService.java:41")),
    )
    for ((agent, findings) in work) {
        start(fs, agent)
        finish(fs, agent, findings)
        println("$agent: exported ${readManifest(fs).getValue(agent).path} (${findings.size} findings), manifest says ${readManifest(fs).getValue(agent).status}")
    }
    start(fs, "search")
    println("search: manifest says running, state file never written (crash)")
    val plan = recoveryPlan(fs, listOf("auth", "billing", "search"))
    println("recovery plan: " + plan.joinToString(", ") { "${it.agent} ${it.action}" })
    val state = injectedState(fs, plan)
    val replay = tokens(3200) + tokens(2400) + tokens(1600)
    println("injected into the next phase: ${work.values.sumOf { it.size }} findings from ${work.size} agents, about ${tokens(state)} tokens")
    println("replaying the three transcripts instead: about $replay tokens")
}
