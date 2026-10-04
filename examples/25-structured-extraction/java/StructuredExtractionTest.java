import static harness.Scripted.map;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import harness.Scripted;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StructuredExtractionTest {
    @SuppressWarnings("unchecked")
    private static Map<String, Object> parse(String json) throws Exception {
        return new ObjectMapper().readValue(json, Map.class);
    }

    @Test
    void theApiSchemaHasNoNumericConstraintAndKeepsItInTheDescription() {
        JsonNode sent = StructuredExtraction.forApi(StructuredExtraction.LOCAL_SCHEMA);
        assertFalse(sent.get("properties").get("total").has("minimum"));
        assertTrue(sent.get("properties").get("total").get("description").asText().contains("minimum 0"));
        assertEquals(0, StructuredExtraction.LOCAL_SCHEMA.get("properties").get("total").get("minimum").asInt());
    }

    @Test
    void aValueBelowTheMinimumIsRejectedByTheProgram() throws Exception {
        assertTrue(StructuredExtraction.problems(parse(StructuredExtraction.body(map("total", -5))), StructuredExtraction.DOC).contains("$.total: must be a number of at least 0"));
    }

    @Test
    void enumCapitalisationIsNormalisedBeforeTheEnumCheck() {
        assertEquals("EUR", StructuredExtraction.normaliseEnum(map("currency", "Eur")).get("currency"));
        assertEquals("XYZ", StructuredExtraction.normaliseEnum(map("currency", "XYZ")).get("currency"));
    }

    @Test
    void anInventedQuotationIsSentBackAndTheSecondReplyIsAccepted() {
        Scripted.Rig rig = Scripted.client(StructuredExtraction.REPLIES.get(1), StructuredExtraction.REPLIES.get(2));
        Map<String, Object> result = StructuredExtraction.extract(rig.client(), StructuredExtraction.DOC);
        assertEquals("ok", result.get("status"));
        assertEquals(2, result.get("attempts"));
        JsonNode second = rig.http().requests.get(1).get("messages");
        assertEquals(List.of("user", "assistant", "user"), List.of(second.get(0).get("role").asText(), second.get(1).get("role").asText(), second.get(2).get("role").asText()));
    }

    @Test
    void aRefusalAndACutOffReplyAreNotRetried() {
        Scripted.Rig rig = Scripted.client(StructuredExtraction.REPLIES.get(3), StructuredExtraction.REPLIES.get(4));
        assertEquals("refused", StructuredExtraction.extract(rig.client(), StructuredExtraction.DOC).get("status"));
        assertEquals("truncated", StructuredExtraction.extract(rig.client(), StructuredExtraction.DOC).get("status"));
        assertEquals(2, rig.http().requests.size());
    }
}
