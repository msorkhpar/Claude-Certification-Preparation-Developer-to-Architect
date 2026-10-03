import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** An MCP server's side of multi round-trip requests, with no state kept between calls. See ../../statement.md. Requests and results are JSON-like maps. */

const val VERSION = "2026-07-28"
const val META_VERSION = "io.modelcontextprotocol/protocolVersion"
const val META_CAPS = "io.modelcontextprotocol/clientCapabilities"
const val TTL_SECONDS = 300

val TOOLS: List<Map<String, Any?>> = listOf(
    mapOf("name" to "deploy", "description" to "Deploy a service to an environment. Production needs a person's confirmation.",
        "inputSchema" to mapOf("type" to "object", "properties" to mapOf("service" to mapOf("type" to "string"), "env" to mapOf("type" to "string", "enum" to listOf("staging", "production"))), "required" to listOf("service", "env"))),
    mapOf("name" to "status", "description" to "Report whether a service is running.",
        "inputSchema" to mapOf("type" to "object", "properties" to mapOf("service" to mapOf("type" to "string")), "required" to listOf("service"))),
)

/** JSON with the keys of every object in sorted order. */
private fun canonical(v: Any?): String = when (v) {
    is Map<*, *> -> v.entries.sortedBy { it.key as String }.joinToString(",", "{", "}") { Json.stringify(it.key) + ":" + canonical(it.value) }
    is List<*> -> v.joinToString(",", "[", "]") { canonical(it) }
    else -> Json.stringify(v)
}

