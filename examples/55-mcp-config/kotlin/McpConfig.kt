import com.fasterxml.jackson.databind.ObjectMapper
import harness.Show.py

private val log = System.getLogger("mcp_config")

/**
 * MCP server configuration in Claude Code, resolved offline: scopes, environment expansion and a lint of a shared `.mcp.json`.
 *
 * A teaching model of what the Claude Code documentation says (page "Connect Claude Code to tools via MCP", read on 2026-10-03): three scopes with
 * the order local, project, user, where the whole entry of the highest scope is used and fields are not merged; `${VAR}` and `${VAR:-default}`
 * expanded in command, args, env, url and headers; an unset variable with no default keeps its text; and, toward a remote server, credential
 * variables read as empty. The set of credential names here is the documentation's examples, not its full list. Not the product's code.
 * The configuration is JSON, read with Jackson.
 */
val SCOPES = listOf("local", "project", "user") // highest precedence first
val COVERED = setOf("ANTHROPIC_API_KEY", "ANTHROPIC_AUTH_TOKEN", "AWS_BEARER_TOKEN_BEDROCK", "HTTPS_PROXY", "NPM_TOKEN")
val REMOTE = setOf("http", "sse", "ws")
val VAR = Regex("""\$\{([A-Za-z_][A-Za-z0-9_]*)(?::-([^}]*))?\}""")
val SECRET_KEY = Regex("token|key|secret|authorization|password", RegexOption.IGNORE_CASE)
const val DESCRIPTION_LIMIT = 2048 // characters kept of a tool description and of a server's instructions
private val JSON = ObjectMapper()

typealias Entry = Map<String, Any?>

/** The text after expansion and the names that were unset and had no default. */
data class Expanded(val text: String, val missing: List<String>)

/** A server entry after expansion, and the warnings it raised. */
data class ExpandedServer(val entry: Entry, val warnings: List<String>)

/** A server's winning scope and entry. */
data class Source(val scope: String, val entry: Entry)

/** The servers in use, and the warnings about conflicts. */
data class Resolved(val servers: Map<String, Source>, val warnings: List<String>)

@Suppress("UNCHECKED_CAST")
fun parse(json: String): Map<String, Any?> = JSON.readValue(json, LinkedHashMap::class.java) as Map<String, Any?>

/** Expand ${VAR} and ${VAR:-default}. Returns the text and the names that were unset and had no default. */
fun expand(text: String, env: Map<String, String>, remote: Boolean = false): Expanded {
    val missing = mutableListOf<String>()
    val out = VAR.replace(text) { m ->
        val name = m.groupValues[1]
        val default = m.groups[2]?.value
        when {
            remote && name in COVERED -> "" // never sent toward a remote server, whether or not it is set, and a default is ignored
            name in env -> env.getValue(name)
            default != null -> default
            else -> {
                missing += name
                m.value // the unexpanded text is used as written
            }
        }
    }
    return Expanded(out, missing)
}

/** Expand the five fields where expansion applies. Returns the entry and the warnings. */
fun expandServer(entry: Entry, env: Map<String, String>): ExpandedServer {
    val remote = entry["type"] in REMOTE
    val warnings = mutableListOf<String>()
    val out = LinkedHashMap(entry)
    fun one(value: Any?): String {
        val e = expand(value as String, env, remote)
        e.missing.forEach { warnings += "$it is not set" }
        return e.text
    }
    for (field in listOf("command", "url")) if (field in out) out[field] = one(out[field])
    if ("args" in out) out["args"] = (entry["args"] as List<*>).map { one(it) }
    for (field in listOf("env", "headers")) if (field in out) out[field] = (entry[field] as Map<*, *>).entries.associate { (k, v) -> k as String to one(v) }
    return ExpandedServer(out, warnings)
}

/** scopes maps a scope name to {server name: entry}. A name defined in several scopes is used once, from the highest scope, whole. */
fun resolveServers(scopes: Map<String, Map<String, Entry>>): Resolved {
    val servers = linkedMapOf<String, Source>()
    val warnings = mutableListOf<String>()
    for (scope in SCOPES) {
        for ((name, entry) in scopes[scope] ?: emptyMap()) {
            val seen = servers[name]
            if (seen == null) servers[name] = Source(scope, entry)
            else if (seen.entry != entry) warnings += "$name is defined in ${seen.scope} and $scope with different settings: ${seen.scope} wins"
        }
    }
    return Resolved(servers, warnings)
}

/** Findings for a shared `.mcp.json`: a server's shape and any credential written out instead of referenced. */
fun lint(config: Entry): List<String> {
    val findings = mutableListOf<String>()
    for ((name, e) in (config["mcpServers"] as Map<*, *>? ?: emptyMap<String, Any?>())) {
        val entry = e as Map<*, *>
        val kind = entry["type"] as String? ?: "stdio"
        if (kind in REMOTE && "url" !in entry) findings += "$name: a $kind server needs a url"
        if (kind == "stdio" && "command" !in entry) findings += "$name: a stdio server needs a command"
        for (field in listOf("env", "headers")) {
            for ((key, value) in (entry[field] as Map<*, *>? ?: emptyMap<String, Any?>())) {
                if (SECRET_KEY.containsMatchIn(key as String) && "\${" !in (value as String)) findings += "$name: $field.$key holds a literal value, reference an environment variable"
            }
        }
    }
    return findings
}

