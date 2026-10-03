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

fun listTools(request: Map<String, Any?>): Map<String, Any?>? {
    // TODO: the tools sorted by name, as a complete result with ttlMs and cacheScope; a request of another protocol version is a protocol error.
    return null
}

fun callTool(request: Map<String, Any?>, secret: String, principal: String, now: Long): Map<String, Any?>? {
    // TODO: run the tool; when the server needs the client's input, return an input_required result with a signed requestState. No state is kept here.
    return null
}
