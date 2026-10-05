import java.util.Locale

private val log = System.getLogger("workflows")

/** Workflow patterns around a model: orchestrator and workers, evaluator and optimiser, routing and voting. See ../../statement.md. */

typealias Ask = (String) -> String

private fun slice(text: String, open: Char, close: Char): String {
    val first = text.indexOf(open)
    val last = text.lastIndexOf(close)
    return if (first != -1 && last > first) text.substring(first, last + 1) else ""
}

private fun parseJson(text: String): Any? = try {
    Json.parse(text)
} catch (e: RuntimeException) {
    null
}

/**
 * TODO 1 of 9 (unlocks e1): the usable subtasks from the parsed plan.
 * Receives whatever Json.parse gave (a list, or anything else, or null) and returns the string items of a list, trimmed, with empty
 * ones and repeats dropped (the first of a repeat stays), in order; anything that is not a list gives an empty list.
 * Example: cleanSteps(listOf("a", " a ", "", 3, "b")) -> ["a", "b"]
 */
private fun cleanSteps(data: Any?): List<String> {
    return emptyList()
}

/**
 * TODO 2 of 9 (unlocks m1 and e2): run one worker call for one subtask.
 * Receives ask, the subtask and the task. Calls ask with "Subtask: $subtask\nTask: $task" and returns the map
 * {subtask, status "ok", output} for a good reply, or {subtask, status "failed", error} when the reply is empty or only spaces
 * (error "empty reply") or the call throws (error: the exception's message). It never throws.
 * Example: a worker that replies "  " -> {subtask: "s", status: "failed", error: "empty reply"}
 */
private fun runWorker(ask: Ask, subtask: String, task: String): Map<String, Any?> {
    log.log(System.Logger.Level.DEBUG, "runWorker input: {0}", subtask)
    return linkedMapOf("subtask" to subtask, "status" to "failed", "error" to "")
}

/**
 * TODO 3 of 9 (unlocks m1): the prompt of the combine call.
 * Receives the task and the results in plan order. Returns "Combine: write one answer to the task from the results.\nTask: $task"
 * followed by one line per result, "\n{i}. {subtask} -> {output}" or "\n{i}. {subtask} -> FAILED" (i from 1).
 * Example: task "T", one ok result "a" with output "x" -> "Combine: write one answer to the task from the results.\nTask: T\n1. a -> x"
 */
private fun combinePrompt(task: String, results: List<Map<String, Any?>>): String {
    return ""
}

fun orchestrate(ask: Ask, task: String, maxSubtasks: Int = 5): Map<String, Any?> {
    var calls = 1
    val planReply = ask("Plan: split the task into at most $maxSubtasks independent subtasks. Reply with a JSON array of strings only.\nTask: $task")
    val steps = cleanSteps(parseJson(slice(planReply, '[', ']')))
    val fallback = steps.isEmpty()
    val plan = if (fallback) listOf(task) else steps.take(maxSubtasks)
    val results = mutableListOf<Map<String, Any?>>()
    for (subtask in plan) {
        calls++
        results.add(runWorker(ask, subtask, task))
    }
    val ok = results.count { it["status"] == "ok" }
    if (ok == 0) return linkedMapOf("status" to "failed", "plan" to plan, "fallback" to fallback, "results" to results, "answer" to null, "calls" to calls)
    val answer = ask(combinePrompt(task, results))
    return linkedMapOf("status" to if (ok == results.size) "done" else "partial", "plan" to plan, "fallback" to fallback, "results" to results, "answer" to answer, "calls" to calls + 1)
}

private const val UNREADABLE = "The judge reply could not be read."

/**
 * TODO 4 of 9 (unlocks e4): is this a usable judge score?
 * Receives the score field of the judge's JSON (any value, or null). Returns true for a number from 0 to 10 and false for anything
 * else, a boolean included.
 * Example: scoreOk(7.5) -> true, scoreOk(true) -> false, scoreOk(11) -> false
 */
private fun scoreOk(score: Any?): Boolean {
    return false
}

/** The score and the feedback from the judge's reply; a score of 0 and a fixed note when the reply cannot be read. */
private fun readJudgement(text: String): Pair<Number, String> {
    val data = parseJson(slice(text, '{', '}')) as? Map<*, *>
    val score = data?.get("score")
    if (!scoreOk(score)) return Pair(0, UNREADABLE)
    return Pair(score as Number, data!!["feedback"] as? String ?: "")
}

