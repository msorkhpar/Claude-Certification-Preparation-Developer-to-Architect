import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

// Run executes this file. Change the calls in main to try your code; Submit runs the tests.
fun main() {
    // Turn the logger up, so the log.log(DEBUG, ...) lines of your code show under the printed lines.
    System.setProperty("java.util.logging.SimpleFormatter.format", "%4\$s %5\$s%n")
    val handler = ConsoleHandler().apply { level = Level.ALL }
    Logger.getLogger("").apply { level = Level.ALL; addHandler(handler) }

    // Two documents, split into chunks that carry their title and section.
    val annual = "# Annual plan\n## Cancellation\nYou can cancel within 14 days for a full refund."
    val monthly = "# Monthly plan\n## Cancellation\nYou can cancel at any time."
    val chunks = (chunkSections("annual", annual) ?: emptyList()) + (chunkSections("monthly", monthly) ?: emptyList())
    for (chunk in chunks) println("chunk: ${chunk.id} | ${chunk.text}")

    // Search ranks the chunks that share the most (and the most specific) words with the question.
    println("search: ${search(chunks, "can I cancel within 14 days", 2)}")
    println("search, annual only: ${search(chunks, "cancel", 2, setOf("annual"))}")

    // The annual document changes: reindex replaces its chunks and leaves the other alone.
    val changed = annual.replace("14 days", "30 days")
    val result = reindex(chunks, mapOf("annual" to changed, "monthly" to monthly))
    println("reindex report: ${result?.report}")
}
