import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

private val log = System.getLogger("vision")

/** Planning a request that carries images and PDFs. See ../../statement.md. Items, blocks and results are JSON-like maps. */

const val MIB = 1024 * 1024
val IMAGE_TYPES = listOf("image/jpeg", "image/png", "image/gif", "image/webp")

/** Given: model id to (tier, context window in tokens, input price in dollars per million tokens). */
val MODELS: Map<String, Triple<String, Int, Double>> = mapOf(
    "claude-fable-5-1" to Triple("high", 1_000_000, 10.0),
    "claude-opus-5-5" to Triple("high", 1_000_000, 4.0),
    "claude-sonnet-5-5" to Triple("high", 1_000_000, 2.0),
    "claude-haiku-4-5" to Triple("standard", 200_000, 1.0),
    "claude-haiku-4-5-20251001" to Triple("standard", 200_000, 1.0),
)

/** Given: tier to (longest edge in pixels, visual token budget). */
val TIERS: Map<String, Pair<Int, Int>> = mapOf("standard" to Pair(1568, 1568), "high" to Pair(2576, 4784))

/** What your code throws, before any call, for a request the API would reject; `field` names the part. */
class RequestError(val field: String, reason: String) : RuntimeException("$field: $reason")

fun visualTokens(width: Int, height: Int): Int {
    // TODO 1 of 8 (unlocks e1 and e7): the visual tokens of an image of this size.
    // Receives the width and height in pixels. Returns one token per 28 x 28 pixel patch, each side rounded up to whole patches.
    // Example: visualTokens(29, 28) -> 2
    return 0
}

private fun fits(w: Int, h: Int, maxEdge: Int, maxTokens: Int) =
    ceil(w / 28.0).toInt() * 28 <= maxEdge && ceil(h / 28.0).toInt() * 28 <= maxEdge && visualTokens(w, h) <= maxTokens

/** The (width, height) the model sees: the largest aspect-preserving size within the tier's edge and token limits. */
fun resizedSize(width: Int, height: Int, tier: String): Pair<Int, Int> {
    val (maxEdge, maxTokens) = TIERS.getValue(tier)
    if (fits(width, height, maxEdge, maxTokens)) return Pair(width, height)
    if (height > width) {
        val (h, w) = resizedSize(height, width, tier)
        return Pair(w, h)
    }
    val ratio = width.toDouble() / height
    for (longEdge in width - 1 downTo 1) {
        val short = max(Math.rint(longEdge / ratio).toInt(), 1) // half to even, like the API
        if (fits(longEdge, short, maxEdge, maxTokens)) return Pair(longEdge, short)
    }
    return Pair(1, 1)
}

fun imageCostUsd(model: String, tokens: Int): Double {
    // TODO 2 of 8 (unlocks e7): the input cost in dollars of that many tokens for the model.
    // Receives a model id and a token count. Returns tokens * price / 1,000,000 rounded to 6 decimals (Math.rint); the price is
    // MODELS.getValue(model).third. Example: imageCostUsd("claude-haiku-4-5", 1000) -> 0.001
    return 0.0
}

fun toOriginalCoordinates(x: Double, y: Double, width: Int, height: Int, model: String): Pair<Double, Double> {
    val (rw, rh) = resizedSize(width, height, MODELS.getValue(model).first)
    // TODO 3 of 8 (unlocks e7): a point Claude returned for the image it saw, as a point on the original width x height image.
    // Clamp x and y into the resized size rw x rh, then scale them onto the original size. Example: a 3000 x 2000 image on a
    // "high" model is seen as 2576 x 1717 (not padded), so the point (1288.0, 0.0) becomes about (1500.0, 0.0).
    return Pair(x, y)
}

private fun sourceOf(item: Map<String, Any?>): Map<String, Any?> {
    // TODO 8 of 8 (unlocks m1, e5 and e6): the "source" object of an item.
    // Returns {type: base64, media_type, data} for source "base64", {type: url, url} for "url" and {type: file, file_id} for
    // "file"; the text is item["value"].
    // Example: source "url", value "https://example.invalid/a.png" -> {type=url, url=https://example.invalid/a.png}
    return emptyMap()
}

private fun maxCount(context: Int): Int {
    // TODO 4 of 8 (unlocks e2 and e5): how many images, and how many PDF pages, a request may hold.
    // Receives the model's context window in tokens. Returns 100 when it is under 1,000,000 and 600 otherwise.
    // Example: maxCount(200_000) -> 100
    return Int.MAX_VALUE
}

