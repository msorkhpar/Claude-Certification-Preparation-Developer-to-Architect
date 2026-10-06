import com.fasterxml.jackson.databind.ObjectMapper
import harness.Scripted
import harness.Scripted.map
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class StructuredExtractionTest {
    @Suppress("UNCHECKED_CAST")
    private fun parse(json: String) = ObjectMapper().readValue(json, Map::class.java) as Map<String, Any?>

    @Test
    fun theApiSchemaHasNoNumericConstraintAndKeepsItInTheDescription() {
        val sent = forApi(LOCAL_SCHEMA)
        assertFalse(sent["properties"]["total"].has("minimum"))
        assertTrue("minimum 0" in sent["properties"]["total"]["description"].asText())
        assertEquals(0, LOCAL_SCHEMA["properties"]["total"]["minimum"].asInt())
    }

    @Test
    fun aValueBelowTheMinimumIsRejectedByTheProgram() {
        assertTrue("$.total: must be a number of at least 0" in problems(parse(body(map("total", -5))), DOC))
    }

    @Test
    fun enumCapitalisationIsNormalisedBeforeTheEnumCheck() {
        assertEquals("EUR", normaliseEnum(map("currency", "Eur"))["currency"])
        assertEquals("XYZ", normaliseEnum(map("currency", "XYZ"))["currency"])
    }

    @Test
    fun anInventedQuotationIsSentBackAndTheSecondReplyIsAccepted() {
        val rig = Scripted.client(REPLIES[1], REPLIES[2])
        val result = extract(rig.client(), DOC)
        assertEquals("ok", result["status"])
        assertEquals(2, result["attempts"])
        assertEquals(listOf("user", "assistant", "user"), rig.http().requests[1]["messages"].map { it["role"].asText() })
    }

    @Test
    fun aRefusalAndACutOffReplyAreNotRetried() {
        val rig = Scripted.client(REPLIES[3], REPLIES[4])
        assertEquals("refused", extract(rig.client(), DOC)["status"])
        assertEquals("truncated", extract(rig.client(), DOC)["status"])
        assertEquals(2, rig.http().requests.size)
    }
}
