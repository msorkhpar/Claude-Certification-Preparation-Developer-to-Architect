import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CachePlanTest {
    private fun block(id: String, section: String, tokens: Int, vararg extra: Pair<String, Any?>): Map<String, Any?> =
        mapOf("id" to id, "section" to section, "tokens" to tokens, *extra)

    private fun ids(plan: List<Map<String, Any?>>?): List<Any?> = plan?.map { it["id"] } ?: emptyList()
    private fun caches(plan: List<Map<String, Any?>>?): Map<Any?, Any?> = (plan ?: emptyList()).associate { it["id"] to it["cache"] }
    private fun values(plan: List<Map<String, Any?>>?): List<Any?> = caches(plan).values.toList()

    private fun raised(blocks: List<Map<String, Any?>>, minTokens: Int = 1024): Throwable? = try {
        planRequest(blocks, minTokens)
        null
    } catch (e: RuntimeException) {
        e
    }

    @Test
    fun m1_stableContentComesFirstAndTheVolatileDateGoesLast() {
        val blocks = listOf(
            block("date", "system", 20, "volatile" to true),
            block("tools", "tools", 2000),
            block("rules", "system", 3000, "breakpoint" to true),
            block("manual", "messages", 6000, "breakpoint" to true),
            block("question", "messages", 40),
        )
        val plan = planRequest(blocks, 1024)
        assertEquals(listOf("tools", "rules", "manual", "question", "date"), ids(plan))
        assertEquals(mapOf("tools" to null, "rules" to "5m", "manual" to "5m", "question" to null, "date" to null), caches(plan))
    }

    @Test
    fun e1_sectionsFollowThePrefixOrderAndKeepTheirOwnOrder() {
        val blocks = listOf(block("m1", "messages", 10), block("s1", "system", 10), block("t1", "tools", 10),
            block("s2", "system", 10), block("t2", "tools", 10), block("m2", "messages", 10))
        assertEquals(listOf("t1", "t2", "s1", "s2", "m1", "m2"), ids(planRequest(blocks)))
    }

    @Test
    fun e2_aBreakpointNeedsTheStablePrefixToReachTheMinimum() {
        val blocks = listOf(block("tools", "tools", 400, "breakpoint" to true), block("rules", "system", 500, "breakpoint" to true),
            block("doc", "messages", 700, "breakpoint" to true), block("ask", "messages", 30))
        assertEquals(mapOf("tools" to null, "rules" to null, "doc" to "5m", "ask" to null), caches(planRequest(blocks, 1024)))
        assertEquals(mapOf("tools" to null, "rules" to null, "doc" to null, "ask" to null), caches(planRequest(blocks, 4096)))
        // volatile tokens come after the prefix, so they never help it reach the minimum
        val padded = listOf(block("stamp", "system", 5000, "volatile" to true), block("rules", "system", 500, "breakpoint" to true))
        assertEquals(mapOf("rules" to null, "stamp" to null), caches(planRequest(padded, 1024)))
    }

    @Test
    fun e3_atMostFourBreakpointsAreSent() {
        val five = List(5) { block("b$it", "messages", 2000, "breakpoint" to true) }
        assertTrue(raised(five) is PlanError)
        assertEquals(listOf<Any?>("5m", "5m", "5m", "5m"), values(planRequest(five.take(4), 1024)))
        // two of the five never reach the minimum, so only three breakpoints are sent
        val small = listOf(block("a", "tools", 10, "breakpoint" to true), block("b", "system", 10, "breakpoint" to true)) + five.take(3)
        assertEquals(listOf<Any?>(null, null, "5m", "5m", "5m"), values(planRequest(small, 1024)))
    }

    @Test
    fun e4_aOneHourBreakpointMayNotFollowAFiveMinuteOne() {
        val longFirst = listOf(block("docs", "system", 3000, "breakpoint" to true, "ttl" to "1h"), block("turns", "messages", 3000, "breakpoint" to true))
        assertEquals(mapOf("docs" to "1h", "turns" to "5m"), caches(planRequest(longFirst, 1024)))
        val wrongWay = listOf(block("turns", "system", 3000, "breakpoint" to true), block("docs", "messages", 3000, "breakpoint" to true, "ttl" to "1h"))
        assertTrue(raised(wrongWay) is PlanError)
    }

    @Test
    fun e5_volatileBlocksNeverCarryABreakpointAndToolsCannotBeVolatile() {
        val blocks = listOf(block("rules", "system", 3000, "breakpoint" to true), block("stamp", "messages", 3000, "volatile" to true, "breakpoint" to true))
        assertEquals(mapOf("rules" to "5m", "stamp" to null), caches(planRequest(blocks, 1024)))
        assertTrue(raised(listOf(block("t", "tools", 3000, "volatile" to true))) is PlanError)
    }

    @Test
    fun e6_everyBlockComesOutOnceAndTheInputIsNotChanged() {
        val blocks = mutableListOf(block("date", "system", 20, "volatile" to true), block("a", "messages", 2000, "breakpoint" to true), block("t", "tools", 2000))
        val before = blocks.map { it.toMap() }
        val plan = planRequest(blocks, 1024)
        assertEquals(listOf<Any?>("a", "date", "t"), ids(plan).sortedBy { it.toString() })
        assertEquals(before, blocks.toList())
    }
}
