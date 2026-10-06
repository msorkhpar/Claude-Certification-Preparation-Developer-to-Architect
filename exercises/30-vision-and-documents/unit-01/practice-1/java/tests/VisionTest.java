import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class VisionTest {
    private static final String OPUS = "claude-opus-5-5";
    private static final String HAIKU = "claude-haiku-4-5";
    private static final int MIB = Vision.MIB;

    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    private static Map<String, Object> img(String name, int w, int h, String mediaType, int size, String source, String value) {
        return map("kind", "image", "name", name, "media_type", mediaType, "width", w, "height", h, "size", size, "source", source, "value", value);
    }

    private static Map<String, Object> img(String name, int w, int h) {
        return img(name, w, h, "image/png", 1000, "base64", "AAAA");
    }

    private static Map<String, Object> pdf(String name, int pages, int size, String source, String value) {
        return map("kind", "pdf", "name", name, "media_type", "application/pdf", "pages", pages, "size", size, "source", source, "value", value);
    }

    private static Map<String, Object> pdf(String name, int pages) {
        return pdf(name, pages, 1000, "base64", "JVBER");
    }

    @SafeVarargs
    private static List<Map<String, Object>> items(Map<String, Object>... its) {
        return new ArrayList<>(List.of(its));
    }

    private static List<Map<String, Object>> images(int count) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (int n = 0; n < count; n++) out.add(img("i" + n, 100, 100));
        return out;
    }

    private static Map<String, Object> orEmpty(Map<String, Object> m) {
        return m == null ? new LinkedHashMap<>() : m;
    }

    /** The RequestError field the call throws, "crash" for another exception, null when it returns. */
    private static String failureOf(Runnable fn) {
        try {
            fn.run();
        } catch (RequestError e) {
            return e.field();
        } catch (RuntimeException e) {
            return "crash";
        }
        return null;
    }

    private static String fail(String model, List<Map<String, Object>> its, String q) {
        return failureOf(() -> Vision.planRequest(model, its, q));
    }

    private static String fail(String model, List<Map<String, Object>> its, boolean exact, String platform) {
        return failureOf(() -> Vision.planRequest(model, its, "q", exact, platform));
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> contentOf(Map<String, Object> plan) {
        Object c = plan.get("content");
        return c == null ? new ArrayList<>() : (List<Map<String, Object>>) c;
    }

    private static List<Object> types(List<Map<String, Object>> content) {
        List<Object> out = new ArrayList<>();
        for (Map<String, Object> b : content) out.add(b.get("type"));
        return out;
    }

    @Test
    void m1_imagesComeFirstWithLabelsAndTheQuestionLast() {
        Map<String, Object> result = orEmpty(Vision.planRequest(OPUS, items(img("chart", 1000, 1000), img("photo", 200, 200, "image/jpeg", 1000, "url", "https://example.invalid/p.jpg")), "What changed?"));
        assertEquals(List.of(
                map("type", "text", "text", "Image 1:"), map("type", "image", "source", map("type", "base64", "media_type", "image/png", "data", "AAAA")),
                map("type", "text", "text", "Image 2:"), map("type", "image", "source", map("type", "url", "url", "https://example.invalid/p.jpg")),
                map("type", "text", "text", "What changed?")), result.get("content"));
        assertEquals(List.of(1296 + 64, List.of(), 0), List.of(result.getOrDefault("image_tokens", -1), result.getOrDefault("resized", "none"), result.getOrDefault("pdf_pages", -1)));
        Map<String, Object> one = orEmpty(Vision.planRequest(OPUS, items(img("only", 200, 200, "image/png", 1000, "file", "file_abc")), "Describe it."));
        assertEquals(List.of(map("type", "image", "source", map("type", "file", "file_id", "file_abc")), map("type", "text", "text", "Describe it.")), one.get("content"));
    }

    @Test
    void e1_theTokenCostFollowsTheModelsResolutionTier() {
        assertEquals(1296, Vision.visualTokens(1000, 1000));
        assertEquals(64, Vision.visualTokens(200, 200));
        assertArrayEquals(new int[] {1456, 819}, Vision.resizedSize(1920, 1080, "standard"));
        assertArrayEquals(new int[] {819, 1456}, Vision.resizedSize(1080, 1920, "standard"));
        assertArrayEquals(new int[] {924, 1307}, Vision.resizedSize(1075, 1520, "standard"));
        assertArrayEquals(new int[] {2576, 1449}, Vision.resizedSize(3840, 2160, "high"));
        assertArrayEquals(new int[] {1920, 1080}, Vision.resizedSize(1920, 1080, "high"));
        Map<String, Object> full = orEmpty(Vision.planRequest(HAIKU, items(img("shot", 1920, 1080)), "q"));
        assertEquals(List.of(1560, List.of("shot")), List.of(full.getOrDefault("image_tokens", -1), full.getOrDefault("resized", "none")));
        Map<String, Object> wide = orEmpty(Vision.planRequest(OPUS, items(img("shot", 1920, 1080)), "q"));
        assertEquals(List.of(2691, List.of()), List.of(wide.getOrDefault("image_tokens", -1), wide.getOrDefault("resized", "none")));
        assertEquals(1560, orEmpty(Vision.planRequest(HAIKU, items(img("a", 3840, 2160)), "q")).get("image_tokens"));
        assertEquals(4784, orEmpty(Vision.planRequest(OPUS, items(img("a", 3840, 2160)), "q")).get("image_tokens"));
        assertEquals(1551, orEmpty(Vision.planRequest(HAIKU, items(img("scan", 1075, 1520)), "q")).get("image_tokens"));
        assertEquals(2145, orEmpty(Vision.planRequest(OPUS, items(img("scan", 1075, 1520)), "q")).get("image_tokens"));
    }

    @Test
    void e2_formatsDimensionsSizesAndCountsAreCheckedBeforeAnyCall() {
        Map<String, Object> ok = img("ok", 100, 100);
        assertEquals("items[1].media_type", fail(OPUS, items(ok, img("b", 100, 100, "image/bmp", 1000, "base64", "AAAA")), "q"));
        assertEquals("items[0].dimensions", fail(OPUS, items(img("a", 8001, 100)), "q"));
        assertNull(fail(OPUS, items(img("a", 8000, 8000)), "q"));
        assertEquals("items[0].size", fail(OPUS, items(img("a", 100, 100, "image/png", 10 * MIB + 1, "base64", "AAAA")), "q"));
        assertNull(fail(OPUS, items(img("a", 100, 100, "image/png", 10 * MIB, "base64", "AAAA")), "q"));
        assertEquals("items", fail(HAIKU, images(101), "q"));
        assertNull(fail(OPUS, images(101), "q"));
        assertEquals("items", fail(OPUS, images(601), "q"));
        assertEquals("model", fail("claude-unknown-1", items(ok), "q"));
        assertEquals("question", fail(OPUS, items(ok), "  "));
    }

    @Test
    void e3_moreThanTwentyImagesLowerTheSideLimitTo2000Pixels() {
        List<Map<String, Object>> twenty = images(20);
        Map<String, Object> big = img("big", 2500, 100);
        List<Map<String, Object>> a = new ArrayList<>(twenty.subList(0, 19));
        a.add(big);
        assertNull(fail(OPUS, a, false, "api"));
        List<Map<String, Object>> four = new ArrayList<>(twenty.subList(0, 4));
        four.add(big);
        four.addAll(twenty.subList(5, 20));
        four.add(img("extra", 100, 100));
        assertEquals("items[4].dimensions", fail(OPUS, four, false, "api"));
        List<Map<String, Object>> edge = new ArrayList<>(twenty.subList(0, 19));
        edge.add(img("edge", 2000, 100));
        edge.add(img("edge2", 100, 2000));
        assertNull(fail(OPUS, edge, false, "api"));
        List<Map<String, Object>> withPdf = new ArrayList<>(twenty.subList(0, 19));
        withPdf.add(big);
        withPdf.add(pdf("doc", 3));
        assertNull(fail(OPUS, withPdf, false, "api"));
        List<Map<String, Object>> cloud = new ArrayList<>(List.of(pdf("doc", 3)));
        cloud.addAll(twenty.subList(0, 19));
        cloud.add(img("w", 2500, 100));
        assertEquals("items[20].dimensions", fail(OPUS, cloud, false, "bedrock"));
    }

    @Test
    void e4_anImageWhoseSizeMattersIsRejectedInsteadOfResized() {
        Map<String, Object> exact = orEmpty(Vision.planRequest(OPUS, items(img("shot", 1000, 1000)), "Where is the button?", true, "api"));
        List<Map<String, Object>> content = contentOf(exact);
        assertEquals(map("oversized_image", "error"), content.isEmpty() ? null : content.get(0).get("transformations"));
        List<Map<String, Object>> plain = contentOf(orEmpty(Vision.planRequest(OPUS, items(img("shot", 1000, 1000)), "q")));
        assertFalse(plain.isEmpty() || plain.get(0).containsKey("transformations") || plain.get(0).get("type") == null);
        assertEquals("items[1].dimensions", fail(HAIKU, items(img("a", 100, 100), img("shot", 1920, 1080)), true, "api"));
        assertNull(fail(OPUS, items(img("shot", 1920, 1080)), true, "api"));
        assertEquals(List.of("shot"), orEmpty(Vision.planRequest(HAIKU, items(img("shot", 1920, 1080)), "q")).get("resized"));
    }

    @Test
    void e5_pdfsBecomeDocumentBlocksInOrderAndAreLimitedByPages() {
        Map<String, Object> result = orEmpty(Vision.planRequest(OPUS, items(pdf("report", 12), img("logo", 200, 200)), "Summarise."));
        List<Map<String, Object>> content = contentOf(result);
        assertEquals(List.of("document", "image", "text"), types(content));
        assertEquals(map("type", "document", "source", map("type", "base64", "media_type", "application/pdf", "data", "JVBER")), content.get(0));
        assertEquals(12, result.get("pdf_pages"));
        List<Map<String, Object>> link = contentOf(orEmpty(Vision.planRequest(OPUS, items(pdf("report", 3, 1000, "url", "https://example.invalid/r.pdf")), "q")));
        assertEquals(map("type", "document", "source", map("type", "url", "url", "https://example.invalid/r.pdf")), link.isEmpty() ? null : link.get(0));
        assertEquals("items", fail(OPUS, items(pdf("a", 300), pdf("b", 301)), "q"));
        assertNull(fail(OPUS, items(pdf("a", 300), pdf("b", 300)), "q"));
        assertEquals("items", fail(HAIKU, items(pdf("a", 101)), "q"));
        assertNull(fail(HAIKU, items(pdf("a", 100)), "q"));
        Map<String, Object> bad = pdf("a", 3);
        bad.put("media_type", "text/plain");
        assertEquals("items[1].media_type", fail(OPUS, items(pdf("ok", 1), bad), "q"));
        assertEquals("items", fail(OPUS, items(pdf("a", 5, 20 * MIB, "base64", "JVBER"), pdf("b", 5, 13 * MIB, "base64", "JVBER")), "q"));
    }

    @Test
    void e6_bedrockAndVertexTakeOnlyBase64SourcesAndSmallerImages() {
        Map<String, Object> link = img("a", 100, 100, "image/png", 1000, "url", "https://example.invalid/a.png");
        assertNull(fail(OPUS, items(link), false, "api"));
        assertEquals("items[1].source", fail(OPUS, items(img("ok", 100, 100), link), false, "bedrock"));
        assertEquals("items[0].source", fail(OPUS, items(img("f", 100, 100, "image/png", 1000, "file", "file_1")), false, "vertex"));
        Map<String, Object> six = img("six", 100, 100, "image/png", 6 * MIB, "base64", "AAAA");
        assertNull(fail(OPUS, items(img("ok", 100, 100), six), false, "api"));
        assertEquals("items[1].size", fail(OPUS, items(img("ok", 100, 100), six), false, "bedrock"));
        assertNull(fail(OPUS, items(img("five", 100, 100, "image/png", 5 * MIB, "base64", "AAAA")), false, "vertex"));
    }

    @Test
    void e7_coordinatesMapBackToTheOriginalAndCostFollowsThePrice() {
        assertArrayEquals(new double[] {537.5, 760.0}, Vision.toOriginalCoordinates(462, 653.5, 1075, 1520, HAIKU));
        assertArrayEquals(new double[] {462.0, 653.5}, Vision.toOriginalCoordinates(462, 653.5, 1075, 1520, OPUS));
        assertArrayEquals(new double[] {1075.0, 0.0}, Vision.toOriginalCoordinates(2000, -5, 1075, 1520, HAIKU));
        assertArrayEquals(new double[] {500.0, 400.0}, Vision.toOriginalCoordinates(500, 400, 1000, 1000, "claude-haiku-4-5-20251001"));
        assertEquals(0.005184, Vision.imageCostUsd(OPUS, 1296));
        assertEquals(0.001296, Vision.imageCostUsd(HAIKU, 1296));
        assertEquals(0.01, Vision.imageCostUsd("claude-fable-5-1", 1000));
        assertEquals(0.009568, Vision.imageCostUsd("claude-sonnet-5-5", 4784));
    }
}
