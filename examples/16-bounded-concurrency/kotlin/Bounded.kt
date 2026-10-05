import com.anthropic.client.AnthropicClientAsync
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.Model
import com.fasterxml.jackson.databind.JsonNode
import harness.Reply
import harness.Scripted
import harness.Scripted.map
import harness.Scripted.message
import harness.Scripted.text
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.function.Function

private val log = System.getLogger("bounded")

/**
 * Twelve classification calls with the async SDK: unbounded, then bounded by a semaphore.
 *
 * The transport is scripted: every request takes 50 ms inside it, the reply is a label built from
 * the ticket in the request, and ticket 7 is answered with a 429. The labels are illustrative.
 * Each ticket runs on its own virtual thread; the semaphore is what bounds them (the Kotlin/JVM counterpart of asyncio.Semaphore).
 */
const val MODEL = "claude-sonnet-5-5"
val TICKETS = (1..12).map { "ticket $it" }

/** A scripted reply computed from the request body. */
val RESPONDER = Function<JsonNode, Any> { body ->
    val ticket = body.at("/messages/0/content").asText()
    if (ticket == "ticket 7") Reply.json(429, map("type", "error", "error", map("type", "rate_limit_error", "message", "slow down")))
    else message(listOf(text("label for $ticket")))
}

/** One ticket's outcome: a label or the error that ended it (a failure does not cancel the others). */
data class Outcome(val label: String?, val error: Throwable?)

fun classify(client: AnthropicClientAsync, ticket: String): CompletableFuture<String> {
    val params = MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(16).addUserMessage(ticket).build()
    return client.messages().create(params).thenApply { it.content()[0].asText().text() }
}

/** The outcomes in input order, and the most requests that were inside the transport at once. */
data class Run(val results: List<Outcome>, val peak: Int)

fun runAll(limit: Int?): Run {
    val rig = Scripted.asyncClient(Duration.ofMillis(50), 0, *TICKETS.map { RESPONDER as Any }.toTypedArray())
    val gate = limit?.let { Semaphore(it) }
    Executors.newVirtualThreadPerTaskExecutor().use { virtual ->
        val tasks = TICKETS.map { ticket ->
            CompletableFuture.supplyAsync({
                gate?.acquireUninterruptibly()
                try {
                    Outcome(classify(rig.client(), ticket).join(), null)
                } catch (e: CompletionException) {
                    Outcome(null, e.cause)
                } finally {
                    gate?.release()
                }
            }, virtual)
        }
        return Run(tasks.map { it.join() }, rig.http().maxInFlight())
    }
}

fun main() {
    var last: Run? = null
    for ((label, limit) in listOf("unbounded" to null, "bounded by 4" to 4)) {
        val run = runAll(limit)
        last = run
        val failed = run.results.withIndex().filter { it.value.error != null }.map { it.index + 1 }
        println("$label: peak in flight ${run.peak}, ${run.results.size - failed.size} answered, failed tickets $failed")
    }
    val r = last!!.results
    println("results keep input order: ${r[0].label} | ${r[5].label} | ${r[6].error!!.javaClass.simpleName} | ${r[7].label}")
}
