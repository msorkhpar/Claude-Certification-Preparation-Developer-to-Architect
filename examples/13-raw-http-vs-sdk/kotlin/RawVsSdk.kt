import com.anthropic.errors.RateLimitException
import com.anthropic.models.messages.Message
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.Model
import harness.Reply
import harness.Scripted
import harness.Scripted.map
import harness.Scripted.message
import harness.Scripted.text
import harness.ScriptedHttp

private val log = System.getLogger("raw_vs_sdk")

/**
 * One Messages call written by hand, then the same call through the SDK.
 *
 * Both go through the same kind of scripted transport, so nothing leaves the container. The reply is an
 * illustrative, hand-written Messages response (claude-sonnet-5-5), not a capture.
 * (`harness` is the course's stand-in: a scripted transport plugged into the SDK's own HttpClient hook.)
 */
const val MODEL = "claude-sonnet-5-5"
const val URL = "https://api.anthropic.com/v1/messages"
val PAYLOAD = map("model", MODEL, "max_tokens", 64, "messages", listOf(map("role", "user", "content", "Capital of France?")))
val OK = message(listOf(text("Paris.")))
val LIMITED = Reply.json(
    429, map("type", "error", "error", map("type", "rate_limit_error", "message", "Rate limited"), "request_id", "req_illustrative_0001"),
    "retry-after", "7", "request-id", "req_illustrative_0001",
)

/** The HTTP request the SDK would build, written out: three headers and a JSON body. */
fun rawCall(transport: ScriptedHttp): Reply =
    transport.send("POST", URL, mapOf("x-api-key" to "placeholder", "anthropic-version" to "2023-06-01", "content-type" to "application/json"), PAYLOAD)

fun sdkCall(transport: ScriptedHttp): Message =
    Scripted.clientOn(transport, 0).messages().create(MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(64).addUserMessage("Capital of France?").build())

private fun py(value: Boolean) = if (value) "True" else "False"

fun main() {
    val raw = Scripted.http(OK, LIMITED)
    val sdk = Scripted.http(OK, LIMITED)

    val reply = rawCall(raw)
    println("raw : ${raw.methods[0]} ${raw.urls[0]} -> ${reply.status()} '${reply.json().at("/content/0/text").asText()}'")
    val message = sdkCall(sdk)
    println("sdk : POST ${sdk.urls[0]} -> 200 '${message.content()[0].asText().text()}'")
    println("same URL: ${py(raw.urls[0] == sdk.urls[0])} | same body: ${py(raw.requests[0] == sdk.requests[0])}")
    for (name in listOf("anthropic-version", "content-type")) println("$name: raw ${raw.headers[0][name]} | sdk ${sdk.headers[0][name]}")
    println("headers only the SDK adds: " + (sdk.headers[0].keys - raw.headers[0].keys).sorted().joinToString(", "))

    val limited = rawCall(raw)
    println("raw 429 : ${limited.status()} ${limited.json().at("/error/type").asText()} retry-after ${limited.headers()["retry-after"]}")
    try {
        sdkCall(sdk)
    } catch (err: RateLimitException) {
        println("sdk 429 : ${err.javaClass.simpleName} ${err.statusCode()} request id ${err.headers().values("request-id")[0]}")
    }
}
