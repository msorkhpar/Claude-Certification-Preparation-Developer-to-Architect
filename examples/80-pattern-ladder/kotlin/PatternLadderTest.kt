import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PatternLadderTest {
    private fun byName(name: String) = TASKS.first { it.name == name }

    @Test
    fun aSingleStepStaysOnTheFirstTwoRungs() {
        assertEquals("plain call", choosePattern(byName("classify ticket")))
        assertEquals("augmented call", choosePattern(byName("answer from policy")))
    }

    @Test
    fun aKnownPathIsAWorkflowAndCostsOneChatPerStep() {
        val task = byName("claims intake")
        assertEquals("workflow", choosePattern(task))
        assertEquals(0.08, Math.round(cost(task, "workflow") * 100) / 100.0)
    }

    @Test
    fun anOpenPathIsAnAgentAndATeamNeedsIndependentPartsAndValue() {
        assertEquals("agent", choosePattern(byName("investigate outage")))
        assertEquals("multi-agent", choosePattern(byName("market research brief")))
        assertEquals("agent", choosePattern(byName("trivia round-up")))
    }

    @Test
    fun theTeamThresholdIsFifteenChats() {
        val base = byName("market research brief")
        val threshold = MULTIPLIER.getValue("multi-agent") * base.chatCost
        assertEquals("multi-agent", choosePattern(base.copy(value = threshold)))
        assertEquals("agent", choosePattern(base.copy(value = threshold - 0.01)))
    }
}
