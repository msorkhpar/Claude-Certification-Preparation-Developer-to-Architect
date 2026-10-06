/** Distributing tools across agents: scoped tool sets, the tool choice of a turn, a check of the reply and the authorisation of a call. See ../../statement.md. Results are JSON-like maps. */

/** Models whose API rejects tool_choice any and tool, as read on 2026-10-03. */
private val log = System.getLogger("distribute")

private val NO_FORCING = setOf("claude-opus-5-5", "claude-sonnet-5-5", "claude-fable-5-1", "claude-mythos-5-1")

@Suppress("UNCHECKED_CAST")
private fun strings(value: Any?): List<String> = (value as List<String>?) ?: emptyList()

private fun shares(tool: Map<String, Any?>, tags: Set<String>) = strings(tool["tags"]).any { it in tags }

/** Give each role the tools of its specialisation, plus what it is explicitly granted, and refuse a set that is too large. */
fun assignTools(roles: Map<String, Map<String, Any?>>, catalog: List<Map<String, Any?>>, budget: Int = 5): Map<String, List<String>>? {
    val byName = linkedMapOf<String, Map<String, Any?>>()
    for (tool in catalog) {
        val name = tool["name"] as String
        require(name !in byName) { "duplicate tool name: $name" }
        byName[name] = tool
    }
    val result = linkedMapOf<String, List<String>>()
    for ((role, spec) in roles) {
        val tags = strings(spec["specialisation"]).toSet()
        require(tags.isNotEmpty()) { "role $role has no specialisation" }
        // TODO 1 of 8 (finish this to pass m1, e2): the tools of a role. Receives the catalog and the role's tags.
        //   Return the names, in catalog order, of the tools that share a tag with the role and are not irreversible (an
        //   irreversible tool is only given by an explicit grant). Example: tags {billing}, catalog [lookup(billing),
        //   refund(billing, irreversible)] -> [lookup].
        val names = byName.keys.toMutableList()
        for (extra in strings(spec["extra"])) {
            val tool = requireNotNull(byName[extra]) { "role $role: unknown tool $extra" }
            require(shares(tool, tags) || tool["scoped"] == true || tool["irreversible"] == true) { "role $role: $extra is outside the specialisation and is not a scoped cross-role tool" }
            if (extra !in names) names.add(extra)
        }
        // TODO 2 of 8 (finish this to pass e1): the budget. When the role would hold more tools than the budget, refuse
        //   with an error that names the role, the count and the budget. Example: 6 tools, budget 5 -> raises; exactly 5
        //   is fine.
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
    // TODO 3 of 8 (finish this to pass e3): the choice for `any` and `named`. Receives the model, the need, the tools,
    //   the forced name and `rejects`. When the model accepts forcing, return tool_choice {type: any} or {type: tool,
    //   name}, the tools, strict false, verify_call false. When it rejects forcing, return tool_choice auto, the tools
    //   (all for any, only the named one for named), strict true and verify_call true. Example: sonnet, named, forced
    //   lookup -> auto, [lookup], strict, verify.
    return linkedMapOf("tool_choice" to linkedMapOf("type" to "auto"), "tools" to tools.toList(), "strict" to false, "verify_call" to false)
}

/** What a change between two requests costs in prompt caching: none, messages (tool_choice changed) or all (the tools changed). */
fun cacheImpact(previous: Map<String, Any?>?, next: Map<String, Any?>): String? {
    if (previous == null) return "none"
    // TODO 4 of 8 (finish this to pass e4): the cost in prompt caching. Receives the previous request and the new one
    //   (the first request has no previous). Return all when the tools differ, otherwise messages when the tool_choice
    //   differs, otherwise none. Example: same tools, auto then {type: any} -> messages.
    return "none"
}

/** Compare a reply with the call that was required: ok, missed_call or wrong_tool. */
fun checkTurn(blocks: List<Map<String, Any?>>, need: String, forced: String? = null): String? {
    if (need != "any" && need != "named") return "ok"
    val calls = blocks.filter { it["type"] == "tool_use" }
    if (calls.isEmpty()) return "missed_call"
    // TODO 5 of 8 (finish this to pass e5): the check of a named call. When the need is named and the first tool call is
    //   not the forced tool, return wrong_tool. Example: need named, forced lookup, first call refund -> wrong_tool.
    return "ok"
}

private fun answer(allowed: Boolean, code: String, message: String, escalate: Boolean = false): Map<String, Any?> =
    linkedMapOf("allowed" to allowed, "code" to code, "message" to message, "escalate" to escalate)

/** Decide a call to a tool that cannot be undone, in the tool layer, whatever the model says. */
@Suppress("UNCHECKED_CAST")
fun authorize(call: Map<String, Any?>, policy: Map<String, Any?>, approvals: Collection<String>): Map<String, Any?>? {
    log.log(System.Logger.Level.DEBUG, "authorize input: {0}", call)
    val tools = (policy["tools"] as Map<String, Map<String, Any?>>?) ?: emptyMap()
    // TODO 6 of 8 (finish this to pass e6): the policy lookup. Find the rule of the called tool in the policy; when the
    //   policy does not name the tool, return the answer (allowed false, code unknown_tool, a message that names the
    //   tool). Example: tool delete_all, policy without it -> unknown_tool.
    val rule = (call["tool"] as? String)?.let { tools[it] } ?: emptyMap()
    val cap = rule["cap"] as Int?
    val amount = call["amount"]
    // TODO 7 of 8 (finish this to pass e6): the amount and the owner. When the tool has a cap and the amount is missing,
    //   a boolean, a decimal or not above zero, return bad_amount. When there is no verified customer or the call's
    //   customer is another, return not_owner. Example: amount 0 -> bad_amount; customer C2 while C1 is verified ->
    //   not_owner.
    // TODO 8 of 8 (finish this to pass e7): the cap and the approval. When the amount is above the cap, return over_cap
    //   with escalate true (an amount equal to the cap is allowed). When the tool is irreversible and the call's id is not
    //   among the approvals, return needs_approval with escalate true. Example: cap 500, amount 501 -> over_cap, even when
    //   approved.
    return answer(true, "ok", "allowed")
}
