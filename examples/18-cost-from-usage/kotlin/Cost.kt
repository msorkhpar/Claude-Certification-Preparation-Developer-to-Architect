import com.anthropic.models.messages.MessageCountTokensParams
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.Model
import com.anthropic.models.messages.Usage
import harness.Scripted
import harness.Scripted.map
import harness.Scripted.message
import harness.Scripted.text

/**
 * Count tokens before sending, then price the reply from its usage object.
 *
 * Both calls go through a scripted transport, so nothing leaves the container. The replies are illustrative,
 * hand-written responses in the API's shapes (claude-sonnet-5-5), not captures. Prices are dollars per million
 * tokens, read from the Claude pricing page on 2026-10-02.
 */
const val MODEL = "claude-sonnet-5-5"
const val SYSTEM = "You answer from the policy document."
const val QUESTION = "Summarise the refund policy in two sentences."

/** input, output, cache-read multiplier */
val PRICES = linkedMapOf(
    "claude-haiku-4-5-20251001" to Triple(1.0, 5.0, 0.1),
    "claude-sonnet-5-5" to Triple(2.0, 10.0, 0.1),
    "claude-opus-5-5" to Triple(4.0, 20.0, 0.05),
    "claude-fable-5-1" to Triple(10.0, 50.0, 0.025),
)

val USAGE = map(
    "input_tokens", 120, "output_tokens", 340, "cache_read_input_tokens", 0, "cache_creation_input_tokens", 4000,
    "cache_creation", map("ephemeral_5m_input_tokens", 4000, "ephemeral_1h_input_tokens", 0),
)

fun params(): MessageCreateParams = MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(300).system(SYSTEM).addUserMessage(QUESTION).build()

/** Dollars for one request: cache writes cost 1.25x input (5 minutes) or 2x (1 hour), reads a model-specific fraction. */
fun cost(model: String, usage: Usage, batch: Boolean = false): Double {
    val (priceIn, priceOut, read) = PRICES.getValue(model)
    val perToken = (usage.inputTokens() * priceIn
        + usage.cacheCreation().get().ephemeral5mInputTokens() * priceIn * 1.25
        + usage.cacheCreation().get().ephemeral1hInputTokens() * priceIn * 2.0
        + usage.cacheReadInputTokens().get() * priceIn * read
        + usage.outputTokens() * priceOut)
    return perToken / 1_000_000 * (if (batch) 0.5 else 1.0)
}

private fun dollars(v: Double) = "$" + "%.6f".format(v)

fun main() {
    val rig = Scripted.client(map("input_tokens", 4821), message(listOf(text("Refunds take 14 days. Opened items are excluded.")), "end_turn", MODEL, USAGE, null))
    val counted = rig.client().messages().countTokens(MessageCountTokensParams.builder().model(MODEL).system(SYSTEM).addUserMessage(QUESTION).build()).inputTokens()
    println("count_tokens -> $counted input tokens (POST ${rig.http().urls[0].split(".com")[1]})")
    println("estimate before sending, input only: ${dollars(counted * PRICES.getValue(MODEL).first / 1_000_000)} on $MODEL")
    val u = rig.client().messages().create(params()).usage()
    println("usage: input ${u.inputTokens()}, cache write ${u.cacheCreationInputTokens().get()}, cache read ${u.cacheReadInputTokens().get()}, output ${u.outputTokens()}")
    println("cost of this request: ${dollars(cost(MODEL, u))}  (batch: ${dollars(cost(MODEL, u, batch = true))})")
    println("the same usage on each model:")
    for (model in PRICES.keys) println("  ${model.padEnd(28)} ${dollars(cost(model, u))}")
}
