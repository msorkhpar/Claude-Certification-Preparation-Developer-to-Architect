import com.anthropic.client.AnthropicClient
import com.anthropic.core.jsonMapper
import com.anthropic.models.messages.Base64ImageSource
import com.anthropic.models.messages.ComputerToolset20260801
import com.anthropic.models.messages.ContentBlockParam
import com.anthropic.models.messages.ImageBlockParam
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.MessageParam
import com.anthropic.models.messages.Model
import com.anthropic.models.messages.ToolResultBlockParam
import com.anthropic.models.messages.ToolUnion
import harness.Scripted
import harness.Scripted.map
import harness.Scripted.message
import harness.Scripted.text
import java.util.Base64
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * The loop around the computer use toolset, on a toy screen: scaling both ways, a batch, a halt after a failure and a confirmation.
 *
 * There is no desktop and no model: the screen is a few rectangles in memory and the replies are hand-written bodies in the shape of the
 * Messages API (claude-sonnet-5-5), illustrative and not captures. The tool entry, the batch rule and the halt text are those of the
 * "Computer use tool" page of the Claude documentation, checked on 2026-10-03.
 */
const val MODEL = "claude-sonnet-5-5"
const val HALT = "Not executed: an earlier computer action in this turn failed."

/** A rectangle on the toy screen; `risk` is "none" or what a click on it could cost. */
data class Element(val id: String, val x: Int, val y: Int, val w: Int, val h: Int, val risk: String)

/** The toy screen: its size, what was typed, a log of the actions performed and the elements on it. */
class Screen(val width: Int, val height: Int) {
    val typed = StringBuilder()
    val log = mutableListOf<List<String?>>()
    val elements = listOf(Element("search", 800, 100, 900, 60, "none"), Element("buy", 2000, 1200, 300, 80, "payment"))
}

fun newScreen() = Screen(2560, 1440)

/** The documentation's example limits (1568 px on the long edge, about 1.15 megapixels): small enough for every model. */
fun scaleFor(width: Int, height: Int): Double = min(1.0, min(1568.0 / max(width, height), sqrt(1_150_000.0 / (width.toDouble() * height))))

fun toScreen(x: Double, y: Double, scale: Double, screen: Screen) =
    min(max(Math.rint(x / scale), 0.0), (screen.width - 1).toDouble()).toInt() to min(max(Math.rint(y / scale), 0.0), (screen.height - 1).toDouble()).toInt()

fun elementAt(screen: Screen, x: Int, y: Int): Element? = screen.elements.lastOrNull { it.x <= x && x < it.x + it.w && it.y <= y && y < it.y + it.h }

fun screenshot(screen: Screen, scale: Double): ToolResultBlockParam.Content {
    val size = "${(screen.width * scale).toInt()}x${(screen.height * scale).toInt()}"
    val data = Base64.getEncoder().encodeToString("$size:${screen.log.size}".toByteArray())
    val image = ImageBlockParam.builder().source(Base64ImageSource.builder().data(data).mediaType(Base64ImageSource.MediaType.IMAGE_PNG).build()).build()
    return ToolResultBlockParam.Content.ofBlocks(listOf(ToolResultBlockParam.Content.Block.ofImage(image)))
}

/** What one action gave back: text or an image, and whether it failed. */
data class Outcome(val content: ToolResultBlockParam.Content, val isError: Boolean)

private fun words(s: String, isError: Boolean) = Outcome(ToolResultBlockParam.Content.ofString(s), isError)

/** Run one member of the toolset on the toy screen. */
fun perform(screen: Screen, name: String, args: Map<*, *>, scale: Double, confirm: ((Map<String, String>) -> Boolean)?): Outcome = when (name) {
    "screenshot" -> Outcome(screenshot(screen, scale), false)
    "left_click" -> {
        val (cx, cy) = (args["coordinate"] as List<*>).map { (it as Number).toDouble() }
        val (x, y) = toScreen(cx, cy, scale, screen)
        val target = elementAt(screen, x, y)
        if (target != null && target.risk != "none" && !(confirm != null && confirm(mapOf("action" to name, "element" to target.id, "risk" to target.risk)))) {
            words("Declined: ${target.id} needs a person's confirmation (${target.risk})", true)
        } else {
            screen.log += listOf("click", target?.id)
            words("Clicked ${target?.id ?: "nothing"} at $x,$y on the screen", false)
        }
    }
    "type" -> {
        screen.typed.append(args["text"] as String)
        screen.log += listOf("type", args["text"] as String)
        words("Typed ${(args["text"] as String).length} characters", false)
    }
    "key" -> {
        screen.log += listOf("key", args["text"] as String)
        words("Pressed ${args["text"]}", false)
    }
    else -> words("Unknown action: $name", true)
}

