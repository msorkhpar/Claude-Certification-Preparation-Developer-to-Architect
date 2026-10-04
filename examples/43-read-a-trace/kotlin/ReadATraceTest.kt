import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ReadATraceTest {
    @Test
    fun anEmptyReplyAfterTextFollowingAToolResultIsOurMessageStructure() {
        val f = firstFailure(TRACES.getValue("A: a tool loop that ends in silence"))!!
        assertEquals(Triple(3, "empty reply", "integration"), Triple(f.index, f.what, f.origin))
    }

    @Test
    fun theSameEmptyReplyWithoutThatTextIsTheModel() {
        val f = firstFailure(listOf(Request(listOf("tool_result")), Response(200, "end_turn", emptyList())))!!
        assertEquals(Triple(1, "empty reply", "model"), Triple(f.index, f.what, f.origin))
    }

    @Test
    fun a529IsTheServiceAndTheNextActionIsARetry() {
        assertEquals(Failure(1, "overloaded_error", "service", "retry with back-off"), firstFailure(TRACES.getValue("B: a busy service and a retry")))
    }

    @Test
    fun jsonInsideACodeFenceIsTheParserAndProseWithoutJsonIsTheModel() {
        val fenced = firstFailure(TRACES.getValue("C: JSON in a code fence"))!!
        assertEquals(Triple(2, "parse failure", "integration"), Triple(fenced.index, fenced.what, fenced.origin))
        val prose = firstFailure(listOf(Parse(false, "I am not sure")))!!
        assertEquals(Triple(0, "parse failure", "model"), Triple(prose.index, prose.what, prose.origin))
    }

    @Test
    fun aCleanTraceHasNoFailure() {
        assertNull(firstFailure(listOf(Request(listOf("text")), Response(200, "end_turn", listOf("text")))))
    }
}
