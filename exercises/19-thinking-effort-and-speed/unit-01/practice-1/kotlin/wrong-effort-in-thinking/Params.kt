/** Request parameters for thinking, effort and speed, checked per model. See ../../statement.md for the contract. */

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

@Suppress("UNCHECKED_CAST")
fun buildParams(model: String, maxTokens: Int, options: Map<String, Any?> = emptyMap()): Map<String, Any?> {
    val family = family(model)
    val haiku = family == "claude-haiku-4-5"
    if (maxTokens < 1) throw RejectedRequest("max_tokens", "must be at least 1")
    val params = linkedMapOf<String, Any?>("model" to model, "max_tokens" to maxTokens)

    val effort = options["effort"] as String?
    if (effort != null) {
        if (haiku) throw RejectedRequest("output_config.effort", "this model does not support effort")
        if (effort !in EFFORTS) throw RejectedRequest("output_config.effort", "$effort is not an effort level")
        params["output_config"] = mapOf("effort" to effort)
    }

    val thinking = options["thinking"] as Map<String, Any?>?
    if (thinking != null) {
        when (val kind = thinking["type"]) {
            "adaptive" -> {
                if (haiku) throw RejectedRequest("thinking", "adaptive thinking is not available on this model")
                params["thinking"] = if (effort == null) mapOf("type" to "adaptive") else mapOf("type" to "adaptive", "effort" to effort)
            }
            "enabled" -> {
                val budget = (thinking["budget_tokens"] as Number?)?.toLong()
                if (!haiku) throw RejectedRequest("thinking", "manual thinking budgets are not accepted on this model")
                if (budget == null || budget < 1024 || budget >= maxTokens) {
                    throw RejectedRequest("thinking.budget_tokens", "at least 1024 and below max_tokens")
                }
                params["thinking"] = mapOf("type" to "enabled", "budget_tokens" to thinking["budget_tokens"])
            }
            "disabled" -> {
                if (!haiku) throw RejectedRequest("thinking", "thinking cannot be turned off on this model")
                params["thinking"] = mapOf("type" to "disabled")
            }
            "between_tools" -> {
                if (family != "claude-sonnet-5-5") throw RejectedRequest("thinking", "between_tools exists only on Claude Sonnet 5.5")
                val level = effort ?: DEFAULT_EFFORT.getValue(family)
                if (level == "xhigh" || level == "max") throw RejectedRequest("thinking", "between_tools works at low, medium and high effort only")
                params["thinking"] = mapOf("type" to "between_tools")
            }
            else -> throw RejectedRequest("thinking", "unknown thinking type $kind")
        }
    }

    for (name in listOf("temperature", "top_p", "top_k")) {
        val value = options[name] ?: continue
        val defaultTemperature = name == "temperature" && (value as Number).toDouble() == 1.0
        if (!haiku && !defaultTemperature) throw RejectedRequest(name, "this model rejects a non-default value")
        params[name] = value
    }

    if (options["speed"] == "fast") {
        if (family != "claude-opus-5-5") throw RejectedRequest("speed", "fast mode is not available on this model")
        if (options["batch"] == true) throw RejectedRequest("speed", "fast mode is not available in a batch")
        params["speed"] = "fast"
        params["betas"] = listOf(FAST_BETA)
    }
    return params
}
