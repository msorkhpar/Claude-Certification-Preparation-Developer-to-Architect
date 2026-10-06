private val log = System.getLogger("capability")

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

private fun remove(agent: Agent): List<String> {
    return agent.holds.filter { it !in agent.needs }
}

private fun risky(remove: List<String>, catalog: Map<String, Tool>): List<String> {
    return remove.filter { catalog.getValue(it).access in RISKY }
}

private fun dormant(agent: Agent): List<String> {
    return agent.holds.filter { it in agent.needs && (agent.used[it] ?: 0) == 0 }
}

fun audit(agent: Agent, catalog: Map<String, Tool>): AuditResult? {
    log.log(System.Logger.Level.DEBUG, "audit input: {0}", agent)
    val gone = remove(agent)
    return AuditResult(gone, risky(gone, catalog), agent.needs.filter { it !in agent.holds }, dormant(agent))
}

private fun clamp(keep: Int): Int {
    return keep.coerceIn(3, 5)
}

private fun defers(tools: Map<String, Int>): Boolean {
    return tools.size >= 10 || tools.values.sum() > 10000
}

private fun ranked(tools: Map<String, Int>, usage: Map<String, Int>, keep: Int): List<String> {
    return tools.keys.sortedWith(compareBy({ -(usage[it] ?: 0) }, { it })).take(keep)
}

fun planLoading(tools: Map<String, Int>, usage: Map<String, Int>, keep: Int = 4, searchTokens: Int = 350): Plan? {
    log.log(System.Logger.Level.DEBUG, "planLoading input: {0}", tools)
    val names = tools.keys.toList()
    if (!defers(tools)) return Plan(false, names, emptyList(), tools.values.sum())
    val first = ranked(tools, usage, clamp(keep))
    return Plan(true, first, names.filter { it !in first }, first.sumOf { tools.getValue(it) } + searchTokens)
}

fun chooseMechanism(consumers: Int, counterpart: String, path: String): String? {
    return when {
        counterpart == "agent" -> "agent-to-agent"
        path == "fixed" -> "direct call in code"
        consumers > 1 -> "MCP server"
        else -> "custom tool"
    }
}

fun authorize(tool: String, userScopes: Set<String>, agentScopes: Set<String>, required: Map<String, String>): String? {
    val scope = required[tool] ?: return "deny: unknown tool"
    if (scope !in userScopes) return "deny: user lacks $scope"
    if (scope !in agentScopes) return "deny: agent lacks $scope"
    return "allow"
}

private fun verdict(team: String?, request: Request, policy: Policy): Pair<String, String> {
    return when {
        team == null -> "deny" to "unauthenticated"
        request.model !in policy.models.getValue(team) -> "deny" to "model not allowed"
        request.tool != null && request.tool !in policy.tools.getValue(team) -> "deny" to "tool not allowed"
        request.recent >= policy.limits.getValue(team) -> "deny" to "rate limited"
        else -> "allow" to "routed to ${policy.routes[request.model] ?: request.model}"
    }
}

private fun record(team: String?, request: Request, decision: String): Map<String, String> {
    return mapOf("team" to (team ?: "unknown"), "model" to request.model, "tool" to (request.tool ?: "none"), "decision" to decision)
}

fun gateway(request: Request, policy: Policy): Outcome? {
    log.log(System.Logger.Level.DEBUG, "gateway input: {0}", request)
    val team = request.credential?.let { policy.credentials[it] }
    val (decision, reason) = verdict(team, request, policy)
    return Outcome(decision, reason, record(team, request, decision))
}
