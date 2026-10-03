import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CoordinatorTest {
    private fun plan(vararg subtasks: Pair<String, String>, delegate: Boolean = true, answer: String? = null): Planner =
        { _ -> linkedMapOf("delegate" to delegate, "answer" to answer, "subtasks" to subtasks.map { linkedMapOf("scope" to it.first, "brief" to it.second) }) }

    /** Subagents: the report is looked up by the start of the brief; every brief received is kept. */
    private class Spokes : (String) -> String? {
        val reports = linkedMapOf<String, Any>()
        val briefs = mutableListOf<String>()

        fun on(prefix: String, report: Any): Spokes {
            reports[prefix] = report
            return this
        }

        override fun invoke(brief: String): String? {
            briefs.add(brief)
            for ((prefix, report) in reports) {
                if (brief.startsWith(prefix)) {
                    if (report is RuntimeException) throw report
                    return report as String
                }
            }
            fail<Unit>("no scripted report for $brief")
            return null
        }
    }

    private val noGaps: Reviewer = { _, _ -> emptyList() }
    private val echo: Synthesizer = { _, findings -> findings.joinToString(" | ") { it["text"] as String } }
    private val chips = "chips" to "chips: find 2024 chip supply news"
    private val cars = "cars" to "cars: find 2024 car output news"
    private val rates = "rates" to "rates: find 2024 interest rates"
    private fun reports() = Spokes().on("chips", "chips report").on("cars", "cars report").on("rates", "rates report")
    private fun finding(scope: String, text: String): Map<String, Any?> = linkedMapOf("scope" to scope, "text" to text)

    @Test
    fun m1_aHubSendsOneBriefToEachSpokeAndSynthesizesWhatComesBack() {
        val spokes = reports()
        val r = coordinate(plan(chips, cars, rates), spokes, noGaps, echo, "How did supply change?")
        assertNotNull(r, "coordinate returned null")
        assertEquals(listOf<Any?>("complete", "chips report | cars report | rates report", 3, 0), listOf(r!!["status"], r["answer"], r["subagent_calls"], r["rounds"]))
        assertEquals(listOf(chips.second, cars.second, rates.second), spokes.briefs)
        assertEquals(listOf(finding("chips", "chips report"), finding("cars", "cars report"), finding("rates", "rates report")), r["findings"])
        assertEquals(listOf<Any?>(emptyList<Any?>(), emptyList<Any?>(), emptyList<Any?>()), listOf(r["failed"], r["dropped"], r["gaps"]))
    }

    @Test
    fun e1_aQuestionTheCoordinatorCanAnswerItselfIsAnsweredWithoutAnySubagent() {
        val spokes = Spokes()
        val asked = mutableListOf<String>()
        val r = coordinate(plan(chips, cars, rates, delegate = false, answer = "Paris."), spokes, { q, _ -> asked.add(q); emptyList() }, { _, _ -> asked.add("synth"); "x" }, "Capital of France?")
        assertNotNull(r, "coordinate returned null")
        assertEquals(listOf<Any?>("direct", "Paris.", 0, emptyList<Any?>()), listOf(r!!["status"], r["answer"], r["subagent_calls"], r["findings"]))
        assertEquals(listOf(0, 0), listOf(spokes.briefs.size, asked.size))
    }

    @Test
    fun e2_aSubagentSeesItsOwnBriefAndNothingTheOthersFound() {
        val spokes = reports()
        coordinate(plan(chips, cars, rates), spokes, noGaps, echo, "How did supply change?")
        assertEquals(3, spokes.briefs.size)
        assertEquals(cars.second, spokes.briefs[1])
        assertEquals(rates.second, spokes.briefs[2])
        assertTrue(spokes.briefs.none { it.contains("report") })
    }

    @Test
    fun e3_thePlanIsCleanedOfEmptyBriefsAndDuplicateScopesAndCapped() {
        val spokes = Spokes().on("a", "a report").on("b", "b report").on("c", "c report")
        val messy = plan("A" to "a: first", "a " to "a: again", "B" to "   ", "b" to "b: second", "c" to "c: third", "d" to "d: fourth")
        val r = coordinate(messy, spokes, noGaps, echo, "q", 3, 2)
        assertNotNull(r, "coordinate returned null")
        assertEquals(listOf("a: first", "b: second", "c: third"), spokes.briefs)
        assertEquals(3, r!!["subagent_calls"])
        assertEquals(listOf(mapOf("scope" to "a ", "reason" to "duplicate scope"), mapOf("scope" to "B", "reason" to "empty brief"), mapOf("scope" to "d", "reason" to "over limit")), r["dropped"])
        val empty = coordinate(plan("x" to "", "y" to "  "), Spokes(), noGaps, echo, "q")
        assertNotNull(empty, "coordinate returned null")
        assertEquals(listOf<Any?>("failed", 0, null), listOf(empty!!["status"], empty["subagent_calls"], empty["answer"]))
    }

    @Test
    fun e4_aFailingSubagentDoesNotStopTheOthersAndARunWithNoFindingsDoesNotSynthesize() {
        val spokes = Spokes().on("chips", IllegalStateException("search offline")).on("cars", "cars report").on("rates", "   ")
        val r = coordinate(plan(chips, cars, rates), spokes, noGaps, echo, "q")
        assertNotNull(r, "coordinate returned null")
        assertEquals(listOf<Any?>("partial", "cars report", 3), listOf(r!!["status"], r["answer"], r["subagent_calls"]))
        assertEquals(listOf(mapOf("scope" to "chips", "error" to "search offline"), mapOf("scope" to "rates", "error" to "empty report")), r["failed"])
        val synthesized = mutableListOf<Any?>()
        val dead = coordinate(plan(chips, cars, rates), Spokes().on("chips", IllegalStateException("x")).on("cars", IllegalStateException("y")).on("rates", ""),
            noGaps, { _, f -> synthesized.add(f); "z" }, "q")
        assertNotNull(dead, "coordinate returned null")
        assertEquals(listOf<Any?>("failed", null, emptyList<Any?>(), 0), listOf(dead!!["status"], dead["answer"], dead["findings"], synthesized.size))
    }

    @Test
    fun e5_aReviewSendsOnlyTheGapsBackOutAndStopsWhenNoneAreLeft() {
        val spokes = Spokes().on("chips", "chips report").on("cars", "cars report").on("Follow", "follow-up report")
        val reviews = mutableListOf<List<Any?>>()
        val reviewer: Reviewer = { _, findings ->
            reviews.add(findings.map { it["scope"] })
            if (reviews.size == 1) listOf("2023 baseline", "  ", "2023 baseline") else emptyList()
        }
        val r = coordinate(plan(chips, cars), spokes, reviewer, echo, "How did supply change?")
        assertNotNull(r, "coordinate returned null")
        assertEquals(listOf(chips.second, cars.second, "Follow up: 2023 baseline\nQuestion: How did supply change?"), spokes.briefs)
        assertEquals(listOf(listOf("chips", "cars"), listOf("chips", "cars", "2023 baseline")), reviews)
        assertEquals(listOf<Any?>("complete", 1, 3, emptyList<Any?>()), listOf(r!!["status"], r["rounds"], r["subagent_calls"], r["gaps"]))
        assertEquals("chips report | cars report | follow-up report", r["answer"])
    }

    @Test
    fun e6_theRoundsAreCappedAndTheGapsThatRemainAreReported() {
        val spokes = Spokes().on("chips", "chips report").on("Follow", "more")
        val reviews = mutableListOf<Int>()
        val r = coordinate(plan(chips), spokes, { _, findings -> reviews.add(findings.size); listOf("still missing") }, echo, "q", 4, 2)
        assertNotNull(r, "coordinate returned null")
        assertEquals(listOf<Any?>("partial", 2, 3, listOf("still missing")), listOf(r!!["status"], r["rounds"], r["subagent_calls"], r["gaps"]))
        assertEquals(listOf(1, 2, 3), reviews)
    }
}
