import com.fasterxml.jackson.databind.ObjectMapper

private val log = System.getLogger("managed_agent")

/**
 * Managed Agents, checked offline: lint the configuration you would send, and price a session against its budget.
 *
 * Claude Managed Agents is a hosted agent harness: you create an agent (model, system prompt, tools), an environment (where the
 * tools run: an Anthropic-managed cloud sandbox or a self-hosted one) and a session, then exchange events. Nothing here calls the
 * API. The rules below are the ones the documentation states for environments, permission policies and session budgets, read on
 * 2026-10-03 (beta header managed-agents-2026-04-01); the prices are the list prices recorded in docs/VERSIONS.md on 2026-10-02.
 * The payloads are JSON objects read into maps, as the API would receive them.
 */
typealias Json = Map<String, Any?>

/** A JSON object as a map. */
@Suppress("UNCHECKED_CAST")
fun obj(json: String): Json = ObjectMapper().readValue(json, LinkedHashMap::class.java) as Json

@Suppress("UNCHECKED_CAST")
private fun map(o: Any?): Json = o as? Json ?: emptyMap()

private fun list(o: Any?): List<Any?> = o as? List<*> ?: emptyList<Any?>()

/** Python's notion of an empty or missing value. */
private fun truthy(o: Any?): Boolean = when (o) {
    null -> false
    is Boolean -> o
    is Map<*, *> -> o.isNotEmpty()
    is List<*> -> o.isNotEmpty()
    else -> true
}

// --- configuration checks -----------------------------------------------------------------------------------------------

/** Findings for an environment payload. An omitted networking field becomes `unrestricted` on the API, so it is a finding. */
fun checkEnvironment(env: Json, agentMcpHosts: List<String> = emptyList()): List<String> {
    val findings = mutableListOf<String>()
    if (env["type"] == "self_hosted") return findings
    val net = env["networking"]?.let { map(it) }
    if (net == null) {
        findings += "networking omitted: a create request that omits it gets unrestricted"
        return findings
    }
    if (net["type"] == "unrestricted") {
        findings += "unrestricted networking: any host except a safety blocklist; keep secrets out of the sandbox"
        return findings
    }
    for (host in list(net["allowed_hosts"]).map { it as String }) {
        if ("://" in host || "/" in host || ":" in host) findings += "allowed_hosts entry ${py(host)}: use a bare hostname, no scheme, port or path"
    }
    if (truthy(env["packages"]) && !truthy(net["allow_package_managers"])) {
        findings += "packages with limited networking need allow_package_managers: true, or the request is rejected (400)"
    }
    val hosts = list(net["allowed_hosts"]).toSet()
    for (host in agentMcpHosts) {
        if (host !in hosts && !truthy(net["allow_mcp_servers"])) {
            findings += "MCP host $host is not reachable: add it to allowed_hosts or set allow_mcp_servers, or session creation fails (400)"
        }
    }
    return findings
}

private fun policyType(config: Any?, fallback: String?): String? = map(config)["permission_policy"]?.let { map(it)["type"] as String? } ?: fallback

/** Findings for an agent payload read together with the environment it will run in. */
fun checkAgent(agent: Json, env: Json): List<String> {
    val findings = mutableListOf<String>()
    for (tool in list(agent["tools"]).map { map(it) }) {
        if (tool["type"] == "agent_toolset_20260401") {
            val policy = policyType(tool["default_config"], "always_allow")
            val overrides = list(tool["configs"]).associate { map(it)["name"] as String to policyType(it, null) }
            val bash = if ("bash" in overrides) overrides["bash"] else policy
            val reach = (env["networking"]?.let { map(it)["type"] } ?: "unrestricted") != "limited"
            if (bash == "always_allow" && reach) {
                findings += "bash runs without approval and the sandbox can reach any host: set bash to always_ask or auto, or use limited networking"
            }
            for (c in list(tool["configs"]).map { map(it) }) {
                if (truthy(c["allowed_domains"]) && truthy(c["blocked_domains"])) findings += "${c["name"]}: allowed_domains and blocked_domains cannot be combined"
            }
        }
        if (tool["type"] == "mcp_toolset" && policyType(tool["default_config"], "always_ask") == "always_allow") {
            findings += "mcp toolset ${tool["mcp_server_name"]}: always_allow lets new tools of that server run unreviewed"
        }
    }
    return findings
}