/**
 * TODO 5 of 9 (unlocks e3): the prompt for the writer in one round.
 * Receives the task, the latest draft, the judge's latest feedback and the round number (from 1). Returns "Task: $task" in round 1
 * and afterwards "Task: $task\nPrevious draft: $draft\nFeedback: $feedback\nRevise the draft."
 * Example: writerPrompt("T", "d", "f", 2) -> "Task: T\nPrevious draft: d\nFeedback: f\nRevise the draft."
 */
private fun writerPrompt(task: String, draft: String?, feedback: String, round: Int): String {
    return "Task: $task"
}

/**
 * TODO 6 of 9 (unlocks e4): does this draft replace the best one so far?
 * Receives the new score and the best score so far (null before any draft was judged). Returns true when there is no best yet or the
 * new score is strictly higher (so the earliest wins a tie).
 * Example: isBetter(5, null) -> true, isBetter(5, 5) -> false
 */
private fun isBetter(score: Number, bestScore: Number?): Boolean {
    return false
}

/**
 * TODO 7 of 9 (unlocks e7): the result when the writer threw.
 * Receives the best draft and score so far (null, null before any), the rounds completed, the history and the exception. Returns the
 * map {status "error", draft, score, rounds, history, error: the exception's message}.
 * Example: errorResult("d", 4, 1, listOf(), RuntimeException("boom"))["error"] -> "boom"
 */
private fun errorResult(best: String?, bestScore: Number?, rounds: Int, history: List<Map<String, Any?>>, e: RuntimeException): Map<String, Any?> {
    return linkedMapOf("status" to "error", "draft" to best, "score" to bestScore, "rounds" to rounds, "history" to history, "error" to "")
}

fun refine(write: Ask, judge: Ask, task: String, maxRounds: Int = 3, threshold: Int = 8): Map<String, Any?> {
    val history = mutableListOf<Map<String, Any?>>()
    var best: String? = null
    var bestScore: Number? = null
    var draft: String? = null
    var feedback = ""
    for (round in 1..maxRounds) {
        val prompt = writerPrompt(task, draft, feedback, round)
        try {
            draft = write(prompt)
        } catch (e: RuntimeException) {
            return errorResult(best, bestScore, round - 1, history, e)
        }
        val (score, note) = readJudgement(judge("Judge: score the draft from 0 to 10 and reply with JSON {\"score\": n, \"feedback\": \"...\"}.\nTask: $task\nDraft: $draft"))
        feedback = note
        history.add(linkedMapOf("round" to round, "score" to score, "feedback" to feedback))
        if (isBetter(score, bestScore)) {
            best = draft
            bestScore = score
        }
        if (score.toDouble() >= threshold) return linkedMapOf("status" to "accepted", "draft" to draft, "score" to score, "rounds" to round, "history" to history)
    }
    return linkedMapOf("status" to "max_rounds", "draft" to best, "score" to bestScore, "rounds" to maxRounds, "history" to history)
}

/**
 * TODO 8 of 9 (unlocks e5): the label in a classifier's reply.
 * Receives the reply. Returns it trimmed of spaces, then of the punctuation .,;:!"' and the backtick at both ends, then of spaces
 * again, in lower case.
 * Example: normaliseLabel("  \"Billing.\" ") -> "billing"
 */
private fun normaliseLabel(reply: String): String {
    return ""
}

/** Classify the text with one model call, then run the handler of the label. routes maps label to a function of the text. */
fun route(ask: Ask, text: String, routes: Map<String, (String) -> String>, defaultLabel: String): Map<String, Any?> {
    val reply = ask("Classify: $text\nLabels: ${routes.keys.joinToString(", ")}")
    var label = normaliseLabel(reply)
    val fallback = label !in routes
    if (fallback) label = defaultLabel
    return linkedMapOf("label" to label, "output" to routes.getValue(label)(text), "fallback" to fallback)
}

/**
 * TODO 9 of 9 (unlocks e6): the answer that wins the vote.
 * Receives the counts (answer to count, in order of first appearance) and the highest count. Returns the first answer whose count is
 * that highest count, so a tie goes to the one seen first.
 * Example: pickWinner(linkedMapOf("a" to 2, "b" to 2), 2) -> "a"
 */
private fun pickWinner(counts: Map<String, Int>, top: Int): String {
    return ""
}

fun vote(ask: Ask, prompt: String, n: Int = 5): Map<String, Any?> {
    val counts = linkedMapOf<String, Int>()
    repeat(n) { counts.merge(ask(prompt).trim().lowercase(Locale.ROOT), 1, Int::plus) }
    if (counts.isEmpty()) return linkedMapOf("answer" to null, "votes" to counts, "agreement" to 0.0)
    val top = counts.values.max()
    val winner = pickWinner(counts, top)
    return linkedMapOf("answer" to winner, "votes" to counts, "agreement" to top.toDouble() / n)
}
