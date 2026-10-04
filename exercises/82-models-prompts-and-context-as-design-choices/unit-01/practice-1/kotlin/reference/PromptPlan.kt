/** A prompt plan: modules in cache-friendly order under a token budget, a model for the workload, and what a cache can reuse. See ../../statement.md. Modules, prompts and models are JSON-like maps. */

const val MIN_CACHEABLE = 512 // tokens: a shorter prefix cannot be cached

private val VARIABLE = Regex("\\{(\\w+)\\}")

/** One token per four characters, rounded up. */
fun tokens(text: String): Int = (text.length + 3) / 4

private fun fill(text: String, variables: Map<String, String>): String = VARIABLE.replace(text) { m ->
    variables[m.groupValues[1]] ?: throw IllegalArgumentException("missing variable: ${m.groupValues[1]}")
}

private fun block(name: Any?, text: String): Map<String, Any?> = linkedMapOf("name" to name, "text" to text)

fun assemble(modules: List<Map<String, Any?>>, variables: Map<String, String>, budget: Int): Map<String, Any?>? {
    val stat = modules.filter { it["static"] == true }
    val dynamic = modules.filter { it["static"] != true }
    for (m in stat) require(!VARIABLE.containsMatchIn(m["text"] as String)) { "static module ${m["name"]} holds a variable, which would break the cache" }
    val kept = dynamic.map { Triple(it["name"] as String, fill(it["text"] as String, variables), (it["priority"] as Int?) ?: 0) }.toMutableList()
    val blocks = stat.map { block(it["name"], it["text"] as String) }.toMutableList()
    val prefix = stat.sumOf { tokens(it["text"] as String) }
    val dropped = mutableListOf<String>()
    while (prefix + kept.sumOf { tokens(it.second) } > budget) {
        require(kept.isNotEmpty()) { "over budget: the static modules alone exceed it" }
        var victim = 0
        for (i in 1 until kept.size) if (kept[i].third <= kept[victim].third) victim = i
        dropped += kept.removeAt(victim).first
    }
    val used = prefix + kept.sumOf { tokens(it.second) }
    blocks += kept.map { block(it.first, it.second) }
    return linkedMapOf("blocks" to blocks, "tokens" to used, "dropped" to dropped, "breakpoint" to if (stat.isNotEmpty() && prefix >= MIN_CACHEABLE) stat.size - 1 else null)
}

fun chooseModel(workload: Map<String, Any?>, models: List<Map<String, Any?>>): String? {
    val fit = models.filter { (it["tier"] as Int) >= (workload["tier"] as Int) && (it["latency_ms"] as Int) <= (workload["max_latency_ms"] as Int) }
    return fit.sortedWith(compareBy({ it["price_out"] as Int }, { it["name"] as String })).firstOrNull()?.get("name") as String?
}

@Suppress("UNCHECKED_CAST")
fun reusablePrefix(a: Map<String, Any?>, b: Map<String, Any?>): Int? {
    val ia = a["breakpoint"] as Int?
    val ib = b["breakpoint"] as Int?
    if (ia == null || ib == null || ia != ib) return 0
    val blocksA = a["blocks"] as List<Map<String, Any?>>
    val blocksB = b["blocks"] as List<Map<String, Any?>>
    val same = (0..ia).all { blocksA[it] == blocksB[it] }
    return if (same) (0..ia).sumOf { tokens(blocksA[it]["text"] as String) } else 0
}
