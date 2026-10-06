import com.anthropic.client.AnthropicClient
import com.anthropic.core.JsonValue
import com.anthropic.models.messages.ContentBlockParam
import com.anthropic.models.messages.Message
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

private val log = System.getLogger("loops")

/**
 * Three loops over the same scripted replies: one ends on stop_reason, one on a word in the text, one after a fixed count.
 *
 * The replies are illustrative, hand-written bodies in the shape of the Messages API (claude-sonnet-5-5), not captures.
 * Only the first loop is right; the other two show what the two anti-patterns of the Architect exam do to a run.
 */
const val MODEL = "claude-sonnet-5-5"

fun tool(name: String, description: String, property: String, type: String): Tool = Tool.builder().name(name).description(description)
    .inputSchema(Tool.InputSchema.builder().properties(JsonValue.from(map(property, map("type", type)))).required(listOf(property)).build()).build()

val TOOLS = listOf(
    tool("save_file", "Save the report under a file name. Use it once the report is written.", "file", "string"),
    tool("lookup", "Look up item number n and return its record.", "n", "integer"),
)

/** What a loop returns: the status, the number of model calls, the tools that ran, and the last reply's text. */
data class Outcome(val status: String, val calls: Int, val ran: List<String>, val answer: String)

fun ask(client: AnthropicClient, messages: MutableList<MessageParam>): Message {
    val request = MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(500).messages(messages.toList()).apply { TOOLS.forEach { addTool(it) } }
    val reply = client.messages().create(request.build())
    messages += reply.toParam()
    return reply
}

fun runTools(reply: Message, ran: MutableList<String>): List<ContentBlockParam> = reply.content().filter { it.isToolUse() }.map {
    val use = it.asToolUse()
    ran += use.name()
    ContentBlockParam.ofToolResult(ToolResultBlockParam.builder().toolUseId(use.id()).content("${use.name()} ok").build())
}

fun textOf(reply: Message): String = reply.content().filter { it.isText() }.joinToString("") { it.asText().text() }

fun user(task: String): MessageParam = MessageParam.builder().role(MessageParam.Role.USER).content(task).build()

fun results(blocks: List<ContentBlockParam>): MessageParam = MessageParam.builder().role(MessageParam.Role.USER).contentOfBlockParams(blocks).build()

/** Right: the model's own stop_reason decides. The count is only a backstop with a status of its own. */
fun byStopReason(client: AnthropicClient, task: String, backstop: Int = 10): Outcome {
    val messages = mutableListOf(user(task))
    val ran = mutableListOf<String>()
    var reply: Message? = null
    for (call in 1..backstop) {
        reply = ask(client, messages)
        if (reply.stopReason().get().asString() != "tool_use") return Outcome("done", call, ran, textOf(reply))
        messages += results(runTools(reply, ran))
    }
    return Outcome("max_turns", backstop, ran, textOf(reply!!))
}

/** Wrong: it reads the words. A reply that says 'done' ends the run, even with a tool call in the same reply. */
fun byTextMarker(client: AnthropicClient, task: String): Outcome {
    val messages = mutableListOf(user(task))
    val ran = mutableListOf<String>()
    var call = 0
    while (true) {
        call++
        val reply = ask(client, messages)
        if ("done" in textOf(reply).lowercase()) return Outcome("done", call, ran, textOf(reply))
        messages += results(runTools(reply, ran))
    }
}

/** Wrong: the count is the loop. Whatever the third reply holds is returned as the answer. */
fun byFixedCount(client: AnthropicClient, task: String, turns: Int = 3): Outcome {
    val messages = mutableListOf(user(task))
    val ran = mutableListOf<String>()
    var reply: Message? = null
    for (call in 1..turns) {
        reply = ask(client, messages)
        if (reply.stopReason().get().asString() == "tool_use") messages += results(runTools(reply, ran))
    }
    return Outcome("done", turns, ran, textOf(reply!!))
}

fun scenarioA(): Array<Any> = arrayOf(
    message(listOf(text("All done with the analysis. Saving it now."), toolUse("toolu_01", "save_file", map("file", "report.txt"))), "tool_use"),
    message(listOf(text("Saved report.txt."))),
)

fun scenarioB(): Array<Any> = (1..4).map<Int, Any> { n -> message(listOf(toolUse("toolu_0$n", "lookup", map("n", n))), "tool_use") }
    .plus(message(listOf(text("Looked up 4 items.")))).toTypedArray()

fun show(label: String, o: Outcome) {
    println("  %-26s status=%-9s model calls=%d  tools run=%d  text=%s".format(label, o.status, o.calls, o.ran.size, py(o.answer)))
}

fun main() {
    println("A: the reply says 'All done' and also calls save_file")
    show("stop_reason loop", byStopReason(Scripted.client(*scenarioA()).client(), "Write and save the report."))
    show("text-marker loop", byTextMarker(Scripted.client(*scenarioA()).client(), "Write and save the report."))
    println("B: the task needs four lookups, then the model ends its turn")
    show("stop_reason loop, cap 10", byStopReason(Scripted.client(*scenarioB()).client(), "Look up items 1 to 4."))
    show("stop_reason loop, cap 3", byStopReason(Scripted.client(*scenarioB()).client(), "Look up items 1 to 4.", backstop = 3))
    show("fixed-count loop of 3", byFixedCount(Scripted.client(*scenarioB()).client(), "Look up items 1 to 4."))
}
