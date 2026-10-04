import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class EscalationRulesTest {
    @Test
    fun theCriteriaRouteEveryCaseAsACarefulPersonWouldAndTheSentimentRuleDoesNot() {
        assertEquals(emptyList<Int>(), errors(::byCriteria))
        assertEquals(listOf(1, 2, 3, 4, 5), errors(::bySentiment))
        assertEquals(6, CASES.size)
    }

    @Test
    fun theRecencyHeuristicPicksOneAccountAndTheQuestionPicksNone() {
        assertEquals("c2", pickMostRecent(listOf(Account("c1", 1), Account("c2", 2))))
        assertEquals("I found 2 accounts for that name. Please give me one of: the email, the postcode.", askForIdentifier(2, listOf("the email", "the postcode")))
    }

    @Test
    fun thePromptSectionListsTheCriteriaAndTheExamplesWithTheirReasons() {
        assertEquals("Escalate to a person when:\n- a request for a person\n\nExamples:\nCustomer: \"Hi\" -> resolve (simple)", escalationSection(listOf("a request for a person"), listOf(Example("Hi", "resolve", "simple"))))
    }
}
