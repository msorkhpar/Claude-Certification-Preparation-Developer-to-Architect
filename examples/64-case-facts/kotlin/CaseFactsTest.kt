import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CaseFactsTest {
    @Test
    fun shrinkingKeepsTheFieldsTheToolIsUsedForWithExactValues() {
        val result = linkedMapOf("order_id" to "A-1", "refund_amount" to "$129.50", "secret_hash" to "zz", "items" to "kettle", "purchase_date" to "d", "return_window" to "30 days")
        assertEquals("order_id=A-1;purchase_date=d;items=kettle;return_window=30 days;refund_amount=$129.50", render(shrink("lookup_order", result)))
        assertEquals(mapOf("tier" to "gold"), shrink("lookup_customer", mapOf("tier" to "gold", "x" to "1")))
    }

    @Test
    fun tokensRoundUpByFourCharacters() {
        assertEquals(listOf(0, 1, 2), listOf(tokens(""), tokens("abcd"), tokens("abcde")))
    }

    @Test
    fun theFactsBlockCarriesTheValueAndTheDayItWasRead() {
        assertEquals("## Case facts\nrefund_amount: $129.50 (as of day 118)", caseFactsBlock(listOf(Fact("refund_amount", "$129.50", 118))))
    }

    @Test
    fun thePromptPutsKeyFactsFirstAndTheQuestionLast() {
        val prompt = assemble("## Case facts\nx: 1 (as of day 1)", listOf("f1"), listOf(Document("Doc", "text")), "Q?")
        assertEquals(listOf("## Case facts", "## Key findings", "## Documents", "### Doc", "## Question"), prompt.lines().filter { it.startsWith("#") })
        assertTrue(prompt.endsWith("Q?"))
    }

    @Test
    fun aValueOlderThanTheLimitIsReadAgain() {
        assertEquals(listOf(true, false, false), listOf(stale(118, 125, 3), stale(124, 125, 3), stale(122, 125, 3)))
    }
}
