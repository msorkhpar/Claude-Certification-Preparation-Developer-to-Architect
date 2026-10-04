import com.anthropic.client.AnthropicClient
import com.anthropic.errors.AnthropicIoException
import com.anthropic.errors.AnthropicServiceException
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.Model
import harness.Reply
import harness.Scripted
import harness.Scripted.map
import harness.Scripted.message
import harness.Scripted.text
import harness.ScriptedHttp
import java.net.SocketTimeoutException

/**
 * What the SDK retries on its own, and what it does not, against a scripted transport.
 *
 * The failures are illustrative, hand-written replies shaped like the API's error bodies.
 * The SDK asks for a short exponential back-off between attempts (about 0.5 s, then about 1 s); the course's transport records
 * those waits instead of sleeping them.
 */
const val MODEL = "claude-sonnet-5-5"

fun params(): MessageCreateParams = MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(16).addUserMessage("Hi").build()

fun error(status: Int, kind: String, requestId: String) =
    Reply.json(status, map("type", "error", "error", map("type", kind, "message", kind), "request_id", requestId), "request-id", requestId)

val OVERLOADED = error(529, "overloaded_error", "req_illustrative_0529")
val BAD_REQUEST = error(400, "invalid_request_error", "req_illustrative_0400")
val SPEND_CAP = Reply.json(
    429,
    map(
        "type", "error",
        "error", map("type", "rate_limit_error", "message", "monthly limit reached", "details", map("error_code", "enforced_spend_limit_reached")),
        "request_id", "req_illustrative_0429",
    ),
    "request-id", "req_illustrative_0429",
)

fun clientFor(transport: ScriptedHttp, maxRetries: Int): AnthropicClient = Scripted.clientOn(transport, maxRetries)

fun attempt(label: String, script: List<Any>, maxRetries: Int) {
    val transport = Scripted.http(*script.toTypedArray())
    val outcome = try {
        "ok '${clientFor(transport, maxRetries).messages().create(params()).content()[0].asText().text()}'"
    } catch (err: AnthropicServiceException) {
        "${err.javaClass.simpleName} ${err.statusCode()} ${err.errorType().get().asString()} request id ${err.headers().values("request-id")[0]}"
    } catch (err: AnthropicIoException) {
        err.javaClass.simpleName
    }
    val counts = transport.headers.joinToString(", ", "[", "]") { "'${it["x-stainless-retry-count"]}'" }
    println("$label: ${transport.requests.size} request(s), retry-count header $counts -> $outcome")
}

fun main() {
    attempt("529, 529, then 200, max_retries=2", listOf(OVERLOADED, OVERLOADED, message(listOf(text("Hello.")))), 2)
    attempt("529, 529, then 200, max_retries=0", listOf(OVERLOADED, OVERLOADED, message(listOf(text("Hello.")))), 0)
    attempt("400 is never retried, max_retries=2", listOf(BAD_REQUEST, message(listOf(text("Hello.")))), 2)
    attempt("spend-cap 429, max_retries=2", listOf(SPEND_CAP, SPEND_CAP, SPEND_CAP), 2)
    val timeout = AnthropicIoException("scripted", SocketTimeoutException("scripted"))
    attempt("timeout twice, max_retries=1", listOf(timeout, timeout), 1)
}
