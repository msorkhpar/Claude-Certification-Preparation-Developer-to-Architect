import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DistributeTest {
    private fun tool(name: String, tag: String, vararg more: Pair<String, Any?>): Map<String, Any?> = linkedMapOf<String, Any?>("name" to name, "tags" to listOf(tag)).also { it.putAll(more) }

    private fun catalog() = listOf(tool("web_search", "web"), tool("fetch_page", "web"), tool("load_document", "documents"), tool("extract_data_points", "documents"),
        tool("summarize_content", "synthesis"), tool("verify_fact", "web", "scoped" to true), tool("write_report", "reports"), tool("publish_report", "reports", "irreversible" to true))

    private fun role(specialisation: String, vararg extra: String): Map<String, Any?> =
        if (extra.isEmpty()) linkedMapOf("specialisation" to listOf(specialisation)) else linkedMapOf("specialisation" to listOf(specialisation), "extra" to extra.toList())

    private fun roles(): Map<String, Map<String, Any?>> = linkedMapOf("searcher" to role("web"), "analyst" to role("documents"), "synthesizer" to role("synthesis", "verify_fact"), "reporter" to role("reports"))

    private fun assign(roles: Map<String, Map<String, Any?>> = roles(), budget: Int = 5): Map<String, List<String>> {
        val result = assignTools(roles, catalog(), budget)
        assertNotNull(result, "assignTools returned nothing")
        return result!!
    }

    private fun policy(): Map<String, Any?> = mapOf("tools" to mapOf("process_refund" to mapOf("cap" to 500, "irreversible" to true), "lookup_order" to mapOf("cap" to null, "irreversible" to false)))

    private fun call(vararg over: Pair<String, Any?>): Map<String, Any?> =
        linkedMapOf<String, Any?>("id" to "c1", "tool" to "process_refund", "amount" to 50, "customer" to "C-1", "verified_customer" to "C-1").also { it.putAll(over) }

    private fun decide(c: Map<String, Any?>, approvals: Set<String> = emptySet()): Map<String, Any?> {
        val answer = authorize(c, policy(), approvals)
        assertNotNull(answer, "authorize returned nothing")
        return answer!!
    }

    private fun plan(model: String, need: String, tools: List<String>, forced: String? = null, manual: Boolean = false): Map<String, Any?> {
        val result = planTurn(model, need, tools, forced, manual)
        assertNotNull(result, "planTurn returned nothing")
        return result!!
    }

    private fun turn(choice: Any?, tools: List<String>, strict: Boolean): Map<String, Any?> = mapOf("tool_choice" to choice, "tools" to tools, "strict" to strict, "verify_call" to strict)

    @Test
    fun m1_eachRoleGetsOnlyTheToolsOfItsSpecialisationInCatalogOrder() {
        assertEquals(mapOf("searcher" to listOf("web_search", "fetch_page", "verify_fact"), "analyst" to listOf("load_document", "extract_data_points"),
            "synthesizer" to listOf("summarize_content", "verify_fact"), "reporter" to listOf("write_report")), assign())
    }

    @Test
    fun e1_aRoleOverItsBudgetOrGivenAnUnknownOrUnscopedOutsideToolOrADuplicateCatalogNameIsRefused() {
        var refused = 0
        val duplicated = catalog() + tool("web_search", "web")
        val cases = listOf(
            Triple(roles(), catalog(), 2),
            Triple(mapOf("synthesizer" to role("synthesis", "nope")), catalog(), 5),
            Triple(mapOf("synthesizer" to role("synthesis", "fetch_page")), catalog(), 5),
            Triple(roles(), duplicated, 5),
            Triple(mapOf("nobody" to mapOf<String, Any?>("specialisation" to emptyList<String>())), catalog(), 5),
        )
        for ((r, c, b) in cases) {
            try {
                assignTools(r, c, b)
            } catch (expected: IllegalArgumentException) {
                refused++
            }
        }
        assertEquals(5, refused)
        assertEquals(listOf("web_search", "fetch_page", "verify_fact"), assign(budget = 3)["searcher"])
        assertEquals(listOf("web_search", "fetch_page", "verify_fact"), assign(mapOf("searcher" to role("web", "fetch_page")))["searcher"])
    }

    @Test
    fun e2_anIrreversibleToolIsGivenOnlyByAnExplicitGrant() {
        assertFalse(assign()["reporter"]!!.contains("publish_report"))
        assertEquals(listOf("write_report", "publish_report"), assign(mapOf("reporter" to role("reports", "publish_report")))["reporter"])
        assertEquals(listOf("load_document", "extract_data_points", "publish_report"), assign(mapOf("analyst" to role("documents", "publish_report")))["analyst"])
    }

    @Test
    fun e3_aModelThatAcceptsForcingGetsTheNativeChoiceAndTheOthersGetAutoWithStrictToolsAndOneNamedTool() {
        val tools = listOf("extract_metadata", "enrich")
        assertEquals(mapOf("tool_choice" to mapOf("type" to "tool", "name" to "extract_metadata"), "tools" to tools, "strict" to false, "verify_call" to false), plan("claude-opus-5", "named", tools, "extract_metadata"))
        assertEquals(mapOf("tool_choice" to mapOf("type" to "any"), "tools" to tools, "strict" to false, "verify_call" to false), plan("claude-opus-5", "any", tools))
        for (model in listOf("claude-opus-5-5", "claude-sonnet-5-5", "claude-fable-5-1", "claude-mythos-5-1")) {
            assertEquals(turn(mapOf("type" to "auto"), listOf("extract_metadata"), true), plan(model, "named", tools, "extract_metadata"))
            assertEquals(turn(mapOf("type" to "auto"), tools, true), plan(model, "any", tools))
            assertEquals(mapOf("type" to "auto"), plan(model, "free", tools)["tool_choice"])
            assertEquals(mapOf("type" to "none"), plan(model, "none", tools)["tool_choice"])
        }
        assertEquals(listOf("enrich"), plan("claude-opus-5", "named", tools, "enrich", true)["tools"])
        var refused = 0
        for (need in listOf("sometimes", "named")) {
            try {
                planTurn("claude-opus-5", need, tools)
            } catch (expected: IllegalArgumentException) {
                refused++
            }
        }
        assertEquals(2, refused)
    }

    @Test
    fun e4_aChangeOfToolChoiceCostsTheCachedMessagesAChangeOfToolsCostsEverythingAndARepeatCostsNothing() {
        val tools = listOf("extract_metadata", "enrich")
        val free = mapOf("tool_choice" to mapOf("type" to "auto"), "tools" to tools)
        assertEquals("none", cacheImpact(null, free))
        assertEquals("none", cacheImpact(free, mapOf("tool_choice" to mapOf("type" to "auto"), "tools" to listOf("extract_metadata", "enrich"))))
        val any = mapOf("tool_choice" to mapOf("type" to "any"), "tools" to tools)
        assertEquals("messages", cacheImpact(free, any))
        assertEquals("messages", cacheImpact(any, free))
        val a = mapOf("tool_choice" to mapOf("type" to "tool", "name" to "a"), "tools" to tools)
        val b = mapOf("tool_choice" to mapOf("type" to "tool", "name" to "b"), "tools" to tools)
        assertEquals("messages", cacheImpact(a, b))
        assertEquals("none", cacheImpact(a, a))
        assertEquals("all", cacheImpact(free, plan("claude-sonnet-5-5", "named", tools, "extract_metadata")))
        assertEquals("all", cacheImpact(free, mapOf("tool_choice" to mapOf("type" to "auto"), "tools" to listOf("enrich"))))
    }

    @Test
    fun e5_aReplyIsCheckedAgainstTheCallThatWasRequired() {
        val text = mapOf<String, Any?>("type" to "text", "text" to "I think so.")
        val useA = mapOf<String, Any?>("type" to "tool_use", "name" to "a")
        val useB = mapOf<String, Any?>("type" to "tool_use", "name" to "b")
        assertEquals("missed_call", checkTurn(listOf(text), "any"))
        assertEquals("ok", checkTurn(listOf(text, useB), "any"))
        assertEquals("missed_call", checkTurn(emptyList(), "named", "a"))
        assertEquals("wrong_tool", checkTurn(listOf(useB), "named", "a"))
        assertEquals("ok", checkTurn(listOf(useA, useB), "named", "a"))
        assertEquals("wrong_tool", checkTurn(listOf(useB, useA), "named", "a"))
        assertEquals("ok", checkTurn(listOf(text), "free"))
        assertEquals("ok", checkTurn(listOf(useA), "none"))
    }

    @Test
    fun e6_anUnknownToolAWrongOwnerOrABadAmountIsRefused() {
        val unknown = decide(call("tool" to "delete_account"))
        assertEquals(false, unknown["allowed"])
        assertEquals("unknown_tool", unknown["code"])
        assertEquals(false, unknown["escalate"])
        for (other in listOf<String?>("C-2", null)) {
            val answer = decide(call("customer" to other))
            assertEquals(false, answer["allowed"])
            assertEquals("not_owner", answer["code"])
            assertEquals(false, answer["escalate"])
        }
        assertEquals("not_owner", decide(call("verified_customer" to null))["code"])
        for (amount in listOf<Any?>(0, -5, 12.5, "50", true, null)) {
            val answer = decide(call("amount" to amount), setOf("c1"))
            assertEquals(false, answer["allowed"], amount.toString())
            assertEquals("bad_amount", answer["code"], amount.toString())
        }
        val free = decide(mapOf("id" to "c2", "tool" to "lookup_order", "customer" to "C-1", "verified_customer" to "C-1"))
        assertEquals(true, free["allowed"])
        assertEquals("ok", free["code"])
    }

    @Test
    fun e7_theCapIsInclusiveAndAnIrreversibleCallNeedsApprovalThatNeverLiftsTheCap() {
        assertEquals("ok", decide(call("amount" to 500), setOf("c1"))["code"])
        val waiting = decide(call("amount" to 50))
        assertEquals(false, waiting["allowed"])
        assertEquals("needs_approval", waiting["code"])
        assertEquals(true, waiting["escalate"])
        assertEquals(true, decide(call("amount" to 50), setOf("c1"))["allowed"])
        assertEquals("needs_approval", decide(call("amount" to 50), setOf("c9"))["code"])
        val over = decide(call("amount" to 501), setOf("c1"))
        assertEquals(false, over["allowed"])
        assertEquals("over_cap", over["code"])
        assertEquals(true, over["escalate"])
        assertTrue((over["message"] as String).contains("500"))
        assertEquals("not_owner", decide(call("amount" to 501, "customer" to "C-2"))["code"])
    }
}
