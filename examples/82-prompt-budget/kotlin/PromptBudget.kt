/**
 * A prompt assembled from modules in cache-friendly order, with a token budget and the cache breakpoint.
 *
 * The Claude documentation on prompt caching (read on 2026-10-04) says cache prefixes are created "in the following order: tools, system,
 * then messages" and that a prompt shorter than the model's minimum "cannot be cached, even if marked with cache_control" (512 tokens for
 * Claude Sonnet 5.5). The prompting guide says to put long documents "near the top of your prompt, above your query". This file orders
 * the modules of a request that way, estimates tokens as one per four characters (a rough rule, not the model's tokenizer), marks the
 * breakpoint after the last static module and shows which edits keep the cached prefix and which break it. No model is called.
 */
data class Module(val name: String, val isStatic: Boolean, val text: String)

/** The blocks in order, the estimated tokens, the tokens of the static prefix and the index of the breakpoint (null when there is none). */
data class Prompt(val blocks: List<Module>, val tokens: Int, val prefixTokens: Int, val breakpoint: Int?)

const val MIN_CACHEABLE = 512 // tokens, Claude Sonnet 5.5

val POLICY = "Refunds above 200 are approved by a supervisor. Gift cards are never refunded in cash. ".repeat(26)
val MODULES = listOf(
    Module("role", true, "You are the support assistant of Northwind Outfitters. Answer from the policy only."),
    Module("policy", true, POLICY),
    Module("customer", false, "Customer: {customer}. Tier: {tier}."),
    Module("question", false, "Question: {question}"),
)

/** The ceiling of characters over four. */
fun tokens(text: String): Int = (text.length + 3) / 4

private fun fill(text: String, variables: Map<String, String>): String = variables.entries.fold(text) { acc, (k, v) -> acc.replace("{$k}", v) }

/** Static modules first, in the order given, then the dynamic ones with their variables filled in. */
fun assemble(modules: List<Module>, variables: Map<String, String>): Prompt {
    val blocks = modules.filter { it.isStatic } + modules.filter { !it.isStatic }.map { it.copy(text = fill(it.text, variables)) }
    val prefix = blocks.filter { it.isStatic }.sumOf { tokens(it.text) }
    val lastStatic = blocks.indexOfLast { it.isStatic }
    return Prompt(blocks, blocks.sumOf { tokens(it.text) }, prefix, if (lastStatic >= 0 && prefix >= MIN_CACHEABLE) lastStatic else null)
}

fun cachedPrefix(prompt: Prompt): String = prompt.breakpoint?.let { prompt.blocks.take(it + 1).joinToString("") { b -> b.text } } ?: ""

private fun py(value: Boolean) = if (value) "True" else "False"

fun main() {
    val ana = mapOf("customer" to "Ana", "tier" to "gold", "question" to "Can I return a gift card?")
    val first = assemble(MODULES, ana)
    println("order: " + first.blocks.joinToString(" > ") { it.name })
    println("tokens: ${first.tokens} in all, ${first.prefixTokens} in the static prefix, minimum $MIN_CACHEABLE")
    println("breakpoint after: " + first.blocks[first.breakpoint!!].name)
    val second = assemble(MODULES, mapOf("customer" to "Ben", "tier" to "basic", "question" to "Where is my parcel?"))
    println("next request, other customer: prefix identical: " + py(cachedPrefix(second) == cachedPrefix(first)))
    val edited = MODULES.map { if (it.name == "policy") it.copy(text = it.text.replace("200", "300")) else it }
    println("after a policy edit: prefix identical: " + py(cachedPrefix(assemble(edited, ana)) == cachedPrefix(first)))
    val short = assemble(MODULES.filter { it.name != "policy" }, mapOf("customer" to "Ana", "tier" to "gold", "question" to "Hi"))
    println("without the policy: breakpoint ${short.breakpoint ?: "None"} because ${short.prefixTokens} tokens is under $MIN_CACHEABLE")
}
