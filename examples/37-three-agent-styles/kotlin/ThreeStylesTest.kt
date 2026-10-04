import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ThreeStylesTest {
    @Test
    fun theGraphFollowsItsEdgesAndOtherTopicsSkipTheLookup() {
        val m = Scripted("Billing", "reply")
        val result = runGraph(m, TICKET)
        assertEquals(listOf("classify", "lookup", "draft"), result.path)
        assertEquals("paid twice on 2026-09-30", result.state["invoice"])
        assertEquals(2, m.seen.size)
        val other = runGraph(Scripted("other", "hello"), "How do I log in?")
        assertEquals(listOf("classify", "draft"), other.path)
        assertFalse("invoice" in other.state)
    }

    @Test
    fun aGraphResumesFromACheckpointWithoutRepeatingEarlierNodes() {
        val first = runGraph(Scripted("billing", "the reply"), TICKET)
        val again = Scripted("the reply")
        val resumed = runGraph(again, TICKET, resumeFrom = 2, given = first.checkpoints.toMutableList())
        assertEquals(listOf("draft"), resumed.path)
        assertEquals(1, again.seen.size)
        assertEquals(first.state, resumed.state)
    }

    @Test
    fun theAgentLoopRunsTheToolTheModelPickedAndStopsAtTheStepLimit() {
        val m = Scripted("""{"tool": "lookup_invoice", "arg": "1042"}""", """{"final": "done"}""")
        assertEquals(AgentRun("done", listOf("lookup_invoice(1042) -> paid twice on 2026-09-30")), runAgent(m, TICKET))
        val endless = Scripted(*Array(4) { """{"tool": "lookup_invoice", "arg": "x"}""" })
        val result = runAgent(endless, TICKET, maxSteps = 4)
        assertNull(result.reply)
        assertEquals("max_steps", result.stopped)
        assertEquals(4, endless.seen.size)
    }

    @Test
    fun aTypedReplyIsValidatedAndAMismatchIsFedBackOnce() {
        assertEquals(mapOf("topic" to "x", "refund_cents" to 5), validate("""{"topic": "x", "refund_cents": 5}""").data)
        assertEquals("field refund_cents must be int", validate("""{"topic": "x", "refund_cents": true}""").error)
        assertEquals("the reply is not JSON", validate("nope").error)
        val m = Scripted("""{"topic": "billing", "refund_cents": "49"}""", """{"topic": "billing", "refund_cents": 49}""")
        val result = runTyped(m, TICKET)
        assertEquals(mapOf("topic" to "billing", "refund_cents" to 49), result.data)
        assertEquals(2, result.attempts)
        assertTrue("field refund_cents must be int" in m.seen[1])
        assertEquals(2, runTyped(Scripted("a", "b"), TICKET).attempts)
    }
}
