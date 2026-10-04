/**
 * Two decompositions with scripted model replies: a per-file pass followed by one cross-file pass, and an adaptive loop that plans each step from the last.
 *
 * The "model" is a set of hand-written functions, so the output shows what each pass was given and what the control flow did with the answers, and nothing about
 * how a real model would review the code. The files, summaries and findings are illustrative.
 */
val CHANGE = linkedMapOf(
    "api.py" to "def get_user(id):\n    return db.find(id)\n",
    "db.py" to "def find(name):\n    return rows.get(name)\n",
    "ui.py" to "def show(user):\n    print(user['name'])\n",
)
val SUMMARIES = linkedMapOf("api.py" to "get_user passes an id to db.find", "db.py" to "find looks a row up by name", "ui.py" to "show prints the name field")

/** One step of the adaptive run: the subtask and what the worker answered. */
data class Step(val subtask: String, val result: String)

/** What the planner answers: done with a summary, or the next subtask. */
data class Plan(val done: Boolean, val next: String? = null, val summary: String? = null)

/** The end of an adaptive run. */
data class Run(val status: String, val steps: List<Step>, val summary: String)

/** One call per file: it is given this file and nothing else. */
fun filePass(path: String, text: String): String {
    println("  file pass $path: ${text.lines().size - (if (text.endsWith("\n")) 1 else 0)} lines, no other file")
    return SUMMARIES.getValue(path)
}

/** One call over the summaries: the relations between files, never the text. */
fun crossPass(summaries: List<Pair<String, String>>): List<String> {
    println("  cross pass: ${summaries.size} summaries, ${CHANGE.values.sumOf { it.length }} characters of source withheld")
    val names = summaries.toMap()
    return if ("id" in names.getValue("api.py") && "name" in names.getValue("db.py")) listOf("api.py passes an id but db.py looks up by name") else emptyList()
}

/** A scripted planner: what it answers depends on what the steps so far found. */
fun plan(goal: String, steps: List<Step>): Plan {
    val done = steps.map { it.subtask }
    return when {
        steps.isEmpty() -> Plan(false, next = "list the test files")
        "list the test files" in done && "run the failing test" !in done -> Plan(false, next = "run the failing test")
        "read the module under test" in done -> Plan(true, summary = "the failure is in parse()")
        steps.last().result.startsWith("1 failure") -> Plan(false, next = "read the module under test")
        else -> Plan(true, summary = "nothing failed")
    }
}

fun work(subtask: String): String =
    mapOf("list the test files" to "3 files", "run the failing test" to "1 failure in test_parse", "read the module under test" to "parse() drops the last field").getValue(subtask)

fun runAdaptive(goal: String, maxSteps: Int = 5): Run {
    val steps = mutableListOf<Step>()
    while (true) {
        val reply = plan(goal, steps.toList())
        if (reply.done) return Run("done", steps, reply.summary!!)
        if (steps.size >= maxSteps) return Run("step_limit", steps, "")
        steps += Step(reply.next!!, work(reply.next))
        println("  step ${steps.size}: ${reply.next} -> ${steps.last().result}")
    }
}

fun main() {
    println("per file, then across files:")
    val summaries = CHANGE.map { (path, text) -> path to filePass(path, text) }
    for (finding in crossPass(summaries)) println("  finding: $finding")
    println("\nadaptive, each step planned from the last:")
    val run = runAdaptive("find why the parser test fails")
    println("  status: ${run.status} after ${run.steps.size} steps; ${run.summary}")
}
