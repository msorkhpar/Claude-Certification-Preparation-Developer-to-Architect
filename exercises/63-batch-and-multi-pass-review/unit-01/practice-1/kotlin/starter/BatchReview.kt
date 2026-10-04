/** Batch and multi-pass review decisions: when a batch fits, what to resubmit, how a review is split into passes, and how the passes are combined. See ../../statement.md. */

val SEVERITIES = listOf("low", "medium", "high")

data class Result(val customId: String, val kind: String)

data class Step(val customId: String, val action: String)

data class Pass(val name: String, val files: List<String>)

data class Finding(val file: String, val line: Int, val severity: String, val issue: String, val confidence: Int)

data class Merged(val file: String, val line: Int, val issue: String, val severity: String, val passes: Int, val confidence: Int, val route: String)

fun submissionInterval(slaHours: Int, windowHours: Int = 24, handlingHours: Int = 2): Int {
    // TODO: the hours between submissions that still keep every item inside the SLA; refuse an SLA with no room.
    return 0
}

fun chooseApi(blocking: Boolean, needsToolLoop: Boolean = false): String? {
    // TODO: "synchronous" or "batch" for a workload.
    return null
}

fun resubmissionPlan(results: List<Result>, sizes: Map<String, Int>, limit: Int): List<Step>? {
    // TODO: for the items that did not succeed, the action of each: resubmit, fix or chunk.
    return null
}

fun reviewPlan(files: List<String>): List<Pass>? {
    // TODO: the passes of a multi-file review.
    return null
}

fun mergePasses(passes: List<List<Finding>>): List<Merged>? {
    // TODO: combine the findings of independent passes into one list, each with its route.
    return null
}
