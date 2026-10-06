import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TriageTest {
    private fun sp(id: String, parent: String = "", kind: String = "agent", name: String = "n", status: String = "ok", ms: Int = 100, note: String = "") = Span(id, parent, kind, name, status, ms, note)

    private val plain = listOf(sp("s1", "", "agent", "assistant", "ok", 1900), sp("s2", "s1", "retrieval", "search", "ok", 100), sp("s3", "s1", "llm", "answer", "ok", 1700))

    private fun keep(id: String, spans: List<Span>, rate: Int, feedback: Boolean = false, slowMs: Int = 5000): String {
        val result = keepTrace(id, spans, rate, feedback, slowMs)
        assertNotNull(result, "keepTrace returned nothing")
        return result ?: ""
    }

    private fun cause(spans: List<Span>): Cause {
        val result = rootCause(spans)
        assertNotNull(result, "rootCause returned nothing")
        return result ?: Cause("", "", "", listOf())
    }

    private fun listed(result: List<String>?): List<String> {
        assertNotNull(result, "a list was expected")
        return result ?: listOf()
    }

    @Test
    fun m1_everyTraceGetsOneReasonAndAHealthyTraceIsKeptByItsId() {
        assertEquals("error", keep("a", listOf(sp("s1", status = "error")), 0))
        assertEquals("slow", keep("a", listOf(sp("s1", ms = 6000)), 0))
        assertEquals("feedback", keep("a", plain, 0, feedback = true))
        assertEquals("dropped", keep("a", plain, 0))
        assertEquals("sampled", keep("a", plain, 100))
        val ids = (0 until 100).map { "trace-$it" }
        val first = ids.map { keep(it, plain, 10) }
        assertEquals(first, ids.map { keep(it, plain, 10) })
        assertTrue(first.count { it == "sampled" } in 1..99)
    }

    @Test
    fun e1_anErrorOutranksASlowRootWhichOutranksRetriesWhichOutrankAFlag() {
        val calls = listOf(sp("s2", "s1", "tool", "fetch"), sp("s3", "s1", "tool", "fetch"), sp("s4", "s1", "tool", "fetch"))
        assertEquals("error", keep("a", listOf(sp("s1", ms = 9000, status = "error")), 0))
        assertEquals("slow", keep("a", listOf(sp("s1", ms = 9000)) + calls, 0, feedback = true))
        assertEquals("retries", keep("a", listOf(sp("s1")) + calls, 0, feedback = true))
        assertEquals("dropped", keep("a", listOf(sp("s1")) + calls.take(2), 0))
        assertEquals("dropped", keep("a", listOf(sp("s1", ms = 5000)), 0))
        assertEquals("slow", keep("a", listOf(sp("s1", ms = 3000)), 0, slowMs = 2000))
    }

    @Test
    fun e2_theRootCauseIsTheDeepestFailingSpanAndItsPathStartsAtTheRoot() {
        val spans = listOf(sp("s1", name = "orchestrator", status = "error"), sp("s2", "s1", "agent", "researcher", "error"), sp("s3", "s2", "tool", "fetch", "error"), sp("s4", "s1", "llm", "summarise"))
        val c = cause(spans)
        assertEquals("tool", c.layer)
        assertEquals("fetch", c.name)
        assertEquals("failed", c.why)
        assertEquals(listOf("orchestrator", "researcher", "fetch"), c.path)
        val two = listOf(sp("s1", name = "top"), sp("s2", "s1", "tool", "first", "error"), sp("s3", "s1", "tool", "second", "error"))
        assertEquals("first", cause(two).name)
    }

    @Test
    fun e3_aStaleOrEmptyRetrievalIsBlamedOnlyWhenNoSpanFailed() {
        val stale = listOf(sp("s1", name = "assistant"), sp("s2", "s1", "retrieval", "search", note = "stale"), sp("s3", "s1", "llm", "answer"))
        assertEquals("retrieval", cause(stale).layer)
        assertEquals("stale", cause(stale).why)
        assertEquals(listOf("assistant", "search"), cause(stale).path)
        val empty = listOf(sp("s1", name = "assistant"), sp("s2", "s1", "retrieval", "search", note = "no-hits"))
        assertEquals("no-hits", cause(empty).why)
        val both = listOf(stale[0], stale[1], sp("s3", "s1", "llm", "answer", "error"))
        assertEquals("llm", cause(both).layer)
        assertEquals("failed", cause(both).why)
        assertEquals(Cause("none", "", "no span failed", listOf()), cause(plain))
    }

    @Test
    fun e4_driftReportsAMoveInEitherDirectionOverTheToleranceAndNeverDividesByZero() {
        assertEquals(listOf("a up 40%", "b down 40%"), listed(drift(mapOf("a" to 10, "b" to 10, "c" to 10), mapOf("a" to 14, "b" to 6, "c" to 11), 25)))
        assertEquals(listOf<String>(), listed(drift(mapOf("a" to 10), mapOf("a" to 13), 30)))
        assertEquals(listOf("a up 40%"), listed(drift(mapOf("a" to 10), mapOf("a" to 14), 30)))
        assertEquals(listOf("z up 100%"), listed(drift(mapOf("z" to 0, "y" to 0), mapOf("z" to 5, "y" to 0), 25)))
        assertEquals(listOf("a up 100%", "b up 100%"), listed(drift(mapOf("b" to 5, "a" to 5), mapOf("b" to 10, "a" to 10), 0)))
    }

    @Test
    fun e5_anAlertNeedsConsecutiveWindowsOverTheThresholdAndADipStartsTheCountAgain() {
        val s = listOf(1, 2, 9, 2, 8, 9, 10, 3)
        assertEquals(2, alertAt(s, 5, 1))
        assertEquals(6, alertAt(s, 5, 3))
        assertEquals(-1, alertAt(listOf(9, 1, 9, 1, 9), 5, 2))
        assertEquals(-1, alertAt(listOf(5, 5, 5), 5, 1))
        assertEquals(-1, alertAt(listOf(), 5, 1))
    }

    @Test
    fun e6_aLogRecordDropsTheContentFieldsUnlessTheyAreAllowedByName() {
        val e = linkedMapOf<String, Any>("trace" to "t", "prompt" to "x", "response" to "y", "tool_input" to "z", "tool_output" to "w", "input_tokens" to 5)
        val plainRecord = redact(e)
        assertNotNull(plainRecord, "redact returned nothing")
        assertEquals(mapOf("trace" to "t", "input_tokens" to 5), plainRecord)
        val some = redact(e, setOf("tool_input"))
        assertNotNull(some, "redact returned nothing")
        assertEquals(mapOf("trace" to "t", "tool_input" to "z", "input_tokens" to 5), some)
        val same = redact(e, setOf("input_tokens"))
        assertNotNull(same, "redact returned nothing")
        assertEquals(mapOf("trace" to "t", "input_tokens" to 5), same)
    }

    @Test
    fun e7_aRequestsTrailJoinsTheEventsOfEveryComponentInTimeOrder() {
        val events = listOf(Event("r1", 30, "tool", "lookup done"), Event("r2", 10, "api", "other"), Event("r1", 10, "api", "received"), Event("r1", 20, "agent", "plan"), Event("r1", 20, "llm", "called"))
        assertEquals(listOf("api: received", "agent: plan", "llm: called", "tool: lookup done"), listed(requestTrail(events, "r1")))
        assertEquals(listOf<String>(), listed(requestTrail(events, "r9")))
    }
}
