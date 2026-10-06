import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

// Run executes this file. Change the calls in main to try your code; Submit runs the tests.
fun main() {
    // Turn the logger up, so the log.log(DEBUG, ...) lines of your code show under the printed lines.
    System.setProperty("java.util.logging.SimpleFormatter.format", "%4\$s %5\$s%n")
    val handler = ConsoleHandler().apply { level = Level.ALL }
    Logger.getLogger("").apply { level = Level.ALL; addHandler(handler) }

    val model = "claude-haiku-4-5-20251001"
    val items = listOf("t-1", "t-2", "t-3").map { id ->
        mapOf("id" to id, "params" to mapOf("model" to model, "max_tokens" to 200,
            "messages" to listOf(mapOf("role" to "user", "content" to "Classify ticket $id"))))
    }

    // One line of the .jsonl results file, like the ones the tests build.
    fun resultLine(customId: String, text: String) =
        """{"custom_id":"$customId","result":{"type":"succeeded","message":{"id":"msg_illustrative","type":"message","role":"assistant","model":"$model","stop_reason":"end_turn","content":[{"type":"text","text":"$text"}],"usage":{"input_tokens":50,"output_tokens":7}}}}"""

    try {
        val requests = buildRequests(items)
        println("custom ids: ${requests.map { it["custom_id"] }}")
        // The results come back in any order: they are matched by custom_id, not by position.
        val done = collect(requests, listOf(resultLine("t-3", "billing"), resultLine("t-1", "refund"), resultLine("t-2", "shipping")))
        val outcomes = (done["outcomes"] as? List<*> ?: emptyList<Any?>()).map { o ->
            o as Map<*, *>
            "${o["custom_id"]}/${o["status"]}/${o["text"]}"
        }
        println("outcomes: $outcomes")
        println("to retry / fix / unknown: ${done["retry"]} ${done["fix"]} ${done["unknown"]}")
    } catch (err: BatchError) {
        println("batch error: ${err.message}")
    }
}
