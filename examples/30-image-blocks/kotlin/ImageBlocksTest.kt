import com.anthropic.core.jsonMapper
import com.anthropic.models.messages.ContentBlockParam
import com.fasterxml.jackson.databind.JsonNode
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ImageBlocksTest {
    @Test
    fun theDocumentationTableForTwoSizesOnBothTiers() {
        assertEquals(1456 to 819, resizedSize(1920, 1080, "standard"))
        assertEquals(1560, visualTokens(1456, 819))
        assertEquals(1920 to 1080, resizedSize(1920, 1080, "high"))
        assertEquals(2691, visualTokens(1920, 1080))
        assertEquals(2576 to 1449, resizedSize(3840, 2160, "high"))
        assertEquals(4784, visualTokens(2576, 1449))
        assertEquals(200 to 200, resizedSize(200, 200, "standard"))
        assertEquals(64, visualTokens(200, 200))
    }

    @Test
    fun aScanUnderTheEdgeLimitIsStillResizedByTheTokenBudget() {
        assertEquals(2145, visualTokens(1075, 1520))
        assertEquals(924 to 1307, resizedSize(1075, 1520, "standard"))
        assertEquals(924 to 1316, padded(924, 1307))
        assertEquals(1075 to 1520, resizedSize(1075, 1520, "high"))
    }

    @Test
    fun theThreeSourcesAndTheLabelOrder() {
        val blocks = listOf(imageBlock("base64", "AAAA", "image/jpeg"), imageBlock("url", "https://example.invalid/a.png"), imageBlock("file", "file_1"))
        val content = comparison(blocks, "Which is larger?")
        assertEquals(listOf("text", "image/base64", "text", "image/url", "text", "image/file", "text"), content.map { shape(it) })
        assertEquals(listOf("Image 1:", "Image 2:", "Image 3:", "Which is larger?"), content.filter { it.isText() }.map { it.asText().text() })
        val doc = jsonMapper().valueToTree<JsonNode>(documentBlock("file", "file_2", "T"))
        assertEquals("document", doc["type"].asText())
        assertEquals("file", doc.at("/source/type").asText())
        assertEquals("file_2", doc.at("/source/file_id").asText())
        assertEquals("T", doc["title"].asText())
    }

    @Test
    fun aCoordinateMapsBackByTheResizedSizeAndNotThePaddedOne() {
        val (x, y) = toOriginal(462, 654, 1075, 1520, "standard")
        assertEquals(537.5, Math.round(x * 10) / 10.0)
        assertEquals(760.6, Math.round(y * 10) / 10.0)
        assertEquals(10.0 to 20.0, toOriginal(10, 20, 100, 100, "standard"))
    }
}
