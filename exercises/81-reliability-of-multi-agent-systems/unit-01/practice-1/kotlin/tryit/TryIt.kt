import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

// Run executes this file. Change the calls in main to try your code; Submit runs the tests.
fun main() {
    // Turn the logger up, so the log.log(DEBUG, ...) lines of your code show under the printed lines.
    System.setProperty("java.util.logging.SimpleFormatter.format", "%4\$s %5\$s%n")
    val handler = ConsoleHandler().apply { level = Level.ALL }
    Logger.getLogger("").apply { level = Level.ALL; addHandler(handler) }

    fun task(id: String, needs: List<String> = emptyList()): Map<String, Any?> =
        linkedMapOf("id" to id, "agent" to "w", "key" to "key-$id", "needs" to needs, "fallback" to null)

    // A stand-in for a worker agent, like the tests use: its first call is lost, then it answers.
    var calls = 0
    val flaky: Agent = { key, inputs ->
        calls += 1
        if (calls == 1) throw Transient("lost")
        key + "|" + inputs.toSortedMap().values.joinToString(",")
    }

    // Task b needs the result of a; the lost call is retried, the result is checkpointed in the store.
    val store = linkedMapOf<String, String>()
    val result = runPlan(listOf(task("a"), task("b", listOf("a"))), mapOf("w" to flaky), store, 3, 3)
    println("done: ${result?.get("done")}")
    println("attempts: ${result?.get("attempts")}")
    println("failed: ${result?.get("failed")} | skipped: ${result?.get("skipped")}")
    println("store: $store")
}
