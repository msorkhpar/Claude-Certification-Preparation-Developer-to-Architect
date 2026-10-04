import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CapacityModelTest {
    @Test
    fun cacheReadsDoNotCountTowardTheInputLimit() {
        assertEquals(Need(800, 800 * 1700, 320_000), requiredCapacity(CACHED, 0))
    }

    @Test
    fun headroomRoundsEveryFigureUp() {
        assertEquals(Need(2, 2, 2), requiredCapacity(Workload(1, 1, 0, 0, 1), 30))
        assertEquals(Need(1040, 1_768_000, 416_000), requiredCapacity(CACHED, 30))
    }

    @Test
    fun theTierMustCoverAllThreeLimitsAndCustomIsTheLastResort() {
        assertEquals("Start", smallestTier(Need(100, 100, 100), TIERS))
        assertEquals("Build", smallestTier(Need(100, 100, 400_001), TIERS))
        assertEquals("Custom", smallestTier(Need(10_001, 1, 1), TIERS))
        assertEquals("Scale", smallestTier(requiredCapacity(UNCACHED, 30), TIERS))
    }

    @Test
    fun theMonthlyBillCountsEveryTokenKindAndBatchIsHalfPrice() {
        assertEquals(1_740_000L, monthlyCents(CACHED, 2_000_000, 0))
        assertEquals(3_880_000L, monthlyCents(UNCACHED, 2_000_000, 0))
        assertEquals(870_000L, monthlyCents(CACHED, 2_000_000, 100))
        assertEquals(1_479_000L, monthlyCents(CACHED, 2_000_000, 30))
    }

    @Test
    fun dollarsPrintsCentsWithAThousandsSeparator() {
        assertEquals("$17,400.00", dollars(1_740_000))
        assertEquals("$0.05", dollars(5))
        assertEquals("$1.00", dollars(100))
    }
}
