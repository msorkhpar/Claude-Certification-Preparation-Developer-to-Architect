import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

// Run executes this file. Change the calls in main to try your code; Submit runs the tests.
fun main() {
    // Turn the logger up, so the log.log(DEBUG, ...) lines of your code show under the printed lines.
    System.setProperty("java.util.logging.SimpleFormatter.format", "%4\$s %5\$s%n")
    val handler = ConsoleHandler().apply { level = Level.ALL }
    Logger.getLogger("").apply { level = Level.ALL; addHandler(handler) }

    fun img(name: String, w: Int, h: Int, mediaType: String = "image/png", source: String = "base64", value: String = "AAAA"): Map<String, Any?> =
        mapOf("kind" to "image", "name" to name, "media_type" to mediaType, "width" to w, "height" to h, "size" to 1000, "source" to source, "value" to value)

    // Two images and a question: the planner puts the images first, each labelled, and the question last.
    val items = listOf(img("chart", 1000, 1000), img("photo", 200, 200, "image/jpeg", "url", "https://example.invalid/p.jpg"))
    try {
        val plan = planRequest("claude-opus-5-5", items, "What changed?")
        for (block in plan["content"] as? List<*> ?: emptyList<Any?>()) {
            block as Map<*, *>
            println("block: ${block["type"]} ${block["text"] ?: ""}")
        }
        println("image tokens: ${plan["image_tokens"]}")
        println("resized: ${plan["resized"]}")
    } catch (err: RequestError) {
        println("refused: ${err.message}")
    }
}
