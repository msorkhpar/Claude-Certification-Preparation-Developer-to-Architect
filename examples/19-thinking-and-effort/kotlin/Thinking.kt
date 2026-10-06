import com.anthropic.client.AnthropicClient
import com.anthropic.models.messages.Message
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.Model
import com.anthropic.models.messages.OutputConfig
import com.anthropic.models.messages.ThinkingConfigAdaptive
import com.anthropic.models.messages.Usage
import harness.Scripted
import harness.Scripted.map
import harness.Scripted.message
import harness.Scripted.text
import harness.Show.py

private val log = System.getLogger("thinking")

/**
 * Adaptive thinking steered by effort: the request, the reply's blocks and what the thinking cost.
 *
 * The reply is an illustrative, hand-written response in the API's shape (claude-opus-5-5), not a capture. It carries
 * an omitted thinking block (the default display on this model: an empty `thinking` field and a signature) and the
 * `output_tokens_details.thinking_tokens` breakdown the thinking page documents.
 * (`py` is the harness's formatter: it prints a value the way the Python edition does, so the output of the editions matches.)
 */
const val MODEL = "claude-opus-5-5"
const val PRICE_OUT = 20.0 // dollars per million output tokens, pricing page 2026-10-02
val THINKING_BLOCK = map("type", "thinking", "thinking", "", "signature", "illustrative-signature")
val USAGE = map("input_tokens", 410, "output_tokens", 1900, "output_tokens_details", map("thinking_tokens", 1650))

/** usage.output_tokens_details.thinking_tokens */
fun thinkingTokens(usage: Usage): Long = usage.outputTokensDetails().get().thinkingTokens()

fun request(client: AnthropicClient, effort: String): Message =
    client.messages().create(
        MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(8000)
            .thinking(ThinkingConfigAdaptive.builder().build()).outputConfig(OutputConfig.builder().effort(OutputConfig.Effort.of(effort)).build())
            .addUserMessage("Which of these two schedules has no conflicts?").build(),
    )

fun kinds(reply: Message): String = py(reply.content().map { if (it.isThinking()) "thinking" else if (it.isText()) "text" else "other" })

fun main() {
    val replyBody = message(listOf(THINKING_BLOCK, text("Schedule B has no conflicts.")), "end_turn", MODEL, USAGE, null)
    val rig = Scripted.client(replyBody, message(listOf(text("B.")), "end_turn", MODEL, map("input_tokens", 410, "output_tokens", 12), null))
    val reply = request(rig.client(), "high")
    val sent = rig.http().requests[0]
    println("thinking sent: ${py(sent["thinking"])} | effort sent: ${py(sent["output_config"])}")
    println("blocks: ${kinds(reply)} | thinking text shown: ${py(reply.content()[0].asThinking().thinking())}")
    val thinking = thinkingTokens(reply.usage())
    println("output_tokens ${reply.usage().outputTokens()} = thinking $thinking + answer ${reply.usage().outputTokens() - thinking}")
    println("output cost: $${"%.4f".format(reply.usage().outputTokens() * PRICE_OUT / 1_000_000)} (thinking is billed as output, shown or not)")
    val quick = request(rig.client(), "low")
    println("a low-effort turn may skip thinking: ${kinds(quick)} | output_tokens ${quick.usage().outputTokens()}")
    println("effort differs between the two requests: ${py(rig.http().requests[0]["output_config"] != rig.http().requests[1]["output_config"])}")
}
