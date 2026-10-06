private val log = System.getLogger("capability_audit")

/**
 * Integration design decisions in code: which tools a role keeps, which tool definitions load up front, which mechanism connects a capability, whose rights a tool call uses and what a gateway decides.
 *
 * The numbers are invented for the example, and the sizes of tool definitions are the example's own. The rules are those of the Claude Certified Architect - Professional exam guide (domain 3), the Claude documentation page
 * "Tool search tool", the Model Context Protocol security best practices and the Claude Code gateway pages, read on 2026-10-04. Nothing here calls a model.
 */

/** A tool's access class and the size of its definition in tokens. */
data class Tool(val access: String, val tokens: Int)

/** The outcome of an audit: what to remove, which of that is risky and what the role lacks. */
data class Audit(val remove: List<String>, val risky: List<String>, val missing: List<String>)

/** How tool definitions load: with a search tool or not, which tools now and which later, and the tokens up front. */
data class Plan(val search: Boolean, val loadNow: List<String>, val deferred: List<String>, val tokens: Int)

/** The gateway's rules: which credential belongs to which team, the models and requests a minute each team may use, and where a model name is routed. */
data class Policy(val credentials: Map<String, String>, val models: Map<String, Set<String>>, val limits: Map<String, Int>, val routes: Map<String, String>)

/** The gateway's decision, its reason and the record kept of it. */
data class Outcome(val decision: String, val reason: String, val audit: Map<String, String>)

val CATALOG = linkedMapOf("read_ticket" to Tool("read", 160), "draft_reply" to Tool("draft", 220), "issue_refund" to Tool("money", 240), "delete_account" to Tool("destroy", 210))
private val RISKY = setOf("money", "destroy")
private val SERVER_TOOLS = linkedMapOf(
    "github" to listOf("create_issue", "search_code", "get_pr", "list_prs", "merge_pr", "comment", "list_repos", "get_file"),
    "slack" to listOf("post_message", "search", "list_channels", "get_thread", "react", "upload"),
    "sentry" to listOf("list_issues", "get_event", "resolve", "assign", "search"),
    "grafana" to listOf("query", "list_dashboards", "get_panel", "create_alert", "list_alerts"),
)
private val SERVER_SIZE = mapOf("github" to 520, "slack" to 410, "sentry" to 480, "grafana" to 620)
val TOOLS: Map<String, Int> = SERVER_TOOLS.flatMap { (server, names) -> names.map { "${server}_$it" to SERVER_SIZE.getValue(server) } }.toMap(LinkedHashMap())
val USAGE = mapOf("github_create_issue" to 90, "github_search_code" to 70, "slack_post_message" to 60, "github_get_pr" to 50, "sentry_list_issues" to 20, "grafana_query" to 10)
val POLICY = Policy(
    mapOf("key-a" to "support", "key-b" to "research"), mapOf("support" to setOf("standard"), "research" to setOf("standard", "deep")), mapOf("support" to 30, "research" to 10),
    mapOf("standard" to "claude-sonnet-5-5", "deep" to "claude-opus-5-5"),
)

/** Least privilege: a tool the role does not need is removed from its configuration, not logged or put behind a confirmation. */
fun audit(holds: List<String>, needs: List<String>, catalog: Map<String, Tool>): Audit {
    log.log(System.Logger.Level.DEBUG, "audit input: {0}", holds)
    val remove = holds.filter { it !in needs }
    return Audit(remove, remove.filter { catalog.getValue(it).access in RISKY }, needs.filter { it !in holds })
}

/** With 10 or more tools, or definitions over 10,000 tokens, the 3 to 5 most used tools stay loaded and the rest are found through a search tool. */
fun planLoading(tools: Map<String, Int>, usage: Map<String, Int>, keep: Int = 4, searchTokens: Int = 350): Plan {
    val kept = keep.coerceIn(3, 5)
    val names = tools.keys.toList()
    val total = tools.values.sum()
    if (names.size < 10 && total <= 10000) return Plan(false, names, emptyList(), total)
    val ranked = names.sortedWith(compareBy({ -(usage[it] ?: 0) }, { it })).take(kept)
    return Plan(true, ranked, names.filter { it !in ranked }, ranked.sumOf { tools.getValue(it) } + searchTokens)
}

