/** Task decomposition: a per-item pass and a cross-item pass, an adaptive loop, and the choice between them. See ../../statement.md. */

/** The model's review of one file, or of one part of it: a map with "findings" (a list of text) and "summary" (text). */
typealias FilePass = (String, String, Int, Int) -> Map<String, Any?>
typealias CrossPass = (List<Map<String, String>>) -> List<String>
typealias StepPlanner = (String, List<Map<String, String>>) -> Any?

fun reviewChanges(files: List<Map<String, String>>, filePass: FilePass, crossPass: CrossPass, maxLines: Int = 40): Map<String, Any?>? {
    // TODO: review each file alone (long files in parts), then run one cross pass over the summaries of the reviewed files.
    return null
}

fun runAdaptive(planner: StepPlanner, worker: (String) -> String, goal: String, maxSteps: Int = 6): Map<String, Any?>? {
    // TODO: ask the planner what to do next after every step, and stop when it is done, stuck or out of steps.
    return null
}

fun chooseStrategy(task: Map<String, Any?>): String? {
    // TODO: fixed_chain, per_item_then_cross or adaptive.
    return null
}
