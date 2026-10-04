import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EscalationTest {
    private static Escalation.Case kase(boolean asked, int matches, boolean covers, int stalled, String sentiment, int confidence) {
        return new Escalation.Case(asked, matches, covers, stalled, sentiment, confidence);
    }

    private static Escalation.Case plain() {
        return kase(false, 1, true, 0, "calm", 50);
    }

    private static String action(Escalation.Case c) {
        return Escalation.decide(c).action();
    }

    private static Map<String, String> rec(String... kv) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put(kv[i], kv[i + 1]);
        return m;
    }

    @Test
    void m1_aCustomerWhoAsksForAPersonIsEscalatedAtOnceEvenWhenTheAgentCouldResolveIt() {
        assertEquals(new Escalation.Decision("escalate", "customer asked for a person", false), Escalation.decide(kase(true, 1, true, 0, "calm", 95)));
    }

    @Test
    void e1_frustrationAloneDoesNotEscalateAndTheReplyAcknowledgesIt() {
        assertEquals(new Escalation.Decision("resolve", "within capability", true), Escalation.decide(kase(false, 1, true, 0, "frustrated", 50)));
        assertFalse(Escalation.decide(plain()).acknowledge());
    }

    @Test
    void e2_aRequestThePolicyDoesNotCoverIsEscalatedAndACoveredOneIsResolved() {
        assertEquals(new Escalation.Decision("escalate", "policy does not cover the request", false), Escalation.decide(kase(false, 1, false, 0, "calm", 50)));
        assertEquals("resolve", action(plain()));
    }

    @Test
    void e3_severalMatchingRecordsNeedAClarifyingQuestionAndNeverAGuess() {
        assertEquals(new Escalation.Decision("clarify", "ambiguous customer match", false), Escalation.decide(kase(false, 3, true, 0, "calm", 50)));
        assertEquals("resolve", action(kase(false, 1, true, 0, "calm", 50)));
        assertEquals("resolve", action(kase(false, 0, true, 0, "calm", 50)));
    }

    @Test
    void e4_anExplicitRequestForAPersonOutranksAnAmbiguousMatch() {
        assertEquals("escalate", action(kase(true, 4, true, 0, "calm", 50)));
        assertEquals("escalate", action(kase(true, 1, false, 0, "calm", 50)));
    }

    @Test
    void e5_noProgressAfterTheAttemptLimitEscalatesAndBelowItDoesNot() {
        assertEquals(new Escalation.Decision("escalate", "no progress", false), Escalation.decide(kase(false, 1, true, 2, "calm", 50)));
        assertEquals("resolve", action(kase(false, 1, true, 1, "calm", 50)));
        assertEquals("resolve", Escalation.decide(kase(false, 1, true, 3, "calm", 50), 4).action());
    }

    @Test
    void e6_sentimentAndConfidenceScoresNeverChangeTheDecision() {
        for (String sentiment : List.of("calm", "frustrated", "angry")) {
            for (int confidence : List.of(5, 50, 99)) assertEquals("resolve", action(kase(false, 1, true, 0, sentiment, confidence)));
        }
        assertEquals("clarify", action(kase(false, 2, true, 0, "angry", 1)));
    }

    @Test
    void e7_theClarifyingQuestionNamesOnlyTheFieldsThatTellTheMatchesApart() {
        assertEquals(List.of("email"), Escalation.clarifyingFields(List.of(rec("id", "c1", "name", "Ana Ruiz", "email", "ana@example.com", "zip", "10115"), rec("id", "c2", "name", "Ana Ruiz", "email", "ana.r@example.com", "zip", "10115"))));
        assertEquals(List.of("name", "zip"), Escalation.clarifyingFields(List.of(rec("id", "c1", "name", "Ana", "zip", "1"), rec("id", "c2", "name", "Bo", "zip", "2"), rec("id", "c3", "name", "Ana", "zip", "3"))));
        assertEquals(List.of(), Escalation.clarifyingFields(List.of(rec("id", "c1", "name", "Ana"))));
    }

    @Test
    void e8_theHandOffCarriesTheStructuredFactsAndNoTranscriptAndRefusesACaseWithoutAnId() {
        Escalation.HandoffCase c = new Escalation.HandoffCase("C-77", "refund over the limit", "duplicate charge", "$129.50", List.of("verified identity", "checked order"), "approve the refund", "user: hello ... 40 turns ...");
        assertEquals("Customer: C-77\nIssue: refund over the limit\nRoot cause: duplicate charge\nAmount: $129.50\nActions taken: verified identity; checked order\nRecommended action: approve the refund", Escalation.handoffText(c));
        assertFalse(Escalation.handoffText(c).contains("40 turns"));
        assertEquals("Customer: C-1\nIssue: late parcel\nRoot cause: unknown\nAmount: unknown\nActions taken: none\nRecommended action: review the case", Escalation.handoffText(new Escalation.HandoffCase("C-1", "late parcel", null, null, null, null, null)));
        assertThrows(IllegalArgumentException.class, () -> Escalation.handoffText(new Escalation.HandoffCase(null, "late parcel", null, null, null, null, null)));
    }
}
