/** A prompt plan: modules in cache-friendly order under a token budget, a model for the workload, and what a cache can reuse. See ../../statement.md. Modules, prompts and models are JSON-like maps. */

const val MIN_CACHEABLE = 512 // tokens: a shorter prefix cannot be cached

/** One token per four characters, rounded up. */
fun tokens(text: String): Int = (text.length + 3) / 4

fun assemble(modules: List<Map<String, Any?>>, variables: Map<String, String>, budget: Int): Map<String, Any?>? {
    // TODO: static modules first, then the dynamic ones with their variables filled; drop dynamic modules to fit the budget; mark the breakpoint.
    return null
}

fun chooseModel(workload: Map<String, Any?>, models: List<Map<String, Any?>>): String? {
    // TODO: the name of the cheapest model that meets the tier and the latency, or null.
    return null
}

fun reusablePrefix(a: Map<String, Any?>, b: Map<String, Any?>): Int? {
    // TODO: the tokens of the cached prefix that two assembled prompts share, or 0.
    return null
}
