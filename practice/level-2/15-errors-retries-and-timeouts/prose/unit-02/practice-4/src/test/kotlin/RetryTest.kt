import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RetryTest {
    private val ok = Response(200, emptyMap(), mapOf("type" to "message"))

    private fun status(code: Int, headers: Map<String, String> = emptyMap(), kind: String? = null, details: Map<String, Any?>? = null): Response {
        val error = linkedMapOf<String, Any?>("type" to (kind ?: "api_error"), "message" to "x")
        if (details != null) error["details"] = details
        return Response(code, headers, mapOf("type" to "error", "error" to error))
    }

    /** What one run produced: the calls made, the waits requested, the result or the error. */
    private class Run {
        var calls = 0
        val slept = mutableListOf<Double>()
        var result: Response? = null
        var err: RuntimeException? = null
    }

    private fun run(policy: Policy, vararg replies: Any): Run {
        val r = Run()
        val queue = replies.toMutableList()
        val send = {
            r.calls++
            val item = queue.removeAt(0)
            if (item is RuntimeException) throw item
            item as Response
        }
        try {
            r.result = callWithRetry(send, { r.slept.add(it) }, policy)
        } catch (e: RuntimeException) {
            r.err = e
        }
        return r
    }

    private fun run(vararg replies: Any) = run(Policy(), *replies)

    private fun with(maxAttempts: Int, base: Double, cap: Double) = Policy(maxAttempts, base, cap)

    @Test
    fun m1_overloadedTwiceThenSuccessReturnsTheGoodReplyAfterTwoWaits() {
        val r = run(status(529, kind = "overloaded_error"), status(503), ok)
        assertNull(r.err)
        assertSame(ok, r.result)
        assertEquals(3, r.calls)
        assertEquals(listOf(0.5, 1.0), r.slept)
    }

    @Test
    fun e1_clientErrorsAreNotRetried() {
        for ((code, kind) in listOf(400 to "invalid_request_error", 401 to "authentication_error", 404 to "not_found_error", 413 to "request_too_large")) {
            val r = run(status(code, mapOf("request-id" to "req_1"), kind), ok)
            val err = r.err
            assertTrue(err is CallFailed, "$code: $err")
            err as CallFailed
            assertEquals(listOf<Any?>(code, kind, 1, "req_1"), listOf(err.status, err.errorType, err.attempts, err.requestId))
            assertTrue(r.calls == 1 && r.slept.isEmpty())
        }
    }

    @Test
    fun e2_retryAfterIsAFloorForTheWait() {
        val r = run(
            with(5, 0.5, 8.0),
            status(429, mapOf("retry-after" to "3"), "rate_limit_error"),
            status(429, mapOf("retry-after" to "1"), "rate_limit_error"),
            status(529, mapOf("retry-after" to "1")),
            ok,
        )
        assertNull(r.err)
        assertEquals(listOf(3.0, 1.0, 2.0), r.slept) // waits: max(0.5, 3), max(1, 1), max(2, 1)
    }

    @Test
    fun e3_delayDoublesUpToTheCapAndTheJitterIsAppliedLast() {
        val r = run(with(7, 1.0, 5.0), status(500), status(500), status(500), status(500), status(500), status(500), ok)
        assertNull(r.err)
        assertEquals(listOf(1.0, 2.0, 4.0, 5.0, 5.0, 5.0), r.slept)
        val halved = run(Policy(4, 0.5, 8.0) { it / 2 }, status(500), status(500), ok)
        assertEquals(listOf(0.25, 0.5), halved.slept)
    }

    @Test
    fun e4_aSpendCap429IsNotRetried() {
        val cap = status(429, mapOf("request-id" to "req_cap"), "rate_limit_error", mapOf("error_code" to "enforced_spend_limit_reached"))
        val r = run(cap, ok)
        val err = r.err
        assertTrue(err is CallFailed, "got $err")
        err as CallFailed
        assertEquals(listOf<Any?>(429, 1, "req_cap"), listOf(err.status, err.attempts, err.requestId))
        assertTrue(r.calls == 1 && r.slept.isEmpty())
    }

    @Test
    fun e5_connectionErrorsAreRetriedLikeServerErrors() {
        val r = run(TransportError("reset"), TransportError("timeout"), ok)
        assertNull(r.err)
        assertSame(ok, r.result)
        assertEquals(3, r.calls)
        assertEquals(listOf(0.5, 1.0), r.slept)
        val dead = run(with(2, 0.5, 8.0), TransportError("a"), TransportError("b"))
        val err = dead.err
        assertTrue(err is CallFailed, "got $err")
        err as CallFailed
        assertEquals(listOf<Any?>(0, "connection_error", 2), listOf(err.status, err.errorType, err.attempts))
        assertNull(err.requestId)
    }

    @Test
    fun e6_givingUpReportsTheLastReplyAndDoesNotWaitAfterTheLastAttempt() {
        val r = run(
            with(3, 0.5, 8.0),
            status(500, mapOf("request-id" to "req_a")),
            status(503, mapOf("request-id" to "req_b")),
            status(529, mapOf("request-id" to "req_c"), "overloaded_error"),
            ok,
        )
        val err = r.err
        assertTrue(err is CallFailed, "got $err")
        err as CallFailed
        assertTrue(r.calls == 3 && r.slept.size == 2)
        assertEquals(listOf<Any?>(529, "overloaded_error", 3, "req_c"), listOf(err.status, err.errorType, err.attempts, err.requestId))
    }
}
