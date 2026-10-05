import com.anthropic.client.AnthropicClient
import com.anthropic.core.JsonValue
import com.anthropic.core.jsonMapper
import com.anthropic.models.messages.ContentBlockParam
import com.anthropic.models.messages.Message
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.MessageParam
import com.anthropic.models.messages.Model
import com.anthropic.models.messages.Tool
import com.anthropic.models.messages.ToolChoice
import com.anthropic.models.messages.ToolChoiceAuto
import com.anthropic.models.messages.ToolResultBlockParam
import com.anthropic.models.messages.ToolUseBlock
import harness.Scripted
import harness.Scripted.map
import harness.Scripted.message
import harness.Scripted.text
import harness.Scripted.toolUse
import harness.Show.py

private val log = System.getLogger("tool_loop")

/**
 * A tool loop on the official SDK, against a scripted model: parallel calls, one failing tool and a tool_choice that is kept.
 *
 * The replies are illustrative, hand-written bodies in the shape of the Messages API (claude-sonnet-5-5), not captures.
 */
const val MODEL = "claude-sonnet-5-5"

fun tool(name: String, description: String): Tool = Tool.builder().name(name).description(description)
    .inputSchema(
        Tool.InputSchema.builder()
            .properties(JsonValue.from(map("city", map("type", "string", "description", "City name, for example Oslo"))))
            .required(listOf("city")).build(),
    ).build()

val TOOLS = listOf(
    tool("get_weather", "Current weather for one city. Use it when the user asks about weather now. Returns a short sentence; it knows nothing about forecasts."),
    tool("get_time", "Local time for one city, as HH:MM on a 24 hour clock. Use it when the user asks what time it is somewhere."),
)

val HANDLERS: Map<String, (Map<*, *>) -> String?> = mapOf(
    "get_weather" to { a -> mapOf("Oslo" to "Oslo: 4 C, light rain")[a["city"]] },
    "get_time" to { a -> mapOf("Oslo" to "09:15", "Rome" to "09:15")[a["city"]] },
)

/** One tool call answered: the result block that goes back to the model. */
fun runTool(block: ToolUseBlock): ToolResultBlockParam {
    val input = jsonMapper().convertValue(block._input(), Map::class.java)
    val answer = HANDLERS.getValue(block.name())(input)
        ?: return ToolResultBlockParam.builder().toolUseId(block.id()).content("No data for '${input["city"]}'. Known cities: Oslo, Rome.").isError(true).build()
    return ToolResultBlockParam.builder().toolUseId(block.id()).content(answer).build()
}

/** The last reply and the whole transcript, as the requests carried it. */
data class Loop(val reply: Message, val messages: List<MessageParam>)

fun loop(client: AnthropicClient, question: String, choice: ToolChoice? = null): Loop {
    val messages = mutableListOf(MessageParam.builder().role(MessageParam.Role.USER).content(question).build())
    var active = choice
    while (true) {
        val request = MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(500).messages(messages.toList()).apply { TOOLS.forEach { addTool(it) } }
        active?.let { request.toolChoice(it) }
        val reply = client.messages().create(request.build())
        messages += reply.toParam()
        if (reply.stopReason().get().asString() != "tool_use") return Loop(reply, messages)
        val results = reply.content().filter { it.isToolUse() }.map { ContentBlockParam.ofToolResult(runTool(it.asToolUse())) }
        messages += MessageParam.builder().role(MessageParam.Role.USER).contentOfBlockParams(results).build()
        if (active != null && (active.isAny() || active.isTool())) active = null // a forced choice applies to the first request only; auto and none stay
    }
}

val REPLIES = listOf<Any>(
    message(
        listOf(text("Checking all three."), toolUse("toolu_01", "get_weather", map("city", "Oslo")), toolUse("toolu_02", "get_time", map("city", "Oslo")), toolUse("toolu_03", "get_weather", map("city", "Atlantis"))),
        "tool_use",
    ),
    message(listOf(text("In Oslo it is 09:15 and 4 C with light rain. I have no weather data for Atlantis."))),
)

fun main() {
    val rig = Scripted.client(*REPLIES.toTypedArray())
    val run = loop(rig.client(), "Weather and time in Oslo, and the weather in Atlantis?", ToolChoice.ofAuto(ToolChoiceAuto.builder().disableParallelToolUse(false).build()))
    println("model calls: ${rig.http().requests.size} | stop reasons: ${py(listOf("tool_use", run.reply.stopReason().get().asString()))}")
    println("roles after the first reply: ${py(run.messages.map { it.role().asString() })}")
    val results = run.messages[2].content().blockParams().get()
    println("tool results in ONE user message: ${results.size} | ids in order: ${py(results.map { it.asToolResult().toolUseId() })}")
    for (r in results) {
        val result = r.asToolResult()
        println("  ${result.toolUseId()}: is_error=${py(result.isError().orElse(false))} content=${py(result.content().get().string().get())}")
    }
    println("tool_choice sent on requests 1 and 2: ${py(rig.http().requests.map { it["tool_choice"] })}")
    println("tool definitions sent carry no handler: ${py(rig.http().requests[0]["tools"].all { t -> t.fieldNames().asSequence().toSet() == setOf("name", "description", "input_schema") })}")
    println("final text: ${run.reply.content()[0].asText().text()}")
}
