import com.anthropic.client.AnthropicClient
import com.anthropic.core.JsonValue
import com.anthropic.models.messages.JsonOutputFormat
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.Model
import com.anthropic.models.messages.OutputConfig
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import harness.Scripted
import harness.Scripted.map
import harness.Scripted.message
import harness.Scripted.text
import harness.Show.py

private val log = System.getLogger("structured_extraction")

/**
 * Structured outputs plus the checks a schema cannot make, against a scripted model.
 *
 * The replies are illustrative, hand-written bodies in the shape of the Messages API (claude-sonnet-5-5), not captures.
 * The API gets a schema without numeric constraints (structured outputs do not support them); the program enforces them.
 */
const val MODEL = "claude-sonnet-5-5"
private val JSON = ObjectMapper()
val LOCAL_SCHEMA: JsonNode = Scripted.tree(
    """
    {"type": "object",
     "properties": {"vendor": {"type": "string"}, "total": {"type": "number", "minimum": 0},
                    "currency": {"type": "string", "enum": ["USD", "EUR", "GBP"]}, "evidence": {"type": "string"}},
     "required": ["vendor", "total", "currency", "evidence"],
     "additionalProperties": false}""",
)
val UNSUPPORTED = listOf("minimum", "maximum", "multipleOf", "minLength", "maxLength")
val CURRENCIES = listOf("USD", "EUR", "GBP")

/** The schema without the constraints that structured outputs reject; they move to the field's description. */
fun forApi(schema: JsonNode): ObjectNode = schema.deepCopy<ObjectNode>().also { walk(it) }

private fun walk(node: ObjectNode) {
    val notes = UNSUPPORTED.filter { node.has(it) }.map { k -> "$k ${node.remove(k).asText()}" }
    if (notes.isNotEmpty()) node.put("description", ((node["description"]?.asText() ?: "") + " (" + notes.joinToString(", ") + ")").trim())
    node["properties"]?.forEach { walk(it as ObjectNode) }
}

/** What the API cannot promise: numeric limits, the enum's capital letters, and a quotation that is really in the document. */
fun problems(value: Map<String, Any?>, document: String): List<String> {
    val found = mutableListOf<String>()
    val total = value["total"]
    if (total !is Number || total.toDouble() < 0) found += "$.total: must be a number of at least 0"
    if ((value["currency"] as? String) !in CURRENCIES) found += "$.currency: must be one of USD, EUR, GBP, not ${py(value["currency"])}"
    val evidence = value["evidence"]
    if (evidence !is String || evidence !in document) found += "$.evidence: is not found in the document"
    return found
}

/** Structured outputs may change the capital letters of an enum value; compare without them. */
fun normaliseEnum(value: Map<String, Any?>): Map<String, Any?> {
    val currency = value["currency"] as? String ?: return value
    val allowed = CURRENCIES.firstOrNull { it.equals(currency, ignoreCase = true) } ?: return value
    return value + ("currency" to allowed)
}

private fun outputConfig(): OutputConfig {
    val schema = forApi(LOCAL_SCHEMA).fields().asSequence().associate { (k, v) -> k to JsonValue.from(JSON.convertValue(v, Any::class.java)) }
    return OutputConfig.builder().format(JsonOutputFormat.builder().schema(JsonOutputFormat.Schema.builder().additionalProperties(schema).build()).build()).build()
}

@Suppress("UNCHECKED_CAST")
fun extract(client: AnthropicClient, document: String, maxAttempts: Int = 2): Map<String, Any?> {
    val messages = MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(300).outputConfig(outputConfig())
        .addUserMessage("Extract the invoice data.\n<document>\n$document\n</document>")
    var errors = emptyList<String>()
    for (attempt in 1..maxAttempts) {
        val reply = client.messages().create(messages.build())
        val stop = reply.stopReason().get().asString()
        if (stop == "refusal" || stop == "max_tokens") return map("status", if (stop == "refusal") "refused" else "truncated", "attempts", attempt)
        val raw = reply.content()[0].asText().text()
        val value = normaliseEnum(JSON.readValue(raw, LinkedHashMap::class.java) as Map<String, Any?>)
        errors = problems(value, document)
        if (errors.isEmpty()) return map("status", "ok", "attempts", attempt, "value", value)
        messages.addAssistantMessage(raw).addUserMessage("Rejected:\n" + errors.joinToString("\n") + "\nReturn corrected JSON.")
    }
    return map("status", "failed", "attempts", maxAttempts, "errors", errors)
}

const val DOC = "Invoice from Acme Tools. Total due: 120.50 EUR. Thank you for your business."

fun body(overrides: Map<String, Any?> = emptyMap()): String =
    JSON.writeValueAsString(map("vendor", "Acme Tools", "total", 120.5, "currency", "EUR", "evidence", "Total due: 120.50 EUR") + overrides)

val REPLIES = listOf<Any>(
    message(listOf(text(body(map("currency", "Eur"))))),
    message(listOf(text(body(map("evidence", "Total due: 999.00 USD"))))),
    message(listOf(text(body()))),
    message(listOf(text("I can't help with that.")), "refusal"),
    message(listOf(text("{\"vendor\": \"Acme")), "max_tokens"),
)

fun main() {
    val rig = Scripted.client(*REPLIES.toTypedArray())
    println("schema sent to the API: ${forApi(LOCAL_SCHEMA)["properties"]["total"]}")
    for (n in 1..2) println("document $n: ${py(extract(rig.client(), DOC))}")
    println("document 3: ${py(extract(rig.client(), DOC))}")
    println("document 4: ${py(extract(rig.client(), DOC))}")
    val sent = rig.http().requests
    println("requests sent: ${sent.size} | each carried output_config.format.type: ${sent.map { "'${it.at("/output_config/format/type").asText()}'" }.distinct().joinToString(", ", "{", "}")}")
    val third = sent[2]["messages"]
    println("second call of document 2 sent: ${py(third.map { it["role"].asText() })} | feedback: ${third.last()["content"].asText().split("\n")[1]}")
}
