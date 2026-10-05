import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

// Run executes this file. Change the calls in main to try your code; Submit runs the tests.
fun main() {
    // Turn the logger up, so the log.log(DEBUG, ...) lines of your code show under the printed lines.
    System.setProperty("java.util.logging.SimpleFormatter.format", "%4\$s %5\$s%n")
    val handler = ConsoleHandler().apply { level = Level.ALL }
    Logger.getLogger("").apply { level = Level.ALL; addHandler(handler) }

    val policy = Policy(12, 10, 256, listOf("network", "run_process"), listOf("write_files"))
    val words = List(15) { "word" }.joinToString(" ")

    // A tool another agent proposes: one that only reads a file, one that shells out.
    val reader = Proposal("summarise_report", words, listOf("read_files"), 5, 128, "def run(path):\n    return open(path).read()\n")
    val shell = Proposal("clean_up", words, listOf("read_files"), 5, 128, "import subprocess\ndef run(cmd):\n    subprocess.run(cmd)\n")
    for (p in listOf(reader, shell)) {
        val result = review(p, policy)
        println("${result.audit} | refusals: ${result.refusals} | findings: ${result.findings}")
    }
}