private fun b64(data: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(data)

private fun hmac(secret: String, body: String): ByteArray {
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(SecretKeySpec(secret.toByteArray(), "HmacSHA256"))
    return mac.doFinal(body.toByteArray())
}

/** Given: a requestState for payload: its JSON, then a HMAC-SHA256 signature of it, both in base64url, joined by a dot. */
fun mintState(secret: String, payload: Map<String, Any?>): String {
    val body = b64(canonical(payload).toByteArray())
    return body + "." + b64(hmac(secret, body))
}

/** Given: the payload of a requestState, or null when it is not one this secret signed (tampered, truncated, foreign, garbage). */
@Suppress("UNCHECKED_CAST")
fun readState(secret: String, token: String): Map<String, Any?>? = try {
    val parts = token.split(".")
    if (parts.size != 2 || !MessageDigest.isEqual(b64(hmac(secret, parts[0])).toByteArray(), parts[1].toByteArray())) null
    else Json.parse(String(Base64.getUrlDecoder().decode(parts[0]))) as? Map<String, Any?>
} catch (e: RuntimeException) {
    null
}

/** Given: a fingerprint of the call's arguments, the same for the same arguments in any key order. */
fun argsDigest(arguments: Map<String, Any?>): String =
    MessageDigest.getInstance("SHA-256").digest(canonical(arguments).toByteArray()).joinToString("") { "%02x".format(it) }

private fun error(code: Int, message: String, data: Any? = null): Map<String, Any?> =
    mapOf("error" to linkedMapOf<String, Any?>("code" to code, "message" to message).also { if (data != null) it["data"] = data })

@Suppress("UNCHECKED_CAST")
private fun meta(request: Map<String, Any?>): Map<String, Any?> = (request["_meta"] as? Map<String, Any?>) ?: emptyMap()

private fun metaError(request: Map<String, Any?>): Map<String, Any?>? =
    if (meta(request)[META_VERSION] != VERSION) error(-32022, "Unsupported protocol version", mapOf("supported" to listOf(VERSION))) else null

fun listTools(request: Map<String, Any?>): Map<String, Any?> =
    metaError(request) ?: mapOf("resultType" to "complete", "tools" to TOOLS.sortedBy { it["name"] as String }, "ttlMs" to 300000, "cacheScope" to "public")

private fun complete(text: String, isError: Boolean = false): Map<String, Any?> =
    mapOf("resultType" to "complete", "content" to listOf(mapOf("type" to "text", "text" to text)), "isError" to isError)

private fun confirmRequest(service: String): Map<String, Any?> = mapOf("confirm" to mapOf("method" to "elicitation/create", "params" to mapOf("mode" to "form", "message" to "Deploy $service to production?",
    "requestedSchema" to mapOf("type" to "object", "properties" to mapOf("confirm" to mapOf("type" to "boolean", "title" to "Confirm the deployment")), "required" to listOf("confirm")))))

private fun notesRequest(service: String): Map<String, Any?> = mapOf("notes" to mapOf("method" to "sampling/createMessage", "params" to mapOf(
    "messages" to listOf(mapOf("role" to "user", "content" to mapOf("type" to "text", "text" to "Write one sentence of release notes for $service."))), "maxTokens" to 100)))

private fun ask(requests: Map<String, Any?>, step: String, secret: String, name: String, arguments: Map<String, Any?>, principal: String, now: Long): Map<String, Any?> {
    val state = mapOf("v" to 1, "tool" to name, "digest" to argsDigest(arguments), "sub" to principal, "exp" to now + TTL_SECONDS, "step" to step)
    return mapOf("resultType" to "input_required", "inputRequests" to requests, "requestState" to mintState(secret, state))
}

@Suppress("UNCHECKED_CAST")
fun callTool(request: Map<String, Any?>, secret: String, principal: String, now: Long): Map<String, Any?> {
    metaError(request)?.let { return it }
    val name = request["name"]
    val arguments = (request["arguments"] as? Map<String, Any?>) ?: emptyMap()
    if (name != "deploy" && name != "status") return error(-32602, "Unknown tool: $name")
    val service = arguments["service"]
    if (service !is String || service.isBlank()) return error(-32602, "Invalid params: service is required")
    if (name == "status") return complete("$service: running")
    val env = arguments["env"]
    if (env != "staging" && env != "production") return error(-32602, "Invalid params: env must be staging or production")
    if (env == "staging") return complete("Deployed $service to staging")
    val caps = (meta(request)[META_CAPS] as? Map<String, Any?>) ?: emptyMap()
    val elicitation = caps["elicitation"] as? Map<String, Any?>
    if (elicitation == null || (elicitation.isNotEmpty() && !elicitation.containsKey("form"))) return complete("Deploying to production needs confirmation, and this client cannot be asked.", true)
    var step = "confirm"
    val token = request["requestState"]?.takeIf { readState(secret, it.toString()) != null }
    if (token != null) {
        val state = readState(secret, token.toString()) ?: return error(-32602, "Invalid requestState")
        if (now > ((state["exp"] as? Number)?.toLong() ?: 0L)) return error(-32602, "Expired requestState")
        if (state["sub"] != principal || state["tool"] != name || state["digest"] != argsDigest(arguments)) return error(-32602, "requestState does not match this request")
        step = state["step"].toString()
    }
    val answers = if (token != null) (request["inputResponses"] as? Map<String, Any?>) ?: emptyMap() else emptyMap()
    if (step == "confirm") {
        val answer = answers["confirm"] as? Map<String, Any?>
        if (answer == null || answer["action"] !in listOf("accept", "decline", "cancel")) return ask(confirmRequest(service), "confirm", secret, "deploy", arguments, principal, now)
        val confirmed = ((answer["content"] as? Map<String, Any?>)?.get("confirm")) == true
        if (answer["action"] != "accept" || !confirmed) return complete("Deployment cancelled")
        if (!caps.containsKey("sampling")) return complete("Deployed $service to production")
        return ask(notesRequest(service), "notes", secret, "deploy", arguments, principal, now)
    }
    val content = (answers["notes"] as? Map<String, Any?>)?.get("content") as? Map<String, Any?>
    val text = content?.get("text") as? String ?: return ask(notesRequest(service), "notes", secret, "deploy", arguments, principal, now)
    return complete("Deployed $service to production. Release notes: $text")
}
