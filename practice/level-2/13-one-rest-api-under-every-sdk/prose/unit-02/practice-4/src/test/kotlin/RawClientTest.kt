import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RawClientTest {
    private val key = "sk-test-0123456789abcdef"

    @Suppress("UNCHECKED_CAST")
    private fun list(json: String) = Json.parse(json) as List<Map<String, Any?>>

    private val msgs = list("""[{"role":"user","content":"Capital of France?"}]""")
    private val okBody = """{"id":"msg_x","type":"message","role":"assistant","model":"claude-sonnet-5-5",
        "content":[{"type":"text","text":"Paris."}],"stop_reason":"end_turn","stop_sequence":null,
        "usage":{"input_tokens":9,"output_tokens":3}}"""

    private fun reply(status: Int, body: String, headers: Map<String, String> = emptyMap()) = Response(status, headers, body)

    /** The exception the call threw, or null when it did not throw. */
    private fun errorOf(call: () -> Unit): RuntimeException? =
        try {
            call()
            null
        } catch (e: RuntimeException) {
            e
        }

    @Test
    fun m1_requestHasMethodUrlThreeHeadersAndJsonBody() {
        val req = buildRequest(key, "claude-sonnet-5-5", msgs, 64, "Be brief.")
        assertEquals("POST", req.method)
        assertEquals("https://api.anthropic.com/v1/messages", req.url)
        assertEquals(mapOf("x-api-key" to key, "anthropic-version" to "2023-06-01", "content-type" to "application/json"), req.headers)
        assertEquals(
            Json.parse("""{"model":"claude-sonnet-5-5","max_tokens":64,"messages":[{"role":"user","content":"Capital of France?"}],"system":"Be brief."}"""),
            Json.parse(req.body),
        )
    }

    @Test
    fun e1_blankOrAbsentSystemIsLeftOutOfTheBody() {
        for (system in listOf(null, "", "   ")) {
            val body = Json.parse(buildRequest(key, "m", msgs, 8, system).body) as Map<*, *>
            assertFalse(body.containsKey("system"), system.toString())
        }
        val withSystem = Json.parse(buildRequest(key, "m", msgs, 8, "x").body) as Map<*, *>
        assertEquals("x", withSystem["system"])
    }

    @Test
    fun e2_badInputIsRefusedBeforeAnythingIsSent() {
        var calls = 0
        val transport = Transport { calls++; reply(200, okBody) }
        val errors = listOf(
            errorOf { sendMessages(transport, key, "m", msgs, 0) },
            errorOf { sendMessages(transport, key, "m", msgs, -5) },
            errorOf { sendMessages(transport, key, "m", emptyList(), 8) },
            errorOf { sendMessages(transport, "  ", "m", msgs, 8) },
        )
        for (e in errors) assertTrue(e is IllegalArgumentException, "got $e")
        assertEquals(0, calls)
    }

    @Test
    fun e3_successReturnsTheMessageAndTextJoinsTextBlocksOnly() {
        val seen = mutableListOf<Request>()
        val message = sendMessages({ r -> seen.add(r); reply(200, okBody) }, key, "claude-sonnet-5-5", msgs, 64)
        assertTrue(message["stop_reason"] == "end_turn" && seen.size == 1 && seen[0].method == "POST")
        @Suppress("UNCHECKED_CAST")
        val mixed = Json.parse("""{"content":[{"type":"text","text":"Let me "},{"type":"tool_use","id":"t","name":"x","input":{}},{"type":"text","text":"check."}]}""") as Map<String, Any?>
        assertEquals("Let me check.", textOf(mixed))
        assertEquals("", textOf(mapOf("content" to emptyList<Any>())))
    }

    @Test
    fun e4_anErrorReplyBecomesAnApiErrorWithTheHeaderRequestId() {
        val body = """{"type":"error","error":{"type":"rate_limit_error","message":"Rate limited"},"request_id":"req_from_body"}"""
        val e = errorOf { sendMessages({ reply(429, body, mapOf("request-id" to "req_from_header")) }, key, "m", msgs, 8) }
        assertTrue(e is ApiError, "got $e")
        e as ApiError
        assertEquals(429, e.status)
        assertEquals("rate_limit_error", e.errorType)
        assertEquals("Rate limited", e.detail)
        assertEquals("req_from_header", e.requestId)
        val overloaded = """{"type":"error","error":{"type":"overloaded_error","message":"Overloaded"},"request_id":"req_from_body"}"""
        val e2 = errorOf { sendMessages({ reply(529, overloaded) }, key, "m", msgs, 8) }
        assertTrue(e2 is ApiError, "got $e2")
        e2 as ApiError
        assertEquals("req_from_body", e2.requestId)
        assertEquals("overloaded_error", e2.errorType)
    }

    @Test
    fun e5_aReplyThatIsNotJsonStillGivesAnApiError() {
        val html = "<html><body><h1>502 Bad Gateway</h1></body></html>"
        val e = errorOf { sendMessages({ reply(502, html) }, key, "m", msgs, 8) }
        assertTrue(e is ApiError, "got $e")
        e as ApiError
        assertEquals(502, e.status)
        assertEquals("unknown", e.errorType)
        assertTrue(e.detail.contains("502 Bad Gateway"))
        assertNull(e.requestId)
    }

    @Test
    fun e6_theApiKeyNeverAppearsInAnError() {
        val body = """{"type":"error","error":{"type":"authentication_error","message":"invalid x-api-key: $key"}}"""
        val e = errorOf { sendMessages({ reply(401, body) }, key, "m", msgs, 8) }
        assertTrue(e is ApiError, "got $e")
        e as ApiError
        assertFalse(e.message!!.contains(key) || e.detail.contains(key))
        assertTrue(e.detail.contains("[redacted]"))
    }
}
