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
    // TODO 1 of 10 (unlocks m1): the tools to take away.
    // Receives the agent (`holds`, `needs`, `used`). Returns the held tools that the role does not need, in the order they are held.
    // Example: holds [read, refund], needs [read] -> [refund]
    return emptyList()
}

private fun risky(remove: List<String>, catalog: Map<String, Tool>): List<String> {
    // TODO 2 of 10 (unlocks m1): the risky tools among those to remove.
    // Receives the tools to remove and the catalog (tool to its access class and tokens). Returns those whose access class is in RISKY, in the same order.
    // Example: remove [refund, export], refund has access "money" and export has "read" -> [refund]
    return emptyList()
}

private fun dormant(agent: Agent): List<String> {
    // TODO 3 of 10 (unlocks e1): the tools that are kept but never used.
    // Receives the agent. Returns the held tools that the role needs and that have no calls in `used` (a tool missing from `used` has none), in the order held. They are
    // reported and never removed.
    // Example: holds [a, b], needs [a, b], used {a: 5, b: 0} -> [b]
    return emptyList()
}

fun audit(agent: Agent, catalog: Map<String, Tool>): AuditResult? {
    log.log(System.Logger.Level.DEBUG, "audit input: {0}", agent)
    val gone = remove(agent)
    return AuditResult(gone, risky(gone, catalog), agent.needs.filter { it !in agent.holds }, dormant(agent))
}

private fun clamp(keep: Int): Int {
    // TODO 4 of 10 (unlocks e3): how many tools to keep loaded.
    // Receives the number asked for. Returns it limited to the range from 3 to 5.
    // Example: clamp(1) -> 3, clamp(8) -> 5, clamp(4) -> 4
    return keep
}

private fun defers(tools: Map<String, Int>): Boolean {
    // TODO 5 of 10 (unlocks e2): must the definitions be deferred?
    // Receives the tools (name to the tokens of its definition). Returns true when there are 10 tools or more, or the definitions together are over 10000 tokens (10000 itself is fine).
    // Example: 9 tools of 100 tokens -> false; 10 tools -> true; two tools of 5000 and 5001 -> true
    return false
}

private fun ranked(tools: Map<String, Int>, usage: Map<String, Int>, keep: Int): List<String> {
    // TODO 6 of 10 (unlocks e3): the tools to load first.
    // Receives the tools, the usage counts (a tool missing from `usage` has 0) and how many to keep. Returns that many tool names, the most used first and by name among equals.
    // Example: usage {b: 9, a: 9, c: 1}, keep 2 -> [a, b]
    return tools.keys.toList().take(keep)
}

fun planLoading(tools: Map<String, Int>, usage: Map<String, Int>, keep: Int = 4, searchTokens: Int = 350): Plan? {
    log.log(System.Logger.Level.DEBUG, "planLoading input: {0}", tools)
    val names = tools.keys.toList()
    if (!defers(tools)) return Plan(false, names, emptyList(), tools.values.sum())
    val first = ranked(tools, usage, clamp(keep))
    return Plan(true, first, names.filter { it !in first }, first.sumOf { tools.getValue(it) } + searchTokens)
}

fun chooseMechanism(consumers: Int, counterpart: String, path: String): String? {
    // TODO 7 of 10 (unlocks e4): how a capability is connected.
    // Receives the number of clients that will use it, the counterpart ("agent" or "tool") and the path ("fixed" or "model-chosen"). Decide in this order:
    // a counterpart that is an agent gives "agent-to-agent"; a fixed path gives "direct call in code"; otherwise "MCP server" for more than one client and "custom tool" for one.
    // Example: chooseMechanism(4, "tool", "model-chosen") -> "MCP server"
    return null
}

fun authorize(tool: String, userScopes: Set<String>, agentScopes: Set<String>, required: Map<String, String>): String? {
    // TODO 8 of 10 (unlocks e5): whose rights a tool call uses.
    // Receives the tool name, the scopes of the user, the scopes of the agent and `required` (tool to the scope it needs). Returns "deny: unknown tool" for a tool
    // that is not in `required`; then "deny: user lacks <scope>"; then "deny: agent lacks <scope>"; otherwise "allow". Both the user and the agent must hold the scope.
    // Example: scope "refunds:write" held by the agent only -> "deny: user lacks refunds:write"
    return null
}

private fun verdict(team: String?, request: Request, policy: Policy): Pair<String, String> {
    // TODO 9 of 10 (unlocks e6): the decision and its reason.
    // Receives the team (null when the credential is unknown), the request (`model`, `tool` or null, `recent`) and the policy (`models`, `tools` and `limits` per team,
    // `routes`). Checks in this order and stops at the first that fails, returning "deny" to reason: "unauthenticated" when there is no team; "model not allowed";
    // "tool not allowed" (only when a tool is named); "rate limited" when `recent` is at or over the team's limit. Otherwise "allow" to "routed to <name>", where the name is
    // `policy.routes` for the model, or the model itself when it has no route.
    // Example: team "support", model "standard", no tool, recent 1, limit 2 -> "allow" to "routed to claude-sonnet-5-5"
    return "allow" to ""
}

private fun record(team: String?, request: Request, decision: String): Map<String, String> {
    // TODO 10 of 10 (unlocks e7): the record kept of a decision.
    // Receives the team (null when unknown), the request and the decision. Returns a map with `team`, `model`, `tool` and `decision`: the team or "unknown", the model, the tool or
    // "none", and the decision. Nothing else: no prompt and no credential.
    // Example: team null, model "deep", tool null, "deny" -> {team: unknown, model: deep, tool: none, decision: deny}
    return emptyMap()
}

fun gateway(request: Request, policy: Policy): Outcome? {
    log.log(System.Logger.Level.DEBUG, "gateway input: {0}", request)
    val team = request.credential?.let { policy.credentials[it] }
    val (decision, reason) = verdict(team, request, policy)
    return Outcome(decision, reason, record(team, request, decision))
}
