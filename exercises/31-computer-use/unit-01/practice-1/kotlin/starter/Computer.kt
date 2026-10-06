import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

private val log = System.getLogger("computer")

/** The loop around the computer use tool, against a toy screen. See ../../statement.md. Screens, messages and replies are JSON-like maps. */

const val TOOLSET = "computer_toolset_20260801"
val CLICKS = listOf("left_click", "right_click", "middle_click", "double_click", "triple_click")
const val NOT_EXECUTED = "Not executed: an earlier computer action in this turn failed."

typealias Ask = (Map<String, Any?>) -> Map<String, Any?>
typealias Confirm = (Map<String, Any?>) -> Boolean

private fun num(o: Any?) = (o as Number).toInt()

@Suppress("UNCHECKED_CAST")
private fun actionLog(screen: Map<String, Any?>) = screen["log"] as MutableList<Any?>

/** Given: the screenshot of the toy screen at width x height, as the base64 text of a picture. It changes after every logged action. */
fun render(screen: Map<String, Any?>, width: Int, height: Int): String = "png:${width}x$height:${actionLog(screen).size}"

/**
 * TODO 1 of 9 (unlocks e1, e7 and m1): the factor that shrinks a screen to what the model may be sent.
 * Receives the real width and height. Returns min(1, 1568 / longest side, sqrt(1,150,000 / (width * height))).
 * Example: scaleFor(1280, 800) is 1.0, scaleFor(1920, 1080) is 0.7447...
 */
fun scaleFor(width: Int, height: Int): Double = 1.0

/**
 * TODO 2 of 9 (unlocks e7 and m1): the size of the screenshot the model is sent, as a pair of width and height.
 * Receives the real width and height. Returns each side times scaleFor, cut to a whole number with toInt().
 * Example: scaledSize(1920, 1080) is Pair(1429, 804)
 */
fun scaledSize(width: Int, height: Int): Pair<Int, Int> = Pair(width, height)

/**
 * TODO 3 of 9 (unlocks e1, e2 and m1): a point on the (scaled) screenshot as a point on the real screen.
 * Receives x and y on the screenshot, the scale and the screen. Returns Pair(x, y) on the screen: divide by the scale, round half to even
 * (Math.rint), and clamp into 0 .. width - 1 and 0 .. height - 1.
 * Example: toScreen(894.0, 164.0, 0.7447, screen) is Pair(1200, 220); toScreen(-3.0, -3.0, 1.0, screen) is Pair(0, 0)
 */
fun toScreen(x: Double, y: Double, scale: Double, screen: Map<String, Any?>): Pair<Int, Int> = Pair(0, 0)

@Suppress("UNCHECKED_CAST")
private fun elementAt(screen: Map<String, Any?>, x: Int, y: Int): Map<String, Any?>? {
    var hit: Map<String, Any?>? = null
    for (e in screen["elements"] as List<Map<String, Any?>>) {
        if (num(e["x"]) <= x && x < num(e["x"]) + num(e["w"]) && num(e["y"]) <= y && y < num(e["y"]) + num(e["h"])) hit = e
    }
    return hit
}

private fun inside(shot: Pair<Int, Int>, point: Any?): Boolean {
    if (point !is List<*> || point.size != 2) return false
    val a = point[0] as? Number ?: return false
    val b = point[1] as? Number ?: return false
    return a.toDouble() >= 0 && a.toDouble() < shot.first && b.toDouble() >= 0 && b.toDouble() < shot.second
}

private fun image(data: String): Pair<Any?, Boolean> = Pair(listOf(mapOf("type" to "image", "source" to mapOf("type" to "base64", "media_type" to "image/png", "data" to data))), false)

private fun isInt(o: Any?) = o is Int || o is Long

private fun pt(p: Any?): Pair<Double, Double> = Pair((p as List<*>)[0].let { (it as Number).toDouble() }, p[1].let { (it as Number).toDouble() })

/**
 * TODO 4 of 9 (unlocks e2): the error text when a click on this element needs a person and does not get one, else null.
 * Receives the action name, the element under the click (a map with "id" and "risk", or null) and confirm (a function or null). A risk
 * other than "none" is asked of confirm with mapOf("action", "element", "risk"); with no confirm or an answer of false return
 * "Declined: <id> needs a person's confirmation (<risk>)". Example: a "payment" element "pay", confirm null: "Declined: pay needs a person's confirmation (payment)"
 */
fun riskError(name: String, element: Map<String, Any?>?, confirm: Confirm?): String? {
    return null
}

