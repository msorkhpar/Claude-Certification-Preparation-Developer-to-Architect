import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CacheHitsTest {
    @Test
    fun theFirstCallWritesAndTheSecondReadsTheSamePrefix() {
        val sim = CacheSim()
        val first = send(sim, stable("a"))
        val second = send(sim, stable("b"), 60)
        assertTrue(first.cacheCreationInputTokens().get() > 500 && first.cacheReadInputTokens().get() == 0L)
        assertEquals(first.cacheCreationInputTokens().get(), second.cacheReadInputTokens().get())
        assertEquals(0L, second.cacheCreationInputTokens().get())
        assertEquals(tokens(POLICY).toLong(), first.cacheCreationInputTokens().get())
    }

    @Test
    fun aHitRefreshesTheFiveMinuteLifetimeAndSilenceLosesIt() {
        val sim = CacheSim()
        send(sim, stable("a"))
        assertTrue(send(sim, stable("b"), 250).cacheReadInputTokens().get() > 0)
        assertTrue(send(sim, stable("c"), 250).cacheReadInputTokens().get() > 0) // 500 s after the write, 250 after the last hit
        assertEquals(0L, send(sim, stable("d"), 400).cacheReadInputTokens().get())
    }

    @Test
    fun aChangingBlockBeforeTheBreakpointDefeatsTheCache() {
        val sim = CacheSim()
        send(sim, stampFirst("10:01", "q"))
        assertEquals(0L, send(sim, stampFirst("10:02", "q"), 60).cacheReadInputTokens().get())
    }

    @Test
    fun theSameBlockAfterTheBreakpointCostsNothing() {
        val sim = CacheSim()
        send(sim, stampLast("10:01", "q"))
        assertTrue(send(sim, stampLast("10:02", "q"), 60).cacheReadInputTokens().get() > 500)
    }

    @Test
    fun aPrefixUnderTheMinimumIsNotCachedAndNoErrorIsRaised() {
        val sim = CacheSim(minimum = 100000)
        val first = send(sim, stable("a"))
        assertEquals(0L, first.cacheCreationInputTokens().get())
        assertTrue(first.inputTokens() > 500)
    }
}
