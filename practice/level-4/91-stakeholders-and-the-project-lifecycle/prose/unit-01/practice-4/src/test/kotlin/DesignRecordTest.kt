import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class DesignRecordTest {
    // the folder that holds docs/design-record.md: the starter, the reference or a planted wrong solution
    private val root: Path = Path.of(System.getProperty("solution.dir", "starter"))

    private val sectionNames = listOf("Summary for the sponsor", "Decision statement", "Options considered", "Recommendation and trade-offs", "Accuracy by segment", "Service levels", "Pilot to scale", "Hand-off and monitoring")
    private val role = "billing operations manager"
    private val errorCost = 250
    private val reviewCost = 5
    private val maxLatencyMs = 2000
    private val minAvailability = 99.5
    private val badOwners = setOf("", "tbd", "everyone", "team", "n/a", "none")
    private val shapes = setOf("wrong amount", "outdated figure", "refusal", "made-up clause", "omitted exception")
    private val errorCosts = mapOf("credit" to 250, "complaint" to 60, "status" to 12)

    private fun record(): String {
        val path = root.resolve("docs/design-record.md")
        assertTrue(Files.isRegularFile(path), "docs/design-record.md is missing")
        return Files.readString(path)
    }

    /** title to body for every level 2 heading, in order. */
    private fun sections(): LinkedHashMap<String, String> {
        val text = record()
        val found = Regex("^## (.*)$", RegexOption.MULTILINE).findAll(text).toList()
        val out = LinkedHashMap<String, String>()
        for ((i, m) in found.withIndex()) {
            val end = if (i + 1 < found.size) found[i + 1].range.first else text.length
            out[m.groupValues[1].trim()] = text.substring(m.range.last + 1, end).trim()
        }
        return out
    }

    private fun body(title: String): String = sections()[title] ?: throw AssertionError("the section '$title' is missing")

    /** The rows of the first table in a section, without the header and the separator. */
    private fun table(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        for (line in text.split("\n")) {
            if (line.startsWith("|")) {
                val cells = line.trim().replace(Regex("^\\||\\|$"), "").split("|").map { it.trim() }
                if (!cells.all { it.matches(Regex("[-: ]*")) }) rows += cells
            }
        }
        return rows.drop(1)
    }

    private fun number(cell: String): Double {
        val found = Regex("\\d[\\d,]*\\.?\\d*").find(cell)
        assertNotNull(found, "no number in '$cell'")
        return found!!.value.replace(",", "").toDouble()
    }

    private fun words(text: String): Int = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.size

    private fun fmt(n: Double): String = String.format(java.util.Locale.US, "%,d", n.toLong())

    private fun recommended(): List<List<String>> = table(body("Options considered")).filter { it.size == 5 && it[3] == "recommended" }

    @Test
    fun m1_theEightSectionsArePresentInOrderAndFilled() {
        assertEquals(sectionNames, sections().keys.toList(), "the sections are the eight headings, in order")
        for ((name, text) in sections()) assertTrue(words(text) >= 15, "'$name' says too little")
        assertFalse(record().contains("TODO"), "replace every TODO")
    }

    @Test
    fun e1_theDecisionStatementCarriesTheVolumeTheLatencyTheErrorCostAndTheAccountableRole() {
        val text = body("Decision statement").lowercase()
        for (needed in listOf("3,000", "2 seconds", role)) assertTrue(text.contains(needed), "the decision statement does not say '$needed'")
        for (value in listOf(errorCost, reviewCost)) assertTrue(Regex("\\b$value\\b").containsMatchIn(text), "the decision statement does not give the cost $value")
    }

    @Test
    fun e2_theOptionsIncludeARecommendedOneThatIsTheCheapestToMeetTheServiceLevelsAndAReasonForEachRejection() {
        val rows = table(body("Options considered"))
        assertTrue(rows.size >= 4, "compare at least four options")
        for (row in rows) {
            assertTrue(row.size == 5 && row[3] in listOf("recommended", "alternative", "rejected"), "$row: status is recommended, alternative or rejected")
            if (row[3] == "rejected") assertTrue(words(row[4]) >= 5, "${row[0]}: give a reason for the rejection")
        }
        val chosen = rows.filter { it[3] == "recommended" }
        assertEquals(1, chosen.size, "exactly one option is recommended")
        assertEquals("yes", chosen[0][2].lowercase(), "the recommended option meets the service levels")
        val cheapest = rows.filter { it[2].equals("yes", ignoreCase = true) }.minOf { number(it[1]) }
        assertEquals(cheapest, number(chosen[0][1]), "the recommended option is the cheapest one that meets the service levels")
    }

    @Test
    fun e3_theBreakEvenAccuracyIs98PercentAndTheRecommendationStatesTheCost() {
        val text = body("Recommendation and trade-offs")
        val expected = 100 - Math.ceil(100.0 * reviewCost / errorCost).toInt()
        val found = Regex("at or above (\\d+) percent").find(text)
        assertTrue(found != null && found.groupValues[1].toInt() == expected, "state the rule 'at or above $expected percent'")
        val chosen = recommended()
        assertTrue(chosen.isNotEmpty(), "recommend one option")
        assertTrue(text.contains(fmt(number(chosen[0][1]))), "the recommendation states the monthly cost of the recommended option")
    }

    @Test
    fun e4_eachServiceLevelHasATargetWithinTheCaseLimitsAndANamedOwner() {
        val rows = table(body("Service levels")).filter { it.size == 4 }
        val latency = rows.firstOrNull { it[0].lowercase().contains("latency") }
        val availability = rows.firstOrNull { it[0].lowercase().contains("availability") }
        val accuracy = rows.firstOrNull { it[0].lowercase().contains("accuracy") }
        assertTrue(latency != null && availability != null && accuracy != null, "list latency, availability and accuracy")
        assertTrue(latency!![1].contains("ms") && number(latency[1]) <= maxLatencyMs, "the latency target is at most $maxLatencyMs ms")
        assertTrue(availability!![1].contains("percent") && number(availability[1]) >= minAvailability, "the availability target is at least $minAvailability percent")
        assertTrue(accuracy!![1].lowercase().contains("credit"), "the accuracy target is stated for the credit segment")
        for (row in listOf(latency, availability, accuracy)) assertTrue(row[2].isNotEmpty() && row[3].lowercase() !in badOwners, "${row[0]}: say how it is measured and who owns it")
    }

    @Test
    fun e5_segmentsAreListedByErrorCostAndHandledByTheBreakEven() {
        val rows = table(body("Accuracy by segment"))
        assertEquals(errorCosts.keys.sorted(), rows.map { it[0] }.sorted(), "list the credit, complaint and status segments")
        val costs = mutableListOf<Double>()
        for (row in rows) {
            assertEquals(6, row.size, "$row is missing a column")
            val segment = row[0]
            val accuracy = number(row[2])
            assertTrue(accuracy in 0.0..100.0 && row[3] in shapes, "$segment: give an accuracy and one of the failure shapes")
            assertEquals(errorCosts[segment]!!.toDouble(), number(row[4]), "$segment: the cost per error is ${errorCosts[segment]}")
            assertEquals(if (accuracy >= 98) "auto" else "reviewed", row[5], "$segment: handling follows the break-even accuracy of 98 percent")
            costs += number(row[4])
        }
        assertEquals(costs.sortedDescending(), costs, "list the costliest segment first")
    }

    @Test
    fun e6_thePilotToScaleTableHasFourAssumptionsEachWithATestAndAStopTriggerWithANumber() {
        val rows = table(body("Pilot to scale"))
        assertTrue(rows.size >= 4, "name at least four assumptions of the pilot")
        for (row in rows) {
            assertTrue(row.size == 3 && row.all { it.isNotEmpty() }, "$row: give the assumption, how to test it and what stops the roll-out")
            assertTrue(Regex("\\d").containsMatchIn(row[2]), "${row[0]}: the stop trigger needs a number")
        }
    }

    @Test
    fun e7_theHandOffNamesAnOwnerARunbookARollbackAndMonitors() {
        val text = body("Hand-off and monitoring").lowercase()
        for (needed in listOf("owner", "runbook", "rollback", "previous model")) assertTrue(text.contains(needed), "the hand-off does not mention '$needed'")
        assertTrue(listOf("refusals", "tokens per answer", "flagged", "latency").count { text.contains(it) } >= 2, "name at least two monitors")
    }

    @Test
    fun e8_theSponsorSummaryHasAtMost80WordsAndStatesTheCostAndTheDecisionAndNoFileHoldsPersonalData() {
        val summary = body("Summary for the sponsor")
        assertTrue(words(summary) <= 80, "the sponsor summary has at most 80 words")
        val chosen = recommended()
        assertTrue(chosen.isNotEmpty(), "recommend one option")
        assertTrue(summary.contains(fmt(number(chosen[0][1]))), "the summary states the monthly cost")
        assertTrue(summary.lowercase().contains("decision") && !summary.contains("`"), "the summary asks for a decision, in plain words")
        val hits = mutableListOf<String>()
        val home = Regex("(/home/\\w+|/Users/\\w+|C:\\\\Users)")
        val email = Regex("[\\w.+-]+@(?!example\\.(com|invalid))[\\w-]+\\.[\\w.]+")
        Files.walk(root).use { s ->
            for (p in s.filter { Files.isRegularFile(it) }.sorted().toList()) {
                val text = Files.readString(p)
                val rel = root.relativize(p).toString()
                if (home.containsMatchIn(text)) hits += "$rel: home path"
                if (email.containsMatchIn(text)) hits += "$rel: email address"
            }
        }
        assertEquals(listOf<String>(), hits)
    }
}
