/** An eval harness. See ../../statement.md. */
object Harness {
    /** Grade one output against the case's check: {"passed", "reason"} (and "score" for a judge check). */
    fun grade(c: Map<String, Any?>, output: String, judge: ((String) -> String)?): Map<String, Any?> =
        linkedMapOf("passed" to true, "reason" to "ok")

    /** Run every case `repeats` times through the model and grade it. A case passes only if every run passes. */
    fun runEval(cases: List<Map<String, Any?>>, model: (String) -> String, judge: ((String) -> String)?, repeats: Int): Map<String, Any?> =
        linkedMapOf("total" to 0, "passed" to 0, "pass_rate" to 0.0, "results" to emptyList<Any?>(), "by_tag" to linkedMapOf<String, Any?>(), "flaky" to emptyList<Any?>())

    /** Compare a report with success criteria: min_pass_rate, tags {tag: minimum rate}, max_flaky. */
    fun meets(report: Map<String, Any?>, criteria: Map<String, Any?>): Map<String, Any?> =
        linkedMapOf("met" to true, "failures" to emptyList<Any?>())

    /** What changed between two reports: regressions, fixed, added and removed case ids, the pass-rate change. */
    fun compare(baseline: Map<String, Any?>, current: Map<String, Any?>): Map<String, Any?> =
        linkedMapOf("regressions" to emptyList<Any?>(), "fixed" to emptyList<Any?>(), "added" to emptyList<Any?>(), "removed" to emptyList<Any?>(), "pass_rate_delta" to 0.0, "ok" to true)
}
