import com.fasterxml.jackson.databind.JsonNode
import com.anthropic.models.messages.Base64ImageSource
import com.anthropic.models.messages.Base64PdfSource
import com.anthropic.models.messages.ContentBlockParam
import com.anthropic.models.messages.DocumentBlockParam
import com.anthropic.models.messages.FileDocumentSource
import com.anthropic.models.messages.FileImageSource
import com.anthropic.models.messages.ImageBlockParam
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.Model
import com.anthropic.models.messages.UrlImageSource
import com.anthropic.models.messages.UrlPdfSource
import harness.Scripted
import harness.Scripted.message
import harness.Scripted.text
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

private val log = System.getLogger("image_blocks")

/**
 * Image and document blocks: three kinds of source, a labelled comparison, the resize rule, the cost and the way back.
 *
 * The model reply is illustrative, a hand-written body in the shape of the Messages API (claude-sonnet-5-5), not a capture. The resize
 * rule is the reference implementation of the "Coordinates and bounding boxes" page of the Claude documentation, checked on 2026-10-03.
 */
const val MODEL = "claude-sonnet-5-5"

/** tier to (longest edge in pixels, visual token budget) */
val TIERS = mapOf("standard" to (1568 to 1568), "high" to (2576 to 4784))
const val PRICE = 2.0 // dollars per million input tokens for claude-sonnet-5-5 (docs/VERSIONS.md)

private fun ceilDiv(a: Int, b: Int) = (a + b - 1) / b

fun visualTokens(width: Int, height: Int) = ceilDiv(width, 28) * ceilDiv(height, 28)

/** Claude pads every image up to the next multiple of 28 on the bottom and the right; the padding holds no content. */
fun padded(width: Int, height: Int) = ceilDiv(width, 28) * 28 to ceilDiv(height, 28) * 28

/** The size Claude sees: the largest aspect-preserving size within the tier's edge limit and token budget. */
fun resizedSize(width: Int, height: Int, tier: String): Pair<Int, Int> {
    val (maxEdge, maxTokens) = TIERS.getValue(tier)
    fun fits(w: Int, h: Int) = padded(w, h).toList().max() <= maxEdge && visualTokens(w, h) <= maxTokens
    if (fits(width, height)) return width to height
    if (height > width) return resizedSize(height, width, tier).let { (h, w) -> w to h }
    val ratio = width.toDouble() / height
    for (longEdge in width - 1 downTo 1) {
        val short = max(Math.rint(longEdge / ratio).toInt(), 1) // rint rounds halves to even, as Python's round does
        if (fits(longEdge, short)) return longEdge to short
    }
    return 1 to 1
}

/** An image block with one of the three sources of the Messages API: base64, url or file (a Files API id). */
fun imageBlock(kind: String, value: String, mediaType: String = "image/png"): ContentBlockParam {
    val source = when (kind) {
        "base64" -> ImageBlockParam.Source.ofBase64(Base64ImageSource.builder().data(value).mediaType(Base64ImageSource.MediaType.of(mediaType)).build())
        "url" -> ImageBlockParam.Source.ofUrl(UrlImageSource.builder().url(value).build())
        "file" -> ImageBlockParam.Source.ofFile(FileImageSource.builder().fileId(value).build())
        else -> throw IllegalArgumentException(kind)
    }
    return ContentBlockParam.ofImage(ImageBlockParam.builder().source(source).build())
}

fun documentBlock(kind: String, value: String, title: String? = null): ContentBlockParam {
    val source = when (kind) {
        "base64" -> DocumentBlockParam.Source.ofBase64(Base64PdfSource.builder().data(value).build())
        "url" -> DocumentBlockParam.Source.ofUrl(UrlPdfSource.builder().url(value).build())
        "file" -> DocumentBlockParam.Source.ofFile(FileDocumentSource.builder().fileId(value).build())
        else -> throw IllegalArgumentException(kind)
    }
    return ContentBlockParam.ofDocument(DocumentBlockParam.builder().source(source).apply { title?.let { title(it) } }.build())
}

