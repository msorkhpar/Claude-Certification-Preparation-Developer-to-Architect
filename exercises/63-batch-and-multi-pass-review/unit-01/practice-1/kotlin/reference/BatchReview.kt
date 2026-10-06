/** Batch and multi-pass review decisions: when a batch fits, what to resubmit, how a review is split into passes, and how the passes are combined. See ../../statement.md. */

private val log = System.getLogger("batch_review")

val SEVERITIES = listOf("low", "medium", "high")

data class Result(val customId: String, val kind: String)

data class Step(val customId: String, val action: String)

data class Pass(val name: String, val files: List<String>)

data class Finding(val file: String, val line: Int, val severity: String, val issue: String, val confidence: Int)

data class Merged(val file: String, val line: Int, val issue: String, val severity: String, val passes: Int, val confidence: Int, val route: String)

fun submissionInterval(slaHours: Int, windowHours: Int = 24, handlingHours: Int = 2): Int {
    log.log(System.Logger.Level.DEBUG, "submissionInterval input: {0}", slaHours)
    val interval = slaHours - windowHours - handlingHours
    require(interval > 0) { "the SLA leaves no room to wait for a batch to fill" }
    return interval
}

fun chooseApi(blocking: Boolean, needsToolLoop: Boolean = false): String = if (blocking || needsToolLoop) "synchronous" else "batch"

fun resubmissionPlan(results: List<Result>, sizes: Map<String, Int>, limit: Int): List<Step> {
    val plan = mutableListOf<Step>()
    for (r in results) {
        if (r.kind == "succeeded") continue
        val action = if ((sizes[r.customId] ?: 0) > limit) "chunk" else if (r.kind == "invalid_request") "fix" else "resubmit"
        plan += Step(r.customId, action)
    }
    return plan
}

fun reviewPlan(files: List<String>): List<Pass> {
    val passes = files.map { Pass("local:$it", listOf(it)) }.toMutableList()
    if (files.size > 1) passes += Pass("integration", files.toList())
    return passes
}

fun mergePasses(passes: List<List<Finding>>): List<Merged> {
    val severity = linkedMapOf<List<Any>, String>()
    val confidence = linkedMapOf<List<Any>, Int>()
    val seen = linkedMapOf<List<Any>, MutableSet<Int>>()
    passes.forEachIndexed { number, findings ->
        for (f in findings) {
            val key = listOf<Any>(f.file, f.line, f.issue)
            val old = severity[key]
            severity[key] = if (old == null || SEVERITIES.indexOf(f.severity) > SEVERITIES.indexOf(old)) f.severity else old
            confidence[key] = minOf(confidence[key] ?: f.confidence, f.confidence)
            seen.getOrPut(key) { linkedSetOf() }.add(number)
        }
    }
    return severity.keys.map { key ->
        val count = seen.getValue(key).size
        val conf = confidence.getValue(key)
        Merged(key[0] as String, key[1] as Int, key[2] as String, severity.getValue(key), count, conf, if (count >= 2 && conf >= 80) "accept" else "verify")
    }
}
