import java.util.Base64

/** Tool interfaces graded on rules: lint a tool and a tool set, page large results, and weigh a tool's annotations. See ../../statement.md. Tools are JSON-like maps. */

private val NAME = Regex("[A-Za-z0-9_-]{1,128}")
private val VAGUE = setOf("tool", "helper", "do", "run", "process", "handle", "data", "util", "utils", "query")
private val READ_PREFIXES = listOf("list_", "search_", "find_")
private val WRITE_PREFIXES = listOf("create_", "update_", "delete_", "remove_", "send_", "write_")
private val DELETE_PREFIXES = listOf("delete_", "remove_")
private const val OVERLAP = 0.6 // token overlap of two descriptions from which the tools count as overlapping
private const val MAX_LIMIT = 50

private fun str(value: Any?): String = value?.toString() ?: ""

@Suppress("UNCHECKED_CAST")
private fun obj(value: Any?): Map<String, Any?> = (value as? Map<String, Any?>) ?: emptyMap()

private fun startsWithAny(text: String, prefixes: List<String>) = prefixes.any { text.startsWith(it) }

private fun valid(value: Any?, schema: Map<String, Any?>): Boolean {
    when (str(schema["type"])) {
        "string" -> if (value !is String) return false
        "integer" -> if (!(value is Int || value is Long)) return false
        "number" -> if (value !is Number) return false
        "boolean" -> if (value !is Boolean) return false
        "array" -> if (value !is List<*>) return false
        "object" -> if (value !is Map<*, *>) return false
    }
    @Suppress("UNCHECKED_CAST")
    val allowed = schema["enum"] as? Collection<Any?>
    return allowed == null || allowed.contains(value)
}

private fun exampleOk(example: Any?, properties: Map<String, Any?>, required: List<*>): Boolean {
    if (example !is Map<*, *>) return false
    val given = example.entries.associate { str(it.key) to it.value }
    if (required.any { str(it) !in given }) return false
    return given.all { (key, value) -> key in properties && valid(value, obj(properties[key])) }
}

fun lintTool(tool: Map<String, Any?>): List<String>? {
    val found = sortedSetOf<String>()
    val name = str(tool["name"])
    val description = str(tool["description"])
    val schema = obj(tool["input_schema"])
    val properties = obj(schema["properties"])
    val required = (schema["required"] as? List<*>) ?: emptyList<Any?>()
    val low = description.lowercase()
    val specs = properties.values.map { obj(it) }
    if (!NAME.matches(name)) found.add("bad-name")
    if (name.lowercase() in VAGUE) found.add("vague-name")
    if (Regex("[.!?](?:\\s|$)").findAll(description).count() < 3) found.add("short-description")
    if (!low.contains("use when")) found.add("no-use-when")
    if (listOf("do not use", "not for", "instead of").none { low.contains(it) }) found.add("no-boundary")
    if (specs.any { str(it["description"]).isBlank() }) found.add("param-undescribed")
    if (required.any { str(it) !in properties }) found.add("required-unknown")
    if (specs.any { str(it["type"]) == "string" && !it.containsKey("enum") && Regex("one of|either").containsMatchIn(str(it["description"]).lowercase()) }) found.add("open-set")
    if (properties.entries.any { (key, spec) -> Regex("reasoning|thinking").containsMatchIn((key + " " + str(obj(spec)["description"])).lowercase()) }) found.add("reasoning-param")
    if ((tool["input_examples"] as? List<*>)?.any { !exampleOk(it, properties, required) } == true) found.add("bad-example")
    if (startsWithAny(name, READ_PREFIXES) && !(properties.containsKey("limit") && properties.containsKey("cursor"))) found.add("list-unbounded")
    val hints = obj(tool["annotations"])
    if ((hints["readOnlyHint"] == true && startsWithAny(name, WRITE_PREFIXES)) || (hints["destructiveHint"] == false && startsWithAny(name, DELETE_PREFIXES))) found.add("hint-contradicts-name")
    return found.toList()
}

private fun words(text: Any?): Set<String> = Regex("[a-z]{3,}").findAll(str(text).lowercase()).map { it.value }.toSet()

fun lintToolSet(tools: List<Map<String, Any?>>, maxTools: Int = 20): List<List<String>>? {
    val found = linkedSetOf<List<String>>()
    for (tool in tools) for (rule in lintTool(tool)!!) found.add(listOf(str(tool["name"]), rule))
    val names = tools.map { str(it["name"]) }
    for (name in names.toSet()) if (names.count { it == name } > 1) found.add(listOf(name, "duplicate-name"))
    for (i in tools.indices) {
        for (j in i + 1 until tools.size) {
            val a = tools[i]
            val b = tools[j]
            val wa = words(a["description"])
            val wb = words(b["description"])
            val union = wa + wb
            if (str(a["name"]) != str(b["name"]) && union.isNotEmpty() && wa.count { it in wb }.toDouble() / union.size >= OVERLAP) {
                found.add(listOf(str(a["name"]), "overlap:" + str(b["name"])))
                found.add(listOf(str(b["name"]), "overlap:" + str(a["name"])))
            }
        }
    }
    if (tools.size >= maxTools) found.add(listOf("*", "too-many-tools"))
    return found.sortedWith(compareBy({ it[0] }, { it[1] }))
}

private fun encode(offset: Int): String = Base64.getEncoder().encodeToString("offset:$offset".toByteArray())

private fun decode(cursor: String, total: Int): Int {
    var offset = -1
    try {
        val text = String(Base64.getDecoder().decode(cursor))
        if (Regex("offset:\\d{1,9}").matches(text)) offset = text.substring(7).toInt()
    } catch (error: IllegalArgumentException) {
        offset = -1
    }
    if (offset < 0 || offset > total) throw IllegalArgumentException("invalid cursor")
    return offset
}

fun pageResults(items: List<String>, cursor: String? = null, limit: Int = 10, maxChars: Int = 2000): Map<String, Any?>? {
    if (limit < 1) throw IllegalArgumentException("limit must be a whole number of at least 1")
    val size = minOf(limit, MAX_LIMIT)
    val offset = if (cursor == null) 0 else decode(cursor, items.size)
    val page = mutableListOf<String>()
    var used = 0
    for (item in items.subList(offset, minOf(items.size, offset + size))) {
        if (page.isNotEmpty() && used + item.length > maxChars) break
        page.add(item)
        used += item.length
    }
    val taken = offset + page.size
    val nextCursor = if (taken < items.size) encode(taken) else null
    val note = if (nextCursor != null) "Showing ${page.size} of ${items.size} results; pass next_cursor to continue, or narrow the query with a filter." else null
    return linkedMapOf("items" to page, "next_cursor" to nextCursor, "truncated" to (page.size < minOf(size, items.size - offset)), "note" to note)
}

fun effectiveHints(tool: Map<String, Any?>, trustedServer: Boolean): Map<String, Boolean>? {
    val hints = linkedMapOf("readOnlyHint" to false, "destructiveHint" to true, "idempotentHint" to false, "openWorldHint" to true)
    if (trustedServer) {
        val own = obj(tool["annotations"])
        for (key in hints.keys.toList()) (own[key] as? Boolean)?.let { hints[key] = it }
    }
    return hints
}

fun parallelSafe(tools: List<Map<String, Any?>>, trustedServers: Set<String>): List<String>? =
    tools.filter { effectiveHints(it, str(it["server"]) in trustedServers)!!["readOnlyHint"] == true }.map { str(it["name"]) }