/** How a loop ended. */
data class Result(val status: String, val turns: Int, val answer: String?)

private fun json(value: Any?): String = jsonMapper().writeValueAsString(value)

fun runLoop(client: AnthropicClient, screen: Screen, confirm: ((Map<String, String>) -> Boolean)? = null, maxTurns: Int = 6): Result {
    val scale = scaleFor(screen.width, screen.height)
    val messages = mutableListOf(MessageParam.builder().role(MessageParam.Role.USER).content("Search for a kettle and buy the first one.").build())
    val toolset = ToolUnion.ofComputerToolset20260801(ComputerToolset20260801.builder().build())
    for (turn in 1..maxTurns) {
        val reply = client.messages().create(MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(4096).addTool(toolset).messages(messages.toList()).build())
        messages += reply.toParam()
        if (reply.stopReason().get().asString() != "tool_use") return Result("done", turn, reply.content().last().asText().text())
        val results = mutableListOf<ContentBlockParam>()
        var failed = false
        for (block in reply.content().filter { it.isToolUse() }.map { it.asToolUse() }) {
            val outcome = if (failed) words(HALT, true) else perform(screen, block.name(), jsonMapper().convertValue(block._input(), Map::class.java), scale, confirm)
            failed = outcome.isError
            println("turn $turn: ${block.name()} ${json(block._input())} -> ${if (outcome.isError) "ERROR " else ""}${outcome.content.string().orElse("image")}")
            val result = ToolResultBlockParam.builder().toolUseId(block.id()).content(outcome.content).toolsetName(block.toolsetName().get())
            if (outcome.isError) result.isError(true)
            results += ContentBlockParam.ofToolResult(result.build())
        }
        messages += MessageParam.builder().role(MessageParam.Role.USER).contentOfBlockParams(results).build()
    }
    return Result("max_turns", maxTurns, null)
}

fun call(id: String, name: String, input: Map<String, Any?> = map()) = map("type", "tool_use", "id", id, "name", name, "toolset_name", "computer", "input", input)

fun main() {
    val screen = newScreen()
    val scale = scaleFor(screen.width, screen.height)
    fun point(e: Element) = listOf(Math.rint((e.x + e.w / 2.0) * scale).toInt(), Math.rint((e.y + e.h / 2.0) * scale).toInt()) // what the model would see
    val (search, buy) = screen.elements
    val rig = Scripted.client(
        message(listOf(call("toolu_1", "screenshot"), call("toolu_2", "left_click", map("coordinate", point(search))), call("toolu_3", "type", map("text", "kettle")), call("toolu_4", "key", map("text", "Return"))), "tool_use"),
        message(listOf(call("toolu_5", "left_click", map("coordinate", point(buy))), call("toolu_6", "screenshot")), "tool_use"),
        message(listOf(text("I did not buy it: the payment button needs your confirmation."))),
    )
    println("screen ${screen.width}x${screen.height}, scale ${"%.4f".format(scale)}, the model sees ${(screen.width * scale).toInt()}x${(screen.height * scale).toInt()}")
    val result = runLoop(rig.client(), screen, confirm = { false })
    println("result: ${result.status} after ${result.turns} turns: ${result.answer}")
    println("screen log: ${json(screen.log)} typed: ${screen.typed}")
    println("tools sent: ${rig.http().requests[0]["tools"]} beta header sent: ${if ("anthropic-beta" in rig.http().headers[0]) "True" else "False"}")
    val last = rig.http().requests[2]["messages"].last()["content"]
    println("second result message: ${last.map { "('${it["tool_use_id"].asText()}', '${it["toolset_name"].asText()}', ${if (it.path("is_error").asBoolean(false)) "True" else "False"})" }}")
}
