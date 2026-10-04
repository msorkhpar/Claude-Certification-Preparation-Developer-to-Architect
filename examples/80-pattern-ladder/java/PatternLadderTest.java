import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PatternLadderTest {
    private static PatternLadder.Task byName(String name) {
        return PatternLadder.TASKS.stream().filter(t -> t.name().equals(name)).findFirst().orElseThrow();
    }

    @Test
    void aSingleStepStaysOnTheFirstTwoRungs() {
        assertEquals("plain call", PatternLadder.choosePattern(byName("classify ticket")));
        assertEquals("augmented call", PatternLadder.choosePattern(byName("answer from policy")));
    }

    @Test
    void aKnownPathIsAWorkflowAndCostsOneChatPerStep() {
        PatternLadder.Task task = byName("claims intake");
        assertEquals("workflow", PatternLadder.choosePattern(task));
        assertEquals(0.08, Math.round(PatternLadder.cost(task, "workflow") * 100) / 100.0);
    }

    @Test
    void anOpenPathIsAnAgentAndATeamNeedsIndependentPartsAndValue() {
        assertEquals("agent", PatternLadder.choosePattern(byName("investigate outage")));
        assertEquals("multi-agent", PatternLadder.choosePattern(byName("market research brief")));
        assertEquals("agent", PatternLadder.choosePattern(byName("trivia round-up")));
    }

    @Test
    void theTeamThresholdIsFifteenChats() {
        PatternLadder.Task base = byName("market research brief");
        double threshold = PatternLadder.MULTIPLIER.get("multi-agent") * base.chatCost();
        PatternLadder.Task at = new PatternLadder.Task(base.name(), base.oneStep(), base.steps(), base.needsExternal(), base.stepsKnown(), base.independentParts(), threshold, base.chatCost());
        PatternLadder.Task below = new PatternLadder.Task(base.name(), base.oneStep(), base.steps(), base.needsExternal(), base.stepsKnown(), base.independentParts(), threshold - 0.01, base.chatCost());
        assertEquals("multi-agent", PatternLadder.choosePattern(at));
        assertEquals("agent", PatternLadder.choosePattern(below));
    }
}
