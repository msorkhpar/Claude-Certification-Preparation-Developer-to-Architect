/** Which Claude Code extension (or API tool) a situation calls for, and why. See ../../statement.md. */

data class Choice(val mechanism: String, val reason: String)

private val SURFACES = listOf("code", "api")
private val KNOWLEDGE = listOf("none", "convention", "reference", "procedure")
private val CARRIED = listOf("skill", "hook", "subagent", "mcp") // what a plugin can bundle

private fun flag(s: Map<String, Any>, key: String) = s[key] == true

fun choose(situation: Map<String, Any>): Choice {
    val surface = situation["surface"]?.toString() ?: "code"
    val knowledge = situation["knowledge"]?.toString() ?: "none"
    val repos = (situation["repos"] as? Number)?.toInt() ?: 1
    require(surface in SURFACES) { "unknown surface: $surface" }
    require(knowledge in KNOWLEDGE) { "unknown knowledge kind: $knowledge" }
    require(repos >= 1) { "repos starts at 1" }
    if (surface == "api") {
        if (flag(situation, "builtin_covers")) return Choice("builtin-tool", "provided-schema")
        if (flag(situation, "external_system") && flag(situation, "remote_server")) return Choice("mcp", "remote-server")
        return Choice("api-tool", "own-schema-and-code")
    }
    val choice = when {
        flag(situation, "guarantee") -> Choice("hook", "must-hold-every-time")
        flag(situation, "external_system") -> Choice("mcp", "external-system")
        flag(situation, "noisy") -> Choice("subagent", "isolate-context")
        knowledge == "convention" -> if (flag(situation, "path_scoped")) Choice("path-rule", "scoped-convention") else Choice("claude-md", "always-known")
        knowledge == "reference" -> Choice("skill", "on-demand-reference")
        knowledge == "procedure" -> Choice("skill", "repeatable-procedure")
        else -> Choice("builtin-tool", "built-in-covers")
    }
    return if (repos >= 2 && choice.mechanism in CARRIED) Choice("plugin", "shared-setup") else choice
}
