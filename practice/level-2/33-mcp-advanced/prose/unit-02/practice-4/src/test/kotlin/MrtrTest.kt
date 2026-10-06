import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MrtrTest {
    private val secret = "s3cret"
    private val version = "2026-07-28"
    private val both: Map<String, Any?> = mapOf("elicitation" to emptyMap<String, Any?>(), "sampling" to emptyMap<String, Any?>())
    private val args: Map<String, Any?> = mapOf("service" to "api", "env" to "production")
    private val confirm: Map<String, Any?> = mapOf("confirm" to mapOf("method" to "elicitation/create", "params" to mapOf("mode" to "form", "message" to "Deploy api to production?", "requestedSchema" to
        mapOf("type" to "object", "properties" to mapOf("confirm" to mapOf("type" to "boolean", "title" to "Confirm the deployment")), "required" to listOf("confirm")))))
    private val notes: Map<String, Any?> = mapOf("notes" to mapOf("method" to "sampling/createMessage", "params" to mapOf(
        "messages" to listOf(mapOf("role" to "user", "content" to mapOf("type" to "text", "text" to "Write one sentence of release notes for api."))), "maxTokens" to 100)))
    private val yes: Map<String, Any?> = mapOf("confirm" to mapOf("action" to "accept", "content" to mapOf("confirm" to true)))

    private fun request(name: String = "deploy", arguments: Map<String, Any?>? = null, caps: Map<String, Any?>? = null, ver: String? = version, vararg extra: Pair<String, Any?>): Map<String, Any?> {
        val meta = linkedMapOf<String, Any?>()
        if (ver != null) meta["io.modelcontextprotocol/protocolVersion"] = ver
        meta["io.modelcontextprotocol/clientCapabilities"] = caps ?: both
        return linkedMapOf<String, Any?>("name" to name, "arguments" to (arguments ?: args), "_meta" to meta).also { it.putAll(extra) }
    }

    private fun call(req: Map<String, Any?>, now: Long = 1000, principal: String = "alice", sec: String = secret): Map<String, Any?> = callTool(req, sec, principal, now) ?: emptyMap()
    private fun retry(first: Map<String, Any?>, responses: Any?, caps: Map<String, Any?>? = null) = request(caps = caps, extra = arrayOf("inputResponses" to responses, "requestState" to first["requestState"]))
    private fun complete(text: String, isError: Boolean = false): Map<String, Any?> = mapOf("resultType" to "complete", "content" to listOf(mapOf("type" to "text", "text" to text)), "isError" to isError)

    @Suppress("UNCHECKED_CAST")
    private fun errorOf(result: Map<String, Any?>): List<Any?> {
        val e = (result["error"] as? Map<String, Any?>) ?: emptyMap()
        return listOf(e["code"], e["message"])
    }

    private fun state(vararg over: Pair<String, Any?>): Map<String, Any?> =
        linkedMapOf<String, Any?>("v" to 1, "tool" to "deploy", "digest" to argsDigest(args), "sub" to "alice", "exp" to 999, "step" to "confirm").also { it.putAll(over) }

    @Test
    fun m1_aProductionDeployTakesThreeRoundTripsAndKeepsNoState() {
        val first = call(request())
        assertEquals("input_required", first["resultType"])
        assertEquals(confirm, first["inputRequests"])
        assertTrue(first["requestState"] is String && !first.containsKey("content"))
        val second = call(retry(first, yes), 1010)
        assertEquals("input_required", second["resultType"])
        assertEquals(notes, second["inputRequests"])
        assertTrue(second["requestState"] is String && second["requestState"] != first["requestState"])
        val answer = mapOf("notes" to mapOf("role" to "assistant", "content" to mapOf("type" to "text", "text" to "Faster checkout."), "model" to "m", "stopReason" to "endTurn"))
        assertEquals(complete("Deployed api to production. Release notes: Faster checkout."), call(retry(second, answer), 1020))
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun e1_aStagingDeployAndAStatusCallFinishAtOnceAndTheToolListIsCacheable() {
        assertEquals(complete("Deployed api to staging"), call(request("deploy", mapOf("service" to "api", "env" to "staging"), emptyMap())))
        assertEquals(complete("api: running"), call(request("status", mapOf("service" to "api"), emptyMap())))
        val listing = listTools(mapOf("_meta" to mapOf("io.modelcontextprotocol/protocolVersion" to version))) ?: emptyMap()
        assertEquals(listOf<Any?>("complete", 300000, "public"), listOf(listing["resultType"], listing["ttlMs"], listing["cacheScope"]))
        val tools = (listing["tools"] as? List<Map<String, Any?>>) ?: emptyList()
        assertEquals(listOf<Any?>("deploy", "status"), tools.map { it["name"] })
        assertEquals(listOf<Any?>(listOf("service", "env"), listOf("service")), tools.map { (it["inputSchema"] as Map<String, Any?>)["required"] })
    }

    @Test
    fun e2_theServerOnlyAsksWhatTheClientDeclaredItCanAnswer() {
        val refusal = "Deploying to production needs confirmation, and this client cannot be asked."
        assertEquals(complete(refusal, true), call(request(caps = emptyMap())))
        assertEquals(complete(refusal, true), call(request(caps = mapOf("sampling" to emptyMap<String, Any?>()))))
        assertEquals(complete(refusal, true), call(request(caps = mapOf("elicitation" to mapOf("url" to emptyMap<String, Any?>())))))
        val formOnly: Map<String, Any?> = mapOf("elicitation" to mapOf("form" to emptyMap<String, Any?>()))
        val first = call(request(caps = formOnly))
        assertEquals(confirm, first["inputRequests"])
        assertEquals(complete("Deployed api to production"), call(retry(first, yes, formOnly), 1010))
    }

    @Test
    fun e3_aNoOrAMissingAnswerIsHandledWithoutAnError() {
        val first = call(request())
        val answers = listOf(mapOf("action" to "decline"), mapOf("action" to "cancel"), mapOf("action" to "accept", "content" to mapOf("confirm" to false)), mapOf("action" to "accept"),
            mapOf("action" to "decline", "content" to mapOf("confirm" to true)))
        for (answer in answers) assertEquals(complete("Deployment cancelled"), call(retry(first, mapOf("confirm" to answer)), 1010))
        for (responses in listOf<Any?>(emptyMap<String, Any?>(), mapOf("other" to 1), mapOf("confirm" to mapOf("action" to "maybe")), mapOf("confirm" to "yes"))) {
            val again = call(retry(first, responses), 1100)
            assertEquals("input_required", again["resultType"])
            assertEquals(confirm, again["inputRequests"])
            assertTrue(again["requestState"] != null && again["requestState"] != first["requestState"])
        }
        val second = call(retry(first, yes), 1010)
        assertEquals(notes, call(retry(second, emptyMap<String, Any?>()), 1020)["inputRequests"])
        assertEquals(notes, call(retry(second, mapOf("notes" to mapOf("role" to "assistant", "content" to mapOf("type" to "image")))), 1020)["inputRequests"])
    }

    @Test
    fun e4_aStateTheServerDidNotSignIsRefused() {
        val first = call(request())
        val token = first["requestState"] as? String ?: "x.y"
        val flipped = token.dropLast(1) + (if (token.endsWith("A")) "B" else "A")
        val foreign = call(request(), 1000, "alice", "another secret")["requestState"]
        for (bad in listOf(flipped, "abc", "", foreign)) {
            assertEquals(listOf<Any?>(-32602, "Invalid requestState"), errorOf(call(request(extra = arrayOf("inputResponses" to yes, "requestState" to bad)), 1010)))
        }
    }

    private fun go(token: String, arguments: Map<String, Any?>? = null) = request(arguments = arguments, extra = arrayOf("inputResponses" to yes, "requestState" to token))

    @Test
    fun e5_aStateWorksOnlyForTheSameUserTheSameCallAndBeforeItExpires() {
        val same = "requestState does not match this request"
        val token = mintState(secret, state())
        assertEquals(notes, call(go(token), 999)["inputRequests"])
        assertEquals(listOf<Any?>(-32602, "Expired requestState"), errorOf(call(go(token), 1000)))
        assertEquals(listOf<Any?>(-32602, same), errorOf(call(go(token), 999, "bob")))
        assertEquals(listOf<Any?>(-32602, same), errorOf(call(go(token, mapOf("service" to "billing", "env" to "production")), 999)))
        assertEquals(listOf<Any?>(-32602, same), errorOf(call(go(mintState(secret, state("tool" to "status"))), 999)))
        assertEquals(notes, call(go(mintState(secret, state("step" to "notes"))), 999)["inputRequests"])
    }

    @Test
    fun e6_aRequestTheServerCannotServeIsAProtocolErrorWithACode() {
        val old = call(request(ver = "2025-11-25"))
        assertEquals(listOf<Any?>(-32022, "Unsupported protocol version"), errorOf(old))
        assertEquals(mapOf("supported" to listOf(version)), (old["error"] as? Map<*, *>)?.get("data"))
        assertEquals(-32022, errorOf(call(request(ver = null)))[0])
        assertEquals(listOf<Any?>(-32602, "Unknown tool: rollback"), errorOf(call(request("rollback"))))
        assertEquals(listOf<Any?>(-32602, "Invalid params: service is required"), errorOf(call(request(arguments = mapOf("env" to "staging")))))
        assertEquals(listOf<Any?>(-32602, "Invalid params: service is required"), errorOf(call(request(arguments = mapOf("service" to "  ", "env" to "staging")))))
        assertEquals(listOf<Any?>(-32602, "Invalid params: env must be staging or production"), errorOf(call(request(arguments = mapOf("service" to "api", "env" to "dev")))))
        assertEquals("Invalid params: service is required", errorOf(call(request("status", emptyMap())))[1])
        assertEquals(-32022, errorOf(listTools(mapOf("_meta" to mapOf("io.modelcontextprotocol/protocolVersion" to "2024-11-05"))) ?: emptyMap())[0])
    }

    @Test
    fun e7_theStateCarriesTheWholeContextAndAnAnswerAloneNeverSkipsAStep() {
        val first = call(request(), 1000)
        val again = call(request(), 1000)
        assertEquals(first["inputRequests"], again["inputRequests"])
        val payload = (first["requestState"] as? String)?.let { readState(secret, it) } ?: emptyMap()
        assertEquals(listOf<Any?>(1L, "deploy", argsDigest(args), "alice", 1300L, "confirm"),
            listOf((payload["v"] as? Number)?.toLong(), payload["tool"], payload["digest"], payload["sub"], (payload["exp"] as? Number)?.toLong(), payload["step"]))
        assertEquals(argsDigest(args), argsDigest(mapOf("env" to "production", "service" to "api")))
        val alone = call(request(extra = arrayOf("inputResponses" to yes)))
        assertEquals("input_required", alone["resultType"])
        assertEquals(confirm, alone["inputRequests"])
        assertEquals(notes, call(retry(first, yes + ("junk" to mapOf("action" to "accept"))), 1010)["inputRequests"])
        val second = call(retry(first, yes), 1010)
        assertEquals("notes", ((second["requestState"] as? String)?.let { readState(secret, it) })?.get("step") ?: "none")
        val before = LinkedHashMap(first)
        call(retry(first, yes), 1010)
        assertEquals(before, first)
    }
}
