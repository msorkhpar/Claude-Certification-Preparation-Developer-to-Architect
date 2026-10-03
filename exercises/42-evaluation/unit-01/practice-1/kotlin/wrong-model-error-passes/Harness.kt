/** An eval harness. See ../../statement.md. */
object Harness {
    private fun norm(text: String) = text.trim().replace(Regex("\\s+"), " ").lowercase()

    @Suppress("UNCHECKED_CAST")
    private fun asMap(o: Any?): Map<String, Any?> = o as Map<String, Any?>

    private fun verdict(ok: Boolean, failReason: String): Map<String, Any?> = linkedMapOf("passed" to ok, "reason" to if (ok) "ok" else failReason)

    private fun ungradable(): Map<String, Any?> = linkedMapOf("passed" to false, "reason" to "ungradable", "score" to null)

    fun judgePrompt(criterion: String, output: String) =
        "Rate this response on a scale of 1-5 for $criterion:\n<response>$output</response>\n" +
            "1: Not at all $criterion\n5: Perfectly $criterion\nOutput only the number."

    /** Grade one output against the case's check: {"passed", "reason"} (and "score" for a judge check). */
    fun grade(c: Map<String, Any?>, output: String, judge: ((String) -> String)?): Map<String, Any?> {
        val check = asMap(c["check"])
        when (check["type"] as String) {
            "exact" -> return verdict(norm(output) == norm(check["expected"] as String), "mismatch")
            "regex" -> return verdict(Regex(check["pattern"] as String).containsMatchIn(output), "mismatch")
            "json_field" -> {
                val data = try { Json.parse(output) } catch (e: RuntimeException) { return verdict(false, "not json") }
                if (data !is Map<*, *>) return verdict(false, "not json")
                val obj = asMap(data)
                val field = check["field"] as String
                if (field !in obj) return verdict(false, "missing field")
                return verdict(obj[field] == check["equals"], "mismatch")
            }
            "judge" -> {
                if (judge == null) return ungradable()
                val reply = try { judge(judgePrompt(check["criterion"] as String, output)) } catch (e: RuntimeException) { return ungradable() }
                val text = reply.trim()
                if (!Regex("[1-5]").matches(text)) return ungradable()
                val score = text.toInt()
                val threshold = (check["threshold"] as? Number)?.toLong() ?: 4L
                return if (score >= threshold) linkedMapOf("passed" to true, "reason" to "ok", "score" to score)
                else linkedMapOf("passed" to false, "reason" to "below threshold", "score" to score)
            }
            else -> return verdict(false, "ungradable")
        }
    }

    /** Run every case `repeats` times through the model and grade it. A case passes only if every run passes. */
    fun runEval(cases: List<Map<String, Any?>>, model: (String) -> String, judge: ((String) -> String)?, repeats: Int): Map<String, Any?> {
        val results = mutableListOf<Any?>()
        val byTag = linkedMapOf<String, MutableMap<String, Any?>>()
        val flaky = mutableListOf<Any?>()
        for (c in cases) {
            val runs = (0 until repeats).map {
                val output = try { model(c["input"] as String) } catch (e: RuntimeException) { return@map linkedMapOf<String, Any?>("passed" to true, "reason" to "ok") }
                grade(c, output, judge)
            }
            val passed = runs.all { it["passed"] == true }
            val mixed = runs.any { it["passed"] == true } && !passed
            val reason = if (passed) "ok" else runs.first { it["passed"] != true }["reason"]
            results.add(linkedMapOf("id" to c["id"], "passed" to passed, "reason" to reason, "flaky" to mixed))
            if (mixed) flaky.add(c["id"])
            for (tag in (c["tags"] as? List<*>) ?: emptyList<Any?>()) {
                val row = byTag.getOrPut(tag as String) { linkedMapOf("passed" to 0, "total" to 0) }
                row["total"] = (row["total"] as Int) + 1
                row["passed"] = (row["passed"] as Int) + if (passed) 1 else 0
            }
        }
        val total = results.size
        val passedCount = results.count { asMap(it)["passed"] == true }
        return linkedMapOf("total" to total, "passed" to passedCount, "pass_rate" to if (total == 0) 0.0 else passedCount.toDouble() / total,
            "results" to results, "by_tag" to byTag, "flaky" to flaky)
    }

    /** Compare a report with success criteria: min_pass_rate, tags {tag: minimum rate}, max_flaky. */
    fun meets(report: Map<String, Any?>, criteria: Map<String, Any?>): Map<String, Any?> {
        val failures = mutableListOf<Any?>()
        val min = criteria["min_pass_rate"] as? Number
        if (min != null && (report["pass_rate"] as Number).toDouble() < min.toDouble()) failures.add("overall")
        val byTag = asMap(report["by_tag"])
        for ((tag, minimum) in (criteria["tags"] as? Map<*, *>) ?: emptyMap<Any?, Any?>()) {
            val row = byTag[tag as String]?.let { asMap(it) }
            val total = (row?.get("total") as? Number)?.toDouble() ?: 0.0
            if (row == null || total == 0.0 || (row["passed"] as Number).toDouble() / total < (minimum as Number).toDouble()) failures.add("tag:$tag")
        }
        val maxFlaky = criteria["max_flaky"] as? Number
        if (maxFlaky != null && (report["flaky"] as List<*>).size > maxFlaky.toLong()) failures.add("flaky")
        return linkedMapOf("met" to failures.isEmpty(), "failures" to failures)
    }

    private fun outcomes(report: Map<String, Any?>): Map<String, Boolean> =
        (report["results"] as List<*>).associate { asMap(it)["id"] as String to (asMap(it)["passed"] == true) }

    /** What changed between two reports: regressions, fixes, added and removed cases, the pass-rate change. */
    fun compare(baseline: Map<String, Any?>, current: Map<String, Any?>): Map<String, Any?> {
        val before = outcomes(baseline)
        val now = outcomes(current)
        val regressions = now.keys.filter { before[it] == true && now[it] == false }
        val fixed = now.keys.filter { before[it] == false && now[it] == true }
        val added = now.keys.filter { it !in before }
        val removed = before.keys.filter { it !in now }
        val delta = (current["pass_rate"] as Number).toDouble() - (baseline["pass_rate"] as Number).toDouble()
        return linkedMapOf("regressions" to regressions, "fixed" to fixed, "added" to added, "removed" to removed,
            "pass_rate_delta" to delta, "ok" to (regressions.isEmpty() && removed.isEmpty()))
    }
}
