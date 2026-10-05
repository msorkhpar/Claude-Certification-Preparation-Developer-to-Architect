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

private fun nameRules(name: String): Set<String> {
    // TODO 1 of 8 (finish this to pass e1): the rule ids a tool name breaks.
    // Receives the tool's name. Returns a set with "bad-name" when the name does not match NAME in full, and "vague-name" when the name
    // in lower case is in VAGUE. Example: nameRules("Helper") -> [vague-name], nameRules("my tool") -> [bad-name]
    val found = sortedSetOf<String>()
    return found
}

private fun descriptionRules(description: String): Set<String> {
    // TODO 2 of 8 (finish this to pass e2): the rule ids a description breaks.
    // Receives the description text. Returns a set with "short-description" when it has fewer than 3 sentences (a `.`, `!` or `?` followed
    // by white space or the end) and "no-boundary" when the lower-cased text has none of "do not use", "not for", "instead of".
    // Example: descriptionRules("Gets stuff.") -> [no-boundary, short-description]
    val found = sortedSetOf<String>()
    return found
}

private fun parameterRules(properties: Map<String, Any?>, required: List<*>): Set<String> {
    // TODO 3 of 8 (finish this to pass e3): the rule ids the parameters break.
    // Receives the schema's `properties` (name -> spec map) and the `required` list. Returns a set with "param-undescribed" when a property
    // has no description or a blank one, and "required-unknown" when `required` names something that is not a property.
    // Example: a property q without description and required ["limit"] -> [param-undescribed, required-unknown]
    val found = sortedSetOf<String>()
    return found
}

private fun listAndHintRules(name: String, properties: Map<String, Any?>, hints: Map<String, Any?>): Set<String> {
    // TODO 4 of 8 (finish this to pass e4): the rule ids a list tool and its annotations break.
    // Receives the name, the `properties` and the tool's `annotations` map. Returns a set with "list-unbounded" when the name starts with a
    // READ_PREFIXES entry and `properties` lacks "limit" or "cursor", and "hint-contradicts-name" when readOnlyHint is true and the name
    // starts with a WRITE_PREFIXES entry, or destructiveHint is false and it starts with a DELETE_PREFIXES entry.
    // Example: listAndHintRules("list_users", mapOf("limit" to emptyMap<String, Any?>()), emptyMap()) -> [list-unbounded]
    val found = sortedSetOf<String>()
    return found
}

fun lintTool(tool: Map<String, Any?>): List<String>? {
    val found = sortedSetOf<String>()
    val name = str(tool["name"])
    val description = str(tool["description"])
    val schema = obj(tool["input_schema"])
    val properties = obj(schema["properties"])
    val required = (schema["required"] as? List<*>) ?: emptyList<Any?>()
    val specs = properties.values.map { obj(it) }
    found.addAll(nameRules(name))
    found.addAll(descriptionRules(description))
    if (!description.lowercase().contains("use when")) found.add("no-use-when")
    found.addAll(parameterRules(properties, required))
    if (specs.any { str(it["type"]) == "string" && !it.containsKey("enum") && Regex("one of|either").containsMatchIn(str(it["description"]).lowercase()) }) found.add("open-set")
    if (properties.entries.any { (key, spec) -> Regex("reasoning|thinking").containsMatchIn((key + " " + str(obj(spec)["description"])).lowercase()) }) found.add("reasoning-param")
    if ((tool["input_examples"] as? List<*>)?.any { !exampleOk(it, properties, required) } == true) found.add("bad-example")
    found.addAll(listAndHintRules(name, properties, obj(tool["annotations"])))
    return found.toList()
}

private fun words(text: Any?): Set<String> = Regex("[a-z]{3,}").findAll(str(text).lowercase()).map { it.value }.toSet()

private fun similar(wa: Set<String>, wb: Set<String>): Boolean {
    // TODO 5 of 8 (finish this to pass e5): do two descriptions overlap?
    // Receives two sets of words. Returns true when the share of common words over all the words is at least OVERLAP (0.6), and false when
    // both sets are empty. Example: similar(setOf("a", "b", "c"), setOf("a", "b", "c", "d")) -> true (3 of 4),
    // similar(setOf("a", "b"), setOf("c", "d")) -> false
    return false
}

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
            if (str(a["name"]) != str(b["name"]) && similar(wa, wb)) {
                found.add(listOf(str(a["name"]), "overlap:" + str(b["name"])))
                found.add(listOf(str(b["name"]), "overlap:" + str(a["name"])))
            }
        }
    }
    if (tools.size > maxTools) found.add(listOf("*", "too-many-tools"))
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

private fun checkLimit(limit: Int): Int {
    // TODO 6 of 8 (finish this to pass e6): validate and clamp a page limit.
    // Receives the requested limit. Throws IllegalArgumentException unless it is at least 1; otherwise returns it cut to MAX_LIMIT.
    // Example: checkLimit(500) -> 50, checkLimit(0) throws IllegalArgumentException
    return limit
}

private fun overCap(page: List<String>, used: Int, item: String, maxChars: Int): Boolean {
    // TODO 7 of 8 (finish this to pass e7): would this item push the page over the size cap?
    // Receives the items already in the page, their total length `used`, the next item and `maxChars`. Returns true when the page is not
    // empty and adding the item would make the total longer than maxChars; the first item is always taken, however long.
    // Example: overCap(listOf("aaaaa"), 5, "bbbbbb", 10) -> true, overCap(emptyList(), 0, "b".repeat(99), 10) -> false
    return false
}

fun pageResults(items: List<String>, cursor: String? = null, limit: Int = 10, maxChars: Int = 2000): Map<String, Any?>? {
    val size = checkLimit(limit)
    val offset = if (cursor == null) 0 else decode(cursor, items.size)
    val page = mutableListOf<String>()
    var used = 0
    for (item in items.subList(offset, minOf(items.size, offset + size))) {
        if (overCap(page, used, item, maxChars)) break
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
    // TODO 8 of 8 (finish this to pass e8): the four annotation hints a client acts on.
    // `hints` holds the defaults. When the server is trusted, the tool's own Boolean value in its "annotations" map replaces the default for
    // each of the four keys (values that are not Booleans are ignored); an untrusted server keeps the defaults.
    // Example: a tool with readOnlyHint true -> readOnlyHint stays false when untrusted, becomes true when trusted.
    return hints
}

fun parallelSafe(tools: List<Map<String, Any?>>, trustedServers: Set<String>): List<String>? =
    tools.filter { effectiveHints(it, str(it["server"]) in trustedServers)!!["readOnlyHint"] == true }.map { str(it["name"]) }
