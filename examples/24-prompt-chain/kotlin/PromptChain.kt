import com.anthropic.client.AnthropicClient
import com.anthropic.models.messages.Message
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.Model
import harness.Scripted
import harness.Scripted.message
import harness.Scripted.text
import harness.Show.py

private val log = System.getLogger("prompt_chain")

/**
 * A two-step prompt chain with versioned templates, against a scripted model.
 *
 * The replies are illustrative, hand-written bodies in the shape of the Messages API (claude-sonnet-5-5), not captures.
 */
const val MODEL = "claude-sonnet-5-5"

/** A versioned template: a system prompt and a user prompt with {{name}} placeholders. */
data class Template(val system: String, val user: String)

val TEMPLATES = mapOf(
    "extract-quotes@2" to Template(
        "You answer questions about company documents and use only what the documents say.",
        "{{documents}}\n\nFirst quote the passages that bear on the question, each in <quote> tags inside <quotes>. If nothing bears on it, write <quotes></quotes>.\n\n<question>{{question}}</question>",
    ),
    "answer-from-quotes@1" to Template(
        "You answer from quoted evidence. If the quotes do not settle the question, say what is missing.",
        "{{quotes}}\n\nAnswer the question in one or two sentences, and say which quote supports each claim.\n\n<question>{{question}}</question>",
    ),
)

val DOCUMENTS = listOf(
    "travel-policy.txt" to
        "Flights above 400 dollars need approval from a manager before booking. Economy class is the default for flights under six hours.",
    "expenses-faq.txt" to
        "Meals are reimbursed up to 60 dollars a day when travelling. Receipts are required for every claim over 25 dollars.",
)
const val QUESTION = "Does a 450 dollar economy flight need approval?"

private val PLACEHOLDER = Regex("""\{\{(\w+)\}\}""")

/** Fill {{name}} placeholders in one pass; a value is data and is never read as a template. */
fun render(template: String, values: Map<String, String>): String {
    val missing = PLACEHOLDER.findAll(template).map { it.groupValues[1] }.filter { it !in values }.toSortedSet()
    if (missing.isNotEmpty()) throw NoSuchElementException("unfilled variables: ${py(missing.toList())}")
    return PLACEHOLDER.replace(template) { values.getValue(it.groupValues[1]) }
}

fun documentsBlock(documents: List<Pair<String, String>>): String {
    val body = documents.withIndex().joinToString("") { (i, d) ->
        "<document index=\"${i + 1}\">\n<source>${d.first}</source>\n<document_content>\n${d.second}\n</document_content>\n</document>\n"
    }
    return "<documents>\n$body</documents>"
}

fun step(client: AnthropicClient, version: String, values: Map<String, String>): Message {
    val template = TEMPLATES.getValue(version)
    return client.messages().create(MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(500).system(template.system).addUserMessage(render(template.user, values)).build())
}

val REPLIES = listOf<Any>(
    message(listOf(text("<quotes>\n<quote>Flights above 400 dollars need approval from a manager before booking.</quote>\n</quotes>"))),
    message(listOf(text("Yes: at 450 dollars it is above the 400 dollar limit, so it needs a manager's approval first (quote 1)."))),
)

fun main() {
    val rig = Scripted.client(*REPLIES.toTypedArray())
    val first = step(rig.client(), "extract-quotes@2", mapOf("documents" to documentsBlock(DOCUMENTS), "question" to QUESTION))
    val quotes = first.content()[0].asText().text()
    val second = step(rig.client(), "answer-from-quotes@1", mapOf("quotes" to quotes, "question" to QUESTION))
    val requests = rig.http().requests
    val one = requests[0].at("/messages/0/content").asText()
    val two = requests[1].at("/messages/0/content").asText()
    println("templates used: ${py(listOf("extract-quotes@2", "answer-from-quotes@1"))}")
    println("step 1: documents come before the question: ${py(one.indexOf("<documents>") < one.indexOf("<question>"))}")
    println("step 1: the prompt ends with the question: ${py(one.trimEnd().endsWith("</question>"))}")
    println("step 2: the full documents are not resent: ${py("<documents>" !in two)} (${one.length} characters then ${two.length})")
    println("last message of each request is a user turn (no prefill): ${py(requests.map { r -> r["messages"].last()["role"].asText() })}")
    val sampling = listOf("temperature", "top_p", "top_k").filter { requests[0].has(it) }.sorted()
    println("sampling parameters sent: ${if (sampling.isEmpty()) "none" else py(sampling)}")
    println("system prompts differ per step: ${py(requests[0]["system"] != requests[1]["system"])}")
    println("data is not read as a template: ${py(render(TEMPLATES.getValue("answer-from-quotes@1").user, mapOf("quotes" to "{{question}} stays", "question" to "Q?")).split("{{question}} stays").size - 1 == 1)}")
    try {
        render(TEMPLATES.getValue("answer-from-quotes@1").user, mapOf("quotes" to quotes))
    } catch (err: NoSuchElementException) {
        println("a missing variable is an error: ${err.message}")
    }
    println("step 1 output: ${quotes.replace("\n", " ")}")
    println("step 2 output: ${second.content()[0].asText().text()}")
}
