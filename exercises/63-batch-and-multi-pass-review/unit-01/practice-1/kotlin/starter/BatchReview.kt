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
    // TODO 1 of 8 (finish this to pass m1): the interval. Receives the SLA, the window and the handling time, all in
    //   hours. Return the SLA minus the window minus the handling time. Example: an SLA of 30 hours, defaults -> 4.
    val interval = slaHours
    // TODO 2 of 8 (finish this to pass e1): the refusal. When the interval is zero or negative, refuse with an error
    //   that says the SLA leaves no room to wait for a batch to fill. Example: an SLA of 26 hours, defaults -> refused.
    return interval
}

// TODO 3 of 8 (finish this to pass e2): the API. Receives whether something is blocked on the result and whether the job
//   needs a tool loop. Return synchronous when either is true, otherwise batch. Example: blocking false, tool loop true ->
//   synchronous.
fun chooseApi(blocking: Boolean, needsToolLoop: Boolean = false): String = "batch"

fun resubmissionPlan(results: List<Result>, sizes: Map<String, Int>, limit: Int): List<Step> {
    val plan = mutableListOf<Step>()
    for (r in results) {
        // TODO 4 of 8 (finish this to pass e3): the items to resubmit. Skip a result whose kind is succeeded; every
        //   other result is planned by its custom id. Example: results [(a, succeeded), (b, errored)] -> a plan for b
        //   only.
        // TODO 5 of 8 (finish this to pass e4): the action for one item. When the item's size is above the limit, the
        //   action is chunk; otherwise fix when its kind is invalid_request; otherwise resubmit. An entry exactly at the
        //   limit is not chunked. Example: size 101, limit 100 -> chunk.
        val action = if (r.kind == "invalid_request") "fix" else "resubmit"
        plan += Step(r.customId, action)
    }
    return plan
}

fun reviewPlan(files: List<String>): List<Pass> {
    val passes = files.map { Pass("local:$it", listOf(it)) }.toMutableList()
    // TODO 6 of 8 (finish this to pass e5): the integration pass. After the local passes, when there is more than one
    //   file add one pass named integration over all the files. Example: two files -> local, local, integration; one file
    //   -> local only.
    return passes
}

fun mergePasses(passes: List<List<Finding>>): List<Merged> {
    val severity = linkedMapOf<List<Any>, String>()
    val confidence = linkedMapOf<List<Any>, Int>()
    val seen = linkedMapOf<List<Any>, MutableSet<Int>>()
    passes.forEachIndexed { number, findings ->
        for (f in findings) {
            val key = listOf<Any>(f.file, f.line, f.issue)
            // TODO 7 of 8 (finish this to pass e6): the merge of one finding seen again. When the same finding (file,
            //   line, issue) is reported by another pass, keep the higher of the two severities (low, medium, high) and
            //   the lower of the two confidences. Example: medium at 90 and high at 70 -> high at 70.
            severity.putIfAbsent(key, f.severity)
            confidence.putIfAbsent(key, f.confidence)
            seen.getOrPut(key) { linkedSetOf() }.add(number)
        }
    }
    return severity.keys.map { key ->
        val count = seen.getValue(key).size
        val conf = confidence.getValue(key)
        // TODO 8 of 8 (finish this to pass e7): the route of a merged finding. Receives the number of passes that
        //   reported it and its confidence. Return accept when at least 2 passes reported it and the confidence is at
        //   least 80, otherwise verify. Example: 2 passes, confidence 79 -> verify.
        Merged(key[0] as String, key[1] as Int, key[2] as String, severity.getValue(key), count, conf, "verify")
    }
}
