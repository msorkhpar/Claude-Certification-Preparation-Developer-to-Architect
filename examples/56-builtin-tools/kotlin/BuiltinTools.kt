import harness.Show.py

private val log = System.getLogger("builtin_tools")

/**
 * The built-in file tools of Claude Code, modelled offline: Edit's exact match, the way out when it cannot apply, which search tools exist
 * on which platform, and which permission rule covers which tool.
 *
 * A teaching model of the "Tools reference" page of the Claude Code documentation (read on 2026-10-03), not the product's code. It covers six tools:
 * Read, Write, Edit, Bash, Grep and Glob.
 */
val SEARCH_TOOLS = listOf("Grep", "Glob")
val BASE_TOOLS = listOf("Read", "Write", "Edit", "Bash")
val RULE_COVERS = mapOf("Read" to listOf("Read", "Grep", "Glob"), "Edit" to listOf("Edit", "Write"), "Bash" to listOf("Bash")) // a Write(path) rule is never matched

/** The result of Edit: whether it applied, the new text and the number replaced, or the error. */
data class EditResult(val ok: Boolean, val text: String? = null, val replaced: Int? = null, val error: String? = null)

/** What to do for a change: the action and the string to edit with (null when none). */
data class Plan(val action: String, val anchor: String?) {
    override fun toString() = "(${py(action)}, ${py(anchor)})"
}

private fun count(text: String, part: String): Int {
    var n = 0
    var i = text.indexOf(part)
    while (i >= 0) {
        n++
        i = text.indexOf(part, i + part.length)
    }
    return n
}

/** Edit is an exact string replacement: no regex, no fuzzy match. old must be present, and appear once unless replaceAll is set. */
fun edit(text: String, old: String, replacement: String, replaceAll: Boolean = false): EditResult {
    val count = count(text, old)
    if (count == 0) return EditResult(false, error = "old_string not found")
    if (count > 1 && !replaceAll) return EditResult(false, error = "old_string appears $count times")
    val changed = if (replaceAll) text.replace(old, replacement) else text.replaceFirst(old, replacement)
    return EditResult(true, text = changed, replaced = if (replaceAll) count else 1)
}

/**
 * What to do for a change to `old`: Edit as it is, Edit with a longer unique string that holds it, replace_all for every occurrence,
 * and only when no unique anchor exists, read the file and write it back whole.
 */
fun planEdit(text: String, old: String, every: Boolean = false, anchors: List<String> = emptyList()): Plan {
    val count = count(text, old)
    if (count == 0) return Plan("read_again", null)
    if (count == 1) return Plan("edit", old)
    if (every) return Plan("replace_all", old)
    for (anchor in anchors) if (old in anchor && count(text, anchor) == 1) return Plan("edit", anchor)
    return Plan("read_write", null)
}

/**
 * The six tools a session has. Grep and Glob are in the default set on Windows only; elsewhere they return when named in `tools`
 * or `allowedTools` (naming either in allowedTools restores both), or when Bash is removed.
 */
fun toolSet(platform: String, tools: List<String>? = null, allowedTools: List<String> = emptyList(), disallowedTools: List<String> = emptyList()): List<String> {
    val have = if (tools != null) tools.filter { it in BASE_TOOLS + SEARCH_TOOLS } else {
        BASE_TOOLS + if (platform == "windows" || allowedTools.any { it in SEARCH_TOOLS } || "Bash" in disallowedTools) SEARCH_TOOLS else emptyList()
    }
    return have.filter { it !in disallowedTools }
}

/** The tools that a permission rule such as Read(secrets/...) applies to. */
fun coveredBy(rule: String): List<String> = RULE_COVERS[rule.substringBefore("(")] ?: emptyList()

/** The tool name that a permission rule is written under: Read(...) covers Read, Grep and Glob; Edit(...) covers Edit and Write. */
fun ruleTool(tool: String): String = RULE_COVERS.entries.firstOrNull { tool in it.value }?.key ?: tool

fun main() {
    val text = "def a():\n    return 1\n\ndef b():\n    return 1\n"
    for ((label, old, every) in listOf(Triple("unique", "def a():", false), Triple("twice", "    return 1", false), Triple("twice, every", "    return 1", true), Triple("absent", "def c():", false))) {
        val r = edit(text, old, "X", every)
        println("edit $label: ${if (!r.ok) r.error else "replaced ${r.replaced}"}")
    }
    val anchors = listOf("def b():\n    return 1")
    println("plan, unique: ${planEdit(text, "def a():")}")
    println("plan, twice with an anchor: ${planEdit(text, "    return 1", anchors = anchors)}")
    println("plan, twice, every one: ${planEdit(text, "    return 1", every = true)}")
    println("plan, twice, no unique anchor: ${planEdit(text, "    return 1", anchors = listOf("return 1"))}")
    for (platform in listOf("linux", "windows")) println("$platform, default: ${toolSet(platform).joinToString(", ")}")
    println("linux, allowedTools Grep: ${toolSet("linux", allowedTools = listOf("Grep")).joinToString(", ")}")
    println("linux, tools Read Grep Glob: ${toolSet("linux", tools = listOf("Read", "Grep", "Glob")).joinToString(", ")}")
    println("linux, Bash removed: ${toolSet("linux", disallowedTools = listOf("Bash")).joinToString(", ")}")
    println("rules are written under: ${listOf("Grep", "Glob", "Write", "Bash").joinToString(", ") { "$it as ${ruleTool(it)}" }}")
    for (rule in listOf("Read(secrets/**)", "Edit(src/**)", "Write(src/**)", "Bash(git log *)")) println("$rule covers: ${coveredBy(rule).joinToString(", ").ifEmpty { "nothing" }}")
}
