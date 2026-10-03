import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class HarnessTest {
    @Suppress("UNCHECKED_CAST")
    private fun obj(json: String): Map<String, Any?> = Json.parse(json) as Map<String, Any?>

    @Suppress("UNCHECKED_CAST")
    private fun cases(json: String): List<Map<String, Any?>> = Json.parse(json) as List<Map<String, Any?>>

    private fun mk(checkJson: String) = obj("""{"id":"c","input":"x","check":$checkJson}""")

    @Suppress("UNCHECKED_CAST")
    private fun asMap(o: Any?): Map<String, Any?> = (o as? Map<String, Any?>) ?: emptyMap()

    private fun asList(o: Any?): List<*> = (o as? List<*>) ?: emptyList<Any?>()

    private fun num(o: Any?): Double = (o as? Number)?.toDouble() ?: Double.NaN

    /** "passed|reason" of one grading. */
    private fun v(checkJson: String, output: String, judge: ((String) -> String)?): String {
        val r = Harness.grade(mk(checkJson), output, judge)
        return "${r["passed"]}|${r["reason"]}"
    }

    private fun reply(text: String, seen: MutableList<String>): (String) -> String = { prompt -> seen.add(prompt); text }

    private fun results(report: Map<String, Any?>): String =
        asList(report["results"]).joinToString(" ") { val r = asMap(it); "${r["id"]}:${r["passed"]}:${r["reason"]}:${r["flaky"]}" }

    @Test
    fun m1_aRunGradesEveryCaseWithItsOwnCheckAndReportsThePassRate() {
        val cs = cases("""[{"id":"c1","input":"I love it","tags":["core"],"check":{"type":"exact","expected":"positive"}},
            {"id":"c2","input":"awful","tags":["core"],"check":{"type":"exact","expected":"negative"}},
            {"id":"c3","input":"order 7","tags":["extract"],"check":{"type":"regex","pattern":"ORD-\\d{4}"}},
            {"id":"c4","input":"meh","tags":["core","edge"],"check":{"type":"exact","expected":"neutral"}}]""")
        val answers = mapOf("I love it" to "positive", "awful" to "negative", "order 7" to "The order is ORD-0007.", "meh" to "positive")
        val report = Harness.runEval(cs, { answers.getValue(it) }, null, 1)
        assertEquals(4.0, num(report["total"]))
        assertEquals(3.0, num(report["passed"]))
        assertEquals(0.75, num(report["pass_rate"]), 1e-9)
        assertEquals("c1:true:ok:false c2:true:ok:false c3:true:ok:false c4:false:mismatch:false", results(report))
        val empty = Harness.runEval(emptyList(), { it }, null, 1)
        assertEquals(0.0, num(empty["total"]))
        assertEquals(0.0, num(empty["pass_rate"]), 1e-9)
        assertTrue(asList(empty["results"]).isEmpty())
    }

    @Test
    fun e1_anExactCheckIgnoresCaseAndSpacingButNothingElse() {
        val positive = """{"type":"exact","expected":"positive"}"""
        assertEquals("true|ok", v(positive, "  Positive \n", null))
        assertEquals("true|ok", v("""{"type":"exact","expected":"not  enough info"}""", "Not enough\ninfo", null))
        assertEquals("false|mismatch", v(positive, "positively", null))
        assertEquals("false|mismatch", v(positive, "negative", null))
        assertEquals("false|mismatch", v(positive, "", null))
        assertEquals("false|mismatch", v("""{"type":"regex","pattern":"^ORD-\\d{4}$"}""", "ORD-12345", null))
    }

    @Test
    fun e2_aJsonFieldCheckNeedsAJsonObjectWithTheFieldAndTheSameTypedValue() {
        val spam = """{"type":"json_field","field":"label","equals":"spam"}"""
        assertEquals("true|ok", v(spam, """{"label":"spam","score":0.9}""", null))
        assertEquals("true|ok", v(spam, " \n{\"label\": \"spam\"}\n", null))
        assertEquals("false|mismatch", v(spam, """{"label":"ham"}""", null))
        assertEquals("false|missing field", v(spam, """{"score":1}""", null))
        assertEquals("false|not json", v(spam, """Sure! {"label":"spam"}""", null))
        assertEquals("false|not json", v(spam, "```json\n{\"label\":\"spam\"}\n```", null))
        assertEquals("false|not json", v(spam, """["spam"]""", null))
        val count = """{"type":"json_field","field":"count","equals":3}"""
        assertEquals("true|ok", v(count, """{"count":3}""", null))
        assertEquals("false|mismatch", v(count, """{"count":"3"}""", null))
        assertEquals("false|mismatch", v("""{"type":"json_field","field":"ok","equals":true}""", """{"ok":1}""", null))
    }

    @Test
    fun e3_aJudgeCheckSendsTheRubricPromptAndAcceptsOnlyABareScoreAtTheThreshold() {
        val check = """{"type":"judge","criterion":"empathetic","threshold":4}"""
        val seen = mutableListOf<String>()
        val r = Harness.grade(mk(check), "We are sorry.", reply("5", seen))
        assertEquals("true|ok|5", "${r["passed"]}|${r["reason"]}|${r["score"]}")
        assertEquals(listOf("Rate this response on a scale of 1-5 for empathetic:\n<response>We are sorry.</response>\n1: Not at all empathetic\n5: Perfectly empathetic\nOutput only the number."), seen)
        assertEquals("true|ok", v(check, "x", reply(" 4\n", seen)))
        assertEquals("false|below threshold", v(check, "x", reply("3", seen)))
        val calm = """{"type":"judge","criterion":"calm"}"""
        assertEquals("true|ok", v(calm, "x", reply("4", seen)))
        assertEquals("false|below threshold", v(calm, "x", reply("3", seen)))
        for (bad in listOf("Score: 4", "I'd say 4 or 5", "6", "0", "", "4.5")) assertEquals("false|ungradable", v(check, "x", reply(bad, seen)), bad)
        assertEquals("false|ungradable", v(check, "x", null))
        assertEquals("false|ungradable", v(check, "x") { throw IllegalStateException("judge down") })
        var calls = 0
        Harness.runEval(cases("""[{"id":"a","input":"q","check":{"type":"exact","expected":"y"}}]"""), { "y" }, { calls++; "5" }, 1)
        assertEquals(0, calls)
    }

    @Test
    fun e4_aModelThatFailsOnOneCaseDoesNotStopTheRun() {
        val model = { text: String -> if (text == "boom") throw IllegalStateException("503 from upstream") else "ok" }
        val cs = cases("""[{"id":"a","input":"a","check":{"type":"exact","expected":"ok"}},
            {"id":"boom","input":"boom","check":{"type":"exact","expected":"ok"}},
            {"id":"c","input":"c","check":{"type":"exact","expected":"ok"}}]""")
        val report = Harness.runEval(cs, model, null, 1)
        assertEquals(3.0, num(report["total"]))
        assertEquals(2.0, num(report["passed"]))
        assertEquals("a:true:ok:false boom:false:model error:false c:true:ok:false", results(report))
    }

    @Test
    fun e5_tagsReportTheirOwnRatesAndSuccessCriteriaJudgeEachDimension() {
        val cs = cases("""[{"id":"a","input":"1","tags":["core"],"check":{"type":"exact","expected":"1"}},
            {"id":"b","input":"2","tags":["core","edge"],"check":{"type":"exact","expected":"2"}},
            {"id":"c","input":"3","tags":["edge"],"check":{"type":"exact","expected":"x"}}]""")
        val byTag = asMap(Harness.runEval(cs, { it }, null, 1)["by_tag"])
        assertEquals("[core, edge]", byTag.keys.toString())
        assertEquals(2.0, num(asMap(byTag["core"])["passed"]))
        assertEquals(2.0, num(asMap(byTag["core"])["total"]))
        assertEquals(1.0, num(asMap(byTag["edge"])["passed"]))
        assertEquals(2.0, num(asMap(byTag["edge"])["total"]))
        val rep = """{"total":10,"passed":8,"pass_rate":0.8,"results":[],"flaky":[],"by_tag":{"core":{"passed":6,"total":6},"edge":{"passed":2,"total":4}}}"""
        val flaky = rep.replace("\"flaky\":[]", "\"flaky\":[\"c3\"]")
        assertEquals("{met=true, failures=[]}", Harness.meets(obj(rep), obj("""{"min_pass_rate":0.8,"tags":{"edge":0.5}}""")).toString())
        assertEquals("{met=true, failures=[]}", Harness.meets(obj(rep), obj("{}")).toString())
        assertEquals("{met=false, failures=[overall, tag:edge, tag:rare]}",
            Harness.meets(obj(rep), obj("""{"min_pass_rate":0.85,"tags":{"edge":0.75,"core":1.0,"rare":0.5}}""")).toString())
        assertEquals("{met=false, failures=[flaky]}", Harness.meets(obj(flaky), obj("""{"max_flaky":0}""")).toString())
        assertEquals("{met=true, failures=[]}", Harness.meets(obj(flaky), obj("""{"max_flaky":1}""")).toString())
    }

    private fun report(rate: Double, rows: String): Map<String, Any?> {
        val results = rows.split(" ").joinToString(",") { val (id, p) = it.split(":"); """{"id":"$id","passed":${p == "T"}}""" }
        return obj("""{"pass_rate":$rate,"results":[$results]}""")
    }

    @Test
    fun e6_aRegressionRunNamesWhatBrokeWhatWasFixedAndWhatWentMissing() {
        val base = report(0.75, "a:T b:T c:F d:T")
        val diff = Harness.compare(base, report(0.75, "a:T b:F c:T e:T"))
        assertEquals("[b]|[c]|[e]|[d]", "${diff["regressions"]}|${diff["fixed"]}|${diff["added"]}|${diff["removed"]}")
        assertEquals(0.0, num(diff["pass_rate_delta"]), 1e-9)
        assertEquals(false, diff["ok"])
        val better = Harness.compare(report(0.5, "a:T b:T c:F d:F"), report(0.75, "a:T b:F c:T d:T"))
        assertEquals("[b]", better["regressions"].toString())
        assertEquals(0.25, num(better["pass_rate_delta"]), 1e-9)
        assertEquals(false, better["ok"])
        val same = Harness.compare(base, report(1.0, "a:T b:T c:T d:T"))
        assertEquals("[]|[c]|true", "${same["regressions"]}|${same["fixed"]}|${same["ok"]}")
        val dropped = Harness.compare(report(0.5, "a:T d:F"), report(1.0, "a:T"))
        assertEquals("[]|[d]|false", "${dropped["regressions"]}|${dropped["removed"]}|${dropped["ok"]}")
    }

    @Test
    fun e7_repeatedRunsExposeFlakyCasesAndACasePassesOnlyIfEveryRunDoes() {
        val outputs = mapOf("q" to listOf("yes", "yes", "no"), "r" to listOf("yes", "yes", "yes"), "s" to listOf("no", "no", "no"))
        val counts = mutableMapOf<String, Int>()
        var calls = 0
        val model = { text: String -> calls++; val n = counts.merge(text, 1, Int::plus)!! - 1; outputs.getValue(text)[n] }
        val cs = cases("""[{"id":"q","input":"q","check":{"type":"exact","expected":"yes"}},
            {"id":"r","input":"r","check":{"type":"exact","expected":"yes"}},
            {"id":"s","input":"s","check":{"type":"exact","expected":"yes"}}]""")
        val report = Harness.runEval(cs, model, null, 3)
        assertEquals(9, calls)
        assertEquals("q:false:mismatch:true r:true:ok:false s:false:mismatch:false", results(report))
        assertEquals("[q]", report["flaky"].toString())
        assertEquals(1.0, num(report["passed"]))
        val single = Harness.runEval(cs.subList(0, 1), { "yes" }, null, 1)
        assertEquals("q:true:ok:false", results(single))
        assertTrue(asList(single["flaky"]).isEmpty())
    }
}
