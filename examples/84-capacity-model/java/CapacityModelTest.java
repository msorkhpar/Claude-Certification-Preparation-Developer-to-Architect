import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CapacityModelTest {
    @Test
    void cacheReadsDoNotCountTowardTheInputLimit() {
        assertEquals(new CapacityModel.Need(800, 800 * 1700, 320_000), CapacityModel.requiredCapacity(CapacityModel.CACHED, 0));
    }

    @Test
    void headroomRoundsEveryFigureUp() {
        assertEquals(new CapacityModel.Need(2, 2, 2), CapacityModel.requiredCapacity(new CapacityModel.Workload(1, 1, 0, 0, 1), 30));
        assertEquals(new CapacityModel.Need(1040, 1_768_000, 416_000), CapacityModel.requiredCapacity(CapacityModel.CACHED, 30));
    }

    @Test
    void theTierMustCoverAllThreeLimitsAndCustomIsTheLastResort() {
        assertEquals("Start", CapacityModel.smallestTier(new CapacityModel.Need(100, 100, 100), CapacityModel.TIERS));
        assertEquals("Build", CapacityModel.smallestTier(new CapacityModel.Need(100, 100, 400_001), CapacityModel.TIERS));
        assertEquals("Custom", CapacityModel.smallestTier(new CapacityModel.Need(10_001, 1, 1), CapacityModel.TIERS));
        assertEquals("Scale", CapacityModel.smallestTier(CapacityModel.requiredCapacity(CapacityModel.UNCACHED, 30), CapacityModel.TIERS));
    }

    @Test
    void theMonthlyBillCountsEveryTokenKindAndBatchIsHalfPrice() {
        assertEquals(1_740_000, CapacityModel.monthlyCents(CapacityModel.CACHED, 2_000_000, 0));
        assertEquals(3_880_000, CapacityModel.monthlyCents(CapacityModel.UNCACHED, 2_000_000, 0));
        assertEquals(870_000, CapacityModel.monthlyCents(CapacityModel.CACHED, 2_000_000, 100));
        assertEquals(1_479_000, CapacityModel.monthlyCents(CapacityModel.CACHED, 2_000_000, 30));
    }

    @Test
    void dollarsPrintsCentsWithAThousandsSeparator() {
        assertEquals("$17,400.00", CapacityModel.dollars(1_740_000));
        assertEquals("$0.05", CapacityModel.dollars(5));
        assertEquals("$1.00", CapacityModel.dollars(100));
    }
}
