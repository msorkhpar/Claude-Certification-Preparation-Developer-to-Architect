import com.anthropic.models.messages.CacheControlEphemeral
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.Model
import com.anthropic.models.messages.TextBlockParam
import com.anthropic.models.messages.Usage
import com.fasterxml.jackson.databind.JsonNode
import harness.Scripted
import harness.Scripted.map
import harness.Scripted.message
import harness.Scripted.text
import java.util.function.Function

private val log = System.getLogger("cache_hits")

/**
 * Prompt caching seen through the usage object, against a scripted server that applies the documented prefix rule.
 *
 * The server (CacheSim) is an illustrative, hand-written stand-in, not a capture: it counts a token as four characters,
 * caches the prefix up to a block that carries cache_control when that prefix reaches the minimum size, keeps it for five
 * minutes from its last use, and reports `cache_creation_input_tokens`, `cache_read_input_tokens` and `input_tokens` (the
 * tokens after the last breakpoint) as the prompt caching page describes them (claude-sonnet-5-5, minimum 512 tokens).
 */
const val MODEL = "claude-sonnet-5-5"
val POLICY = "Refund policy clause: items may be returned within 14 days. ".repeat(40) // about 600 tokens

fun tokens(piece: String) = (piece.length + 3) / 4 // ceil(length / 4)

/** One block of the prompt with the text it holds and whether it carries a cache_control breakpoint. */
data class Block(val text: String, val mark: Boolean)

/** The request flattened in prefix order: tools, then system, then messages, each with its cache_control. */
fun blocksOf(body: JsonNode): List<Block> {
    val out = mutableListOf<Block>()
    for (tool in body.path("tools")) out += Block(tool.toString(), tool.has("cache_control"))
    val system = body.path("system")
    if (system.isTextual) out += Block(system.asText(), false) else for (b in system) out += Block(b["text"].asText(), b.has("cache_control"))
    for (m in body["messages"]) {
        val role = m["role"].asText()
        if (m["content"].isTextual) out += Block("$role: ${m["content"].asText()}", false)
        else for (b in m["content"]) out += Block("$role: ${b["text"].asText()}", b.has("cache_control"))
    }
    return out
}

/** The scripted server: a reply function of the request body, with a clock the program moves. */
class CacheSim(private val minimum: Int = 512, private val ttl: Int = 300) : Function<JsonNode, Any> {
    var clock = 0
    private val entries = HashMap<String, Int>()

    private fun key(blocks: List<Block>, i: Int) = blocks.take(i + 1).joinToString("\u0000") { it.text }

    private fun size(blocks: List<Block>, i: Int) = blocks.take(i + 1).sumOf { tokens(it.text) }

    override fun apply(body: JsonNode): Any {
        val blocks = blocksOf(body)
        val marks = blocks.indices.filter { blocks[it].mark }
        var read = 0
        var written = 0
        var hit: Int? = null
        for (i in marks.reversed()) {
            if ((entries[key(blocks, i)] ?: -1) > clock) {
                read = size(blocks, i)
                hit = i
                entries[key(blocks, i)] = clock + ttl // a hit refreshes the entry
                break
            }
        }
        if (marks.isNotEmpty() && hit != marks.last() && size(blocks, marks.last()) >= minimum) {
            written = size(blocks, marks.last()) - read
            entries[key(blocks, marks.last())] = clock + ttl
        }
        val fresh = size(blocks, blocks.size - 1) - read - written
        val usage = map(
            "input_tokens", fresh, "output_tokens", 20, "cache_read_input_tokens", read, "cache_creation_input_tokens", written,
            "cache_creation", map("ephemeral_5m_input_tokens", written, "ephemeral_1h_input_tokens", 0),
        )
        return message(listOf(text("ok")), "end_turn", MODEL, usage, null)
    }
}

fun request(system: List<Block>, question: String): MessageCreateParams {
    val blocks = system.map { b ->
        TextBlockParam.builder().text(b.text).apply { if (b.mark) cacheControl(CacheControlEphemeral.builder().build()) }.build()
    }
    return MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(50).systemOfTextBlockParams(blocks).addUserMessage(question).build()
}

fun stable(question: String) = request(listOf(Block(POLICY, true)), question)

fun stampFirst(clockLabel: String, question: String) = request(listOf(Block("Current time: $clockLabel", false), Block(POLICY, true)), question)

fun stampLast(clockLabel: String, question: String) = request(listOf(Block(POLICY, true), Block("Current time: $clockLabel", false)), question)

/** Move the clock, send the request through the SDK to the simulated server and read the usage back. */
fun send(sim: CacheSim, body: MessageCreateParams, wait: Int = 0): Usage {
    sim.clock += wait
    return Scripted.client(sim).client().messages().create(body).usage()
}

fun run(sim: CacheSim, label: String, body: MessageCreateParams, wait: Int = 0): Usage {
    val usage = send(sim, body, wait)
    println("%-34s write %4d  read %4d  fresh %4d".format(label, usage.cacheCreationInputTokens().get(), usage.cacheReadInputTokens().get(), usage.inputTokens()))
    return usage
}

fun main() {
    var sim = CacheSim()
    run(sim, "1 stable system, first call", stable("Can I return a lamp?"))
    run(sim, "2 same system, new question", stable("Can I return a chair?"), wait = 60)
    run(sim, "3 six minutes of silence", stable("Can I return a desk?"), wait = 360)
    sim = CacheSim()
    run(sim, "4 timestamp first, 10:01", stampFirst("10:01", "Can I return a lamp?"))
    run(sim, "5 timestamp first, 10:02", stampFirst("10:02", "Can I return a lamp?"), wait = 60)
    run(sim, "6 timestamp last, 10:03", stampLast("10:03", "Can I return a lamp?"), wait = 60)
    run(sim, "7 timestamp last, 10:04", stampLast("10:04", "Can I return a lamp?"), wait = 60)
}
