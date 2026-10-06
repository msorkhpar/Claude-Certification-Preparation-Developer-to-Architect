private val log = System.getLogger("pattern_ladder")

/**
 * The pattern ladder: which rung a task needs, and whether its value pays for the rung.
 *
 * Anthropic's article "Building effective agents" (read on 2026-10-04) says to "find the simplest solution possible, and only increasing
 * complexity when needed", and its multi-agent research write-up reports that "agents typically use about 4× more tokens than chat
 * interactions, and multi-agent systems use about 15× more tokens than chats". This file turns those two statements into a rule that picks
 * the lowest rung a task can stand on and prices it with the article's multipliers. The multipliers are the articles' reported figures, not
 * a measurement of your workload, and the rule is the course's own teaching model.
 */
data class Task(
    val name: String, val oneStep: Boolean, val steps: Int, val needsExternal: Boolean, val stepsKnown: Boolean,
    val independentParts: Boolean, val value: Double, val chatCost: Double,
)

val MULTIPLIER = mapOf("plain call" to 1, "augmented call" to 1, "agent" to 4, "multi-agent" to 15) // a workflow costs one chat per step

val TASKS = listOf(
    Task("classify ticket", true, 1, false, true, false, 0.05, 0.02),
    Task("answer from policy", true, 1, true, true, false, 0.40, 0.02),
    Task("claims intake", false, 4, true, true, false, 6.00, 0.02),
    Task("investigate outage", false, 0, true, false, false, 40.00, 0.02),
    Task("market research brief", false, 0, true, false, true, 25.00, 0.02),
    Task("trivia round-up", false, 0, true, false, true, 0.05, 0.02),
)

/** The lowest rung that fits: a call, an augmented call, a workflow, an agent, and a team only when its value covers the team's cost. */
fun choosePattern(task: Task): String {
    log.log(System.Logger.Level.DEBUG, "choosePattern input: {0}", task)
    return when {
        task.oneStep -> if (task.needsExternal) "augmented call" else "plain call"
        task.stepsKnown -> "workflow"
        task.independentParts && task.value >= MULTIPLIER.getValue("multi-agent") * task.chatCost -> "multi-agent"
        else -> "agent"
    }
}

fun cost(task: Task, pattern: String): Double = (if (pattern == "workflow") task.steps else MULTIPLIER.getValue(pattern)) * task.chatCost

private fun py(value: Boolean) = if (value) "True" else "False"

fun main() {
    for (task in TASKS) {
        val pattern = choosePattern(task)
        val price = cost(task, pattern)
        println("${task.name}: $pattern, cost ${"%.2f".format(price)}, value ${"%.2f".format(task.value)}, pays: ${py(task.value >= price)}")
    }
}
