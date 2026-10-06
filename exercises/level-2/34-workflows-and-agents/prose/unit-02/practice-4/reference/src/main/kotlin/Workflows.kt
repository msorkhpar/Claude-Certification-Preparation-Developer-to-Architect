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

private fun cleanSteps(data: Any?): List<String> {
    val steps = mutableListOf<String>()
    (data as? List<*>)?.forEach { item ->
        if (item is String && item.isNotBlank() && item.trim() !in steps) steps.add(item.trim())
    }
    return steps
}

private fun runWorker(ask: Ask, subtask: String, task: String): Map<String, Any?> {
    log.log(System.Logger.Level.DEBUG, "runWorker input: {0}", subtask)
    return try {
        val output = ask("Subtask: $subtask\nTask: $task")
        if (output.isBlank()) throw IllegalStateException("empty reply")
        linkedMapOf("subtask" to subtask, "status" to "ok", "output" to output)
    } catch (e: RuntimeException) { // one worker failing must not stop the others
        linkedMapOf("subtask" to subtask, "status" to "failed", "error" to e.message)
    }
}

private fun combinePrompt(task: String, results: List<Map<String, Any?>>): String {
    val lines = results.mapIndexed { i, r -> "${i + 1}. ${r["subtask"]} -> ${if (r["status"] == "ok") r["output"] else "FAILED"}" }
    return "Combine: write one answer to the task from the results.\nTask: $task\n" + lines.joinToString("\n")
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

private fun scoreOk(score: Any?): Boolean {
    return score is Number && score.toDouble() >= 0 && score.toDouble() <= 10
}

/** The score and the feedback from the judge's reply; a score of 0 and a fixed note when the reply cannot be read. */
private fun readJudgement(text: String): Pair<Number, String> {
    val data = parseJson(slice(text, '{', '}')) as? Map<*, *>
    val score = data?.get("score")
    if (!scoreOk(score)) return Pair(0, UNREADABLE)
    return Pair(score as Number, data!!["feedback"] as? String ?: "")
}

private fun writerPrompt(task: String, draft: String?, feedback: String, round: Int): String {
    return if (round == 1) "Task: $task" else "Task: $task\nPrevious draft: $draft\nFeedback: $feedback\nRevise the draft."
}

private fun isBetter(score: Number, bestScore: Number?): Boolean {
    return bestScore == null || score.toDouble() > bestScore.toDouble()
}

private fun errorResult(best: String?, bestScore: Number?, rounds: Int, history: List<Map<String, Any?>>, e: RuntimeException): Map<String, Any?> {
    return linkedMapOf("status" to "error", "draft" to best, "score" to bestScore, "rounds" to rounds, "history" to history, "error" to e.message)
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

private fun normaliseLabel(reply: String): String {
    return reply.trim().replace(Regex("^[.,;:!\"'`]+|[.,;:!\"'`]+$"), "").trim().lowercase(Locale.ROOT)
}

/** Classify the text with one model call, then run the handler of the label. routes maps label to a function of the text. */
fun route(ask: Ask, text: String, routes: Map<String, (String) -> String>, defaultLabel: String): Map<String, Any?> {
    val reply = ask("Classify: $text\nLabels: ${routes.keys.joinToString(", ")}")
    var label = normaliseLabel(reply)
    val fallback = label !in routes
    if (fallback) label = defaultLabel
    return linkedMapOf("label" to label, "output" to routes.getValue(label)(text), "fallback" to fallback)
}

private fun pickWinner(counts: Map<String, Int>, top: Int): String {
    return counts.entries.first { it.value == top }.key
}

fun vote(ask: Ask, prompt: String, n: Int = 5): Map<String, Any?> {
    val counts = linkedMapOf<String, Int>()
    repeat(n) { counts.merge(ask(prompt).trim().lowercase(Locale.ROOT), 1, Int::plus) }
    if (counts.isEmpty()) return linkedMapOf("answer" to null, "votes" to counts, "agreement" to 0.0)
    val top = counts.values.max()
    val winner = pickWinner(counts, top)
    return linkedMapOf("answer" to winner, "votes" to counts, "agreement" to top.toDouble() / n)
}
