import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

// Run executes this file. Change the calls in main to try your code; Submit runs the tests.
fun main() {
    // Turn the logger up, so the log.log(DEBUG, ...) lines of your code show under the printed lines.
    System.setProperty("java.util.logging.SimpleFormatter.format", "%4\$s %5\$s%n")
    val handler = ConsoleHandler().apply { level = Level.ALL }
    Logger.getLogger("").apply { level = Level.ALL; addHandler(handler) }

    val files = listOf(mapOf("path" to "api.py", "text" to "def get(): ..."), mapOf("path" to "db.py", "text" to "def query(): ..."),
        mapOf("path" to "ui.py", "text" to "def show(): ..."))
    val script = mapOf(
        "api" to mapOf("findings" to listOf("api: no auth"), "summary" to "api calls db.query(id)"),
        "db" to mapOf("findings" to emptyList<String>(), "summary" to "db.query takes a name"),
        "ui" to mapOf("findings" to listOf("ui: unused"), "summary" to "ui shows rows"))

    // A scripted stand-in for the model reviewing one file (or one part of it).
    val filePass: FilePass = { path, _, _, _ -> script.getValue(path.substringBefore(".")) }
    // The second look: it reads only the summaries, and finds what no single file shows.
    val crossPass: CrossPass = { _ -> listOf("api passes id but db expects a name") }

    // Each file is reviewed alone, then the cross pass reads their summaries.
    val result = reviewChanges(files, filePass, crossPass)

    (result?.get("files") as? Map<*, *>)?.forEach { (path, review) -> println("$path -> ${(review as Map<*, *>)["findings"]} | parts: ${review["parts"]}") }
    println("cross findings: ${result?.get("cross")}")
    println("failed: ${result?.get("failed")} | skipped: ${result?.get("skipped")}")
}
