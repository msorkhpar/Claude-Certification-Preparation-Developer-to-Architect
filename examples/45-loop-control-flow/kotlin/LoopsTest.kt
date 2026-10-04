import harness.Scripted
import harness.Scripted.message
import harness.Scripted.text
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class LoopsTest {
    @Test
    fun theStopReasonLoopRunsTheToolOfAReplyThatSaysDone() {
        assertEquals(Outcome("done", 2, listOf("save_file"), "Saved report.txt."), byStopReason(Scripted.client(*scenarioA()).client(), "t"))
    }

    @Test
    fun theTextMarkerLoopStopsBeforeTheToolRuns() {
        val o = byTextMarker(Scripted.client(*scenarioA()).client(), "t")
        assertEquals(Triple("done", 1, emptyList<String>()), Triple(o.status, o.calls, o.ran))
    }

    @Test
    fun aBackstopBelowTheRealNeedGetsItsOwnStatus() {
        val o = byStopReason(Scripted.client(*scenarioB()).client(), "t", backstop = 3)
        assertEquals(Triple("max_turns", 3, 3), Triple(o.status, o.calls, o.ran.size))
    }

    @Test
    fun aBackstopAboveTheNeedIsNeverReached() {
        val o = byStopReason(Scripted.client(*scenarioB()).client(), "t", backstop = 10)
        assertEquals("done" to 5, o.status to o.calls)
    }

    @Test
    fun theFixedCountLoopReportsDoneWithoutAFinalAnswer() {
        val o = byFixedCount(Scripted.client(*scenarioB()).client(), "t")
        assertEquals(Triple("done", 3, ""), Triple(o.status, o.ran.size, o.answer))
    }

    @Test
    fun anEndTurnThatAnnouncesAToolCallIsStillTheEnd() {
        val rig = Scripted.client(message(listOf(text("Let me call the lookup tool next."))))
        val o = byStopReason(rig.client(), "t")
        assertEquals("done" to 1, o.status to o.calls)
        assertEquals(1, rig.http().requests.size)
    }
}
