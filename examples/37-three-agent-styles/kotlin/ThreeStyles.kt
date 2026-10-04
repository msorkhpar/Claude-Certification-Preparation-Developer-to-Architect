import com.fasterxml.jackson.databind.ObjectMapper

/**
 * One small task, three ways to build it: a graph you draw, a loop the model drives, and a typed result you validate.
 *
 * The task is a support ticket: decide whether it is about billing, look the invoice up when it is, and write a reply. The three
 * functions below are miniatures of what graph-based, model-driven and typed agent frameworks give you. They are the course's own
 * sketches, not any framework's code, and the model is a scripted function, so nothing here calls an API.
 */
const val TICKET = "I was charged twice for invoice 1042."
val INVOICES = mapOf("1042" to "paid twice on 2026-09-30")
private val JSON = ObjectMapper()

/** A stand-in model: it returns the next scripted reply and keeps the prompts it was shown. */
class Scripted(vararg replies: String) : (String) -> String {
    private val queue = replies.toMutableList()
    val seen = mutableListOf<String>()

    override fun invoke(prompt: String): String {
        seen += prompt
        return queue.removeAt(0)
    }
}

// 1. Graph style: the programmer fixes the nodes and the edges; the model only fills in a node. State is explicit and checkpointed.

typealias State = Map<String, String>

data class Checkpoint(val node: String, val state: State)

data class GraphRun(val path: List<String>, val state: State, val checkpoints: MutableList<Checkpoint>)

fun runGraph(model: (String) -> String, ticket: String, resumeFrom: Int = 0, given: MutableList<Checkpoint>? = null): GraphRun {
    val nodes = mapOf<String, (State) -> State>(
        "classify" to { s -> mapOf("topic" to model("Classify as billing or other: ${s["ticket"]}").trim().lowercase()) },
        "lookup" to { s -> mapOf("invoice" to (INVOICES[s.getValue("ticket").split("invoice ")[1].trimEnd('.')] ?: "unknown")) },
        "draft" to { s -> mapOf("reply" to model("Write a reply. Topic: ${s["topic"]}. Invoice: ${s["invoice"] ?: "none"}")) },
    )
    val edges = mapOf<String, (State) -> String?>(
        "classify" to { s -> if (s["topic"] == "billing") "lookup" else "draft" },
        "lookup" to { "draft" },
        "draft" to { null },
    )
    val checkpoints = given ?: mutableListOf(Checkpoint("classify", mapOf("ticket" to ticket)))
    var node: String? = checkpoints[resumeFrom].node
    var state = checkpoints[resumeFrom].state
    while (checkpoints.size > resumeFrom + 1) checkpoints.removeLast()
    val path = mutableListOf<String>()
    while (node != null) {
        state = state + nodes.getValue(node)(state)
        path += node
        node = edges.getValue(node)(state)
        if (node != null) checkpoints += Checkpoint(node, state.toMap())
    }
    return GraphRun(path, state, checkpoints)
}

// 2. Model-driven style: the model sees the tools and decides the next step; the loop only executes and stops.

data class AgentRun(val reply: String?, val trace: List<String>, val stopped: String? = null)

fun runAgent(model: (String) -> String, ticket: String, maxSteps: Int = 5): AgentRun {
    val tools = mapOf<String, (String) -> String>("lookup_invoice" to { arg -> INVOICES[arg] ?: "unknown" })
    val trace = mutableListOf<String>()
    var observation = ""
    repeat(maxSteps) {
        @Suppress("UNCHECKED_CAST")
        val step = JSON.readValue(model("Ticket: $ticket\nTools: ${py(tools.keys.sorted())}\nLast result: $observation\nReply JSON: a tool call or a final reply."), Map::class.java) as Map<String, Any?>
        if ("final" in step) return AgentRun(step["final"] as String, trace)
        observation = tools.getValue(step["tool"] as String)(step["arg"] as String)
        trace += "${step["tool"]}(${step["arg"]}) -> $observation"
    }
    return AgentRun(null, trace, "max_steps")
}

// 3. Typed style: the answer must match a schema; a mismatch goes back to the model as feedback, once.

/** The checked reply, or the reason it was refused. */
data class Checked(val data: Map<String, Any?>?, val error: String?)

data class TypedRun(val data: Map<String, Any?>?, val attempts: Int, val error: String? = null)

fun validate(raw: String): Checked {
    @Suppress("UNCHECKED_CAST")
    val data = runCatching { JSON.readValue(raw, LinkedHashMap::class.java) as Map<String, Any?> }.getOrNull() ?: return Checked(null, "the reply is not JSON")
    if (data["topic"] !is String) return Checked(null, "field topic must be str")
    if (data["refund_cents"] !is Int) return Checked(null, "field refund_cents must be int")
    return Checked(data, null)
}

fun runTyped(model: (String) -> String, ticket: String, retries: Int = 1): TypedRun {
    var prompt = "Return JSON with topic and refund_cents for: $ticket"
    var attempts = 0
    while (true) {
        attempts++
        val (data, error) = validate(model(prompt))
        if (data != null) return TypedRun(data, attempts)
        if (attempts > retries) return TypedRun(null, attempts, error)
        prompt = "$prompt\nYour last reply was refused: $error. Fix it."
    }
}

/** Python's repr of strings, lists and maps, so every language of the course prints the same text. */
fun py(v: Any?): String = when (v) {
    null -> "None"
    is String -> (if ("'" in v && "\"" !in v) "\"" else "'").let { q -> q + v.replace("\\", "\\\\").replace("\n", "\\n").replace(q, "\\" + q) + q }
    is Map<*, *> -> v.entries.joinToString(", ", "{", "}") { "${py(it.key)}: ${py(it.value)}" }
    is List<*> -> v.joinToString(", ", "[", "]") { py(it) }
    else -> v.toString()
}

fun main() {
    val m = Scripted("billing", "We refunded the duplicate charge on invoice 1042.")
    val graph = runGraph(m, TICKET)
    println("graph: ${graph.path.joinToString(" -> ")} | model calls: ${m.seen.size} | checkpoints: ${graph.checkpoints.size}")
    val again = Scripted("We refunded the duplicate charge on invoice 1042.")
    val resumed = runGraph(again, TICKET, resumeFrom = 2, given = graph.checkpoints.toMutableList())
    println("graph resumed from checkpoint 2: ${resumed.path.joinToString(" -> ")} | model calls: ${again.seen.size} | same reply: ${if (resumed.state["reply"] == graph.state["reply"]) "True" else "False"}")
    val a = Scripted("""{"tool": "lookup_invoice", "arg": "1042"}""", """{"final": "Refunded the second payment."}""")
    val agent = runAgent(a, TICKET)
    println("agent: ${py(agent.trace)} -> ${agent.reply} | model calls: ${a.seen.size}")
    val loop = Scripted(*Array(3) { """{"tool": "lookup_invoice", "arg": "1"}""" })
    println("agent that never finishes: ${runAgent(loop, TICKET, maxSteps = 3).stopped} after ${loop.seen.size} calls")
    val t = Scripted("""{"topic": "billing", "refund_cents": "4999"}""", """{"topic": "billing", "refund_cents": 4999}""")
    val typed = runTyped(t, TICKET)
    println("typed: ${py(typed.data)} | attempts: ${typed.attempts} | second prompt ends: ${t.seen[1].lines().last()}")
    val bad = runTyped(Scripted("no json", "still no json"), TICKET)
    println("typed, never valid: ${bad.error} after ${bad.attempts} attempts")
}