private fun maxImageSize(cloud: Boolean): Int {
    // TODO 5 of 8 (unlocks e2 and e6): the largest image payload in bytes.
    // Receives true on bedrock and vertex. Returns 10 MiB, or 5 MiB when it is true.
    // Example: maxImageSize(true) -> 5 * MIB
    return Int.MAX_VALUE
}

private fun isMany(images: Int, pdfs: Int, cloud: Boolean): Boolean {
    // TODO 6 of 8 (unlocks e3): does the request hold more than 20 image blocks?
    // Receives the image count, the PDF count and the cloud flag; on bedrock and vertex the PDFs count as well.
    // Example: isMany(18, 3, true) -> true, isMany(18, 3, false) -> false
    return false
}

private fun imageBlocks(item: Map<String, Any?>, n: Int, imageCount: Int, exact: Boolean): List<Map<String, Any?>> {
    // TODO 7 of 8 (unlocks m1 and e4): the content blocks of the n-th image (counting from 1).
    // Returns a text block "Image n:" first when there are two or more images, then the image block {type: image, source:
    // sourceOf(item)}, which also carries transformations: {oversized_image: error} when exact is true.
    // Example: imageBlocks(item, 2, 2, false) -> [{type=text, text=Image 2:}, {type=image, source={...}}]
    return emptyList()
}

private fun num(item: Map<String, Any?>, key: String) = (item[key] as Number).toInt()

fun planRequest(model: String, items: List<Map<String, Any?>>, question: String, exact: Boolean = false, platform: String = "api"): Map<String, Any?> {
    log.log(System.Logger.Level.DEBUG, "planRequest input: {0}", items)
    val spec = MODELS[model] ?: throw RequestError("model", "unknown model $model")
    val (tier, context, _) = spec
    if (question.isBlank()) throw RequestError("question", "the question must be a non-empty string")
    val cloud = platform == "bedrock" || platform == "vertex"
    val images = items.count { it["kind"] == "image" }
    val pdfs = items.count { it["kind"] == "pdf" }
    val pages = items.filter { it["kind"] == "pdf" }.sumOf { num(it, "pages") }
    val limit = maxCount(context)
    if (images > limit) throw RequestError("items", "too many images for $model")
    if (pages > limit) throw RequestError("items", "too many PDF pages for $model")
    if (items.sumOf { num(it, "size").toLong() } > 32L * MIB) throw RequestError("items", "the request would be larger than 32 MiB")
    val many = isMany(images, pdfs, cloud)
    var tokens = 0
    val resized = mutableListOf<Any?>()
    items.forEachIndexed { i, it ->
        if (cloud && it["source"] != "base64") throw RequestError("items[$i].source", "$platform accepts base64 sources only")
        if (it["kind"] == "pdf") {
            if (it["media_type"] != "application/pdf") throw RequestError("items[$i].media_type", "a PDF must be application/pdf")
            return@forEachIndexed
        }
        val w = num(it, "width")
        val h = num(it, "height")
        if (it["media_type"] !in IMAGE_TYPES) throw RequestError("items[$i].media_type", "${it["media_type"]} is not a supported image format")
        if (w > 8000 || h > 8000) throw RequestError("items[$i].dimensions", "an image may not exceed 8000 x 8000 pixels")
        if (num(it, "size") > maxImageSize(cloud)) throw RequestError("items[$i].size", "the image is too large")
        if (many && max(w, h) > 2000) throw RequestError("items[$i].dimensions", "with more than 20 images, no side may exceed 2000 pixels")
        val seen = resizedSize(w, h, tier)
        if (seen != Pair(w, h)) {
            if (exact) throw RequestError("items[$i].dimensions", "would be resized to ${seen.first}x${seen.second}")
            resized.add(it["name"])
        }
        tokens += visualTokens(seen.first, seen.second)
    }
    val content = mutableListOf<Map<String, Any?>>()
    var n = 0
    for (it in items) {
        if (it["kind"] == "image") {
            n++
            content.addAll(imageBlocks(it, n, images, exact))
        } else {
            content.add(mapOf("type" to "document", "source" to sourceOf(it)))
        }
    }
    content.add(mapOf("type" to "text", "text" to question))
    return linkedMapOf("content" to content, "image_tokens" to tokens, "resized" to resized, "pdf_pages" to pages)
}
