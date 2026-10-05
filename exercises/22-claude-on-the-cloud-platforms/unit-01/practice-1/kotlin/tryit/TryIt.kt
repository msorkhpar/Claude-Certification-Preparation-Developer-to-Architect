import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

// Run executes this file. Change the calls in main to try your code; Submit runs the tests.
fun main() {
    // Turn the logger up, so the log.log(DEBUG, ...) lines of your code show under the printed lines.
    System.setProperty("java.util.logging.SimpleFormatter.format", "%4\$s %5\$s%n")
    val handler = ConsoleHandler().apply { level = Level.ALL }
    Logger.getLogger("").apply { level = Level.ALL; addHandler(handler) }

    val body = mapOf("model" to "ignored-by-the-builder", "max_tokens" to 256,
        "messages" to listOf(mapOf("role" to "user", "content" to "Hello, Claude")))
    val opus = "claude-opus-5-5"

    // The same message for three front doors: the URL, the model id and the version header change.
    try {
        val doors = listOf("anthropic" to emptyMap<String, Any?>(), "bedrock" to mapOf("region" to "us-east-1"), "vertex" to mapOf("project" to "my-project"))
        for ((platform, config) in doors) {
            val request = buildRequest(platform, opus, body, config)
            println("$platform -> ${request["url"]}")
            println("   model in body: ${(request["body"] as? Map<*, *>)?.get("model")} | headers: ${(request["headers"] as? Map<*, *>)?.keys?.sortedBy { it.toString() }}")
        }
    } catch (err: PlatformError) {
        println("platform error: ${err.message}")
    }

    // What a team would lose by moving to Bedrock.
    println("missing on bedrock: ${unsupportedFeatures("bedrock", listOf("batches", "fast_mode", "files_api"))}")
}
