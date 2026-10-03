/** Workflow patterns around a model: orchestrator and workers, evaluator and optimiser, routing and voting. See ../../statement.md. */

typealias Ask = (String) -> String

fun orchestrate(ask: Ask, task: String, maxSubtasks: Int = 5): Map<String, Any?>? {
    // TODO: plan with one call, run a worker call for each subtask, combine the results with one more call.
    return null
}

fun refine(write: Ask, judge: Ask, task: String, maxRounds: Int = 3, threshold: Int = 8): Map<String, Any?>? {
    // TODO: write a draft, judge it, and revise with the feedback until the score reaches the threshold or the rounds run out.
    return null
}

fun route(ask: Ask, text: String, routes: Map<String, (String) -> String>, defaultLabel: String): Map<String, Any?>? {
    // TODO: classify the text with one model call, then run the handler of the label (or of the default label).
    return null
}

fun vote(ask: Ask, prompt: String, n: Int = 5): Map<String, Any?>? {
    // TODO: ask n times and return the majority answer, the counts and the share of the winner.
    return null
}
