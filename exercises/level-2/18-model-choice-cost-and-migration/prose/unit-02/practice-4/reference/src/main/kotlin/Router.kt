/** A cost model and a model router. See ../../statement.md for the contract. Models, usages and tasks are JSON-like maps. */
private val log = System.getLogger("router")

/** No model in the catalog can take the task. */
class NoModelError(message: String) : RuntimeException(message)

private fun n(m: Map<String, Any?>, key: String): Long = (m[key] as Number?)?.toLong() ?: 0L

private fun d(m: Map<String, Any?>, key: String): Double = (m[key] as Number).toDouble()

/** The cache-write tokens as (5-minute kind, 1-hour kind); an unsplit count is all the 5-minute kind. */
@Suppress("UNCHECKED_CAST")
private fun writes(usage: Map<String, Any?>): Pair<Long, Long> {
    val split = usage["cache_creation"] as Map<String, Any?>? ?: return Pair(n(usage, "cache_creation_input_tokens"), 0L)
    return Pair(n(split, "ephemeral_5m_input_tokens"), n(split, "ephemeral_1h_input_tokens"))
}

/** The cost of the cache parts of a request: the writes and the reads. */
private fun cacheCost(model: Map<String, Any?>, usage: Map<String, Any?>): Double {
    val (five, hour) = writes(usage)
    val price = d(model, "input")
    return five * price * 1.25 + hour * price * 2.0 + n(usage, "cache_read_input_tokens") * price * d(model, "cache_read_multiplier")
}

/** The cost after the batch discount: half of it when `batch` is true. */
private fun applyBatch(total: Double, batch: Boolean): Double = if (batch) total * 0.5 else total

/** Cost of one request in micro-dollars (a price of $N per million tokens is N micro-dollars per token). */
fun requestCost(model: Map<String, Any?>, usage: Map<String, Any?>, batch: Boolean = false): Double {
    val total = n(usage, "input_tokens") * d(model, "input") + cacheCost(model, usage) + n(usage, "output_tokens") * d(model, "output")
    return Math.round(applyBatch(total, batch) * 1e6) / 1e6
}

/** The tokens the model has to hold as input: uncached input, cache reads and every cache write. */
private fun inputTokens(usage: Map<String, Any?>): Long {
    val (five, hour) = writes(usage)
    return n(usage, "input_tokens") + n(usage, "cache_read_input_tokens") + five + hour
}

/** Whether the model is allowed and big enough for the task. */
private fun canTake(model: Map<String, Any?>, task: Map<String, Any?>, tokens: Long): Boolean {
    val minTier = if (task.containsKey("min_tier")) n(task, "min_tier") else 1L
    return model["deprecated"] != true && n(model, "tier") >= minTier && tokens <= n(model, "context") && n(task, "max_tokens") <= n(model, "max_output")
}

/** The id of the cheapest of `models` for this usage; on a tie the lower tier, then the smaller id. */
private fun cheapest(models: List<Map<String, Any?>>, usage: Map<String, Any?>, batch: Boolean): String {
    val best = models.minWithOrNull(compareBy<Map<String, Any?>> { requestCost(it, usage, batch) }.thenBy { n(it, "tier") }.thenBy { it["id"] as String })
    return best?.get("id") as String? ?: ""
}

/** An empty list of models is an error: no model can take the task. */
private fun requireChoice(models: List<Map<String, Any?>>) {
    if (models.isEmpty()) throw NoModelError("no model can take this task")
}

/** The id of the cheapest model that can take the task; throws NoModelError when none can. */
@Suppress("UNCHECKED_CAST")
fun route(catalog: List<Map<String, Any?>>, task: Map<String, Any?>): String {
    log.log(System.Logger.Level.DEBUG, "route input: {0}", task)
    val usage = task["usage"] as Map<String, Any?>
    val tokens = inputTokens(usage)
    val eligible = catalog.filter { canTake(it, task, tokens) }
    requireChoice(eligible)
    return cheapest(eligible, usage, task["batch"] == true)
}
