import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RoutingAndVotingTest {
    private static final List<String[]> DESK = RoutingAndVoting.rules("billing specialist", "I see two charges.", "front desk", "Open 9 to 5.");

    private static List<String[]> withLabel(String label) {
        List<String[]> all = new ArrayList<>(java.util.Arrays.<String[]>asList(new String[] {"Classify", label}));
        all.addAll(DESK);
        return all;
    }

    @Test
    void aLabelPicksTheModelAndAnUnknownLabelTakesTheDefault() {
        var rig = RoutingAndVoting.scripted(withLabel("Billing."), 2, Duration.ZERO);
        RoutingAndVoting.Routed billing = RoutingAndVoting.route(rig.client(), "charged twice").join();
        assertEquals(List.of("billing", RoutingAndVoting.STRONG, false), List.of(billing.label(), billing.model(), billing.fallback()));
        assertEquals(RoutingAndVoting.CHEAP, rig.http().requests.get(0).get("model").asText());
        var odd = RoutingAndVoting.route(RoutingAndVoting.scripted(withLabel("refunds?"), 2, Duration.ZERO).client(), "hello").join();
        assertEquals(List.of("refunds?", RoutingAndVoting.CHEAP, true, "Open 9 to 5."), List.of(odd.label(), odd.model(), odd.fallback(), odd.answer()));
    }

    @Test
    void sectioningRunsBothCallsTogetherAndDropsTheAnswerWhenTheScreenBlocks() {
        var rig = RoutingAndVoting.scripted(RoutingAndVoting.rules("Answer the question", "Fine.", "Screen the question", "block"), 2, Duration.ofMillis(20));
        RoutingAndVoting.Guarded result = RoutingAndVoting.guarded(rig.client(), "q").join();
        assertEquals(new RoutingAndVoting.Guarded("block", null), result);
        assertEquals(2, rig.http().maxInFlight());
    }

    @Test
    void votingCountsTheReviewsAgainstTheThreshold() {
        var two = RoutingAndVoting.disagreeing(Duration.ofMillis(10));
        RoutingAndVoting.Verdict flagged = RoutingAndVoting.vote(two.client(), "code", 2, 3).join();
        var three = RoutingAndVoting.disagreeing(Duration.ofMillis(10));
        RoutingAndVoting.Verdict notFlagged = RoutingAndVoting.vote(three.client(), "code", 3, 3).join();
        assertTrue(flagged.flagged());
        assertFalse(notFlagged.flagged());
        assertEquals(3, two.http().maxInFlight());
        assertEquals(Map.of("SAFE", 1L, "VULNERABLE", 2L), flagged.votes());
    }
}
