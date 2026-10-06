import harness.Scripted
import harness.Scripted.map
import harness.Scripted.message
import harness.Scripted.text
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ScreenLoopTest {
    @Test
    fun theScaleComesFromThePixelBudgetForALargeScreenAndClicksMapBack() {
        val scale = scaleFor(2560, 1440)
        assertEquals(0.5585, Math.round(scale * 10000) / 10000.0)
        assertEquals(1429, (2560 * scale).toInt())
        assertEquals(804, (1440 * scale).toInt())
        assertEquals(1.0, scaleFor(1280, 720))
        val screen = newScreen()
        assertEquals(0 to 0, toScreen(0.0, 0.0, scale, screen))
        assertEquals(2559 to 1439, toScreen(10_000.0, 10_000.0, scale, screen))
    }

    @Test
    fun aRiskyClickIsDeclinedUnlessAPersonSaysYes() {
        val screen = newScreen()
        val scale = scaleFor(2560, 1440)
        val click = map("coordinate", listOf(Math.rint(2150 * scale).toInt(), Math.rint(1240 * scale).toInt()))
        val declined = perform(screen, "left_click", click, scale, null)
        assertEquals("Declined: buy needs a person's confirmation (payment)", declined.content.string().get())
        assertTrue(declined.isError)
        assertFalse(perform(screen, "left_click", click, scale) { it["risk"] == "payment" }.isError)
        assertEquals(listOf(listOf("click", "buy")), screen.log)
    }

    @Test
    fun aFailureHaltsTheRestOfItsBatchWithTheDocumentedText() {
        val screen = newScreen()
        val rig = Scripted.client(
            message(listOf(call("t1", "left_click", map("coordinate", listOf(900, 900))), call("t2", "mystery"), call("t3", "type", map("text", "x"))), "tool_use"),
            message(listOf(text("ok"))),
        )
        assertEquals("done", runLoop(rig.client(), screen).status)
        val results = rig.http().requests[1]["messages"].last()["content"]
        assertEquals(listOf(false, true, true), results.map { it.path("is_error").asBoolean(false) })
        assertEquals(HALT, results[2]["content"].asText())
        assertEquals("", screen.typed.toString())
        assertTrue(results.all { it["toolset_name"].asText() == "computer" })
    }
}
