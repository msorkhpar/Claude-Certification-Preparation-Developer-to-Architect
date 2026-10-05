/** Request parameters for thinking, effort and speed, checked per model. See ../../statement.md for the contract. */

private val log = System.getLogger("params")

/** The API would answer 400. [param] names the offending parameter. */
class RejectedRequest(val param: String, reason: String) : RuntimeException("$param: $reason")

private val EFFORTS = listOf("low", "medium", "high", "xhigh", "max")
private const val FAST_BETA = "fast-mode-2026-02-01"
private val DEFAULT_EFFORT = mapOf("claude-fable-5-1" to "high", "claude-opus-5-5" to "medium", "claude-sonnet-5-5" to "high")

private fun family(model: String): String = when {
    model.startsWith("claude-haiku-4-5") -> "claude-haiku-4-5"
    model in DEFAULT_EFFORT -> model
    else -> throw RejectedRequest("model", "unknown model $model")
}

/** Refuse an effort level the model cannot take. */
private fun checkEffort(haiku: Boolean, effort: String) {
    if (haiku) throw RejectedRequest("output_config.effort", "this model does not support effort")
    if (effort !in EFFORTS) throw RejectedRequest("output_config.effort", "$effort is not an effort level")
}

/** Refuse a thinking mode the model does not have. */
private fun checkMode(haiku: Boolean, kind: String) {
    if (kind == "adaptive" && haiku) throw RejectedRequest("thinking", "adaptive thinking is not available on this model")
    if (kind == "enabled" && !haiku) throw RejectedRequest("thinking", "manual thinking budgets are not accepted on this model")
    if (kind == "disabled" && !haiku) throw RejectedRequest("thinking", "thinking cannot be turned off on this model")
}

/** Refuse a manual thinking budget that is missing, below 1024 or not below maxTokens. */
private fun checkBudget(budget: Long?, maxTokens: Int) {
    if (budget == null || budget < 1024 || budget >= maxTokens) {
        throw RejectedRequest("thinking.budget_tokens", "at least 1024 and below max_tokens")
    }
}

/** Refuse between_tools off Sonnet 5.5, or when the effective effort is xhigh or max. */
private fun checkBetweenTools(family: String, effort: String?) {
    if (family != "claude-sonnet-5-5") throw RejectedRequest("thinking", "between_tools exists only on Claude Sonnet 5.5")
    val level = effort ?: DEFAULT_EFFORT.getValue(family)
    if (level == "xhigh" || level == "max") throw RejectedRequest("thinking", "between_tools works at low, medium and high effort only")
}

/** The `thinking` value of the request: the type, and the budget for a manual one. */
private fun thinkingObject(kind: String, budget: Any?): Map<String, Any?> =
    if (kind == "enabled") mapOf("type" to kind, "budget_tokens" to budget) else mapOf("type" to kind)

/** Whether the model accepts this sampling parameter: Haiku all, the others only temperature 1.0. */
private fun samplingAllowed(haiku: Boolean, name: String, value: Any): Boolean =
    haiku || (name == "temperature" && (value as Number).toDouble() == 1.0)

/** The parameters fast mode adds, or a refusal: Opus 5.5 only, and never in a batch. */
private fun fastParams(family: String, batch: Boolean): Map<String, Any?> {
    if (family != "claude-opus-5-5") throw RejectedRequest("speed", "fast mode is not available on this model")
    if (batch) throw RejectedRequest("speed", "fast mode is not available in a batch")
    return mapOf("speed" to "fast", "betas" to listOf(FAST_BETA))
}

@Suppress("UNCHECKED_CAST")
fun buildParams(model: String, maxTokens: Int, options: Map<String, Any?> = emptyMap()): Map<String, Any?> {
    log.log(System.Logger.Level.DEBUG, "buildParams input: {0} {1} {2}", model, maxTokens, options)
    val family = family(model)
    val haiku = family == "claude-haiku-4-5"
    if (maxTokens < 1) throw RejectedRequest("max_tokens", "must be at least 1")
    val params = linkedMapOf<String, Any?>("model" to model, "max_tokens" to maxTokens)

    val effort = options["effort"] as String?
    if (effort != null) {
        checkEffort(haiku, effort)
        params["output_config"] = mapOf("effort" to effort)
    }

    val thinking = options["thinking"] as Map<String, Any?>?
    if (thinking != null) {
        val kind = thinking["type"] as String?
        if (kind !in listOf("adaptive", "enabled", "disabled", "between_tools")) throw RejectedRequest("thinking", "unknown thinking type $kind")
        if (kind == "between_tools") checkBetweenTools(family, effort) else checkMode(haiku, kind!!)
        if (kind == "enabled") checkBudget((thinking["budget_tokens"] as Number?)?.toLong(), maxTokens)
        params["thinking"] = thinkingObject(kind!!, thinking["budget_tokens"])
    }

    for (name in listOf("temperature", "top_p", "top_k")) {
        val value = options[name] ?: continue
        if (!samplingAllowed(haiku, name, value)) throw RejectedRequest(name, "this model rejects a non-default value")
        params[name] = value
    }

    if (options["speed"] == "fast") params.putAll(fastParams(family, options["batch"] == true))
    return params
}
