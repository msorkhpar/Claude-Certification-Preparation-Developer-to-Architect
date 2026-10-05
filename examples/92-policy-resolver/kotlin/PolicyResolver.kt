/**
 * Which value does a developer's Claude Code actually use when a team, a person and an organisation all set the same key? A small resolver that applies the documented precedence, the keys only an organisation can set, the lists that merge and the locks that stop them merging.
 *
 * The layers are invented and the key list is a subset of the settings documentation read on 2026-10-04 (Claude Code settings and managed-settings pages). Nothing here starts Claude Code or calls a model.
 */

private val log = System.getLogger("policy_resolver")

data class Result(val settings: Map<String, Any>, val notes: List<String>)

val LEVELS = listOf("managed", "command line", "local", "project", "user")
val MANAGED_ONLY = setOf("allowManagedPermissionRulesOnly", "allowManagedHooksOnly", "allowManagedMcpServersOnly", "strictKnownMarketplaces", "disableSideloadFlags")
val EFFORT = listOf("low", "medium", "high", "xhigh", "max")

/** The settings Claude Code applies, and a note for every entry that was ignored and why. */
fun effective(layers: Map<String, Map<String, Any>>): Result {
    log.log(System.Logger.Level.DEBUG, "effective input: {0}", layers)
    val managed = layers["managed"] ?: mapOf()
    val lockRules = managed["allowManagedPermissionRulesOnly"] == true
    val lockMcp = managed["allowManagedMcpServersOnly"] == true
    val out = linkedMapOf<String, Any>()
    val notes = mutableListOf<String>()
    for (key in layers.values.flatMap { it.keys }.toSortedSet()) {
        var values = LEVELS.filter { layers[it]?.containsKey(key) == true }.map { it to layers.getValue(it).getValue(key) }
        val reason = when {
            key in MANAGED_ONLY -> "a managed-only key"
            (key == "permissions.allow" || key == "permissions.deny") && lockRules -> "managed settings are the only source of permission rules"
            key == "allowedMcpServers" && lockMcp -> "managed settings are the only source of the MCP allowlist"
            key == "availableModels" && values.any { it.first == "managed" } -> "the managed list applies as it is"
            else -> null
        }
        if (reason != null) {
            for ((level, _) in values) if (level != "managed") notes.add("ignored $key from $level: $reason")
            values = values.filter { it.first == "managed" }
        }
        if (values.isEmpty()) continue
        out[key] = combine(key, values.map { it.second })
    }
    return Result(out, notes)
}

/** Lists merge without duplicates, the lowest effort cap wins, a connector ban from any level stands, and any other key takes the highest level's value. */
@Suppress("UNCHECKED_CAST")
fun combine(key: String, values: List<Any>): Any {
    if (values[0] is List<*>) {
        val merged = mutableListOf<String>()
        for (value in values) for (item in value as List<String>) if (item !in merged) merged.add(item)
        return merged
    }
    if (key == "maxEffortLevel") return (values as List<String>).minByOrNull { EFFORT.indexOf(it) }!!
    if (key == "disableClaudeAiConnectors") return values.any { it == true }
    return values[0]
}

/** A managed list of available models refuses any other choice, whoever makes it. */
@Suppress("UNCHECKED_CAST")
fun allowedModel(requested: String, settings: Map<String, Any>): String {
    val listed = settings["availableModels"] as List<String>?
    return if (listed == null || requested in listed) "allowed" else "refused (not in availableModels)"
}

@Suppress("UNCHECKED_CAST")
fun show(value: Any?): String = when (value) {
    is List<*> -> (value as List<String>).joinToString(", ")
    true -> "True"
    false -> "False"
    else -> value.toString()
}

fun main() {
    val layers = linkedMapOf<String, Map<String, Any>>(
        "managed" to linkedMapOf("allowManagedPermissionRulesOnly" to true, "permissions.deny" to listOf("Read(./.env)"), "permissions.allow" to listOf("Read(./docs/**)"),
            "maxEffortLevel" to "high", "availableModels" to listOf("sonnet", "haiku"), "cleanupPeriodDays" to 7, "spinnerTipsEnabled" to true),
        "command line" to mapOf("cleanupPeriodDays" to 14),
        "local" to mapOf("permissions.allow" to listOf("Bash(npm test)"), "allowManagedHooksOnly" to false),
        "project" to mapOf("permissions.allow" to listOf("Bash(git status)"), "permissions.deny" to listOf("Read(./secrets/**)"), "maxEffortLevel" to "xhigh",
            "availableModels" to listOf("opus"), "disableClaudeAiConnectors" to false, "spinnerTipsEnabled" to false),
        "user" to mapOf("permissions.allow" to listOf("Edit(./notes/**)"), "disableClaudeAiConnectors" to true, "spinnerTipsEnabled" to false),
    )
    val result = effective(layers)
    println("effective settings:")
    for ((key, value) in result.settings) println("  $key = ${show(value)}")
    println("notes:")
    for (note in result.notes) println("  $note")
    for (model in listOf("opus", "haiku")) println("model $model: ${allowedModel(model, result.settings)}")
    val open = linkedMapOf<String, Map<String, Any>>("project" to mapOf("permissions.allow" to listOf("Bash(git status)")), "user" to mapOf("permissions.allow" to listOf("Edit(./notes/**)", "Bash(git status)")))
    println("without a lock the lists merge: ${show(effective(open).settings["permissions.allow"])}")
}
