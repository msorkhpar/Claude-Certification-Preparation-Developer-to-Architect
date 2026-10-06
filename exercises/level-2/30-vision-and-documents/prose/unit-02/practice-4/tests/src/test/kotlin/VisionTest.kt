import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class VisionTest {
    private val opus = "claude-opus-5-5"
    private val haiku = "claude-haiku-4-5"

    private fun img(name: String, w: Int, h: Int, mediaType: String = "image/png", size: Int = 1000, source: String = "base64", value: String = "AAAA"): Map<String, Any?> =
        linkedMapOf("kind" to "image", "name" to name, "media_type" to mediaType, "width" to w, "height" to h, "size" to size, "source" to source, "value" to value)

    private fun pdf(name: String, pages: Int, size: Int = 1000, source: String = "base64", value: String = "JVBER"): MutableMap<String, Any?> =
        linkedMapOf("kind" to "pdf", "name" to name, "media_type" to "application/pdf", "pages" to pages, "size" to size, "source" to source, "value" to value)

    private fun images(count: Int) = (0 until count).map { img("i$it", 100, 100) }

    private fun plan(model: String, items: List<Map<String, Any?>>, q: String, exact: Boolean = false, platform: String = "api"): Map<String, Any?> =
        planRequest(model, items, q, exact, platform) ?: emptyMap()

    /** The RequestError field the call throws, "crash" for another exception, null when it returns. */
    private fun failureOf(fn: () -> Unit): String? {
        try {
            fn()
        } catch (e: RequestError) {
            return e.field
        } catch (e: RuntimeException) {
            return "crash"
        }
        return null
    }

    private fun fail(model: String, items: List<Map<String, Any?>>, q: String = "q", exact: Boolean = false, platform: String = "api") =
        failureOf { planRequest(model, items, q, exact, platform) }

    @Suppress("UNCHECKED_CAST")
    private fun contentOf(plan: Map<String, Any?>): List<Map<String, Any?>> = (plan["content"] as List<Map<String, Any?>>?) ?: emptyList()

    @Test
    fun m1_imagesComeFirstWithLabelsAndTheQuestionLast() {
        val result = plan(opus, listOf(img("chart", 1000, 1000), img("photo", 200, 200, "image/jpeg", 1000, "url", "https://example.invalid/p.jpg")), "What changed?")
        assertEquals(listOf(
            mapOf("type" to "text", "text" to "Image 1:"), mapOf("type" to "image", "source" to mapOf("type" to "base64", "media_type" to "image/png", "data" to "AAAA")),
            mapOf("type" to "text", "text" to "Image 2:"), mapOf("type" to "image", "source" to mapOf("type" to "url", "url" to "https://example.invalid/p.jpg")),
            mapOf("type" to "text", "text" to "What changed?")), result["content"])
        assertEquals(listOf<Any?>(1296 + 64, listOf<Any?>(), 0), listOf(result["image_tokens"], result["resized"], result["pdf_pages"]))
        val one = plan(opus, listOf(img("only", 200, 200, "image/png", 1000, "file", "file_abc")), "Describe it.")
        assertEquals(listOf(mapOf("type" to "image", "source" to mapOf("type" to "file", "file_id" to "file_abc")), mapOf("type" to "text", "text" to "Describe it.")), one["content"])
    }

    @Test
    fun e1_theTokenCostFollowsTheModelsResolutionTier() {
        assertEquals(1296, visualTokens(1000, 1000))
        assertEquals(64, visualTokens(200, 200))
        assertEquals(Pair(1456, 819), resizedSize(1920, 1080, "standard"))
        assertEquals(Pair(819, 1456), resizedSize(1080, 1920, "standard"))
        assertEquals(Pair(924, 1307), resizedSize(1075, 1520, "standard"))
        assertEquals(Pair(2576, 1449), resizedSize(3840, 2160, "high"))
        assertEquals(Pair(1920, 1080), resizedSize(1920, 1080, "high"))
        val full = plan(haiku, listOf(img("shot", 1920, 1080)), "q")
        assertEquals(listOf<Any?>(1560, listOf("shot")), listOf(full["image_tokens"], full["resized"]))
        val wide = plan(opus, listOf(img("shot", 1920, 1080)), "q")
        assertEquals(listOf<Any?>(2691, listOf<Any?>()), listOf(wide["image_tokens"], wide["resized"]))
        assertEquals(1560, plan(haiku, listOf(img("a", 3840, 2160)), "q")["image_tokens"])
        assertEquals(4784, plan(opus, listOf(img("a", 3840, 2160)), "q")["image_tokens"])
        assertEquals(1551, plan(haiku, listOf(img("scan", 1075, 1520)), "q")["image_tokens"])
        assertEquals(2145, plan(opus, listOf(img("scan", 1075, 1520)), "q")["image_tokens"])
    }

    @Test
    fun e2_formatsDimensionsSizesAndCountsAreCheckedBeforeAnyCall() {
        val ok = img("ok", 100, 100)
        assertEquals("items[1].media_type", fail(opus, listOf(ok, img("b", 100, 100, "image/bmp"))))
        assertEquals("items[0].dimensions", fail(opus, listOf(img("a", 8001, 100))))
        assertNull(fail(opus, listOf(img("a", 8000, 8000))))
        assertEquals("items[0].size", fail(opus, listOf(img("a", 100, 100, size = 10 * MIB + 1))))
        assertNull(fail(opus, listOf(img("a", 100, 100, size = 10 * MIB))))
        assertEquals("items", fail(haiku, images(101)))
        assertNull(fail(opus, images(101)))
        assertEquals("items", fail(opus, images(601)))
        assertEquals("model", fail("claude-unknown-1", listOf(ok)))
        assertEquals("question", fail(opus, listOf(ok), "  "))
    }

    @Test
    fun e3_moreThanTwentyImagesLowerTheSideLimitTo2000Pixels() {
        val twenty = images(20)
        val big = img("big", 2500, 100)
        assertNull(fail(opus, twenty.take(19) + big))
        assertEquals("items[4].dimensions", fail(opus, twenty.take(4) + big + twenty.drop(5) + img("extra", 100, 100)))
        assertNull(fail(opus, twenty.take(19) + img("edge", 2000, 100) + img("edge2", 100, 2000)))
        assertNull(fail(opus, twenty.take(19) + big + pdf("doc", 3)))
        assertEquals("items[20].dimensions", fail(opus, listOf(pdf("doc", 3)) + twenty.take(19) + img("w", 2500, 100), platform = "bedrock"))
    }

    @Test
    fun e4_anImageWhoseSizeMattersIsRejectedInsteadOfResized() {
        val exact = contentOf(plan(opus, listOf(img("shot", 1000, 1000)), "Where is the button?", exact = true))
        assertEquals(mapOf("oversized_image" to "error"), exact.firstOrNull()?.get("transformations"))
        val plain = contentOf(plan(opus, listOf(img("shot", 1000, 1000)), "q"))
        assertFalse(plain.isEmpty() || plain[0].containsKey("transformations") || plain[0]["type"] == null)
        assertEquals("items[1].dimensions", fail(haiku, listOf(img("a", 100, 100), img("shot", 1920, 1080)), exact = true))
        assertNull(fail(opus, listOf(img("shot", 1920, 1080)), exact = true))
        assertEquals(listOf("shot"), plan(haiku, listOf(img("shot", 1920, 1080)), "q")["resized"])
    }

    @Test
    fun e5_pdfsBecomeDocumentBlocksInOrderAndAreLimitedByPages() {
        val result = plan(opus, listOf(pdf("report", 12), img("logo", 200, 200)), "Summarise.")
        val content = contentOf(result)
        assertEquals(listOf<Any?>("document", "image", "text"), content.map { it["type"] })
        assertEquals(mapOf("type" to "document", "source" to mapOf("type" to "base64", "media_type" to "application/pdf", "data" to "JVBER")), content.firstOrNull())
        assertEquals(12, result["pdf_pages"])
        val link = contentOf(plan(opus, listOf(pdf("report", 3, source = "url", value = "https://example.invalid/r.pdf")), "q"))
        assertEquals(mapOf("type" to "document", "source" to mapOf("type" to "url", "url" to "https://example.invalid/r.pdf")), link.firstOrNull())
        assertEquals("items", fail(opus, listOf(pdf("a", 300), pdf("b", 301))))
        assertNull(fail(opus, listOf(pdf("a", 300), pdf("b", 300))))
        assertEquals("items", fail(haiku, listOf(pdf("a", 101))))
        assertNull(fail(haiku, listOf(pdf("a", 100))))
        val bad = pdf("a", 3)
        bad["media_type"] = "text/plain"
        assertEquals("items[1].media_type", fail(opus, listOf(pdf("ok", 1), bad)))
        assertEquals("items", fail(opus, listOf(pdf("a", 5, 20 * MIB), pdf("b", 5, 13 * MIB))))
    }

    @Test
    fun e6_bedrockAndVertexTakeOnlyBase64SourcesAndSmallerImages() {
        val link = img("a", 100, 100, source = "url", value = "https://example.invalid/a.png")
        assertNull(fail(opus, listOf(link)))
        assertEquals("items[1].source", fail(opus, listOf(img("ok", 100, 100), link), platform = "bedrock"))
        assertEquals("items[0].source", fail(opus, listOf(img("f", 100, 100, source = "file", value = "file_1")), platform = "vertex"))
        val six = img("six", 100, 100, size = 6 * MIB)
        assertNull(fail(opus, listOf(img("ok", 100, 100), six)))
        assertEquals("items[1].size", fail(opus, listOf(img("ok", 100, 100), six), platform = "bedrock"))
        assertNull(fail(opus, listOf(img("five", 100, 100, size = 5 * MIB)), platform = "vertex"))
    }

    @Test
    fun e7_coordinatesMapBackToTheOriginalAndCostFollowsThePrice() {
        assertEquals(Pair(537.5, 760.0), toOriginalCoordinates(462.0, 653.5, 1075, 1520, haiku))
        assertEquals(Pair(462.0, 653.5), toOriginalCoordinates(462.0, 653.5, 1075, 1520, opus))
        assertEquals(Pair(1075.0, 0.0), toOriginalCoordinates(2000.0, -5.0, 1075, 1520, haiku))
        assertEquals(Pair(500.0, 400.0), toOriginalCoordinates(500.0, 400.0, 1000, 1000, "claude-haiku-4-5-20251001"))
        assertEquals(0.005184, imageCostUsd(opus, 1296))
        assertEquals(0.001296, imageCostUsd(haiku, 1296))
        assertEquals(0.01, imageCostUsd("claude-fable-5-1", 1000))
        assertEquals(0.009568, imageCostUsd("claude-sonnet-5-5", 4784))
    }
}
