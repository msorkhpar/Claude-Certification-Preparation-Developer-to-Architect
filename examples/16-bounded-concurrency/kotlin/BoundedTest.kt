import com.anthropic.errors.RateLimitException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BoundedTest {
    @Test
    fun unboundedRunsEverythingAtOnce() {
        assertEquals(12, runAll(null).peak)
    }

    @Test
    fun aSemaphoreCapsInFlightRequests() {
        assertEquals(4, runAll(4).peak)
    }

    @Test
    fun oneFailureDoesNotCancelTheOthersAndOrderIsKept() {
        val run = runAll(4)
        assertInstanceOf(RateLimitException::class.java, run.results[6].error)
        assertEquals((1..12).filter { it != 7 }.map { "label for ticket $it" }, run.results.filterIndexed { i, _ -> i != 6 }.map { it.label })
    }
}
