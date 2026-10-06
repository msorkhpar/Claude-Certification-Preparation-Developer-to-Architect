import com.anthropic.core.jsonMapper
import com.anthropic.models.beta.messages.BetaContextManagementConfig
import com.anthropic.models.beta.messages.BetaMessageParam
import com.anthropic.models.messages.CitationsConfigParam
import com.anthropic.models.messages.ContentBlockParam
import com.anthropic.models.messages.DocumentBlockParam
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.Model
import com.anthropic.models.messages.TextBlockParam
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import harness.Scripted
import harness.Scripted.map
import harness.Scripted.message
import harness.Scripted.text
import harness.Show.py

private val log = System.getLogger("context_trimming")

/**
 * Clearing old tool results, asking the API to clear them, and checking the citations in an answer.
 *
 * The replies are illustrative, hand-written bodies in the shapes of the context editing and citations pages (claude-sonnet-5-5),
 * not captures; the numbers in the context editing response are the documentation's own example.
 * Messages are JSON trees (Jackson), the same shape the API takes; the SDK's own types read them for the beta call.
 */
const val MODEL = "claude-sonnet-5-5"
const val POLICY = "The grass is green. The sky is blue. Water is essential for life."
private val JSON = ObjectMapper()

/** A rough size: 4 per message, 1 per 4 characters of text, 10 per tool call. */
fun tokens(messages: JsonNode): Int = messages.sumOf { m ->
    val content = m["content"]
    4 + if (content.isTextual) (content.asText().length + 3) / 4 else content.sumOf { b ->
        when (b["type"].asText()) {
            "text" -> (b["text"].asText().length + 3) / 4
            "tool_result" -> (b["content"].asText().length + 3) / 4
            else -> 10 // tool_use
        }
    }
}

fun conversation(): ArrayNode {
    val messages = JSON.createArrayNode()
    messages.add(JSON.valueToTree<JsonNode>(map("role", "user", "content", "Find every mention of the grass in the logs.")))
    for (i in 1..5) {
        messages.add(JSON.valueToTree<JsonNode>(map("role", "assistant", "content", listOf(map("type", "tool_use", "id", "toolu_$i", "name", "grep_logs", "input", map("pattern", "grass-$i"))))))
        messages.add(JSON.valueToTree<JsonNode>(map("role", "user", "content", listOf(map("type", "tool_result", "tool_use_id", "toolu_$i", "content", "log line $i: " + "x".repeat(400))))))
    }
    messages.add(JSON.valueToTree<JsonNode>(map("role", "assistant", "content", listOf(map("type", "text", "text", "Found them all.")))))
    return messages
}

fun toolResults(messages: JsonNode): List<ObjectNode> =
    messages.filter { it["content"].isArray }.flatMap { m -> m["content"].filter { it["type"].asText() == "tool_result" }.map { it as ObjectNode } }

/** A copy in which every tool result but the newest `keep` has its content replaced; the calls stay. */
fun clearToolResults(messages: JsonNode, keep: Int = 2, placeholder: String = "[cleared]"): ArrayNode {
    val out = messages.deepCopy<ArrayNode>()
    val results = toolResults(out)
    results.take(maxOf(results.size - keep, 0)).forEach { it.put("content", placeholder) }
    return out
}

/** A citation is a claim about where text came from; check it against the document. */
fun verify(blocks: JsonNode, documents: List<String>): List<Map<String, Any>> {
    val bad = mutableListOf<Map<String, Any>>()
    for ((i, block) in blocks.withIndex()) for ((j, cite) in block.path("citations").withIndex()) {
        val cited = documents[cite["document_index"].asInt()].substring(cite["start_char_index"].asInt(), cite["end_char_index"].asInt())
        if (cited != cite["cited_text"].asText()) bad += map("block", i, "citation", j, "problem", "text_mismatch")
    }
    return bad
}

fun footnotes(blocks: JsonNode, titles: List<String>): String {
    val numbers = linkedMapOf<String, Int>()
    val lines = mutableListOf<String>()
    val out = StringBuilder()
    for (block in blocks) {
        out.append(block["text"].asText())
        for (cite in block.path("citations")) {
            val key = "${cite["document_index"].asInt()}:${cite["start_char_index"].asInt()}:${cite["end_char_index"].asInt()}"
            if (key !in numbers) {
                numbers[key] = numbers.size + 1
                lines += "[${numbers[key]}] ${titles[cite["document_index"].asInt()]}: \"${cite["cited_text"].asText()}\""
            }
            out.append("[${numbers[key]}]")
        }
    }
    return out.toString() + if (lines.isNotEmpty()) "\n\nSources:\n" + lines.joinToString("\n") else ""
}

