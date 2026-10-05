import com.anthropic.client.AnthropicClient
import com.anthropic.core.JsonValue
import com.anthropic.core.jsonMapper
import com.anthropic.models.messages.ContentBlockParam
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.MessageParam
import com.anthropic.models.messages.Model
import com.anthropic.models.messages.Tool
import com.anthropic.models.messages.ToolResultBlockParam
import harness.Scripted
import harness.Scripted.map
import harness.Scripted.message
import harness.Scripted.text
import harness.Scripted.toolUse
import harness.Show.py

private val log = System.getLogger("identity_gate")

/**
 * The same scripted model, which skips identity verification, run against a loop that trusts the prompt and a loop that enforces the prerequisite in code.
 *
 * The replies are illustrative, hand-written bodies in the shape of the Messages API (claude-sonnet-5-5), not captures. The system prompt asks for
 * verification first in both runs: what differs is whether the code that runs the tools checks it.
 */
const val MODEL = "claude-sonnet-5-5"
const val SYSTEM = "You are a support agent. Verify the customer's identity before any refund."

fun tool(name: String, description: String, properties: Map<String, Any>): Tool = Tool.builder().name(name).description(description)
    .inputSchema(Tool.InputSchema.builder().properties(JsonValue.from(properties)).required(properties.keys.toList()).build()).build()

val TOOLS = listOf(
    tool("verify_identity", "Check the customer's identity code. Use it before any order or refund action.", map("code", map("type", "string"))),
    tool("lookup_order", "Look up one order of the verified customer.", map("order_id", map("type", "string"))),
    tool("process_refund", "Refund an amount in cents on a looked-up order.", map("order_id", map("type", "string"), "amount_cents", map("type", "integer"))),
)

class Backend {
    val log = mutableListOf<String>()
    var verified = false

    fun call(name: String, args: Map<*, *>): String {
        log += name
        return when (name) {
            "verify_identity" -> {
                verified = args["code"] == "1234"
                if (verified) "verified=yes" else "verified=no"
            }
            "lookup_order" -> "order_id=O1; total_cents=5000"
            "process_refund" -> "refund_id=R1"
            else -> throw IllegalArgumentException(name)
        }
    }
}

/** What one tool call returned and whether it is an error. */
data class Result(val name: String, val content: String, val isError: Boolean)

/** The prerequisite, in code: nothing but verification runs before the customer is verified. */
fun gate(backend: Backend, name: String, args: Map<*, *>): Result {
    if (name != "verify_identity" && !backend.verified) return Result(name, "BLOCKED identity_required: Verify the customer's identity before this action.", true)
    return Result(name, backend.call(name, args), false)
}

fun run(client: AnthropicClient, backend: Backend, gated: Boolean): List<Result> {
    val messages = mutableListOf(MessageParam.builder().role(MessageParam.Role.USER).content("Please refund 20.00 on order O1. My code is 1234.").build())
    val results = mutableListOf<Result>()
    while (true) {
        val request = MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(500).system(SYSTEM).messages(messages.toList()).apply { TOOLS.forEach { addTool(it) } }
        val reply = client.messages().create(request.build())
        messages += reply.toParam()
        if (reply.stopReason().get().asString() != "tool_use") return results
        val out = reply.content().filter { it.isToolUse() }.map { block ->
            val use = block.asToolUse()
            val args = jsonMapper().convertValue(use._input(), Map::class.java)
            val r = if (gated) gate(backend, use.name(), args) else Result(use.name(), backend.call(use.name(), args), false)
            results += r
            val b = ToolResultBlockParam.builder().toolUseId(use.id()).content(r.content)
            if (r.isError) b.isError(true)
            ContentBlockParam.ofToolResult(b.build())
        }
        messages += MessageParam.builder().role(MessageParam.Role.USER).contentOfBlockParams(out).build()
    }
}

fun replies(gated: Boolean): Array<Any> {
    val first = message(listOf(text("Refunding now."), toolUse("toolu_01", "process_refund", map("order_id", "O1", "amount_cents", 2000))), "tool_use")
    if (!gated) return arrayOf(first, message(listOf(text("Refund issued."))))
    return arrayOf(
        first,
        message(listOf(toolUse("toolu_02", "verify_identity", map("code", "1234"))), "tool_use"),
        message(listOf(toolUse("toolu_03", "lookup_order", map("order_id", "O1"))), "tool_use"),
        message(listOf(toolUse("toolu_04", "process_refund", map("order_id", "O1", "amount_cents", 2000))), "tool_use"),
        message(listOf(text("Refund issued after verification."))),
    )
}

fun main() {
    println("system prompt in both runs: $SYSTEM")
    for ((label, gated) in listOf("prompt only" to false, "code gate  " to true)) {
        val rig = Scripted.client(*replies(gated))
        val backend = Backend()
        val results = run(rig.client(), backend, gated)
        val before = if ("verify_identity" in backend.log) backend.log.indexOf("process_refund") < backend.log.indexOf("verify_identity") else "process_refund" in backend.log
        println("$label: backend calls = ${py(backend.log)}; refund before verification: ${py(before)}")
        if (gated) println("$label: first result sent back to the model: ${results[0].content} (is_error=${py(results[0].isError)})")
        check(rig.http().requests.all { it["system"].asText() == SYSTEM })
    }
}
