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
    // TODO: the visual tokens of an image of this size (one per 28 x 28 pixel patch, edges rounded up).
    return 0
}

fun resizedSize(width: Int, height: Int, tier: String): Pair<Int, Int>? {
    // TODO: the (width, height) the model sees: the largest aspect-preserving size within the tier's edge and token limits.
    return null
}

fun imageCostUsd(model: String, tokens: Int): Double {
    // TODO: the input cost in dollars of that many tokens, rounded to 6 decimals.
    return 0.0
}

fun toOriginalCoordinates(x: Double, y: Double, width: Int, height: Int, model: String): Pair<Double, Double>? {
    // TODO: a point Claude returned for the image it saw, as a point on the original width x height image.
    return null
}

fun planRequest(model: String, items: List<Map<String, Any?>>, question: String, exact: Boolean = false, platform: String = "api"): Map<String, Any?>? {
    // TODO: validate the items and build the user content. See the statement for the rules and their order.
    return null
}