fun cite(start: Int, end: Int) = map(
    "type", "char_location", "cited_text", POLICY.substring(start, end), "document_index", 0, "document_title", "Policy",
    "start_char_index", start, "end_char_index", end, "file_id", null,
)

const val EDITS = """
    {"edits": [{"type": "clear_tool_uses_20250919", "trigger": {"type": "input_tokens", "value": 30000}, "keep": {"type": "tool_uses", "value": 3},
                "clear_at_least": {"type": "input_tokens", "value": 5000}, "exclude_tools": ["web_search"]}]}"""

fun editingReply(): Map<String, Any?> = message(listOf(text("Found them all."))).also {
    it["context_management"] = map("applied_edits", listOf(map("type", "clear_tool_uses_20250919", "cleared_tool_uses", 8, "cleared_input_tokens", 50000)))
}

fun citedReply(): Map<String, Any?> = message(
    listOf(
        map("type", "text", "text", "The grass is green. ", "citations", listOf(cite(0, 19))),
        map("type", "text", "text", "Water matters. ", "citations", listOf(cite(37, 65))),
        map("type", "text", "text", "Green again.", "citations", listOf(cite(0, 19))),
    ),
)

fun main() {
    val before = conversation()
    val after = clearToolResults(before, 2)
    val cleared = toolResults(after).count { it["content"].asText() == "[cleared]" }
    val callsKept = (0 until before.size()).filter { before[it]["role"].asText() == "assistant" }.all { before[it] == after[it] }
    println("conversation: ${before.size()} messages, 5 tool results, about ${tokens(before)} tokens")
    println("after clearing all but the newest 2 results: about ${tokens(after)} tokens, $cleared results replaced, calls kept: ${py(callsKept)}")
    val rig = Scripted.client(editingReply(), citedReply())
    val reply = rig.client().beta().messages().create(
        com.anthropic.models.beta.messages.MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(300)
            .messages(jsonMapper().convertValue(before, object : TypeReference<List<BetaMessageParam>>() {}))
            .addBeta("context-management-2025-06-27").contextManagement(jsonMapper().readValue(EDITS, BetaContextManagementConfig::class.java)).build(),
    )
    println("beta header sent: ${rig.http().headers[0]["anthropic-beta"]}")
    val edit = rig.http().requests[0].at("/context_management/edits/0")
    println("edit sent: ${edit["type"].asText()} trigger ${edit.at("/trigger/value").asInt()} keep ${edit.at("/keep/value").asInt()} exclude ${py(edit["exclude_tools"])}")
    val applied = jsonMapper().valueToTree<JsonNode>(reply.contextManagement().get().appliedEdits()[0])
    println("applied edit reported: ${applied["type"].asText()} cleared ${applied["cleared_tool_uses"].asInt()} tool uses, ${applied["cleared_input_tokens"].asInt()} input tokens")
    val ask = MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(300).addUserMessageOfBlockParams(
        listOf(
            ContentBlockParam.ofDocument(DocumentBlockParam.builder().source(DocumentBlockParam.Source.ofText(POLICY)).title("Policy").citations(CitationsConfigParam.builder().enabled(true).build()).build()),
            ContentBlockParam.ofText(TextBlockParam.builder().text("What does the policy say about grass and water?").build()),
        ),
    ).build()
    val answer = rig.client().messages().create(ask)
    val blocks = jsonMapper().valueToTree<JsonNode>(answer.content())
    println("citations enabled in the request: ${py(rig.http().requests[1].at("/messages/0/content/0/citations"))}")
    println("citation problems: ${py(verify(blocks, listOf(POLICY)))}")
    val tampered = blocks.deepCopy<ArrayNode>()
    (tampered[1]["citations"][0] as ObjectNode).put("cited_text", "Water is optional.")
    println("after tampering with one cited_text: ${py(verify(tampered, listOf(POLICY)))}")
    println(footnotes(blocks, listOf("Policy")))
}
