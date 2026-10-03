/** Distributing tools across agents: scoped tool sets, the tool choice of a turn, a check of the reply and the authorisation of a call. See ../../statement.md. Results are JSON-like maps. */

/** Models whose API rejects tool_choice any and tool, as read on 2026-10-03. */
private val NO_FORCING = setOf("claude-opus-5-5", "claude-sonnet-5-5", "claude-fable-5-1", "claude-mythos-5-1")

@Suppress("UNCHECKED_CAST")
private fun strings(value: Any?): List<String> = (value as List<String>?) ?: emptyList()

private fun shares(tool: Map<String, Any?>, tags: Set<String>) = strings(tool["tags"]).any { it in tags }

/** Give each role the tools of its specialisation, plus what it is explicitly granted, and refuse a set that is too large. */
fun assignTools(roles: Map<String, Map<String, Any?>>, catalog: List<Map<String, Any?>>, budget: Int = 5): Map<String, List<String>>? {
    val byName = linkedMapOf<String, Map<String, Any?>>()
    for (tool in catalog) {
        val name = tool["name"] as String
        byName[name] = tool
    }
    val result = linkedMapOf<String, List<String>>()
    for ((role, spec) in roles) {
        val tags = strings(spec["specialisation"]).toSet()
        require(tags.isNotEmpty()) { "role $role has no specialisation" }
        val names = catalog.filter { shares(it, tags) && it["irreversible"] != true }.map { it["name"] as String }.toMutableList()
        for (extra in strings(spec["extra"])) {
            val tool = requireNotNull(byName[extra]) { "role $role: unknown tool $extra" }
            require(shares(tool, tags) || tool["scoped"] == true || tool["irreversible"] == true) { "role $role: $extra is outside the specialisation and is not a scoped cross-role tool" }
            if (extra !in names) names.add(extra)
        }
        require(names.size <= budget) { "role $role has ${names.size} tools, more than the budget of $budget" }
        result[role] = names
    }
    return result
}

/** The tool_choice and tool list of one request, with the portable fallback where forcing is not accepted. */
fun planTurn(model: String, need: String, tools: List<String>, forced: String? = null, manualThinking: Boolean = false): Map<String, Any?>? {
    require(need in listOf("free", "none", "any", "named")) { "unknown need: $need" }
    require(need != "named" || (forced != null && forced in tools)) { "a named choice needs a tool from the list" }
    val rejects = model in NO_FORCING || manualThinking
    if (need == "free") return linkedMapOf("tool_choice" to linkedMapOf("type" to "auto"), "tools" to tools.toList(), "strict" to false, "verify_call" to false)
    if (need == "none") return linkedMapOf("tool_choice" to linkedMapOf("type" to "none"), "tools" to tools.toList(), "strict" to false, "verify_call" to false)
    if (!rejects) {
        val choice = if (need == "any") linkedMapOf("type" to "any") else linkedMapOf("type" to "tool", "name" to forced)
        return linkedMapOf("tool_choice" to choice, "tools" to tools.toList(), "strict" to false, "verify_call" to false)
    }
    return linkedMapOf("tool_choice" to linkedMapOf("type" to "auto"), "tools" to (if (need == "any") tools.toList() else listOf(forced)), "strict" to true, "verify_call" to true)
}

/** What a change between two requests costs in prompt caching: none, messages (tool_choice changed) or all (the tools changed). */
fun cacheImpact(previous: Map<String, Any?>?, next: Map<String, Any?>): String? {
    if (previous == null) return "none"
    if (previous["tools"] != next["tools"]) return "all"
    if (previous["tool_choice"] != next["tool_choice"]) return "messages"
    return "none"
}

/** Compare a reply with the call that was required: ok, missed_call or wrong_tool. */
fun checkTurn(blocks: List<Map<String, Any?>>, need: String, forced: String? = null): String? {
    if (need != "any" && need != "named") return "ok"
    val calls = blocks.filter { it["type"] == "tool_use" }
    if (calls.isEmpty()) return "missed_call"
    if (need == "named" && calls[0]["name"] != forced) return "wrong_tool"
    return "ok"
}

private fun answer(allowed: Boolean, code: String, message: String, escalate: Boolean = false): Map<String, Any?> =
    linkedMapOf("allowed" to allowed, "code" to code, "message" to message, "escalate" to escalate)

/** Decide a call to a tool that cannot be undone, in the tool layer, whatever the model says. */
@Suppress("UNCHECKED_CAST")
fun authorize(call: Map<String, Any?>, policy: Map<String, Any?>, approvals: Collection<String>): Map<String, Any?>? {
    val tools = (policy["tools"] as Map<String, Map<String, Any?>>?) ?: emptyMap()
    val rule = (call["tool"] as? String)?.let { tools[it] } ?: return answer(false, "unknown_tool", "${call["tool"]} is not an allowed tool")
    val cap = rule["cap"] as Int?
    val amount = call["amount"]
    if (cap != null && !(amount is Int && amount > 0)) return answer(false, "bad_amount", "the amount must be a positive whole number")
    val verified = call["verified_customer"]
    if (verified == null || verified != call["customer"]) return answer(false, "not_owner", "the call is not for the verified customer")
    if (cap != null && (amount as Int) > cap) return answer(false, "over_cap", "$amount is above the limit of $cap: send this to a person", true)
    if (rule["irreversible"] == true && (call["id"] as String) !in approvals) return answer(false, "needs_approval", "this call cannot be undone: a person must approve it first", true)
    return answer(true, "ok", "allowed")
}
