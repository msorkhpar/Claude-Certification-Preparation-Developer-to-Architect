import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class EscalationRulesTest {
    @Test
    void theCriteriaRouteEveryCaseAsACarefulPersonWouldAndTheSentimentRuleDoesNot() {
        assertEquals(List.of(), EscalationRules.errors(EscalationRules::byCriteria));
        assertEquals(List.of(1, 2, 3, 4, 5), EscalationRules.errors(EscalationRules::bySentiment));
        assertEquals(6, EscalationRules.CASES.size());
    }

    @Test
    void theRecencyHeuristicPicksOneAccountAndTheQuestionPicksNone() {
        assertEquals("c2", EscalationRules.pickMostRecent(List.of(new EscalationRules.Account("c1", 1), new EscalationRules.Account("c2", 2))));
        assertEquals("I found 2 accounts for that name. Please give me one of: the email, the postcode.", EscalationRules.askForIdentifier(2, List.of("the email", "the postcode")));
    }

    @Test
    void thePromptSectionListsTheCriteriaAndTheExamplesWithTheirReasons() {
        assertEquals("Escalate to a person when:\n- a request for a person\n\nExamples:\nCustomer: \"Hi\" -> resolve (simple)",
            EscalationRules.escalationSection(List.of("a request for a person"), List.of(new EscalationRules.Example("Hi", "resolve", "simple"))));
    }
}
