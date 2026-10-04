/** Capability design: what a role keeps, which tool definitions load up front, which mechanism connects a capability, whose rights a tool call uses and what a gateway decides. See ../../statement.md. */

/** A tool's access class and the size of its definition in tokens. */
data class Tool(val access: String, val tokens: Int)

/** What an agent holds, what its role needs and how often each tool was called. */
data class Agent(val holds: List<String>, val needs: List<String>, val used: Map<String, Int>)

/** The tools to remove, the risky ones among them, the needed tools the agent lacks and the held, needed tools nobody used. */
data class AuditResult(val remove: List<String>, val risky: List<String>, val missing: List<String>, val dormant: List<String>)

/** Whether a search tool is used, the tools loaded now and deferred, and the tokens loaded up front. */
data class Plan(val search: Boolean, val loadNow: List<String>, val deferred: List<String>, val tokens: Int)

/** A request to the gateway: who calls (null for nobody), which model, which tool (null for none) and how many requests the team sent this minute. */
data class Request(val credential: String?, val model: String, val tool: String?, val recent: Int)

/** The gateway's rules: credential to team, the models and tools each team may use, requests a minute and where each model name is routed. */
data class Policy(val credentials: Map<String, String>, val models: Map<String, Set<String>>, val tools: Map<String, Set<String>>, val limits: Map<String, Int>, val routes: Map<String, String>)

/** The gateway's decision, its reason and the record kept of it. */
data class Outcome(val decision: String, val reason: String, val audit: Map<String, String>)

private val RISKY = setOf("money", "destroy")

fun audit(agent: Agent, catalog: Map<String, Tool>): AuditResult? {
    val remove = agent.holds.filter { it !in agent.needs }
    return AuditResult(
        remove,
        remove.filter { catalog.getValue(it).access in RISKY },
        agent.needs.filter { it !in agent.holds },
        agent.holds.filter { it in agent.needs && (agent.used[it] ?: 0) == 0 },
    )
}

fun planLoading(tools: Map<String, Int>, usage: Map<String, Int>, keep: Int = 4, searchTokens: Int = 350): Plan? {
    val kept = keep.coerceIn(3, 5)
    val names = tools.keys.toList()
    if (names.size < 10 && tools.values.sum() <= 10000) return Plan(false, names, emptyList(), tools.values.sum())
    val ranked = names.sortedWith(compareBy({ -(usage[it] ?: 0) }, { it })).take(kept)
    return Plan(true, ranked, names.filter { it !in ranked }, ranked.sumOf { tools.getValue(it) } + searchTokens)
}

fun chooseMechanism(consumers: Int, counterpart: String, path: String): String? = when {
    counterpart == "agent" -> "agent-to-agent"
    path == "fixed" -> "direct call in code"
    consumers > 1 -> "MCP server"
    else -> "custom tool"
}

fun authorize(tool: String, userScopes: Set<String>, agentScopes: Set<String>, required: Map<String, String>): String? {
    val scope = required[tool] ?: return "deny: unknown tool"
    if (scope !in userScopes) return "deny: user lacks $scope"
    if (scope !in agentScopes) return "deny: agent lacks $scope"
    return "allow"
}

fun gateway(request: Request, policy: Policy): Outcome? {
    val team = request.credential?.let { policy.credentials[it] }
    val (decision, reason) = when {
        team == null -> "deny" to "unauthenticated"
        request.model !in policy.models.getValue(team) -> "deny" to "model not allowed"
        request.tool != null && request.tool !in policy.tools.getValue(team) -> "deny" to "tool not allowed"
        request.recent >= policy.limits.getValue(team) -> "deny" to "rate limited"
        else -> "allow" to "routed to ${policy.routes[request.model] ?: request.model}"
    }
    return Outcome(decision, reason, mapOf("team" to (team ?: "unknown"), "model" to request.model, "tool" to (request.tool ?: "none"), "decision" to decision))
}
