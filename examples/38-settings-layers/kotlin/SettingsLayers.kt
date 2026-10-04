import com.fasterxml.jackson.databind.ObjectMapper

/**
 * Claude Code settings layers, permission rules and memory files, resolved offline.
 *
 * Three small functions that follow what the Claude Code documentation says (read on 2026-10-03): settings merge from five levels with
 * the highest level winning a scalar key and lists combining; permission rules are checked deny, then ask, then allow, with the first
 * match deciding; and CLAUDE.md files are concatenated from the broadest scope to the most specific, with @imports expanded. It is a
 * teaching model of the documented behaviour, not the product's code: Read and Edit patterns use a reduced form of the gitignore rules.
 * The settings files are JSON, read with Jackson into maps.
 */
typealias Settings = Map<String, Any?>

val LEVELS = listOf("managed", "command line", "local", "project", "user") // highest precedence first
val REPO_LEVELS = listOf("project", "local") // files that live in the repository

/** A JSON object (a settings file) as a map. */
@Suppress("UNCHECKED_CAST")
fun json(text: String): Settings = ObjectMapper().readValue(text, LinkedHashMap::class.java) as Settings

@Suppress("UNCHECKED_CAST")
private fun map(o: Any?): Settings = o as Settings

@Suppress("UNCHECKED_CAST")
private fun list(o: Any?): List<Any?> = o as List<Any?>

fun merge(low: Settings, high: Settings): Settings {
    val out = LinkedHashMap(low)
    for ((key, value) in high) {
        val current = out[key]
        out[key] = when {
            value is Map<*, *> && current is Map<*, *> -> merge(map(current), map(value))
            value is List<*> && current is List<*> -> list(current) + list(value).filter { it !in list(current) }
            else -> value
        }
    }
    return out
}

/** Merge the five levels. A repository file cannot set defaultMode auto or bypassPermissions, and its allow rules wait for trust. */
fun effectiveSettings(layers: Map<String, Settings>, trusted: Boolean = true): Settings {
    var result: Settings = emptyMap()
    for (level in LEVELS.reversed()) {
        val part = LinkedHashMap<String, Any?>()
        for ((k, v) in layers[level] ?: emptyMap()) part[k] = if (v is Map<*, *>) LinkedHashMap(map(v)) else v
        @Suppress("UNCHECKED_CAST") val perms = part["permissions"] as MutableMap<String, Any?>?
        if (level in REPO_LEVELS && !perms.isNullOrEmpty()) {
            if (perms["defaultMode"] in listOf("auto", "bypassPermissions")) perms.remove("defaultMode")
            if (!trusted) perms.remove("allow")
        }
        result = merge(result, part)
    }
    return result
}

private fun bashRegex(ruleText: String): Regex {
    val rule = if (ruleText.endsWith(":*")) ruleText.dropLast(2) + " *" else ruleText
    if (rule.endsWith(" *") && "*" !in rule.dropLast(2)) return Regex(Regex.escape(rule.dropLast(2)) + "(?: .*)?", RegexOption.DOT_MATCHES_ALL)
    return Regex(rule.split("*").joinToString(".*") { Regex.escape(it) }, RegexOption.DOT_MATCHES_ALL)
}

private fun glob(p: String): String {
    val out = StringBuilder()
    var i = 0
    while (i < p.length) {
        when {
            p.startsWith("**/", i) -> { out.append("(?:.*/)?"); i += 3 }
            p.startsWith("**", i) -> { out.append(".*"); i += 2 }
            p[i] == '*' -> { out.append("[^/]*"); i += 1 }
            else -> { out.append(Regex.escape(p[i].toString())); i += 1 }
        }
    }
    return out.toString()
}

private fun pathMatches(patternText: String, pathText: String, kind: String): Boolean {
    val pattern = patternText.removePrefix("./")
    val path = pathText.removePrefix("./")
    if (pattern.startsWith("/")) return Regex(glob(pattern.substring(1))).matches(path) // "//x" and "/x" both drop one slash
    if ("/" !in pattern) return Regex("(?:.*/)?" + glob(pattern)).matches(path)
    val first = pattern.split("/")[0]
    val deep = kind != "allow" && "*" !in first && pattern.count { it == '/' } >= 1 && !pattern.startsWith("**")
    return Regex((if (deep) "(?:.*/)?" else "") + glob(pattern)).matches(path)
}

private val RULE = Regex("""(\w+)(?:\((.*)\))?""", RegexOption.DOT_MATCHES_ALL)

private fun ruleMatches(rule: String, tool: String, arg: String, kind: String): Boolean {
    val m = RULE.matchEntire(rule)
    if (m == null || m.groupValues[1] != tool) return false
    val inner = m.groups[2]?.value ?: return true
    return if (tool == "Bash") bashRegex(inner).matches(arg) else pathMatches(inner, arg, kind)
}

/**
 * allow, ask or deny. Deny first, then ask, then allow; a Bash call is split at && || ; | & and newlines and every part must pass.
 * In acceptEdits mode an edit that no rule decided is accepted instead of asked.
 */
