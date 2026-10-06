import com.fasterxml.jackson.databind.JsonNode
import harness.Reply
import harness.Scripted
import harness.Scripted.map
import harness.Scripted.message
import harness.Scripted.text
import harness.ScriptedHttp

private val log = System.getLogger("front_doors")

/**
 * The same question sent to the direct API, to Claude in Amazon Bedrock and to Claude on Google Vertex AI.
 *
 * Each request is written out by hand and sent through a scripted transport, so nothing leaves the container and nothing
 * is signed: AWS SigV4 (Bedrock), a Google access token (Vertex) or an API key (direct) is added by an SDK or a proxy. The
 * reply is the same illustrative, hand-written Messages response for all three, because the response body keeps the same shape.
 */
val MESSAGES = listOf(map("role", "user", "content", "Capital of France?"))
const val PROJECT = "example-project"
const val REGION = "us-east-1"

/** One front door: where the request goes, its headers and its body. */
data class Door(val url: String, val headers: Map<String, String>, val body: Map<String, Any?>)

val DOORS = linkedMapOf(
    "anthropic" to Door(
        "https://api.anthropic.com/v1/messages",
        mapOf("anthropic-version" to "2023-06-01", "content-type" to "application/json"),
        map("model", "claude-sonnet-5-5", "max_tokens", 64, "messages", MESSAGES),
    ),
    "bedrock" to Door(
        "https://bedrock-mantle.$REGION.api.aws/anthropic/v1/messages",
        mapOf("anthropic-version" to "2023-06-01", "content-type" to "application/json"),
        map("model", "anthropic.claude-sonnet-5-5", "max_tokens", 64, "messages", MESSAGES),
    ),
    "vertex" to Door(
        "https://aiplatform.googleapis.com/v1/projects/$PROJECT/locations/global/publishers/anthropic/models/claude-sonnet-5-5:rawPredict",
        mapOf("content-type" to "application/json"),
        map("anthropic_version", "vertex-2023-10-16", "max_tokens", 64, "messages", MESSAGES),
    ),
)

/** What went out and what came back for one door. */
data class Sent(val transport: ScriptedHttp, val reply: Reply)

fun send(name: String): Sent {
    val door = DOORS.getValue(name)
    val transport = Scripted.http(message(listOf(text("Paris."))))
    return Sent(transport, transport.send("POST", door.url, door.headers, door.body))
}

fun orNone(node: JsonNode, field: String): String = if (node.has(field)) node[field].asText() else "none"

fun main() {
    for (name in DOORS.keys) {
        val (transport, reply) = send(name)
        val sent = transport.requests[0]
        println("${name.padEnd(9)} ${transport.urls[0]}")
        println("          version header: ${transport.headers[0]["anthropic-version"] ?: "none"} | body model: ${orNone(sent, "model")} | body version: ${orNone(sent, "anthropic_version")}")
        println("          reply: ${reply.status()} '${reply.json().at("/content/0/text").asText()}' (same parser for every door)")
    }
}