/** Another agent is reached agent-to-agent; a step with a known path is a call in code; a capability several clients share is an MCP server; otherwise it is a tool of the one application. */
fun chooseMechanism(consumers: Int, counterpart: String, path: String): String = when {
    counterpart == "agent" -> "agent-to-agent"
    path == "fixed" -> "direct call in code"
    consumers > 1 -> "MCP server"
    else -> "custom tool"
}

/** A call is allowed only when the user holds the scope the tool needs and the agent does too; the agent's own rights are a ceiling, not a licence. */
fun authorize(tool: String, userScopes: Set<String>, agentScopes: Set<String>, required: Map<String, String>): String {
    val scope = required[tool] ?: return "deny: unknown tool"
    if (scope !in userScopes) return "deny: user lacks $scope"
    if (scope !in agentScopes) return "deny: agent lacks $scope"
    return "allow"
}

/** One place decides who is calling, which models that team may use and how many requests a minute it may send, and keeps a record of every decision. */
fun gateway(credential: String?, model: String, recent: Int, policy: Policy): Outcome {
    val team = credential?.let { policy.credentials[it] }
    val (decision, reason) = when {
        team == null -> "deny" to "unauthenticated"
        model !in policy.models.getValue(team) -> "deny" to "model not allowed"
        recent >= policy.limits.getValue(team) -> "deny" to "rate limited"
        else -> "allow" to "routed to ${policy.routes[model] ?: model}"
    }
    return Outcome(decision, reason, mapOf("team" to (team ?: "unknown"), "model" to model, "decision" to decision))
}

private fun percent(part: Int, whole: Int) = (200 * part + whole) / (2 * whole)

fun main() {
    val holds = CATALOG.keys.toList()
    val needs = listOf("read_ticket", "draft_reply")
    val result = audit(holds, needs, CATALOG)
    println("support agent holds ${holds.size} tools (${holds.sumOf { CATALOG.getValue(it).tokens }} tokens) and needs ${needs.size}")
    println("  remove: ${result.remove.joinToString(", ")}; risky among them: ${result.risky.joinToString(", ")}")
    println("  after removal: ${holds.size - result.remove.size} tools, ${needs.sumOf { CATALOG.getValue(it).tokens }} tokens")
    val three = CATALOG.entries.take(3).associate { it.key to it.value.tokens }
    var plan = planLoading(three, USAGE)
    for ((name, tools) in listOf("three tools" to three, "four servers" to TOOLS)) {
        plan = planLoading(tools, USAGE)
        val total = tools.values.sum()
        val saved = if (plan.search) ", ${percent(total - plan.tokens, total)}% fewer" else ""
        println("$name: ${tools.size} tools, $total tokens of definitions -> search tool ${if (plan.search) "yes" else "no"}, ${plan.loadNow.size} loaded now, ${plan.deferred.size} deferred, ${plan.tokens} tokens up front$saved")
    }
    println("  loaded now: " + plan.loadNow.joinToString(", "))
    for ((consumers, counterpart, path) in listOf(Triple(1, "tool", "model-chosen"), Triple(4, "tool", "model-chosen"), Triple(1, "tool", "fixed"), Triple(1, "agent", "model-chosen"))) {
        println("  %d application(s), counterpart %-5s, path %-12s -> %s".format(consumers, counterpart, path, chooseMechanism(consumers, counterpart, path)))
    }
    val required = mapOf("read_ticket" to "tickets:read", "issue_refund" to "refunds:write")
    val user = setOf("tickets:read")
    val agent = setOf("tickets:read", "refunds:write")
    println("refund asked by a user who may only read: agent's rights alone -> ${if (required.getValue("issue_refund") in agent) "allow" else "deny"}; user's and agent's rights -> ${authorize("issue_refund", user, agent, required)}")
    val log = mutableListOf<Map<String, String>>()
    for ((credential, model, recent) in listOf(Triple(null, "standard", 0), Triple("key-a", "deep", 0), Triple("key-a", "standard", 30), Triple("key-b", "deep", 3))) {
        val outcome = gateway(credential, model, recent, POLICY)
        log += outcome.audit
        println("  gateway: credential %-5s model %-8s recent %2d -> %s: %s".format(credential ?: "none", model, recent, outcome.decision, outcome.reason))
    }
    println("  records kept: ${log.size}, denials among them: ${log.count { it["decision"] == "deny" }}")
}
