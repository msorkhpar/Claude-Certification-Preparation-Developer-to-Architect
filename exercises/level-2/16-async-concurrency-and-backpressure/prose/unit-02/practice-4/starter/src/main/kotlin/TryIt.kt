import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

// Run executes this file. Change the calls in main to try your code; Submit runs the tests.
fun main() {
    // Turn the logger up, so the log.log(DEBUG, ...) lines of your code show under the printed lines.
    System.setProperty("java.util.logging.SimpleFormatter.format", "%4\$s %5\$s%n")
    val handler = ConsoleHandler().apply { level = Level.ALL }
    Logger.getLogger("").apply { level = Level.ALL; addHandler(handler) }

    val now = AtomicInteger()
    val peak = AtomicInteger()

    // Ten items, never more than three at a time; the work is a stand-in for one API call.
    val outcomes = mapBounded((0 until 10).iterator(), { item: Int ->
        peak.accumulateAndGet(now.incrementAndGet()) { a, b -> maxOf(a, b) }
        Thread.sleep(20)
        now.decrementAndGet()
        item * 2
    }, 3)

    println("outcomes: ${outcomes.size}")
    println("values: ${outcomes.filter { it.ok }.map { it.value }.sortedBy { it }}")
    println("all ok: ${outcomes.all { it.ok }}")
    println("peak in flight: ${peak.get()}")
}
