/** A prompt plan: modules in cache-friendly order under a token budget, a model for the workload, and what a cache can reuse. See ../../statement.md. Modules, prompts and models are JSON-like maps. */
private val log = System.getLogger("prompt_plan")

const val MIN_CACHEABLE = 512 // tokens: a shorter prefix cannot be cached

private val VARIABLE = Regex("\\{(\\w+)\\}")

/** One token per four characters, rounded up. */
fun tokens(text: String): Int = (text.length + 3) / 4

/** A dynamic module that is still in the prompt. */
data class Kept(val name: String, val text: String, val priority: Int)

private fun block(name: Any?, text: String): Map<String, Any?> = linkedMapOf("name" to name, "text" to text)

/**
 * TODO 1 of 7 (unlocks e2): fill the variables of a dynamic module's text.
 * Receives the text and a map of variables. Returns the text with every `{name}` replaced by that variable's value (extra variables are ignored).
 * Throws `IllegalArgumentException("missing variable: <name>")` when a variable has no value (`VARIABLE` matches a variable).
 * Example: fill("Q: {q}", mapOf("q" to "hello")) -> "Q: hello"
 */
fun fill(text: String, variables: Map<String, String>): String = text

/**
 * TODO 2 of 7 (unlocks e1): refuse a variable in a static module.
 * Receives the list of static modules. Throws `IllegalArgumentException` with a message that says `static` (for example `static module <name> holds a variable, which
 * would break the cache`) when any module's text holds a variable; otherwise returns nothing.
 * Example: a static module with the text "policy for {customer}" -> IllegalArgumentException
 */
fun checkStatic(stat: List<Map<String, Any?>>) {}

/**
 * TODO 3 of 7 (unlocks e3): which dynamic module is dropped first?
 * Receives the list of kept dynamic modules (`Kept(name, text, priority)`). Returns the index of the one with the lowest priority; of two with the same
 * priority, the later one (the higher index).
 * Example: priorities [1, 9, 1] -> 2
 */
fun pickVictim(kept: List<Kept>): Int = 0

/**
 * TODO 4 of 7 (unlocks e3 and e4): drop dynamic modules until the prompt fits the budget.
 * Receives the tokens of the static prefix, the list `kept` of dynamic modules (change it in place) and the budget. While `prefix` plus the tokens of the
 * kept modules exceeds the budget, remove the module `pickVictim` chooses and note its name. Returns the dropped names in the order they were dropped.
 * When nothing is left to drop and the budget is still exceeded, throws `IllegalArgumentException("over budget: ...")`: static modules are never dropped.
 * Example: kept priorities [1, 1] and a budget that fits one of them -> the later name is dropped
 */
fun fitBudget(prefix: Int, kept: MutableList<Kept>, budget: Int): List<String> = emptyList()

/**
 * TODO 5 of 7 (unlocks m1 and e5): where the cache breakpoint goes.
 * Receives the number of static blocks and the tokens of the static prefix. Returns the index of the last static block when there is at least one static
 * block and the prefix has at least `MIN_CACHEABLE` tokens; otherwise `null`.
 * Example: breakpointOf(2, 512) -> 1, breakpointOf(2, 511) -> null, breakpointOf(0, 900) -> null
 */
fun breakpointOf(staticCount: Int, prefix: Int): Int? = null

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

/**
 * TODO 6 of 7 (unlocks e6): choose the model for a workload.
 * Receives the workload (`tier`, `max_latency_ms`) and a list of models (`name`, `tier`, `latency_ms`, `price_out`). Returns the name of the model with the lowest
 * `price_out` among those whose `tier` is at least the workload's and whose `latency_ms` is within the limit; equal prices go to the lower name; `null` when no
 * model fits. Example: two models at the same price named "mid" and "mid2", both fitting -> "mid"
 */
fun chooseModel(workload: Map<String, Any?>, models: List<Map<String, Any?>>): String? = null

/**
 * TODO 7 of 7 (unlocks e7): how many tokens of cached prefix can be reused?
 * Receives two assembled prompts (each a map with `blocks` and `breakpoint`). When both have a breakpoint, the breakpoints are equal and every block up to and
 * including it is identical (name and text) in both, returns the tokens of those blocks; otherwise returns 0.
 * Example: two prompts that differ only in their dynamic blocks -> the tokens of the static prefix; one edited static block -> 0
 */
fun reusablePrefix(a: Map<String, Any?>, b: Map<String, Any?>): Int? = 0
