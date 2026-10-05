private val log = System.getLogger("harness")

/** An eval harness. See ../../statement.md. */
object Harness {
    private fun norm(text: String): String {
        // TODO 1 of 9 (unlocks e1): the form two texts are compared in for an exact check.
        // Receives a text and returns it trimmed, every run of white space made one space, and lower-cased. Nothing else changes.
        // Example: norm("  Paris   FRANCE ") -> "paris france"
        return text
    }

    @Suppress("UNCHECKED_CAST")
    private fun asMap(o: Any?): Map<String, Any?> = o as Map<String, Any?>

    private fun verdict(ok: Boolean, failReason: String): Map<String, Any?> = linkedMapOf("passed" to ok, "reason" to if (ok) "ok" else failReason)

    private fun ungradable(): Map<String, Any?> = linkedMapOf("passed" to false, "reason" to "ungradable", "score" to null)

    fun judgePrompt(criterion: String, output: String) =
        "Rate this response on a scale of 1-5 for $criterion:\n<response>$output</response>\n" +
            "1: Not at all $criterion\n5: Perfectly $criterion\nOutput only the number."

    private fun fieldResult(obj: Map<String, Any?>, check: Map<String, Any?>): Map<String, Any?> {
        // TODO 2 of 9 (unlocks e2): grade a parsed JSON object against a json_field check.
        // Receives the parsed object and the check {field, equals}. Returns `verdict(passed, reason)`: "missing field" when the field is
        // absent; "ok" when the value equals `equals` with the same JSON type (== on the parsed values does that); otherwise "mismatch".
        // Example: {"n": "3"} against equals 3 -> {passed=false, reason=mismatch}
        return verdict(false, "mismatch")
    }

    private fun parseScore(reply: String): Int? {
        // TODO 3 of 9 (unlocks e3): read the judge's reply as a score.
        // Receives the reply. Returns the number when the trimmed reply is exactly one digit from 1 to 5, else null.
        // Example: parseScore(" 4 ") -> 4, parseScore("I give it a 4") -> null
        return null
    }

    private fun judged(score: Int, threshold: Long): Map<String, Any?> {
        // TODO 4 of 9 (unlocks e3): the verdict for a judge score.
        // Receives the score and the threshold. Returns a map {passed, reason, score}: "ok" at or above the threshold, else "below threshold".
        // Example: judged(4, 4) -> {passed=true, reason=ok, score=4}
        return linkedMapOf("passed" to false, "reason" to "below threshold", "score" to score)
    }

    /** Grade one output against the case's check: {"passed", "reason"} (and "score" for a judge check). */
    fun grade(c: Map<String, Any?>, output: String, judge: ((String) -> String)?): Map<String, Any?> {
        log.log(System.Logger.Level.DEBUG, "grade input: {0}", output)
        val check = asMap(c["check"])
        when (check["type"] as String) {
            "exact" -> return verdict(norm(output) == norm(check["expected"] as String), "mismatch")
            "regex" -> return verdict(Regex(check["pattern"] as String).containsMatchIn(output), "mismatch")
            "json_field" -> {
                val data = try { Json.parse(output) } catch (e: RuntimeException) { return verdict(false, "not json") }
                if (data !is Map<*, *>) return verdict(false, "not json")
                val obj = asMap(data)
                return fieldResult(obj, check)
            }
            "judge" -> {
                if (judge == null) return ungradable()
                val reply = try { judge(judgePrompt(check["criterion"] as String, output)) } catch (e: RuntimeException) { return ungradable() }
                val score = parseScore(reply) ?: return ungradable()
                return judged(score, (check["threshold"] as? Number)?.toLong() ?: 4L)
            }
            else -> return verdict(false, "ungradable")
        }
    }

    private fun runOnce(model: (String) -> String, judge: ((String) -> String)?, c: Map<String, Any?>): Map<String, Any?> {
        // TODO 5 of 9 (unlocks e4): one run of one case.
        // Receives the model, the judge and the case. Returns the verdict of `grade` for the model's output; when the model throws, the
        // verdict is {passed=false, reason=model error} and nothing is thrown. Example: a model that throws -> {passed=false, reason=model error}
        return linkedMapOf("passed" to false, "reason" to "model error")
    }

    private fun outcome(runs: List<Map<String, Any?>>): Triple<Boolean, Boolean, Any?> {
        // TODO 6 of 9 (unlocks e7): combine the runs of one case.
        // Receives the verdicts of its runs. Returns Triple(passed, mixed, reason): passed only if every run passed; mixed (flaky) when
        // some passed and some failed; the reason is "ok", or the reason of the first failed run.
        // Example: [ok, mismatch] -> Triple(false, true, "mismatch")
        return Triple(false, false, "mismatch")
    }

