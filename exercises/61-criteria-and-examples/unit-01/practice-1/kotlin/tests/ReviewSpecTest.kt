import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ReviewSpecTest {
    private fun crit(vararg over: Pair<String, Any?>): Map<String, Any?> = linkedMapOf<String, Any?>(
        "id" to "bug", "report" to "A comment whose claimed behaviour contradicts what the code does.", "skip" to "Minor style, naming and patterns the codebase already uses.",
        "severity" to mapOf("high" to "A null dereference on a request path, such as user.profile.name when user may be None.", "low" to "A misleading variable name."),
    ).also { it.putAll(over) }

    private val report: Map<String, Any?> = mapOf("verdict" to "report", "category" to "bug", "code" to "total = price * qty  # sum of the line items",
        "reason" to "The comment says the line sums items, the code multiplies one price by a quantity.")
    private val skip: Map<String, Any?> = mapOf("verdict" to "skip", "code" to "for i in range(n):  # loop", "reason" to "The comment is terse and accurate; at most a style matter.")

    private fun with(base: Map<String, Any?>, vararg over: Pair<String, Any?>): Map<String, Any?> = LinkedHashMap(base).also { it.putAll(over) }

    private fun spec(criteria: List<Map<String, Any?>>, examples: List<Map<String, Any?>>): Map<String, Any?> = mapOf("criteria" to criteria, "examples" to examples)

    private fun refused(spec: Map<String, Any?>): String {
        try {
            buildReviewPrompt(spec, "+ x = 1")
        } catch (error: IllegalArgumentException) {
            return error.message ?: ""
        }
        return fail("the specification was accepted: $spec")
    }

    private fun built(spec: Map<String, Any?>): String {
        val result = buildReviewPrompt(spec, "+ x = 1")
        assertNotNull(result, "buildReviewPrompt returned nothing")
        return result!!
    }

    @Test
    fun m1_thePromptPutsCriteriaFirstThenExamplesThenTheDiffLast() {
        val expected = listOf("<criteria>", "<criterion id=\"bug\">", "Report: A comment whose claimed behaviour contradicts what the code does.",
            "Skip: Minor style, naming and patterns the codebase already uses.", "Severity high: A null dereference on a request path, such as user.profile.name when user may be None.",
            "Severity low: A misleading variable name.", "</criterion>", "</criteria>", "<examples>", "<example verdict=\"report\" category=\"bug\">",
            "<code>total = price * qty  # sum of the line items</code>", "<reason>The comment says the line sums items, the code multiplies one price by a quantity.</reason>", "</example>",
            "<example verdict=\"skip\">", "<code>for i in range(n):  # loop</code>", "<reason>The comment is terse and accurate; at most a style matter.</reason>", "</example>",
            "</examples>", "<diff>", "+ x = 1", "</diff>").joinToString("\n")
        assertEquals(expected, built(spec(listOf(crit()), listOf(report, skip))))
    }

    @Test
    fun e1_vagueCriteriaAreRefusedInBothTheReportAndTheSkipText() {
        for (phrase in listOf("Be conservative and flag only what matters.", "Only report high-confidence findings.", "Report it when you are sure.", "Flag only important problems.", "Use your judgment about what matters.")) {
            for (key in listOf("report", "skip")) {
                val message = refused(spec(listOf(crit(key to phrase)), listOf(report, skip)))
                assertTrue(message.contains("vague"), "$key: '$phrase' was refused for another reason: $message")
            }
        }
    }

    @Test
    fun e2_aCriterionNeedsReportSkipAndAConcreteSeverityExampleForHighAndLow() {
        refused(spec(emptyList(), listOf(report, skip)))
        val bad = listOf(crit("report" to ""), crit("skip" to "  "), crit("skip" to null), crit("severity" to mapOf("high" to "A null dereference.")), crit("severity" to mapOf("low" to "A misleading name.")),
            crit("severity" to mapOf("high" to "x", "low" to " ")), crit("severity" to null))
        for (c in bad) refused(spec(listOf(c), listOf(report, skip)))
        assertTrue(built(spec(listOf(crit()), listOf(report, skip))).contains("<criterion id=\"bug\">"))
    }

    @Test
    fun e3_twoToFourExamplesWithAReportAndASkipEachCarryingAReason() {
        val criteria = listOf(crit())
        refused(spec(criteria, listOf(report)))
        refused(spec(criteria, listOf(report, skip, report, skip, report)))
        refused(spec(criteria, listOf(report, with(report))))
        refused(spec(criteria, listOf(skip, with(skip))))
        refused(spec(criteria, listOf(report, with(skip, "verdict" to "maybe"))))
        refused(spec(criteria, listOf(report, with(skip, "reason" to " "))))
        refused(spec(criteria, listOf(with(report, "category" to "performance"), skip)))
        val all = listOf(report, skip, report, skip)
        for (count in 2..4) assertEquals(count, built(spec(criteria, all.take(count))).split("<example ").size - 1)
    }

    private fun findings(category: String, accepted: Int, dismissed: Int, pattern: String = "p"): List<Map<String, Any?>> =
        List(accepted) { mapOf("category" to category, "verdict" to "accepted", "detected_pattern" to pattern) } + List(dismissed) { mapOf("category" to category, "verdict" to "dismissed", "detected_pattern" to pattern) }

    private fun reportOf(data: List<Map<String, Any?>>, minReviewed: Int = 5, minPrecision: Double = 0.5): Map<String, Any?> {
        val result = categoryReport(data, minReviewed, minPrecision)
        assertNotNull(result, "categoryReport returned nothing")
        return result!!
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun e4_aCategoryWithEnoughReviewsAndLowPrecisionIsDisabled() {
        val data = findings("bug", 8, 2) + findings("style", 2, 4) + findings("naming", 0, 4) + findings("docs", 3, 3)
        val report = reportOf(data)
        assertEquals(listOf("style"), report["disable"])
        val brief = (report["categories"] as Map<String, Map<String, Any?>>).mapValues { listOf(it.value["reviewed"], it.value["precision"], it.value["disable"]) }
        assertEquals(mapOf("bug" to listOf(10, 0.8, false), "style" to listOf(6, 0.33, true), "naming" to listOf(4, 0.0, false), "docs" to listOf(6, 0.5, false)), brief)
        assertEquals(listOf("naming", "style"), reportOf(data, 4)["disable"])
        assertEquals(listOf("bug", "docs", "style"), reportOf(data, 5, 0.9)["disable"])
        assertEquals(mapOf("categories" to emptyMap<String, Any?>(), "disable" to emptyList<String>()), reportOf(emptyList()))
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun e5_theMostDismissedPatternsAreListedByCountThenNameAndCappedAtThree() {
        val data = findings("style", 0, 3, "line-length") + findings("style", 0, 2, "import-order") + findings("style", 0, 1, "quote-style") + findings("style", 0, 1, "brace-style") + findings("style", 5, 0, "accepted-only")
        val style = (reportOf(data)["categories"] as Map<String, Map<String, Any?>>)["style"]!!
        assertEquals(listOf(listOf("line-length", 3), listOf("import-order", 2), listOf("brace-style", 1)), style["top_dismissed"])
        val bug = (reportOf(findings("bug", 3, 0))["categories"] as Map<String, Map<String, Any?>>)["bug"]!!
        assertEquals(emptyList<Any?>(), bug["top_dismissed"])
    }

    private val required = listOf("repo", "branch", "reviewer")

    private fun step(request: Map<String, Any?>, defaults: Map<String, String>, attended: Boolean): Map<String, Any?> {
        val result = nextStep(request, required, defaults, attended)
        assertNotNull(result, "nextStep returned nothing")
        return result!!
    }

    @Test
    fun e6_anAttendedRunAsksOnlyWhatItCannotAssumeAndStatesItsAssumptions() {
        val request = mapOf("repo" to "api", "branch" to "", "reviewer" to null)
        assertEquals(mapOf("action" to "ask", "ask" to listOf("reviewer"), "assumptions" to mapOf("branch" to "main")), step(request, mapOf("branch" to "main"), true))
        assertEquals(mapOf("action" to "proceed", "ask" to emptyList<String>(), "assumptions" to mapOf("branch" to "main")), step(mapOf("repo" to "api", "branch" to " ", "reviewer" to "ana"), mapOf("branch" to "main"), true))
        assertEquals(mapOf("action" to "proceed", "ask" to emptyList<String>(), "assumptions" to emptyMap<String, Any?>()), step(mapOf("repo" to "api", "branch" to "dev", "reviewer" to "ana"), mapOf("branch" to "main"), true))
        assertEquals(mapOf("action" to "ask", "ask" to listOf("repo", "branch", "reviewer"), "assumptions" to emptyMap<String, Any?>()), step(emptyMap(), emptyMap(), true))
    }

    @Test
    fun e7_anUnattendedRunNeverAsksItStatesAssumptionsOrStops() {
        val request = mapOf("repo" to "api", "branch" to "", "reviewer" to null)
        assertEquals(mapOf("action" to "stop", "ask" to emptyList<String>(), "assumptions" to mapOf("branch" to "main")), step(request, mapOf("branch" to "main"), false))
        assertEquals(mapOf("action" to "proceed", "ask" to emptyList<String>(), "assumptions" to mapOf("branch" to "main")), step(mapOf("repo" to "api", "branch" to "", "reviewer" to "ana"), mapOf("branch" to "main"), false))
        assertEquals(mapOf("action" to "proceed", "ask" to emptyList<String>(), "assumptions" to emptyMap<String, Any?>()), step(mapOf("repo" to "api", "branch" to "dev", "reviewer" to "ana"), emptyMap(), false))
    }
}
