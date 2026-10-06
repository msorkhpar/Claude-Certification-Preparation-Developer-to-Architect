import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class AssistantTurnsTest {
    @Test
    void aSignalOfRiskIsRoutedToAPersonBeforeAnythingElse() {
        assertEquals("handoff:safety", AssistantTurns.route("I might hurt myself, please get me a human", 0));
        assertEquals("handoff:safety", AssistantTurns.route("this is an emergency", 5));
    }

    @Test
    void aRequestForAPersonIsHonouredAndAStallNeedsTwoMisses() {
        assertEquals("handoff:requested", AssistantTurns.route("Can I speak to a person?", 0));
        assertEquals("answer", AssistantTurns.route("what?", 1));
        assertEquals("handoff:stalled", AssistantTurns.route("what?", 2));
    }

    @Test
    void theWindowKeepsTheNewestTurnsThatFitAndAllThePinnedFacts() {
        var turns = List.of("one two", "three four five", "six");
        assertEquals(new AssistantTurns.Window(List.of("three four five", "six"), 1, List.of("fact")), AssistantTurns.window(turns, 4, List.of("fact")));
        assertEquals(List.of("six"), AssistantTurns.window(turns, 3, List.of("fact")).kept());
        assertEquals(0, AssistantTurns.window(turns, 6, List.of("fact")).dropped());
    }

    @Test
    void memoryIsReadPerCustomerAndOldFactsAreMarkedToVerify() {
        assertEquals(List.of(new AssistantTurns.Recalled("address", "12 Elm Road", "current"), new AssistantTurns.Recalled("plan", "Plus", "verify")), AssistantTurns.recall("ada", "2026-10-04", 30));
        assertEquals(List.of(), AssistantTurns.recall("bob", "2026-10-04", 30));
        assertEquals("current", AssistantTurns.recall("ada", "2026-10-20", 30).get(0).status());
        assertEquals("verify", AssistantTurns.recall("ada", "2026-10-21", 30).get(0).status());
    }
}