    private fun countTags(byTag: MutableMap<String, MutableMap<String, Any?>>, tags: List<*>, passed: Boolean) {
        // TODO 7 of 9 (unlocks e5): count one case under each of its tags.
        // Receives the map `byTag` (changed in place), the case's tags and whether it passed. For every tag, make a row
        // {passed=0, total=0} when there is none (getOrPut), then `total` goes up by one and `passed` by one when the case passed.
        // Example: tags ["a"], passed -> byTag["a"] is {passed=1, total=1}
    }

    /** Run every case `repeats` times through the model and grade it. A case passes only if every run passes. */
    fun runEval(cases: List<Map<String, Any?>>, model: (String) -> String, judge: ((String) -> String)?, repeats: Int): Map<String, Any?> {
        val results = mutableListOf<Any?>()
        val byTag = linkedMapOf<String, MutableMap<String, Any?>>()
        val flaky = mutableListOf<Any?>()
        for (c in cases) {
            val runs = (0 until repeats).map { runOnce(model, judge, c) }
            val (passed, mixed, reason) = outcome(runs)
            results.add(linkedMapOf("id" to c["id"], "passed" to passed, "reason" to reason, "flaky" to mixed))
            if (mixed) flaky.add(c["id"])
            countTags(byTag, (c["tags"] as? List<*>) ?: emptyList<Any?>(), passed)
        }
        val total = results.size
        val passedCount = results.count { asMap(it)["passed"] == true }
        return linkedMapOf("total" to total, "passed" to passedCount, "pass_rate" to if (total == 0) 0.0 else passedCount.toDouble() / total,
            "results" to results, "by_tag" to byTag, "flaky" to flaky)
    }

    private fun tagFailed(row: Map<String, Any?>?, minimum: Double): Boolean {
        // TODO 8 of 9 (unlocks e5): does one tag fail its minimum rate?
        // Receives the tag's row {passed, total} or null (no case carries the tag) and the minimum rate. Returns true for null, an empty
        // row, or a rate below the minimum; a rate equal to the minimum is fine. Example: ({passed=1, total=2}, 0.5) -> false
        return false
    }

    /** Compare a report with success criteria: min_pass_rate, tags {tag: minimum rate}, max_flaky. */
    fun meets(report: Map<String, Any?>, criteria: Map<String, Any?>): Map<String, Any?> {
        val failures = mutableListOf<Any?>()
        val min = criteria["min_pass_rate"] as? Number
        if (min != null && (report["pass_rate"] as Number).toDouble() < min.toDouble()) failures.add("overall")
        val byTag = asMap(report["by_tag"])
        for ((tag, minimum) in (criteria["tags"] as? Map<*, *>) ?: emptyMap<Any?, Any?>()) {
            if (tagFailed(byTag[tag as String]?.let { asMap(it) }, (minimum as Number).toDouble())) failures.add("tag:$tag")
        }
        val maxFlaky = criteria["max_flaky"] as? Number
        if (maxFlaky != null && (report["flaky"] as List<*>).size > maxFlaky.toLong()) failures.add("flaky")
        return linkedMapOf("met" to failures.isEmpty(), "failures" to failures)
    }

    private fun outcomes(report: Map<String, Any?>): Map<String, Boolean> =
        (report["results"] as List<*>).associate { asMap(it)["id"] as String to (asMap(it)["passed"] == true) }

    private fun changes(before: Map<String, Boolean>, now: Map<String, Boolean>): List<List<String>> {
        // TODO 9 of 9 (unlocks e6): what changed between two runs.
        // Receives two maps {case id -> passed}, the baseline and the current run. Returns a list of four id lists: regressions (passed
        // before, fails now), fixed (failed before, passes now), added (only in the current run), removed (only in the baseline). The
        // first three follow the current order, removed the baseline's. Example: before {a=true}, now {a=false, b=true} -> [[a], [], [b], []]
        return listOf(emptyList(), emptyList(), emptyList(), emptyList())
    }

    /** What changed between two reports: regressions, fixes, added and removed cases, the pass-rate change. */
    fun compare(baseline: Map<String, Any?>, current: Map<String, Any?>): Map<String, Any?> {
        val before = outcomes(baseline)
        val now = outcomes(current)
        val (regressions, fixed, added, removed) = changes(before, now)
        val delta = (current["pass_rate"] as Number).toDouble() - (baseline["pass_rate"] as Number).toDouble()
        return linkedMapOf("regressions" to regressions, "fixed" to fixed, "added" to added, "removed" to removed,
            "pass_rate_delta" to delta, "ok" to (regressions.isEmpty() && removed.isEmpty()))
    }
}
