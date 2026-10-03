/** Task decomposition: a per-item pass and a cross-item pass, an adaptive loop, and the choice between them. See ../../statement.md. */

/** The model's review of one file, or of one part of it: a map with "findings" (a list of text) and "summary" (text). */
typealias FilePass = (String, String, Int, Int) -> Map<String, Any?>
typealias CrossPass = (List<Map<String, String>>) -> List<String>
typealias StepPlanner = (String, List<Map<String, String>>) -> Any?

@Suppress("UNCHECKED_CAST")
fun reviewChanges(files: List<Map<String, String>>, filePass: FilePass, crossPass: CrossPass, maxLines: Int = 40): Map<String, Any?>? {
    val reviewed = linkedMapOf<String, Map<String, Any?>>()
    val failed = linkedMapOf<String, String>()
    val skipped = mutableListOf<String>()
    for (item in files) {
        val path = item.getValue("path")
        val text = item.getValue("text")
        if (text.isBlank()) {
            skipped.add(path) // nothing to review is not worth a model call
            continue
        }
        val lines = text.lines().let { if (it.last().isEmpty()) it.dropLast(1) else it }
        val parts = (lines.size + maxLines - 1) / maxLines
        val findings = mutableListOf<String>()
        val summaries = mutableListOf<String>()
        try {
            for (part in 0 until parts) {
                val chunk = lines.subList(part * maxLines, minOf(lines.size, (part + 1) * maxLines)).joinToString("\n")
                val result = filePass(path, chunk, part + 1, parts)
                findings.addAll(result["findings"] as List<String>)
                summaries.add(result["summary"] as String)
            }
        } catch (error: RuntimeException) { // one file failing must not stop the others
            failed[path] = error.message.toString()
            continue
        }
        reviewed[path] = linkedMapOf("findings" to findings, "summary" to summaries.joinToString(" "), "parts" to parts)
    }
    var cross: List<String> = emptyList()
    var crossError: String? = null
    if (reviewed.size >= 1) { // a relation between files needs at least two of them
        try {
            cross = crossPass(reviewed.map { (path, r) -> linkedMapOf("path" to path, "summary" to r["summary"] as String) })
        } catch (error: RuntimeException) {
            crossError = error.message.toString()
        }
    }
    return linkedMapOf("files" to reviewed, "cross" to cross, "failed" to failed, "skipped" to skipped, "cross_error" to crossError)
}

fun runAdaptive(planner: StepPlanner, worker: (String) -> String, goal: String, maxSteps: Int = 6): Map<String, Any?>? {
    val steps = mutableListOf<Map<String, String>>()
    fun finish(status: String, summary: String = "", reason: String = ""): Map<String, Any?> = linkedMapOf("status" to status, "summary" to summary, "steps" to steps, "reason" to reason)
    while (true) {
        val reply = planner(goal, steps.map { LinkedHashMap(it) })
        val done = (reply as? Map<*, *>)?.get("done") as? Boolean ?: return finish("bad_plan", reason = "the planner reply could not be read")
        val plan = reply as Map<*, *>
        if (done) return finish("done", plan["summary"]?.toString() ?: "")
        val subtask = plan["next"]?.toString()?.trim() ?: ""
        if (subtask.isEmpty()) return finish("stuck", reason = "no next step")
        if (steps.any { it.getValue("subtask").lowercase() == subtask.lowercase() }) return finish("stuck", reason = "repeated subtask: $subtask")
        if (steps.size >= maxSteps) return finish("step_limit", reason = "step limit reached")
        val result = try {
            worker(subtask)
        } catch (error: RuntimeException) { // the planner decides what a failed step means
            "ERROR: ${error.message}"
        }
        steps.add(linkedMapOf("subtask" to subtask, "result" to result))
    }
}

fun chooseStrategy(task: Map<String, Any?>): String {
    val known = task["steps_known"] as? Boolean
    val count = task["items"] as? Int
    require(known != null && count != null && count >= 0) { "steps_known must be true or false and items a whole number of at least 0" }
    if (!known) return "adaptive"
    if (count >= 2 && task["items_interact"] == true) return "per_item_then_cross"
    return "fixed_chain"
}
