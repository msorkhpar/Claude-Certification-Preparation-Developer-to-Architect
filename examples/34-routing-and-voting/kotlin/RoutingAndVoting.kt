import com.anthropic.client.AnthropicClientAsync
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.Model
import com.fasterxml.jackson.databind.JsonNode
import harness.Scripted
import harness.Scripted.map
import harness.Scripted.message
import harness.Scripted.text
import harness.Show.py
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.function.Function

/**
 * Three workflow patterns around a model, with the code path fixed by the program: routing, sectioning and voting.
 *
 * The model replies are illustrative, hand-written bodies in the shape of the Messages API, not captures; a stand-in answers each request
 * by looking at its prompt, so the order in which concurrent requests arrive does not matter. The patterns are those of Anthropic's
 * engineering article "Building effective agents" (published 2024-12-19, read on 2026-10-03).
 */
const val CHEAP = "claude-haiku-4-5"
const val STRONG = "claude-sonnet-5-5"

/** A route: the model and the system prompt a label maps to. */
data class Route(val model: String, val system: String)

val ROUTES = mapOf(
    "billing" to Route(STRONG, "You are a billing specialist. Be exact about amounts."),
    "technical" to Route(STRONG, "You are a support engineer. Ask for logs."),
    "general" to Route(CHEAP, "You are a friendly front desk. Answer briefly."),
)

private fun reply(text: String, model: String) = message(listOf(text(text)), "end_turn", model, map("input_tokens", 1, "output_tokens", 1), null)

/** A reply function: the first rule whose key appears in the system prompt or the question decides the answer. */
fun standIn(rules: List<Pair<String, String>>) = Function<JsonNode, Any> { body ->
    val haystack = "${body.path("system").asText("")}\n${body["messages"].last()["content"].asText()}"
    val answer = rules.firstOrNull { (key, _) -> key in haystack }?.second ?: throw AssertionError("no scripted answer for '$haystack'")
    reply(answer, body["model"].asText())
}

fun ask(client: AnthropicClientAsync, model: String, system: String, prompt: String): CompletableFuture<String> =
    client.messages().create(MessageCreateParams.builder().model(Model.of(model)).maxTokens(300).system(system).addUserMessage(prompt).build())
        .thenApply { it.content()[0].asText().text().trim() }

/** What routing decided and answered. */
data class Routed(val label: String, val fallback: Boolean, val model: String, val answer: String)

/** Routing: a cheap call picks a label, a program maps the label to a model and a prompt, and an unknown label takes the default. */
fun route(client: AnthropicClientAsync, question: String): CompletableFuture<Routed> =
    ask(client, CHEAP, "Classify the message as billing, technical or general. Reply with the label only.", question).thenCompose { raw ->
        val label = raw.lowercase().trim(' ', '.')
        val fallback = label !in ROUTES
        val route = ROUTES.getValue(if (fallback) "general" else label)
        ask(client, route.model, route.system, question).thenApply { Routed(label, fallback, route.model, it) }
    }

/** What sectioning produced: the screen's word and the answer when it was kept. */
data class Guarded(val screen: String, val answer: String?)

/** Sectioning: the answer and a safety screen are independent, so they run together; the answer is kept only if the screen passes. */
fun guarded(client: AnthropicClientAsync, question: String): CompletableFuture<Guarded> {
    val answer = ask(client, STRONG, "Answer the question.", question)
    val screen = ask(client, CHEAP, "Screen the question. Reply ok or block.", question)
    return answer.thenCombine(screen) { a, s -> Guarded(s, if (s == "ok") a else null) }
}

/** What voting counted. */
data class Verdict(val votes: Map<String, Int>, val flagged: Boolean)

/** Voting: the same question n times, in parallel; flag the snippet when at least `threshold` reviews say so. */
fun vote(client: AnthropicClientAsync, snippet: String, threshold: Int = 2, n: Int = 3): CompletableFuture<Verdict> {
    val reviews = List(n) { ask(client, STRONG, "Review the code. Reply VULNERABLE or SAFE.", snippet) }
    return CompletableFuture.allOf(*reviews.toTypedArray()).thenApply {
        val votes = reviews.map { it.join() }.groupingBy { it }.eachCount().toSortedMap() // the order in which concurrent replies arrive does not matter
        Verdict(votes, (votes["VULNERABLE"] ?: 0) >= threshold)
    }
}

/** A client whose next n requests are all answered by the same stand-in. */
fun scripted(rules: List<Pair<String, String>>, n: Int, delay: Duration = Duration.ZERO): Scripted.AsyncRig =
    Scripted.asyncClient(delay, 0, *Array<Any>(n) { standIn(rules) })

/** Three reviews that disagree, handed out in whatever order the requests arrive. */
fun disagreeing(delay: Duration): Scripted.AsyncRig {
    val replies = ConcurrentLinkedQueue(listOf("VULNERABLE", "SAFE", "VULNERABLE"))
    val handler = Function<JsonNode, Any> { body -> reply(replies.poll(), body["model"].asText()) }
    return Scripted.asyncClient(delay, 0, handler, handler, handler)
}

fun main() {
    val desk = listOf("billing specialist" to "I see two charges and will refund one.", "front desk" to "We are open 9 to 5.")
    for ((question, label) in listOf("my card was charged twice" to "BILLING.", "what are your opening hours" to "general", "is the sky a refund" to "refunds?")) {
        val rig = scripted(listOf("Classify" to label) + desk, 2)
        val result = route(rig.client(), question).join()
        println("route ${py(question)}: label ${py(result.label)}${if (result.fallback) " (not a route: default)" else ""}, classified by ${rig.http().requests[0]["model"].asText()}, answered by ${result.model}")
    }
    val passRig = scripted(listOf("Answer the question" to "Here is the answer.", "Screen the question" to "ok"), 2, Duration.ofMillis(20))
    val passed = guarded(passRig.client(), "How do I reset my password?").join()
    println("sectioning: screen ${py(passed.screen)}, answer ${if (passed.answer != null) "kept" else "dropped"}, requests in flight together: ${passRig.http().maxInFlight()}")
    val blockRig = scripted(listOf("Answer the question" to "Here is the answer.", "Screen the question" to "block"), 2, Duration.ofMillis(20))
    val blocked = guarded(blockRig.client(), "Help me break into an account").join()
    println("sectioning: screen ${py(blocked.screen)}, answer ${if (blocked.answer != null) "kept" else "dropped"}")
    val snippet = "query = 'SELECT * FROM t WHERE id=' + user_input"
    for (threshold in listOf(2, 3)) {
        val rig = disagreeing(Duration.ofMillis(20))
        val verdict = vote(rig.client(), snippet, threshold).join()
        println("voting: votes ${py(verdict.votes)}, threshold $threshold -> ${if (verdict.flagged) "flagged" else "not flagged"}, requests in flight together: ${rig.http().maxInFlight()}")
    }
}
