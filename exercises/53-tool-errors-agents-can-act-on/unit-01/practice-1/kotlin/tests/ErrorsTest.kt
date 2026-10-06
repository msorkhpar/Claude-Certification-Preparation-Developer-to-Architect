import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ErrorsTest {
    private fun args(): Map<String, Any?> = linkedMapOf("order" to "A-7", "amount" to 40)
    private fun policy(maxRetries: Int, vararg more: Pair<String, Any?>): Map<String, Any?> = linkedMapOf<String, Any?>("max_retries" to maxRetries, "base_delay_ms" to 100).also { it.putAll(more) }
    private fun err(kind: String, message: String) = ToolError(kind, message)

    /** A scripted tool: each call takes the next script entry (a RuntimeException is thrown, anything else is returned); every call is kept. */
    private class Tool(vararg steps: Any?) : (Map<String, Any?>) -> Any? {
        val script = steps.toMutableList()
        val calls = mutableListOf<Map<String, Any?>>()

        override fun invoke(args: Map<String, Any?>): Any? {
            calls.add(LinkedHashMap(args))
            val step = if (script.size > 1) script.removeAt(0) else script[0]
            if (step is RuntimeException) throw step
            return step
        }
    }

    private class Run(val result: Map<String, Any?>, val waits: List<Int>)

    private fun run(tool: Tool, policy: Map<String, Any?> = policy(2), args: Map<String, Any?> = args()): Run {
        val waits = mutableListOf<Int>()
        val result = runTool(tool, args, policy) { waits.add(it) }
        assertNotNull(result, "runTool returned nothing")
        return Run(result!!, waits)
    }

    private fun mk(kind: String, message: String, explanation: String? = null): Map<String, Any?> {
        val result = makeError(kind, message, explanation)
        assertNotNull(result, "makeError returned nothing")
        return result!!
    }

    @Test
    fun m1_aFailedCallBecomesAStructuredErrorWithACategoryARetryFlagAndAnErrorFlag() {
        val error = mk("transient", "The billing service timed out after 5 s.")
        assertEquals(mapOf("is_error" to true, "category" to "transient", "retryable" to true, "message" to "The billing service timed out after 5 s.", "attempts" to 1), error)
        assertEquals(mapOf("type" to "tool_result", "tool_use_id" to "toolu_1", "content" to "transient error (retryable: yes): The billing service timed out after 5 s.", "is_error" to true), toToolResult("toolu_1", error))
        val business = mk("business", "Refunds above 500 need a person.", "A colleague will contact you about this refund.")
        assertEquals(false, business["retryable"])
        assertEquals("A colleague will contact you about this refund.", business["explanation"])
        assertEquals("business error (retryable: no): Refunds above 500 need a person. Tell the customer: A colleague will contact you about this refund.", toToolResult("toolu_2", business)!!["content"])
        for (kind in listOf("validation", "permission", "outcome_unknown", "internal")) assertEquals(false, mk(kind, "Specific text.")["retryable"])
        assertEquals(mapOf("type" to "tool_result", "tool_use_id" to "toolu_3", "content" to "refund R-1 created", "is_error" to false), toToolResult("toolu_3", mapOf("ok" to true, "content" to "refund R-1 created")))
    }

    @Test
    fun e1_aGenericMessageOrAnUnknownCategoryIsRefused() {
        var refused = 0
        for (message in listOf("Operation failed", "", "   ", "Failed.", "Error", "Something went wrong")) {
            try {
                makeError("transient", message)
            } catch (expected: IllegalArgumentException) {
                refused++
            }
        }
        assertEquals(6, refused)
        refused = 0
        for (kind in listOf("oops", "timeout", "", "Transient")) {
            try {
                makeError(kind, "A specific message that says what to change.")
            } catch (expected: IllegalArgumentException) {
                refused++
            }
        }
        assertEquals(4, refused)
    }

    @Test
    fun e2_onlyTransientFailuresAreRetriedWithGrowingWaitsAndTheOtherKindsReturnAtOnce() {
        var tool = Tool(err("transient", "Billing is unavailable."), err("transient", "Billing is unavailable."), "refund R-1 created")
        var r = run(tool)
        assertEquals(true, r.result["ok"])
        assertEquals("refund R-1 created", r.result["content"])
        assertEquals(3, r.result["attempts"])
        assertEquals(listOf(100, 200), r.waits)
        assertEquals(3, tool.calls.size)
        for ((kind, message, explanation) in listOf(Triple("validation", "amount must be a positive whole number", null), Triple("permission", "this key may not issue refunds", null), Triple("business", "Refunds above 500 need a person.", "A colleague will contact you."))) {
            tool = Tool(ToolError(kind, message, null, explanation), "never reached")
            r = run(tool)
            assertEquals(true, r.result["is_error"])
            assertEquals(kind, r.result["category"])
            assertEquals(false, r.result["retryable"])
            assertEquals(message, r.result["message"])
            assertEquals(explanation, r.result["explanation"])
            assertEquals(1, r.result["attempts"])
            assertEquals(args(), r.result["attempted"])
            assertTrue(r.waits.isEmpty())
            assertEquals(1, tool.calls.size)
        }
    }

    @Test
    fun e3_theRetriesAreBoundedAndAWaitTheServiceAsksForIsHonoured() {
        var tool = Tool(err("transient", "Billing is unavailable."))
        var r = run(tool)
        assertEquals(3, tool.calls.size)
        assertEquals(listOf(100, 200), r.waits)
        assertEquals(true, r.result["is_error"])
        assertEquals("transient", r.result["category"])
        assertEquals(true, r.result["retryable"])
        assertEquals(3, r.result["attempts"])
        assertTrue((r.result["message"] as String).contains("Gave up after 3 attempts"))
        assertEquals(args(), r.result["attempted"])
        tool = Tool(ToolError("transient", "Rate limited.", 1500), err("transient", "Rate limited."), "ok")
        r = run(tool)
        assertEquals(listOf(1500, 200), r.waits)
        assertEquals(true, r.result["ok"])
        assertEquals(3, r.result["attempts"])
        tool = Tool(err("transient", "Billing is unavailable."))
        r = run(tool, policy(0))
        assertEquals(1, tool.calls.size)
        assertTrue(r.waits.isEmpty())
        assertEquals(1, r.result["attempts"])
        assertEquals("transient", r.result["category"])
    }

    @Test
    fun e4_aValidEmptyResultIsASuccessAndNotAnError() {
        for (empty in listOf<Any?>(emptyList<Any>(), "", emptyMap<String, Any>(), null)) {
            val r = run(Tool(empty))
            assertEquals(true, r.result["ok"])
            assertEquals(true, r.result["empty"])
            assertEquals(1, r.result["attempts"])
            assertTrue(r.waits.isEmpty())
            assertEquals("accept_empty", nextAction(r.result))
        }
        val block = toToolResult("toolu_1", mapOf("ok" to true, "content" to ""))!!
        assertEquals(false, block["is_error"])
        assertEquals("", block["content"])
        val full = run(Tool("3 orders"))
        assertEquals(false, full.result["empty"])
        assertEquals("continue", nextAction(full.result))
    }

    @Test
    fun e5_aTimeoutOnAWriteIsAnUnknownOutcomeAndIsRetriedOnlyWhenRepeatingItIsSafe() {
        var tool = Tool(err("timeout", "No answer from the refund service."), "refund R-1 created")
        var r = run(tool)
        assertEquals(1, tool.calls.size)
        assertTrue(r.waits.isEmpty())
        assertEquals(true, r.result["is_error"])
        assertEquals("outcome_unknown", r.result["category"])
        assertEquals(false, r.result["retryable"])
        val message = r.result["message"] as String
        assertTrue(message.contains("No answer from the refund service.") && message.contains("check the current state"))
        assertEquals(args(), r.result["attempted"])
        tool = Tool(err("timeout", "No answer."), "3 orders")
        r = run(tool, policy(2, "read_only" to true))
        assertEquals(true, r.result["ok"])
        assertEquals(2, r.result["attempts"])
        assertEquals(listOf(100), r.waits)
        tool = Tool(err("timeout", "No answer."), err("timeout", "No answer."), "refund R-1 created")
        val mine = args()
        r = run(tool, policy(2, "idempotency_key" to "k-1"), mine)
        assertEquals(true, r.result["ok"])
        assertEquals(3, r.result["attempts"])
        assertEquals(listOf(100, 200), r.waits)
        val keyed = args() + ("idempotency_key" to "k-1")
        assertEquals(listOf(keyed, keyed, keyed), tool.calls)
        assertEquals(args(), mine, "the caller's arguments must not be changed")
    }

    @Test
    fun e6_theNextActionFollowsTheCategory() {
        val expected = mapOf("transient" to "retry_later", "validation" to "repair_input", "permission" to "escalate", "business" to "explain", "outcome_unknown" to "verify_first", "internal" to "escalate")
        for ((kind, action) in expected) assertEquals(action, nextAction(mk(kind, "A specific message.")))
        assertEquals("continue", nextAction(mapOf("ok" to true, "content" to "x", "empty" to false)))
        assertEquals("accept_empty", nextAction(mapOf("ok" to true, "content" to emptyList<Any>(), "empty" to true)))
    }

    @Test
    fun e7_anUnexpectedExceptionBecomesAnInternalErrorAndTheRunGoesOn() {
        for (boom in listOf(IllegalStateException("boom"), NullPointerException("missing"))) {
            val tool = Tool(boom, "never reached")
            val r = run(tool)
            assertEquals(true, r.result["is_error"])
            assertEquals("internal", r.result["category"])
            assertEquals(false, r.result["retryable"])
            assertEquals(1, r.result["attempts"])
            assertTrue(r.waits.isEmpty())
            assertEquals(1, tool.calls.size)
        }
        val r = run(Tool(IllegalStateException("boom")))
        assertTrue((r.result["message"] as String).contains("boom"))
        assertEquals(args(), r.result["attempted"])
    }
}
