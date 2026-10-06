import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

// Run executes this file. Change the calls in main to try your code; Submit runs the tests.
fun main() {
    // Turn the logger up, so the log.log(DEBUG, ...) lines of your code show under the printed lines.
    System.setProperty("java.util.logging.SimpleFormatter.format", "%4\$s %5\$s%n")
    val handler = ConsoleHandler().apply { level = Level.ALL }
    Logger.getLogger("").apply { level = Level.ALL; addHandler(handler) }

    // Accuracy per document type and field, next to the overall figure.
    val records = List(90) { Rec("invoice", "total", true) } + List(8) { Rec("receipt", "date", true) } + List(2) { Rec("receipt", "date", false) }
    for (row in accuracyBy(records)) println("accuracy: $row")

    // The lowest confidence at which the model still reaches the target accuracy, from labelled outcomes.
    val labeled = listOf(Labeled(95, true), Labeled(90, true), Labeled(85, true), Labeled(80, false), Labeled(60, false))
    println("threshold for 90% accuracy: ${calibrateThreshold(labeled, 90)}")

    // Which extractions a person reviews: conflicts and low confidence first, up to the capacity.
    val extractions = listOf(Extraction("x1", 95, false), Extraction("x2", 60, false), Extraction("x3", 90, true), Extraction("x4", 70, false))
    println("routing: ${route(extractions, 80, 2)}")
    println("checkpoints: ${checkpoint("send_payment", 5)} ${checkpoint("update_note", 50)}")
}
