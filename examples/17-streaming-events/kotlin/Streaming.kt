import com.anthropic.core.JsonValue
import com.anthropic.core.jsonMapper
import com.anthropic.errors.SseException
import com.anthropic.helpers.MessageAccumulator
import com.anthropic.client.AnthropicClient
import com.anthropic.models.messages.ContentBlock
import com.anthropic.models.messages.Message
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.Model
import com.anthropic.models.messages.RawMessageStreamEvent
import com.anthropic.models.messages.Tool
import harness.Reply
import harness.Scripted
import harness.Scripted.map

/**
 * A streamed reply read three ways, from a scripted server-sent-event body.
 *
 * The stream is an illustrative, hand-written sequence of events in the API's framing
 * (claude-sonnet-5-5): one text block, then one tool_use block whose input arrives in fragments.
 * The Java SDK (used from Kotlin) reads the events itself and drops `ping` events, so a raw event list has no `ping` in it.
 */
const val MODEL = "claude-sonnet-5-5"

fun params(): MessageCreateParams {
    val weather = Tool.builder().name("get_weather").description("Weather for a city.")
        .inputSchema(Tool.InputSchema.builder().properties(JsonValue.from(map("city", map("type", "string")))).required(listOf("city")).build()).build()
    return MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(128).addUserMessage("Weather in Paris?").addTool(weather).build()
}

fun delta(index: Int, kind: String, key: String, value: String) = map("type", "content_block_delta", "index", index, "delta", map("type", kind, key, value))

val EVENTS = listOf(
    map(
        "type", "message_start",
        "message", map("id", "msg_illustrative", "type", "message", "role", "assistant", "model", MODEL, "content", listOf<Any>(), "stop_reason", null, "stop_sequence", null, "usage", map("input_tokens", 52, "output_tokens", 1)),
    ),
    map("type", "content_block_start", "index", 0, "content_block", map("type", "text", "text", "")),
    map("type", "ping"),
    delta(0, "text_delta", "text", "Let me "),
    delta(0, "text_delta", "text", "check."),
    map("type", "content_block_stop", "index", 0),
    map("type", "content_block_start", "index", 1, "content_block", map("type", "tool_use", "id", "toolu_illustrative_1", "name", "get_weather", "input", map())),
    delta(1, "input_json_delta", "partial_json", ""),
    delta(1, "input_json_delta", "partial_json", "{\"ci"),
    delta(1, "input_json_delta", "partial_json", "ty\": \"Par"),
    delta(1, "input_json_delta", "partial_json", "is\"}"),
    map("type", "content_block_stop", "index", 1),
    map("type", "message_delta", "delta", map("stop_reason", "tool_use", "stop_sequence", null), "usage", map("output_tokens", 38)),
    map("type", "message_stop"),
)
val FAILING = EVENTS.take(5) + map("type", "error", "error", map("type", "overloaded_error", "message", "Overloaded"))

/** The pieces of a streamed reply: the text deltas, and the message the SDK's accumulator assembled from all the events. */
data class Streamed(val textPieces: List<String>, val message: Message)

/** The name of a raw stream event, as it is on the wire. */
fun typeName(e: RawMessageStreamEvent) = when {
    e.isMessageStart() -> "message_start"
    e.isMessageDelta() -> "message_delta"
    e.isMessageStop() -> "message_stop"
    e.isContentBlockStart() -> "content_block_start"
    e.isContentBlockDelta() -> "content_block_delta"
    else -> "content_block_stop"
}

fun readText(client: AnthropicClient): Streamed {
    val accumulator = MessageAccumulator.create()
    val pieces = mutableListOf<String>()
    client.messages().createStreaming(params()).use { stream ->
        stream.stream().forEach { event ->
            accumulator.accumulate(event)
            event.contentBlockDelta().flatMap { it.delta().text() }.ifPresent { pieces += it.text() }
        }
    }
    return Streamed(pieces, accumulator.message())
}

fun rawEventNames(client: AnthropicClient): List<String> = client.messages().createStreaming(params()).use { stream -> stream.stream().map(::typeName).toList() }

/** ['a', 'b', 'b'] becomes "a, b x2". */
fun runLength(names: List<String>): String {
    val parts = mutableListOf<String>()
    var i = 0
    while (i < names.size) {
        var j = i
        while (j < names.size && names[j] == names[i]) j++
        parts += if (j - i == 1) names[i] else "${names[i]} x${j - i}"
        i = j
    }
    return parts.joinToString(", ")
}

fun clientFor(events: List<Map<String, Any?>>) = Scripted.client(Reply.sse(events))

/** The tool input the stream assembled, as a map. */
fun toolInput(block: ContentBlock): Map<*, *> = jsonMapper().convertValue(block.asToolUse()._input(), Map::class.java)

private fun py(v: Any?): String = when (v) {
    is String -> "'$v'"
    is Map<*, *> -> v.entries.joinToString(", ", "{", "}") { "${py(it.key)}: ${py(it.value)}" }
    is List<*> -> v.joinToString(", ", "[", "]") { py(it) }
    else -> v.toString()
}

fun main() {
    val rig = clientFor(EVENTS)
    val streamed = readText(rig.client())
    println("request sets stream: ${if (rig.http().requests[0]["stream"].asBoolean()) "True" else "False"}")
    println("text pieces: ${py(streamed.textPieces)}")
    val message = streamed.message
    println("final stop_reason: ${message.stopReason().get().asString()} | usage: ${message.usage().inputTokens()} in, ${message.usage().outputTokens()} out")
    println("blocks: ${py(message.content().map { if (it.isText()) "text" else "tool_use" })} | tool input: ${py(toolInput(message.content()[1]))}")

    println("raw events: ${runLength(rawEventNames(clientFor(EVENTS).client()))}")

    try {
        readText(clientFor(FAILING).client())
    } catch (err: SseException) {
        println("mid-stream error: ${err.javaClass.simpleName} ${err.errorType().map { it.asString() }.orElse("?")}")
    }
}
