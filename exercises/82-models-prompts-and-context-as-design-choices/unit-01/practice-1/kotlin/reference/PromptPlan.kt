/** A prompt plan: modules in cache-friendly order under a token budget, a model for the workload, and what a cache can reuse. See ../../statement.md. Modules, prompts and models are JSON-like maps. */
private val log = System.getLogger("prompt_plan")

const val MIN_CACHEABLE = 512 // tokens: a shorter prefix cannot be cached

private val VARIABLE = Regex("\\{(\\w+)\\}")

/** One token per four characters, rounded up. */
fun tokens(text: String): Int = (text.length + 3) / 4

/** A dynamic module that is still in the prompt. */
data class Kept(val name: String, val text: String, val priority: Int)

private fun block(name: Any?, text: String): Map<String, Any?> = linkedMapOf("name" to name, "text" to text)

/** Replace every {variable} in the text from the variables; a variable with no value is refused. */
fun fill(text: String, variables: Map<String, String>): String = VARIABLE.replace(text) { m ->
    variables[m.groupValues[1]] ?: throw IllegalArgumentException("missing variable: ${m.groupValues[1]}")
}

/** A static module whose text holds a {variable} is refused: a value that changes in the prefix breaks the cache. */
fun checkStatic(stat: List<Map<String, Any?>>) {
    for (m in stat) require(!VARIABLE.containsMatchIn(m["text"] as String)) { "static module ${m["name"]} holds a variable, which would break the cache" }
}

/** The index of the dynamic module to drop first: the lowest priority, and of a tie the later one. */
fun pickVictim(kept: List<Kept>): Int {
    var victim = 0
    for (i in 1 until kept.size) if (kept[i].priority <= kept[victim].priority) victim = i
    return victim
}

/** Drop dynamic modules until prefix + the kept tokens fit the budget; returns the dropped names in order. */
fun fitBudget(prefix: Int, kept: MutableList<Kept>, budget: Int): List<String> {
    val dropped = mutableListOf<String>()
    while (prefix + kept.sumOf { tokens(it.text) } > budget) {
        require(kept.isNotEmpty()) { "over budget: the static modules alone exceed it" }
        dropped += kept.removeAt(pickVictim(kept)).name
    }
    return dropped
}

/** The index of the last static block when there is one and the prefix has at least MIN_CACHEABLE tokens; otherwise null. */
fun breakpointOf(staticCount: Int, prefix: Int): Int? = if (staticCount > 0 && prefix >= MIN_CACHEABLE) staticCount - 1 else null

fun assemble(modules: List<Map<String, Any?>>, variables: Map<String, String>, budget: Int): Map<String, Any?>? {
    log.log(System.Logger.Level.DEBUG, "assemble input: {0}", modules)
    val stat = modules.filter { it["static"] == true }
    val dynamic = modules.filter { it["static"] != true }
    checkStatic(stat)
    val kept = dynamic.map { Kept(it["name"] as String, fill(it["text"] as String, variables), (it["priority"] as Int?) ?: 0) }.toMutableList()
    val blocks = stat.map { block(it["name"], it["text"] as String) }.toMutableList()
    val prefix = stat.sumOf { tokens(it["text"] as String) }
    val dropped = fitBudget(prefix, kept, budget)
    val used = prefix + kept.sumOf { tokens(it.text) }
    blocks += kept.map { block(it.name, it.text) }
    return linkedMapOf("blocks" to blocks, "tokens" to used, "dropped" to dropped, "breakpoint" to breakpointOf(stat.size, prefix))
}

/** The name of the cheapest model that meets the tier and the latency; ties go to the lower name; null when none fits. */
fun chooseModel(workload: Map<String, Any?>, models: List<Map<String, Any?>>): String? {
    val fit = models.filter { (it["tier"] as Int) >= (workload["tier"] as Int) && (it["latency_ms"] as Int) <= (workload["max_latency_ms"] as Int) }
    return fit.sortedWith(compareBy({ it["price_out"] as Int }, { it["name"] as String })).firstOrNull()?.get("name") as String?
}

/** The tokens of the cached prefix two assembled prompts share, or 0. */
@Suppress("UNCHECKED_CAST")
fun reusablePrefix(a: Map<String, Any?>, b: Map<String, Any?>): Int? {
    val ia = a["breakpoint"] as Int?
    if (ia == null || ia != b["breakpoint"]) return 0
    val head = (a["blocks"] as List<Map<String, Any?>>).take(ia + 1)
    return if (head == (b["blocks"] as List<Map<String, Any?>>).take(ia + 1)) head.sumOf { tokens(it["text"] as String) } else 0
}
