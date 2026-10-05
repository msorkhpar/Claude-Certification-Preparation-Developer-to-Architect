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

/** The mechanism for work that runs without a person, on a rhythm or on an event, or null when the situation has none of these. */
private fun rhythm(s: Map<String, Any>): Choice? {
    val timing = s["timing"]?.toString() ?: "none"
    val presence = s["presence"]?.toString() ?: "session"
    val days = (s["lasts_days"] as? Number)?.toInt() ?: 1
    if (presence == "pipeline") return Choice("headless-ci", "no-person-present")
    if (timing == "condition") return Choice("goal", "until-condition-holds")
    if (timing == "background") return Choice("background-task", "work-while-it-runs")
    if (timing == "event") return if (presence == "away") Choice("routine", "runs-unattended") else Choice("monitor", "push-not-poll")
    if (timing == "interval") {
        if (presence == "away") return Choice("routine", "runs-unattended")
        if (days > LOOP_EXPIRY_DAYS) return if (flag(s, "local_files")) Choice("desktop-task", "durable-and-local") else Choice("routine", "runs-unattended")
        return Choice("loop", "session-rhythm")
    }
    return null
}

fun choose(situation: Map<String, Any>): Choice {
    val surface = situation["surface"]?.toString() ?: "code"
    val knowledge = situation["knowledge"]?.toString() ?: "none"
    val repos = (situation["repos"] as? Number)?.toInt() ?: 1
    require(surface in SURFACES) { "unknown surface: $surface" }
    require(knowledge in KNOWLEDGE) { "unknown knowledge kind: $knowledge" }
    require(repos >= 1) { "repos starts at 1" }
    require((situation["timing"]?.toString() ?: "none") in TIMINGS) { "unknown timing: ${situation["timing"]}" }
    require((situation["presence"]?.toString() ?: "session") in PRESENCE) { "unknown presence: ${situation["presence"]}" }
    require((situation["personal"]?.toString() ?: "none") in PERSONAL) { "unknown personal preference: ${situation["personal"]}" }
    require(((situation["lasts_days"] as? Number)?.toInt() ?: 1) >= 1) { "lasts_days starts at 1" }
    require(!((situation["presence"]?.toString() ?: "session") == "away" && flag(situation, "local_files"))) { "a cloud run starts from a fresh clone and sees no local files" }
    if (surface == "api") {
        if (flag(situation, "builtin_covers")) return Choice("builtin-tool", "provided-schema")
        if (flag(situation, "external_system") && flag(situation, "remote_server")) return Choice("mcp", "remote-server")
        return Choice("api-tool", "own-schema-and-code")
    }
    val choice = when {
        flag(situation, "guarantee") -> Choice("hook", "must-hold-every-time")
        flag(situation, "external_system") -> Choice("mcp", "external-system")
        flag(situation, "noisy") -> Choice("subagent", "isolate-context")
        rhythm(situation) != null -> rhythm(situation)!!
        situation["personal"] == "voice" -> Choice("output-style", "response-voice")
        situation["personal"] == "display" -> Choice("status-line", "personal-display")
        situation["personal"] == "keys" -> Choice("keybinding", "personal-keys")
        knowledge == "convention" -> if (flag(situation, "path_scoped")) Choice("path-rule", "scoped-convention") else Choice("claude-md", "always-known")
        knowledge == "reference" -> Choice("skill", "on-demand-reference")
        knowledge == "procedure" -> Choice("skill", "repeatable-procedure")
        else -> Choice("builtin-tool", "built-in-covers")
    }
    return if (repos >= 2 && choice.mechanism in CARRIED) Choice("plugin", "shared-setup") else choice
}
