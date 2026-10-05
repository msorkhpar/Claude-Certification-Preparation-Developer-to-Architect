import harness.Show.py

private val log = System.getLogger("refinement")

/**
 * Three decisions of a Claude Code session on a code-generation task: plan mode or direct execution, one message or several for a list of problems, and what a failing test run must say.
 *
 * The rules are the ones the exam guide states for tasks 3.4 and 3.5 and the best-practices page confirms (read on 2026-10-03): plan when the change is large, architectural,
 * touches many files or has more than one valid approach; execute directly when you could describe the diff in one sentence; send interacting problems in one message and
 * independent problems one after another; and give the model the failing tests, with input and expected output, as the target. No model is called.
 */

/** What decides the mode of a task. */
data class Task(val diffInOneSentence: Boolean, val files: Int, val architectural: Boolean, val approaches: Int)

/** A problem found in review and the ids of the problems it interacts with. */
data class Issue(val id: String, val interactsWith: List<String> = emptyList())

/** One test run: the test's name, its input, the expected output and the actual one. */
data class Result(val name: String, val input: Any?, val expected: Any?, val actual: Any?)

/** Phases of the work: `plan` first when the change is large, architectural, spread over files or has several valid approaches. */
fun chooseMode(task: Task): List<String> {
    val small = task.diffInOneSentence && task.files <= 1 && !task.architectural
    if (small) return listOf("implement")
    if (task.architectural || task.approaches > 1 || task.files > 1) return listOf("explore", "plan", "implement")
    return listOf("implement")
}

/** Messages to send, in order: problems that interact travel together, independent ones go one at a time. */
fun groupFeedback(issues: List<Issue>): List<List<String>> {
    val parent = issues.associate { it.id to it.id }.toMutableMap()
    fun find(start: String): String {
        var x = start
        while (parent.getValue(x) != x) {
            parent[x] = parent.getValue(parent.getValue(x))
            x = parent.getValue(x)
        }
        return x
    }
    for (issue in issues) for (other in issue.interactsWith) if (other in parent) parent[find(issue.id)] = find(other)
    val groups = linkedMapOf<String, MutableList<String>>()
    for (issue in issues) groups.getOrPut(find(issue.id)) { mutableListOf() }.add(issue.id)
    return groups.values.toList()
}

/** The message that closes the loop: each failing test with its input, the expected output and the actual one; nothing about the passing tests. */
fun failureReport(results: List<Result>): String {
    val failing = results.filter { it.actual != it.expected }
    if (failing.isEmpty()) return "All tests pass."
    return (listOf("${failing.size} of ${results.size} tests fail:") + failing.map { "- ${it.name}: input ${py(it.input)}, expected ${py(it.expected)}, got ${py(it.actual)}" }).joinToString("\n")
}

fun main() {
    val tasks = linkedMapOf(
        "rename a variable in one function" to Task(true, 1, false, 1),
        "add a date check to one handler" to Task(true, 1, false, 1),
        "split a monolith into services" to Task(false, 60, true, 3),
        "migrate a library used in 45 files" to Task(false, 45, false, 1),
    )
    for ((name, task) in tasks) println("$name: ${chooseMode(task).joinToString(" > ")}")
    val issues = listOf(Issue("sort-order", listOf("pagination")), Issue("pagination", listOf("sort-order")), Issue("typo-in-label"), Issue("null-date"))
    println("messages: ${py(groupFeedback(issues))}")
    val results = listOf(
        Result("keeps order", listOf(3, 1, 2), listOf(1, 2, 3), listOf(1, 2, 3)),
        Result("empty list", emptyList<Int>(), emptyList<Int>(), null),
        Result("null entry", listOf(2, null), listOf(2), listOf(2, null)),
    )
    println(failureReport(results))
}
