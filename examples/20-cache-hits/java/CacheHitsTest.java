import static org.junit.jupiter.api.Assertions.*;

import com.anthropic.models.messages.Usage;
import org.junit.jupiter.api.Test;

class CacheHitsTest {
    private static Usage usage(CacheHits.CacheSim sim, com.anthropic.models.messages.MessageCreateParams body, int wait) {
        return CacheHits.send(sim, body, wait);
    }

    @Test
    void theFirstCallWritesAndTheSecondReadsTheSamePrefix() {
        CacheHits.CacheSim sim = new CacheHits.CacheSim();
        Usage first = usage(sim, CacheHits.stable("a"), 0);
        Usage second = usage(sim, CacheHits.stable("b"), 60);
        assertTrue(first.cacheCreationInputTokens().get() > 500 && first.cacheReadInputTokens().get() == 0);
        assertEquals(first.cacheCreationInputTokens().get(), second.cacheReadInputTokens().get());
        assertEquals(0, second.cacheCreationInputTokens().get());
        assertEquals(CacheHits.tokens(CacheHits.POLICY), first.cacheCreationInputTokens().get());
    }

    @Test
    void aHitRefreshesTheFiveMinuteLifetimeAndSilenceLosesIt() {
        CacheHits.CacheSim sim = new CacheHits.CacheSim();
        usage(sim, CacheHits.stable("a"), 0);
        assertTrue(usage(sim, CacheHits.stable("b"), 250).cacheReadInputTokens().get() > 0);
        assertTrue(usage(sim, CacheHits.stable("c"), 250).cacheReadInputTokens().get() > 0); // 500 s after the write, 250 after the last hit
        assertEquals(0, usage(sim, CacheHits.stable("d"), 400).cacheReadInputTokens().get());
    }

    @Test
    void aChangingBlockBeforeTheBreakpointDefeatsTheCache() {
        CacheHits.CacheSim sim = new CacheHits.CacheSim();
        usage(sim, CacheHits.stampFirst("10:01", "q"), 0);
        assertEquals(0, usage(sim, CacheHits.stampFirst("10:02", "q"), 60).cacheReadInputTokens().get());
    }

    @Test
    void theSameBlockAfterTheBreakpointCostsNothing() {
        CacheHits.CacheSim sim = new CacheHits.CacheSim();
        usage(sim, CacheHits.stampLast("10:01", "q"), 0);
        assertTrue(usage(sim, CacheHits.stampLast("10:02", "q"), 60).cacheReadInputTokens().get() > 500);
    }

    @Test
    void aPrefixUnderTheMinimumIsNotCachedAndNoErrorIsRaised() {
        CacheHits.CacheSim sim = new CacheHits.CacheSim(100000, 300);
        Usage first = usage(sim, CacheHits.stable("a"), 0);
        assertEquals(0, first.cacheCreationInputTokens().get());
        assertTrue(first.inputTokens() > 500);
    }
}
