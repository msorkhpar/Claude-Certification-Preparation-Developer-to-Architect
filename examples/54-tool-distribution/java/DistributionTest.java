import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class DistributionTest {
    @Test
    void eachRoleGetsItsOwnToolsAndTheIrreversibleToolIsNobodysByDefault() {
        assertEquals(List.of("web_search", "fetch_page", "verify_fact"), Distribution.toolsFor("searcher"));
        assertEquals(List.of("verify_fact", "summarize_content"), Distribution.toolsFor("synthesizer"));
        assertFalse(Distribution.toolsFor("reporter").contains("send_report"));
        for (String role : Distribution.ROLES.keySet()) assertTrue(Distribution.toolsFor(role).size() <= 5);
    }

    @Test
    void aModelThatRejectsForcingGetsAutoOneToolAndACheckOnTheReply() {
        assertEquals(new Distribution.Turn("auto", List.of("a"), true), Distribution.turnFor("claude-sonnet-5-5", "a", List.of("a", "b")));
        assertEquals(new Distribution.Turn("tool:a", List.of("a", "b"), false), Distribution.turnFor("claude-opus-5", "a", List.of("a", "b")));
    }

    @Test
    void narrowingTheToolsCostsMoreThanChangingTheChoice() {
        var base = new Distribution.Turn("auto", List.of("a", "b"), false);
        assertTrue(Distribution.cacheCost(base, Distribution.turnFor("claude-opus-5", "a", List.of("a", "b"))).startsWith("the cached messages"));
        assertTrue(Distribution.cacheCost(base, Distribution.turnFor("claude-sonnet-5-5", "a", List.of("a", "b"))).startsWith("everything"));
        assertEquals("nothing", Distribution.cacheCost(base, new Distribution.Turn("auto", List.of("a", "b"), false)));
    }

    @Test
    void aReplyMadeTheCallOnlyWhenTheFirstToolUseIsTheForcedOne() {
        assertTrue(Distribution.madeTheCall(List.of(new Distribution.Block("tool_use", "a")), "a"));
        assertFalse(Distribution.madeTheCall(List.of(new Distribution.Block("text", "x")), "a"));
        assertFalse(Distribution.madeTheCall(List.of(new Distribution.Block("tool_use", "b"), new Distribution.Block("tool_use", "a")), "a"));
    }

    @Test
    void aRefundNeedsApprovalAndStaysUnderTheLimit() {
        assertTrue(Distribution.allowed("refund", 150, false).startsWith("wait"));
        assertEquals("run", Distribution.allowed("refund", 150, true));
        assertTrue(Distribution.allowed("refund", 400, true).startsWith("refused"));
        assertTrue(Distribution.allowed("delete_account", 0, true).startsWith("refused"));
    }
}
