import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DistributionTest {
    @Test
    fun eachRoleGetsItsOwnToolsAndTheIrreversibleToolIsNobodysByDefault() {
        assertEquals(listOf("web_search", "fetch_page", "verify_fact"), toolsFor("searcher"))
        assertEquals(listOf("verify_fact", "summarize_content"), toolsFor("synthesizer"))
        assertFalse("send_report" in toolsFor("reporter"))
        assertTrue(ROLES.keys.all { toolsFor(it).size <= 5 })
    }

    @Test
    fun aModelThatRejectsForcingGetsAutoOneToolAndACheckOnTheReply() {
        assertEquals(Turn("auto", listOf("a"), true), turnFor("claude-sonnet-5-5", "a", listOf("a", "b")))
        assertEquals(Turn("tool:a", listOf("a", "b"), false), turnFor("claude-opus-5", "a", listOf("a", "b")))
    }

    @Test
    fun narrowingTheToolsCostsMoreThanChangingTheChoice() {
        val base = Turn("auto", listOf("a", "b"), false)
        assertTrue(cacheCost(base, turnFor("claude-opus-5", "a", listOf("a", "b"))).startsWith("the cached messages"))
        assertTrue(cacheCost(base, turnFor("claude-sonnet-5-5", "a", listOf("a", "b"))).startsWith("everything"))
        assertEquals("nothing", cacheCost(base, base.copy()))
    }

    @Test
    fun aReplyMadeTheCallOnlyWhenTheFirstToolUseIsTheForcedOne() {
        assertTrue(madeTheCall(listOf(Block("tool_use", "a")), "a"))
        assertFalse(madeTheCall(listOf(Block("text", "x")), "a"))
        assertFalse(madeTheCall(listOf(Block("tool_use", "b"), Block("tool_use", "a")), "a"))
    }

    @Test
    fun aRefundNeedsApprovalAndStaysUnderTheLimit() {
        assertTrue(allowed("refund", 150, false).startsWith("wait"))
        assertEquals("run", allowed("refund", 150, true))
        assertTrue(allowed("refund", 400, true).startsWith("refused"))
        assertTrue(allowed("delete_account", 0, true).startsWith("refused"))
    }
}
