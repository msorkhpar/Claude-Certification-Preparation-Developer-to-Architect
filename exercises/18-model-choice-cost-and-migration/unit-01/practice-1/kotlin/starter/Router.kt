/** A cost model and a model router. See ../../statement.md for the contract. Models, usages and tasks are JSON-like maps. */

/** No model in the catalog can take the task. */
class NoModelError(message: String) : RuntimeException(message)

fun requestCost(model: Map<String, Any?>, usage: Map<String, Any?>, batch: Boolean = false): Double {
    // TODO: the cost of one request in micro-dollars, rounded to 6 decimals.
    return Double.NaN
}

fun route(catalog: List<Map<String, Any?>>, task: Map<String, Any?>): String? {
    // TODO: the id of the cheapest model that can take the task; throw NoModelError when none can.
    return null
}
