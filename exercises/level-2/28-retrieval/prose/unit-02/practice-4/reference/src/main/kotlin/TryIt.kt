import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

// Run executes this file. Change the calls in main to try your code; Submit runs the tests.
fun main() {
    // Turn the logger up, so the log.log(DEBUG, ...) lines of your code show under the printed lines.
    System.setProperty("java.util.logging.SimpleFormatter.format", "%4\$s %5\$s%n")
    val handler = ConsoleHandler().apply { level = Level.ALL }
    Logger.getLogger("").apply { level = Level.ALL; addHandler(handler) }

    // A tiny corpus like the test fixture: two documents, cut into windows of 14 words that overlap by 4.
    val corpus = listOf(
        Doc("refunds", "Refund policy: customers may request a refund within 30 days of purchase. Refunds are issued to the " +
            "original payment method within five business days. Digital goods are not refundable after download."),
        Doc("shipping", "Shipping: orders over 50 euros ship free of charge. Standard delivery takes three to five business " +
            "days, express delivery takes one day."),
    )
    val chunks = buildChunks(corpus, 14, 4)
    println("chunks: ${chunks.map { it.id }}")

    val query = "free shipping threshold"
    for (mode in listOf("bm25", "embedding", "hybrid")) {
        println("$mode top 3: ${retrieve(chunks, query, mode, 3)}")
    }

    // Recall@3 over one question whose answer we know.
    println("hybrid recall@3: ${if (chunks.isEmpty()) null else evaluate(chunks, listOf(Query(query, listOf("shipping"))), "hybrid", 3)}")
}
