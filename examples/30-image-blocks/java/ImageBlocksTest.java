import static org.junit.jupiter.api.Assertions.*;

import com.anthropic.core.ObjectMappers;
import com.anthropic.models.messages.ContentBlockParam;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.junit.jupiter.api.Test;

class ImageBlocksTest {
    @Test
    void theDocumentationTableForTwoSizesOnBothTiers() {
        assertArrayEquals(new int[] {1456, 819}, ImageBlocks.resizedSize(1920, 1080, "standard"));
        assertEquals(1560, ImageBlocks.visualTokens(1456, 819));
        assertArrayEquals(new int[] {1920, 1080}, ImageBlocks.resizedSize(1920, 1080, "high"));
        assertEquals(2691, ImageBlocks.visualTokens(1920, 1080));
        assertArrayEquals(new int[] {2576, 1449}, ImageBlocks.resizedSize(3840, 2160, "high"));
        assertEquals(4784, ImageBlocks.visualTokens(2576, 1449));
        assertArrayEquals(new int[] {200, 200}, ImageBlocks.resizedSize(200, 200, "standard"));
        assertEquals(64, ImageBlocks.visualTokens(200, 200));
    }

    @Test
    void aScanUnderTheEdgeLimitIsStillResizedByTheTokenBudget() {
        assertEquals(2145, ImageBlocks.visualTokens(1075, 1520));
        assertArrayEquals(new int[] {924, 1307}, ImageBlocks.resizedSize(1075, 1520, "standard"));
        assertArrayEquals(new int[] {924, 1316}, ImageBlocks.padded(924, 1307));
        assertArrayEquals(new int[] {1075, 1520}, ImageBlocks.resizedSize(1075, 1520, "high"));
    }

    @Test
    void theThreeSourcesAndTheLabelOrder() throws Exception {
        List<ContentBlockParam> blocks = List.of(ImageBlocks.imageBlock("base64", "AAAA", "image/jpeg"), ImageBlocks.imageBlock("url", "https://example.invalid/a.png"), ImageBlocks.imageBlock("file", "file_1"));
        List<ContentBlockParam> content = ImageBlocks.comparison(blocks, "Which is larger?");
        assertEquals(List.of("text", "image/base64", "text", "image/url", "text", "image/file", "text"), content.stream().map(ImageBlocks::shape).toList());
        assertEquals(List.of("Image 1:", "Image 2:", "Image 3:", "Which is larger?"), content.stream().filter(ContentBlockParam::isText).map(b -> b.asText().text()).toList());
        JsonNode doc = ObjectMappers.jsonMapper().valueToTree(ImageBlocks.documentBlock("file", "file_2", "T"));
        assertEquals("document", doc.get("type").asText());
        assertEquals("file", doc.at("/source/type").asText());
        assertEquals("file_2", doc.at("/source/file_id").asText());
        assertEquals("T", doc.get("title").asText());
    }

    @Test
    void aCoordinateMapsBackByTheResizedSizeAndNotThePaddedOne() {
        double[] xy = ImageBlocks.toOriginal(462, 654, 1075, 1520, "standard");
        assertEquals(537.5, Math.round(xy[0] * 10) / 10.0);
        assertEquals(760.6, Math.round(xy[1] * 10) / 10.0);
        double[] same = ImageBlocks.toOriginal(10, 20, 100, 100, "standard");
        assertEquals(10.0, same[0]);
        assertEquals(20.0, same[1]);
    }
}
