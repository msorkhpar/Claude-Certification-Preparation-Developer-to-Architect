import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper

private val log = System.getLogger("read_a_trace")

/**
 * Reading a trace: where did it fail, in the integration or in the model, and what should happen next?
 *
 * The Claude documentation on API errors and on stop reasons (read on 2026-10-03) lists the error types and says that a stop reason is part of
 * a successful response ("Response contains valid content") while an error is a 4xx or 5xx status. It also says that adding text right after
 * a tool result can make Claude end its turn with an empty reply. This file reads three hand-written traces, each a list of events, and names
 * the first failure, its origin and the next action. The traces are scripted and carry no live output.
 */
sealed interface Event

/** A request; only the kinds of block in its last user message matter here. */
data class Request(val lastUserBlocks: List<String>) : Event

/** A successful reply; `content` holds the kinds of its blocks. */
data class Response(val status: Int, val stopReason: String, val content: List<String>) : Event

data class ApiError(val status: Int, val errorType: String) : Event

data class Parse(val ok: Boolean, val text: String) : Event

data class Failure(val index: Int, val what: String, val origin: String, val next: String)

val ORIGIN = mapOf(
    "invalid_request_error" to "integration", "authentication_error" to "account", "rate_limit_error" to "service",
    "api_error" to "service", "overloaded_error" to "service", "timeout_error" to "service",
)
val NEXT = mapOf(
    "invalid_request_error" to "fix the request, do not retry", "authentication_error" to "fix the credential",
    "rate_limit_error" to "wait, then retry", "api_error" to "retry with back-off", "overloaded_error" to "retry with back-off",
    "timeout_error" to "stream the request",
)

val TRACES: Map<String, List<Event>> = linkedMapOf(
    "A: a tool loop that ends in silence" to listOf(
        Request(listOf("text")),
        Response(200, "tool_use", listOf("tool_use")),
        Request(listOf("tool_result", "text")),
        Response(200, "end_turn", emptyList()),
    ),
    "B: a busy service and a retry" to listOf(
        Request(listOf("text")),
        ApiError(529, "overloaded_error"),
        Request(listOf("text")),
        Response(200, "end_turn", listOf("text")),
    ),
    "C: JSON in a code fence" to listOf(
        Request(listOf("text")),
        Response(200, "end_turn", listOf("text")),
        Parse(false, "```json\n{\"label\": \"spam\"}\n```"),
    ),
)

private val JSON = ObjectMapper().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)

private fun isJson(text: String) = runCatching { JSON.readValue(text, Any::class.java) }.isSuccess

/** The first failing event with its origin and next action, or null. */
fun firstFailure(trace: List<Event>): Failure? {
    var lastBlocks = emptyList<String>()
    for ((i, e) in trace.withIndex()) {
        when {
            e is Request -> lastBlocks = e.lastUserBlocks
            e is ApiError -> return Failure(i, e.errorType, ORIGIN.getValue(e.errorType), NEXT.getValue(e.errorType))
            e is Response && e.stopReason == "end_turn" && e.content.isEmpty() -> {
                val at = lastBlocks.indexOf("tool_result")
                return if (at >= 0 && "text" in lastBlocks.drop(at)) Failure(i, "empty reply", "integration", "send the tool result alone, with no text after it")
                else Failure(i, "empty reply", "model", "add a new user message that asks it to continue")
            }
            e is Parse && !e.ok -> {
                val start = e.text.indexOf('{')
                val end = e.text.lastIndexOf('}')
                val candidate = if (start >= 0 && end >= start) e.text.substring(start, end + 1) else ""
                return if (isJson(candidate)) Failure(i, "parse failure", "integration", "extract the JSON object before parsing")
                else Failure(i, "parse failure", "model", "validate the output and retry")
            }
        }
    }
    return null
}

fun main() {
    for ((name, trace) in TRACES) {
        val found = firstFailure(trace)!!
        val recovered = trace.drop(found.index + 1).any { it is Response && it.stopReason == "end_turn" && it.content.isNotEmpty() }
        println(name)
        println("  first failure: event ${found.index}, ${found.what}; origin: ${found.origin}; next: ${found.next}; recovered later: ${if (recovered) "True" else "False"}")
    }
}