/** Python's fnmatch.fnmatchcase: `*` any run, `?` one character, `[...]` a set. */
fun fnmatch(name: String, pattern: String): Boolean {
    val re = StringBuilder()
    var i = 0
    while (i < pattern.length) {
        val c = pattern[i]
        when {
            c == '*' -> re.append(".*")
            c == '?' -> re.append('.')
            c == '[' && pattern.indexOf(']', i + 2) >= 0 -> {
                val j = pattern.indexOf(']', i + 2)
                val set = pattern.substring(i + 1, j)
                re.append('[').append(if (set.startsWith("!")) "^" + set.substring(1) else set).append(']')
                i = j
            }
            else -> re.append(Regex.escape(c.toString()))
        }
        i++
    }
    return Regex(re.toString(), RegexOption.DOT_MATCHES_ALL).matches(name)
}

/**
 * allow, ask or deny for an MCP tool named mcp__<server>__<tool>. Deny rules may use globs; an allow rule counts only when the
 * server part is written out and glob-free (mcp__docs__* is honoured, mcp__* and * are ignored). Deny wins, then allow, else ask.
 */
fun mcpDecision(settings: Entry, tool: String): String {
    val perms = settings["permissions"] as Map<*, *>? ?: emptyMap<String, Any?>()
    if ((perms["deny"] as List<*>? ?: emptyList<String>()).any { fnmatch(tool, it as String) }) return "deny"
    for (rule in (perms["allow"] as List<*>? ?: emptyList<String>())) {
        val parts = (rule as String).split("__")
        if (parts.size >= 3 && parts[0] == "mcp" && parts[1].isNotEmpty() && "*" !in parts[1] && fnmatch(tool, rule)) return "allow"
    }
    return "ask"
}

fun truncate(text: String, limit: Int = DESCRIPTION_LIMIT): String = text.take(limit)

fun show(value: Any?): String = JSON.writeValueAsString(value)

@Suppress("UNCHECKED_CAST")
fun main() {
    val env = mapOf("GITHUB_TOKEN" to "demo-gh", "NPM_TOKEN" to "demo-npm")
    val shared = parse(
        """
        {"mcpServers": {
          "github": {"type": "http", "url": "${'$'}{GITHUB_MCP_URL:-https://github-mcp.example.com/mcp}", "headers": {"Authorization": "Bearer ${'$'}{GITHUB_TOKEN}"}},
          "registry": {"type": "http", "url": "https://registry-mcp.example.com/mcp", "headers": {"Authorization": "Bearer ${'$'}{NPM_TOKEN}"}},
          "docs": {"command": "python3", "args": ["${'$'}{CLAUDE_PROJECT_DIR:-.}/tools/docs_server.py", "${'$'}{DOCS_INDEX}"], "env": {"DOCS_API_KEY": "${'$'}{DOCS_API_KEY}"}}
        }}
        """,
    )
    val sharedServers = shared["mcpServers"] as Map<String, Entry>
    println("expanded entries:")
    for ((name, entry) in sharedServers) {
        val out = expandServer(entry, env)
        println("  $name: ${show(out.entry)}")
        out.warnings.forEach { println("    warning: $it") }
    }
    val user = parse("""{"github": {"type": "http", "url": "https://other.example.com/mcp"}, "scratch": {"command": "python3", "args": ["scratch.py"]}}""") as Map<String, Entry>
    val resolved = resolveServers(mapOf("project" to sharedServers, "user" to user))
    println("resolved servers: " + resolved.servers.entries.joinToString(", ") { (n, s) -> "$n from ${s.scope}" })
    resolved.warnings.forEach { println("  warning: $it") }
    val bad = parse("""{"mcpServers": {"api": {"type": "http", "headers": {"X-Api-Key": "abc123"}}, "tool": {"args": ["x"]}}}""")
    println("lint of a shared file with literal secrets:")
    lint(bad).forEach { println("  $it") }
    val clean = lint(shared)
    println("lint of the file above: ${if (clean.isEmpty()) "no findings" else py(clean)}")
    val settings = parse("""{"permissions": {"allow": ["mcp__docs__*", "mcp__*"], "deny": ["mcp__github__delete_*"]}}""")
    for (tool in listOf("mcp__docs__search_docs", "mcp__github__list_prs", "mcp__github__delete_repository")) println("$tool -> ${mcpDecision(settings, tool)}")
    println("a description of 3000 characters keeps ${truncate("x".repeat(3000)).length} of them")
}
