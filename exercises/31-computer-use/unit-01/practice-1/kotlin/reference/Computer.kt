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

/** The factor that shrinks a screen to what the model may be sent. */
fun scaleFor(width: Int, height: Int): Double = min(1.0, min(1568.0 / max(width, height), sqrt(1_150_000.0 / (width.toDouble() * height))))

/** The size of the screenshot the model is sent, as a pair of width and height. */
fun scaledSize(width: Int, height: Int): Pair<Int, Int> {
    val scale = scaleFor(width, height)
    return Pair((width * scale).toInt(), (height * scale).toInt())
}

/** A point on the (scaled) screenshot as a point on the real screen, clamped into the screen. */
fun toScreen(x: Double, y: Double, scale: Double, screen: Map<String, Any?>): Pair<Int, Int> =
    Pair(min(max(Math.rint(x / scale), 0.0), (num(screen["width"]) - 1).toDouble()).toInt(), min(max(Math.rint(y / scale), 0.0), (num(screen["height"]) - 1).toDouble()).toInt())

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

/** The error text when a click on this element needs a person and does not get one, else null. */
fun riskError(name: String, element: Map<String, Any?>?, confirm: Confirm?): String? {
    if (element == null || (element["risk"] ?: "none") == "none") return null
    if (confirm == null || !confirm(mapOf("action" to name, "element" to element["id"], "risk" to element["risk"]))) return "Declined: ${element["id"]} needs a person's confirmation (${element["risk"]})"
    return null
}

/** Is this zoom region four numbers inside the scaled screenshot, with x0 < x1 and y0 < y1? */
fun validRegion(shot: Pair<Int, Int>, r: Any?): Boolean =
    r is List<*> && r.size == 4 && inside(shot, r.subList(0, 2)) && r[2] is Number && r[3] is Number &&
        (r[2] as Number).toDouble() > 0 && (r[2] as Number).toDouble() <= shot.first && (r[3] as Number).toDouble() > 0 && (r[3] as Number).toDouble() <= shot.second &&
        (r[0] as Number).toDouble() < (r[2] as Number).toDouble() && (r[1] as Number).toDouble() < (r[3] as Number).toDouble()

/** Is this the input of a key press: a non-empty text and a repeat that is an integer from 1 to 100? */
fun validKey(args: Map<String, Any?>): Boolean {
    val repeat = args["repeat"] ?: 1
    return !(args["text"] as? String).isNullOrEmpty() && isInt(repeat) && num(repeat) >= 1 && num(repeat) <= 100
}

/** Is this the input of a scroll: a direction of up, down, left or right and an integer amount of at least 1? */
fun validScroll(args: Map<String, Any?>): Boolean =
    args["scroll_direction"] in listOf("up", "down", "left", "right") && isInt(args["scroll_amount"]) && num(args["scroll_amount"]) >= 1

/** How many of the oldest images to replace by a note: all but the newest keep (all of them when keep is 0). */
fun removeCount(total: Int, keep: Int): Int = if (keep > 0) max(total - keep, 0) else total

/** The status the loop ends with for a stop reason, or null when the loop goes on (pause_turn). */
fun finalStatus(stop: Any?): String? {
    if (stop == "pause_turn") return null
    if (stop == "refusal") return "refused"
    return if (stop == "end_turn" || stop == "stop_sequence") "done" else "truncated"
}

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
