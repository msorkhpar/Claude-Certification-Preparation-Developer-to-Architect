import com.anthropic.client.AnthropicClient
import com.anthropic.models.messages.Message
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.Model
import harness.Scripted
import harness.Scripted.map
import harness.Scripted.message
import harness.Scripted.text

private val log = System.getLogger("conversation")

/**
 * A conversation the client keeps: the API is stateless, so every request carries the whole history.
 *
 * Three turns through the real SDK against a scripted transport. The replies are illustrative,
 * hand-written Messages responses (claude-sonnet-5-5), not captures.
 */
const val MODEL = "claude-sonnet-5-5"
const val SYSTEM = "You answer in one short sentence."

val REPLIES = listOf(
    message(listOf(text("Paris.")), "end_turn", MODEL, map("input_tokens", 18, "output_tokens", 4), null),
    message(listOf(text("It has been the capital since")), "max_tokens", MODEL, map("input_tokens", 30, "output_tokens", 6), null),
    message(listOf(text("Seine")), "stop_sequence", MODEL, map("input_tokens", 41, "output_tokens", 2), "END"),
)
val QUESTIONS = listOf("Capital of France?", "Since when?", "Name its river. End with END.")

/** One turn: the reply and the token totals so far. */
data class Turn(val reply: Message, val input: Long, val output: Long)

/** Keep the history in the request builder and send all of it every time. */
fun run(client: AnthropicClient, questions: List<String>): List<Turn> {
    val history = MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(16).system(SYSTEM).stopSequences(listOf("END"))
    var input = 0L
    var output = 0L
    return questions.map { question ->
        history.addUserMessage(question)
        val reply = client.messages().create(history.build())
        history.addMessage(reply) // the assistant turn goes back exactly as it was received
        input += reply.usage().inputTokens()
        output += reply.usage().outputTokens()
        Turn(reply, input, output)
    }
}

fun main() {
    val transport = Scripted.http(*REPLIES.toTypedArray())
    val turns = run(Scripted.clientOn(transport, 0), QUESTIONS)
    for ((index, turn) in turns.withIndex()) {
        val sent = transport.requests[index]["messages"]
        val roles = sent.map { it["role"].asText() }
        val sequence = turn.reply.stopSequence().map { " '$it'" }.orElse("")
        println("turn ${index + 1}: sent ${sent.size()} message(s) [${roles.joinToString(", ")}] -> ${turn.reply.stopReason().get().asString()}$sequence, '${turn.reply.content()[0].asText().text()}'")
    }
    println("totals: {'input': ${turns.last().input}, 'output': ${turns.last().output}}")
    val systemTopLevel = transport.requests.all { it.path("system").asText() == SYSTEM }
    val used = transport.requests.flatMap { r -> r["messages"].map { it["role"].asText() } }.toSortedSet()
    println("system is a top-level field: ${if (systemTopLevel) "True" else "False"} | roles ever used in messages: ${used.joinToString(", ", "[", "]") { "'$it'" }}")
}
