import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class WorkflowsTest {
    private val plan3 = "[\"research the topic\", \"draft the outline\", \"check the facts\"]"

    /** A stand-in for the model: the first handler whose prefix starts the prompt answers; every prompt is kept. */
    private class Model : (String) -> String {
        val byPrefix = linkedMapOf<String, (String) -> String>()
        val prompts = mutableListOf<String>()

        fun on(prefix: String, answer: String): Model {
            byPrefix[prefix] = { answer }
            return this
        }

        fun on(prefix: String, handler: (String) -> String): Model {
            byPrefix[prefix] = handler
            return this
        }

        override fun invoke(prompt: String): String {
            prompts += prompt
            for ((prefix, handler) in byPrefix) if (prompt.startsWith(prefix)) return handler(prompt)
            fail<Unit>("no scripted answer for $prompt")
            return ""
        }
    }

    private fun sequence(vararg items: String): (String) -> String {
        val queue = items.toMutableList()
        return { queue.removeAt(0) }
    }

    private fun judged(vararg replies: String) = Model().on("Judge", sequence(*replies))
    private fun starting(m: Model, prefix: String) = m.prompts.filter { it.startsWith(prefix) }
    private fun n(o: Any?): Any? = if (o is Number) o.toDouble() else o

    @Suppress("UNCHECKED_CAST")
    private fun list(m: Map<String, Any?>, key: String): List<Map<String, Any?>> = (m[key] as List<Map<String, Any?>>?) ?: emptyList()

    @Test
    fun m1_anOrchestratorPlansRunsAWorkerPerSubtaskAndCombines() {
        val m = Model().on("Plan", plan3).on("Subtask") { p -> "done: " + p.split("\n")[0].removePrefix("Subtask: ") }.on("Combine", "FINAL")
        val result = orchestrate(m, "Write a guide") ?: emptyMap()
        assertEquals(listOf<Any?>("done", false, "FINAL", 5), listOf(result["status"], result["fallback"], result["answer"], result["calls"]))
        assertEquals(listOf("research the topic", "draft the outline", "check the facts"), result["plan"])
        assertEquals(listOf(listOf<Any?>("research the topic", "ok", "done: research the topic"), listOf("draft the outline", "ok", "done: draft the outline"), listOf("check the facts", "ok", "done: check the facts")),
            list(result, "results").map { listOf(it["subtask"], it["status"], it["output"]) })
        assertEquals(listOf("Plan: split the task into at most 5 independent subtasks. Reply with a JSON array of strings only.\nTask: Write a guide"), m.prompts.take(1))
        assertEquals(listOf("Subtask: research the topic\nTask: Write a guide"), starting(m, "Subtask").take(1))
        assertEquals(3, starting(m, "Subtask").size)
        assertEquals(listOf("Combine: write one answer to the task from the results.\nTask: Write a guide\n1. research the topic -> done: research the topic\n" +
            "2. draft the outline -> done: draft the outline\n3. check the facts -> done: check the facts"), starting(m, "Combine"))
    }

    @Test
    fun e1_thePlanIsReadFromProseCleanedCappedAndReplacedWhenUnusable() {
        val reply = "Here is the plan:\n```json\n[\"a\", \"b\", \"a\", \"  c  \", \"\", 5, \"d\", \"e\"]\n```\nGood luck."
        fun planner(r: String) = Model().on("Plan", r).on("Subtask", "x").on("Combine", "F")
        val capped = orchestrate(planner(reply), "T", 3) ?: emptyMap()
        assertEquals(listOf("a", "b", "c"), capped["plan"])
        assertEquals(5, capped["calls"])
        assertEquals(listOf("a", "b", "c", "d", "e"), (orchestrate(planner(reply), "T") ?: emptyMap())["plan"])
        val seen = planner(reply)
        orchestrate(seen, "T", 2)
        assertTrue(seen.prompts.firstOrNull()?.startsWith("Plan: split the task into at most 2 independent subtasks.") == true)
        for (bad in listOf("I cannot plan this.", "[]", "[\"\", 7]", "[not json]")) {
            val m = planner(bad)
            val result = orchestrate(m, "T") ?: emptyMap()
            assertEquals(listOf<Any?>(listOf("T"), true, 3), listOf(result["plan"], result["fallback"], result["calls"]))
            assertEquals(listOf("Subtask: T\nTask: T"), starting(m, "Subtask"))
        }
    }

    @Test
    fun e2_oneFailingWorkerDoesNotStopTheOthersOrTheAnswer() {
        val worker: (String) -> String = { prompt ->
            if (prompt.startsWith("Subtask: b")) throw IllegalStateException("disk full")
            if (prompt.startsWith("Subtask: c")) "" else "ok " + prompt[9]
        }
        val m = Model().on("Plan", "[\"a\", \"b\", \"c\"]").on("Subtask", worker).on("Combine", "FINAL")
        val result = orchestrate(m, "T") ?: emptyMap()
        assertEquals(listOf<Any?>("partial", "FINAL", 5), listOf(result["status"], result["answer"], result["calls"]))
        assertEquals(listOf(listOf("a", "ok", null), listOf("b", "failed", "disk full"), listOf("c", "failed", "empty reply")),
            list(result, "results").map { listOf(it["subtask"], it["status"], it["error"]) })
        assertEquals(listOf("Combine: write one answer to the task from the results.\nTask: T\n1. a -> ok a\n2. b -> FAILED\n3. c -> FAILED"), starting(m, "Combine"))
        val nothing = Model().on("Plan", "[\"a\", \"b\"]").on("Subtask", "  ").on("Combine", "never")
        val failed = orchestrate(nothing, "T") ?: emptyMap()
        assertEquals(listOf<Any?>("failed", null, 3), listOf(failed["status"], failed["answer"], failed["calls"]))
        assertEquals(emptyList<String>(), starting(nothing, "Combine"))
    }

    @Test
    fun e3_aDraftIsRevisedWithTheFeedbackUntilTheJudgeAcceptsIt() {
        val writer = Model().on("Task", sequence("draft1", "draft2", "draft3"))
        val judge = judged("{\"score\": 5, \"feedback\": \"add examples\"}", "{\"score\": 9, \"feedback\": \"good\"}")
        val result = refine(writer, judge, "T") ?: emptyMap()
        assertEquals(listOf<Any?>("accepted", "draft2", 9.0, 2), listOf(result["status"], result["draft"], n(result["score"]), result["rounds"]))
        assertEquals(listOf("Task: T", "Task: T\nPrevious draft: draft1\nFeedback: add examples\nRevise the draft."), writer.prompts)
        assertEquals("Judge: score the draft from 0 to 10 and reply with JSON {\"score\": n, \"feedback\": \"...\"}.\nTask: T\nDraft: draft1", judge.prompts.firstOrNull() ?: "")
        assertEquals(listOf(listOf<Any?>(1, 5.0, "add examples"), listOf(2, 9.0, "good")), list(result, "history").map { listOf(it["round"], n(it["score"]), it["feedback"]) })
        val edge = refine(Model().on("Task", "d"), judged("{\"score\": 8, \"feedback\": \"\"}"), "T") ?: emptyMap()
        assertEquals(listOf<Any?>("accepted", 1), listOf(edge["status"], edge["rounds"]))
        val custom = refine(Model().on("Task", "d"), judged("{\"score\": 8, \"feedback\": \"\"}", "{\"score\": 10}"), "T", 4, 10) ?: emptyMap()
        assertEquals(listOf<Any?>("accepted", 2, 10.0), listOf(custom["status"], custom["rounds"], n(custom["score"])))
    }

    @Test
    fun e4_whenTheRoundsRunOutTheBestDraftWinsAndAnUnreadableJudgeScoresZero() {
        val result = refine(Model().on("Task", sequence("draft1", "draft2", "draft3")),
            judged("{\"score\": 6, \"feedback\": \"x\"}", "{\"score\": 7, \"feedback\": \"y\"}", "{\"score\": 7, \"feedback\": \"z\"}"), "T") ?: emptyMap()
        assertEquals(listOf<Any?>("max_rounds", "draft2", 7.0, 3), listOf(result["status"], result["draft"], n(result["score"]), result["rounds"]))
        val unreadable = refine(Model().on("Task", sequence("draft1", "draft2", "draft3", "draft4")),
            judged("not json", "{\"score\": \"high\"}", "{\"score\": 11}", "{\"score\": true, \"feedback\": \"x\"}"), "T", 4) ?: emptyMap()
        assertEquals(listOf<Any?>("max_rounds", "draft1", 0.0), listOf(unreadable["status"], unreadable["draft"], n(unreadable["score"])))
        assertEquals(List(4) { "The judge reply could not be read." }, list(unreadable, "history").map { it["feedback"] })
        val prose = refine(Model().on("Task", sequence("d1", "d2")), judged("hmm", "Verdict: {\"score\": 9, \"feedback\": \"ok\"} thanks"), "T") ?: emptyMap()
        assertEquals(listOf<Any?>("accepted", "d2", 9.0), listOf(prose["status"], prose["draft"], n(prose["score"])))
    }

    @Test
    fun e5_aLabelIsReadFromTheReplyAndAnythingElseTakesTheDefaultRoute() {
        val routes = linkedMapOf<String, (String) -> String>("billing" to { t -> "B:$t" }, "technical" to { t -> "T:$t" }, "other" to { t -> "O:$t" })
        val text = "My card was charged twice"
        val m = Model().on("Classify", " Billing. ")
        assertEquals(mapOf("label" to "billing", "output" to "B:$text", "fallback" to false), route(m, text, routes, "other"))
        assertEquals(listOf("Classify: My card was charged twice\nLabels: billing, technical, other"), m.prompts)
        assertEquals("technical", (route(Model().on("Classify", "TECHNICAL"), text, routes, "other") ?: emptyMap())["label"])
        for (reply in listOf("I think it is billing", "", "refund")) {
            assertEquals(mapOf("label" to "other", "output" to "O:$text", "fallback" to true), route(Model().on("Classify", reply), text, routes, "other"))
        }
    }

    @Test
    fun e6_theMajorityAnswerWinsAndATieGoesToTheOneSeenFirst() {
        val m = Model().on("Is", sequence("Yes", "yes ", " NO", "yes"))
        val result = vote(m, "Is it safe?", 4) ?: emptyMap()
        assertEquals(listOf<Any?>("yes", mapOf("yes" to 3, "no" to 1), 0.75), listOf(result["answer"], result["votes"], result["agreement"]))
        assertEquals(List(4) { "Is it safe?" }, m.prompts)
        assertEquals("b", (vote(Model().on("Is", sequence("b", "a", "a", "b")), "Is it safe?", 4) ?: emptyMap())["answer"])
        val none = vote(Model().on("Is", "x"), "Is it safe?", 0)
        assertTrue(none != null && none.containsKey("answer") && none["answer"] == null)
        assertEquals(mapOf("x" to 5), (vote(Model().on("Is", "x"), "Is it safe?") ?: emptyMap())["votes"])
    }

    @Test
    fun e7_aWriterThatFailsEndsTheLoopWithTheBestDraftSoFar() {
        var first = true
        val flaky: (String) -> String = {
            if (first) {
                first = false
                "draft1"
            } else throw IllegalStateException("boom")
        }
        val result = refine(Model().on("Task", flaky), judged("{\"score\": 4, \"feedback\": \"more\"}"), "T") ?: emptyMap()
        assertEquals(listOf<Any?>("error", "draft1", 4.0, 1, "boom"), listOf(result["status"], result["draft"], n(result["score"]), result["rounds"], result["error"]))
        assertEquals(1, list(result, "history").size)
        val none = refine(Model().on("Task") { throw IllegalStateException("down") }, judged(), "T")
        assertTrue(none != null && none.containsKey("draft"))
        assertEquals(listOf<Any?>("error", null, null, 0, "down"), listOf(none?.get("status"), none?.get("draft"), none?.get("score"), none?.get("rounds"), none?.get("error")))
    }
}
