import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CaseFactsTest {
    @Test
    void shrinkingKeepsTheFieldsTheToolIsUsedForWithExactValues() {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("order_id", "A-1");
        result.put("refund_amount", "$129.50");
        result.put("secret_hash", "zz");
        result.put("items", "kettle");
        result.put("purchase_date", "d");
        result.put("return_window", "30 days");
        assertEquals("order_id=A-1;purchase_date=d;items=kettle;return_window=30 days;refund_amount=$129.50", CaseFacts.render(CaseFacts.shrink("lookup_order", result)));
        assertEquals(Map.of("tier", "gold"), CaseFacts.shrink("lookup_customer", Map.of("tier", "gold", "x", "1")));
    }

    @Test
    void tokensRoundUpByFourCharacters() {
        assertEquals(List.of(0, 1, 2), List.of(CaseFacts.tokens(""), CaseFacts.tokens("abcd"), CaseFacts.tokens("abcde")));
    }

    @Test
    void theFactsBlockCarriesTheValueAndTheDayItWasRead() {
        assertEquals("## Case facts\nrefund_amount: $129.50 (as of day 118)", CaseFacts.caseFactsBlock(List.of(new CaseFacts.Fact("refund_amount", "$129.50", 118))));
    }

    @Test
    void thePromptPutsKeyFactsFirstAndTheQuestionLast() {
        String prompt = CaseFacts.assemble("## Case facts\nx: 1 (as of day 1)", List.of("f1"), List.of(new CaseFacts.Document("Doc", "text")), "Q?");
        assertEquals(List.of("## Case facts", "## Key findings", "## Documents", "### Doc", "## Question"), List.of(prompt.split("\n")).stream().filter(l -> l.startsWith("#")).toList());
        assertTrue(prompt.endsWith("Q?"));
    }

    @Test
    void aValueOlderThanTheLimitIsReadAgain() {
        assertEquals(List.of(true, false, false), List.of(CaseFacts.stale(118, 125, 3), CaseFacts.stale(124, 125, 3), CaseFacts.stale(122, 125, 3)));
    }
}