/**
 * TODO 5 of 9 (unlocks e4): is this zoom region valid?
 * Receives the scaled screenshot size as a pair and the region. True for a list of four numbers [x0, y0, x1, y1] where x0, y0 is inside the
 * screenshot (inside), x1 and y1 are above 0 and within the size, x0 < x1 and y0 < y1. Example: Pair(1429, 804), [0, 0, 300, 200] is true
 */
fun validRegion(shot: Pair<Int, Int>, r: Any?): Boolean = true

/**
 * TODO 6 of 9 (unlocks e4): is this the input of a key press?
 * Receives the input map. True when "text" is a non-empty string and "repeat" (default 1) is an integer from 1 to 100 (isInt and num are written).
 * Example: mapOf("text" to "ctrl+s", "repeat" to 2) is true, mapOf("text" to "Return", "repeat" to 101) is false
 */
fun validKey(args: Map<String, Any?>): Boolean = true

/**
 * TODO 7 of 9 (unlocks e4): is this the input of a scroll?
 * Receives the input map. True when "scroll_direction" is up, down, left or right and "scroll_amount" is an integer of at least 1.
 * Example: mapOf("scroll_direction" to "down", "scroll_amount" to 3) is true, with an amount of 0 it is false
 */
fun validScroll(args: Map<String, Any?>): Boolean = true

/**
 * TODO 8 of 9 (unlocks e6): how many of the oldest images to replace by a note.
 * Receives the number of images and keep. Returns all but the newest keep, and all of them when keep is 0.
 * Example: removeCount(3, 1) is 2, removeCount(3, 0) is 3, removeCount(3, 9) is 0
 */
fun removeCount(total: Int, keep: Int): Int = 0

/**
 * TODO 9 of 9 (unlocks e5): the status the loop ends with for a stop reason, or null when it goes on.
 * Receives the stop reason of a reply that is not tool_use. Returns "refused" for refusal, "done" for end_turn and stop_sequence,
 * null for pause_turn (call again), and "truncated" for anything else. Example: "max_tokens" gives "truncated"
 */
fun finalStatus(stop: Any?): String? = null

/** Run one action. Returns the content for the result and whether it failed. */
@Suppress("UNCHECKED_CAST")
fun perform(screen: MutableMap<String, Any?>, name: String, args: Map<String, Any?>, scale: Double, confirm: Confirm? = null): Pair<Any?, Boolean> {
    log.log(System.Logger.Level.DEBUG, "perform input: {0}", "$name $args")
    val shot = scaledSize(num(screen["width"]), num(screen["height"]))
    if (name == "screenshot") return image(render(screen, shot.first, shot.second))
    if (name == "zoom") {
        val r = args["region"]
        if (!validRegion(shot, r)) return Pair("Invalid zoom region: $r", true)
        val l = r as List<*>
        val a = toScreen((l[0] as Number).toDouble(), (l[1] as Number).toDouble(), scale, screen)
        val b = toScreen((l[2] as Number).toDouble(), (l[3] as Number).toDouble(), scale, screen)
        return image(render(screen, b.first - a.first, b.second - a.second))
    }
    if (name in CLICKS) {
        val point = args["coordinate"]
        val sx: Int
        val sy: Int
        if (point == null) {
            val cursor = screen["cursor"] as List<Any?>
            sx = num(cursor[0])
            sy = num(cursor[1])
        } else if (!inside(shot, point)) {
            return Pair("Coordinate $point is outside the screenshot", true)
        } else {
            val (px, py) = pt(point)
            val s = toScreen(px, py, scale, screen)
            sx = s.first
            sy = s.second
        }
        val element = elementAt(screen, sx, sy)
        val declined = riskError(name, element, confirm)
        if (declined != null) return Pair(declined, true)
        val cursor = screen["cursor"] as MutableList<Any?>
        cursor[0] = sx
        cursor[1] = sy
        actionLog(screen).add(listOf(name, element?.get("id")))
        return Pair("Clicked ${element?.get("id") ?: "nothing"}", false)
    }
    when (name) {
        "type" -> {
            val text = args["text"] as? String ?: return Pair("type needs a text", true)
            screen["typed"] = (screen["typed"] as String) + text
            actionLog(screen).add(listOf("type", text))
            return Pair("Typed ${text.codePointCount(0, text.length)} characters", false)
        }
        "key" -> {
            if (!validKey(args)) return Pair("key needs a text and a repeat from 1 to 100", true)
            val text = args["text"] as String
            actionLog(screen).add(listOf("key", text))
            return Pair("Pressed $text", false)
        }
        "wait" -> {
            val d = args["duration"]
            if (d !is Number || d.toDouble() < 0 || d.toDouble() > 300) return Pair("wait needs a duration from 0 to 300 seconds", true)
            return Pair("Waited ${if (isInt(d)) d.toLong().toString() else d.toDouble().toString()}s", false)
        }
        "scroll" -> {
            val direction = args["scroll_direction"]
            val amount = args["scroll_amount"]
            if (!validScroll(args)) return Pair("scroll needs a direction and a positive amount", true)
            actionLog(screen).add(listOf("scroll", direction))
            return Pair("Scrolled $direction $amount", false)
        }
        "mouse_move" -> {
            if (!inside(shot, args["coordinate"])) return Pair("Coordinate ${args["coordinate"]} is outside the screenshot", true)
            val (px, py) = pt(args["coordinate"])
            val s = toScreen(px, py, scale, screen)
            val cursor = screen["cursor"] as MutableList<Any?>
            cursor[0] = s.first
            cursor[1] = s.second
            return Pair("Moved", false)
        }
        "cursor_position" -> {
            val cursor = screen["cursor"] as List<Any?>
            return Pair("X=${Math.rint(num(cursor[0]) * scale).toLong()},Y=${Math.rint(num(cursor[1]) * scale).toLong()}", false)
        }
        else -> return Pair("Unknown action: $name", true)
    }
}