/** A self-hosted sandbox accepts memory_store resources only. */
fun checkSessionResources(env: Json, resources: List<Json>): List<String> {
    if (env["type"] != "self_hosted") return emptyList()
    return resources.map { it["type"] as String }.filter { it != "memory_store" }.toSortedSet().map { "self-hosted sandboxes reject $it resources (400)" }
}

// --- list cost and budget -----------------------------------------------------------------------------------------------

/** Dollars per million input and output tokens. */
val PRICES = mapOf("claude-opus-5-5" to (4L to 20L), "claude-sonnet-5-5" to (2L to 10L))
const val WEB_SEARCH_MICRO = 10_000L // $10 per 1,000 searches, in millionths of a dollar per search
const val RUNNING_MICRO_PER_HOUR = 80_000L // $0.08 per hour of session running time

/** The session's list cost in whole cents, rounded to the nearest cent, as the platform reports it. */
fun listCostCents(model: String, inputTokens: Long, outputTokens: Long, searches: Long, activeSeconds: Long): Long {
    val (priceIn, priceOut) = PRICES.getValue(model)
    val micro = inputTokens * priceIn + outputTokens * priceOut + searches * WEB_SEARCH_MICRO + activeSeconds * RUNNING_MICRO_PER_HOUR / 3600
    return (micro + 5_000) / 10_000
}

/** max_list_cost.amount is a whole number of cents as a string, no leading zeros, greater than zero. */
fun checkBudget(amount: String): String =
    if (!(amount.isNotEmpty() && amount.all { it in '0'..'9' } && !amount.startsWith("0"))) "amount ${py(amount)} is rejected: write whole cents as a string with no leading zeros" else "ok"

/** At or past the cap the session goes idle with stop_reason budget_reached; the request in flight still finishes. */
fun budgetState(costCents: Long, amount: String): String {
    val cap = amount.toLong()
    return if (costCents >= cap) "budget_reached" else "running, ${cap - costCents} cents left"
}

/** Python's repr of strings and lists of strings, so every language of the course prints the same text. */
fun py(v: Any?): String {
    if (v is List<*>) return v.joinToString(", ", "[", "]") { py(it) }
    val s = v.toString()
    val q = if ("'" in s && "\"" !in s) "\"" else "'"
    return q + s.replace("\\", "\\\\").replace("\n", "\\n").replace(q, "\\" + q) + q
}

fun main() {
    val env = obj("""{"type": "cloud", "packages": {"pip": ["sqlalchemy==2.0.30"]}, "networking": {"type": "limited", "allowed_hosts": ["https://api.example.com"]}}""")
    val agent = obj("""{"tools": [{"type": "agent_toolset_20260401"}, {"type": "mcp_toolset", "mcp_server_name": "github", "default_config": {"permission_policy": {"type": "always_allow"}}}]}""")
    println("environment findings:")
    for (line in checkEnvironment(env, listOf("mcp.example.com"))) println(" - $line")
    println("omitted networking: ${checkEnvironment(obj("""{"type": "cloud"}"""))[0]}")
    println("agent findings, unrestricted sandbox:")
    for (line in checkAgent(agent, obj("""{"type": "cloud"}"""))) println(" - $line")
    println("agent findings, limited sandbox: ${py(checkAgent(agent, obj("""{"type": "cloud", "networking": {"type": "limited"}}""")))}")
    println("self-hosted with a file resource: ${py(checkSessionResources(obj("""{"type": "self_hosted"}"""), listOf(obj("""{"type": "file"}"""), obj("""{"type": "memory_store"}"""))))}")
    println()
    for (model in listOf("claude-opus-5-5", "claude-sonnet-5-5")) {
        val cents = listCostCents(model, 1_200_000, 150_000, 8, 7200)
        println("$model: list cost $cents cents; budget 800 -> ${budgetState(cents, "800")}; budget 1000 -> ${budgetState(cents, "1000")}")
    }
    println("budget '25.00': ${checkBudget("25.00")} | budget '050': ${checkBudget("050")} | budget '125': ${checkBudget("125")}")
}
