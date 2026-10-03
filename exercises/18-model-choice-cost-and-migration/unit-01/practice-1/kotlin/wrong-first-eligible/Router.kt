/** A cost model and a model router. See ../../statement.md for the contract. Models, usages and tasks are JSON-like maps. */

/** No model in the catalog can take the task. */
class NoModelError(message: String) : RuntimeException(message)

private fun n(m: Map<String, Any?>, key: String): Long = (m[key] as Number?)?.toLong() ?: 0L

@Suppress("UNCHECKED_CAST")
private fun created(usage: Map<String, Any?>): Map<String, Any?>? = usage["cache_creation"] as Map<String, Any?>?

/** Cost of one request in micro-dollars (a price of $N per million tokens is N micro-dollars per token). */
fun requestCost(model: Map<String, Any?>, usage: Map<String, Any?>, batch: Boolean = false): Double {
    val price = (model["input"] as Number).toDouble()
    val split = created(usage)
    val five = if (split == null) n(usage, "cache_creation_input_tokens") else n(split, "ephemeral_5m_input_tokens")
    val hour = if (split == null) 0L else n(split, "ephemeral_1h_input_tokens")
    var total = n(usage, "input_tokens") * price +
        five * price * 1.25 +
        hour * price * 2.0 +
        n(usage, "cache_read_input_tokens") * price * (model["cache_read_multiplier"] as Number).toDouble() +
        n(usage, "output_tokens") * (model["output"] as Number).toDouble()
    if (batch) total *= 0.5
    return Math.round(total * 1e6) / 1e6
}

private fun inputTokens(usage: Map<String, Any?>): Long {
    val split = created(usage)
    val written = if (split == null) n(usage, "cache_creation_input_tokens") else n(split, "ephemeral_5m_input_tokens") + n(split, "ephemeral_1h_input_tokens")
    return n(usage, "input_tokens") + n(usage, "cache_read_input_tokens") + written
}

/** The id of the cheapest model that can take the task; throws NoModelError when none can. */
@Suppress("UNCHECKED_CAST")
fun route(catalog: List<Map<String, Any?>>, task: Map<String, Any?>): String {
    val usage = task["usage"] as Map<String, Any?>
    val wanted = n(task, "max_tokens")
    val minTier = if (task.containsKey("min_tier")) n(task, "min_tier") else 1L
    val batch = task["batch"] == true
    val best = catalog
        .filter { it["deprecated"] != true && n(it, "tier") >= minTier && inputTokens(usage) <= n(it, "context") && wanted <= n(it, "max_output") }
        .firstOrNull()
        ?: throw NoModelError("no model can take this task")
    return best["id"] as String
}
