import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

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

fun visualTokens(width: Int, height: Int): Int = ceil(width / 28.0).toInt() * ceil(height / 28.0).toInt()

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

fun imageCostUsd(model: String, tokens: Int): Double = Math.rint(tokens * MODELS.getValue(model).third / 1_000_000 * 1e6) / 1e6

fun toOriginalCoordinates(x: Double, y: Double, width: Int, height: Int, model: String): Pair<Double, Double> {
    val (rw, rh) = resizedSize(width, height, MODELS.getValue(model).first)
    val cx = min(max(x, 0.0), rw.toDouble())
    val cy = min(max(y, 0.0), rh.toDouble())
    return Pair(cx / rw * width, cy / (ceil(rh / 28.0).toInt() * 28) * height)
}

private fun sourceOf(item: Map<String, Any?>): Map<String, Any?> {
    val value = item["value"]
    return when (item["source"]) {
        "base64" -> mapOf("type" to "base64", "media_type" to item["media_type"], "data" to value)
        "url" -> mapOf("type" to "url", "url" to value)
        else -> mapOf("type" to "file", "file_id" to value)
    }
}

private fun num(item: Map<String, Any?>, key: String) = (item[key] as Number).toInt()

fun planRequest(model: String, items: List<Map<String, Any?>>, question: String, exact: Boolean = false, platform: String = "api"): Map<String, Any?> {
    val spec = MODELS[model] ?: throw RequestError("model", "unknown model $model")
    val (tier, context, _) = spec
    if (question.isBlank()) throw RequestError("question", "the question must be a non-empty string")
    val cloud = platform == "bedrock" || platform == "vertex"
    val images = items.count { it["kind"] == "image" }
    val pdfs = items.count { it["kind"] == "pdf" }
    val pages = items.filter { it["kind"] == "pdf" }.sumOf { num(it, "pages") }
    val limit = if (context < 1_000_000) 100 else 600
    if (images > limit) throw RequestError("items", "too many images for $model")
    if (pages > limit) throw RequestError("items", "too many PDF pages for $model")
    if (items.sumOf { num(it, "size").toLong() } > 32L * MIB) throw RequestError("items", "the request would be larger than 32 MiB")
    val many = images + (if (cloud) pdfs else 0) > 20
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
        if (num(it, "size") > (if (cloud) 5 else 10) * MIB) throw RequestError("items[$i].size", "the image is too large")
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
            if (images > 1) content.add(mapOf("type" to "text", "text" to "Image $n:"))
            val block = linkedMapOf<String, Any?>("type" to "image", "source" to sourceOf(it))
            if (exact) block["transformations"] = mapOf("oversized_image" to "error")
            content.add(block)
        } else {
            content.add(mapOf("type" to "document", "source" to sourceOf(it)))
        }
    }
    content.add(mapOf("type" to "text", "text" to question))
    return linkedMapOf("content" to content, "image_tokens" to tokens, "resized" to resized, "pdf_pages" to pages)
}
