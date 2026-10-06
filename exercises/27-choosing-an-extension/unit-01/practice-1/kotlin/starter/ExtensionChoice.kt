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
    // TODO 1 of 8 (finish this to pass e6): refuse numbers and a combination that make no sense.
    // Receives repos, lastsDays, presence and localFiles (already defaulted). Throws IllegalArgumentException (use require) when repos or
    // lastsDays is below 1, or when presence is "away" and localFiles is true (a cloud run starts from a fresh clone). Returns nothing otherwise.
    // Example: checkCounts(0, 1, "session", false) throws; checkCounts(1, 1, "away", true) throws; checkCounts(2, 30, "away", false) returns
}

private fun apiChoice(s: Map<String, Any>): Choice? {
    // TODO 2 of 8 (finish this to pass e5): the pick of an application on the Messages API, or null for the default own tool.
    // Receives the situation. Returns builtin-tool / provided-schema when builtin_covers is true, else mcp / remote-server when
    // external_system and remote_server are both true, else null (the caller then answers api-tool / own-schema-and-code).
    // Example: apiChoice(mapOf("builtin_covers" to true, "remote_server" to true)) -> builtin-tool; apiChoice(mapOf("external_system" to true)) -> null
    return null
}

private fun priorityChoice(s: Map<String, Any>): Choice? {
    // TODO 3 of 8 (finish this to pass e1 and e2): the pick for a rule that must hold, an outside system or noisy work.
    // Receives the situation. The first match wins: guarantee gives hook / must-hold-every-time; external_system gives mcp / external-system;
    // noisy gives subagent / isolate-context; otherwise null. Whatever else is in the situation (timing, knowledge) does not matter here.
    // Example: priorityChoice(mapOf("guarantee" to true, "noisy" to true)) -> hook; priorityChoice(mapOf("timing" to "event")) -> null
    return null
}

private fun eventChoice(presence: String): Choice? {
    // TODO 4 of 8 (finish this to pass e7): the pick for work that reacts to an event.
    // Receives presence. Returns routine / runs-unattended when presence is "away", else monitor / push-not-poll.
    // Example: eventChoice("session") -> monitor / push-not-poll
    return null
}

private fun intervalChoice(s: Map<String, Any>): Choice? {
    // TODO 5 of 8 (finish this to pass e8): the pick for work that repeats every so often.
    // Receives the situation. Returns routine / runs-unattended when presence is "away"; else, when lasts_days is above LOOP_EXPIRY_DAYS,
    // desktop-task / durable-and-local if local_files is true and routine / runs-unattended if not; else loop / session-rhythm.
    // Example: intervalChoice(mapOf("lasts_days" to 8, "local_files" to true)) -> desktop-task; intervalChoice(mapOf("lasts_days" to 7)) -> loop
    return null
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

private fun personalChoice(personal: String): Choice? {
    // TODO 6 of 8 (finish this to pass e9): the pick for a preference of one person, or null.
    // Receives the personal value. Returns output-style / response-voice for "voice", status-line / personal-display for "display",
    // keybinding / personal-keys for "keys", and null for "none".
    // Example: personalChoice("keys") -> keybinding / personal-keys; personalChoice("none") -> null
    return null
}

private fun knowledgeChoice(knowledge: String, pathScoped: Boolean): Choice? {
    // TODO 7 of 8 (finish this to pass e3): the pick for what Claude must be told, or null when nothing needs telling.
    // Receives knowledge and pathScoped. "convention" gives path-rule / scoped-convention when pathScoped, else claude-md / always-known;
    // "reference" gives skill / on-demand-reference; "procedure" gives skill / repeatable-procedure; anything else gives null. pathScoped
    // matters only for a convention.
    // Example: knowledgeChoice("reference", true) -> skill / on-demand-reference; knowledgeChoice("none", true) -> null
    return null
}

private fun packageChoice(choice: Choice, repos: Int): Choice {
    // TODO 8 of 8 (finish this to pass e4): a plugin carries a skill, a hook, a subagent or a server to a second repository.
    // Receives the pick and repos. Returns plugin / shared-setup when repos is 2 or more and the pick's mechanism is in CARRIED; otherwise
    // returns the pick unchanged (an instruction file, a path rule and a built-in tool are never packaged).
    // Example: packageChoice(Choice("hook", "must-hold-every-time"), 2) -> plugin; packageChoice(Choice("claude-md", "always-known"), 2) -> unchanged
    return choice
}

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
