import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ComputerTest {
    private val notExecuted = "Not executed: an earlier computer action in this turn failed."

    private fun makeScreen(width: Int = 1920, height: Int = 1080): MutableMap<String, Any?> = linkedMapOf(
        "width" to width, "height" to height, "cursor" to mutableListOf<Any?>(0, 0), "typed" to "", "log" to mutableListOf<Any?>(),
        "elements" to listOf(mapOf("id" to "search", "x" to 1000, "y" to 200, "w" to 400, "h" to 40, "risk" to "none"), mapOf("id" to "pay", "x" to 800, "y" to 600, "w" to 200, "h" to 60, "risk" to "payment")))

    private fun use(id: String, name: String, vararg input: Pair<String, Any?>): Map<String, Any?> =
        mapOf("type" to "tool_use", "id" to id, "name" to name, "toolset_name" to "computer", "input" to linkedMapOf(*input))

    private fun reply(content: List<Any?>, stop: String): Map<String, Any?> = mapOf("content" to content, "stop_reason" to stop)
    private fun toolReply(vararg blocks: Map<String, Any?>) = reply(blocks.toList(), "tool_use")
    private fun says(text: String) = reply(listOf(mapOf("type" to "text", "text" to text)), "end_turn")

    private fun copy(v: Any?): Any? = when (v) {
        is Map<*, *> -> v.entries.associateTo(LinkedHashMap<String, Any?>()) { (it.key as String) to copy(it.value) }
        is List<*> -> v.mapTo(ArrayList<Any?>()) { copy(it) }
        else -> v
    }

    /** The model: returns the scripted replies in order and keeps a copy of every request. */
    private class Scripted(vararg replies: Map<String, Any?>, val say: (String) -> Map<String, Any?>) : (Map<String, Any?>) -> Map<String, Any?> {
        val replies = replies.toMutableList()
        val seen = mutableListOf<Map<String, Any?>>()

        @Suppress("UNCHECKED_CAST")
        override fun invoke(request: Map<String, Any?>): Map<String, Any?> {
            seen += Json.parse(Json.stringify(request)) as Map<String, Any?>
            return if (replies.isEmpty()) say("script ran out") else replies.removeAt(0)
        }
    }

    private fun scripted(vararg replies: Map<String, Any?>) = Scripted(*replies, say = { says(it) })

    @Suppress("UNCHECKED_CAST")
    private fun resultsOf(model: Scripted, index: Int): List<Map<String, Any?>> {
        if (model.seen.size <= index) return emptyList()
        val messages = model.seen[index]["messages"] as List<Map<String, Any?>>
        return messages.last()["content"] as List<Map<String, Any?>>
    }

    private fun run(screen: MutableMap<String, Any?>, name: String, vararg args: Pair<String, Any?>, scale: Double = scaleFor(1920, 1080)): Pair<Any?, Boolean> =
        perform(screen, name, linkedMapOf(*args), scale) ?: Pair("no result", false)

    @Suppress("UNCHECKED_CAST")
    private fun dataOf(o: Pair<Any?, Boolean>): String = try {
        (((o.first as List<Map<String, Any?>>)[0]["source"]) as Map<String, Any?>)["data"] as String
    } catch (e: RuntimeException) {
        "no image"
    }

    @Test
    fun m1_theLoopRunsScaledActionsAndSendsEveryResultInOneMessage() {
        val screen = makeScreen()
        val model = scripted(toolReply(use("t1", "screenshot")),
            toolReply(use("t2", "left_click", "coordinate" to listOf(894, 164)), use("t3", "type", "text" to "weather"), use("t4", "screenshot")), says("Typed it."))
        val result = runComputerLoop(model, screen) ?: emptyMap()
        assertEquals(listOf<Any?>("done", 3), listOf(result["status"], result["turns"]))
        assertEquals(listOf(listOf("left_click", "search"), listOf("type", "weather")), screen["log"])
        assertEquals("weather", screen["typed"])
        assertEquals(3, model.seen.size)
        for (r in model.seen) {
            assertEquals(listOf(mapOf("type" to "computer_toolset_20260801")), r["tools"])
            assertEquals("claude-sonnet-5-5", r["model"])
            assertEquals(4096, (r["max_tokens"] as Number).toInt())
        }
        assertEquals(listOf(mapOf("type" to "tool_result", "tool_use_id" to "t1", "toolset_name" to "computer",
            "content" to listOf(mapOf("type" to "image", "source" to mapOf("type" to "base64", "media_type" to "image/png", "data" to "png:1429x804:0"))))), resultsOf(model, 1))
        val last = resultsOf(model, 2)
        assertEquals(listOf<Any?>("t2", "t3", "t4"), last.map { it["tool_use_id"] })
        assertTrue(last.none { it.containsKey("is_error") })
        assertEquals(listOf<Any?>("Clicked search", "Typed 7 characters"), last.take(2).map { it["content"] })
        assertEquals(mapOf("type" to "image", "source" to mapOf("type" to "base64", "media_type" to "image/png", "data" to "png:1429x804:2")), (last[2]["content"] as List<*>)[0])
        @Suppress("UNCHECKED_CAST")
        val messages = model.seen[2]["messages"] as List<Map<String, Any?>>
        assertEquals(listOf<Any?>("user", "assistant", "user", "assistant", "user"), messages.map { it["role"] })
    }

    @Test
    fun e1_theScreenIsScaledToWhatTheModelMaySeeAndClicksAreScaledBack() {
        assertEquals(1.0, scaleFor(1280, 800))
        assertEquals(1.0, scaleFor(1024, 768))
        assertEquals(0.744709, scaleFor(1920, 1080), 1e-5)
        assertEquals(0.558531, scaleFor(2560, 1440), 1e-5)
        assertEquals(1568.0 / 3000, scaleFor(3000, 100), 1e-9)
        assertEquals(Pair(1429, 804), scaledSize(1920, 1080))
        assertEquals(Pair(1429, 804), scaledSize(2560, 1440))
        assertEquals(Pair(1024, 768), scaledSize(1024, 768))
        val screen = makeScreen()
        assertEquals(Pair(1200, 220), toScreen(894.0, 164.0, scaleFor(1920, 1080), screen))
        assertEquals(Pair(1919, 1079), toScreen(5000.0, 5000.0, 1.0, screen))
        assertEquals(Pair(0, 0), toScreen(-3.0, -3.0, 1.0, screen))
    }

    @Test
    fun e2_aClickOnARiskyElementNeedsAPersonsConfirmation() {
        val scale = scaleFor(1920, 1080)
        val asked = mutableListOf<Any?>()
        val yes: Confirm = { asked.add(it); true }
        for (confirm in listOf<Confirm?>(null, { false })) {
            val screen = makeScreen()
            val o = perform(screen, "left_click", mapOf("coordinate" to listOf(670, 469)), scale, confirm)
            assertTrue(o != null && o.second && o.first.toString().startsWith("Declined: pay needs a person's confirmation (payment)"))
            assertEquals(emptyList<Any?>(), screen["log"])
            assertEquals(listOf<Any?>(0, 0), screen["cursor"])
        }
        val screen = makeScreen()
        assertEquals(Pair<Any?, Boolean>("Clicked pay", false), perform(screen, "double_click", mapOf("coordinate" to listOf(670, 469)), scale, yes))
        assertEquals(listOf(listOf("double_click", "pay")), screen["log"])
        assertEquals(listOf<Any?>(mapOf("action" to "double_click", "element" to "pay", "risk" to "payment")), asked)
        asked.clear()
        assertEquals(Pair<Any?, Boolean>("Clicked search", false), perform(screen, "left_click", mapOf("coordinate" to listOf(894, 164)), scale, yes))
        assertEquals(0, asked.size)
        assertEquals(Pair<Any?, Boolean>("Clicked nothing", false), perform(screen, "left_click", mapOf("coordinate" to listOf(5, 5)), scale, yes))
    }

    @Test
    fun e3_aFailedActionStopsTheRestOfItsBatch() {
        val screen = makeScreen()
        val model = scripted(toolReply(use("a", "left_click", "coordinate" to listOf(894, 164)), use("b", "left_click", "coordinate" to listOf(9999, 5)), use("c", "type", "text" to "x"), use("d", "screenshot")), says("Stopped."))
        runComputerLoop(model, screen)
        val results = resultsOf(model, 1)
        assertEquals(listOf<Any?>("a", "b", "c", "d"), results.map { it["tool_use_id"] })
        assertEquals(listOf(false, true, true, true), results.map { it.containsKey("is_error") })
        assertTrue(results.getOrNull(1)?.get("content").toString().contains("outside the screenshot"))
        assertEquals(notExecuted, results.getOrNull(2)?.get("content"))
        assertEquals(notExecuted, results.getOrNull(3)?.get("content"))
        assertTrue(results.all { it["toolset_name"] == "computer" })
        assertEquals(listOf(listOf("left_click", "search")), screen["log"])
        assertEquals("", screen["typed"])
    }

    @Test
    fun e4_invalidActionsComeBackAsErrorResultsWithAReason() {
        val screen = makeScreen()
        assertEquals(Pair<Any?, Boolean>("Pressed ctrl+s", false), run(screen, "key", "text" to "ctrl+s", "repeat" to 2))
        assertTrue(run(screen, "key", "text" to "Return", "repeat" to 101).second && run(screen, "key", "repeat" to 1).second)
        assertEquals(Pair<Any?, Boolean>("Waited 2.5s", false), run(screen, "wait", "duration" to 2.5))
        assertTrue(run(screen, "wait", "duration" to 301).second)
        assertEquals(Pair<Any?, Boolean>("Scrolled down 3", false), run(screen, "scroll", "scroll_direction" to "down", "scroll_amount" to 3))
        assertTrue(run(screen, "scroll", "scroll_direction" to "sideways", "scroll_amount" to 3).second && run(screen, "scroll", "scroll_direction" to "up", "scroll_amount" to 0).second)
        assertEquals(Pair<Any?, Boolean>("Unknown action: teleport", true), run(screen, "teleport"))
        assertTrue(run(screen, "type").second && run(screen, "left_click", "coordinate" to listOf(5000, 5)).second)
        assertEquals(Pair<Any?, Boolean>("Moved", false), run(screen, "mouse_move", "coordinate" to listOf(670, 469)))
        assertEquals(Pair<Any?, Boolean>("X=670,Y=469", false), run(screen, "cursor_position"))
        assertTrue(run(screen, "zoom", "region" to listOf(0, 0, 5000, 5000)).second && run(screen, "zoom", "region" to listOf(300, 200, 100, 100)).second)
    }

    @Test
    fun e5_theLoopEndsOnAStopReasonOrAtTheTurnLimit() {
        val model = scripted(*Array(10) { toolReply(use("t$it", "screenshot")) })
        val result = runComputerLoop(model, makeScreen(), "claude-sonnet-5-5", 3) ?: emptyMap()
        assertEquals(listOf<Any?>("max_turns", 3, 3), listOf(result["status"], result["turns"], model.seen.size))
        assertEquals("refused", (runComputerLoop(scripted(reply(listOf(mapOf("type" to "text", "text" to "no")), "refusal")), makeScreen()) ?: emptyMap())["status"])
        assertEquals("truncated", (runComputerLoop(scripted(reply(listOf(mapOf("type" to "text", "text" to "cut")), "max_tokens")), makeScreen()) ?: emptyMap())["status"])
        val paused = scripted(reply(listOf(mapOf("type" to "text", "text" to "working")), "pause_turn"), says("done"))
        val again = runComputerLoop(paused, makeScreen()) ?: emptyMap()
        assertEquals(listOf<Any?>("done", 2), listOf(again["status"], again["turns"]))
        @Suppress("UNCHECKED_CAST")
        val second = if (paused.seen.size > 1) paused.seen[1]["messages"] as List<Map<String, Any?>> else emptyList()
        assertEquals(listOf<Any?>("user", "assistant"), second.map { it["role"] })
    }

    private fun shot(n: Int): Map<String, Any?> = mapOf("type" to "image", "source" to mapOf("type" to "base64", "media_type" to "image/png", "data" to "png:$n"))

    @Suppress("UNCHECKED_CAST")
    private fun contents(msgs: List<Map<String, Any?>>?): List<Any?> =
        (msgs ?: emptyList()).filter { it["role"] == "user" && it["content"] is List<*> }.map { ((it["content"] as List<Map<String, Any?>>)[0])["content"] }

    @Test
    fun e6_oldScreenshotsAreReplacedSoTheContextDoesNotFillUp() {
        val messages = mutableListOf<Map<String, Any?>>(mapOf("role" to "user", "content" to "task"))
        for (n in 0 until 5) {
            messages.add(mapOf("role" to "assistant", "content" to listOf(use("t$n", "screenshot"))))
            messages.add(mapOf("role" to "user", "content" to listOf(mapOf("type" to "tool_result", "tool_use_id" to "t$n", "toolset_name" to "computer", "content" to listOf(shot(n))))))
        }
        messages.add(mapOf("role" to "assistant", "content" to listOf(use("k", "type", "text" to "x"))))
        messages.add(mapOf("role" to "user", "content" to listOf(mapOf("type" to "tool_result", "tool_use_id" to "k", "content" to "Typed 1 characters"))))
        @Suppress("UNCHECKED_CAST")
        val before = copy(messages) as List<Map<String, Any?>>
        val pruned = pruneScreenshots(messages, 2)
        assertEquals(before, messages)
        val note = listOf(mapOf("type" to "text", "text" to "[screenshot removed]"))
        assertEquals(listOf<Any?>(note, note, note, listOf(shot(3)), listOf(shot(4)), "Typed 1 characters"), contents(pruned))
        assertTrue(pruned != null && pruned.size == messages.size && pruned[0] == messages[0])
        assertEquals(messages, pruneScreenshots(messages, 9))
        assertEquals(listOf<Any?>(note, note, note, note, note), contents(pruneScreenshots(messages, 0)).take(5))
    }

    @Test
    fun e7_screenshotsAndZoomsAreSentAtTheSizeTheModelMaySee() {
        for ((w, h, size) in listOf(Triple(1920, 1080, "1429x804"), Triple(2560, 1440, "1429x804"), Triple(1280, 800, "1280x800"))) {
            val o = run(makeScreen(w, h), "screenshot", scale = scaleFor(w, h))
            assertFalse(o.second)
            assertEquals("png:$size:0", dataOf(o))
        }
        val screen = makeScreen()
        val zoom = run(screen, "zoom", "region" to listOf(0, 0, 300, 200))
        assertFalse(zoom.second)
        assertEquals("png:403x269:0", dataOf(zoom))
        run(screen, "left_click", "coordinate" to listOf(5, 5))
        assertEquals("png:1429x804:1", dataOf(run(screen, "screenshot")))
    }
}