fun decide(settings: Settings, tool: String, arg: String = "", mode: String = "default"): String {
    val perms = settings["permissions"]?.let { map(it) } ?: emptyMap()
    val rules = listOf("deny", "ask", "allow").associateWith { kind -> list(perms[kind] ?: emptyList<Any?>()).map { it as String } }
    val parts = if (tool == "Bash") arg.split(Regex("&&|\\|\\||;|\\||&|\n")).map { it.trim() }.filter { it.isNotEmpty() } else listOf(arg)
    for (kind in listOf("deny", "ask")) {
        if (rules.getValue(kind).any { r -> parts.any { p -> ruleMatches(r, tool, p, kind) } }) return kind
    }
    if (tool in listOf("Edit", "Write") && rules.getValue("deny").filter { it.startsWith("Read(") }.any { ruleMatches("Edit(" + it.substring(5), "Edit", arg, "deny") }) {
        return "deny" // a Read deny rule also blocks edits and writes on the same path
    }
    if (parts.all { p -> rules.getValue("allow").any { r -> ruleMatches(r, tool, p, "allow") } }) return "allow"
    if (tool in listOf("Read", "Grep", "Glob")) return "allow" // read-only tools inside the working directory need no approval
    if (mode == "acceptEdits" && tool in listOf("Edit", "Write")) return "allow"
    return "ask" // nothing matched: Manual mode asks
}

/**
 * The memory files in the order they reach the context: managed, user, then each directory from the root down to cwd, where
 * CLAUDE.md comes before CLAUDE.local.md. files maps a path to its text; @path lines import files (relative to the importing file,
 * at most four hops deep) and a path inside backticks is not an import.
 */
fun loadMemory(files: Map<String, String>, cwd: String, managed: String? = null, user: String? = null): List<String> {
    val order = listOfNotNull(managed, user).filter { it in files }.toMutableList()
    val parts = cwd.trim('/').split("/")
    for (depth in 0..parts.size) {
        val base = ("/" + parts.take(depth).joinToString("/")).trimEnd('/')
        order += listOf("$base/CLAUDE.md", "$base/CLAUDE.local.md").filter { it in files }
    }
    val loaded = mutableListOf<String>()
    fun add(path: String, hops: Int) {
        loaded += path
        if (hops == 4) return
        for (m in Regex("""(?<![`\w])@([\w./-]+)""").findAll(files.getValue(path).replace(Regex("`[^`]*`"), ""))) {
            val token = m.groupValues[1]
            val target = if (token.startsWith("/")) token else join(path, token)
            if (target in files) add(target, hops + 1)
        }
    }
    for (path in order) add(path, 0)
    return loaded
}

private fun join(path: String, rel: String): String {
    val stack = path.split("/").dropLast(1).toMutableList()
    for (part in rel.split("/")) {
        if (part == "..") stack.removeLast() else if (part != ".") stack += part
    }
    return stack.joinToString("/")
}

/** Python's repr of strings and lists of strings, so every language of the course prints the same text. */
fun py(v: Any?): String {
    if (v is List<*>) return v.joinToString(", ", "[", "]") { py(it) }
    val s = v.toString()
    val q = if ("'" in s && "\"" !in s) "\"" else "'"
    return q + s.replace("\\", "\\\\").replace("\n", "\\n").replace(q, "\\" + q) + q
}

fun main() {
    val layers = linkedMapOf(
        "managed" to json("""{"permissions": {"deny": ["Bash(curl *)"]}}"""),
        "user" to json("""{"model": "sonnet", "permissions": {"allow": ["Bash(git status *)"]}}"""),
        "project" to json(
            """{"model": "opus", "permissions": {"defaultMode": "bypassPermissions", "allow": ["Bash(npm run *)", "Bash(curl *)"],
                                                 "ask": ["Bash(git push *)"], "deny": ["Read(./.env)"]}}""",
        ),
        "local" to json("""{"permissions": {"allow": ["Bash(git push *)"]}}"""),
    )
    val s = effectiveSettings(layers)
    println("model: ${s["model"]} | defaultMode: ${map(s["permissions"]).getOrDefault("defaultMode", "(not set)")}")
    println("allow rules: ${py(map(s["permissions"])["allow"])}")
    val calls = listOf("Bash" to "npm run build", "Bash" to "npm run build && git push origin main", "Bash" to "curl https://example.com",
        "Read" to "./.env", "Edit" to ".env", "Bash" to "git status", "Bash" to "rm -rf build")
    for ((tool, arg) in calls) println("$tool($arg) -> ${decide(s, tool, arg)}")
    println("project not trusted yet: npm run build -> ${decide(effectiveSettings(layers, trusted = false), "Bash", "npm run build")}")
    val files = linkedMapOf(
        "/etc/claude-code/CLAUDE.md" to "managed", "/home/dev/.claude/CLAUDE.md" to "user", "/repo/CLAUDE.md" to "See @docs/git.md and `@README` here",
        "/repo/docs/git.md" to "git rules", "/repo/svc/CLAUDE.md" to "service", "/repo/svc/CLAUDE.local.md" to "mine", "/repo/other/CLAUDE.md" to "other",
    )
    println("memory order for /repo/svc: ${py(loadMemory(files, "/repo/svc", "/etc/claude-code/CLAUDE.md", "/home/dev/.claude/CLAUDE.md"))}")
}
