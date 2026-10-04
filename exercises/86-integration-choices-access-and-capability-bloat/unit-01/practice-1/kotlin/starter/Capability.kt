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
    // TODO: the tools to remove (held, not needed), the risky ones among them, the needed tools the agent lacks and the held, needed tools nobody used.
    return null
}

fun planLoading(tools: Map<String, Int>, usage: Map<String, Int>, keep: Int = 4, searchTokens: Int = 350): Plan? {
    // TODO: load everything for a small set, otherwise keep the most used tools loaded and defer the rest behind a search tool.
    return null
}

fun chooseMechanism(consumers: Int, counterpart: String, path: String): String? {
    // TODO: agent-to-agent, direct call in code, MCP server or custom tool.
    return null
}

fun authorize(tool: String, userScopes: Set<String>, agentScopes: Set<String>, required: Map<String, String>): String? {
    // TODO: allow, or deny with the reason, when the user's rights and the agent's rights do not both cover the tool.
    return null
}

fun gateway(request: Request, policy: Policy): Outcome? {
    // TODO: authenticate, check the model, the tool and the rate in that order, route an allowed request and keep a record of every decision.
    return null
}