private fun deepCopy(v: Any?): Any? = when (v) {
    is Map<*, *> -> v.entries.associateTo(LinkedHashMap<String, Any?>()) { (it.key as String) to deepCopy(it.value) }
    is List<*> -> v.mapTo(ArrayList<Any?>()) { deepCopy(it) }
    else -> v
}

/** A copy of the conversation in which every screenshot except the newest keep is replaced by a text note. */
@Suppress("UNCHECKED_CAST")
fun pruneScreenshots(messages: List<Map<String, Any?>>, keep: Int = 3): List<Map<String, Any?>> {
    val out = deepCopy(messages) as List<Map<String, Any?>>
    val images = mutableListOf<Pair<MutableList<Any?>, Int>>()
    for (m in out) {
        val blocks = m["content"] as? List<Map<String, Any?>> ?: continue
        for (block in blocks) {
            val inner = block["content"] as? MutableList<Any?> ?: continue
            if (block["type"] != "tool_result") continue
            inner.forEachIndexed { i, c -> if ((c as Map<String, Any?>)["type"] == "image") images.add(Pair(inner, i)) }
        }
    }
    val upTo = removeCount(images.size, keep)
    for ((list, i) in images.take(upTo)) list[i] = mapOf("type" to "text", "text" to "[screenshot removed]")
    return out
}

@Suppress("UNCHECKED_CAST")
fun runComputerLoop(ask: Ask, screen: MutableMap<String, Any?>, model: String = "claude-sonnet-5-5", maxTurns: Int = 10, confirm: Confirm? = null): Map<String, Any?> {
    val scale = scaleFor(num(screen["width"]), num(screen["height"]))
    val messages = mutableListOf<Map<String, Any?>>(mapOf("role" to "user", "content" to "Do the task on the screen."))
    for (turn in 1..maxTurns) {
        val reply = ask(mapOf("model" to model, "max_tokens" to 4096, "tools" to listOf(mapOf("type" to TOOLSET)), "messages" to deepCopy(messages)))
        val content = reply["content"] as List<Map<String, Any?>>
        messages.add(mapOf("role" to "assistant", "content" to content))
        val stop = reply["stop_reason"]
        if (stop == "tool_use") {
            val results = mutableListOf<Map<String, Any?>>()
            var failed = false
            for (block in content) {
                if (block["type"] != "tool_use") continue
                val result = linkedMapOf<String, Any?>("type" to "tool_result", "tool_use_id" to block["id"])
                if (block.containsKey("toolset_name")) result["toolset_name"] = block["toolset_name"]
                if (failed) {
                    result["content"] = NOT_EXECUTED
                    result["is_error"] = true
                } else {
                    val (out, isError) = perform(screen, block["name"] as String, block["input"] as Map<String, Any?>, scale, confirm)
                    result["content"] = out
                    if (isError) {
                        result["is_error"] = true
                        failed = true
                    }
                }
                results.add(result)
            }
            messages.add(mapOf("role" to "user", "content" to results))
        } else if (finalStatus(stop) != null) {
            return mapOf("status" to finalStatus(stop), "turns" to turn, "messages" to messages)
        }
    }
    return mapOf("status" to "max_turns", "turns" to maxTurns, "messages" to messages)
}
