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

private fun checkEffort(haiku: Boolean, effort: String) {
    // TODO 1 of 7 (finish this to pass e3): refuse an effort level the model cannot take.
    // Receives whether the model is Haiku and the effort given. Throws RejectedRequest("output_config.effort", ...) for Haiku (it has no effort) and for a level that is not in EFFORTS.
    // Example: checkEffort(true, "low") throws, checkEffort(false, "adaptive") throws, checkEffort(false, "high") returns
}

private fun checkMode(haiku: Boolean, kind: String) {
    // TODO 2 of 7 (finish this to pass e1): refuse a thinking mode the model does not have.
    // Receives whether the model is Haiku and the thinking type. Throws RejectedRequest("thinking", ...) for "adaptive" on Haiku, and for "enabled" or "disabled" on any model that is not Haiku.
    // Example: checkMode(false, "disabled") throws, checkMode(true, "disabled") returns
}

private fun checkBudget(budget: Long?, maxTokens: Int) {
    // TODO 3 of 7 (finish this to pass e6): refuse a manual thinking budget that does not fit.
    // Receives the budget (null when missing) and maxTokens. Throws RejectedRequest("thinking.budget_tokens", ...) when the budget is missing, below 1024 or not below maxTokens.
    // Example: checkBudget(1023, 4096) throws, checkBudget(2048, 2048) throws, checkBudget(1024, 2048) returns
}

private fun checkBetweenTools(family: String, effort: String?) {
    // TODO 4 of 7 (finish this to pass e2): refuse between_tools where it does not work.
    // Receives the model family and the effort given (null when absent). Throws RejectedRequest("thinking", ...) unless the family is "claude-sonnet-5-5", and when the
    // effective effort (the one given, else DEFAULT_EFFORT.getValue(family)) is "xhigh" or "max".
    // Example: checkBetweenTools("claude-sonnet-5-5", "max") throws, checkBetweenTools("claude-sonnet-5-5", null) returns
}

private fun thinkingObject(kind: String, budget: Any?): Map<String, Any?> {
    // TODO 5 of 7 (finish this to pass m1 and e7): the `thinking` value of the request.
    // Receives the thinking type and the budget (null unless "enabled"). Returns a map with "type" to kind, plus "budget_tokens" to budget for "enabled". Effort never goes in here.
    // Example: thinkingObject("adaptive", null) -> {type=adaptive}, thinkingObject("enabled", 2048) -> {type=enabled, budget_tokens=2048}
    return emptyMap()
}

private fun samplingAllowed(haiku: Boolean, name: String, value: Any): Boolean {
    // TODO 6 of 7 (finish this to pass e4): whether the model accepts this sampling parameter.
    // Receives whether the model is Haiku, the parameter name (temperature, top_p or top_k) and its value. Returns true for Haiku, and for the others only a temperature of exactly 1.0.
    // Example: samplingAllowed(false, "temperature", 0.2) -> false, samplingAllowed(true, "top_k", 40) -> true
    return true
}

private fun fastParams(family: String, batch: Boolean): Map<String, Any?> {
    // TODO 7 of 7 (finish this to pass e5): the parameters fast mode adds, or a refusal.
    // Receives the model family and whether the request goes into a batch. Throws RejectedRequest("speed", ...) unless the family is "claude-opus-5-5", and when `batch` is true.
    // Otherwise returns a map with "speed" to "fast" and "betas" to listOf(FAST_BETA).
    // Example: fastParams("claude-opus-5-5", false) -> {speed=fast, betas=[fast-mode-2026-02-01]}
    return emptyMap()
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
