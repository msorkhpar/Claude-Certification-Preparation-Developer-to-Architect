/** The loop around the computer use tool, against a toy screen. See ../../statement.md. Screens, messages and replies are JSON-like maps. */

const val TOOLSET = "computer_toolset_20260801"
val CLICKS = listOf("left_click", "right_click", "middle_click", "double_click", "triple_click")
const val NOT_EXECUTED = "Not executed: an earlier computer action in this turn failed."

typealias Ask = (Map<String, Any?>) -> Map<String, Any?>
typealias Confirm = (Map<String, Any?>) -> Boolean

private fun num(o: Any?) = (o as Number).toInt()

@Suppress("UNCHECKED_CAST")
private fun log(screen: Map<String, Any?>) = screen["log"] as MutableList<Any?>

/** Given: the screenshot of the toy screen at width x height, as the base64 text of a picture. It changes after every logged action. */
fun render(screen: Map<String, Any?>, width: Int, height: Int): String = "png:${width}x$height:${log(screen).size}"

fun scaleFor(width: Int, height: Int): Double {
    // TODO: the factor that shrinks a screen to what the model may be sent: min(1, 1568 / longest side, sqrt(1,150,000 / pixels)).
    return 0.0
}

fun scaledSize(width: Int, height: Int): Pair<Int, Int>? {
    // TODO: ((width * scale).toInt(), (height * scale).toInt()).
    return null
}

fun toScreen(x: Double, y: Double, scale: Double, screen: Map<String, Any?>): Pair<Int, Int>? {
    // TODO: a point on the scaled screenshot as a point on the real screen: divide by the scale, round (half to even), clamp into the screen.
    return null
}

fun perform(screen: MutableMap<String, Any?>, name: String, args: Map<String, Any?>, scale: Double, confirm: Confirm? = null): Pair<Any?, Boolean>? {
    // TODO: run one action on the toy screen and return (content, is_error). See the statement for every action.
    return null
}

fun pruneScreenshots(messages: List<Map<String, Any?>>, keep: Int = 3): List<Map<String, Any?>>? {
    // TODO: a copy of the conversation in which every screenshot except the newest keep is replaced by a text note.
    return null
}

fun runComputerLoop(ask: Ask, screen: MutableMap<String, Any?>, model: String = "claude-sonnet-5-5", maxTurns: Int = 10, confirm: Confirm? = null): Map<String, Any?>? {
    // TODO: call the model with the computer toolset, run its actions, send the results back, until it ends or a limit is hit.
    return null
}
