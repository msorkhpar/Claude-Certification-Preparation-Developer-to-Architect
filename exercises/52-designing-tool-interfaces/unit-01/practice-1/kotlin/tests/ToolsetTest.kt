import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ToolsetTest {
    private val orderText = "Looks up one order by its id and returns its status, items and total in cents. Use when the customer gives an order id such as A-1042 " +
        "or asks where an order is. Do not use it to find a customer by name; use get_customer instead of this tool for that. It returns no payment details."
    private val customerText = "Finds one customer by email address and returns the customer id, name and plan. Use when the person gives an email or asks about their account. " +
        "Do not use it for orders; call lookup_order instead of this tool for those. It returns no payment details."

    private fun prop(type: String, description: String, enum: List<String>? = null): Map<String, Any?> =
        linkedMapOf<String, Any?>("type" to type, "description" to description).also { if (enum != null) it["enum"] = enum }

    private fun orderProps(): Map<String, Any?> = linkedMapOf("order_id" to prop("string", "The order id, for example A-1042."))

    private fun tool(name: String = "lookup_order", description: String = orderText, properties: Map<String, Any?> = orderProps(), required: List<String> = listOf("order_id"), more: List<Pair<String, Any?>> = emptyList()): Map<String, Any?> =
        linkedMapOf<String, Any?>("name" to name, "description" to description, "input_schema" to linkedMapOf("type" to "object", "properties" to properties, "required" to required)).also { m -> more.forEach { m[it.first] = it.second } }

    private fun rules(tool: Map<String, Any?>): List<String> {
        val found = lintTool(tool)
        assertNotNull(found, "lintTool returned null")
        return found!!
    }

    private fun pairs(tools: List<Map<String, Any?>>, maxTools: Int? = null): List<List<String>> {
        val found = if (maxTools == null) lintToolSet(tools) else lintToolSet(tools, maxTools)
        assertNotNull(found, "lintToolSet returned null")
        return found!!
    }

    @Test
    fun m1_aWellMadeToolLintsCleanAndAPoorOneIsNamedForEveryRuleItBreaks() {
        assertEquals(emptyList<String>(), rules(tool(more = listOf("input_examples" to listOf(mapOf("order_id" to "A-1042")), "annotations" to mapOf("readOnlyHint" to true)))))
        val poor = tool(name = "helper", description = "Gets stuff.", properties = mapOf("q" to mapOf("type" to "string")), required = listOf("q"))
        assertEquals(listOf("no-boundary", "no-use-when", "param-undescribed", "short-description", "vague-name"), rules(poor))
    }

    @Test
    fun e1_namesMustMatchThePatternAndAVagueNameIsFlagged() {
        for (bad in listOf("get order", "get.order", "a".repeat(129), "order#1")) assertEquals(listOf("bad-name"), rules(tool(name = bad)), bad)
        for (good in listOf("a".repeat(128), "look-up-order", "run_report")) assertEquals(emptyList<String>(), rules(tool(name = good)), good)
        for (vague in listOf("run", "Run", "helper", "query")) assertEquals(listOf("vague-name"), rules(tool(name = vague)), vague)
    }

    @Test
    fun e2_aDescriptionNeedsThreeSentencesAWhenToUsePhraseAndABoundaryAgainstTheNeighbour() {
        assertEquals(listOf("short-description"), rules(tool(description = "Looks up one order by id. Use when the customer gives an order id, not for customers.")))
        assertEquals(listOf("no-use-when"), rules(tool(description = "Looks up one order by id. It returns the status and total. Do not use it for customers.")))
        assertEquals(listOf("no-boundary"), rules(tool(description = "Looks up one order by id. Use when the customer gives an order id. It returns the status and total.")))
        assertEquals(emptyList<String>(), rules(tool(description = "Looks up one order by id. USE WHEN the customer gives an id! Prefer it instead of get_customer for orders.")))
        assertEquals(listOf("no-boundary", "no-use-when", "short-description"), rules(tool(description = "")))
    }

    @Test
    fun e3_parametersAreDescribedAndRequiredNamesExistAndClosedSetsAreEnumsAndExamplesFitTheSchema() {
        assertEquals(listOf("param-undescribed"), rules(tool(properties = mapOf("order_id" to prop("string", "  ")))))
        assertEquals(listOf("required-unknown"), rules(tool(required = listOf("order_id", "missing"))))
        val orderId = prop("string", "The order id.")
        val status = prop("string", "One of open, shipped or closed.")
        assertEquals(listOf("open-set"), rules(tool(properties = linkedMapOf("order_id" to orderId, "status" to status))))
        assertEquals(listOf("open-set"), rules(tool(properties = linkedMapOf("order_id" to orderId, "status" to prop("string", "Either open or closed.")))))
        assertEquals(emptyList<String>(), rules(tool(properties = linkedMapOf("order_id" to orderId, "status" to prop("string", "One of open, shipped or closed.", listOf("open", "shipped", "closed"))))))
        assertEquals(listOf("reasoning-param"), rules(tool(properties = linkedMapOf("order_id" to orderId, "reasoning" to prop("string", "Why.")))))
        assertEquals(listOf("reasoning-param"), rules(tool(properties = linkedMapOf("order_id" to orderId, "note" to prop("string", "Your step by step thinking.")))))
        assertEquals(emptyList<String>(), rules(tool(properties = linkedMapOf("order_id" to orderId, "note" to prop("string", "A short explanation of why the call is made.")))))
        val props = linkedMapOf("order_id" to orderId, "status" to prop("string", "The status.", listOf("open", "closed")), "limit" to prop("integer", "Page size."))
        assertEquals(emptyList<String>(), rules(tool(properties = props, more = listOf("input_examples" to listOf(mapOf("order_id" to "A", "status" to "open", "limit" to 5), mapOf("order_id" to "B"))))))
        val bad: List<Map<String, Any>> = listOf(mapOf("order_id" to 5), emptyMap<String, Any>(), mapOf("order_id" to "A", "extra" to 1), mapOf("order_id" to "A", "status" to "lost"), mapOf("order_id" to "A", "limit" to true), mapOf("order_id" to "A", "limit" to "5"))
        for (example in bad) assertEquals(listOf("bad-example"), rules(tool(properties = props, more = listOf("input_examples" to listOf(example)))), example.toString())
    }

    @Test
    fun e4_aListToolNeedsALimitAndACursorAndAHintMayNotContradictTheName() {
        val query = prop("string", "Search text.")
        val limit = prop("integer", "Page size.")
        val cursor = prop("string", "Opaque cursor from the last page.")
        assertEquals(listOf("list-unbounded"), rules(tool(name = "search_orders", properties = mapOf("query" to query), required = emptyList())))
        assertEquals(listOf("list-unbounded"), rules(tool(name = "search_orders", properties = mapOf("query" to query, "limit" to limit), required = emptyList())))
        assertEquals(listOf("list-unbounded"), rules(tool(name = "list_orders", properties = emptyMap(), required = emptyList())))
        assertEquals(emptyList<String>(), rules(tool(name = "find_customer", properties = mapOf("query" to query, "limit" to limit, "cursor" to cursor), required = emptyList())))
        assertEquals(emptyList<String>(), rules(tool(name = "get_order", properties = mapOf("query" to query), required = emptyList())))
        assertEquals(listOf("hint-contradicts-name"), rules(tool(name = "delete_order", more = listOf("annotations" to mapOf("readOnlyHint" to true)))))
        assertEquals(listOf("hint-contradicts-name"), rules(tool(name = "delete_order", more = listOf("annotations" to mapOf("destructiveHint" to false)))))
        assertEquals(listOf("hint-contradicts-name"), rules(tool(name = "remove_item", more = listOf("annotations" to mapOf("destructiveHint" to false)))))
        assertEquals(listOf("hint-contradicts-name"), rules(tool(name = "send_receipt", more = listOf("annotations" to mapOf("readOnlyHint" to true)))))
        for (ok in listOf(tool(name = "delete_order", more = listOf("annotations" to mapOf("destructiveHint" to true))), tool(name = "get_order", more = listOf("annotations" to mapOf("readOnlyHint" to true))), tool(name = "create_order", more = listOf("annotations" to mapOf("readOnlyHint" to false))))) {
            assertEquals(emptyList<String>(), rules(ok))
        }
    }

    private fun overlap(a: String, b: String): List<List<String>> = pairs(listOf(tool(name = "a_tool", description = a), tool(name = "b_tool", description = b))).filter { it[1].startsWith("overlap:") }

    @Test
    fun e5_aSetIsGradedForDuplicateNamesOverlappingDescriptionsAndSize() {
        assertEquals(listOf(listOf("get_order_status", "overlap:lookup_order"), listOf("lookup_order", "overlap:get_order_status")), pairs(listOf(tool(), tool(name = "get_order_status"))))
        val customer = tool(name = "get_customer", description = customerText)
        assertEquals(emptyList<List<String>>(), pairs(listOf(tool(), customer)))
        assertEquals(listOf(listOf("lookup_order", "duplicate-name")), pairs(listOf(tool(), tool(description = customerText))))
        assertEquals(listOf(listOf("a_tool", "overlap:b_tool"), listOf("b_tool", "overlap:a_tool")), overlap("alpha beta gamma delta", "alpha beta gamma epsilon"))
        assertEquals(emptyList<List<String>>(), overlap("alpha beta gamma delta epsilon", "alpha beta gamma zeta eta"))
        assertEquals(listOf(listOf("*", "too-many-tools")), pairs(listOf(tool(), customer), 1))
        assertEquals(emptyList<List<String>>(), pairs(listOf(tool(), customer), 2))
    }

    @Suppress("UNCHECKED_CAST")
    private fun itemsOf(page: Map<String, Any?>?): List<String> = page!!["items"] as List<String>

    @Test
    fun e6_pagesCarryAnOpaqueCursorAndAClampedLimitAndANote() {
        val rows = (0 until 25).map { "row-%02d".format(it) }
        val first = pageResults(rows)
        assertNotNull(first)
        assertEquals(rows.subList(0, 10), itemsOf(first))
        assertEquals(false, first!!["truncated"])
        assertEquals("Showing 10 of 25 results; pass next_cursor to continue, or narrow the query with a filter.", first["note"])
        val cursor = first["next_cursor"] as String?
        assertNotNull(cursor)
        assertFalse(cursor!!.contains("10") || cursor.contains("offset"))
        val second = pageResults(rows, cursor)!!
        assertEquals(rows.subList(10, 20), itemsOf(second))
        assertNotNull(second["next_cursor"])
        val last = pageResults(rows, second["next_cursor"] as String)!!
        assertEquals(rows.subList(20, 25), itemsOf(last))
        assertNull(last["next_cursor"])
        assertNull(last["note"])
        assertEquals(50, itemsOf(pageResults((0 until 120).map { "r%03d".format(it) }, null, 500)).size)
        assertEquals(rows.subList(0, 3), itemsOf(pageResults(rows, null, 3)))
        for (bad in listOf(0, -1)) assertThrows(IllegalArgumentException::class.java, { pageResults(rows, null, bad) }, "limit $bad was accepted")
        for (bad in listOf("not a cursor!", "")) assertThrows(IllegalArgumentException::class.java, { pageResults(rows, bad) }, "cursor $bad was accepted")
    }

    @Test
    fun e7_aPageStopsAtTheSizeCapAndSaysSoButAlwaysCarriesOneItem() {
        val ten = List(5) { "0123456789" }
        val cut = pageResults(ten, null, 10, 25)
        assertNotNull(cut)
        assertEquals(ten.subList(0, 2), itemsOf(cut))
        assertEquals(true, cut!!["truncated"])
        assertNotNull(cut["next_cursor"])
        assertTrue(cut["note"].toString().startsWith("Showing 2 of 5 results"))
        assertEquals(2, itemsOf(pageResults(ten, null, 10, 20)).size)
        val big = pageResults(listOf("x".repeat(100), "y"), null, 10, 10)
        assertEquals(listOf("x".repeat(100)), itemsOf(big))
        assertEquals(true, big!!["truncated"])
        val whole = pageResults(ten, null, 10, 1000)!!
        assertEquals(ten, itemsOf(whole))
        assertEquals(false, whole["truncated"])
        assertNull(whole["next_cursor"])
    }

    @Test
    fun e8_aToolsOwnHintsAreTrustedOnlyFromATrustedServerAndTheDefaultsApplyOtherwise() {
        val defaults = mapOf("readOnlyHint" to false, "destructiveHint" to true, "idempotentHint" to false, "openWorldHint" to true)
        val reader = mapOf("name" to "get_order", "server" to "orders", "annotations" to mapOf("readOnlyHint" to true, "idempotentHint" to true))
        assertEquals(mapOf("readOnlyHint" to true, "destructiveHint" to true, "idempotentHint" to true, "openWorldHint" to true), effectiveHints(reader, true))
        assertEquals(defaults, effectiveHints(reader, false))
        assertEquals(defaults, effectiveHints(mapOf("name" to "x", "annotations" to mapOf("readOnlyHint" to "yes")), true))
        assertEquals(defaults, effectiveHints(mapOf("name" to "x"), true))
        val writer = mapOf("name" to "update_order", "server" to "orders", "annotations" to mapOf("readOnlyHint" to false))
        val stranger = mapOf("name" to "get_notes", "server" to "unknown", "annotations" to mapOf("readOnlyHint" to true))
        assertEquals(listOf("get_order"), parallelSafe(listOf(reader, writer, stranger), setOf("orders")))
        assertEquals(listOf("get_order", "get_notes"), parallelSafe(listOf(reader, writer, stranger), setOf("orders", "unknown")))
        assertEquals(emptyList<String>(), parallelSafe(listOf(reader), emptySet()))
    }
}
