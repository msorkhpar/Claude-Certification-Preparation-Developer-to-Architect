private val log = System.getLogger("extension_choice")

/** Which Claude Code extension (or API tool) a situation calls for, and why. See ../../statement.md. */

data class Choice(val mechanism: String, val reason: String)

private val SURFACES = listOf("code", "api")
private val KNOWLEDGE = listOf("none", "convention", "reference", "procedure")
private val CARRIED = listOf("skill", "hook", "subagent", "mcp") // what a plugin can bundle
private val TIMINGS = listOf("none", "interval", "event", "condition", "background")
private val PRESENCE = listOf("session", "pipeline", "away")
private val PERSONAL = listOf("none", "voice", "display", "keys")
private const val LOOP_EXPIRY_DAYS = 7 // a recurring task of a session expires after seven days

private fun flag(s: Map<String, Any>, key: String) = s[key] == true

private fun text(s: Map<String, Any>, key: String, fallback: String) = s[key]?.toString() ?: fallback

private fun number(s: Map<String, Any>, key: String) = (s[key] as? Number)?.toInt() ?: 1

private fun checkCounts(repos: Int, lastsDays: Int, presence: String, localFiles: Boolean) {
    require(repos >= 1 && lastsDays >= 1 && !(presence == "away" && localFiles)) { "repos and lasts_days start at 1, and a cloud run starts from a fresh clone and sees no local files" }
}

private fun apiChoice(s: Map<String, Any>): Choice? {
    if (flag(s, "builtin_covers")) return Choice("builtin-tool", "provided-schema")
    if (flag(s, "external_system") && flag(s, "remote_server")) return Choice("mcp", "remote-server")
    return null
}

private fun priorityChoice(s: Map<String, Any>): Choice? {
    if (flag(s, "guarantee")) return Choice("hook", "must-hold-every-time")
    if (flag(s, "external_system")) return Choice("mcp", "external-system")
    if (flag(s, "noisy")) return Choice("subagent", "isolate-context")
    return null
}

private fun eventChoice(presence: String): Choice? =
    if (presence == "away") Choice("routine", "runs-unattended") else Choice("monitor", "push-not-poll")

private fun intervalChoice(s: Map<String, Any>): Choice? {
    if (text(s, "presence", "session") == "away") return Choice("routine", "runs-unattended")
    if (number(s, "lasts_days") > LOOP_EXPIRY_DAYS) return if (flag(s, "local_files")) Choice("desktop-task", "durable-and-local") else Choice("routine", "runs-unattended")
    return Choice("loop", "session-rhythm")
}

/** The pick for work that runs without a person, on a rhythm or on an event, or null when the situation has none of these. */
private fun rhythm(s: Map<String, Any>): Choice? {
    val timing = text(s, "timing", "none")
    val presence = text(s, "presence", "session")
    if (presence == "pipeline") return Choice("headless-ci", "no-person-present")
    if (timing == "condition") return Choice("goal", "until-condition-holds")
    if (timing == "background") return Choice("background-task", "work-while-it-runs")
    if (timing == "event") return eventChoice(presence)
    if (timing == "interval") return intervalChoice(s)
    return null
}

private fun personalChoice(personal: String): Choice? = when (personal) {
    "voice" -> Choice("output-style", "response-voice")
    "display" -> Choice("status-line", "personal-display")
    "keys" -> Choice("keybinding", "personal-keys")
    else -> null
}

private fun knowledgeChoice(knowledge: String, pathScoped: Boolean): Choice? = when (knowledge) {
    "convention" -> if (pathScoped) Choice("path-rule", "scoped-convention") else Choice("claude-md", "always-known")
    "reference" -> Choice("skill", "on-demand-reference")
    "procedure" -> Choice("skill", "repeatable-procedure")
    else -> null
}

private fun packageChoice(choice: Choice, repos: Int): Choice =
    if (repos >= 2 && choice.mechanism in CARRIED) Choice("plugin", "shared-setup") else choice

fun choose(situation: Map<String, Any>): Choice {
    log.log(System.Logger.Level.DEBUG, "choose input: {0}", situation)
    val surface = text(situation, "surface", "code")
    val knowledge = text(situation, "knowledge", "none")
    val repos = number(situation, "repos")
    require(surface in SURFACES) { "unknown surface: $surface" }
    require(knowledge in KNOWLEDGE) { "unknown knowledge kind: $knowledge" }
    require(text(situation, "timing", "none") in TIMINGS) { "unknown timing: ${situation["timing"]}" }
    require(text(situation, "presence", "session") in PRESENCE) { "unknown presence: ${situation["presence"]}" }
    require(text(situation, "personal", "none") in PERSONAL) { "unknown personal preference: ${situation["personal"]}" }
    checkCounts(repos, number(situation, "lasts_days"), text(situation, "presence", "session"), flag(situation, "local_files"))
    if (surface == "api") return apiChoice(situation) ?: Choice("api-tool", "own-schema-and-code")
    val choice = priorityChoice(situation)
        ?: rhythm(situation)
        ?: personalChoice(text(situation, "personal", "none"))
        ?: knowledgeChoice(knowledge, flag(situation, "path_scoped"))
        ?: Choice("builtin-tool", "built-in-covers")
    return packageChoice(choice, repos)
}
