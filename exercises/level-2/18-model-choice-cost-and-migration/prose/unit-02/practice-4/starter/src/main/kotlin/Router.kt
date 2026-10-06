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

private fun cacheCost(model: Map<String, Any?>, usage: Map<String, Any?>): Double {
    // TODO 1 of 6 (finish this to pass e1): the cost of the cache parts of a request: the writes and the reads.
    // Receives the model ("input" price, "cache_read_multiplier") and the usage. Returns the 5-minute write tokens times 1.25 times the input price, plus the 1-hour write
    // tokens times 2 times the input price, plus the cache read tokens times the input price times the model's cache_read_multiplier. writes(usage) gives the two write counts.
    // Example: input price 4, cache_read_input_tokens 1000, multiplier 0.05 -> 200.0
    return 0.0
}

private fun applyBatch(total: Double, batch: Boolean): Double {
    // TODO 2 of 6 (finish this to pass e2): the cost after the batch discount.
    // Receives the total cost and whether the request is a batch. Returns half of the total when `batch` is true, the total unchanged otherwise.
    // Example: applyBatch(1000.0, true) -> 500.0, applyBatch(1000.0, false) -> 1000.0
    return total
}

/** Cost of one request in micro-dollars (a price of $N per million tokens is N micro-dollars per token). */
fun requestCost(model: Map<String, Any?>, usage: Map<String, Any?>, batch: Boolean = false): Double {
    val total = n(usage, "input_tokens") * d(model, "input") + cacheCost(model, usage) + n(usage, "output_tokens") * d(model, "output")
    return Math.round(applyBatch(total, batch) * 1e6) / 1e6
}

private fun inputTokens(usage: Map<String, Any?>): Long {
    // TODO 3 of 6 (finish this to pass e4): the tokens the model has to hold as input.
    // Receives the usage. Returns uncached input tokens plus cache read tokens plus every cache write (both kinds; writes(usage) gives them). A missing field counts as 0.
    // Example: {input_tokens=250000, cache_read_input_tokens=50000} -> 300000
    return 0L
}

private fun canTake(model: Map<String, Any?>, task: Map<String, Any?>, tokens: Long): Boolean {
    // TODO 4 of 6 (finish this to pass m1, e4 and e5): whether the model is allowed and big enough for the task.
    // Receives the model, the task ("min_tier" default 1, "max_tokens" default 0) and the task's total input tokens. Returns true when the model is not deprecated,
    // its tier is at least min_tier, its context holds the input tokens and its max_output holds max_tokens. n(map, key) reads a number, 0 when missing.
    // Example: a model with context 200000 and tokens 250000 -> false
    return false
}

/** The id of the cheapest of `models` for this usage; on a tie the lower tier, then the smaller id. */
private fun cheapest(models: List<Map<String, Any?>>, usage: Map<String, Any?>, batch: Boolean): String {
    // TODO 5 of 6 (finish this to pass m1, e3 and e6): the id of the cheapest of `models` for this usage.
    // Receives a non-empty list of models, the usage and the batch flag. Prices each with requestCost(model, usage, batch) and returns the id of the lowest; on equal
    // cost the lower tier wins, then the smaller id. The order of the list never matters.
    // Example: Sonnet and Opus costing 200000 each -> the Sonnet id (tier 2 before tier 3)
    return ""
}

/** An empty list of models is an error: no model can take the task. */
private fun requireChoice(models: List<Map<String, Any?>>) {
    // TODO 6 of 6 (finish this to pass e5): an empty list of models is an error.
    // Receives the models that can take the task. Throws NoModelError("no model can take this task") when the list is empty; returns nothing otherwise.
    // Example: requireChoice(listOf()) throws NoModelError
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
