import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.nio.file.Files
import java.nio.file.Path

/**
 * Check that the pieces of a developer-productivity setup agree with each other: the servers of the project file, the tools of each subagent, the permission rules and the credentials.
 *
 * The projects are two small folders of plain files, project-before and project-after, beside this example. The checks are this course's own checklist, built on the
 * documented behaviour (checked 2026-10-04): the project's .mcp.json holds the servers and expands ${VAR} and ${VAR:-default} from the environment; an MCP tool is named
 * mcp__<server>__<tool> in permission rules and in a subagent's tools field; a subagent that omits tools inherits every tool available to subagents; project subagents live in
 * .claude/agents/. Nothing here starts Claude Code or an MCP server.
 */
val HERE: Path = Path.of("..").toAbsolutePath().normalize()
private val SECRET_KEY = Regex("token|key|secret|authorization", RegexOption.IGNORE_CASE)
private val JSON = ObjectMapper()

class Project(val servers: JsonNode, val agents: Map<String, List<String>?>, val perms: JsonNode)

/** The tools line of each subagent, or null when it has none (the subagent then inherits every tool). */
fun agents(root: Path): Map<String, List<String>?> {
    val found = linkedMapOf<String, List<String>?>()
    for (p in Files.list(root.resolve(".claude/agents")).use { s -> s.filter { it.toString().endsWith(".md") }.sorted().toList() }) {
        val head = Regex("^---\\n(.*?)\\n---\\n", RegexOption.DOT_MATCHES_ALL).find(Files.readString(p))!!.groupValues[1]
        val tools = Regex("^tools:\\s*(.*)$", RegexOption.MULTILINE).find(head)
        found[Regex("^name:\\s*(\\S+)", RegexOption.MULTILINE).find(head)!!.groupValues[1]] = tools?.groupValues?.get(1)?.split(",")?.map { it.trim() }
    }
    return found
}

fun load(root: Path) = Project(JSON.readTree(Files.readString(root.resolve(".mcp.json"))).get("mcpServers"), agents(root),
    JSON.readTree(Files.readString(root.resolve(".claude/settings.json"))).path("permissions"))

fun literalSecrets(servers: JsonNode): List<String> {
    val out = mutableListOf<String>()
    for ((name, server) in servers.fields()) {
        for (field in listOf("headers", "env")) {
            for ((key, value) in server.path(field).fields()) if (SECRET_KEY.containsMatchIn(key) && "\${" !in value.asText()) out += "$name $field.$key"
        }
    }
    return out
}

private fun rules(perms: JsonNode, kind: String): List<String> = perms.path(kind).map { it.asText() }

fun audit(root: Path): List<String> {
    val p = load(root)
    val found = literalSecrets(p.servers).map { "literal-secret: $it" }.toMutableList()
    val refs = mutableListOf<Pair<String, String>>()
    for ((agent, tools) in p.agents) for (t in tools ?: listOf()) if (t.startsWith("mcp__")) refs += agent to t
    for (kind in listOf("allow", "deny")) for (r in rules(p.perms, kind)) if (r.startsWith("mcp__")) refs += "settings" to r
    for ((who, ref) in refs) {
        val server = ref.split("__")[1]
        if (!p.servers.has(server)) found += "unknown-server: $server ($who)"
    }
    for ((name, tools) in p.agents) {
        if (tools == null) found += "agent-inherits-all: $name" else if ("Bash" in tools) found += "agent-bare-bash: $name"
    }
    if ("Read(./.env)" !in rules(p.perms, "deny")) found += "env-readable"
    if (rules(p.perms, "allow").any { it == "Edit" || it == "Write" }) found += "bare-write-allowed"
    return found
}

fun main() {
    for (name in listOf("project-before", "project-after")) {
        val p = load(HERE.resolve(name))
        val rules = rules(p.perms, "allow").size + rules(p.perms, "deny").size
        println("$name: ${p.servers.size()} servers, ${p.agents.size} agents, $rules permission rules")
        val found = audit(HERE.resolve(name))
        for (finding in found) println("  finding: $finding")
        if (found.isEmpty()) {
            println("  no findings")
            for ((agent, tools) in p.agents) println("  $agent: ${tools!!.joinToString(", ")}")
        }
    }
}