/** Several images are each introduced by a label, and the question comes last. */
fun comparison(images: List<ContentBlockParam>, question: String): MutableList<ContentBlockParam> {
    val content = mutableListOf<ContentBlockParam>()
    for ((i, image) in images.withIndex()) content += listOf(ContentBlockParam.ofText("Image ${i + 1}:"), image)
    content += ContentBlockParam.ofText(question)
    return content
}

/** A point on the picture Claude saw, as a point on the original: divide by the resized size, never by the padded one. */
fun toOriginal(x: Int, y: Int, width: Int, height: Int, tier: String): Pair<Double, Double> {
    val (rw, rh) = resizedSize(width, height, tier)
    return x.toDouble() / rw * width to y.toDouble() / rh * height
}

fun shape(block: ContentBlockParam): String = when {
    block.isText() -> "text"
    block.isImage() -> block.asImage().source().let { "image/" + if (it.isBase64()) "base64" else if (it.isUrl()) "url" else "file" }
    else -> block.asDocument().source().let { "document/" + if (it.isBase64()) "base64" else if (it.isUrl()) "url" else if (it.isFile()) "file" else "other" }
}

private fun size(wh: Pair<Int, Int>) = "${wh.first}x${wh.second}"

/** The SDK's JSON mapper sorts keys alphabetically; the block is shown with the keys in the order the API documents them: type first. */
fun inWireOrder(node: JsonNode): String {
    val order = listOf("type", "source", "media_type", "data")
    if (!node.isObject) return node.toString()
    val keys = node.fieldNames().asSequence().sortedWith(compareBy({ order.indexOf(it).let { i -> if (i < 0) order.size else i } }, { it }))
    return keys.joinToString(",", "{", "}") { "\"$it\":${inWireOrder(node[it])}" }
}

fun main() {
    val rig = Scripted.client(message(listOf(text("Image 1 has the larger bars; image 2 matches the table in the PDF."))))
    val content = comparison(
        listOf(imageBlock("base64", "iVBORw0KGgo="), imageBlock("url", "https://example.invalid/chart.png"), imageBlock("file", "file_011CNha8iCJcU1wXNR6q4V8w")),
        "Compare the images with the table in the document.",
    )
    content.add(content.size - 1, documentBlock("file", "file_011CPMxVD3fHLUhvTqtsQA5w", "Quarterly table"))
    val reply = rig.client().messages().create(MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(300).addUserMessageOfBlockParams(content).build())
    val sent = rig.http().requests[0].at("/messages/0/content")
    println("content order: ${content.joinToString(", ") { shape(it) }}")
    println("labels: ${content.filter { it.isText() }.map { it.asText().text() }.filter { it.startsWith("Image") }.joinToString(" ")}")
    println("one image block: ${inWireOrder(sent[1])}")
    println("reply: ${reply.content()[0].asText().text()}")
    println()
    println("size         tier       seen        padded      tokens  dollars per 1000 images")
    for ((width, height) in listOf(200 to 200, 1920 to 1080, 3840 to 2160, 1075 to 1520)) {
        for (tier in listOf("standard", "high")) {
            val seen = resizedSize(width, height, tier)
            val tokens = visualTokens(seen.first, seen.second)
            println("%-12s %-10s %-11s %-11s %6d  %.2f".format("${width}x$height", tier, size(seen), size(padded(seen.first, seen.second)), tokens, tokens * PRICE / 1000))
        }
    }
    println()
    val (x, y) = toOriginal(462, 654, 1075, 1520, "standard")
    val (pw, ph) = padded(resizedSize(1075, 1520, "standard").first, resizedSize(1075, 1520, "standard").second)
    val wrongX = 462.0 / pw * 1075
    val wrongY = 654.0 / ph * 1520
    println("point (462, 654) on the picture Claude saw of a 1075x1520 scan: (${"%.1f".format(x)}, ${"%.1f".format(y)}) on the original")
    println("dividing by the padded size instead gives (${"%.1f".format(wrongX)}, ${"%.1f".format(wrongY)}), ${"%.1f".format(abs(y - wrongY))} pixels off")
}
