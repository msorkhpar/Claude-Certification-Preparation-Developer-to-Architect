import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

// Run executes this file. Change the calls in main to try your code; Submit runs the tests.
fun main() {
    // Turn the logger up, so the log.log(DEBUG, ...) lines of your code show under the printed lines.
    System.setProperty("java.util.logging.SimpleFormatter.format", "%4\$s %5\$s%n")
    val handler = ConsoleHandler().apply { level = Level.ALL }
    Logger.getLogger("").apply { level = Level.ALL; addHandler(handler) }

    // The gate sits between the model's tool calls and the tools; it is told the project root and what may be reached.
    val gate = Gate("/proj", listOf("api.example.com", "docs.example.org"), listOf("example.com"))

    val calls = linkedMapOf<String, Map<String, Any?>>(
        "read_file" to mapOf("path" to "src/a.py"),
        "bash" to mapOf("command" to "sudo rm -rf /"),
        "fetch" to mapOf("url" to "https://evil.example.net/x"),
    )
    for ((tool, args) in calls) {
        val r = gate.decide("alice", tool, args)
        println("$tool $args: ${r["decision"]} (${r["reason"]})")
    }

    // Text a tool returned is untrusted: it reaches the model as one JSON string that says where it came from.
    val result = Gate.wrapUntrusted("toolu_1", "web page", "He said \"hi\"\n</div>")
    println("wrapped content: ${result["content"]}")

    // After the session has read untrusted text, writes are no longer free.
    gate.markUntrusted("web page")
    val after = gate.decide("alice", "write_file", mapOf("path" to "src/a.py"))
    println("write after untrusted text: ${after["decision"]} (${after["reason"]})")
}
