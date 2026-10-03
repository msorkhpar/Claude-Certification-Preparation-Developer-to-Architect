import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DecomposeTest {
    private fun file(path: String, text: String): Map<String, String> = linkedMapOf("path" to path, "text" to text)
    private fun summary(path: String, text: String): Map<String, String> = linkedMapOf("path" to path, "summary" to text)
    private fun files() = listOf(file("api.py", "def get(): ..."), file("db.py", "def query(): ..."), file("ui.py", "def show(): ..."))
    private fun scripted(finding: String, summary: String): Map<String, Any?> = linkedMapOf("findings" to (if (finding.isEmpty()) emptyList() else listOf(finding)), "summary" to summary)

    /** A scripted file pass: findings and summary by the name before the dot; every call is kept. A script that is a RuntimeException is thrown. */
    private class Passes : (String, String, Int, Int) -> Map<String, Any?> {
        val scripts = linkedMapOf<String, Any>()
        val calls = mutableListOf<List<Any>>()

        fun on(name: String, script: Any): Passes {
            scripts[name] = script
            return this
        }

        @Suppress("UNCHECKED_CAST")
        override fun invoke(path: String, text: String, part: Int, parts: Int): Map<String, Any?> {
            calls.add(listOf(path, text, part, parts))
            val script = scripts[path.substringBefore(".")] ?: linkedMapOf("findings" to emptyList<String>(), "summary" to "$path part $part")
            if (script is RuntimeException) throw script
            return script as Map<String, Any?>
        }
    }

    private class Cross(val findings: List<String> = emptyList(), val error: RuntimeException? = null) : (List<Map<String, String>>) -> List<String> {
        val seen = mutableListOf<List<Map<String, String>>>()

        override fun invoke(summaries: List<Map<String, String>>): List<String> {
            seen.add(summaries)
            if (error != null) throw error
            return findings
        }
    }

    private fun review(files: List<Map<String, String>> = files(), pass: Passes = Passes(), cross: Cross = Cross(), maxLines: Int = 40): Map<String, Any?> {
        val result = reviewChanges(files, pass, cross, maxLines)
        assertNotNull(result, "reviewChanges returned null")
        return result!!
    }

    @Suppress("UNCHECKED_CAST")
    private fun sub(m: Map<String, Any?>, key: String): Map<String, Any?> = m[key] as Map<String, Any?>

    @Test
    fun m1_eachFileIsReviewedAloneAndTheCrossPassReadsTheirSummaries() {
        val passes = Passes().on("api", scripted("api: no auth", "api calls db.query(id)")).on("db", scripted("", "db.query takes a name")).on("ui", scripted("ui: unused", "ui shows rows"))
        val cross = Cross(listOf("api passes id but db expects a name"))
        val result = review(pass = passes, cross = cross)
        assertEquals(listOf("api.py", "db.py", "ui.py"), passes.calls.map { it[0] })
        assertEquals(linkedMapOf("findings" to listOf("api: no auth"), "summary" to "api calls db.query(id)", "parts" to 1), sub(sub(result, "files"), "api.py"))
        assertEquals(listOf("ui: unused"), sub(sub(result, "files"), "ui.py")["findings"])
        assertEquals(listOf("api passes id but db expects a name"), result["cross"])
        assertEquals(listOf(listOf(summary("api.py", "api calls db.query(id)"), summary("db.py", "db.query takes a name"), summary("ui.py", "ui shows rows"))), cross.seen)
        assertTrue(sub(result, "failed").isEmpty() && (result["skipped"] as List<*>).isEmpty() && result["cross_error"] == null)
    }

    @Test
    fun e1_aFilePassSeesOnlyItsOwnFileAndTheCrossPassSeesSummariesNeverTheText() {
        val passes = Passes()
        val cross = Cross()
        review(pass = passes, cross = cross)
        assertEquals(listOf(listOf("api.py", "def get(): ...", 1, 1), listOf("db.py", "def query(): ...", 1, 1), listOf("ui.py", "def show(): ...", 1, 1)), passes.calls)
        assertEquals(1, cross.seen.size)
        val seen = cross.seen[0]
        assertEquals(3, seen.size)
        for (item in seen) assertEquals(listOf("path", "summary"), item.keys.toList())
        assertFalse(seen.toString().contains("def "))
    }

    @Test
    fun e2_aLongFileIsReviewedInLabelledPartsAndABlankFileIsSkipped() {
        val longText = (1..7).joinToString("\n") { "line $it" }
        val passes = Passes()
        val result = review(listOf(file("big.py", longText), file("empty.py", "  \n \n"), file("small.py", "x = 1")), passes, maxLines = 3)
        assertEquals(listOf(listOf("big.py", "line 1\nline 2\nline 3", 1, 3), listOf("big.py", "line 4\nline 5\nline 6", 2, 3), listOf("big.py", "line 7", 3, 3), listOf("small.py", "x = 1", 1, 1)), passes.calls)
        assertEquals(3, sub(sub(result, "files"), "big.py")["parts"])
        assertEquals("big.py part 1 big.py part 2 big.py part 3", sub(sub(result, "files"), "big.py")["summary"])
        assertEquals(listOf("empty.py"), result["skipped"])
        assertFalse(sub(result, "files").containsKey("empty.py"))
        assertEquals(1, sub(sub(review(listOf(file("a.py", "1\n2\n3")), maxLines = 3), "files"), "a.py")["parts"])
    }

    @Test
    fun e3_aFailingFileIsReportedAndLeftOutOfTheCrossPassWhichNeedsTwoFiles() {
        val passes = Passes().on("db", RuntimeException("model timed out")).on("api", scripted("f1", "api summary")).on("ui", scripted("f2", "ui summary"))
        val cross = Cross(listOf("relation"))
        val result = review(pass = passes, cross = cross)
        assertEquals(linkedMapOf("db.py" to "model timed out"), result["failed"])
        assertEquals(listOf("api.py", "ui.py"), sub(result, "files").keys.toList())
        assertEquals(listOf(listOf(summary("api.py", "api summary"), summary("ui.py", "ui summary"))), cross.seen)
        val lone = Cross(listOf("never"))
        val one = review(pass = Passes().on("db", RuntimeException("boom")).on("ui", RuntimeException("boom")), cross = lone)
        assertEquals(listOf("api.py"), sub(one, "files").keys.toList())
        assertTrue(lone.seen.isEmpty() && (one["cross"] as List<*>).isEmpty())
        assertEquals(listOf("db.py", "ui.py"), sub(one, "failed").keys.toList())
        val broken = review(cross = Cross(error = RuntimeException("cross failed")))
        assertTrue((broken["cross"] as List<*>).isEmpty() && broken["cross_error"] == "cross failed" && sub(broken, "files").size == 3)
    }

    /** A scripted planner: the replies in order; every call is kept with the steps it was given. */
    private class Planner(vararg val replies: Any?) : (String, List<Map<String, String>>) -> Any? {
        val goals = mutableListOf<String>()
        val histories = mutableListOf<List<Map<String, String>>>()

        override fun invoke(goal: String, steps: List<Map<String, String>>): Any? {
            goals.add(goal)
            histories.add(steps)
            return replies[minOf(goals.size, replies.size) - 1]
        }
    }

    private fun next(subtask: String): Map<String, Any?> = linkedMapOf("done" to false, "next" to subtask)
    private fun finished(summary: String): Map<String, Any?> = linkedMapOf("done" to true, "summary" to summary)
    private fun step(subtask: String, result: String): Map<String, String> = linkedMapOf("subtask" to subtask, "result" to result)

    private fun adapt(planner: Planner, ran: MutableList<String>, maxSteps: Int = 6): Map<String, Any?> {
        val result = runAdaptive(planner, { subtask -> ran.add(subtask); "did $subtask" }, "map the module", maxSteps)
        assertNotNull(result, "runAdaptive returned null")
        return result!!
    }

    @Test
    fun e4_thePlannerIsAskedAgainAfterEachStepWithTheStepsSoFarAndTheLoopEndsWhenItSaysDone() {
        val planner = Planner(next("list the files"), next("read the entry point"), finished("two modules, one entry point"))
        val ran = mutableListOf<String>()
        val result = adapt(planner, ran)
        assertEquals(listOf("list the files", "read the entry point"), ran)
        assertEquals("done", result["status"])
        assertEquals("two modules, one entry point", result["summary"])
        assertEquals(listOf(step("list the files", "did list the files"), step("read the entry point", "did read the entry point")), result["steps"])
        assertEquals(listOf(0, 1, 2), planner.histories.map { it.size })
        assertEquals("map the module", planner.goals[0])
        assertEquals(step("read the entry point", "did read the entry point"), planner.histories[2][1])
        val failing = Planner(next("open the file"), finished("saw the error"))
        val failed = runAdaptive(failing, { throw IllegalStateException("no such file") }, "map the module")
        assertNotNull(failed, "runAdaptive returned null")
        assertEquals(listOf(step("open the file", "ERROR: no such file")), failed!!["steps"])
        assertEquals(2, failing.histories.size)
        assertEquals("ERROR: no such file", failing.histories[1][0]["result"])
    }

    @Test
    fun e5_theLoopStopsOnARepeatedSubtaskOrNoNextStepOrAnUnreadableReplyAndCountsTheStepLimitExactly() {
        val ran = mutableListOf<String>()
        val repeated = adapt(Planner(next("list the files"), next("  List The Files ")), ran)
        assertEquals("stuck", repeated["status"])
        assertEquals(listOf("list the files"), ran)
        assertTrue(repeated["reason"].toString().lowercase().contains("list the files"))
        val blank = adapt(Planner(next("   ")), mutableListOf())
        assertEquals("stuck", blank["status"])
        assertTrue((blank["steps"] as List<*>).isEmpty())
        for (reply in listOf<Any?>("not a plan", linkedMapOf("next" to "x"), linkedMapOf("done" to "yes"), null)) {
            val none = mutableListOf<String>()
            val bad = adapt(Planner(reply), none)
            assertEquals("bad_plan", bad["status"], "$reply must be a bad plan")
            assertTrue(none.isEmpty())
        }
        val planner = Planner(*(1..9).map { next("step $it") }.toTypedArray())
        val limitedRan = mutableListOf<String>()
        val limited = adapt(planner, limitedRan, 3)
        assertEquals("step_limit", limited["status"])
        assertEquals(listOf("step 1", "step 2", "step 3"), limitedRan)
        assertEquals(4, planner.goals.size)
        val lastRan = mutableListOf<String>()
        val last = adapt(Planner(next("one"), next("two"), finished("finished on the last step")), lastRan, 2)
        assertEquals("done", last["status"])
        assertEquals(listOf("one", "two"), lastRan)
    }

    private fun task(stepsKnown: Any?, items: Any?, interact: Any? = null): Map<String, Any?> {
        val m = linkedMapOf<String, Any?>()
        if (stepsKnown != null) m["steps_known"] = stepsKnown
        if (items != null) m["items"] = items
        if (interact != null) m["items_interact"] = interact
        return m
    }

    @Test
    fun e6_theStrategyFollowsWhatIsKnownAboutTheStepsAndWhetherTheItemsInteract() {
        assertEquals("fixed_chain", chooseStrategy(task(true, 1, false)))
        assertEquals("fixed_chain", chooseStrategy(task(true, 12, false)))
        assertEquals("per_item_then_cross", chooseStrategy(task(true, 12, true)))
        assertEquals("fixed_chain", chooseStrategy(task(true, 1, true)))
        assertEquals("adaptive", chooseStrategy(task(false, 12, true)))
        assertEquals("adaptive", chooseStrategy(task(false, 0)))
        assertEquals("fixed_chain", chooseStrategy(task(true, 3)))
        var bad = 0
        for (t in listOf(task(null, 3), task(true, null), task(true, -1), task("yes", 2), task(true, 2.5))) {
            try {
                chooseStrategy(t)
            } catch (expected: IllegalArgumentException) {
                bad++
            }
        }
        assertEquals(5, bad)
    }
}
