import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TraceTriageTest {
    private fun trace(name: String): List<Span> {
        val spans = TRACES[name]
        assertNotNull(spans)
        return spans ?: listOf()
    }

    @Test
    fun m1_errorsAndSlowTracesAreAlwaysKept() {
        assertEquals("error", keepReason("t-refund", trace("t-refund"), 0))
        assertEquals("slow", keepReason("x", listOf(Span("s1", "", "agent", "a", "ok", 6000, "")), 0))
    }

    @Test
    fun healthyTracesAreKeptByIdAndNotByChance() {
        val ids = (0 until 100).map { "trace-$it" }
        val first = ids.map { keepReason(it, trace("t-plain"), 10) }
        assertEquals(first, ids.map { keepReason(it, trace("t-plain"), 10) })
        val sampled = first.count { it == "sampled" }
        assertTrue(sampled in 1..99)
        assertEquals("dropped", keepReason("trace-1", trace("t-plain"), 0))
    }

    @Test
    fun rootCauseIsTheDeepestFailingSpan() {
        val c = rootCause(trace("t-refund"))
        assertEquals("tool", c.layer)
        assertEquals("web_fetch", c.name)
        assertEquals(listOf("orchestrator", "order-researcher", "web_fetch"), c.path)
    }

    @Test
    fun aStaleRetrievalIsBlamedWhenNothingFailed() {
        assertEquals("stale", rootCause(trace("t-policy")).why)
        assertEquals("no-hits", rootCause(trace("t-empty")).why)
        assertEquals("none", rootCause(trace("t-plain")).layer)
    }

    @Test
    fun driftFlagsBothDirectionsOverTolerance() {
        assertEquals(listOf("a up 40%", "b down 40%"), drift(mapOf("a" to 10, "b" to 10, "c" to 10), mapOf("a" to 14, "b" to 6, "c" to 11), 25))
    }

    @Test
    fun anAlertNeedsConsecutiveWindows() {
        val s = listOf(1, 2, 9, 2, 8, 9, 10, 3)
        assertEquals(2, alertAt(s, 5, 1))
        assertEquals(6, alertAt(s, 5, 3))
        assertEquals(-1, alertAt(listOf(9, 1, 9, 1, 9), 5, 2))
    }

    @Test
    fun redactDropsContentUnlessAllowed() {
        val e = linkedMapOf<String, Any>("trace" to "t", "prompt" to "x", "tool_input" to "y", "input_tokens" to 5)
        assertEquals(setOf("trace", "input_tokens"), redact(e).keys)
        assertEquals(setOf("trace", "input_tokens", "tool_input"), redact(e, setOf("tool_input")).keys)
    }
}
