import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

private val log = System.getLogger("mrtr")

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

/**
 * TODO 1 of 8 (unlocks e1): the result of tools/list.
 * Receives nothing and returns the map from the statement: resultType "complete", the TOOLS sorted by name, ttlMs 300000, cacheScope "public".
 * Example: toolListing()["tools"] first name -> "deploy"
 */
private fun toolListing(): Map<String, Any?> = emptyMap()

fun listTools(request: Map<String, Any?>): Map<String, Any?> = metaError(request) ?: toolListing()

private fun complete(text: String, isError: Boolean = false): Map<String, Any?> =
    mapOf("resultType" to "complete", "content" to listOf(mapOf("type" to "text", "text" to text)), "isError" to isError)

private fun confirmRequest(service: String): Map<String, Any?> = mapOf("confirm" to mapOf("method" to "elicitation/create", "params" to mapOf("mode" to "form", "message" to "Deploy $service to production?",
    "requestedSchema" to mapOf("type" to "object", "properties" to mapOf("confirm" to mapOf("type" to "boolean", "title" to "Confirm the deployment")), "required" to listOf("confirm")))))

private fun notesRequest(service: String): Map<String, Any?> = mapOf("notes" to mapOf("method" to "sampling/createMessage", "params" to mapOf(
    "messages" to listOf(mapOf("role" to "user", "content" to mapOf("type" to "text", "text" to "Write one sentence of release notes for $service."))), "maxTokens" to 100)))

/**
 * TODO 2 of 8 (unlocks e7): the payload of a requestState.
 * Receives the tool name, the arguments, the user, the time in seconds and the step. Returns the map from the statement: v, tool, digest, sub, exp, step.
 * Example: newState("deploy", mapOf("service" to "api"), "alice", 1000, "confirm")["exp"] -> 1300
 */
private fun newState(name: Any?, arguments: Map<String, Any?>, principal: String, now: Long, step: String): Map<String, Any?> = emptyMap()

/**
 * TODO 3 of 8 (unlocks e6): the protocol error for a call that cannot be served, in the order of the statement.
 * Receives the tool name and the arguments. Returns error(-32602, ...) for an unknown tool, a missing or blank service, or (for deploy) an env
 * that is not staging or production; null when the call is valid.
 * Example: validate("deploy", mapOf("service" to "api", "env" to "dev")) -> error(-32602, "Invalid params: env must be staging or production")
 */
private fun validate(name: Any?, arguments: Map<String, Any?>): Map<String, Any?>? = null

/**
 * TODO 4 of 8 (unlocks e2): can the client be asked for a confirmation?
 * Receives the client capabilities map. Returns true when "elicitation" is present and is empty or holds "form"; false for none or only "url".
 * Example: canElicit(mapOf("elicitation" to emptyMap<String, Any?>())) -> true, canElicit(mapOf("elicitation" to mapOf("url" to emptyMap<String, Any?>()))) -> false
 */
private fun canElicit(caps: Map<String, Any?>): Boolean = false

/**
 * TODO 5 of 8 (unlocks e4 and e5): the error for a requestState that cannot be used.
 * Receives the secret, the token, the user, the tool name, the arguments and the time. Returns error(-32602, ...) with "Invalid requestState"
 * (readState gives null), "Expired requestState" (now after exp) or "requestState does not match this request" (sub, tool or digest differ), checked in
 * that order; null when the state is good.
 * Example: a state minted for "alice" and read for "bob" -> error(-32602, "requestState does not match this request")
 */
private fun stateError(secret: String, token: String, principal: String, name: Any?, arguments: Map<String, Any?>, now: Long): Map<String, Any?>? = null

/**
 * TODO 6 of 8 (unlocks m1 and e3): is this an answer to the confirmation question?
 * Receives inputResponses.confirm (anything, or null). Returns true when it is a map whose "action" is accept, decline or cancel.
 * Example: confirmUsable(mapOf("action" to "decline")) -> true, confirmUsable("yes") -> false
 */
private fun confirmUsable(answer: Any?): Boolean = false

/**
 * TODO 7 of 8 (unlocks e3): did the person accept?
 * Receives a usable answer. Returns true only for action "accept" with content.confirm exactly true.
 * Example: confirmed(mapOf("action" to "accept", "content" to mapOf("confirm" to false))) -> false
 */
private fun confirmed(answer: Map<String, Any?>): Boolean = false

/**
 * TODO 8 of 8 (unlocks m1): the release notes the client wrote.
 * Receives the inputResponses map. Returns answers["notes"]["content"]["text"] when that is a string; null for anything else.
 * Example: notesText(mapOf("notes" to mapOf("content" to mapOf("type" to "text", "text" to "Faster.")))) -> "Faster."
 */
private fun notesText(answers: Map<String, Any?>): String? = null

private fun ask(requests: Map<String, Any?>, step: String, secret: String, name: String, arguments: Map<String, Any?>, principal: String, now: Long): Map<String, Any?> {
    val state = newState(name, arguments, principal, now, step)
    return mapOf("resultType" to "input_required", "inputRequests" to requests, "requestState" to mintState(secret, state))
}

@Suppress("UNCHECKED_CAST")
fun callTool(request: Map<String, Any?>, secret: String, principal: String, now: Long): Map<String, Any?> {
    log.log(System.Logger.Level.DEBUG, "callTool input: {0}", request)
    metaError(request)?.let { return it }
    val name = request["name"]
    val arguments = (request["arguments"] as? Map<String, Any?>) ?: emptyMap()
    validate(name, arguments)?.let { return it }
    val service = arguments["service"] as String
    if (name == "status") return complete("$service: running")
    if (arguments["env"] == "staging") return complete("Deployed $service to staging")
    val caps = (meta(request)[META_CAPS] as? Map<String, Any?>) ?: emptyMap()
    if (!canElicit(caps)) return complete("Deploying to production needs confirmation, and this client cannot be asked.", true)
    var step = "confirm"
    val token = request["requestState"]
    if (token != null) {
        stateError(secret, token.toString(), principal, name, arguments, now)?.let { return it }
        step = readState(secret, token.toString())?.get("step").toString()
    }
    val answers = if (token != null) (request["inputResponses"] as? Map<String, Any?>) ?: emptyMap() else emptyMap()
    if (step == "confirm") {
        val answer = answers["confirm"]
        if (!confirmUsable(answer)) return ask(confirmRequest(service), "confirm", secret, "deploy", arguments, principal, now)
        if (!confirmed(answer as Map<String, Any?>)) return complete("Deployment cancelled")
        if (!caps.containsKey("sampling")) return complete("Deployed $service to production")
        return ask(notesRequest(service), "notes", secret, "deploy", arguments, principal, now)
    }
    val text = notesText(answers) ?: return ask(notesRequest(service), "notes", secret, "deploy", arguments, principal, now)
    return complete("Deployed $service to production. Release notes: $text")
}
