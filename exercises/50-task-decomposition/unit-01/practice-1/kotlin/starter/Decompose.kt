/** Task decomposition: a per-item pass and a cross-item pass, an adaptive loop, and the choice between them. See ../../statement.md. */

/** The model's review of one file, or of one part of it: a map with "findings" (a list of text) and "summary" (text). */
private val log = System.getLogger("decompose")

typealias FilePass = (String, String, Int, Int) -> Map<String, Any?>
typealias CrossPass = (List<Map<String, String>>) -> List<String>
typealias StepPlanner = (String, List<Map<String, String>>) -> Any?

@Suppress("UNCHECKED_CAST")
fun reviewChanges(files: List<Map<String, String>>, filePass: FilePass, crossPass: CrossPass, maxLines: Int = 40): Map<String, Any?>? {
    log.log(System.Logger.Level.DEBUG, "reviewChanges input: {0}", files)
    val reviewed = linkedMapOf<String, Map<String, Any?>>()
    val failed = linkedMapOf<String, String>()
    val skipped = mutableListOf<String>()
    for (item in files) {
        val path = item.getValue("path")
        val text = item.getValue("text")
        // TODO 2 of 9 (finish this to pass e2): the blank file rule. When the file's text is blank, add its path to
        //   `skipped` and go on to the next file. Example: "\n  \n" -> skipped.
        val lines = text.lines().let { if (it.last().isEmpty()) it.dropLast(1) else it }
        // TODO 3 of 9 (finish this to pass e2): the number of parts. Receives the lines of the file and max_lines.
        //   Return how many parts of at most max_lines lines the file needs, rounding up. Example: 41 lines, max_lines 40
        //   -> 2.
        val parts = 1
        val findings = mutableListOf<String>()
        val summaries = mutableListOf<String>()
        try {
            for (part in 0 until parts) {
                // TODO 1 of 9 (finish this to pass m1, e1): the review of one part of a file. Slice the part's lines
                //   (part number 1..parts, max_lines per part), call the file pass with (path, text of the part, part
                //   number from 1, parts), add its findings to `findings` and its summary to `summaries`. Example: a 5
                //   line file, max_lines 3 -> two calls, parts 1 and 2 of 2.
            }
        } catch (error: RuntimeException) {
            // TODO 4 of 9 (finish this to pass e3): the failing file. When the file pass throws, record the path with the
            //   error's message in `failed` and go on to the next file. Example: the pass raises "boom" for a.py -> failed
            //   {a.py: "boom"}, and b.py is still reviewed.
            continue
        }
        reviewed[path] = linkedMapOf("findings" to findings, "summary" to summaries.joinToString(" "), "parts" to parts)
    }
    var cross: List<String> = emptyList()
    var crossError: String? = null
    // TODO 5 of 9 (finish this to pass e3): the guard of the cross pass. Run it only when at least two files were
    //   reviewed (a relation between files needs two). Example: one reviewed file and one failed -> no cross pass.
    if (reviewed.size >= 0) {
        try {
            // TODO 6 of 9 (finish this to pass m1, e1): the cross pass call. Call the cross pass with one {path,
            //   summary} entry per reviewed file (the joined summary, never the text) and keep what it returns in `cross`.
            //   Example: two files -> one call with two entries.
            cross = emptyList()
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
        // TODO 7 of 9 (finish this to pass e4): the question to the planner. Call the planner with the goal and a copy
        //   of the steps done so far (each {subtask, result}). Example: after two steps the planner receives a list of
        //   two.
        val reply = planner(goal, emptyList())
        val done = (reply as? Map<*, *>)?.get("done") as? Boolean ?: return finish("bad_plan", reason = "the planner reply could not be read")
        val plan = reply as Map<*, *>
        if (done) return finish("done", plan["summary"]?.toString() ?: "")
        val subtask = plan["next"]?.toString()?.trim() ?: ""
        // TODO 8 of 9 (finish this to pass e5): the stuck rules. When the planner gives no next step, finish with status
        //   stuck and the reason "no next step"; when the next step was already done (compare ignoring case), finish with
        //   status stuck and the reason "repeated subtask: NAME". Example: next "Fix A" after "fix a" -> stuck.
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
    // TODO 9 of 9 (finish this to pass e6): the choice. Receives the validated fields. Return adaptive when the steps
    //   are not known; otherwise per_item_then_cross when there are at least 2 items and items_interact is true; otherwise
    //   fixed_chain. Example: steps known, 3 items that interact -> per_item_then_cross.
    return "fixed_chain"
}
